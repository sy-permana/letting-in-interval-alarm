package com.lettingin.intervalAlarm.data.database

import androidx.room.*
import com.lettingin.intervalAlarm.data.model.AlarmCycleStatistics
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface StatisticsDao {
    
    @Query("SELECT * FROM alarm_statistics WHERE alarmId = :alarmId ORDER BY cycleDate DESC LIMIT :limit")
    fun getStatisticsForAlarm(alarmId: Long, limit: Int = 5): Flow<List<AlarmCycleStatistics>>
    
    @Query("SELECT * FROM alarm_statistics WHERE alarmId = :alarmId AND cycleDate = :date LIMIT 1")
    suspend fun getStatisticsForDate(alarmId: Long, date: LocalDate): AlarmCycleStatistics?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStatistics(statistics: AlarmCycleStatistics): Long
    
    @Update
    suspend fun updateStatistics(statistics: AlarmCycleStatistics)
    
    @Query("DELETE FROM alarm_statistics WHERE alarmId = :alarmId AND id NOT IN (SELECT id FROM alarm_statistics WHERE alarmId = :alarmId ORDER BY cycleDate DESC LIMIT :keepCount)")
    suspend fun cleanupOldStatistics(alarmId: Long, keepCount: Int)
    
    @Query("DELETE FROM alarm_statistics WHERE alarmId = :alarmId")
    suspend fun deleteStatisticsForAlarm(alarmId: Long)
}
