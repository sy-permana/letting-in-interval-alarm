package com.lettingin.intervalAlarm.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDate

@Entity(
    tableName = "alarm_statistics",
    indices = [
        Index(value = ["alarmId"]),
        Index(value = ["cycleDate"])
    ]
)
data class AlarmCycleStatistics(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val alarmId: Long,
    val cycleDate: LocalDate,
    val totalRings: Int = 0,
    val userDismissals: Int = 0,
    val autoDismissals: Int = 0,
    val cycleStartTime: Long,
    val cycleEndTime: Long? = null
)
