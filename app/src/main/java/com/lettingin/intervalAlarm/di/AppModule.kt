package com.lettingin.intervalAlarm.di

import android.content.Context
import androidx.room.Room
import com.lettingin.intervalAlarm.data.database.*
import com.lettingin.intervalAlarm.data.repository.*
import com.lettingin.intervalAlarm.domain.scheduler.AlarmScheduler
import com.lettingin.intervalAlarm.domain.scheduler.AlarmSchedulerImpl
import com.lettingin.intervalAlarm.util.RingtoneManager
import com.lettingin.intervalAlarm.util.RingtoneManagerImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideApplicationContext(@ApplicationContext context: Context): Context {
        return context
    }
    
    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            AppDatabase.DATABASE_NAME
        )
            .addMigrations(*DatabaseMigrations.getAllMigrations())
            .apply {
                // Enable fallback to destructive migration for development
                // In production, this should be removed to prevent data loss
                if (DatabaseMigrations.FALLBACK_DESTRUCTIVE_MIGRATION) {
                    fallbackToDestructiveMigration()
                }
            }
            .build()
    }
    
    @Provides
    @Singleton
    fun provideAlarmDao(database: AppDatabase): AlarmDao {
        return database.alarmDao()
    }
    
    @Provides
    @Singleton
    fun provideStatisticsDao(database: AppDatabase): StatisticsDao {
        return database.statisticsDao()
    }
    
    @Provides
    @Singleton
    fun provideAlarmStateDao(database: AppDatabase): AlarmStateDao {
        return database.alarmStateDao()
    }
    
    @Provides
    @Singleton
    fun provideSettingsDao(database: AppDatabase): SettingsDao {
        return database.settingsDao()
    }
    
    @Provides
    @Singleton
    fun provideAlarmRepository(
        alarmDao: AlarmDao,
        appLogger: com.lettingin.intervalAlarm.util.AppLogger
    ): AlarmRepository {
        return AlarmRepositoryImpl(alarmDao, appLogger)
    }
    
    @Provides
    @Singleton
    fun provideStatisticsRepository(
        statisticsDao: StatisticsDao,
        appLogger: com.lettingin.intervalAlarm.util.AppLogger
    ): StatisticsRepository {
        return StatisticsRepositoryImpl(statisticsDao, appLogger)
    }
    
    @Provides
    @Singleton
    fun provideAlarmStateRepository(
        alarmStateDao: AlarmStateDao,
        appLogger: com.lettingin.intervalAlarm.util.AppLogger
    ): AlarmStateRepository {
        return AlarmStateRepositoryImpl(alarmStateDao, appLogger)
    }
    
    @Provides
    @Singleton
    fun provideSettingsRepository(
        settingsDao: SettingsDao,
        appLogger: com.lettingin.intervalAlarm.util.AppLogger
    ): SettingsRepository {
        return SettingsRepositoryImpl(settingsDao, appLogger)
    }
    
    @Provides
    @Singleton
    fun provideAlarmScheduler(
        @ApplicationContext context: Context,
        alarmRepository: AlarmRepository,
        alarmStateRepository: AlarmStateRepository,
        appLogger: com.lettingin.intervalAlarm.util.AppLogger
    ): AlarmScheduler {
        return AlarmSchedulerImpl(context, alarmRepository, alarmStateRepository, appLogger)
    }
    
    @Provides
    @Singleton
    fun provideRingtoneManager(
        @ApplicationContext context: Context
    ): RingtoneManager {
        return RingtoneManagerImpl(context)
    }
}
