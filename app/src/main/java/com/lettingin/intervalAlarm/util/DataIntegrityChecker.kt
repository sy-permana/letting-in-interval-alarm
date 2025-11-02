package com.lettingin.intervalAlarm.util

import com.lettingin.intervalAlarm.data.repository.AlarmRepository
import com.lettingin.intervalAlarm.data.repository.AlarmStateRepository
import com.lettingin.intervalAlarm.data.repository.StatisticsRepository
import kotlinx.coroutines.flow.firstOrNull
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Data integrity checker that validates and cleans up data on app startup.
 * Handles corrupted data, orphaned states, and old statistics.
 */
@Singleton
class DataIntegrityChecker @Inject constructor(
    private val alarmRepository: AlarmRepository,
    private val alarmStateRepository: AlarmStateRepository,
    private val statisticsRepository: StatisticsRepository,
    private val dataValidator: DataValidator,
    private val appLogger: AppLogger
) {
    
    companion object {
        private const val TAG = "DataIntegrityChecker"
        private const val MAX_STATISTICS_PER_ALARM = 5
    }
    
    /**
     * Integrity check result
     */
    data class IntegrityCheckResult(
        val corruptedAlarmsRemoved: Int = 0,
        val orphanedStatesRemoved: Int = 0,
        val oldStatisticsRemoved: Int = 0,
        val errors: List<String> = emptyList()
    )
    
    /**
     * Run comprehensive data integrity check
     */
    suspend fun runIntegrityCheck(): IntegrityCheckResult {
        appLogger.i(AppLogger.CATEGORY_SYSTEM, TAG, "Starting data integrity check")
        
        var corruptedCount = 0
        var orphanedCount = 0
        var oldStatsCount = 0
        val errors = mutableListOf<String>()
        
        try {
            // Check for corrupted alarms
            corruptedCount = checkAndRemoveCorruptedAlarms()
            
            // Check for orphaned alarm states
            orphanedCount = checkAndRemoveOrphanedStates()
            
            // Clean up old statistics
            oldStatsCount = cleanupOldStatistics()
            
            appLogger.i(AppLogger.CATEGORY_SYSTEM, TAG,
                "Integrity check complete: $corruptedCount corrupted, $orphanedCount orphaned, $oldStatsCount old stats")
            
        } catch (e: Exception) {
            appLogger.e(AppLogger.CATEGORY_ERROR, TAG,
                "Error during integrity check", e)
            errors.add("Integrity check failed: ${e.message}")
        }
        
        return IntegrityCheckResult(
            corruptedAlarmsRemoved = corruptedCount,
            orphanedStatesRemoved = orphanedCount,
            oldStatisticsRemoved = oldStatsCount,
            errors = errors
        )
    }
    
    /**
     * Check for and remove corrupted alarms
     */
    private suspend fun checkAndRemoveCorruptedAlarms(): Int {
        var removedCount = 0
        
        try {
            val allAlarms = alarmRepository.getAllAlarms().firstOrNull() ?: emptyList()
            
            for (alarm in allAlarms) {
                if (dataValidator.isAlarmCorrupted(alarm)) {
                    appLogger.w(AppLogger.CATEGORY_DATABASE, TAG,
                        "Found corrupted alarm: id=${alarm.id}, removing")
                    
                    try {
                        alarmRepository.deleteAlarm(alarm.id)
                        alarmStateRepository.deleteAlarmState(alarm.id)
                        removedCount++
                    } catch (e: Exception) {
                        appLogger.e(AppLogger.CATEGORY_ERROR, TAG,
                            "Failed to remove corrupted alarm: id=${alarm.id}", e)
                    }
                }
            }
        } catch (e: Exception) {
            appLogger.e(AppLogger.CATEGORY_ERROR, TAG,
                "Error checking corrupted alarms", e)
        }
        
        return removedCount
    }
    
    /**
     * Check for and remove orphaned alarm states
     */
    private suspend fun checkAndRemoveOrphanedStates(): Int {
        var removedCount = 0
        
        try {
            val allAlarms = alarmRepository.getAllAlarms().firstOrNull() ?: emptyList()
            val alarmIds = allAlarms.map { it.id }.toSet()
            
            // Get all alarm states (we need to implement a method to get all states)
            // For now, we'll check states for known alarms
            for (alarm in allAlarms) {
                val state = alarmStateRepository.getAlarmState(alarm.id).firstOrNull()
                if (state != null && !alarmIds.contains(state.alarmId)) {
                    appLogger.w(AppLogger.CATEGORY_DATABASE, TAG,
                        "Found orphaned state: alarmId=${state.alarmId}, removing")
                    
                    try {
                        alarmStateRepository.deleteAlarmState(state.alarmId)
                        removedCount++
                    } catch (e: Exception) {
                        appLogger.e(AppLogger.CATEGORY_ERROR, TAG,
                            "Failed to remove orphaned state: alarmId=${state.alarmId}", e)
                    }
                }
            }
        } catch (e: Exception) {
            appLogger.e(AppLogger.CATEGORY_ERROR, TAG,
                "Error checking orphaned states", e)
        }
        
        return removedCount
    }
    
    /**
     * Clean up old statistics (keep only last 5 cycles per alarm)
     */
    private suspend fun cleanupOldStatistics(): Int {
        var removedCount = 0
        
        try {
            val allAlarms = alarmRepository.getAllAlarms().firstOrNull() ?: emptyList()
            
            for (alarm in allAlarms) {
                try {
                    val stats = statisticsRepository.getStatisticsForAlarm(alarm.id)
                        .firstOrNull() ?: emptyList()
                    
                    if (stats.size > MAX_STATISTICS_PER_ALARM) {
                        val toRemove = stats.size - MAX_STATISTICS_PER_ALARM
                        appLogger.d(AppLogger.CATEGORY_DATABASE, TAG,
                            "Cleaning up $toRemove old statistics for alarm ${alarm.id}")
                        
                        statisticsRepository.cleanupOldStatistics(alarm.id, MAX_STATISTICS_PER_ALARM)
                        removedCount += toRemove
                    }
                } catch (e: Exception) {
                    appLogger.e(AppLogger.CATEGORY_ERROR, TAG,
                        "Failed to cleanup statistics for alarm ${alarm.id}", e)
                }
            }
        } catch (e: Exception) {
            appLogger.e(AppLogger.CATEGORY_ERROR, TAG,
                "Error cleaning up statistics", e)
        }
        
        return removedCount
    }
    
    /**
     * Validate active alarm state
     */
    suspend fun validateActiveAlarmState(): Boolean {
        try {
            val activeAlarm = alarmRepository.getActiveAlarm().firstOrNull()
            
            if (activeAlarm != null) {
                // Check if alarm is corrupted
                if (dataValidator.isAlarmCorrupted(activeAlarm)) {
                    appLogger.e(AppLogger.CATEGORY_ERROR, TAG,
                        "Active alarm is corrupted: id=${activeAlarm.id}")
                    
                    // Deactivate and remove
                    alarmRepository.deactivateAlarm(activeAlarm.id)
                    alarmRepository.deleteAlarm(activeAlarm.id)
                    return false
                }
                
                // Validate alarm configuration
                val validationResult = dataValidator.validateAlarm(activeAlarm)
                if (!validationResult.isValid) {
                    appLogger.w(AppLogger.CATEGORY_DATABASE, TAG,
                        "Active alarm validation failed: ${validationResult.getErrorMessage()}")
                    return false
                }
            }
            
            return true
        } catch (e: Exception) {
            appLogger.e(AppLogger.CATEGORY_ERROR, TAG,
                "Error validating active alarm state", e)
            return false
        }
    }
}
