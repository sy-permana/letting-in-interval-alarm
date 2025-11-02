package com.lettingin.intervalAlarm.util

import java.time.Instant
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Utility object for consistent 24-hour time formatting across the application.
 * All time displays should use this formatter to ensure HH:mm format (00:00 to 23:59).
 */
object TimeFormatter {
    private val formatter24Hour = DateTimeFormatter.ofPattern("HH:mm")
    
    /**
     * Formats LocalTime to 24-hour format string (HH:mm).
     * 
     * @param time The LocalTime to format
     * @return Formatted time string in HH:mm format (e.g., "09:00", "15:30", "23:59")
     */
    fun format24Hour(time: LocalTime): String {
        return time.format(formatter24Hour)
    }
    
    /**
     * Formats timestamp to 24-hour format string (HH:mm).
     * 
     * @param timestamp The epoch milliseconds timestamp to format
     * @return Formatted time string in HH:mm format
     */
    fun format24Hour(timestamp: Long): String {
        val instant = Instant.ofEpochMilli(timestamp)
        val localTime = LocalDateTime.ofInstant(instant, ZoneId.systemDefault()).toLocalTime()
        return format24Hour(localTime)
    }
    
    /**
     * Formats time range for display as "HH:mm - HH:mm".
     * 
     * @param startTime The start time of the range
     * @param endTime The end time of the range
     * @return Formatted time range string (e.g., "09:00 - 17:30")
     */
    fun formatTimeRange(startTime: LocalTime, endTime: LocalTime): String {
        return "${format24Hour(startTime)} - ${format24Hour(endTime)}"
    }
}
