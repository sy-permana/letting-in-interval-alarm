package com.lettingin.intervalAlarm.domain.scheduler

import com.lettingin.intervalAlarm.data.model.IntervalAlarm

interface AlarmScheduler {
    suspend fun scheduleAlarm(alarm: IntervalAlarm)
    suspend fun scheduleNextRing(alarmId: Long, nextRingTime: Long)
    suspend fun cancelAlarm(alarmId: Long)
    suspend fun pauseAlarm(alarmId: Long, pauseDurationMillis: Long)
    suspend fun resumeAlarm(alarmId: Long)
    suspend fun stopForDay(alarmId: Long)
}
