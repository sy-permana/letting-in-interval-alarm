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
    
    companion object {
        private const val TAG = "AlarmStateRepository"
        const val CATEGORY_STATE_TRANSITION = "STATE_TRANSITION"
    }
    
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
        // Get current state before update for logging
        val oldState = try {
            alarmStateDao.getAlarmStateSync(state.alarmId)
        } catch (e: Exception) {
            null
        }
        
        // Log state transition with before/after values
        logStateTransition(oldState, state)
        
        safeDbOperationUnit("updateAlarmState(alarmId=${state.alarmId})") {
            alarmStateDao.insertAlarmState(state)
        }
    }
    
    /**
     * Log alarm state transitions with before/after values
     */
    private fun logStateTransition(oldState: AlarmState?, newState: AlarmState) {
        val currentTime = System.currentTimeMillis()
        
        if (oldState == null) {
            // New state being created
            appLogger.i(CATEGORY_STATE_TRANSITION, TAG,
                "Creating new alarm state: alarmId=${newState.alarmId}, " +
                "nextRingTime=${newState.nextScheduledRingTime}, " +
                "isPaused=${newState.isPaused}, " +
                "isStoppedForDay=${newState.isStoppedForDay}, " +
                "currentTime=$currentTime")
        } else {
            // State being updated - log changes
            val changes = mutableListOf<String>()
            
            if (oldState.nextScheduledRingTime != newState.nextScheduledRingTime) {
                changes.add("nextRingTime: ${oldState.nextScheduledRingTime} -> ${newState.nextScheduledRingTime}")
            }
            
            if (oldState.isPaused != newState.isPaused) {
                changes.add("isPaused: ${oldState.isPaused} -> ${newState.isPaused}")
            }
            
            if (oldState.pauseUntilTime != newState.pauseUntilTime) {
                changes.add("pauseUntilTime: ${oldState.pauseUntilTime} -> ${newState.pauseUntilTime}")
            }
            
            if (oldState.isStoppedForDay != newState.isStoppedForDay) {
                changes.add("isStoppedForDay: ${oldState.isStoppedForDay} -> ${newState.isStoppedForDay}")
            }
            
            if (oldState.lastRingTime != newState.lastRingTime) {
                changes.add("lastRingTime: ${oldState.lastRingTime} -> ${newState.lastRingTime}")
            }
            
            if (oldState.todayRingCount != newState.todayRingCount) {
                changes.add("todayRingCount: ${oldState.todayRingCount} -> ${newState.todayRingCount}")
            }
            
            if (oldState.todayUserDismissCount != newState.todayUserDismissCount) {
                changes.add("todayUserDismissCount: ${oldState.todayUserDismissCount} -> ${newState.todayUserDismissCount}")
            }
            
            if (oldState.todayAutoDismissCount != newState.todayAutoDismissCount) {
                changes.add("todayAutoDismissCount: ${oldState.todayAutoDismissCount} -> ${newState.todayAutoDismissCount}")
            }
            
            if (changes.isNotEmpty()) {
                appLogger.i(CATEGORY_STATE_TRANSITION, TAG,
                    "Alarm state transition: alarmId=${newState.alarmId}, " +
                    "changes=[${changes.joinToString(", ")}], " +
                    "currentTime=$currentTime")
            }
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
