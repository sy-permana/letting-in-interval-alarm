package com.lettingin.intervalAlarm.util

import com.lettingin.intervalAlarm.data.repository.AlarmRepository
import com.lettingin.intervalAlarm.data.repository.AlarmStateRepository
import com.lettingin.intervalAlarm.domain.scheduler.AlarmScheduler
import kotlinx.coroutines.flow.firstOrNull
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Validates alarm state consistency after device reboot.
 * Ensures that active alarms are properly restored and scheduled.
 */
@Singleton
class RebootValidator @Inject constructor(
    private val alarmRepository: AlarmRepository,
    private val alarmStateRepository: AlarmStateRepository,
    private val alarmScheduler: AlarmScheduler,
    private val dataValidator: DataValidator,
    private val appLogger: AppLogger
) {
    
    companion object {
        private const val TAG = "RebootValidator"
    }
    
    /**
     * Validation result after reboot
     */
    data class RebootValidationResult(
        val activeAlarmFound: Boolean = false,
        val alarmStateValid: Boolean = false,
        val alarmRescheduled: Boolean = false,
        val errors: List<String> = emptyList()
    )
    
    /**
     * Validate and restore alarm state after device reboot
     */
    suspend fun validateAfterReboot(): RebootValidationResult {
        appLogger.i(AppLogger.CATEGORY_SYSTEM, TAG, "Starting reboot validation")
        
        val errors = mutableListOf<String>()
        var activeAlarmFound = false
        var alarmStateValid = false
        var alarmRescheduled = false
        
        try {
            // Get active alarm
            val activeAlarm = alarmRepository.getActiveAlarm().firstOrNull()
            
            if (activeAlarm == null) {
                appLogger.i(AppLogger.CATEGORY_SYSTEM, TAG, "No active alarm found after reboot")
                return RebootValidationResult(
                    activeAlarmFound = false,
                    alarmStateValid = true,
                    alarmRescheduled = false
                )
            }
            
            activeAlarmFound = true
            appLogger.i(AppLogger.CATEGORY_SYSTEM, TAG, 
                "Active alarm found: id=${activeAlarm.id}, label='${activeAlarm.label}'")
            
            // Validate alarm data
            val validationResult = dataValidator.validateAlarm(activeAlarm)
            if (!validationResult.isValid) {
                errors.add("Active alarm validation failed: ${validationResult.getErrorMessage()}")
                appLogger.e(AppLogger.CATEGORY_ERROR, TAG,
                    "Active alarm validation failed: ${validationResult.getErrorMessage()}")
                
                // Deactivate invalid alarm
                alarmRepository.deactivateAlarm(activeAlarm.id)
                return RebootValidationResult(
                    activeAlarmFound = true,
                    alarmStateValid = false,
                    alarmRescheduled = false,
                    errors = errors
                )
            }
            
            alarmStateValid = true
            
            // Check if alarm is corrupted
            if (dataValidator.isAlarmCorrupted(activeAlarm)) {
                errors.add("Active alarm is corrupted")
                appLogger.e(AppLogger.CATEGORY_ERROR, TAG, "Active alarm is corrupted: id=${activeAlarm.id}")
                
                // Deactivate and remove corrupted alarm
                alarmRepository.deactivateAlarm(activeAlarm.id)
                alarmRepository.deleteAlarm(activeAlarm.id)
                return RebootValidationResult(
                    activeAlarmFound = true,
                    alarmStateValid = false,
                    alarmRescheduled = false,
                    errors = errors
                )
            }
            
            // Get alarm state
            val alarmState = alarmStateRepository.getAlarmState(activeAlarm.id).firstOrNull()
            
            // Validate alarm state consistency
            if (alarmState != null) {
                val stateErrors = validateAlarmStateConsistency(activeAlarm, alarmState)
                if (stateErrors.isNotEmpty()) {
                    errors.addAll(stateErrors)
                    appLogger.w(AppLogger.CATEGORY_SYSTEM, TAG,
                        "Alarm state inconsistencies found: ${stateErrors.joinToString(", ")}")
                }
            }
            
            // Calculate next ring time
            val currentTime = System.currentTimeMillis()
            val nextRingTime = (alarmScheduler as? com.lettingin.intervalAlarm.domain.scheduler.AlarmSchedulerImpl)
                ?.calculateNextRingTime(activeAlarm, currentTime)
            
            if (nextRingTime != null) {
                // Reschedule alarm
                alarmScheduler.scheduleNextRing(activeAlarm.id, nextRingTime)
                alarmRescheduled = true
                
                // Update alarm state with new next ring time
                if (alarmState != null) {
                    alarmStateRepository.updateAlarmState(
                        alarmState.copy(nextScheduledRingTime = nextRingTime)
                    )
                }
                
                appLogger.i(AppLogger.CATEGORY_SYSTEM, TAG,
                    "Alarm rescheduled after reboot: id=${activeAlarm.id}, nextRing=$nextRingTime")
            } else {
                errors.add("No valid next ring time found")
                appLogger.w(AppLogger.CATEGORY_SYSTEM, TAG,
                    "No valid next ring time found for alarm ${activeAlarm.id}")
                
                // If one-cycle mode and no more valid times, deactivate
                if (!activeAlarm.isRepeatable) {
                    alarmRepository.deactivateAlarm(activeAlarm.id)
                    appLogger.i(AppLogger.CATEGORY_SYSTEM, TAG,
                        "One-cycle alarm completed, deactivated: id=${activeAlarm.id}")
                }
            }
            
        } catch (e: Exception) {
            errors.add("Reboot validation failed: ${e.message}")
            appLogger.e(AppLogger.CATEGORY_ERROR, TAG, "Reboot validation failed", e)
        }
        
        val result = RebootValidationResult(
            activeAlarmFound = activeAlarmFound,
            alarmStateValid = alarmStateValid,
            alarmRescheduled = alarmRescheduled,
            errors = errors
        )
        
        appLogger.i(AppLogger.CATEGORY_SYSTEM, TAG,
            "Reboot validation complete: activeAlarm=$activeAlarmFound, valid=$alarmStateValid, rescheduled=$alarmRescheduled")
        
        return result
    }
    
    /**
     * Validate alarm state consistency
     */
    private fun validateAlarmStateConsistency(
        alarm: com.lettingin.intervalAlarm.data.model.IntervalAlarm,
        state: com.lettingin.intervalAlarm.data.model.AlarmState
    ): List<String> {
        val errors = mutableListOf<String>()
        
        // Check if alarm ID matches
        if (alarm.id != state.alarmId) {
            errors.add("Alarm ID mismatch: alarm=${alarm.id}, state=${state.alarmId}")
        }
        
        // Check if paused state is valid
        if (state.isPaused && state.pauseUntilTime == null) {
            errors.add("Alarm is paused but pauseUntilTime is null")
        }
        
        // Check if pause time has expired
        if (state.isPaused && state.pauseUntilTime != null) {
            val currentTime = System.currentTimeMillis()
            if (currentTime > state.pauseUntilTime) {
                errors.add("Pause time has expired but alarm is still marked as paused")
            }
        }
        
        // Check if next scheduled ring time is in the past
        if (state.nextScheduledRingTime != null) {
            val currentTime = System.currentTimeMillis()
            if (state.nextScheduledRingTime < currentTime - 60000) { // Allow 1 minute grace period
                errors.add("Next scheduled ring time is in the past")
            }
        }
        
        // Check if stopped for day but it's a new day
        if (state.isStoppedForDay && state.currentDayStartTime != null) {
            val currentTime = System.currentTimeMillis()
            val daysSinceStart = (currentTime - state.currentDayStartTime) / (24 * 60 * 60 * 1000)
            if (daysSinceStart >= 1) {
                errors.add("Alarm is stopped for day but it's a new day")
            }
        }
        
        return errors
    }
    
    /**
     * Reset alarm state after reboot if needed
     */
    suspend fun resetAlarmStateIfNeeded(alarmId: Long) {
        try {
            val state = alarmStateRepository.getAlarmState(alarmId).firstOrNull()
            if (state == null) {
                appLogger.w(AppLogger.CATEGORY_SYSTEM, TAG, "No alarm state found for alarm $alarmId")
                return
            }
            
            val currentTime = System.currentTimeMillis()
            var needsUpdate = false
            var updatedState = state
            
            // Reset pause if expired
            if (state.isPaused && state.pauseUntilTime != null && currentTime > state.pauseUntilTime) {
                updatedState = updatedState.copy(isPaused = false, pauseUntilTime = null)
                needsUpdate = true
                appLogger.i(AppLogger.CATEGORY_SYSTEM, TAG, "Reset expired pause for alarm $alarmId")
            }
            
            // Reset stopped for day if it's a new day
            if (state.isStoppedForDay && state.currentDayStartTime != null) {
                val daysSinceStart = (currentTime - state.currentDayStartTime) / (24 * 60 * 60 * 1000)
                if (daysSinceStart >= 1) {
                    updatedState = updatedState.copy(
                        isStoppedForDay = false,
                        currentDayStartTime = currentTime,
                        todayRingCount = 0,
                        todayUserDismissCount = 0,
                        todayAutoDismissCount = 0
                    )
                    needsUpdate = true
                    appLogger.i(AppLogger.CATEGORY_SYSTEM, TAG, "Reset stopped for day for alarm $alarmId")
                }
            }
            
            if (needsUpdate) {
                alarmStateRepository.updateAlarmState(updatedState)
            }
            
        } catch (e: Exception) {
            appLogger.e(AppLogger.CATEGORY_ERROR, TAG, "Failed to reset alarm state for alarm $alarmId", e)
        }
    }
}
