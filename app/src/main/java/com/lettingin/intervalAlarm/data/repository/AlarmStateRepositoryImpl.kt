package com.lettingin.intervalAlarm.data.repository

import com.lettingin.intervalAlarm.data.database.AlarmStateDao
import com.lettingin.intervalAlarm.data.model.AlarmState
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AlarmStateRepositoryImpl @Inject constructor(
    private val alarmStateDao: AlarmStateDao,
    appLogger: com.lettingin.intervalAlarm.util.AppLogger
) : SafeRepository(appLogger), AlarmStateRepository {
    
    override fun getAlarmState(alarmId: Long): Flow<AlarmState?> {
        return alarmStateDao.getAlarmState(alarmId)
    }
    
    override suspend fun getAlarmStateSync(alarmId: Long): AlarmState? {
        val result = safeDbOperation("getAlarmStateSync(alarmId=$alarmId)") {
            alarmStateDao.getAlarmStateSync(alarmId)
        }
        
        return result.getOrElse { throw it }
    }
    
    override suspend fun updateAlarmState(state: AlarmState) {
        safeDbOperationUnit("updateAlarmState(alarmId=${state.alarmId})") {
            alarmStateDao.insertAlarmState(state)
        }
    }
    
    override suspend fun deleteAlarmState(alarmId: Long) {
        safeDbOperationUnit("deleteAlarmState(alarmId=$alarmId)") {
            alarmStateDao.deleteAlarmState(alarmId)
        }
    }
    
    override suspend fun resetDailyCounters(alarmId: Long) {
        safeDbOperationUnit("resetDailyCounters(alarmId=$alarmId)") {
            alarmStateDao.resetDailyCounters(alarmId)
        }
    }
}
