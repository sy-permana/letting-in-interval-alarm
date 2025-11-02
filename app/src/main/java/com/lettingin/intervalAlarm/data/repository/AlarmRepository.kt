package com.lettingin.intervalAlarm.data.repository

import com.lettingin.intervalAlarm.data.model.IntervalAlarm
import kotlinx.coroutines.flow.Flow

interface AlarmRepository {
    fun getAllAlarms(): Flow<List<IntervalAlarm>>
    fun getAlarmById(id: Long): Flow<IntervalAlarm?>
    fun getActiveAlarm(): Flow<IntervalAlarm?>
    suspend fun insertAlarm(alarm: IntervalAlarm): Long
    suspend fun updateAlarm(alarm: IntervalAlarm)
    suspend fun deleteAlarm(id: Long)
    suspend fun setActiveAlarm(id: Long)
    suspend fun deactivateAlarm(id: Long)
    suspend fun getAlarmCount(): Int
}
