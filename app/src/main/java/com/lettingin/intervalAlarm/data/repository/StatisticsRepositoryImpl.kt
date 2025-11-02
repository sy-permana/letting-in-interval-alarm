package com.lettingin.intervalAlarm.data.repository

import com.lettingin.intervalAlarm.data.database.StatisticsDao
import com.lettingin.intervalAlarm.data.model.AlarmCycleStatistics
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StatisticsRepositoryImpl @Inject constructor(
    private val statisticsDao: StatisticsDao
) : StatisticsRepository {
    
    override fun getStatisticsForAlarm(alarmId: Long): Flow<List<AlarmCycleStatistics>> {
        return statisticsDao.getStatisticsForAlarm(alarmId, limit = 5)
    }
    
    override suspend fun insertStatistics(statistics: AlarmCycleStatistics): Long {
        return try {
            statisticsDao.insertStatistics(statistics)
        } catch (e: Exception) {
            throw RepositoryException("Failed to insert statistics", e)
        }
    }
    
    override suspend fun updateStatistics(statistics: AlarmCycleStatistics) {
        try {
            statisticsDao.updateStatistics(statistics)
        } catch (e: Exception) {
            throw RepositoryException("Failed to update statistics", e)
        }
    }
    
    override suspend fun getTodayStatistics(alarmId: Long): AlarmCycleStatistics? {
        return try {
            statisticsDao.getStatisticsForDate(alarmId, LocalDate.now())
        } catch (e: Exception) {
            throw RepositoryException("Failed to get today's statistics", e)
        }
    }
    
    override suspend fun cleanupOldStatistics(alarmId: Long, keepCount: Int) {
        try {
            statisticsDao.cleanupOldStatistics(alarmId, keepCount)
        } catch (e: Exception) {
            throw RepositoryException("Failed to cleanup old statistics", e)
        }
    }
    
    override suspend fun deleteStatisticsForAlarm(alarmId: Long) {
        try {
            statisticsDao.deleteStatisticsForAlarm(alarmId)
        } catch (e: Exception) {
            throw RepositoryException("Failed to delete statistics for alarm", e)
        }
    }
}
