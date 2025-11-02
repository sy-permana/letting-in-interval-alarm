package com.lettingin.intervalAlarm.ui.debug

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebugScreen(
    onNavigateBack: () -> Unit,
    viewModel: DebugViewModel = hiltViewModel()
) {
    val debugInfo by viewModel.debugInfo.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Debug Information") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Text("←")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // System Info
            item {
                DebugSection(title = "System Information") {
                    DebugItem("Current Time", debugInfo.currentTime)
                    DebugItem("Device Uptime", debugInfo.deviceUptime)
                    DebugItem("App Version", debugInfo.appVersion)
                    DebugItem("Android Version", debugInfo.androidVersion)
                }
            }

            // Active Alarm Info
            item {
                DebugSection(title = "Active Alarm") {
                    if (debugInfo.activeAlarm != null) {
                        val alarm = debugInfo.activeAlarm!!
                        DebugItem("Alarm ID", alarm.id.toString())
                        DebugItem("Label", alarm.label.ifEmpty { "(No label)" })
                        DebugItem("Start Time", alarm.startTime.toString())
                        DebugItem("End Time", alarm.endTime.toString())
                        DebugItem("Interval", "${alarm.intervalMinutes} minutes")
                        DebugItem("Selected Days", alarm.selectedDays.joinToString(", "))
                        DebugItem("Is Repeatable", alarm.isRepeatable.toString())
                        DebugItem("Notification Type", alarm.notificationType.toString())
                    } else {
                        Text("No active alarm", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            // Alarm State Info
            item {
                DebugSection(title = "Alarm State") {
                    if (debugInfo.alarmState != null) {
                        val state = debugInfo.alarmState!!
                        DebugItem("Last Ring Time", formatTimestamp(state.lastRingTime))
                        DebugItem("Next Scheduled Ring", formatTimestamp(state.nextScheduledRingTime))
                        DebugItem("Is Paused", state.isPaused.toString())
                        DebugItem("Pause Until", formatTimestamp(state.pauseUntilTime))
                        DebugItem("Stopped for Day", state.isStoppedForDay.toString())
                        DebugItem("Today Ring Count", state.todayRingCount.toString())
                        DebugItem("Today User Dismissals", state.todayUserDismissCount.toString())
                        DebugItem("Today Auto Dismissals", state.todayAutoDismissCount.toString())
                    } else {
                        Text("No alarm state", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            // Permissions Info
            item {
                DebugSection(title = "Permissions") {
                    debugInfo.permissions.forEach { (permission, granted) ->
                        DebugItem(
                            permission,
                            if (granted) "✅ Granted" else "❌ Denied",
                            valueColor = if (granted) 
                                MaterialTheme.colorScheme.primary 
                            else 
                                MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            // All Alarms
            item {
                DebugSection(title = "All Alarms (${debugInfo.allAlarms.size})") {
                    if (debugInfo.allAlarms.isEmpty()) {
                        Text("No alarms", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            items(debugInfo.allAlarms) { alarm ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (alarm.isActive) 
                            MaterialTheme.colorScheme.primaryContainer 
                        else 
                            MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "ID: ${alarm.id} ${if (alarm.isActive) "(ACTIVE)" else ""}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = alarm.label.ifEmpty { "(No label)" },
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "${alarm.startTime} - ${alarm.endTime} (${alarm.intervalMinutes}min)",
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            // Log Statistics
            item {
                DebugSection(title = "Log Statistics") {
                    debugInfo.logStats.forEach { (stat, count) ->
                        DebugItem(stat, count.toString())
                    }
                }
            }
            
            // Recent Logs
            item {
                DebugSection(title = "Recent Logs (Last 20)") {
                    if (debugInfo.recentLogs.isEmpty()) {
                        Text("No logs available", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
            
            items(debugInfo.recentLogs.takeLast(20).reversed()) { logEntry ->
                LogEntryCard(logEntry)
            }

            // Actions
            item {
                DebugSection(title = "Debug Actions") {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { viewModel.refreshDebugInfo() },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Refresh Debug Info")
                        }
                        
                        Button(
                            onClick = { viewModel.exportLogs() },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Export Logs to File")
                        }
                        
                        Button(
                            onClick = { viewModel.clearLogs() },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Text("Clear Logs")
                        }

                        Button(
                            onClick = { viewModel.simulateBootReceiver() },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondary
                            )
                        ) {
                            Text("Simulate Boot Receiver")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LogEntryCard(logEntry: com.lettingin.intervalAlarm.util.AppLogger.LogEntry) {
    val levelColor = when (logEntry.level) {
        com.lettingin.intervalAlarm.util.AppLogger.LogLevel.ERROR -> MaterialTheme.colorScheme.error
        com.lettingin.intervalAlarm.util.AppLogger.LogLevel.WARNING -> MaterialTheme.colorScheme.tertiary
        com.lettingin.intervalAlarm.util.AppLogger.LogLevel.INFO -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurface
    }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = logEntry.timestamp.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss")),
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = logEntry.level.name,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = levelColor
                )
            }
            Text(
                text = "[${logEntry.category}] ${logEntry.message}",
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace
            )
            if (logEntry.throwable != null) {
                Text(
                    text = "Exception: ${logEntry.throwable.message}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

@Composable
fun DebugSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Divider()
            content()
        }
    }
}

@Composable
fun DebugItem(
    label: String,
    value: String,
    valueColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontFamily = FontFamily.Monospace,
            color = valueColor,
            modifier = Modifier.weight(1f)
        )
    }
}

private fun formatTimestamp(timestamp: Long?): String {
    if (timestamp == null) return "N/A"
    val dateTime = LocalDateTime.ofInstant(
        Instant.ofEpochMilli(timestamp),
        ZoneId.systemDefault()
    )
    return dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
}
