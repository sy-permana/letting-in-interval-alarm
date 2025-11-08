package com.lettingin.intervalAlarm.util

import com.lettingin.intervalAlarm.data.repository.AlarmRepository
import com.lettingin.intervalAlarm.data.repository.AlarmStateRepository
import com.lettingin.intervalAlarm.domain.scheduler.AlarmScheduler
import com.lettingin.intervalAlarm.domain.scheduler.AlarmSchedulerImpl
import kotlinx.coroutines.flow.firstOrNull
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages automatic recovery of alarm state inconsistencies.
 * Validates and synchronizes alarm state between database and AlarmManager.
 */
@Singleton
class AlarmStateRecoveryManager @Inject constructor(
    private val alarmRepository: AlarmRepository,
    private val alarmStateRepository: AlarmStateRepository,
    private val alarmScheduler: AlarmScheduler,
    private val alarmStateValidator: AlarmStateValidator,
    private val appLogger: AppLogger
) {
    companion object {
        private const val TAG = "AlarmStateRecoveryManager"
        const val CATEGORY_STATE_RECOVERY = "STATE_RECOVERY"
    }

    /**
     * Performs full state recovery for an active alarm.
     * Called on app startup and resume.
     * 
     * @param alarmId The ID of the alarm to recover
     * @return RecoveryResult indicating success/failure and actions taken
     */
    suspend fun recoverAlarmState(alarmId: Long): RecoveryResult {
        val startTime = System.currentTimeMillis()
        appLogger.i(CATEGORY_STATE_RECOVERY, TAG, 
            "Starting alarm state recovery: alarmId=$alarmId")
        
        return try {
            // Get alarm and state from database
            val alarm = alarmRepository.getAlarmById(alarmId).firstOrNull()
            if (alarm == null) {
                appLogger.w(CATEGORY_STATE_RECOVERY, TAG, 
                    "Alarm not found in database: alarmId=$alarmId")
                return RecoveryResult(
                    success = false,
                    action = "Alarm not found",
                    newNextRingTime = null,
                    error = IllegalStateException("Alarm not found: $alarmId")
                )
            }
            
            val alarmState = alarmStateRepository.getAlarmStateSync(alarmId)
            
            // Validate current state
            val validationResult = alarmStateValidator.validateAlarmState(alarm, alarmState)
            
            if (validationResult.isValid) {
                appLogger.i(CATEGORY_STATE_RECOVERY, TAG, 
                    "Alarm state is valid, no recovery needed: alarmId=$alarmId")
                return RecoveryResult(
                    success = true,
                    action = "No action needed - state is valid",
                    newNextRingTime = alarmState?.nextScheduledRingTime,
                    error = null
                )
            }
            
            // Perform recovery based on suggested action
            val result = when (validationResult.suggestedAction) {
                RecoveryAction.RECALCULATE_AND_RESCHEDULE -> {
                    recalculateAndReschedule(alarm, alarmState, validationResult.issues)
                }
                RecoveryAction.DEACTIVATE_ALARM -> {
                    deactivateAlarm(alarmId)
                }
                RecoveryAction.NO_ACTION_NEEDED -> {
                    RecoveryResult(
                        success = true,
                        action = "No action needed",
                        newNextRingTime = alarmState?.nextScheduledRingTime,
                        error = null
                    )
                }
            }
            
            val duration = System.currentTimeMillis() - startTime
            appLogger.i(CATEGORY_STATE_RECOVERY, TAG, 
                "Recovery completed: alarmId=$alarmId, success=${result.success}, " +
                "duration=${duration}ms, action='${result.action}'")
            
            result
        } catch (e: Exception) {
            val duration = System.currentTimeMillis() - startTime
            appLogger.e(AppLogger.CATEGORY_ERROR, TAG, 
                "Recovery failed: alarmId=$alarmId, duration=${duration}ms", e)
            RecoveryResult(
                success = false,
                action = "Recovery failed with exception",
                newNextRingTime = null,
                error = e
            )
        }
    }

    /**
     * Synchronizes database state with AlarmManager.
     * Ensures the alarm scheduled in AlarmManager matches database state.
     */
    suspend fun synchronizeWithAlarmManager(alarmId: Long) {
        appLogger.d(CATEGORY_STATE_RECOVERY, TAG, 
            "Synchronizing with AlarmManager: alarmId=$alarmId")
        
        try {
            val alarm = alarmRepository.getAlarmById(alarmId).firstOrNull()
            if (alarm == null) {
                appLogger.w(CATEGORY_STATE_RECOVERY, TAG, 
                    "Cannot synchronize - alarm not found: alarmId=$alarmId")
                return
            }
            
            val alarmState = alarmStateRepository.getAlarmStateSync(alarmId)
            if (alarmState == null) {
                appLogger.w(CATEGORY_STATE_RECOVERY, TAG, 
                    "Cannot synchronize - alarm state not found: alarmId=$alarmId")
                return
            }
            
            // Check if alarm should be scheduled
            val shouldBeScheduled = !alarmState.isPaused && 
                                   !alarmState.isStoppedForDay && 
                                   alarmState.nextScheduledRingTime != null
            
            val isScheduled = alarmStateValidator.isAlarmScheduledInSystem(alarmId)
            
            if (shouldBeScheduled && !isScheduled) {
                // Should be scheduled but isn't - reschedule it
                appLogger.i(CATEGORY_STATE_RECOVERY, TAG, 
                    "Alarm should be scheduled but isn't - rescheduling: alarmId=$alarmId")
                alarmState.nextScheduledRingTime?.let { nextRingTime ->
                    alarmScheduler.scheduleNextRing(alarmId, nextRingTime)
                }
            } else if (!shouldBeScheduled && isScheduled) {
                // Shouldn't be scheduled but is - cancel it
                appLogger.i(CATEGORY_STATE_RECOVERY, TAG, 
                    "Alarm shouldn't be scheduled but is - cancelling: alarmId=$alarmId")
                alarmScheduler.cancelAlarm(alarmId)
            }
            
            appLogger.i(CATEGORY_STATE_RECOVERY, TAG, 
                "Synchronization complete: alarmId=$alarmId")
        } catch (e: Exception) {
            appLogger.e(AppLogger.CATEGORY_ERROR, TAG, 
                "Synchronization failed: alarmId=$alarmId", e)
        }
    }

    /**
     * Recalculates next ring time and reschedules the alarm.
     */
    private suspend fun recalculateAndReschedule(
        alarm: com.lettingin.intervalAlarm.data.model.IntervalAlarm,
        alarmState: com.lettingin.intervalAlarm.data.model.AlarmState?,
        issues: List<ValidationIssue>
    ): RecoveryResult {
        appLogger.i(CATEGORY_STATE_RECOVERY, TAG, 
            "Recalculating and rescheduling: alarmId=${alarm.id}, issues=$issues")
        
        return try {
            val currentTime = System.currentTimeMillis()
            
            // Calculate new next ring time
            val schedulerImpl = alarmScheduler as? AlarmSchedulerImpl
            val newNextRingTime = schedulerImpl?.calculateNextRingTime(alarm, currentTime)
            
            if (newNextRingTime == null) {
                appLogger.w(CATEGORY_STATE_RECOVERY, TAG, 
                    "No valid next ring time found: alarmId=${alarm.id}")
                return RecoveryResult(
                    success = false,
                    action = "No valid next ring time available",
                    newNextRingTime = null,
                    error = IllegalStateException("No valid next ring time")
                )
            }
            
            // Update alarm state in database
            val updatedState = alarmState?.copy(
                nextScheduledRingTime = newNextRingTime,
                isPaused = false,
                pauseUntilTime = null
            ) ?: com.lettingin.intervalAlarm.data.model.AlarmState(
                alarmId = alarm.id,
                lastRingTime = null,
                nextScheduledRingTime = newNextRingTime,
                isPaused = false,
                pauseUntilTime = null,
                isStoppedForDay = false,
                currentDayStartTime = currentTime,
                todayRingCount = 0,
                todayUserDismissCount = 0,
                todayAutoDismissCount = 0
            )
            
            alarmStateRepository.updateAlarmState(updatedState)
            
            // Reschedule with AlarmManager
            alarmScheduler.scheduleNextRing(alarm.id, newNextRingTime)
            
            appLogger.i(CATEGORY_STATE_RECOVERY, TAG, 
                "Successfully recalculated and rescheduled: alarmId=${alarm.id}, " +
                "newNextRingTime=$newNextRingTime")
            
            RecoveryResult(
                success = true,
                action = "Recalculated and rescheduled alarm",
                newNextRingTime = newNextRingTime,
                error = null
            )
        } catch (e: Exception) {
            appLogger.e(AppLogger.CATEGORY_ERROR, TAG, 
                "Failed to recalculate and reschedule: alarmId=${alarm.id}", e)
            RecoveryResult(
                success = false,
                action = "Failed to recalculate and reschedule",
                newNextRingTime = null,
                error = e
            )
        }
    }

    /**
     * Deactivates an alarm that cannot be recovered.
     */
    private suspend fun deactivateAlarm(alarmId: Long): RecoveryResult {
        appLogger.w(CATEGORY_STATE_RECOVERY, TAG, 
            "Deactivating alarm due to unrecoverable state: alarmId=$alarmId")
        
        return try {
            alarmRepository.deactivateAlarm(alarmId)
            alarmScheduler.cancelAlarm(alarmId)
            
            RecoveryResult(
                success = true,
                action = "Deactivated alarm due to unrecoverable state",
                newNextRingTime = null,
                error = null
            )
        } catch (e: Exception) {
            appLogger.e(AppLogger.CATEGORY_ERROR, TAG, 
                "Failed to deactivate alarm: alarmId=$alarmId", e)
            RecoveryResult(
                success = false,
                action = "Failed to deactivate alarm",
                newNextRingTime = null,
                error = e
            )
        }
    }
}

/**
 * Result of alarm state recovery operation.
 */
data class RecoveryResult(
    val success: Boolean,
    val action: String,
    val newNextRingTime: Long?,
    val error: Throwable?
)
