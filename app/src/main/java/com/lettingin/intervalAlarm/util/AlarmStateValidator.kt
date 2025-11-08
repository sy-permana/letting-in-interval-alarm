package com.lettingin.intervalAlarm.util

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.lettingin.intervalAlarm.data.model.AlarmState
import com.lettingin.intervalAlarm.data.model.IntervalAlarm
import com.lettingin.intervalAlarm.receiver.AlarmReceiver
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Utility class for validating alarm state consistency.
 * Detects stale next ring times and validates alarm state against AlarmManager.
 */
@Singleton
class AlarmStateValidator @Inject constructor(
    @ApplicationContext private val context: Context,
    private val appLogger: AppLogger
) {
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    companion object {
        private const val TAG = "AlarmStateValidator"
        const val CATEGORY_STATE_VALIDATION = "STATE_VALIDATION"
    }

    /**
     * Validates that the displayed alarm state matches actual scheduled alarms.
     * Returns validation result with any inconsistencies found.
     */
    fun validateAlarmState(
        alarm: IntervalAlarm,
        alarmState: AlarmState?
    ): ValidationResult {
        appLogger.d(CATEGORY_STATE_VALIDATION, TAG, 
            "Validating alarm state: alarmId=${alarm.id}")
        
        val issues = mutableListOf<ValidationIssue>()
        
        // Check if alarm state exists
        if (alarmState == null) {
            appLogger.w(CATEGORY_STATE_VALIDATION, TAG, 
                "Missing alarm state for active alarm: alarmId=${alarm.id}")
            issues.add(ValidationIssue.MISSING_DATABASE_STATE)
            return ValidationResult(
                isValid = false,
                issues = issues,
                suggestedAction = RecoveryAction.RECALCULATE_AND_RESCHEDULE
            )
        }
        
        // Check if next ring time is stale
        if (isNextRingTimeStale(alarmState.nextScheduledRingTime)) {
            appLogger.w(CATEGORY_STATE_VALIDATION, TAG, 
                "Stale next ring time detected: alarmId=${alarm.id}, " +
                "nextRingTime=${alarmState.nextScheduledRingTime}")
            issues.add(ValidationIssue.STALE_NEXT_RING_TIME)
        }
        
        // Check if alarm is scheduled in AlarmManager
        if (!alarmState.isPaused && !alarmState.isStoppedForDay) {
            if (!isAlarmScheduledInSystem(alarm.id)) {
                appLogger.w(CATEGORY_STATE_VALIDATION, TAG, 
                    "Alarm not found in AlarmManager: alarmId=${alarm.id}")
                issues.add(ValidationIssue.MISSING_ALARM_MANAGER_ENTRY)
            }
        }
        
        // Validate time window consistency
        if (alarmState.nextScheduledRingTime != null && 
            !alarmState.isPaused && 
            !alarmState.isStoppedForDay) {
            if (!isNextRingTimeWithinWindow(alarm, alarmState.nextScheduledRingTime)) {
                appLogger.w(CATEGORY_STATE_VALIDATION, TAG, 
                    "Next ring time outside valid window: alarmId=${alarm.id}")
                issues.add(ValidationIssue.TIME_WINDOW_MISMATCH)
            }
        }
        
        // Determine suggested action
        val suggestedAction = when {
            issues.isEmpty() -> RecoveryAction.NO_ACTION_NEEDED
            issues.contains(ValidationIssue.MISSING_DATABASE_STATE) -> 
                RecoveryAction.RECALCULATE_AND_RESCHEDULE
            issues.any { it in listOf(
                ValidationIssue.STALE_NEXT_RING_TIME,
                ValidationIssue.MISSING_ALARM_MANAGER_ENTRY,
                ValidationIssue.TIME_WINDOW_MISMATCH
            )} -> RecoveryAction.RECALCULATE_AND_RESCHEDULE
            else -> RecoveryAction.NO_ACTION_NEEDED
        }
        
        val isValid = issues.isEmpty()
        appLogger.i(CATEGORY_STATE_VALIDATION, TAG, 
            "Validation complete: alarmId=${alarm.id}, valid=$isValid, " +
            "issues=${issues.size}, action=$suggestedAction")
        
        return ValidationResult(
            isValid = isValid,
            issues = issues,
            suggestedAction = suggestedAction
        )
    }

    /**
     * Checks if a next ring time is stale (in the past).
     */
    fun isNextRingTimeStale(nextRingTime: Long?): Boolean {
        if (nextRingTime == null) {
            return false
        }
        
        val currentTime = System.currentTimeMillis()
        val isStale = nextRingTime < currentTime
        
        if (isStale) {
            val staleDuration = (currentTime - nextRingTime) / 1000 / 60 // minutes
            appLogger.d(CATEGORY_STATE_VALIDATION, TAG, 
                "Next ring time is stale by $staleDuration minutes")
        }
        
        return isStale
    }

    /**
     * Verifies that an alarm is actually scheduled in AlarmManager.
     * Attempts to retrieve the PendingIntent to check if it exists.
     */
    fun isAlarmScheduledInSystem(alarmId: Long): Boolean {
        return try {
            val intent = Intent(context, AlarmReceiver::class.java).apply {
                putExtra("alarm_id", alarmId)
            }
            
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                alarmId.toInt(),
                intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            
            val isScheduled = pendingIntent != null
            
            appLogger.d(CATEGORY_STATE_VALIDATION, TAG, 
                "AlarmManager check: alarmId=$alarmId, scheduled=$isScheduled")
            
            isScheduled
        } catch (e: Exception) {
            appLogger.e(AppLogger.CATEGORY_ERROR, TAG, 
                "Error checking AlarmManager for alarm: $alarmId", e)
            false
        }
    }

    /**
     * Validates that the next ring time is within the alarm's time window.
     * Checks if the time is on a selected day and within start/end time.
     */
    private fun isNextRingTimeWithinWindow(
        alarm: IntervalAlarm,
        nextRingTime: Long
    ): Boolean {
        val nextRingDateTime = LocalDateTime.ofInstant(
            Instant.ofEpochMilli(nextRingTime),
            ZoneId.systemDefault()
        )
        
        // Check if day is selected
        val isValidDay = alarm.selectedDays.contains(nextRingDateTime.dayOfWeek)
        if (!isValidDay) {
            appLogger.d(CATEGORY_STATE_VALIDATION, TAG, 
                "Next ring time on non-selected day: ${nextRingDateTime.dayOfWeek}")
            return false
        }
        
        // Check if time is within window
        val ringTime = nextRingDateTime.toLocalTime()
        val isWithinWindow = (ringTime >= alarm.startTime && 
                             ringTime <= alarm.endTime)
        
        if (!isWithinWindow) {
            appLogger.d(CATEGORY_STATE_VALIDATION, TAG, 
                "Next ring time outside window: $ringTime, " +
                "window=${alarm.startTime}-${alarm.endTime}")
        }
        
        return isWithinWindow
    }
}

/**
 * Result of alarm state validation.
 */
data class ValidationResult(
    val isValid: Boolean,
    val issues: List<ValidationIssue>,
    val suggestedAction: RecoveryAction
)

/**
 * Types of validation issues that can be detected.
 */
enum class ValidationIssue {
    STALE_NEXT_RING_TIME,
    MISSING_ALARM_MANAGER_ENTRY,
    MISSING_DATABASE_STATE,
    TIME_WINDOW_MISMATCH
}

/**
 * Suggested recovery actions based on validation results.
 */
enum class RecoveryAction {
    RECALCULATE_AND_RESCHEDULE,
    DEACTIVATE_ALARM,
    NO_ACTION_NEEDED
}
