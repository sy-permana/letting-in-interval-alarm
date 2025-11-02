package com.lettingin.intervalAlarm.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.lettingin.intervalAlarm.data.model.AlarmCycleStatistics
import com.lettingin.intervalAlarm.data.model.AlarmState
import com.lettingin.intervalAlarm.data.model.AppSettings
import com.lettingin.intervalAlarm.data.model.IntervalAlarm

@Database(
    entities = [
        IntervalAlarm::class,
        AlarmCycleStatistics::class,
        AlarmState::class,
        AppSettings::class
    ],
    version = 2,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun alarmDao(): AlarmDao
    abstract fun statisticsDao(): StatisticsDao
    abstract fun alarmStateDao(): AlarmStateDao
    abstract fun settingsDao(): SettingsDao

    companion object {
        const val DATABASE_NAME = "letting_in_database"
    }
}
