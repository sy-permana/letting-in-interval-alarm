package com.lettingin.intervalAlarm.ui.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.lettingin.intervalAlarm.data.model.AlarmState
import com.lettingin.intervalAlarm.data.model.IntervalAlarm
import com.lettingin.intervalAlarm.util.TimeFormatter
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToEditor: (Long?) -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToStatistics: (Long) -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val allAlarms by viewModel.allAlarms.collectAsState()
    val activeAlarm by viewModel.activeAlarm.collectAsState()
    val activeAlarmState by viewModel.activeAlarmState.collectAsState()
    val todayStatistics by viewModel.todayStatistics.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    var showPauseDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf<Long?>(null) }
    
    // Get permission checker from context
    val context = androidx.compose.ui.platform.LocalContext.current
    val permissionChecker = remember {
        com.lettingin.intervalAlarm.util.PermissionChecker(context)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Letting In") },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        },
        floatingActionButton = {
            if (allAlarms.size < 10) {
                FloatingActionButton(
                    onClick = { onNavigateToEditor(null) }
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Create Interval Alarm")
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Permission warning banner
            com.lettingin.intervalAlarm.ui.components.PermissionWarningBanner(
                permissionChecker = permissionChecker,
                onNavigateToSettings = onNavigateToSettings
            )

            // Error message
            errorMessage?.let { error ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = error,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            if (error.contains("Missing permissions")) {
                                Spacer(Modifier.height(8.dp))
                                TextButton(
                                    onClick = onNavigateToSettings,
                                    colors = ButtonDefaults.textButtonColors(
                                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                ) {
                                    Text("Go to Settings")
                                    Spacer(Modifier.width(4.dp))
                                    Icon(
                                        Icons.Default.ArrowForward,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                        IconButton(onClick = { viewModel.clearError() }) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Dismiss",
                                tint = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
            }

            if (allAlarms.isEmpty()) {
                // Empty state
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Icon(
                            Icons.Default.Notifications,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            "No alarms yet",
                            style = MaterialTheme.typography.headlineSmall
                        )
                        Text(
                            "Tap + to create your first interval alarm",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Active alarm (if exists)
                    activeAlarm?.let { alarm ->
                        item {
                            ActiveAlarmCard(
                                alarm = alarm,
                                alarmState = activeAlarmState,
                                todayStatistics = todayStatistics,
                                onPause = { showPauseDialog = true },
                                onResume = { viewModel.resumeAlarm() },
                                onToggleActive = { isActive -> viewModel.onToggleAlarm(alarm.id, isActive) },
                                onEdit = { onNavigateToEditor(alarm.id) },
                                onViewStatistics = { onNavigateToStatistics(alarm.id) }
                            )
                        }
                    }

                    // Inactive alarms
                    val inactiveAlarms = allAlarms.filter { !it.isActive }
                    items(inactiveAlarms, key = { it.id }) { alarm ->
                        InactiveAlarmCard(
                            alarm = alarm,
                            onToggleActive = { isActive -> viewModel.onToggleAlarm(alarm.id, isActive) },
                            onEdit = { onNavigateToEditor(alarm.id) },
                            onDelete = { showDeleteDialog = alarm.id },
                            onViewStatistics = { onNavigateToStatistics(alarm.id) }
                        )
                    }
                }
            }
        }
    }

    // Pause dialog
    if (showPauseDialog) {
        PauseDialog(
            onDismiss = { showPauseDialog = false },
            onPause = { durationMillis ->
                viewModel.pauseAlarm(durationMillis)
                showPauseDialog = false
            },
            activeAlarm = activeAlarm,
            activeAlarmState = activeAlarmState
        )
    }

    // Delete confirmation dialog
    showDeleteDialog?.let { alarmId ->
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            title = { Text("Delete Alarm") },
            text = { Text("Are you sure you want to delete this alarm? This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteAlarm(alarmId)
                        showDeleteDialog = null
                    }
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = null }) {
                    Text("Cancel")
                }
            }
        )
    }
    
    // Activation confirmation dialog
    val showActivationConfirmation by viewModel.showActivationConfirmation.collectAsState()
    val pendingActivationAlarmId by viewModel.pendingActivationAlarmId.collectAsState()
    
    if (showActivationConfirmation) {
        val currentActiveAlarmLabel = activeAlarm?.label?.ifEmpty { "Interval Alarm" } ?: "Interval Alarm"
        val pendingAlarmLabel = allAlarms.find { it.id == pendingActivationAlarmId }?.label?.ifEmpty { "Interval Alarm" } ?: "Interval Alarm"
        
        ActivationConfirmationDialog(
            currentActiveAlarmLabel = currentActiveAlarmLabel,
            newAlarmLabel = pendingAlarmLabel,
            onConfirm = { viewModel.confirmActivation() },
            onDismiss = { viewModel.dismissActivationConfirmation() }
        )
    }
}


@Composable
fun ActiveAlarmCard(
    alarm: IntervalAlarm,
    alarmState: AlarmState?,
    todayStatistics: com.lettingin.intervalAlarm.data.model.AlarmCycleStatistics?,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onToggleActive: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onViewStatistics: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (alarm.label.isNotEmpty()) alarm.label else "Active Alarm",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "ACTIVE",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                
                // Switch and action buttons
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Switch(
                        checked = alarm.isActive,
                        onCheckedChange = onToggleActive,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                    IconButton(onClick = onEdit) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Edit",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            Divider()

            // Alarm details
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Time Range",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                    Text(
                        text = TimeFormatter.formatTimeRange(alarm.startTime, alarm.endTime),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                Column {
                    Text(
                        text = "Interval",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                    Text(
                        text = formatInterval(alarm.intervalMinutes),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            // Active days
            Text(
                text = "Active Days",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                DayOfWeek.values().forEach { day ->
                    val isSelected = alarm.selectedDays.contains(day)
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = if (isSelected) 
                            MaterialTheme.colorScheme.primary 
                        else 
                            MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = day.getDisplayName(TextStyle.SHORT, Locale.getDefault()).take(1),
                            modifier = Modifier.padding(vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isSelected) 
                                MaterialTheme.colorScheme.onPrimary 
                            else 
                                MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }

            Divider()

            // Statistics
            alarmState?.let { state ->
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Next ring time
                    state.nextScheduledRingTime?.let { nextRing ->
                        StatisticRow(
                            label = "Next Ring",
                            value = TimeFormatter.format24Hour(nextRing),
                            icon = Icons.Default.Schedule
                        )
                    }

                    // Time until end
                    val timeUntilEnd = calculateTimeUntilEnd(alarm.endTime)
                    if (timeUntilEnd != null) {
                        StatisticRow(
                            label = "Time Until End",
                            value = timeUntilEnd,
                            icon = Icons.Default.Timer
                        )
                    }

                    // Today's rings
                    StatisticRow(
                        label = "Today's Rings",
                        value = state.todayRingCount.toString(),
                        icon = Icons.Default.Notifications
                    )

                    // Dismissals
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        StatisticRow(
                            label = "User Dismissals",
                            value = state.todayUserDismissCount.toString(),
                            icon = Icons.Default.TouchApp,
                            modifier = Modifier.weight(1f)
                        )
                        StatisticRow(
                            label = "Auto Dismissals",
                            value = state.todayAutoDismissCount.toString(),
                            icon = Icons.Default.Timer,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            Divider()

            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Pause/Resume button
                if (alarmState?.isPaused == true) {
                    Button(
                        onClick = onResume,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Resume")
                    }
                } else {
                    OutlinedButton(
                        onClick = onPause,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Pause, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Pause")
                    }
                }

                // View statistics button
                OutlinedButton(
                    onClick = onViewStatistics,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.BarChart, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Stats")
                }
            }
        }
    }
}

@Composable
fun InactiveAlarmCard(
    alarm: IntervalAlarm,
    onToggleActive: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onViewStatistics: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (alarm.label.isNotEmpty()) alarm.label else "Interval Alarm",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
                
                // Switch and action buttons
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Switch(
                        checked = alarm.isActive,
                        onCheckedChange = onToggleActive,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit")
                    }
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete")
                    }
                }
            }

            // Alarm details
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Time Range",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = TimeFormatter.formatTimeRange(alarm.startTime, alarm.endTime),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                Column {
                    Text(
                        text = "Interval",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = formatInterval(alarm.intervalMinutes),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            // Active days
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                DayOfWeek.values().forEach { day ->
                    val isSelected = alarm.selectedDays.contains(day)
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = if (isSelected) 
                            MaterialTheme.colorScheme.secondaryContainer 
                        else 
                            MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = day.getDisplayName(TextStyle.SHORT, Locale.getDefault()).take(1),
                            modifier = Modifier.padding(vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isSelected) 
                                MaterialTheme.colorScheme.onSecondaryContainer 
                            else 
                                MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }

            // Action buttons
            OutlinedButton(
                onClick = onViewStatistics,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.BarChart, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("View Statistics")
            }
        }
    }
}

@Composable
fun StatisticRow(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
        )
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}


@Composable
fun PauseDialog(
    onDismiss: () -> Unit,
    onPause: (Long) -> Unit,
    activeAlarm: IntervalAlarm?,
    activeAlarmState: AlarmState?
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Pause Alarm") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Select pause duration:")
                
                // Calculate interval duration in milliseconds
                val intervalMillis = (activeAlarm?.intervalMinutes ?: 30) * 60 * 1000L
                
                // Pause options
                TextButton(
                    onClick = { onPause(intervalMillis) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("1x Interval (${activeAlarm?.intervalMinutes ?: 30} min)")
                }
                
                TextButton(
                    onClick = { onPause(30 * 60 * 1000L) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("30 minutes")
                }
                
                TextButton(
                    onClick = { onPause(60 * 60 * 1000L) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("1 hour")
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

// Utility functions
private fun formatInterval(minutes: Int): String {
    return when {
        minutes < 60 -> "$minutes min"
        minutes % 60 == 0 -> "${minutes / 60} hr"
        else -> "${minutes / 60}h ${minutes % 60}m"
    }
}

private fun calculateTimeUntilEnd(endTime: java.time.LocalTime): String? {
    val now = java.time.LocalTime.now()
    if (now.isAfter(endTime)) {
        return null
    }
    
    val duration = java.time.Duration.between(now, endTime)
    val hours = duration.toHours()
    val minutes = duration.toMinutes() % 60
    
    return when {
        hours > 0 -> "${hours}h ${minutes}m"
        minutes > 0 -> "${minutes}m"
        else -> "Less than 1m"
    }
}

@Composable
fun ActivationConfirmationDialog(
    currentActiveAlarmLabel: String,
    newAlarmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.SwapHoriz,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = {
            Text("Switch Active Alarm?")
        },
        text = {
            Column {
                Text(
                    text = "The alarm \"$currentActiveAlarmLabel\" is currently active.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Activating \"$newAlarmLabel\" will deactivate the current alarm.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Switch")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
