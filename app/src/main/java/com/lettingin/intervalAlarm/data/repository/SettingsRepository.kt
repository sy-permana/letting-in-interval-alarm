package com.lettingin.intervalAlarm.data.repository

import com.lettingin.intervalAlarm.data.model.AppSettings
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    fun getSettings(): Flow<AppSettings?>
    suspend fun getSettingsSync(): AppSettings?
    suspend fun updateSettings(settings: AppSettings)
}
