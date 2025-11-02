package com.lettingin.intervalAlarm.data.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Database migrations for the Letting In app
 * 
 * This object contains all database migrations from one version to another.
 * Each migration should be thoroughly tested before release.
 */
object DatabaseMigrations {

    /**
     * Migration from version 1 to version 2
     * Adds indexes to improve query performance:
     * - Index on interval_alarms.isActive (for finding active alarm)
     * - Index on alarm_statistics.alarmId (for querying statistics by alarm)
     * - Index on alarm_statistics.cycleDate (for querying recent statistics)
     */
    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(database: SupportSQLiteDatabase) {
            // Add index on interval_alarms.isActive
            database.execSQL(
                "CREATE INDEX IF NOT EXISTS index_interval_alarms_isActive " +
                "ON interval_alarms(isActive)"
            )
            
            // Add index on alarm_statistics.alarmId
            database.execSQL(
                "CREATE INDEX IF NOT EXISTS index_alarm_statistics_alarmId " +
                "ON alarm_statistics(alarmId)"
            )
            
            // Add index on alarm_statistics.cycleDate
            database.execSQL(
                "CREATE INDEX IF NOT EXISTS index_alarm_statistics_cycleDate " +
                "ON alarm_statistics(cycleDate)"
            )
        }
    }

    /**
     * Get all available migrations
     * Add new migrations to this array as they are created
     */
    fun getAllMigrations(): Array<Migration> {
        return arrayOf(
            MIGRATION_1_2
        )
    }

    /**
     * Fallback migration strategy
     * This will be used if a migration path is not found
     * WARNING: This will destroy all data and recreate the database
     */
    val FALLBACK_DESTRUCTIVE_MIGRATION = true
}

/**
 * Migration testing utilities
 */
object MigrationTestHelper {
    
    /**
     * Validate that all required migrations are present
     * This should be called in tests to ensure migration path completeness
     */
    fun validateMigrationPath(fromVersion: Int, toVersion: Int, migrations: Array<Migration>): Boolean {
        val migrationMap = migrations.associateBy { it.startVersion to it.endVersion }
        
        var currentVersion = fromVersion
        while (currentVersion < toVersion) {
            val nextMigration = migrationMap[currentVersion to currentVersion + 1]
            if (nextMigration == null) {
                return false
            }
            currentVersion++
        }
        
        return true
    }

    /**
     * Get migration path from one version to another
     */
    fun getMigrationPath(fromVersion: Int, toVersion: Int, migrations: Array<Migration>): List<Migration> {
        val migrationMap = migrations.associateBy { it.startVersion to it.endVersion }
        val path = mutableListOf<Migration>()
        
        var currentVersion = fromVersion
        while (currentVersion < toVersion) {
            val nextMigration = migrationMap[currentVersion to currentVersion + 1]
            if (nextMigration != null) {
                path.add(nextMigration)
                currentVersion++
            } else {
                break
            }
        }
        
        return path
    }
}
