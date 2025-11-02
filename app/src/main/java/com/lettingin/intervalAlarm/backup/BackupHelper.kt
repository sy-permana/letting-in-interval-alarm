package com.lettingin.intervalAlarm.backup

import android.content.Context
import android.util.Log
import com.lettingin.intervalAlarm.data.database.AppDatabase
import com.lettingin.intervalAlarm.data.repository.AlarmRepository
import com.lettingin.intervalAlarm.data.repository.SettingsRepository
import com.lettingin.intervalAlarm.domain.scheduler.AlarmScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Helper class to manage backup and restore operations
 * 
 * This class handles post-restore operations like rescheduling alarms
 * after the app data is restored from backup.
 */
@Singleton
class BackupHelper @Inject constructor(
    private val context: Context,
    private val alarmRepository: AlarmRepository,
    private val settingsRepository: SettingsRepository,
    private val alarmScheduler: AlarmScheduler
) {
    private val scope = CoroutineScope(Dispatchers.IO)

    companion object {
        private const val TAG = "BackupHelper"
        private const val PREFS_NAME = "backup_prefs"
        private const val KEY_LAST_BACKUP_TIME = "last_backup_time"
        private const val KEY_RESTORE_COMPLETED = "restore_completed"
    }

    /**
     * Called after app data is restored from backup
     * This should be called from Application.onCreate() to handle post-restore operations
     */
    fun handlePostRestore() {
        scope.launch {
            try {
                val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                val restoreCompleted = prefs.getBoolean(KEY_RESTORE_COMPLETED, false)

                if (!restoreCompleted) {
                    Log.d(TAG, "Handling post-restore operations")
                    
                    // Reschedule all active alarms
                    rescheduleActiveAlarms()
                    
                    // Mark restore as completed
                    prefs.edit().putBoolean(KEY_RESTORE_COMPLETED, true).apply()
                    
                    Log.d(TAG, "Post-restore operations completed")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error handling post-restore operations", e)
            }
        }
    }

    /**
     * Reschedule all active alarms after restore
     */
    private suspend fun rescheduleActiveAlarms() {
        try {
            val activeAlarm = alarmRepository.getActiveAlarmSync()
            if (activeAlarm != null) {
                Log.d(TAG, "Rescheduling active alarm: ${activeAlarm.id}")
                alarmScheduler.scheduleAlarm(activeAlarm)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error rescheduling active alarms", e)
        }
    }

    /**
     * Record backup time
     * This can be used to track when the last backup occurred
     */
    fun recordBackupTime() {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putLong(KEY_LAST_BACKUP_TIME, System.currentTimeMillis()).apply()
    }

    /**
     * Get the last backup time
     */
    fun getLastBackupTime(): Long {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getLong(KEY_LAST_BACKUP_TIME, 0)
    }

    /**
     * Reset restore flag
     * This should be called after a fresh install or when testing restore functionality
     */
    fun resetRestoreFlag() {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_RESTORE_COMPLETED, false).apply()
    }

    /**
     * Check if app data appears to be restored
     * This is a heuristic check based on database existence and restore flag
     */
    fun isDataRestored(): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val databaseExists = context.getDatabasePath(AppDatabase.DATABASE_NAME).exists()
        val restoreCompleted = prefs.getBoolean(KEY_RESTORE_COMPLETED, false)
        
        // If database exists but restore not completed, likely a restore scenario
        return databaseExists && !restoreCompleted
    }
}

/**
 * Extension function to add getActiveAlarmSync to AlarmRepository
 * This is needed for synchronous access during restore operations
 */
suspend fun AlarmRepository.getActiveAlarmSync() = 
    getActiveAlarm().let { flow ->
        var result: com.lettingin.intervalAlarm.data.model.IntervalAlarm? = null
        flow.collect { alarm ->
            result = alarm
        }
        result
    }
