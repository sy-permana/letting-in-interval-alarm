package com.lettingin.intervalAlarm.util

import com.lettingin.intervalAlarm.data.model.IntervalAlarm
import java.time.DayOfWeek
import java.time.LocalTime
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Data validation utility for alarm configurations.
 * Validates alarm data integrity and business rules.
 */
@Singleton
class DataValidator @Inject constructor(
    private val appLogger: AppLogger
) {
    
    companion object {
        private const val TAG = "DataValidator"
        const val MIN_INTERVAL_MINUTES = 5
        const val MAX_LABEL_LENGTH = 60
        const val MAX_ALARM_COUNT = 10
    }
    
    /**
     * Validation result
     */
    data class ValidationResult(
        val isValid: Boolean,
        val errors: List<String> = emptyList()
    ) {
        fun getErrorMessage(): String = errors.joinToString("\n")
    }
    
    /**
     * Validate an alarm configuration
     */
    fun validateAlarm(alarm: IntervalAlarm): ValidationResult {
        val errors = mutableListOf<String>()
        
        // Validate label
        if (alarm.label.length > MAX_LABEL_LENGTH) {
            errors.add("Label must be $MAX_LABEL_LENGTH characters or less")
        }
        
        // Validate times
        if (alarm.startTime >= alarm.endTime) {
            errors.add("Start time must be before end time")
        }
        
        // Validate interval
        val timeRangeMinutes = calculateTimeRangeMinutes(alarm.startTime, alarm.endTime)
        if (alarm.intervalMinutes < MIN_INTERVAL_MINUTES) {
            errors.add("Interval must be at least $MIN_INTERVAL_MINUTES minutes")
        }
        if (alarm.intervalMinutes > timeRangeMinutes) {
            errors.add("Interval cannot exceed time range ($timeRangeMinutes minutes)")
        }
        
        // Validate selected days
        if (alarm.selectedDays.isEmpty()) {
            errors.add("At least one day must be selected")
        }
        
        // Validate ringtone URI
        if (alarm.ringtoneUri.isBlank()) {
            errors.add("Ringtone must be selected")
        }
        
        if (errors.isNotEmpty()) {
            appLogger.w(AppLogger.CATEGORY_DATABASE, TAG,
                "Alarm validation failed: ${errors.joinToString(", ")}")
        }
        
        return ValidationResult(errors.isEmpty(), errors)
    }
    
    /**
     * Validate pause duration
     */
    fun validatePauseDuration(
        alarm: IntervalAlarm,
        pauseDurationMillis: Long,
        currentTimeMillis: Long
    ): ValidationResult {
        val errors = mutableListOf<String>()
        
        val currentDateTime = java.time.LocalDateTime.ofInstant(
            java.time.Instant.ofEpochMilli(currentTimeMillis),
            java.time.ZoneId.systemDefault()
        )
        
        val pauseEndDateTime = java.time.LocalDateTime.ofInstant(
            java.time.Instant.ofEpochMilli(currentTimeMillis + pauseDurationMillis),
            java.time.ZoneId.systemDefault()
        )
        
        // Check if pause is on the same day
        if (pauseEndDateTime.toLocalDate() == currentDateTime.toLocalDate()) {
            // Validate pause end time plus interval doesn't exceed end time
            val pauseEndTimePlusInterval = pauseEndDateTime.toLocalTime()
                .plusMinutes(alarm.intervalMinutes.toLong())
            
            if (pauseEndDateTime.toLocalTime().isAfter(alarm.endTime)) {
                errors.add("Pause duration would exceed alarm end time")
            }
            
            if (pauseEndTimePlusInterval.isAfter(alarm.endTime) && 
                pauseEndTimePlusInterval != alarm.endTime) {
                errors.add("Pause end time plus interval would exceed alarm end time")
            }
        } else {
            // Pause extends to next day - validate it's a selected day
            if (!alarm.selectedDays.contains(pauseEndDateTime.dayOfWeek)) {
                errors.add("Pause duration extends to a non-selected day")
            }
        }
        
        return ValidationResult(errors.isEmpty(), errors)
    }
    
    /**
     * Check if alarm data is corrupted
     */
    fun isAlarmCorrupted(alarm: IntervalAlarm): Boolean {
        try {
            // Check for null or invalid values
            if (alarm.id < 0) return true
            if (alarm.intervalMinutes <= 0) return true
            if (alarm.selectedDays.isEmpty()) return true
            
            // Check for impossible time ranges
            val timeRange = calculateTimeRangeMinutes(alarm.startTime, alarm.endTime)
            if (timeRange <= 0) return true
            
            // Check for invalid interval
            if (alarm.intervalMinutes > timeRange) return true
            
            return false
        } catch (e: Exception) {
            appLogger.e(AppLogger.CATEGORY_ERROR, TAG,
                "Error checking alarm corruption: id=${alarm.id}", e)
            return true
        }
    }
    
    /**
     * Sanitize alarm data
     */
    fun sanitizeAlarm(alarm: IntervalAlarm): IntervalAlarm {
        return alarm.copy(
            label = alarm.label.take(MAX_LABEL_LENGTH).trim(),
            intervalMinutes = alarm.intervalMinutes.coerceAtLeast(MIN_INTERVAL_MINUTES),
            selectedDays = if (alarm.selectedDays.isEmpty()) {
                setOf(DayOfWeek.MONDAY)
            } else {
                alarm.selectedDays
            }
        )
    }
    
    /**
     * Calculate time range in minutes
     */
    private fun calculateTimeRangeMinutes(startTime: LocalTime, endTime: LocalTime): Int {
        val start = startTime.toSecondOfDay()
        val end = endTime.toSecondOfDay()
        return ((end - start) / 60).coerceAtLeast(0)
    }
}
