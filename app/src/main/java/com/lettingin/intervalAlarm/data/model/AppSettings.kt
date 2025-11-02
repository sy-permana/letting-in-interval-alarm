package com.lettingin.intervalAlarm.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_settings")
data class AppSettings(
    @PrimaryKey
    val id: Int = 1,
    val defaultIntervalMinutes: Int = 30,
    val defaultNotificationType: NotificationType = NotificationType.FULL_SCREEN,
    val themeMode: ThemeMode = ThemeMode.SYSTEM
)

enum class ThemeMode {
    LIGHT,
    DARK,
    SYSTEM
}
