package com.lettingin.intervalAlarm.data.repository

import com.lettingin.intervalAlarm.data.database.StatisticsDao
import com.lettingin.intervalAlarm.data.model.AlarmCycleStatistics
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StatisticsRepositoryImpl @Inject constructor(
    private val statisticsDao: StatisticsDao,
    appLogger: com.lettingin.intervalAlarm.util.AppLogger
) : SafeRepository(appLogger), StatisticsRepository {
    
    override fun getStatisticsForAlarm(alarmId: Long): Flow<List<AlarmCycleStatistics>> {
        return statisticsDao.getStatisticsForAlarm(alarmId, limit = 5)
    }
    
    override suspend fun insertStatistics(statistics: AlarmCycleStatistics): Long {
        val result = safeDbOperation("insertStatistics(alarmId=${statistics.alarmId})") {
            statisticsDao.insertStatistics(statistics)
        }
        
        return result.getOrElse { throw it }
    }
    
    override suspend fun updateStatistics(statistics: AlarmCycleStatistics) {
        safeDbOperationUnit("updateStatistics(alarmId=${statistics.alarmId})") {
            statisticsDao.updateStatistics(statistics)
        }
    }
    
    override suspend fun getTodayStatistics(alarmId: Long): AlarmCycleStatistics? {
        val result = safeDbOperation("getTodayStatistics(alarmId=$alarmId)") {
            statisticsDao.getStatisticsForDate(alarmId, LocalDate.now())
        }
        
        return result.getOrElse { throw it }
    }
    
    override suspend fun cleanupOldStatistics(alarmId: Long, keepCount: Int) {
        safeDbOperationUnit("cleanupOldStatistics(alarmId=$alarmId, keepCount=$keepCount)") {
            statisticsDao.cleanupOldStatistics(alarmId, keepCount)
        }
    }
    
    override suspend fun deleteStatisticsForAlarm(alarmId: Long) {
        safeDbOperationUnit("deleteStatisticsForAlarm(alarmId=$alarmId)") {
            statisticsDao.deleteStatisticsForAlarm(alarmId)
        }
    }
}
