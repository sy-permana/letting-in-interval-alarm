package com.lettingin.intervalAlarm.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

@Entity(tableName = "alarm_state")
data class AlarmState(
    @PrimaryKey
    val alarmId: Long,
    val lastRingTime: Long? = null,
    val nextScheduledRingTime: Long? = null,
    val isPaused: Boolean = false,
    val pauseUntilTime: Long? = null,
    val isStoppedForDay: Boolean = false,
    val currentDayStartTime: Long? = null,
    val todayRingCount: Int = 0,
    val todayUserDismissCount: Int = 0,
    val todayAutoDismissCount: Int = 0
) {
    /**
     * Validates if the next ring time is stale (in the past).
     */
    fun isNextRingTimeStale(): Boolean {
        val nextRing = nextScheduledRingTime ?: return false
        return nextRing < System.currentTimeMillis()
    }
    
    /**
     * Checks if state is consistent with the alarm configuration.
     * Validates that next ring time is within alarm's time window.
     */
    fun isConsistent(alarm: IntervalAlarm): Boolean {
        // If no next ring time, state is consistent (paused or stopped)
        if (nextScheduledRingTime == null) return true
        
        val nextRingDateTime = LocalDateTime.ofInstant(
            Instant.ofEpochMilli(nextScheduledRingTime),
            ZoneId.systemDefault()
        )
        
        // Check if day is selected
        val isValidDay = alarm.selectedDays.contains(nextRingDateTime.dayOfWeek)
        
        // Check if time is within window
        val nextRingTime = nextRingDateTime.toLocalTime()
        val isWithinWindow = nextRingTime >= alarm.startTime && 
                            nextRingTime <= alarm.endTime
        
        return isValidDay && isWithinWindow
    }
}
