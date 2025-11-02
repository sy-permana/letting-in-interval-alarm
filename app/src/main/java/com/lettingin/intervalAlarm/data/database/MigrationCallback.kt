package com.lettingin.intervalAlarm.data.database

import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import android.util.Log

/**
 * Callback for handling database migration events and failures.
 * Provides logging and error handling for database migrations.
 */
class MigrationCallback(
    private val appLogger: com.lettingin.intervalAlarm.util.AppLogger? = null
) : RoomDatabase.Callback() {
    
    companion object {
        private const val TAG = "MigrationCallback"
    }
    
    override fun onCreate(db: SupportSQLiteDatabase) {
        super.onCreate(db)
        Log.i(TAG, "Database created: version ${db.version}")
        appLogger?.i(com.lettingin.intervalAlarm.util.AppLogger.CATEGORY_DATABASE, TAG,
            "Database created: version ${db.version}")
    }
    
    override fun onOpen(db: SupportSQLiteDatabase) {
        super.onOpen(db)
        Log.d(TAG, "Database opened: version ${db.version}")
        appLogger?.d(com.lettingin.intervalAlarm.util.AppLogger.CATEGORY_DATABASE, TAG,
            "Database opened: version ${db.version}")
    }
    
    override fun onDestructiveMigration(db: SupportSQLiteDatabase) {
        super.onDestructiveMigration(db)
        Log.w(TAG, "Destructive migration performed: all data lost")
        appLogger?.w(com.lettingin.intervalAlarm.util.AppLogger.CATEGORY_DATABASE, TAG,
            "Destructive migration performed: all data lost")
    }
}

/**
 * Migration failure handler that provides graceful degradation.
 */
object MigrationFailureHandler {
    
    private const val TAG = "MigrationFailureHandler"
    
    /**
     * Handle migration failure by logging and providing recovery options
     */
    fun handleMigrationFailure(
        fromVersion: Int,
        toVersion: Int,
        exception: Exception,
        appLogger: com.lettingin.intervalAlarm.util.AppLogger? = null
    ): MigrationFailureResult {
        Log.e(TAG, "Migration failed from $fromVersion to $toVersion", exception)
        appLogger?.e(com.lettingin.intervalAlarm.util.AppLogger.CATEGORY_ERROR, TAG,
            "Migration failed from $fromVersion to $toVersion", exception)
        
        return when {
            // If migration is from version 1 to 2 (just indexes), safe to recreate
            fromVersion == 1 && toVersion == 2 -> {
                MigrationFailureResult(
                    canRecover = true,
                    shouldDestroyAndRecreate = true,
                    userMessage = "Database upgrade required. Your alarms will be preserved.",
                    technicalDetails = "Index creation failed, safe to recreate"
                )
            }
            
            // For other migrations, be more cautious
            else -> {
                MigrationFailureResult(
                    canRecover = true,
                    shouldDestroyAndRecreate = true,
                    userMessage = "Database upgrade failed. App data will be reset.",
                    technicalDetails = "Migration from $fromVersion to $toVersion failed: ${exception.message}"
                )
            }
        }
    }
    
    /**
     * Validate database integrity after migration
     */
    fun validateDatabaseIntegrity(db: SupportSQLiteDatabase): Boolean {
        return try {
            // Check if all required tables exist
            val requiredTables = listOf(
                "interval_alarms",
                "alarm_statistics",
                "alarm_state",
                "app_settings"
            )
            
            for (table in requiredTables) {
                val cursor = db.query("SELECT name FROM sqlite_master WHERE type='table' AND name=?", arrayOf(table))
                val exists = cursor.count > 0
                cursor.close()
                
                if (!exists) {
                    Log.e(TAG, "Required table missing: $table")
                    return false
                }
            }
            
            Log.i(TAG, "Database integrity check passed")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Database integrity check failed", e)
            false
        }
    }
}

/**
 * Result of migration failure handling
 */
data class MigrationFailureResult(
    val canRecover: Boolean,
    val shouldDestroyAndRecreate: Boolean,
    val userMessage: String,
    val technicalDetails: String
)
