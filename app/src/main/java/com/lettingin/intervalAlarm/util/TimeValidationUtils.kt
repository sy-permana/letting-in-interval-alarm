package com.lettingin.intervalAlarm.util

import com.lettingin.intervalAlarm.data.model.IntervalAlarm
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset

/**
 * Utility object for time-related validation operations.
 * Provides helper methods for validating time windows, detecting time zone changes,
 * and system time changes.
 */
object TimeValidationUtils {
    
    /**
     * Validates if a timestamp falls within the alarm's configured time window.
     * 
     * @param timestamp The timestamp to validate (in milliseconds)
     * @param alarm The alarm configuration containing start/end times and selected days
     * @return true if the timestamp is within the alarm's time window, false otherwise
     */
    fun isTimestampInTimeWindow(timestamp: Long, alarm: IntervalAlarm): Boolean {
        val dateTime = LocalDateTime.ofInstant(
            Instant.ofEpochMilli(timestamp),
            ZoneId.systemDefault()
        )
        
        // Check if day is selected
        if (!alarm.selectedDays.contains(dateTime.dayOfWeek)) {
            return false
        }
        
        // Check if time is within window
        val time = dateTime.toLocalTime()
        return time >= alarm.startTime && time <= alarm.endTime
    }
    
    /**
     * Validates if a time falls within the specified time window.
     * 
     * @param time The time to validate
     * @param startTime The start of the time window
     * @param endTime The end of the time window
     * @return true if the time is within the window, false otherwise
     */
    fun isTimeInWindow(time: LocalTime, startTime: LocalTime, endTime: LocalTime): Boolean {
        return time >= startTime && time <= endTime
    }
    
    /**
     * Validates if a day of week is in the selected days set.
     * 
     * @param dayOfWeek The day to check
     * @param selectedDays The set of selected days
     * @return true if the day is selected, false otherwise
     */
    fun isDaySelected(dayOfWeek: DayOfWeek, selectedDays: Set<DayOfWeek>): Boolean {
        return selectedDays.contains(dayOfWeek)
    }
    
    /**
     * Detects if the system time zone has changed by comparing stored and current zone IDs.
     * 
     * @param storedZoneId The previously stored zone ID (e.g., "America/New_York")
     * @return true if the time zone has changed, false otherwise
     */
    fun hasTimeZoneChanged(storedZoneId: String?): Boolean {
        if (storedZoneId == null) return false
        val currentZoneId = ZoneId.systemDefault().id
        return storedZoneId != currentZoneId
    }
    
    /**
     * Detects if the system time has changed significantly (more than expected drift).
     * Compares a stored timestamp with the current time to detect manual time changes.
     * 
     * @param storedTimestamp The previously stored timestamp (in milliseconds)
     * @param expectedElapsedMs The expected elapsed time since the stored timestamp
     * @param toleranceMs The tolerance for time drift (default 5 minutes)
     * @return true if system time appears to have been manually changed, false otherwise
     */
    fun hasSystemTimeChanged(
        storedTimestamp: Long,
        expectedElapsedMs: Long,
        toleranceMs: Long = 5 * 60 * 1000 // 5 minutes default
    ): Boolean {
        val currentTime = System.currentTimeMillis()
        val actualElapsed = currentTime - storedTimestamp
        val difference = kotlin.math.abs(actualElapsed - expectedElapsedMs)
        return difference > toleranceMs
    }
    
    /**
     * Gets the current system time zone ID.
     * 
     * @return The current time zone ID (e.g., "America/New_York")
     */
    fun getCurrentTimeZoneId(): String {
        return ZoneId.systemDefault().id
    }
    
    /**
     * Gets the current time zone offset in milliseconds.
     * 
     * @return The offset from UTC in milliseconds
     */
    fun getCurrentTimeZoneOffsetMs(): Long {
        val now = Instant.now()
        val offset = ZoneId.systemDefault().rules.getOffset(now)
        return offset.totalSeconds * 1000L
    }
    
    /**
     * Validates if a timestamp is in the future.
     * 
     * @param timestamp The timestamp to validate (in milliseconds)
     * @param bufferMs Optional buffer time to consider (default 0)
     * @return true if the timestamp is in the future (plus buffer), false otherwise
     */
    fun isTimestampInFuture(timestamp: Long, bufferMs: Long = 0): Boolean {
        return timestamp > System.currentTimeMillis() + bufferMs
    }
    
    /**
     * Validates if a timestamp is in the past.
     * 
     * @param timestamp The timestamp to validate (in milliseconds)
     * @param bufferMs Optional buffer time to consider (default 0)
     * @return true if the timestamp is in the past (minus buffer), false otherwise
     */
    fun isTimestampInPast(timestamp: Long, bufferMs: Long = 0): Boolean {
        return timestamp < System.currentTimeMillis() - bufferMs
    }
    
    /**
     * Calculates the time difference between two timestamps in milliseconds.
     * 
     * @param timestamp1 The first timestamp
     * @param timestamp2 The second timestamp
     * @return The absolute difference in milliseconds
     */
    fun getTimeDifferenceMs(timestamp1: Long, timestamp2: Long): Long {
        return kotlin.math.abs(timestamp1 - timestamp2)
    }
    
    /**
     * Validates if two timestamps are within a specified tolerance of each other.
     * 
     * @param timestamp1 The first timestamp
     * @param timestamp2 The second timestamp
     * @param toleranceMs The tolerance in milliseconds
     * @return true if timestamps are within tolerance, false otherwise
     */
    fun areTimestampsClose(timestamp1: Long, timestamp2: Long, toleranceMs: Long): Boolean {
        return getTimeDifferenceMs(timestamp1, timestamp2) <= toleranceMs
    }
}
