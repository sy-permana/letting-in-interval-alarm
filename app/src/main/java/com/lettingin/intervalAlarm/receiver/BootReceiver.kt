package com.lettingin.intervalAlarm.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.lettingin.intervalAlarm.LettingInApplication
import com.lettingin.intervalAlarm.data.repository.AlarmRepository
import com.lettingin.intervalAlarm.data.repository.AlarmStateRepository
import com.lettingin.intervalAlarm.domain.scheduler.AlarmScheduler
import com.lettingin.intervalAlarm.domain.scheduler.AlarmSchedulerImpl
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.ZoneId
import javax.inject.Inject

class BootReceiver : BroadcastReceiver() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    companion object {
        private const val TAG = "BootReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED &&
            intent.action != "android.intent.action.QUICKBOOT_POWERON"
        ) {
            return
        }

        Log.d(TAG, "Device booted, restoring active alarm")

        // Get dependencies from Hilt
        val appContext = context.applicationContext as LettingInApplication
        val entryPoint = EntryPointAccessors.fromApplication(
            appContext,
            BootReceiverEntryPoint::class.java
        )

        // Use goAsync to handle async operations
        val pendingResult = goAsync()

        scope.launch {
            try {
                val appLogger = entryPoint.appLogger()
                appLogger.logSystemEvent("BOOT_COMPLETED", "Device rebooted, validating and restoring alarms")
                
                // Use RebootValidator for comprehensive validation
                val rebootValidator = entryPoint.rebootValidator()
                val validationResult = rebootValidator.validateAfterReboot()
                
                if (validationResult.errors.isNotEmpty()) {
                    appLogger.w(com.lettingin.intervalAlarm.util.AppLogger.CATEGORY_SYSTEM, TAG,
                        "Reboot validation completed with errors: ${validationResult.errors.joinToString(", ")}")
                } else {
                    appLogger.i(com.lettingin.intervalAlarm.util.AppLogger.CATEGORY_SYSTEM, TAG,
                        "Reboot validation successful: activeAlarm=${validationResult.activeAlarmFound}, rescheduled=${validationResult.alarmRescheduled}")
                }
                
                // Fallback to old method if validation didn't reschedule
                if (validationResult.activeAlarmFound && !validationResult.alarmRescheduled) {
                    Log.d(TAG, "Validation didn't reschedule, using fallback method")
                    restoreActiveAlarm(
                        entryPoint.alarmRepository(),
                        entryPoint.alarmStateRepository(),
                        entryPoint.alarmScheduler()
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error restoring alarm after boot", e)
                entryPoint.appLogger().e(com.lettingin.intervalAlarm.util.AppLogger.CATEGORY_ERROR, TAG,
                    "Error restoring alarm after boot", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private suspend fun restoreActiveAlarm(
        alarmRepository: AlarmRepository,
        alarmStateRepository: AlarmStateRepository,
        alarmScheduler: AlarmScheduler
    ) {
        // Query active alarm from database
        val activeAlarm = alarmRepository.getActiveAlarm().firstOrNull()

        if (activeAlarm == null) {
            Log.d(TAG, "No active alarm to restore")
            return
        }

        Log.d(TAG, "Restoring active alarm: ${activeAlarm.id}")

        // Get current time
        val currentTimeMillis = System.currentTimeMillis()
        val now = LocalDateTime.ofInstant(
            java.time.Instant.ofEpochMilli(currentTimeMillis),
            ZoneId.systemDefault()
        )
        val currentTime = now.toLocalTime()
        val today = now.toLocalDate()
        
        // Check if alarm state exists and reset pause if we've moved to a new day
        val existingState = alarmStateRepository.getAlarmStateSync(activeAlarm.id)
        if (existingState != null && existingState.isPaused) {
            val pauseSetDate = if (existingState.pauseUntilTime != null) {
                LocalDateTime.ofInstant(
                    java.time.Instant.ofEpochMilli(existingState.pauseUntilTime),
                    ZoneId.systemDefault()
                ).toLocalDate()
            } else {
                today
            }
            
            // Reset pause state if we've moved to a new day
            if (pauseSetDate.isBefore(today)) {
                Log.d(TAG, "Resetting pause state - moved to new day")
                alarmStateRepository.updateAlarmState(
                    existingState.copy(
                        isPaused = false,
                        pauseUntilTime = null
                    )
                )
            }
        }

        // Check if we're within the active time window
        val isValidDay = activeAlarm.selectedDays.contains(today.dayOfWeek)
        val isWithinTimeWindow = currentTime.isAfter(activeAlarm.startTime) &&
                currentTime.isBefore(activeAlarm.endTime)

        if (!isValidDay || !isWithinTimeWindow) {
            Log.d(TAG, "Not within active time window, scheduling for next valid time")
            // Calculate next valid ring time
            val nextRingTime = (alarmScheduler as? AlarmSchedulerImpl)?.calculateNextRingTime(
                activeAlarm,
                currentTimeMillis
            )

            if (nextRingTime != null) {
                alarmScheduler.scheduleNextRing(activeAlarm.id, nextRingTime)
                
                // Update alarm state
                val alarmState = alarmStateRepository.getAlarmStateSync(activeAlarm.id)
                if (alarmState != null) {
                    alarmStateRepository.updateAlarmState(
                        alarmState.copy(nextScheduledRingTime = nextRingTime)
                    )
                } else {
                    // Create new alarm state
                    val newState = com.lettingin.intervalAlarm.data.model.AlarmState(
                        alarmId = activeAlarm.id,
                        lastRingTime = null,
                        nextScheduledRingTime = nextRingTime,
                        isPaused = false,
                        pauseUntilTime = null,
                        isStoppedForDay = false,
                        currentDayStartTime = currentTimeMillis,
                        todayRingCount = 0,
                        todayUserDismissCount = 0,
                        todayAutoDismissCount = 0
                    )
                    alarmStateRepository.updateAlarmState(newState)
                }
            } else {
                Log.w(TAG, "No valid next ring time found")
                // If one-cycle mode and no more valid times, deactivate
                if (!activeAlarm.isRepeatable) {
                    alarmRepository.deactivateAlarm(activeAlarm.id)
                }
            }
            return
        }

        // We're within the time window, calculate next ring time based on current time
        Log.d(TAG, "Within active time window, calculating next ring time")
        
        val nextRingTime = (alarmScheduler as? AlarmSchedulerImpl)?.calculateNextRingTime(
            activeAlarm,
            currentTimeMillis
        )

        if (nextRingTime != null) {
            alarmScheduler.scheduleNextRing(activeAlarm.id, nextRingTime)
            
            // Update alarm state
            val alarmState = alarmStateRepository.getAlarmStateSync(activeAlarm.id)
            if (alarmState != null) {
                alarmStateRepository.updateAlarmState(
                    alarmState.copy(
                        nextScheduledRingTime = nextRingTime,
                        // Reset stopped for day flag on boot
                        isStoppedForDay = false
                    )
                )
            } else {
                // Create new alarm state
                val newState = com.lettingin.intervalAlarm.data.model.AlarmState(
                    alarmId = activeAlarm.id,
                    lastRingTime = null,
                    nextScheduledRingTime = nextRingTime,
                    isPaused = false,
                    pauseUntilTime = null,
                    isStoppedForDay = false,
                    currentDayStartTime = currentTimeMillis,
                    todayRingCount = 0,
                    todayUserDismissCount = 0,
                    todayAutoDismissCount = 0
                )
                alarmStateRepository.updateAlarmState(newState)
            }
            
            Log.d(TAG, "Alarm restored and scheduled for ${
                LocalDateTime.ofInstant(
                    java.time.Instant.ofEpochMilli(nextRingTime),
                    ZoneId.systemDefault()
                )
            }")
        } else {
            Log.w(TAG, "No valid next ring time found after boot")
            // If one-cycle mode and no more valid times, deactivate
            if (!activeAlarm.isRepeatable) {
                alarmRepository.deactivateAlarm(activeAlarm.id)
            }
        }
    }
}


@dagger.hilt.EntryPoint
@dagger.hilt.InstallIn(dagger.hilt.components.SingletonComponent::class)
interface BootReceiverEntryPoint {
    fun alarmRepository(): AlarmRepository
    fun alarmStateRepository(): AlarmStateRepository
    fun alarmScheduler(): AlarmScheduler
    fun rebootValidator(): com.lettingin.intervalAlarm.util.RebootValidator
    fun appLogger(): com.lettingin.intervalAlarm.util.AppLogger
}
