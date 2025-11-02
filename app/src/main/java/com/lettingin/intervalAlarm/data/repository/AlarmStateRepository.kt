package com.lettingin.intervalAlarm.data.repository

import com.lettingin.intervalAlarm.data.model.AlarmState
import kotlinx.coroutines.flow.Flow

interface AlarmStateRepository {
    fun getAlarmState(alarmId: Long): Flow<AlarmState?>
    suspend fun getAlarmStateSync(alarmId: Long): AlarmState?
    suspend fun updateAlarmState(state: AlarmState)
    suspend fun deleteAlarmState(alarmId: Long)
    suspend fun resetDailyCounters(alarmId: Long)
}
