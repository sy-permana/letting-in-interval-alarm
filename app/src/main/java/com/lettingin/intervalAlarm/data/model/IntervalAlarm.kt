package com.lettingin.intervalAlarm.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.DayOfWeek
import java.time.LocalTime

@Entity(
    tableName = "interval_alarms",
    indices = [Index(value = ["isActive"])]
)
data class IntervalAlarm(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val label: String = "",
    val startTime: LocalTime,
    val endTime: LocalTime,
    val intervalMinutes: Int,
    val selectedDays: Set<DayOfWeek>,
    val isRepeatable: Boolean,
    val notificationType: NotificationType,
    val ringtoneUri: String,
    val isActive: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

enum class NotificationType {
    FULL_SCREEN,
    NOTIFICATION_POPUP,
    SOUND_ONLY
}
