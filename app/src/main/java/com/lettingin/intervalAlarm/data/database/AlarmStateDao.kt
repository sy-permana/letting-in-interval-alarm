package com.lettingin.intervalAlarm.data.database

import androidx.room.*
import com.lettingin.intervalAlarm.data.model.AlarmState
import kotlinx.coroutines.flow.Flow

@Dao
interface AlarmStateDao {
    
    @Query("SELECT * FROM alarm_state WHERE alarmId = :alarmId")
    fun getAlarmState(alarmId: Long): Flow<AlarmState?>
    
    @Query("SELECT * FROM alarm_state WHERE alarmId = :alarmId")
    suspend fun getAlarmStateSync(alarmId: Long): AlarmState?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlarmState(state: AlarmState)
    
    @Update
    suspend fun updateAlarmState(state: AlarmState)
    
    @Query("DELETE FROM alarm_state WHERE alarmId = :alarmId")
    suspend fun deleteAlarmState(alarmId: Long)
    
    @Query("UPDATE alarm_state SET todayRingCount = 0, todayUserDismissCount = 0, todayAutoDismissCount = 0, isStoppedForDay = 0, isPaused = 0, pauseUntilTime = NULL WHERE alarmId = :alarmId")
    suspend fun resetDailyCounters(alarmId: Long)
}
