package com.lettingin.intervalAlarm.data.repository

import com.lettingin.intervalAlarm.data.model.AlarmCycleStatistics
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

interface StatisticsRepository {
    fun getStatisticsForAlarm(alarmId: Long): Flow<List<AlarmCycleStatistics>>
    suspend fun insertStatistics(statistics: AlarmCycleStatistics): Long
    suspend fun updateStatistics(statistics: AlarmCycleStatistics)
    suspend fun getTodayStatistics(alarmId: Long): AlarmCycleStatistics?
    suspend fun cleanupOldStatistics(alarmId: Long, keepCount: Int = 5)
    suspend fun deleteStatisticsForAlarm(alarmId: Long)
}
