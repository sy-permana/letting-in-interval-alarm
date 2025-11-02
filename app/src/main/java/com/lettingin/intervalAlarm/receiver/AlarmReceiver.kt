package com.lettingin.intervalAlarm.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.lettingin.intervalAlarm.LettingInApplication
import com.lettingin.intervalAlarm.data.repository.AlarmRepository
import com.lettingin.intervalAlarm.data.repository.AlarmStateRepository
import com.lettingin.intervalAlarm.data.repository.StatisticsRepository
import com.lettingin.intervalAlarm.domain.scheduler.AlarmScheduler
import com.lettingin.intervalAlarm.domain.scheduler.AlarmSchedulerImpl
import com.lettingin.intervalAlarm.service.AlarmNotificationService
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import javax.inject.Inject

class AlarmReceiver : BroadcastReceiver() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    companion object {
        private const val TAG = "AlarmReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "onReceive: action=${intent.action}")

        val alarmId = intent.getLongExtra(AlarmSchedulerImpl.EXTRA_ALARM_ID, -1L)
        if (alarmId == -1L) {
            Log.e(TAG, "Invalid alarm ID")
            return
        }

        val isResume = intent.getBooleanExtra(AlarmSchedulerImpl.EXTRA_IS_RESUME, false)

        // Get dependencies from Hilt
        val appContext = context.applicationContext as LettingInApplication
        val entryPoint = EntryPointAccessors.fromApplication(
            appContext,
            AlarmReceiverEntryPoint::class.java
        )

        // Use goAsync to handle async operations
        val pendingResult = goAsync()
        
        val appLogger = entryPoint.appLogger()

