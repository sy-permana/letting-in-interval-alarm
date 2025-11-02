package com.lettingin.intervalAlarm.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "alarm_state")
data class AlarmState(
    @PrimaryKey
    val alarmId: Long,
    val lastRingTime: Long? = null,
    val nextScheduledRingTime: Long? = null,
    val isPaused: Boolean = false,
    val pauseUntilTime: Long? = null,
    val isStoppedForDay: Boolean = false,
    val currentDayStartTime: Long? = null,
    val todayRingCount: Int = 0,
    val todayUserDismissCount: Int = 0,
    val todayAutoDismissCount: Int = 0
)
