package com.lettingin.intervalAlarm.data.repository

import com.lettingin.intervalAlarm.data.database.SettingsDao
import com.lettingin.intervalAlarm.data.model.AppSettings
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    private val settingsDao: SettingsDao
) : SettingsRepository {
    
    override fun getSettings(): Flow<AppSettings?> {
        return settingsDao.getSettings()
    }
    
    override suspend fun getSettingsSync(): AppSettings? {
        return try {
            settingsDao.getSettingsSync()
        } catch (e: Exception) {
            throw RepositoryException("Failed to get settings", e)
        }
    }
    
    override suspend fun updateSettings(settings: AppSettings) {
        try {
            settingsDao.insertSettings(settings)
        } catch (e: Exception) {
            throw RepositoryException("Failed to update settings", e)
        }
    }
}
