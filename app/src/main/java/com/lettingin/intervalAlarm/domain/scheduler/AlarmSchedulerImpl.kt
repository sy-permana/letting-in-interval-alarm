package com.lettingin.intervalAlarm.domain.scheduler

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.lettingin.intervalAlarm.data.model.AlarmState
import com.lettingin.intervalAlarm.data.model.IntervalAlarm
import com.lettingin.intervalAlarm.data.repository.AlarmRepository
import com.lettingin.intervalAlarm.data.repository.AlarmStateRepository
import com.lettingin.intervalAlarm.receiver.AlarmReceiver
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.firstOrNull
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AlarmSchedulerImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val alarmRepository: AlarmRepository,
    private val alarmStateRepository: AlarmStateRepository,
    private val appLogger: com.lettingin.intervalAlarm.util.AppLogger
) : AlarmScheduler {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    companion object {
        private const val TAG = "AlarmSchedulerImpl"
        const val EXTRA_ALARM_ID = "alarm_id"
        const val EXTRA_IS_RESUME = "is_resume"
    }

    override suspend fun scheduleAlarm(alarm: IntervalAlarm) {
        Log.d(TAG, "scheduleAlarm: alarmId=${alarm.id}")
        appLogger.i(com.lettingin.intervalAlarm.util.AppLogger.CATEGORY_SCHEDULING, TAG, 
            "Scheduling alarm: id=${alarm.id}, label='${alarm.label}'")
        
        // Validate alarm configuration
        if (alarm.selectedDays.isEmpty()) {
            appLogger.e(com.lettingin.intervalAlarm.util.AppLogger.CATEGORY_ERROR, TAG,
                "Cannot schedule alarm with no selected days: id=${alarm.id}")
            throw IllegalArgumentException("Alarm must have at least one selected day")
        }
        
        if (alarm.intervalMinutes <= 0) {
            appLogger.e(com.lettingin.intervalAlarm.util.AppLogger.CATEGORY_ERROR, TAG,
                "Cannot schedule alarm with invalid interval: id=${alarm.id}, interval=${alarm.intervalMinutes}")
            throw IllegalArgumentException("Alarm interval must be positive")
        }
        
        // Calculate the next ring time
        val nextRingTime = calculateNextRingTime(alarm, System.currentTimeMillis())
        
        if (nextRingTime == null) {
            Log.w(TAG, "No valid next ring time found for alarm ${alarm.id}")
            appLogger.w(com.lettingin.intervalAlarm.util.AppLogger.CATEGORY_SCHEDULING, TAG,
                "No valid next ring time found for alarm ${alarm.id}")
            return
        }
        
        // Validate next ring time is in the future
        if (nextRingTime <= System.currentTimeMillis()) {
            appLogger.w(com.lettingin.intervalAlarm.util.AppLogger.CATEGORY_SCHEDULING, TAG,
                "Calculated next ring time is not in the future: id=${alarm.id}, time=$nextRingTime")
            return
        }
        
        // Update alarm state
        val alarmState = AlarmState(
            alarmId = alarm.id,
            lastRingTime = null,
            nextScheduledRingTime = nextRingTime,
            isPaused = false,
            pauseUntilTime = null,
            isStoppedForDay = false,
            currentDayStartTime = System.currentTimeMillis(),
            todayRingCount = 0,
            todayUserDismissCount = 0,
            todayAutoDismissCount = 0
        )
        alarmStateRepository.updateAlarmState(alarmState)
        
        // Schedule the alarm
        scheduleNextRing(alarm.id, nextRingTime)
        appLogger.logAlarmScheduled(alarm.id, nextRingTime)
    }

    override suspend fun scheduleNextRing(alarmId: Long, nextRingTime: Long) {
        Log.d(TAG, "scheduleNextRing: alarmId=$alarmId, nextRingTime=$nextRingTime")
        
        // Validate inputs
        if (alarmId <= 0) {
            appLogger.e(com.lettingin.intervalAlarm.util.AppLogger.CATEGORY_ERROR, TAG,
                "Invalid alarm ID: $alarmId")
            throw IllegalArgumentException("Alarm ID must be positive")
        }
        
        if (nextRingTime <= 0) {
            appLogger.e(com.lettingin.intervalAlarm.util.AppLogger.CATEGORY_ERROR, TAG,
                "Invalid next ring time: $nextRingTime for alarm $alarmId")
            throw IllegalArgumentException("Next ring time must be positive")
        }
        
        // Validate next ring time is in the future
        val currentTime = System.currentTimeMillis()
        if (nextRingTime <= currentTime) {
            appLogger.w(com.lettingin.intervalAlarm.util.AppLogger.CATEGORY_SCHEDULING, TAG,
                "Next ring time is not in the future: id=$alarmId, time=$nextRingTime, current=$currentTime")
            // Allow small time differences (up to 1 second) due to processing delays
            if (nextRingTime < currentTime - 1000) {
                throw IllegalArgumentException("Next ring time must be in the future")
            }
        }
        
        try {
            val intent = Intent(context, AlarmReceiver::class.java).apply {
                putExtra(EXTRA_ALARM_ID, alarmId)
            }
            
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                alarmId.toInt(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            
            // Use setExactAndAllowWhileIdle for API 23+ to ensure alarms fire in Doze mode
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    nextRingTime,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    nextRingTime,
                    pendingIntent
                )
            }
            
            Log.d(TAG, "Alarm scheduled for ${LocalDateTime.ofInstant(
                java.time.Instant.ofEpochMilli(nextRingTime), 
                ZoneId.systemDefault()
            )}")
            
            appLogger.i(com.lettingin.intervalAlarm.util.AppLogger.CATEGORY_SCHEDULING, TAG,
                "Successfully scheduled alarm: id=$alarmId")
        } catch (e: SecurityException) {
            appLogger.e(com.lettingin.intervalAlarm.util.AppLogger.CATEGORY_ERROR, TAG,
                "Security exception scheduling alarm: id=$alarmId", e)
            throw com.lettingin.intervalAlarm.util.SchedulingException(
                "Permission denied to schedule exact alarm", e)
        } catch (e: Exception) {
            appLogger.e(com.lettingin.intervalAlarm.util.AppLogger.CATEGORY_ERROR, TAG,
                "Failed to schedule alarm: id=$alarmId", e)
            throw com.lettingin.intervalAlarm.util.SchedulingException(
                "Failed to schedule alarm", e)
        }
    }

    override suspend fun cancelAlarm(alarmId: Long) {
        Log.d(TAG, "cancelAlarm: alarmId=$alarmId")
        
        try {
            val intent = Intent(context, AlarmReceiver::class.java).apply {
                putExtra(EXTRA_ALARM_ID, alarmId)
            }
            
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                alarmId.toInt(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
            
            // Clean up alarm state
            alarmStateRepository.deleteAlarmState(alarmId)
            
            appLogger.i(com.lettingin.intervalAlarm.util.AppLogger.CATEGORY_SCHEDULING, TAG,
                "Successfully cancelled alarm: id=$alarmId")
        } catch (e: Exception) {
            appLogger.e(com.lettingin.intervalAlarm.util.AppLogger.CATEGORY_ERROR, TAG,
                "Error cancelling alarm: id=$alarmId", e)
            // Don't throw - cancellation should be best-effort
        }
    }

    override suspend fun pauseAlarm(alarmId: Long, pauseDurationMillis: Long) {
        Log.d(TAG, "pauseAlarm: alarmId=$alarmId, duration=$pauseDurationMillis")
        
        // Validate inputs
        if (pauseDurationMillis <= 0) {
            appLogger.e(com.lettingin.intervalAlarm.util.AppLogger.CATEGORY_ERROR, TAG,
                "Invalid pause duration: $pauseDurationMillis for alarm $alarmId")
            throw IllegalArgumentException("Pause duration must be positive")
        }
        
        appLogger.logAlarmPaused(alarmId, pauseDurationMillis / 60000)
        
        val alarm = alarmRepository.getAlarmById(alarmId).firstOrNull()
        if (alarm == null) {
            Log.e(TAG, "Alarm not found: $alarmId")
            appLogger.e(com.lettingin.intervalAlarm.util.AppLogger.CATEGORY_ERROR, TAG,
                "Alarm not found for pause: $alarmId")
            return
        }
        
        val currentTime = System.currentTimeMillis()
        val pauseUntilTime = currentTime + pauseDurationMillis
        
        // Validate that pause doesn't exceed end time
        val pauseDateTime = LocalDateTime.ofInstant(
            java.time.Instant.ofEpochMilli(pauseUntilTime),
            ZoneId.systemDefault()
        )
        
        val currentDateTime = LocalDateTime.ofInstant(
            java.time.Instant.ofEpochMilli(currentTime),
            ZoneId.systemDefault()
        )
        
        // Check if pause is on the same day
        if (pauseDateTime.toLocalDate() == currentDateTime.toLocalDate()) {
            // Validate pause end time plus interval doesn't exceed end time
            val pauseEndTimePlusInterval = pauseDateTime.toLocalTime().plusMinutes(alarm.intervalMinutes.toLong())
            
            if (pauseDateTime.toLocalTime().isAfter(alarm.endTime)) {
                Log.w(TAG, "Pause would exceed end time, not pausing")
                throw IllegalArgumentException("Pause duration would exceed alarm end time")
            }
            
            if (pauseEndTimePlusInterval.isAfter(alarm.endTime) && pauseEndTimePlusInterval != alarm.endTime) {
                Log.w(TAG, "Pause end time plus interval would exceed alarm end time")
                throw IllegalArgumentException("Pause end time plus interval would exceed alarm end time")
            }
        } else {
            // Pause extends to next day - validate it's a selected day
            if (!alarm.selectedDays.contains(pauseDateTime.dayOfWeek)) {
                Log.w(TAG, "Pause extends to non-selected day")
                throw IllegalArgumentException("Pause duration extends to a non-selected day")
            }
        }
        
        // Cancel current alarm
        cancelAlarmOnly(alarmId)
        
        // Update state to paused
        val currentState = alarmStateRepository.getAlarmStateSync(alarmId)
        val updatedState = currentState?.copy(
            isPaused = true,
            pauseUntilTime = pauseUntilTime,
            nextScheduledRingTime = null
        ) ?: AlarmState(
            alarmId = alarmId,
            lastRingTime = currentState?.lastRingTime,
            nextScheduledRingTime = null,
            isPaused = true,
            pauseUntilTime = pauseUntilTime,
            isStoppedForDay = false,
            currentDayStartTime = currentState?.currentDayStartTime ?: currentTime,
            todayRingCount = currentState?.todayRingCount ?: 0,
            todayUserDismissCount = currentState?.todayUserDismissCount ?: 0,
            todayAutoDismissCount = currentState?.todayAutoDismissCount ?: 0
        )
        alarmStateRepository.updateAlarmState(updatedState)
        
        // Schedule resume
        scheduleResume(alarmId, pauseUntilTime)
        
        Log.d(TAG, "Alarm paused until ${pauseDateTime}")
    }

    override suspend fun resumeAlarm(alarmId: Long) {
        Log.d(TAG, "resumeAlarm: alarmId=$alarmId")
        appLogger.logAlarmResumed(alarmId)
        
        val alarm = alarmRepository.getAlarmById(alarmId).firstOrNull()
        if (alarm == null) {
            Log.e(TAG, "Alarm not found: $alarmId")
            appLogger.e(com.lettingin.intervalAlarm.util.AppLogger.CATEGORY_ERROR, TAG,
                "Alarm not found for resume: $alarmId")
            return
        }
        
        val currentTime = System.currentTimeMillis()
        val currentDateTime = LocalDateTime.ofInstant(
            java.time.Instant.ofEpochMilli(currentTime),
            ZoneId.systemDefault()
        )
        
        // Get current state
        val currentState = alarmStateRepository.getAlarmStateSync(alarmId)
        if (currentState == null) {
            Log.e(TAG, "Alarm state not found: $alarmId")
            appLogger.e(com.lettingin.intervalAlarm.util.AppLogger.CATEGORY_ERROR, TAG,
                "Alarm state not found for resume: $alarmId")
            return
        }
        
        // Clear pause state
        val updatedState = currentState.copy(
            isPaused = false,
            pauseUntilTime = null
        )
        
        alarmStateRepository.updateAlarmState(updatedState)
        
        // Calculate next ring time from current time
        val nextRingTime = calculateNextRingTime(alarm, currentTime)
        
        if (nextRingTime != null) {
            // Validate next ring time doesn't exceed end time for today
            val nextRingDateTime = LocalDateTime.ofInstant(
                java.time.Instant.ofEpochMilli(nextRingTime),
                ZoneId.systemDefault()
            )
            
            if (nextRingDateTime.toLocalDate() == currentDateTime.toLocalDate()) {
                // Same day - check if within time window
                if (nextRingDateTime.toLocalTime().isAfter(alarm.endTime) && 
                    nextRingDateTime.toLocalTime() != alarm.endTime) {
                    Log.w(TAG, "Next ring time exceeds end time, scheduling for next valid day")
                    // Will be scheduled for next valid day by calculateNextRingTime
                }
            }
            
            scheduleNextRing(alarmId, nextRingTime)
            alarmStateRepository.updateAlarmState(updatedState.copy(nextScheduledRingTime = nextRingTime))
            Log.d(TAG, "Alarm resumed, next ring at ${nextRingDateTime}")
        } else {
            Log.w(TAG, "No valid next ring time after resume")
            
            // If one-cycle mode and no more valid times, deactivate
            if (!alarm.isRepeatable) {
                alarmRepository.deactivateAlarm(alarmId)
                Log.d(TAG, "One-cycle alarm completed, deactivated")
            }
        }
    }

    override suspend fun stopForDay(alarmId: Long) {
        Log.d(TAG, "stopForDay: alarmId=$alarmId")
        appLogger.i(com.lettingin.intervalAlarm.util.AppLogger.CATEGORY_ALARM, TAG,
            "Stopping alarm for day: id=$alarmId")
        
        // Cancel current alarm
        cancelAlarmOnly(alarmId)
        
        // Update state to stopped for day
        val currentState = alarmStateRepository.getAlarmStateSync(alarmId)
        if (currentState == null) {
            appLogger.e(com.lettingin.intervalAlarm.util.AppLogger.CATEGORY_ERROR, TAG,
                "Alarm state not found for stopForDay: $alarmId")
            return
        }
        
        val updatedState = currentState.copy(
            isStoppedForDay = true,
            nextScheduledRingTime = null
        )
        
        alarmStateRepository.updateAlarmState(updatedState)
        
        // Schedule for next day
        val alarm = alarmRepository.getAlarmById(alarmId).firstOrNull()
        if (alarm == null) {
            appLogger.e(com.lettingin.intervalAlarm.util.AppLogger.CATEGORY_ERROR, TAG,
                "Alarm not found for stopForDay: $alarmId")
            return
        }
        
        scheduleNextDay(alarm)
    }

    private suspend fun scheduleResume(alarmId: Long, resumeTime: Long) {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra(EXTRA_ALARM_ID, alarmId)
            putExtra(EXTRA_IS_RESUME, true)
        }
        
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            alarmId.toInt() + 100000, // Different request code for resume
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                resumeTime,
                pendingIntent
            )
        } else {
            alarmManager.setExact(
                AlarmManager.RTC_WAKEUP,
                resumeTime,
                pendingIntent
            )
        }
    }

    private fun cancelAlarmOnly(alarmId: Long) {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra(EXTRA_ALARM_ID, alarmId)
        }
        
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            alarmId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()
    }

    private suspend fun scheduleNextDay(alarm: IntervalAlarm) {
        val now = LocalDateTime.now()
        val tomorrow = now.plusDays(1)
        
        // Find next valid day
        var nextDate = tomorrow.toLocalDate()
        var daysChecked = 0
        
        while (daysChecked < 7) {
            if (alarm.selectedDays.contains(nextDate.dayOfWeek)) {
                // Schedule for start time on this day
                val nextRingDateTime = LocalDateTime.of(nextDate, alarm.startTime)
                val nextRingTime = nextRingDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                
                val currentState = alarmStateRepository.getAlarmStateSync(alarm.id)
                if (currentState == null) {
                    appLogger.e(com.lettingin.intervalAlarm.util.AppLogger.CATEGORY_ERROR, TAG,
                        "Alarm state not found for scheduleNextDay: ${alarm.id}")
                    return
                }
                
                val updatedState = currentState.copy(
                    isStoppedForDay = false,
                    isPaused = false,
                    pauseUntilTime = null,
                    nextScheduledRingTime = nextRingTime,
                    currentDayStartTime = nextRingTime,
                    todayRingCount = 0,
                    todayUserDismissCount = 0,
                    todayAutoDismissCount = 0
                )
                
                alarmStateRepository.updateAlarmState(updatedState)
                scheduleNextRing(alarm.id, nextRingTime)
                return
            }
            nextDate = nextDate.plusDays(1)
            daysChecked++
        }
        
        appLogger.w(com.lettingin.intervalAlarm.util.AppLogger.CATEGORY_SCHEDULING, TAG,
            "No valid next day found for alarm: ${alarm.id}")
    }

    override fun isAlarmScheduled(alarmId: Long): Boolean {
        return try {
            val intent = Intent(context, AlarmReceiver::class.java).apply {
                putExtra(EXTRA_ALARM_ID, alarmId)
            }
            
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                alarmId.toInt(),
                intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            
            val isScheduled = pendingIntent != null
            
            Log.d(TAG, "isAlarmScheduled: alarmId=$alarmId, scheduled=$isScheduled")
            appLogger.d(com.lettingin.intervalAlarm.util.AppLogger.CATEGORY_SCHEDULING, TAG,
                "Alarm scheduled check: id=$alarmId, scheduled=$isScheduled")
            
            isScheduled
        } catch (e: Exception) {
            Log.e(TAG, "Error checking if alarm is scheduled: $alarmId", e)
            appLogger.e(com.lettingin.intervalAlarm.util.AppLogger.CATEGORY_ERROR, TAG,
                "Error checking alarm schedule status: id=$alarmId", e)
            false
        }
    }
    
    override fun getScheduledTime(alarmId: Long): Long? {
        // Note: Android's AlarmManager API doesn't provide a way to retrieve the scheduled time
        // of an existing alarm. We can only check if the PendingIntent exists.
        // The actual scheduled time must be retrieved from the database (AlarmState).
        
        return if (isAlarmScheduled(alarmId)) {
            // Return a marker value indicating the alarm is scheduled
            // The actual time should be retrieved from AlarmState
            Log.d(TAG, "getScheduledTime: alarmId=$alarmId is scheduled, but time must be retrieved from database")
            appLogger.d(com.lettingin.intervalAlarm.util.AppLogger.CATEGORY_SCHEDULING, TAG,
                "Alarm is scheduled but time unavailable from AlarmManager: id=$alarmId")
            -1L // Marker value indicating scheduled but time unknown
        } else {
            Log.d(TAG, "getScheduledTime: alarmId=$alarmId is not scheduled")
            null
        }
    }

    /**
     * Calculate the next ring time for an alarm based on current time
     * Returns null if no valid ring time exists
     */
    fun calculateNextRingTime(alarm: IntervalAlarm, fromTimeMillis: Long): Long? {
        // Validate inputs
        if (fromTimeMillis <= 0) {
            appLogger.e(com.lettingin.intervalAlarm.util.AppLogger.CATEGORY_ERROR, TAG,
                "Invalid fromTimeMillis: $fromTimeMillis for alarm ${alarm.id}")
            return null
        }
        
        if (alarm.selectedDays.isEmpty()) {
            appLogger.e(com.lettingin.intervalAlarm.util.AppLogger.CATEGORY_ERROR, TAG,
                "No selected days for alarm ${alarm.id}")
            return null
        }
        
        val now = LocalDateTime.ofInstant(
            java.time.Instant.ofEpochMilli(fromTimeMillis),
            ZoneId.systemDefault()
        )
        val today = now.toLocalDate()
        val currentTime = now.toLocalTime()
        
        // Check if today is a valid day
        if (alarm.selectedDays.contains(today.dayOfWeek)) {
            // Check if we're within the time window
            if (currentTime.isBefore(alarm.startTime)) {
                // Before start time, schedule for start time today
                val nextRingDateTime = LocalDateTime.of(today, alarm.startTime)
                return nextRingDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            } else if (currentTime.isBefore(alarm.endTime)) {
                // Within time window, calculate next interval
                val nextRingTime = calculateNextIntervalTime(alarm, now)
                if (nextRingTime != null && nextRingTime.toLocalTime().isBefore(alarm.endTime)) {
                    return nextRingTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                } else if (nextRingTime != null && nextRingTime.toLocalTime() == alarm.endTime) {
                    // Allow final ring at end time
                    return nextRingTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                }
            }
        }
        
        // Not valid today, find next valid day
        return findNextValidDay(alarm, today)
    }

    private fun calculateNextIntervalTime(alarm: IntervalAlarm, currentDateTime: LocalDateTime): LocalDateTime? {
        val startDateTime = LocalDateTime.of(currentDateTime.toLocalDate(), alarm.startTime)
        val currentTime = currentDateTime.toLocalTime()
        
        // Calculate how many intervals have passed since start time
        val minutesSinceStart = java.time.Duration.between(alarm.startTime, currentTime).toMinutes()
        val intervalsPassed = (minutesSinceStart / alarm.intervalMinutes).toInt()
        
        // Calculate next interval time
        val nextIntervalMinutes = (intervalsPassed + 1) * alarm.intervalMinutes
        val nextRingTime = startDateTime.plusMinutes(nextIntervalMinutes.toLong())
        
        return nextRingTime
    }

    private fun findNextValidDay(alarm: IntervalAlarm, fromDate: LocalDate): Long? {
        var checkDate = fromDate.plusDays(1)
        var daysChecked = 0
        
        // For one-cycle mode, only check remaining days in the cycle
        val maxDaysToCheck = if (alarm.isRepeatable) 7 else alarm.selectedDays.size
        
        while (daysChecked < maxDaysToCheck) {
            if (alarm.selectedDays.contains(checkDate.dayOfWeek)) {
                val nextRingDateTime = LocalDateTime.of(checkDate, alarm.startTime)
                return nextRingDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            }
            checkDate = checkDate.plusDays(1)
            daysChecked++
            
            // For one-cycle mode, stop after checking all days once
            if (!alarm.isRepeatable && daysChecked >= 7) {
                break
            }
        }
        
        return null
    }
}
