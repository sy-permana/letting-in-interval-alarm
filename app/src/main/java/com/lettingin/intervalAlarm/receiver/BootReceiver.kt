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
import com.lettingin.intervalAlarm.util.AlarmStateRecoveryManager
import com.lettingin.intervalAlarm.util.AlarmStateValidator
import com.lettingin.intervalAlarm.util.AppLogger
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
                
                // Post-restoration validation: verify alarm is actually scheduled
                if (validationResult.activeAlarmFound) {
                    validateRestoredAlarm(
                        entryPoint.alarmRepository(),
                        entryPoint.alarmStateRepository(),
                        entryPoint.alarmScheduler(),
                        entryPoint.alarmStateValidator(),
                        entryPoint.alarmStateRecoveryManager(),
                        appLogger
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

    /**
     * Validates that the restored alarm is actually scheduled in AlarmManager
     * and has a valid next ring time. Performs recovery if validation fails.
     */
    private suspend fun validateRestoredAlarm(
        alarmRepository: AlarmRepository,
        alarmStateRepository: AlarmStateRepository,
        alarmScheduler: AlarmScheduler,
        alarmStateValidator: AlarmStateValidator,
        alarmStateRecoveryManager: AlarmStateRecoveryManager,
        appLogger: AppLogger
    ) {
        try {
            appLogger.i(AlarmStateValidator.CATEGORY_STATE_VALIDATION, TAG,
                "Starting post-boot validation of restored alarm")
            
            // Get active alarm
            val activeAlarm = alarmRepository.getActiveAlarm().firstOrNull()
            if (activeAlarm == null) {
                appLogger.d(AlarmStateValidator.CATEGORY_STATE_VALIDATION, TAG,
                    "No active alarm found during post-boot validation")
                return
            }
            
            // Get alarm state
            val alarmState = alarmStateRepository.getAlarmStateSync(activeAlarm.id)
            
            // Validate the alarm state
            val validationResult = alarmStateValidator.validateAlarmState(activeAlarm, alarmState)
            
            if (!validationResult.isValid) {
                appLogger.w(AlarmStateValidator.CATEGORY_STATE_VALIDATION, TAG,
                    "Post-boot validation failed: alarmId=${activeAlarm.id}, " +
                    "issues=${validationResult.issues}, action=${validationResult.suggestedAction}")
                
                // Attempt recovery
                appLogger.i(AlarmStateRecoveryManager.CATEGORY_STATE_RECOVERY, TAG,
                    "Attempting recovery for failed post-boot validation: alarmId=${activeAlarm.id}")
                
                val recoveryResult = alarmStateRecoveryManager.recoverAlarmState(activeAlarm.id)
                
                if (recoveryResult.success) {
                    appLogger.i(AlarmStateRecoveryManager.CATEGORY_STATE_RECOVERY, TAG,
                        "Post-boot recovery successful: alarmId=${activeAlarm.id}, " +
                        "action='${recoveryResult.action}', newNextRingTime=${recoveryResult.newNextRingTime}")
                } else {
                    appLogger.e(com.lettingin.intervalAlarm.util.AppLogger.CATEGORY_ERROR, TAG,
                        "Post-boot recovery failed: alarmId=${activeAlarm.id}, " +
                        "action='${recoveryResult.action}'", recoveryResult.error)
                }
            } else {
                appLogger.i(AlarmStateValidator.CATEGORY_STATE_VALIDATION, TAG,
                    "Post-boot validation successful: alarmId=${activeAlarm.id}, " +
                    "nextRingTime=${alarmState?.nextScheduledRingTime}")
                
                // Even if validation passed, verify alarm is actually scheduled in AlarmManager
                if (alarmState != null && !alarmState.isPaused && !alarmState.isStoppedForDay) {
                    val isScheduled = alarmScheduler.isAlarmScheduled(activeAlarm.id)
                    if (!isScheduled) {
                        appLogger.w(AlarmStateValidator.CATEGORY_STATE_VALIDATION, TAG,
                            "Alarm passed validation but not scheduled in AlarmManager: alarmId=${activeAlarm.id}")
                        
                        // Reschedule it
                        alarmState.nextScheduledRingTime?.let { nextRingTime ->
                            appLogger.i(AlarmStateRecoveryManager.CATEGORY_STATE_RECOVERY, TAG,
                                "Rescheduling alarm in AlarmManager: alarmId=${activeAlarm.id}, nextRingTime=$nextRingTime")
                            alarmScheduler.scheduleNextRing(activeAlarm.id, nextRingTime)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            appLogger.e(com.lettingin.intervalAlarm.util.AppLogger.CATEGORY_ERROR, TAG,
                "Error during post-boot validation", e)
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
                // Validate that the calculated next ring time is valid (not in the past)
                if (nextRingTime < currentTimeMillis) {
                    Log.w(TAG, "Calculated next ring time is in the past: $nextRingTime, current: $currentTimeMillis")
                    // If one-cycle mode and no more valid times, deactivate
                    if (!activeAlarm.isRepeatable) {
                        alarmRepository.deactivateAlarm(activeAlarm.id)
                    }
                    return
                }
                
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
                
                Log.d(TAG, "Alarm scheduled for next valid time: ${
                    LocalDateTime.ofInstant(
                        java.time.Instant.ofEpochMilli(nextRingTime),
                        ZoneId.systemDefault()
                    )
                }")
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
            // Validate that the calculated next ring time is valid (not in the past)
            if (nextRingTime < currentTimeMillis) {
                Log.w(TAG, "Calculated next ring time is in the past: $nextRingTime, current: $currentTimeMillis")
                // This shouldn't happen, but if it does, don't schedule it
                return
            }
            
            // Validate that next ring time is within alarm's time window
            val nextRingDateTime = LocalDateTime.ofInstant(
                java.time.Instant.ofEpochMilli(nextRingTime),
                ZoneId.systemDefault()
            )
            val isValidNextDay = activeAlarm.selectedDays.contains(nextRingDateTime.dayOfWeek)
            val ringTime = nextRingDateTime.toLocalTime()
            val isWithinNextWindow = ringTime >= activeAlarm.startTime && ringTime <= activeAlarm.endTime
            
            if (!isValidNextDay || !isWithinNextWindow) {
                Log.w(TAG, "Calculated next ring time is outside valid window: day=$isValidNextDay, time=$isWithinNextWindow")
                // This shouldn't happen, but if it does, don't schedule it
                return
            }
            
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
    fun alarmStateValidator(): AlarmStateValidator
    fun alarmStateRecoveryManager(): AlarmStateRecoveryManager
    fun appLogger(): com.lettingin.intervalAlarm.util.AppLogger
}
