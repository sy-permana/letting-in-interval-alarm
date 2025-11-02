package com.lettingin.intervalAlarm.data.database

import androidx.room.*
import com.lettingin.intervalAlarm.data.model.AppSettings
import kotlinx.coroutines.flow.Flow

@Dao
interface SettingsDao {
    
    @Query("SELECT * FROM app_settings WHERE id = 1 LIMIT 1")
    fun getSettings(): Flow<AppSettings?>
    
    @Query("SELECT * FROM app_settings WHERE id = 1 LIMIT 1")
    suspend fun getSettingsSync(): AppSettings?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSettings(settings: AppSettings)
    
    @Update
    suspend fun updateSettings(settings: AppSettings)
}
