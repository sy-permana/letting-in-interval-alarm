package com.lettingin.intervalAlarm.ui.debug

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lettingin.intervalAlarm.data.model.AlarmState
import com.lettingin.intervalAlarm.data.model.IntervalAlarm
import com.lettingin.intervalAlarm.data.repository.AlarmRepository
import com.lettingin.intervalAlarm.data.repository.AlarmStateRepository
import com.lettingin.intervalAlarm.receiver.BootReceiver
import com.lettingin.intervalAlarm.util.AppLogger
import com.lettingin.intervalAlarm.util.PermissionChecker
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.cancelChildren
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject

data class DebugInfo(
    val currentTime: String = "",
    val deviceUptime: String = "",
    val appVersion: String = "",
    val androidVersion: String = "",
    val activeAlarm: IntervalAlarm? = null,
    val alarmState: AlarmState? = null,
    val allAlarms: List<IntervalAlarm> = emptyList(),
    val permissions: Map<String, Boolean> = emptyMap(),
    val recentLogs: List<AppLogger.LogEntry> = emptyList(),
    val logStats: Map<String, Int> = emptyMap(),
    val validationStatus: ValidationStatus? = null,
    val alarmManagerState: AlarmManagerState? = null
)

data class ValidationStatus(
    val isValid: Boolean,
    val lastValidationTime: String,
    val issues: List<String>,
    val suggestedAction: String
)

data class AlarmManagerState(
    val isScheduled: Boolean,
    val scheduledTime: String?,
    val matchesDatabase: Boolean
)

