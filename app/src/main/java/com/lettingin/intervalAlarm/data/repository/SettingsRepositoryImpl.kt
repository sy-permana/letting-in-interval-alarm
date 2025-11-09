package com.lettingin.intervalAlarm.data.repository

import com.lettingin.intervalAlarm.data.database.SettingsDao
import com.lettingin.intervalAlarm.data.model.AppSettings
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    private val settingsDao: SettingsDao,
    appLogger: com.lettingin.intervalAlarm.util.AppLogger
) : SafeRepository(appLogger), SettingsRepository {
    
    override fun getSettings(): Flow<AppSettings?> {
        return settingsDao.getSettings()
    }
    
    override suspend fun getSettingsSync(): AppSettings? {
        val result = safeDbOperation("getSettingsSync()") {
            settingsDao.getSettingsSync()
        }
        
        return result.getOrElse { throw it }
    }
    
    override suspend fun updateSettings(settings: AppSettings) {
        safeDbOperationUnit("updateSettings()") {
            settingsDao.insertSettings(settings)
        }
    }
}
