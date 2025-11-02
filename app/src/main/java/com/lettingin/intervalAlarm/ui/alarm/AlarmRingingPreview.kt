package com.lettingin.intervalAlarm.ui.alarm

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.lettingin.intervalAlarm.data.repository.AlarmRepository
import com.lettingin.intervalAlarm.ui.theme.LettingInTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

// Mock repository for preview
class MockAlarmRepository : AlarmRepository {
    override fun getAllAlarms() = flowOf(emptyList<com.lettingin.intervalAlarm.data.model.IntervalAlarm>())
    override fun getAlarmById(id: Long) = flowOf(
        com.lettingin.intervalAlarm.data.model.IntervalAlarm(
            id = 1,
            label = "Morning Reminder",
            startTime = java.time.LocalTime.of(9, 0),
            endTime = java.time.LocalTime.of(17, 0),
            intervalMinutes = 30,
            selectedDays = setOf(java.time.DayOfWeek.MONDAY, java.time.DayOfWeek.WEDNESDAY),
            isRepeatable = true,
            notificationType = com.lettingin.intervalAlarm.data.model.NotificationType.FULL_SCREEN,
            ringtoneUri = "default",
            isActive = true
        )
    )
    override fun getActiveAlarm() = flowOf(null)
    override suspend fun insertAlarm(alarm: com.lettingin.intervalAlarm.data.model.IntervalAlarm) = 1L
    override suspend fun updateAlarm(alarm: com.lettingin.intervalAlarm.data.model.IntervalAlarm) {}
    override suspend fun deleteAlarm(id: Long) {}
    override suspend fun setActiveAlarm(id: Long) {}
    override suspend fun deactivateAlarm(id: Long) {}
    override suspend fun getAlarmCount() = 0
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun AlarmRingingScreenPreview() {
    LettingInTheme {
        AlarmRingingScreen(
            alarmId = 1L,
            alarmRepository = MockAlarmRepository(),
            onDismiss = {},
            onStopForDay = {}
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun AlarmRingingScreenDarkPreview() {
    LettingInTheme(darkTheme = true) {
        AlarmRingingScreen(
            alarmId = 1L,
            alarmRepository = MockAlarmRepository(),
            onDismiss = {},
            onStopForDay = {}
        )
    }
}
