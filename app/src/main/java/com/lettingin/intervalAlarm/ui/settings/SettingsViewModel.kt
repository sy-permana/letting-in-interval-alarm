package com.lettingin.intervalAlarm.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lettingin.intervalAlarm.data.model.AppSettings
import com.lettingin.intervalAlarm.data.model.NotificationType
import com.lettingin.intervalAlarm.data.model.ThemeMode
import com.lettingin.intervalAlarm.data.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.cancelChildren
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val appLogger: com.lettingin.intervalAlarm.util.AppLogger
) : ViewModel() {

    // StateFlow for app settings
    val settings: StateFlow<AppSettings?> = settingsRepository.getSettings()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    // Error state
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // Loading state
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        // Initialize settings if they don't exist
        viewModelScope.launch {
            try {
                appLogger.d(com.lettingin.intervalAlarm.util.AppLogger.CATEGORY_UI, 
                    "SettingsViewModel", "Initializing settings")
                val currentSettings = settingsRepository.getSettingsSync()
                if (currentSettings == null) {
                    appLogger.i(com.lettingin.intervalAlarm.util.AppLogger.CATEGORY_UI,
                        "SettingsViewModel", "Creating default settings")
                    settingsRepository.updateSettings(AppSettings())
                }
            } catch (e: Exception) {
                appLogger.e(com.lettingin.intervalAlarm.util.AppLogger.CATEGORY_ERROR,
                    "SettingsViewModel", "Failed to initialize settings", e)
                _errorMessage.value = "Failed to initialize settings: ${e.message}"
            }
        }
    }

    /**
     * Update default interval duration
     */
    fun updateDefaultInterval(minutes: Int) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                appLogger.d(com.lettingin.intervalAlarm.util.AppLogger.CATEGORY_UI,
                    "SettingsViewModel", "Updating default interval to $minutes minutes")
                val currentSettings = settings.value ?: AppSettings()
                
                // Validate interval (minimum 5 minutes)
                if (minutes < 5) {
                    appLogger.w(com.lettingin.intervalAlarm.util.AppLogger.CATEGORY_UI,
                        "SettingsViewModel", "Invalid interval: $minutes (minimum 5)")
                    _errorMessage.value = "Interval must be at least 5 minutes"
                    _isLoading.value = false
                    return@launch
                }

                val updatedSettings = currentSettings.copy(defaultIntervalMinutes = minutes)
                settingsRepository.updateSettings(updatedSettings)
                
                _errorMessage.value = null
                _isLoading.value = false
                appLogger.i(com.lettingin.intervalAlarm.util.AppLogger.CATEGORY_UI,
                    "SettingsViewModel", "Successfully updated default interval")
            } catch (e: Exception) {
                appLogger.e(com.lettingin.intervalAlarm.util.AppLogger.CATEGORY_ERROR,
                    "SettingsViewModel", "Failed to update default interval", e)
                _errorMessage.value = "Failed to update default interval: ${e.message}"
                _isLoading.value = false
            }
        }
    }

    /**
     * Update default notification type
     */
    fun updateDefaultNotificationType(type: NotificationType) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                appLogger.d(com.lettingin.intervalAlarm.util.AppLogger.CATEGORY_UI,
                    "SettingsViewModel", "Updating notification type to $type")
                val currentSettings = settings.value ?: AppSettings()
                val updatedSettings = currentSettings.copy(defaultNotificationType = type)
                settingsRepository.updateSettings(updatedSettings)
                
                _errorMessage.value = null
                _isLoading.value = false
                appLogger.i(com.lettingin.intervalAlarm.util.AppLogger.CATEGORY_UI,
                    "SettingsViewModel", "Successfully updated notification type")
            } catch (e: Exception) {
                appLogger.e(com.lettingin.intervalAlarm.util.AppLogger.CATEGORY_ERROR,
                    "SettingsViewModel", "Failed to update notification type", e)
                _errorMessage.value = "Failed to update notification type: ${e.message}"
                _isLoading.value = false
            }
        }
    }

    /**
     * Update theme mode
     */
    fun updateThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                appLogger.d(com.lettingin.intervalAlarm.util.AppLogger.CATEGORY_UI,
                    "SettingsViewModel", "Updating theme mode to $mode")
                val currentSettings = settings.value ?: AppSettings()
                val updatedSettings = currentSettings.copy(themeMode = mode)
                settingsRepository.updateSettings(updatedSettings)
                
                _errorMessage.value = null
                _isLoading.value = false
                appLogger.i(com.lettingin.intervalAlarm.util.AppLogger.CATEGORY_UI,
                    "SettingsViewModel", "Successfully updated theme mode")
            } catch (e: Exception) {
                appLogger.e(com.lettingin.intervalAlarm.util.AppLogger.CATEGORY_ERROR,
                    "SettingsViewModel", "Failed to update theme mode", e)
                _errorMessage.value = "Failed to update theme mode: ${e.message}"
                _isLoading.value = false
            }
        }
    }

    /**
     * Clear error message
     */
    fun clearError() {
        _errorMessage.value = null
    }
    
    override fun onCleared() {
        super.onCleared()
        
        // Cancel all child coroutines in viewModelScope
        viewModelScope.coroutineContext.cancelChildren()
        
        appLogger.d(com.lettingin.intervalAlarm.util.AppLogger.CATEGORY_UI,
            "SettingsViewModel", "ViewModel cleared, cancelled all child coroutines")
    }
}