@HiltViewModel
class DebugViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val alarmRepository: AlarmRepository,
    private val alarmStateRepository: AlarmStateRepository,
    private val permissionChecker: PermissionChecker,
    private val appLogger: AppLogger,
    private val alarmStateValidator: com.lettingin.intervalAlarm.util.AlarmStateValidator,
    private val alarmStateRecoveryManager: com.lettingin.intervalAlarm.util.AlarmStateRecoveryManager
) : ViewModel() {

    private val _debugInfo = MutableStateFlow(DebugInfo())
    val debugInfo: StateFlow<DebugInfo> = _debugInfo.asStateFlow()
    
    private val _exportStatus = MutableStateFlow<String?>(null)
    val exportStatus: StateFlow<String?> = _exportStatus.asStateFlow()
    
    private val _validationMessage = MutableStateFlow<String?>(null)
    val validationMessage: StateFlow<String?> = _validationMessage.asStateFlow()
    
    // Jobs for cancellable operations
    private var refreshJob: kotlinx.coroutines.Job? = null
    private var exportJob: kotlinx.coroutines.Job? = null
    private var clearLogsJob: kotlinx.coroutines.Job? = null
    private var simulateBootJob: kotlinx.coroutines.Job? = null
    private var validateJob: kotlinx.coroutines.Job? = null

    init {
        appLogger.i(AppLogger.CATEGORY_UI, "DebugViewModel", "Debug screen initialized")
        refreshDebugInfo()
    }

    fun refreshDebugInfo() {
        // Cancel previous refresh to prevent overlapping operations
        refreshJob?.cancel()
        
        refreshJob = viewModelScope.launch {
            appLogger.d(AppLogger.CATEGORY_UI, "DebugViewModel", "Refreshing debug info")
            
            val currentTime = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
            
            val uptimeMillis = SystemClock.elapsedRealtime()
            val uptimeMinutes = uptimeMillis / 1000 / 60
            val deviceUptime = "${uptimeMinutes / 60}h ${uptimeMinutes % 60}m"

            val appVersion = "1.0.0 (Build 1)"
            val androidVersion = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})"

            val activeAlarm = alarmRepository.getActiveAlarm().firstOrNull()

            val alarmState = activeAlarm?.let { alarm ->
                alarmStateRepository.getAlarmStateSync(alarm.id)
            }

            val allAlarms = alarmRepository.getAllAlarms().firstOrNull() ?: emptyList()

            val permissions = mapOf(
                "Notification" to permissionChecker.areAllCriticalPermissionsGranted(),
                "Exact Alarm" to true,
                "Full Screen Intent" to true,
                "Battery Optimization" to false
            )
            
            // Get recent logs
            val recentLogs = appLogger.getRecentLogs(50)
            
            // Calculate log statistics
            val logStats = mapOf(
                "Total Logs" to recentLogs.size,
                "Errors" to appLogger.getLogsByLevel(AppLogger.LogLevel.ERROR).size,
                "Warnings" to appLogger.getLogsByLevel(AppLogger.LogLevel.WARNING).size,
                "Alarm Events" to appLogger.getLogsByCategory(AppLogger.CATEGORY_ALARM).size,
                "Scheduling Events" to appLogger.getLogsByCategory(AppLogger.CATEGORY_SCHEDULING).size
            )
            
            // Get validation status if active alarm exists
            val validationStatus = if (activeAlarm != null && alarmState != null) {
                try {
                    val validationResult = alarmStateValidator.validateAlarmState(activeAlarm, alarmState)
                    ValidationStatus(
                        isValid = validationResult.isValid,
                        lastValidationTime = currentTime,
                        issues = validationResult.issues.map { it.name },
                        suggestedAction = validationResult.suggestedAction.name
                    )
                } catch (e: Exception) {
                    appLogger.e(AppLogger.CATEGORY_ERROR, "DebugViewModel", "Failed to validate alarm state", e)
                    null
                }
            } else {
                null
            }
            
            // Get AlarmManager state comparison
            val alarmManagerState = if (activeAlarm != null && alarmState != null) {
                try {
                    val isScheduled = alarmStateValidator.isAlarmScheduledInSystem(activeAlarm.id)
                    val scheduledTime = if (isScheduled) {
                        alarmState.nextScheduledRingTime?.let { formatTimestamp(it) }
                    } else {
                        null
                    }
                    val matchesDatabase = isScheduled && alarmState.nextScheduledRingTime != null
                    
                    AlarmManagerState(
                        isScheduled = isScheduled,
                        scheduledTime = scheduledTime,
                        matchesDatabase = matchesDatabase
                    )
                } catch (e: Exception) {
                    appLogger.e(AppLogger.CATEGORY_ERROR, "DebugViewModel", "Failed to check AlarmManager state", e)
                    null
                }
            } else {
                null
            }

            _debugInfo.value = DebugInfo(
                currentTime = currentTime,
                deviceUptime = deviceUptime,
                appVersion = appVersion,
                androidVersion = androidVersion,
                activeAlarm = activeAlarm,
                alarmState = alarmState,
                allAlarms = allAlarms,
                permissions = permissions,
                recentLogs = recentLogs,
                logStats = logStats,
                validationStatus = validationStatus,
                alarmManagerState = alarmManagerState
            )
        }
    }
    
    private fun formatTimestamp(timestamp: Long): String {
        val dateTime = java.time.LocalDateTime.ofInstant(
            java.time.Instant.ofEpochMilli(timestamp),
            java.time.ZoneId.systemDefault()
        )
        return dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
    }

    fun exportLogs() {
        exportJob?.cancel()
        
        exportJob = viewModelScope.launch {
            try {
                appLogger.i(AppLogger.CATEGORY_UI, "DebugViewModel", "Exporting logs to file")
                
                val logFile = appLogger.exportLogsToFile()
                
                if (logFile != null) {
                    _exportStatus.value = "Logs exported to: ${logFile.absolutePath}"
                    appLogger.i(AppLogger.CATEGORY_UI, "DebugViewModel", "Logs exported successfully")
                } else {
                    _exportStatus.value = "Failed to export logs"
                    appLogger.e(AppLogger.CATEGORY_ERROR, "DebugViewModel", "Log export failed")
                }
                
                // Clear status after 5 seconds with timeout
                kotlinx.coroutines.withTimeout(6000L) {
                    kotlinx.coroutines.delay(5000)
                    _exportStatus.value = null
                }
            } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
                // Timeout is expected, just clear status
                _exportStatus.value = null
            } catch (e: Exception) {
                appLogger.e(AppLogger.CATEGORY_ERROR, "DebugViewModel", "Failed to export logs", e)
                _exportStatus.value = "Error: ${e.message}"
            }
        }
    }
    
    fun clearLogs() {
        clearLogsJob?.cancel()
        
        clearLogsJob = viewModelScope.launch {
            try {
                appLogger.clearLogs()
                appLogger.i(AppLogger.CATEGORY_UI, "DebugViewModel", "Logs cleared by user")
                refreshDebugInfo()
            } catch (e: Exception) {
                appLogger.e(AppLogger.CATEGORY_ERROR, "DebugViewModel", "Failed to clear logs", e)
            }
        }
    }

    fun simulateBootReceiver() {
        simulateBootJob?.cancel()
        
        simulateBootJob = viewModelScope.launch {
            try {
                appLogger.i(AppLogger.CATEGORY_SYSTEM, "DebugViewModel", "Simulating BOOT_COMPLETED broadcast")
                
                val intent = Intent(context, BootReceiver::class.java).apply {
                    action = Intent.ACTION_BOOT_COMPLETED
                }
                
                val receiver = BootReceiver()
                receiver.onReceive(context, intent)
                
                appLogger.i(AppLogger.CATEGORY_SYSTEM, "DebugViewModel", "Boot receiver simulation complete")
                
                // Refresh debug info after simulation with timeout
                kotlinx.coroutines.withTimeout(3000L) {
                    kotlinx.coroutines.delay(2000)
                    refreshDebugInfo()
                }
            } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
                appLogger.w(AppLogger.CATEGORY_SYSTEM, "DebugViewModel", "Boot simulation refresh timed out")
            } catch (e: Exception) {
                appLogger.e(AppLogger.CATEGORY_ERROR, "DebugViewModel", "Failed to simulate boot receiver", e)
            }
        }
    }
    
    /**
     * Manually trigger alarm state validation and recovery.
     * Useful for debugging state inconsistencies.
     */
    fun triggerStateValidation() {
        validateJob?.cancel()
        
        validateJob = viewModelScope.launch {
            try {
                val activeAlarm = alarmRepository.getActiveAlarm().firstOrNull()
                
                if (activeAlarm == null) {
                    _validationMessage.value = "No active alarm to validate"
                    appLogger.i(AppLogger.CATEGORY_UI, "DebugViewModel", "No active alarm for validation")
                    
                    // Clear message after 3 seconds
                    kotlinx.coroutines.delay(3000)
                    _validationMessage.value = null
                    return@launch
                }
                
                appLogger.i(AppLogger.CATEGORY_UI, "DebugViewModel", 
                    "Manual validation triggered for alarm ${activeAlarm.id}")
                
                _validationMessage.value = "Validating alarm state..."
                
                // Perform validation and recovery
                val recoveryResult = alarmStateRecoveryManager.recoverAlarmState(activeAlarm.id)
                
                val message = if (recoveryResult.success) {
                    "✅ Validation complete: ${recoveryResult.action}"
                } else {
                    "❌ Validation failed: ${recoveryResult.action}"
                }
                
                _validationMessage.value = message
                appLogger.i(AppLogger.CATEGORY_UI, "DebugViewModel", 
                    "Manual validation result: $message")
                
                // Refresh debug info to show updated state
                refreshDebugInfo()
                
                // Clear message after 5 seconds
                kotlinx.coroutines.delay(5000)
                _validationMessage.value = null
            } catch (e: Exception) {
                appLogger.e(AppLogger.CATEGORY_ERROR, "DebugViewModel", 
                    "Failed to trigger validation", e)
                _validationMessage.value = "Error: ${e.message}"
                
                // Clear message after 5 seconds
                kotlinx.coroutines.delay(5000)
                _validationMessage.value = null
            }
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        // Cancel all ongoing jobs
        refreshJob?.cancel()
        exportJob?.cancel()
        clearLogsJob?.cancel()
        simulateBootJob?.cancel()
        validateJob?.cancel()
        
        // Cancel all child coroutines in viewModelScope
        viewModelScope.coroutineContext.cancelChildren()
        
        appLogger.d(AppLogger.CATEGORY_UI, "DebugViewModel", "ViewModel cleared, cancelled all jobs and child coroutines")
    }
}
