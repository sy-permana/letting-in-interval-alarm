package com.lettingin.intervalAlarm.ui.statistics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lettingin.intervalAlarm.data.model.AlarmCycleStatistics
import com.lettingin.intervalAlarm.data.repository.StatisticsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.launch
import kotlinx.coroutines.cancelChildren
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

/**
 * Formatted statistics data for display
 */
data class FormattedStatistics(
    val cycleDate: String,
    val totalRings: Int,
    val userDismissals: Int,
    val autoDismissals: Int,
    val dismissalRate: String,
    val autoDismissalRate: String
)

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@HiltViewModel
class StatisticsViewModel @Inject constructor(
    private val statisticsRepository: StatisticsRepository,
    private val appLogger: com.lettingin.intervalAlarm.util.AppLogger
) : ViewModel() {

    // Current alarm ID being viewed
    private val _currentAlarmId = MutableStateFlow<Long?>(null)
    
    // StateFlow for alarm statistics (last 5 cycles) - optimized with shared upstream
    val statistics: StateFlow<List<AlarmCycleStatistics>> = _currentAlarmId
        .flatMapLatest { alarmId ->
            if (alarmId != null) {
                statisticsRepository.getStatisticsForAlarm(alarmId)
                    .map { it.take(5) } // Keep only last 5 cycles
            } else {
                kotlinx.coroutines.flow.flowOf(emptyList())
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // StateFlow for formatted statistics - derived from statistics
    val formattedStatistics: StateFlow<List<FormattedStatistics>> = statistics
        .map { formatStatistics(it) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Error state
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // Loading state - derived from whether we have an alarm ID set
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    /**
     * Load statistics for a specific alarm
     */
    fun loadStatistics(alarmId: Long) {
        appLogger.d(com.lettingin.intervalAlarm.util.AppLogger.CATEGORY_UI,
            "StatisticsViewModel", "Loading statistics for alarm $alarmId")
        
        _isLoading.value = true
        _currentAlarmId.value = alarmId
        
        // Loading state will be cleared when statistics flow emits
        viewModelScope.launch {
            try {
                // Wait for first emission with timeout
                kotlinx.coroutines.withTimeout(5000L) {
                    statistics.first { it.isNotEmpty() || _currentAlarmId.value != alarmId }
                    _isLoading.value = false
                    appLogger.d(com.lettingin.intervalAlarm.util.AppLogger.CATEGORY_UI,
                        "StatisticsViewModel", "Statistics loaded successfully")
                }
            } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
                appLogger.w(com.lettingin.intervalAlarm.util.AppLogger.CATEGORY_UI,
                    "StatisticsViewModel", "Statistics loading timed out, but flow will continue")
                _isLoading.value = false
            } catch (e: Exception) {
                appLogger.e(com.lettingin.intervalAlarm.util.AppLogger.CATEGORY_ERROR,
                    "StatisticsViewModel", "Error loading statistics", e)
                _errorMessage.value = "Failed to load statistics: ${e.message}"
                _isLoading.value = false
            }
        }
    }

    /**
     * Format statistics data for display
     */
    private fun formatStatistics(stats: List<AlarmCycleStatistics>): List<FormattedStatistics> {
        val dateFormatter = DateTimeFormatter.ofPattern("MMM dd, yyyy")
        
        return stats.map { stat ->
            val dismissalRate = if (stat.totalRings > 0) {
                val rate = (stat.userDismissals.toDouble() / stat.totalRings.toDouble()) * 100
                String.format("%.1f%%", rate)
            } else {
                "N/A"
            }

            val autoDismissalRate = if (stat.totalRings > 0) {
                val rate = (stat.autoDismissals.toDouble() / stat.totalRings.toDouble()) * 100
                String.format("%.1f%%", rate)
            } else {
                "N/A"
            }

            FormattedStatistics(
                cycleDate = stat.cycleDate.format(dateFormatter),
                totalRings = stat.totalRings,
                userDismissals = stat.userDismissals,
                autoDismissals = stat.autoDismissals,
                dismissalRate = dismissalRate,
                autoDismissalRate = autoDismissalRate
            )
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
            "StatisticsViewModel", "ViewModel cleared, cancelled all child coroutines")
    }
}
