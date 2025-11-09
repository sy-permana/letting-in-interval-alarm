package com.lettingin.intervalAlarm.data.repository

import com.lettingin.intervalAlarm.data.database.AlarmDao
import com.lettingin.intervalAlarm.data.model.IntervalAlarm
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AlarmRepositoryImpl @Inject constructor(
    private val alarmDao: AlarmDao,
    appLogger: com.lettingin.intervalAlarm.util.AppLogger
) : SafeRepository(appLogger), AlarmRepository {
    
    override fun getAllAlarms(): Flow<List<IntervalAlarm>> {
        return alarmDao.getAllAlarms()
    }
    
    override fun getAlarmById(id: Long): Flow<IntervalAlarm?> {
        return alarmDao.getAlarmById(id)
    }
    
    override fun getActiveAlarm(): Flow<IntervalAlarm?> {
        return alarmDao.getActiveAlarm()
    }
    
    override suspend fun insertAlarm(alarm: IntervalAlarm): Long {
        android.util.Log.d("AlarmRepositoryImpl", "insertAlarm: Inserting alarm - id=${alarm.id}, label='${alarm.label}', isActive=${alarm.isActive}")
        android.util.Log.d("AlarmRepositoryImpl", "insertAlarm: Stack trace:", Exception("Stack trace"))
        
        val result = safeDbOperation("insertAlarm(id=${alarm.id}, label='${alarm.label}')") {
            alarmDao.insertAlarm(alarm)
        }
        
        return result.getOrElse { 
            android.util.Log.e("AlarmRepositoryImpl", "insertAlarm: Failed", it)
            throw it
        }.also {
            android.util.Log.d("AlarmRepositoryImpl", "insertAlarm: Successfully inserted, returned id=$it")
        }
    }
    
    override suspend fun updateAlarm(alarm: IntervalAlarm) {
        android.util.Log.d("AlarmRepositoryImpl", "updateAlarm: Updating alarm - id=${alarm.id}, label='${alarm.label}', isActive=${alarm.isActive}")
        
        safeDbOperationUnit("updateAlarm(id=${alarm.id})") {
            alarmDao.updateAlarm(alarm)
        }
        
        android.util.Log.d("AlarmRepositoryImpl", "updateAlarm: Successfully updated alarm ${alarm.id}")
    }
    
    override suspend fun deleteAlarm(id: Long) {
        safeDbOperationUnit("deleteAlarm(id=$id)") {
            alarmDao.deleteAlarmById(id)
        }
    }
    
    override suspend fun setActiveAlarm(id: Long) {
        safeDbOperationUnit("setActiveAlarm(id=$id)") {
            alarmDao.deactivateAllAlarms()
            alarmDao.setAlarmActive(id)
        }
    }
    
    override suspend fun deactivateAlarm(id: Long) {
        android.util.Log.d("AlarmRepositoryImpl", "deactivateAlarm: Setting alarm $id to inactive")
        
        safeDbOperationUnit("deactivateAlarm(id=$id)") {
            alarmDao.setAlarmInactive(id)
        }
        
        android.util.Log.d("AlarmRepositoryImpl", "deactivateAlarm: Successfully set alarm $id to inactive")
    }
    
    override suspend fun getAlarmCount(): Int {
        val result = safeDbOperation("getAlarmCount()") {
            alarmDao.getAlarmCount()
        }
        
        return result.getOrElse { throw it }
    }
}

class RepositoryException(message: String, cause: Throwable? = null) : Exception(message, cause)
