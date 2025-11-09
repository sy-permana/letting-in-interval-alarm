package com.lettingin.intervalAlarm.ui.statistics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lettingin.intervalAlarm.data.model.AlarmCycleStatistics
import com.lettingin.intervalAlarm.data.repository.StatisticsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
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

@HiltViewModel
class StatisticsViewModel @Inject constructor(
    private val statisticsRepository: StatisticsRepository,
    private val appLogger: com.lettingin.intervalAlarm.util.AppLogger
) : ViewModel() {

    // StateFlow for alarm statistics (last 5 cycles)
    private val _statistics = MutableStateFlow<List<AlarmCycleStatistics>>(emptyList())
    val statistics: StateFlow<List<AlarmCycleStatistics>> = _statistics.asStateFlow()

    // StateFlow for formatted statistics
    private val _formattedStatistics = MutableStateFlow<List<FormattedStatistics>>(emptyList())
    val formattedStatistics: StateFlow<List<FormattedStatistics>> = _formattedStatistics.asStateFlow()

    // Error state
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // Loading state
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    /**
     * Load statistics for a specific alarm
     */
    fun loadStatistics(alarmId: Long) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                appLogger.d(com.lettingin.intervalAlarm.util.AppLogger.CATEGORY_UI,
                    "StatisticsViewModel", "Loading statistics for alarm $alarmId")
                statisticsRepository.getStatisticsForAlarm(alarmId).collect { stats ->
                    _statistics.value = stats.take(5) // Keep only last 5 cycles
                    _formattedStatistics.value = formatStatistics(stats.take(5))
                    _isLoading.value = false
                    appLogger.d(com.lettingin.intervalAlarm.util.AppLogger.CATEGORY_UI,
                        "StatisticsViewModel", "Loaded ${stats.size} statistics entries")
                }
            } catch (e: Exception) {
                appLogger.e(com.lettingin.intervalAlarm.util.AppLogger.CATEGORY_ERROR,
                    "StatisticsViewModel", "Failed to load statistics for alarm $alarmId", e)
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
}
