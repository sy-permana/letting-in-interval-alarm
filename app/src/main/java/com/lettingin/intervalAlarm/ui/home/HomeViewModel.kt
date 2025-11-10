package com.lettingin.intervalAlarm.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lettingin.intervalAlarm.data.model.AlarmCycleStatistics
import com.lettingin.intervalAlarm.data.model.AlarmState
import com.lettingin.intervalAlarm.data.model.IntervalAlarm
import com.lettingin.intervalAlarm.data.repository.AlarmRepository
import com.lettingin.intervalAlarm.data.repository.AlarmStateRepository
import com.lettingin.intervalAlarm.data.repository.StatisticsRepository
import com.lettingin.intervalAlarm.domain.scheduler.AlarmScheduler
import com.lettingin.intervalAlarm.util.AppLogger
import com.lettingin.intervalAlarm.util.ErrorHandler
import com.lettingin.intervalAlarm.util.PermissionChecker
import com.lettingin.intervalAlarm.util.PermissionException
import com.lettingin.intervalAlarm.util.PermissionType
import com.lettingin.intervalAlarm.util.AlarmStateRecoveryManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.cancelChildren
import javax.inject.Inject

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val alarmRepository: AlarmRepository,
    private val alarmStateRepository: AlarmStateRepository,
    private val statisticsRepository: StatisticsRepository,
    private val alarmScheduler: AlarmScheduler,
    private val permissionChecker: PermissionChecker,
    private val errorHandler: ErrorHandler,
    private val appLogger: AppLogger,
    private val alarmStateRecoveryManager: AlarmStateRecoveryManager
) : ViewModel() {

    // StateFlow for all alarms list (filtered to exclude test alarms)
    val allAlarms: StateFlow<List<IntervalAlarm>> = alarmRepository.getAllAlarms()
        .map { alarms ->
            android.util.Log.d("HomeViewModel", "getAllAlarms: Received ${alarms.size} alarms from repository")
            alarms.forEachIndexed { index, alarm ->
                android.util.Log.d("HomeViewModel", "  [$index] id=${alarm.id}, label='${alarm.label}', isActive=${alarm.isActive}")
            }
            // Filter out test alarms
            val filtered = alarms.filter { !it.label.startsWith("[TEST]") }
            android.util.Log.d("HomeViewModel", "getAllAlarms: After filtering, ${filtered.size} alarms (removed ${alarms.size - filtered.size} test alarms)")
            filtered
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // StateFlow for active alarm
    val activeAlarm: StateFlow<IntervalAlarm?> = alarmRepository.getActiveAlarm()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    // StateFlow for active alarm state - optimized with shared upstream
    val activeAlarmState: StateFlow<AlarmState?> = activeAlarm
        .flatMapLatest { alarm ->
            if (alarm != null) {
                alarmStateRepository.getAlarmState(alarm.id)
            } else {
                kotlinx.coroutines.flow.flowOf(null)
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    // StateFlow for today's statistics - optimized with shared upstream
    private val _todayStatistics = MutableStateFlow<AlarmCycleStatistics?>(null)
    val todayStatistics: StateFlow<AlarmCycleStatistics?> = _todayStatistics.asStateFlow()

    // Error state
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()
    
    // Confirmation dialog state
    private val _showActivationConfirmation = MutableStateFlow(false)
    val showActivationConfirmation: StateFlow<Boolean> = _showActivationConfirmation.asStateFlow()
    
    private val _pendingActivationAlarmId = MutableStateFlow<Long?>(null)
    val pendingActivationAlarmId: StateFlow<Long?> = _pendingActivationAlarmId.asStateFlow()
    
    // Job for today statistics loading
    private var todayStatisticsJob: kotlinx.coroutines.Job? = null

    init {
        // Observe active alarm and load statistics, validate state
        viewModelScope.launch {
            try {
                activeAlarm.collect { alarm ->
                    // Cancel previous statistics job to prevent leaks
                    todayStatisticsJob?.cancel()
                    
                    if (alarm != null) {
                        // Validate and recover alarm state whenever active alarm changes
                        validateAndRecoverActiveAlarm(alarm.id)
                        
                        // Load today's statistics
                        loadTodayStatistics(alarm.id)
                    } else {
                        _todayStatistics.value = null
                    }
                }
            } catch (e: Exception) {
                appLogger.e(AppLogger.CATEGORY_ERROR, "HomeViewModel",
                    "Error observing active alarm", e)
                _errorMessage.value = "Error monitoring alarm: ${e.message}"
            }
        }
    }

    /**
     * Validates and recovers active alarm state.
     * Runs with a 2-second timeout to prevent blocking UI.
     * Called whenever active alarm is loaded or app resumes.
     * Optimized with lazy validation - only runs when active alarm exists.
     */
    private suspend fun validateAndRecoverActiveAlarm(alarmId: Long) {
        try {
            // Use withTimeout to ensure recovery completes within 2 seconds
            kotlinx.coroutines.withTimeout(2000L) {
                val validationStartTime = System.currentTimeMillis()
                
                appLogger.i(AppLogger.CATEGORY_ALARM, 
                    "HomeViewModel",
                    "Validating and recovering active alarm: id=$alarmId, timestamp=$validationStartTime")
                
                val recoveryResult = alarmStateRecoveryManager.recoverAlarmState(alarmId)
                
                val validationDuration = System.currentTimeMillis() - validationStartTime
                
                if (!recoveryResult.success) {
                    appLogger.w(AppLogger.CATEGORY_ERROR,
                        "HomeViewModel",
                        "Alarm state recovery failed: ${recoveryResult.action}, duration=${validationDuration}ms")
                    
                    // Notify user of recovery failure
                    _errorMessage.value = "Alarm state recovery issue: ${recoveryResult.action}"
                } else {
                    appLogger.i(AppLogger.CATEGORY_ALARM,
                        "HomeViewModel",
                        "Alarm state recovery successful: ${recoveryResult.action}, duration=${validationDuration}ms")
                    
                    // If recovery changed the next ring time, log it
                    if (recoveryResult.newNextRingTime != null) {
                        val currentState = activeAlarmState.value
                        if (currentState?.nextScheduledRingTime != recoveryResult.newNextRingTime) {
                            appLogger.i(AppLogger.CATEGORY_ALARM,
                                "HomeViewModel",
                                "Next ring time updated by recovery: old=${currentState?.nextScheduledRingTime}, new=${recoveryResult.newNextRingTime}")
                        }
                    }
                }
                
                // Log performance metrics
                if (validationDuration > 100) {
                    appLogger.w(AppLogger.CATEGORY_ERROR,
                        "HomeViewModel",
                        "Validation overhead exceeded target: ${validationDuration}ms (target: <100ms)")
                }
            }
        } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
            appLogger.e(AppLogger.CATEGORY_ERROR,
                "HomeViewModel",
                "Alarm state validation timed out after 2 seconds", e)
            _errorMessage.value = "Alarm validation took too long - please check alarm status"
        } catch (e: Exception) {
            appLogger.e(AppLogger.CATEGORY_ERROR,
                "HomeViewModel",
                "Error during alarm state validation", e)
            _errorMessage.value = "Error validating alarm state: ${e.message}"
        }
    }
    
    /**
     * Public function to manually trigger validation.
     * Can be called when app resumes or user pulls to refresh.
     */
    fun validateActiveAlarm() {
        viewModelScope.launch {
            val alarm = activeAlarm.value
            if (alarm != null) {
                validateAndRecoverActiveAlarm(alarm.id)
            }
        }
    }
    
    private fun loadTodayStatistics(alarmId: Long) {
        todayStatisticsJob = viewModelScope.launch {
            try {
                val stats = statisticsRepository.getTodayStatistics(alarmId)
                _todayStatistics.value = stats
            } catch (e: Exception) {
                appLogger.e(AppLogger.CATEGORY_ERROR, "HomeViewModel",
                    "Failed to load today statistics for alarm $alarmId", e)
                _errorMessage.value = "Failed to load statistics: ${e.message}"
            }
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        // Cancel any ongoing collectors
        todayStatisticsJob?.cancel()
        
        // Cancel all child coroutines in viewModelScope
        viewModelScope.coroutineContext.cancelChildren()
        
        appLogger.d(AppLogger.CATEGORY_UI, "HomeViewModel",
            "ViewModel cleared, cancelled all jobs and child coroutines")
    }

    /**
     * Handle toggle alarm action with confirmation logic
     */
    fun onToggleAlarm(alarmId: Long, shouldActivate: Boolean) {
        if (shouldActivate) {
            val currentActive = activeAlarm.value
            if (currentActive != null && currentActive.id != alarmId) {
                // Show confirmation dialog
                _showActivationConfirmation.value = true
                _pendingActivationAlarmId.value = alarmId
            } else {
                // No active alarm, activate directly
                activateAlarm(alarmId)
            }
        } else {
            deactivateAlarm(alarmId)
        }
    }
    
    /**
     * Confirm activation of pending alarm
     */
    fun confirmActivation() {
        val alarmId = _pendingActivationAlarmId.value ?: return
        dismissActivationConfirmation()
        activateAlarm(alarmId)
    }
    
    /**
     * Dismiss activation confirmation dialog
     */
    fun dismissActivationConfirmation() {
        _showActivationConfirmation.value = false
        _pendingActivationAlarmId.value = null
    }

    /**
     * Activate an alarm with single active alarm enforcement
     */
    fun activateAlarm(alarmId: Long) {
        viewModelScope.launch {
            appLogger.i(AppLogger.CATEGORY_ALARM, "HomeViewModel",
                "Attempting to activate alarm: id=$alarmId")
            
            // Use retry logic for activation
            val result = errorHandler.executeWithRetry(
                maxAttempts = 3,
                operation = "activate alarm"
            ) {
                // Check critical permissions first
                if (!permissionChecker.areAllCriticalPermissionsGranted()) {
                    val missingPermissions = permissionChecker.getMissingCriticalPermissions()
                    val permissionNames = missingPermissions.joinToString(", ") { 
                        when (it) {
                            PermissionType.NOTIFICATION -> "Notification"
                            PermissionType.EXACT_ALARM -> "Exact Alarm"
                            else -> it.name
                        }
                    }
                    throw PermissionException(
                        permissionNames,
                        "Missing permissions: $permissionNames"
                    )
                }

                // Get the alarm to activate
                val alarmToActivate = allAlarms.value.find { it.id == alarmId }
                    ?: throw IllegalArgumentException("Alarm not found: $alarmId")

                // Deactivate current active alarm if exists
                val currentActive = activeAlarm.value
                if (currentActive != null && currentActive.id != alarmId) {
                    alarmRepository.deactivateAlarm(currentActive.id)
                    alarmScheduler.cancelAlarm(currentActive.id)
                }

                // Activate the new alarm
                alarmRepository.setActiveAlarm(alarmId)
                alarmScheduler.scheduleAlarm(alarmToActivate.copy(isActive = true))
            }
            
            result.fold(
                onSuccess = {
                    _errorMessage.value = null
                    appLogger.i(AppLogger.CATEGORY_ALARM, "HomeViewModel",
                        "Successfully activated alarm: id=$alarmId")
                },
                onFailure = { exception ->
                    val errorResult = errorHandler.handleError(exception, "activate alarm")
                    _errorMessage.value = errorResult.userMessage
                    appLogger.e(AppLogger.CATEGORY_ERROR, "HomeViewModel",
                        "Failed to activate alarm: id=$alarmId", exception)
                }
            )
        }
    }

    /**
     * Deactivate the currently active alarm
     */
    fun deactivateAlarm(alarmId: Long) {
        viewModelScope.launch {
            try {
                android.util.Log.d("HomeViewModel", "deactivateAlarm: Starting deactivation for alarmId=$alarmId")
                appLogger.i(AppLogger.CATEGORY_ALARM, "HomeViewModel",
                    "Deactivating alarm: id=$alarmId")
                
                // Check if this is a test alarm and delete it instead
                val alarm = allAlarms.value.find { it.id == alarmId }
                if (alarm?.label?.startsWith("[TEST]") == true) {
                    android.util.Log.d("HomeViewModel", "deactivateAlarm: Detected test alarm, deleting instead")
                    alarmRepository.deleteAlarm(alarmId)
                    alarmScheduler.cancelAlarm(alarmId)
                    alarmStateRepository.deleteAlarmState(alarmId)
                } else {
                    alarmRepository.deactivateAlarm(alarmId)
                    android.util.Log.d("HomeViewModel", "deactivateAlarm: Repository deactivation complete")
                    
                    alarmScheduler.cancelAlarm(alarmId)
                    android.util.Log.d("HomeViewModel", "deactivateAlarm: Scheduler cancellation complete")
                }
                
                // Clean up any orphaned test alarms
                cleanupTestAlarms()
                
                _errorMessage.value = null
                appLogger.i(AppLogger.CATEGORY_ALARM, "HomeViewModel",
                    "Successfully deactivated alarm: id=$alarmId")
            } catch (e: Exception) {
                android.util.Log.e("HomeViewModel", "deactivateAlarm: Error", e)
                appLogger.e(AppLogger.CATEGORY_ERROR, "HomeViewModel",
                    "Failed to deactivate alarm: id=$alarmId", e)
                _errorMessage.value = "Failed to deactivate alarm: ${e.message}"
            }
        }
    }
    
    /**
     * Clean up any test alarms that may have been left in the database
     */
    private suspend fun cleanupTestAlarms() {
        try {
            val testAlarms = allAlarms.value.filter { it.label.startsWith("[TEST]") }
            testAlarms.forEach { alarm ->
                android.util.Log.d("HomeViewModel", "cleanupTestAlarms: Removing test alarm ${alarm.id}")
                alarmRepository.deleteAlarm(alarm.id)
                alarmStateRepository.deleteAlarmState(alarm.id)
            }
        } catch (e: Exception) {
            android.util.Log.e("HomeViewModel", "cleanupTestAlarms: Error", e)
        }
    }

    /**
     * Delete an alarm with active alarm check
     */
    fun deleteAlarm(alarmId: Long) {
        viewModelScope.launch {
            try {
                appLogger.i(AppLogger.CATEGORY_ALARM, "HomeViewModel",
                    "Attempting to delete alarm: id=$alarmId")
                
                // Check if alarm is active
                val alarm = allAlarms.value.find { it.id == alarmId }
                if (alarm?.isActive == true) {
                    _errorMessage.value = "Cannot delete active alarm. Please deactivate it first."
                    appLogger.w(AppLogger.CATEGORY_ALARM, "HomeViewModel",
                        "Cannot delete active alarm: id=$alarmId")
                    return@launch
                }

                // Delete the alarm
                alarmRepository.deleteAlarm(alarmId)
                alarmStateRepository.deleteAlarmState(alarmId)
                statisticsRepository.deleteStatisticsForAlarm(alarmId)
                
                _errorMessage.value = null
                appLogger.i(AppLogger.CATEGORY_ALARM, "HomeViewModel",
                    "Successfully deleted alarm: id=$alarmId")
            } catch (e: Exception) {
                appLogger.e(AppLogger.CATEGORY_ERROR, "HomeViewModel",
                    "Failed to delete alarm: id=$alarmId", e)
                _errorMessage.value = "Failed to delete alarm: ${e.message}"
            }
        }
    }

    /**
     * Pause the active alarm with duration options
     */
    fun pauseAlarm(pauseDurationMillis: Long) {
        viewModelScope.launch {
            try {
                val alarm = activeAlarm.value
                if (alarm == null) {
                    _errorMessage.value = "No active alarm to pause"
                    appLogger.w(AppLogger.CATEGORY_ALARM, "HomeViewModel",
                        "Cannot pause: no active alarm")
                    return@launch
                }

                appLogger.i(AppLogger.CATEGORY_ALARM, "HomeViewModel",
                    "Pausing alarm: id=${alarm.id}, duration=${pauseDurationMillis}ms")
                alarmScheduler.pauseAlarm(alarm.id, pauseDurationMillis)
                _errorMessage.value = null
            } catch (e: Exception) {
                appLogger.e(AppLogger.CATEGORY_ERROR, "HomeViewModel",
                    "Failed to pause alarm", e)
                _errorMessage.value = "Failed to pause alarm: ${e.message}"
            }
        }
    }

    /**
     * Resume the active alarm
     */
    fun resumeAlarm() {
        viewModelScope.launch {
            try {
                val alarm = activeAlarm.value
                if (alarm == null) {
                    _errorMessage.value = "No active alarm to resume"
                    appLogger.w(AppLogger.CATEGORY_ALARM, "HomeViewModel",
                        "Cannot resume: no active alarm")
                    return@launch
                }

                appLogger.i(AppLogger.CATEGORY_ALARM, "HomeViewModel",
                    "Resuming alarm: id=${alarm.id}")
                alarmScheduler.resumeAlarm(alarm.id)
                _errorMessage.value = null
            } catch (e: Exception) {
                appLogger.e(AppLogger.CATEGORY_ERROR, "HomeViewModel",
                    "Failed to resume alarm", e)
                _errorMessage.value = "Failed to resume alarm: ${e.message}"
            }
        }
    }

    /**
     * Clear error message
     */
    fun clearError() {
        _errorMessage.value = null
    }
}
