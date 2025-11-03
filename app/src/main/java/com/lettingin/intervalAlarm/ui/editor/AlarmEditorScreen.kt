package com.lettingin.intervalAlarm.ui.editor

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.lettingin.intervalAlarm.data.model.NotificationType
import com.lettingin.intervalAlarm.ui.components.IntervalSelector
import com.lettingin.intervalAlarm.util.TimeFormatter
import java.time.DayOfWeek
import java.time.LocalTime
import java.time.format.TextStyle
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmEditorScreen(
    alarmId: Long?,
    onNavigateBack: () -> Unit,
    viewModel: AlarmEditorViewModel = hiltViewModel()
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val alarmState by viewModel.alarmState.collectAsState()
    val maxInterval by viewModel.maxInterval.collectAsState()
    val validationResult by viewModel.validationResult.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val saveSuccess by viewModel.saveSuccess.collectAsState()

    // Load alarm on first composition
    LaunchedEffect(alarmId) {
        viewModel.loadAlarm(alarmId)
    }

    // Navigate back on save success
    LaunchedEffect(saveSuccess) {
        if (saveSuccess) {
            viewModel.resetSaveSuccess()
            onNavigateBack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (alarmId == null) "Create Alarm" else "Edit Alarm") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        // Only show loading for existing alarms, not for new ones
        if (isLoading && alarmId != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (alarmState != null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // General error message
                validationResult.errors["general"]?.let { error ->
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Text(
                            text = error,
                            modifier = Modifier.padding(16.dp),
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }

                // Label field
                OutlinedTextField(
                    value = alarmState?.label ?: "",
                    onValueChange = { viewModel.updateLabel(it) },
                    label = { Text("Label (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    isError = validationResult.errors.containsKey("label"),
                    supportingText = {
                        validationResult.errors["label"]?.let { error ->
                            Text(error, color = MaterialTheme.colorScheme.error)
                        } ?: Text("Max 60 characters")
                    },
                    singleLine = true
                )

                // Time pickers
                Card {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Time Range",
                            style = MaterialTheme.typography.titleMedium
                        )

                        var showStartTimePicker by remember { mutableStateOf(false) }
                        var showEndTimePicker by remember { mutableStateOf(false) }

                        // Start time
                        OutlinedButton(
                            onClick = { showStartTimePicker = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.Start
                            ) {
                                Text(
                                    text = "Start Time",
                                    style = MaterialTheme.typography.labelSmall
                                )
                                Text(
                                    text = TimeFormatter.format24Hour(alarmState?.startTime ?: LocalTime.of(9, 0)),
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                        }

                        // End time
                        OutlinedButton(
                            onClick = { showEndTimePicker = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.Start
                            ) {
                                Text(
                                    text = "End Time",
                                    style = MaterialTheme.typography.labelSmall
                                )
                                Text(
                                    text = TimeFormatter.format24Hour(alarmState?.endTime ?: LocalTime.of(17, 0)),
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                        }

                        validationResult.errors["time"]?.let { error ->
                            Text(
                                text = error,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }

                        // Time picker dialogs
                        if (showStartTimePicker) {
                            TimePickerDialog(
                                initialTime = alarmState?.startTime ?: LocalTime.of(9, 0),
                                onDismiss = { showStartTimePicker = false },
                                onConfirm = { time ->
                                    viewModel.updateStartTime(time)
                                    showStartTimePicker = false
                                }
                            )
                        }

                        if (showEndTimePicker) {
                            TimePickerDialog(
                                initialTime = alarmState?.endTime ?: LocalTime.of(17, 0),
                                onDismiss = { showEndTimePicker = false },
                                onConfirm = { time ->
                                    viewModel.updateEndTime(time)
                                    showEndTimePicker = false
                                }
                            )
                        }
                    }
                }

                // Interval selector
                Card {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Interval",
                            style = MaterialTheme.typography.titleMedium
                        )

                        IntervalSelector(
                            currentInterval = alarmState?.intervalMinutes ?: 30,
                            minInterval = 5,
                            maxInterval = maxInterval,
                            onIntervalChange = { viewModel.updateInterval(it) },
                            modifier = Modifier.fillMaxWidth()
                        )

                        validationResult.errors["interval"]?.let { error ->
                            Text(
                                text = error,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }

                // Day selector
                Card {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Active Days",
                            style = MaterialTheme.typography.titleMedium
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            DayOfWeek.values().forEach { day ->
                                val isSelected = alarmState?.selectedDays?.contains(day) == true
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { viewModel.toggleDay(day) },
                                    label = {
                                        Text(
                                            day.getDisplayName(TextStyle.SHORT, Locale.getDefault()).take(1)
                                        )
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        validationResult.errors["days"]?.let { error ->
                            Text(
                                text = error,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }

                // Notification type selector
                Card {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Notification Type",
                            style = MaterialTheme.typography.titleMedium
                        )

                        NotificationType.values().forEach { type ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = alarmState?.notificationType == type,
                                    onClick = { viewModel.updateNotificationType(type) }
                                )
                                Spacer(Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = formatNotificationType(type),
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                    Text(
                                        text = getNotificationTypeDescription(type),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }


                // Ringtone selector
                Card {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Ringtone",
                            style = MaterialTheme.typography.titleMedium
                        )

                        var expanded by remember { mutableStateOf(false) }
                        
                        // Lazy load ringtones only when dropdown is opened
                        var ringtones by remember { mutableStateOf<List<Pair<String, String>>?>(null) }
                        
                        LaunchedEffect(expanded) {
                            if (expanded && ringtones == null) {
                                // Load ringtones in background when dropdown opens
                                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                                    val list = mutableListOf<Pair<String, String>>()
                                    val seenUris = mutableSetOf<String>()
                                    val seenTitles = mutableSetOf<String>()
                                    
                                    try {
                                        val ringtoneManager = android.media.RingtoneManager(context)
                                        ringtoneManager.setType(android.media.RingtoneManager.TYPE_ALARM)
                                        val cursor = ringtoneManager.cursor
                                        
                                        // Get all available alarm ringtones (up to 10 unique)
                                        while (cursor.moveToNext() && list.size < 10) {
                                            val title = cursor.getString(android.media.RingtoneManager.TITLE_COLUMN_INDEX)
                                            val uri = ringtoneManager.getRingtoneUri(cursor.position).toString()
                                            
                                            // Avoid duplicates by both URI and title
                                            if (!seenUris.contains(uri) && !seenTitles.contains(title)) {
                                                list.add(uri to title)
                                                seenUris.add(uri)
                                                seenTitles.add(title)
                                            }
                                        }
                                        cursor.close()
                                    } catch (e: Exception) {
                                        android.util.Log.e("AlarmEditor", "Error loading ringtones", e)
                                        // Fallback to default
                                        val defaultUri = "content://settings/system/alarm_alert"
                                        list.add(defaultUri to "System Default")
                                    }
                                    
                                    if (list.isEmpty()) {
                                        val defaultUri = "content://settings/system/alarm_alert"
                                        list.add(defaultUri to "System Default")
                                    }
                                    
                                    ringtones = list
                                }
                            }
                        }

                        // Find the current ringtone name
                        val currentRingtoneName = remember(alarmState?.ringtoneUri, ringtones) {
                            ringtones?.find { it.first == alarmState?.ringtoneUri }?.second 
                                ?: "System Default"
                        }

                        ExposedDropdownMenuBox(
                            expanded = expanded,
                            onExpandedChange = { expanded = it }
                        ) {
                            OutlinedTextField(
                                value = currentRingtoneName,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Select Ringtone") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor()
                            )

                            ExposedDropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false }
                            ) {
                                if (ringtones == null) {
                                    // Show loading state
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                                    }
                                } else {
                                    ringtones?.forEach { (uri, name) ->
                                        DropdownMenuItem(
                                            text = { Text(name) },
                                            onClick = {
                                                viewModel.updateRingtone(uri)
                                                expanded = false
                                            },
                                            leadingIcon = if (uri == alarmState?.ringtoneUri) {
                                                { Icon(Icons.Default.Check, contentDescription = null) }
                                            } else null
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Cycle type toggle
                Card {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Cycle Type",
                            style = MaterialTheme.typography.titleMedium
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (alarmState?.isRepeatable == true) "Repeatable" else "One Cycle",
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Text(
                                    text = if (alarmState?.isRepeatable == true)
                                        "Repeats weekly on selected days"
                                    else
                                        "Runs once through all selected days",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = alarmState?.isRepeatable == true,
                                onCheckedChange = { viewModel.updateCycleType(it) }
                            )
                        }
                    }
                }

                // Testing features
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Testing & Preview",
                            style = MaterialTheme.typography.titleMedium
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val context = androidx.compose.ui.platform.LocalContext.current
                            
                            OutlinedButton(
                                onClick = { viewModel.testAlarm(context) },
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(Icons.Default.Alarm, contentDescription = null, modifier = Modifier.size(20.dp))
                                    Spacer(Modifier.height(4.dp))
                                    Text("Test Alarm", style = MaterialTheme.typography.labelSmall)
                                    Text("(5 sec)", style = MaterialTheme.typography.labelSmall)
                                }
                            }

                            OutlinedButton(
                                onClick = { viewModel.previewRingtone(context) },
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(Icons.Default.MusicNote, contentDescription = null, modifier = Modifier.size(20.dp))
                                    Spacer(Modifier.height(4.dp))
                                    Text("Preview", style = MaterialTheme.typography.labelSmall)
                                    Text("Ringtone", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }

                        Text(
                            text = "Test alarm will ring in 5 seconds. Preview plays ringtone for 3 seconds.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )

                        // Show success message for test alarm
                        validationResult.errors["success"]?.let { message ->
                            Text(
                                text = message,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))

                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = { viewModel.saveAlarm() },
                        modifier = Modifier.weight(1f),
                        enabled = !isLoading
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Text("Save")
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickerDialog(
    initialTime: LocalTime,
    onDismiss: () -> Unit,
    onConfirm: (LocalTime) -> Unit
) {
    val timePickerState = rememberTimePickerState(
        initialHour = initialTime.hour,
        initialMinute = initialTime.minute,
        is24Hour = true
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Time") },
        text = {
            TimePicker(state = timePickerState)
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(LocalTime.of(timePickerState.hour, timePickerState.minute))
                }
            ) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

// Utility functions
private fun formatNotificationType(type: NotificationType): String {
    return when (type) {
        NotificationType.FULL_SCREEN -> "Full Screen"
        NotificationType.NOTIFICATION_POPUP -> "Notification Popup"
        NotificationType.SOUND_ONLY -> "Sound Only"
    }
}

private fun getNotificationTypeDescription(type: NotificationType): String {
    return when (type) {
        NotificationType.FULL_SCREEN -> "Wakes screen when locked, popup when unlocked"
        NotificationType.NOTIFICATION_POPUP -> "Shows heads-up notification"
        NotificationType.SOUND_ONLY -> "Plays 3-second beep without visual notification"
    }
}
