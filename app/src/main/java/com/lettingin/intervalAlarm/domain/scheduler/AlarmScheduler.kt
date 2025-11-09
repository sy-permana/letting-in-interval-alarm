package com.lettingin.intervalAlarm.domain.scheduler

import com.lettingin.intervalAlarm.data.model.IntervalAlarm

interface AlarmScheduler {
    suspend fun scheduleAlarm(alarm: IntervalAlarm)
    suspend fun scheduleNextRing(alarmId: Long, nextRingTime: Long)
    suspend fun cancelAlarm(alarmId: Long)
    suspend fun pauseAlarm(alarmId: Long, pauseDurationMillis: Long)
    suspend fun resumeAlarm(alarmId: Long)
    suspend fun stopForDay(alarmId: Long)
    
    /**
     * Checks if an alarm is currently scheduled in AlarmManager.
     * @param alarmId The ID of the alarm to check
     * @return true if the alarm is scheduled, false otherwise
     */
    fun isAlarmScheduled(alarmId: Long): Boolean
    
    /**
     * Gets the scheduled time for an alarm from AlarmManager.
     * Note: Android's AlarmManager doesn't provide a direct API to retrieve scheduled times,
     * so this method checks if the PendingIntent exists.
     * @param alarmId The ID of the alarm
     * @return The scheduled time if available, null if not scheduled
     */
    fun getScheduledTime(alarmId: Long): Long?
}