        scope.launch {
            try {
                if (isResume) {
                    appLogger.i(com.lettingin.intervalAlarm.util.AppLogger.CATEGORY_ALARM, TAG,
                        "Resume alarm triggered: id=$alarmId")
                    handleResumeAlarm(alarmId, entryPoint.alarmScheduler())
                } else {
                    appLogger.i(com.lettingin.intervalAlarm.util.AppLogger.CATEGORY_ALARM, TAG,
                        "Alarm ring triggered: id=$alarmId")
                    handleAlarmRing(
                        context,
                        alarmId,
                        entryPoint.alarmRepository(),
                        entryPoint.alarmStateRepository(),
                        entryPoint.statisticsRepository(),
                        entryPoint.alarmScheduler(),
                        appLogger
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error handling alarm", e)
                appLogger.e(com.lettingin.intervalAlarm.util.AppLogger.CATEGORY_ERROR, TAG,
                    "Error handling alarm: id=$alarmId", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private suspend fun handleResumeAlarm(alarmId: Long, alarmScheduler: AlarmScheduler) {
        Log.d(TAG, "handleResumeAlarm: alarmId=$alarmId")
        alarmScheduler.resumeAlarm(alarmId)
    }

    private suspend fun handleAlarmRing(
        context: Context,
        alarmId: Long,
        alarmRepository: AlarmRepository,
        alarmStateRepository: AlarmStateRepository,
        statisticsRepository: StatisticsRepository,
        alarmScheduler: AlarmScheduler,
        appLogger: com.lettingin.intervalAlarm.util.AppLogger
    ) {
        Log.d(TAG, "handleAlarmRing: alarmId=$alarmId")

        // Get alarm details
        val alarm = alarmRepository.getAlarmById(alarmId).firstOrNull()
        if (alarm == null) {
            Log.e(TAG, "Alarm not found: $alarmId")
            appLogger.e(com.lettingin.intervalAlarm.util.AppLogger.CATEGORY_ERROR, TAG,
                "Alarm not found in database: id=$alarmId")
            return
        }

        // Check if this is a test alarm
        val isTestAlarm = alarm.label.startsWith("[TEST]")
        Log.d(TAG, "handleAlarmRing: isTestAlarm=$isTestAlarm, label=${alarm.label}")

        if (!alarm.isActive) {
            Log.w(TAG, "Alarm is not active: $alarmId")
            appLogger.w(com.lettingin.intervalAlarm.util.AppLogger.CATEGORY_ALARM, TAG,
                "Attempted to ring inactive alarm: id=$alarmId")
            return
        }
        
        appLogger.logAlarmRing(alarmId, alarm.label)

        // Get current alarm state
        val alarmState = alarmStateRepository.getAlarmStateSync(alarmId)
        if (alarmState?.isPaused == true) {
            Log.w(TAG, "Alarm is paused: $alarmId")
            return
        }

        if (alarmState?.isStoppedForDay == true) {
            Log.w(TAG, "Alarm is stopped for day: $alarmId")
            return
        }

        // Validate time - check if we're still within the alarm window
        val now = LocalDateTime.now()
        val currentTime = now.toLocalTime()
        
        Log.d(TAG, "handleAlarmRing: currentTime=$currentTime, startTime=${alarm.startTime}, endTime=${alarm.endTime}")
        
        // Check for time changes
        if (alarmState?.lastRingTime != null) {
            val lastRingDateTime = LocalDateTime.ofInstant(
                java.time.Instant.ofEpochMilli(alarmState.lastRingTime),
                ZoneId.systemDefault()
            )
            
            // Detect significant time changes (more than 5 minutes difference from expected)
            val expectedNextRing = lastRingDateTime.plusMinutes(alarm.intervalMinutes.toLong())
            val timeDifference = java.time.Duration.between(expectedNextRing, now).abs().toMinutes()
            
            if (timeDifference > 5) {
                Log.w(TAG, "Time change detected: expected=$expectedNextRing, actual=$now")
                // Continue with the alarm but validate end time
            }
        }

        // Validate we haven't exceeded end time (skip for test alarms)
        if (!isTestAlarm && currentTime.isAfter(alarm.endTime)) {
            Log.w(TAG, "Current time ($currentTime) exceeds alarm end time (${alarm.endTime})")
            appLogger.w(com.lettingin.intervalAlarm.util.AppLogger.CATEGORY_ALARM, TAG,
                "Alarm ring skipped - outside time window: id=$alarmId")
            // Don't ring, but schedule for next day if applicable
            scheduleNextValidRing(alarmId, alarm, alarmRepository, alarmStateRepository, alarmScheduler)
            return
        }

        Log.d(TAG, "handleAlarmRing: Time validation passed, proceeding with alarm ring")

        // Update statistics (skip for test alarms)
        if (!isTestAlarm) {
            updateStatistics(alarmId, statisticsRepository)
        }

        // Update alarm state
        val currentTimeMillis = System.currentTimeMillis()
        val updatedState = alarmState?.copy(
            lastRingTime = currentTimeMillis,
            todayRingCount = alarmState.todayRingCount + 1
        ) ?: com.lettingin.intervalAlarm.data.model.AlarmState(
            alarmId = alarmId,
            lastRingTime = currentTimeMillis,
            nextScheduledRingTime = null,
            isPaused = false,
            pauseUntilTime = null,
            isStoppedForDay = false,
            currentDayStartTime = currentTimeMillis,
            todayRingCount = 1,
            todayUserDismissCount = 0,
            todayAutoDismissCount = 0
        )
        alarmStateRepository.updateAlarmState(updatedState)

        Log.d(TAG, "handleAlarmRing: Starting notification service")
        // Start notification service
        startNotificationService(context, alarmId)

        // Schedule next ring (skip for test alarms)
        if (!isTestAlarm) {
            scheduleNextValidRing(alarmId, alarm, alarmRepository, alarmStateRepository, alarmScheduler)
        } else {
            Log.d(TAG, "handleAlarmRing: Test alarm - skipping next ring scheduling")
        }
    }

    private suspend fun scheduleNextValidRing(
        alarmId: Long,
        alarm: com.lettingin.intervalAlarm.data.model.IntervalAlarm,
        alarmRepository: AlarmRepository,
        alarmStateRepository: AlarmStateRepository,
        alarmScheduler: AlarmScheduler
    ) {
        val currentTimeMillis = System.currentTimeMillis()
        val nextRingTime = (alarmScheduler as? AlarmSchedulerImpl)?.calculateNextRingTime(
            alarm,
            currentTimeMillis
        )

        if (nextRingTime != null) {
            Log.d(TAG, "Scheduling next ring at $nextRingTime")
            alarmScheduler.scheduleNextRing(alarmId, nextRingTime)
            
            // Update state with next ring time
            val currentState = alarmStateRepository.getAlarmStateSync(alarmId)
            currentState?.let {
                alarmStateRepository.updateAlarmState(it.copy(nextScheduledRingTime = nextRingTime))
            }
        } else {
            Log.d(TAG, "No more valid ring times for alarm $alarmId")
            
            // If one-cycle mode, deactivate the alarm
            if (!alarm.isRepeatable) {
                alarmRepository.deactivateAlarm(alarmId)
            }
        }
    }

    private suspend fun updateStatistics(alarmId: Long, statisticsRepository: StatisticsRepository) {
        val today = LocalDate.now()
        val todayStatistics = statisticsRepository.getTodayStatistics(alarmId)

        if (todayStatistics != null) {
            // Update existing statistics
            val updated = todayStatistics.copy(
                totalRings = todayStatistics.totalRings + 1
            )
            statisticsRepository.updateStatistics(updated)
        } else {
            // Create new statistics for today
            val newStatistics = com.lettingin.intervalAlarm.data.model.AlarmCycleStatistics(
                alarmId = alarmId,
                cycleDate = today,
                totalRings = 1,
                userDismissals = 0,
                autoDismissals = 0,
                cycleStartTime = System.currentTimeMillis(),
                cycleEndTime = null
            )
            statisticsRepository.insertStatistics(newStatistics)
        }
    }

    private fun startNotificationService(context: Context, alarmId: Long) {
        Log.d(TAG, "startNotificationService: alarmId=$alarmId")
        
        val serviceIntent = Intent(context, AlarmNotificationService::class.java).apply {
            putExtra(AlarmSchedulerImpl.EXTRA_ALARM_ID, alarmId)
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Log.d(TAG, "Starting foreground service")
                context.startForegroundService(serviceIntent)
            } else {
                Log.d(TAG, "Starting service")
                context.startService(serviceIntent)
            }
            Log.d(TAG, "Service start command sent")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start notification service", e)
        }
    }
}


@dagger.hilt.EntryPoint
@dagger.hilt.InstallIn(dagger.hilt.components.SingletonComponent::class)
interface AlarmReceiverEntryPoint {
    fun alarmRepository(): AlarmRepository
    fun alarmStateRepository(): AlarmStateRepository
    fun statisticsRepository(): StatisticsRepository
    fun alarmScheduler(): AlarmScheduler
    fun appLogger(): com.lettingin.intervalAlarm.util.AppLogger
}
