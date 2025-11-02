package com.lettingin.intervalAlarm.data.repository

import com.lettingin.intervalAlarm.data.database.AlarmDao
import com.lettingin.intervalAlarm.data.model.IntervalAlarm
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AlarmRepositoryImpl @Inject constructor(
    private val alarmDao: AlarmDao
) : AlarmRepository {
    
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
        return try {
            android.util.Log.d("AlarmRepositoryImpl", "insertAlarm: Inserting alarm - id=${alarm.id}, label='${alarm.label}', isActive=${alarm.isActive}")
            android.util.Log.d("AlarmRepositoryImpl", "insertAlarm: Stack trace:", Exception("Stack trace"))
            val result = alarmDao.insertAlarm(alarm)
            android.util.Log.d("AlarmRepositoryImpl", "insertAlarm: Successfully inserted, returned id=$result")
            result
        } catch (e: Exception) {
            android.util.Log.e("AlarmRepositoryImpl", "insertAlarm: Failed", e)
            throw RepositoryException("Failed to insert alarm", e)
        }
    }
    
    override suspend fun updateAlarm(alarm: IntervalAlarm) {
        try {
            android.util.Log.d("AlarmRepositoryImpl", "updateAlarm: Updating alarm - id=${alarm.id}, label='${alarm.label}', isActive=${alarm.isActive}")
            alarmDao.updateAlarm(alarm)
            android.util.Log.d("AlarmRepositoryImpl", "updateAlarm: Successfully updated alarm ${alarm.id}")
        } catch (e: Exception) {
            android.util.Log.e("AlarmRepositoryImpl", "updateAlarm: Failed for alarm ${alarm.id}", e)
            throw RepositoryException("Failed to update alarm", e)
        }
    }
    
    override suspend fun deleteAlarm(id: Long) {
        try {
            alarmDao.deleteAlarmById(id)
        } catch (e: Exception) {
            throw RepositoryException("Failed to delete alarm", e)
        }
    }
    
    override suspend fun setActiveAlarm(id: Long) {
        try {
            alarmDao.deactivateAllAlarms()
            alarmDao.setAlarmActive(id)
        } catch (e: Exception) {
            throw RepositoryException("Failed to set active alarm", e)
        }
    }
    
    override suspend fun deactivateAlarm(id: Long) {
        try {
            android.util.Log.d("AlarmRepositoryImpl", "deactivateAlarm: Setting alarm $id to inactive")
            alarmDao.setAlarmInactive(id)
            android.util.Log.d("AlarmRepositoryImpl", "deactivateAlarm: Successfully set alarm $id to inactive")
        } catch (e: Exception) {
            android.util.Log.e("AlarmRepositoryImpl", "deactivateAlarm: Failed for alarm $id", e)
            throw RepositoryException("Failed to deactivate alarm", e)
        }
    }
    
    override suspend fun getAlarmCount(): Int {
        return try {
            alarmDao.getAlarmCount()
        } catch (e: Exception) {
            throw RepositoryException("Failed to get alarm count", e)
        }
    }
}

class RepositoryException(message: String, cause: Throwable? = null) : Exception(message, cause)
