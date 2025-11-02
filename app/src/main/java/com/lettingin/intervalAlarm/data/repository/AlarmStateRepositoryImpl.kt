package com.lettingin.intervalAlarm.data.repository

import com.lettingin.intervalAlarm.data.database.AlarmStateDao
import com.lettingin.intervalAlarm.data.model.AlarmState
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AlarmStateRepositoryImpl @Inject constructor(
    private val alarmStateDao: AlarmStateDao
) : AlarmStateRepository {
    
    override fun getAlarmState(alarmId: Long): Flow<AlarmState?> {
        return alarmStateDao.getAlarmState(alarmId)
    }
    
    override suspend fun getAlarmStateSync(alarmId: Long): AlarmState? {
        return try {
            alarmStateDao.getAlarmStateSync(alarmId)
        } catch (e: Exception) {
            throw RepositoryException("Failed to get alarm state", e)
        }
    }
    
    override suspend fun updateAlarmState(state: AlarmState) {
        try {
            alarmStateDao.insertAlarmState(state)
        } catch (e: Exception) {
            throw RepositoryException("Failed to update alarm state", e)
        }
    }
    
    override suspend fun deleteAlarmState(alarmId: Long) {
        try {
            alarmStateDao.deleteAlarmState(alarmId)
        } catch (e: Exception) {
            throw RepositoryException("Failed to delete alarm state", e)
        }
    }
    
    override suspend fun resetDailyCounters(alarmId: Long) {
        try {
            alarmStateDao.resetDailyCounters(alarmId)
        } catch (e: Exception) {
            throw RepositoryException("Failed to reset daily counters", e)
        }
    }
}
