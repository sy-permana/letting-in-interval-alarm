package com.lettingin.intervalAlarm.ui.editor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lettingin.intervalAlarm.data.model.AppSettings
import com.lettingin.intervalAlarm.data.model.IntervalAlarm
import com.lettingin.intervalAlarm.data.model.NotificationType
import com.lettingin.intervalAlarm.data.repository.AlarmRepository
import com.lettingin.intervalAlarm.data.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.cancelChildren
import java.time.DayOfWeek
import java.time.LocalTime
import javax.inject.Inject

data class ValidationResult(
    val isValid: Boolean,
    val errors: Map<String, String> = emptyMap()
)

@HiltViewModel
class AlarmEditorViewModel @Inject constructor(
    private val alarmRepository: AlarmRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    // StateFlow for default settings
    val defaultSettings: StateFlow<AppSettings?> = settingsRepository.getSettings()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    // StateFlow for alarm being edited - initialize with default new alarm
    private val _alarmState = MutableStateFlow<IntervalAlarm?>(createDefaultAlarm())
    val alarmState: StateFlow<IntervalAlarm?> = _alarmState.asStateFlow()

    // Maximum interval based on time range
    private val _maxInterval = MutableStateFlow(480) // Default 8 hours
    val maxInterval: StateFlow<Int> = _maxInterval.asStateFlow()

    // Validation state
    private val _validationResult = MutableStateFlow(ValidationResult(isValid = true))
    val validationResult: StateFlow<ValidationResult> = _validationResult.asStateFlow()

    // Loading state
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // Success state
    private val _saveSuccess = MutableStateFlow(false)
    val saveSuccess: StateFlow<Boolean> = _saveSuccess.asStateFlow()

    /**
     * Create a default alarm with standard settings
     */
    private fun createDefaultAlarm(): IntervalAlarm {
        return IntervalAlarm(
            startTime = LocalTime.of(9, 0),
            endTime = LocalTime.of(17, 0),
            intervalMinutes = 30, // Default to 30 minutes
            selectedDays = setOf(
                DayOfWeek.MONDAY,
                DayOfWeek.TUESDAY,
                DayOfWeek.WEDNESDAY,
                DayOfWeek.THURSDAY,
                DayOfWeek.FRIDAY
            ),
            isRepeatable = true,
            notificationType = NotificationType.FULL_SCREEN,
            ringtoneUri = "content://settings/system/alarm_alert"
        )
    }

    // Job for alarm loading collector
    private var loadAlarmJob: kotlinx.coroutines.Job? = null

    /**
     * Load an existing alarm for editing or apply user settings to new alarm
     */
    fun loadAlarm(alarmId: Long?) {
        // Cancel previous load job to prevent leaks
        loadAlarmJob?.cancel()
        
        loadAlarmJob = viewModelScope.launch {
            try {
                if (alarmId != null) {
                    // Load existing alarm - show loading state
                    _isLoading.value = true
                    android.util.Log.d("AlarmEditorViewModel", "loadAlarm: Loading alarm $alarmId")
                    
                    // Use withTimeout to prevent hanging
                    kotlinx.coroutines.withTimeout(5000L) {
                        alarmRepository.getAlarmById(alarmId).collect { alarm ->
                            _alarmState.value = alarm
                            recalculateMaxInterval()
                            _isLoading.value = false
                            android.util.Log.d("AlarmEditorViewModel", "loadAlarm: Successfully loaded alarm $alarmId")
                        }
                    }
                } else {
                    // For new alarms, apply user's default settings if available
                    // This happens in the background without blocking UI
                    defaultSettings.value?.let { settings ->
                        _alarmState.value = _alarmState.value?.copy(
                            intervalMinutes = settings.defaultIntervalMinutes,
                            notificationType = settings.defaultNotificationType
                        )
                        android.util.Log.d("AlarmEditorViewModel", "loadAlarm: Applied default settings")
                    }
                    recalculateMaxInterval()
                }
            } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
                android.util.Log.e("AlarmEditorViewModel", "loadAlarm: Timeout loading alarm $alarmId", e)
                _isLoading.value = false
                _validationResult.value = ValidationResult(
                    isValid = false,
                    errors = mapOf("general" to "Loading alarm timed out")
                )
            } catch (e: Exception) {
                android.util.Log.e("AlarmEditorViewModel", "loadAlarm: Failed to load alarm $alarmId", e)
                _isLoading.value = false
                _validationResult.value = ValidationResult(
                    isValid = false,
                    errors = mapOf("general" to "Failed to load alarm: ${e.message}")
                )
            }
        }
    }

    /**
     * Update alarm label
     */
    fun updateLabel(label: String) {
        _alarmState.value = _alarmState.value?.copy(label = label)
    }

    /**
     * Update start time
     */
    fun updateStartTime(time: LocalTime) {
        _alarmState.value = _alarmState.value?.copy(startTime = time)
        recalculateMaxInterval()
    }

    /**
     * Update end time
     */
    fun updateEndTime(time: LocalTime) {
        _alarmState.value = _alarmState.value?.copy(endTime = time)
        recalculateMaxInterval()
    }

    /**
     * Recalculate maximum interval based on time range
     * Capped at 12 hours (720 minutes) or the time range, whichever is smaller
     */
    private fun recalculateMaxInterval() {
        _alarmState.value?.let { alarm ->
            val duration = java.time.Duration.between(alarm.startTime, alarm.endTime)
            val timeRangeMinutes = if (duration.isNegative) {
                // End time is next day
                java.time.Duration.between(alarm.startTime, LocalTime.MAX)
                    .plus(java.time.Duration.between(LocalTime.MIN, alarm.endTime))
                    .toMinutes()
            } else {
                duration.toMinutes()
            }.toInt()
            
            // Cap at 12 hours (720 minutes) or time range, whichever is smaller
            val maxMinutes = minOf(timeRangeMinutes, 720)
            
            _maxInterval.value = maxMinutes
            
            // Adjust current interval if it exceeds new max
            if (alarm.intervalMinutes > maxMinutes) {
                _alarmState.value = alarm.copy(intervalMinutes = maxMinutes)
            }
        }
    }

    /**
     * Update interval in minutes
     */
    fun updateInterval(minutes: Int) {
        _alarmState.value = _alarmState.value?.copy(intervalMinutes = minutes)
    }

    /**
     * Toggle a day of the week
     */
    fun toggleDay(day: DayOfWeek) {
        _alarmState.value?.let { alarm ->
            val newDays = if (alarm.selectedDays.contains(day)) {
                alarm.selectedDays - day
            } else {
                alarm.selectedDays + day
            }
            _alarmState.value = alarm.copy(selectedDays = newDays)
        }
    }

    /**
     * Update notification type
     */
    fun updateNotificationType(type: NotificationType) {
        _alarmState.value = _alarmState.value?.copy(notificationType = type)
    }

    /**
     * Update ringtone URI
     */
    fun updateRingtone(uri: String) {
        _alarmState.value = _alarmState.value?.copy(ringtoneUri = uri)
    }

    /**
     * Update cycle type (repeatable or one-cycle)
     */
    fun updateCycleType(isRepeatable: Boolean) {
        _alarmState.value = _alarmState.value?.copy(isRepeatable = isRepeatable)
    }

    /**
     * Validate the alarm configuration
     */
    fun validateAlarm(): ValidationResult {
        val alarm = _alarmState.value
        if (alarm == null) {
            return ValidationResult(
                isValid = false,
                errors = mapOf("general" to "No alarm data to validate")
            )
        }

        val errors = mutableMapOf<String, String>()

        // Validate label length (max 60 characters)
        if (alarm.label.length > 60) {
            errors["label"] = "Label must be 60 characters or less"
        }

        // Validate start time is before end time
        if (!alarm.startTime.isBefore(alarm.endTime)) {
            errors["time"] = "Start time must be before end time"
        }

        // Calculate time range in minutes
        val timeRangeMinutes = java.time.Duration.between(alarm.startTime, alarm.endTime).toMinutes()

        // Validate interval is between 5 minutes and time range
        if (alarm.intervalMinutes < 5) {
            errors["interval"] = "Interval must be at least 5 minutes"
        } else if (alarm.intervalMinutes > timeRangeMinutes) {
            errors["interval"] = "Interval cannot exceed time range (${timeRangeMinutes} minutes)"
        }

        // Validate at least one day is selected
        if (alarm.selectedDays.isEmpty()) {
            errors["days"] = "At least one day must be selected"
        }

        val result = ValidationResult(
            isValid = errors.isEmpty(),
            errors = errors
        )
        _validationResult.value = result
        return result
    }

    /**
     * Save the alarm with validation
     */
    fun saveAlarm() {
        viewModelScope.launch {
            _isLoading.value = true
            _saveSuccess.value = false

            try {
                android.util.Log.d("AlarmEditorViewModel", "saveAlarm: Starting save operation")
                
                // Validate alarm
                val validation = validateAlarm()
                if (!validation.isValid) {
                    android.util.Log.w("AlarmEditorViewModel", "saveAlarm: Validation failed: ${validation.errors}")
                    _isLoading.value = false
                    return@launch
                }

                val alarm = _alarmState.value
                if (alarm == null) {
                    android.util.Log.e("AlarmEditorViewModel", "saveAlarm: No alarm data to save")
                    _validationResult.value = ValidationResult(
                        isValid = false,
                        errors = mapOf("general" to "No alarm data to save")
                    )
                    _isLoading.value = false
                    return@launch
                }

                // Check alarm count limit (max 10) for new alarms
                if (alarm.id == 0L) {
                    val count = alarmRepository.getAlarmCount()
                    android.util.Log.d("AlarmEditorViewModel", "saveAlarm: Current alarm count: $count")
                    if (count >= 10) {
                        android.util.Log.w("AlarmEditorViewModel", "saveAlarm: Maximum alarm count reached")
                        _validationResult.value = ValidationResult(
                            isValid = false,
                            errors = mapOf("general" to "Maximum of 10 alarms allowed")
                        )
                        _isLoading.value = false
                        return@launch
                    }
                }

                // Check if trying to edit active alarm
                if (alarm.id != 0L && alarm.isActive) {
                    android.util.Log.w("AlarmEditorViewModel", "saveAlarm: Cannot edit active alarm ${alarm.id}")
                    _validationResult.value = ValidationResult(
                        isValid = false,
                        errors = mapOf("general" to "Cannot edit active alarm. Please deactivate it first.")
                    )
                    _isLoading.value = false
                    return@launch
                }

                // Save the alarm
                val updatedAlarm = alarm.copy(updatedAt = System.currentTimeMillis())
                if (alarm.id == 0L) {
                    android.util.Log.d("AlarmEditorViewModel", "saveAlarm: Inserting new alarm")
                    alarmRepository.insertAlarm(updatedAlarm)
                } else {
                    android.util.Log.d("AlarmEditorViewModel", "saveAlarm: Updating alarm ${alarm.id}")
                    alarmRepository.updateAlarm(updatedAlarm)
                }

                _saveSuccess.value = true
                _isLoading.value = false
                android.util.Log.d("AlarmEditorViewModel", "saveAlarm: Save successful")
            } catch (e: Exception) {
                android.util.Log.e("AlarmEditorViewModel", "saveAlarm: Failed to save alarm", e)
                _validationResult.value = ValidationResult(
                    isValid = false,
                    errors = mapOf("general" to "Failed to save alarm: ${e.message}")
                )
                _isLoading.value = false
            }
        }
    }

    /**
     * Reset save success state
     */
    fun resetSaveSuccess() {
        _saveSuccess.value = false
    }

    /**
     * Clear validation errors
     */
    fun clearValidation() {
        _validationResult.value = ValidationResult(isValid = true)
    }

    // Job for test alarm cleanup
    private var testAlarmCleanupJob: kotlinx.coroutines.Job? = null
    
    // Job for ringtone preview
    private var ringtonePreviewJob: kotlinx.coroutines.Job? = null

    /**
     * Test alarm - triggers alarm in 5 seconds for testing purposes
     */
    fun testAlarm(context: android.content.Context) {
        viewModelScope.launch {
            try {
                android.util.Log.d("AlarmEditorViewModel", "testAlarm: Starting test alarm")
                
                val alarm = _alarmState.value
                if (alarm == null) {
                    android.util.Log.e("AlarmEditorViewModel", "testAlarm: No alarm data")
                    _validationResult.value = ValidationResult(
                        isValid = false,
                        errors = mapOf("general" to "No alarm data to test")
                    )
                    return@launch
                }

                // Create a test alarm that rings in 5 seconds
                val testAlarmId = System.currentTimeMillis() // Use timestamp as unique ID
                
                // Ensure ringtone URI is valid (not a fake string)
                val validRingtoneUri = if (alarm.ringtoneUri.startsWith("content://") || 
                                          alarm.ringtoneUri.startsWith("android.resource://")) {
                    alarm.ringtoneUri
                } else {
                    // Use system default if current URI is invalid
                    android.provider.Settings.System.DEFAULT_ALARM_ALERT_URI?.toString()
                        ?: "content://settings/system/alarm_alert"
                }
                
                // Create test alarm with time window that covers current time + 5 seconds
                val now = java.time.LocalTime.now()
                val testStartTime = now.minusMinutes(5) // Start 5 minutes ago
                val testEndTime = now.plusMinutes(10)   // End 10 minutes from now
                
                val testAlarm = alarm.copy(
                    id = testAlarmId,
                    isActive = true,
                    ringtoneUri = validRingtoneUri,
                    startTime = testStartTime,
                    endTime = testEndTime,
                    label = "[TEST] ${alarm.label}"
                )

                android.util.Log.d("AlarmEditorViewModel", "testAlarm: Test alarm ID = $testAlarmId")
                android.util.Log.d("AlarmEditorViewModel", "testAlarm: Time window = $testStartTime to $testEndTime")
                android.util.Log.d("AlarmEditorViewModel", "testAlarm: Ringtone = $validRingtoneUri")

                // Save test alarm temporarily to database for the receiver to find
                alarmRepository.insertAlarm(testAlarm)
                android.util.Log.d("AlarmEditorViewModel", "testAlarm: Saved to database")

                // Schedule test alarm using AlarmManager directly
                val alarmManager = context.getSystemService(android.content.Context.ALARM_SERVICE) as android.app.AlarmManager
                val intent = android.content.Intent(context, com.lettingin.intervalAlarm.receiver.AlarmReceiver::class.java).apply {
                    putExtra(com.lettingin.intervalAlarm.domain.scheduler.AlarmSchedulerImpl.EXTRA_ALARM_ID, testAlarmId)
                    putExtra("IS_TEST_ALARM", true) // Mark as test alarm
                }
                val pendingIntent = android.app.PendingIntent.getBroadcast(
                    context,
                    testAlarmId.toInt(),
                    intent,
                    android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
                )

                val triggerTime = System.currentTimeMillis() + 5000 // 5 seconds from now
                android.util.Log.d("AlarmEditorViewModel", "testAlarm: Trigger time = ${java.util.Date(triggerTime)}")
                android.util.Log.d("AlarmEditorViewModel", "testAlarm: Current time = ${java.util.Date()}")

                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                    if (alarmManager.canScheduleExactAlarms()) {
                        alarmManager.setExactAndAllowWhileIdle(
                            android.app.AlarmManager.RTC_WAKEUP,
                            triggerTime,
                            pendingIntent
                        )
                        android.util.Log.d("AlarmEditorViewModel", "testAlarm: Scheduled with setExactAndAllowWhileIdle")
                    } else {
                        android.util.Log.e("AlarmEditorViewModel", "testAlarm: Cannot schedule exact alarms")
                        _validationResult.value = ValidationResult(
                            isValid = false,
                            errors = mapOf("general" to "Exact alarm permission required. Go to Settings to enable it.")
                        )
                        // Clean up test alarm
                        alarmRepository.deleteAlarm(testAlarmId)
                        return@launch
                    }
                } else {
                    alarmManager.setExactAndAllowWhileIdle(
                        android.app.AlarmManager.RTC_WAKEUP,
                        triggerTime,
                        pendingIntent
                    )
                    android.util.Log.d("AlarmEditorViewModel", "testAlarm: Scheduled (pre-Android 12)")
                }

                // Schedule cleanup of test alarm after 10 seconds
                testAlarmCleanupJob?.cancel()
                testAlarmCleanupJob = viewModelScope.launch {
                    try {
                        kotlinx.coroutines.delay(10000)
                        alarmRepository.deleteAlarm(testAlarmId)
                        android.util.Log.d("AlarmEditorViewModel", "testAlarm: Cleaned up test alarm $testAlarmId")
                    } catch (e: Exception) {
                        android.util.Log.e("AlarmEditorViewModel", "testAlarm: Failed to clean up $testAlarmId", e)
                    }
                }

                _validationResult.value = ValidationResult(
                    isValid = true,
                    errors = mapOf("success" to "Test alarm will ring in 5 seconds! 🔔")
                )
                android.util.Log.d("AlarmEditorViewModel", "testAlarm: Success!")
            } catch (e: Exception) {
                android.util.Log.e("AlarmEditorViewModel", "testAlarm: Error", e)
                _validationResult.value = ValidationResult(
                    isValid = false,
                    errors = mapOf("general" to "Failed to test alarm: ${e.message}")
                )
            }
        }
    }

    /**
     * Preview ringtone - plays selected ringtone for 3 seconds
     */
    fun previewRingtone(context: android.content.Context) {
        // Cancel previous preview to prevent overlapping sounds
        ringtonePreviewJob?.cancel()
        
        ringtonePreviewJob = viewModelScope.launch {
            try {
                android.util.Log.d("AlarmEditorViewModel", "previewRingtone: Starting ringtone preview")
                val alarm = _alarmState.value
                if (alarm == null) {
                    android.util.Log.w("AlarmEditorViewModel", "previewRingtone: No alarm data")
                    return@launch
                }

                val ringtoneManager = com.lettingin.intervalAlarm.util.RingtoneManagerImpl(context)
                ringtoneManager.playRingtone(alarm.ringtoneUri, 1.0f)
                android.util.Log.d("AlarmEditorViewModel", "previewRingtone: Playing ringtone ${alarm.ringtoneUri}")

                // Stop after 3 seconds
                kotlinx.coroutines.delay(3000)
                ringtoneManager.stopRingtone()
                android.util.Log.d("AlarmEditorViewModel", "previewRingtone: Stopped ringtone")
            } catch (e: Exception) {
                android.util.Log.e("AlarmEditorViewModel", "previewRingtone: Failed", e)
                _validationResult.value = ValidationResult(
                    isValid = false,
                    errors = mapOf("general" to "Failed to preview ringtone: ${e.message}")
                )
            }
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        // Cancel all ongoing jobs
        loadAlarmJob?.cancel()
        testAlarmCleanupJob?.cancel()
        ringtonePreviewJob?.cancel()
        
        // Cancel all child coroutines in viewModelScope
        viewModelScope.coroutineContext.cancelChildren()
        
        android.util.Log.d("AlarmEditorViewModel", "onCleared: Cancelled all jobs and child coroutines")
    }
}
