package com.lettingin.intervalAlarm.data.database

import androidx.room.*
import com.lettingin.intervalAlarm.data.model.IntervalAlarm
import kotlinx.coroutines.flow.Flow

@Dao
interface AlarmDao {
    
    @Query("SELECT * FROM interval_alarms ORDER BY isActive DESC, createdAt DESC")
    fun getAllAlarms(): Flow<List<IntervalAlarm>>
    
    @Query("SELECT * FROM interval_alarms WHERE id = :id")
    fun getAlarmById(id: Long): Flow<IntervalAlarm?>
    
    @Query("SELECT * FROM interval_alarms WHERE isActive = 1 LIMIT 1")
    fun getActiveAlarm(): Flow<IntervalAlarm?>
    
    @Query("SELECT COUNT(*) FROM interval_alarms")
    suspend fun getAlarmCount(): Int
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlarm(alarm: IntervalAlarm): Long
    
    @Update
    suspend fun updateAlarm(alarm: IntervalAlarm)
    
    @Delete
    suspend fun deleteAlarm(alarm: IntervalAlarm)
    
    @Query("DELETE FROM interval_alarms WHERE id = :id")
    suspend fun deleteAlarmById(id: Long)
    
    @Query("UPDATE interval_alarms SET isActive = 0")
    suspend fun deactivateAllAlarms()
    
    @Query("UPDATE interval_alarms SET isActive = 1 WHERE id = :id")
    suspend fun setAlarmActive(id: Long)
    
    @Query("UPDATE interval_alarms SET isActive = 0 WHERE id = :id")
    suspend fun setAlarmInactive(id: Long)
}
