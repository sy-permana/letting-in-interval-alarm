package com.lettingin.intervalAlarm.ui.settings

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.lettingin.intervalAlarm.data.model.NotificationType
import com.lettingin.intervalAlarm.data.model.ThemeMode

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val settings by viewModel.settings.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val context = LocalContext.current

    // Permission states
    val notificationPermissionState = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        rememberPermissionState(Manifest.permission.POST_NOTIFICATIONS)
    } else {
        null
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Error message
            errorMessage?.let { error ->
                Card(
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
                        Text(
                            text = error,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.weight(1f)
                        )
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

            // Default Settings Section
            Text(
                text = "Default Alarm Settings",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary
            )

            // Default interval
            Card {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Default Interval",
                        style = MaterialTheme.typography.titleMedium
                    )

                    var intervalText by remember(settings?.defaultIntervalMinutes) {
                        mutableStateOf((settings?.defaultIntervalMinutes ?: 30).toString())
                    }

                    OutlinedTextField(
                        value = intervalText,
                        onValueChange = { newValue ->
                            intervalText = newValue
                            newValue.toIntOrNull()?.let { minutes ->
                                if (minutes >= 5) {
                                    viewModel.updateDefaultInterval(minutes)
                                }
                            }
                        },
                        label = { Text("Minutes") },
                        modifier = Modifier.fillMaxWidth(),
                        supportingText = { Text("Minimum 5 minutes") },
                        singleLine = true
                    )

                    // Quick interval buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(15, 30, 45, 60).forEach { minutes ->
                            FilterChip(
                                selected = settings?.defaultIntervalMinutes == minutes,
                                onClick = {
                                    viewModel.updateDefaultInterval(minutes)
                                    intervalText = minutes.toString()
                                },
                                label = { Text("${minutes}m") },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // Default notification type
            Card {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Default Notification Type",
                        style = MaterialTheme.typography.titleMedium
                    )

                    NotificationType.values().forEach { type ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = settings?.defaultNotificationType == type,
                                onClick = { viewModel.updateDefaultNotificationType(type) }
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

            // Appearance Section
            Text(
                text = "Appearance",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary
            )

            // Theme mode selector
            Card {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Theme",
                        style = MaterialTheme.typography.titleMedium
                    )

                    ThemeMode.values().forEach { mode ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = settings?.themeMode == mode,
                                onClick = { viewModel.updateThemeMode(mode) }
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = formatThemeMode(mode),
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }
            }

            // Permissions Section
            Text(
                text = "Permissions",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary
            )

            // Notification permission
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                Card {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Notification Permission",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = if (notificationPermissionState?.status?.isGranted == true)
                                    "Granted"
                                else
                                    "Not granted - Required for alarms",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (notificationPermissionState?.status?.isGranted == true)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.error
                            )
                        }

                        if (notificationPermissionState?.status?.isGranted != true) {
                            Button(
                                onClick = {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        notificationPermissionState?.launchPermissionRequest()
                                    } else {
                                        com.lettingin.intervalAlarm.util.PermissionHandler.openNotificationSettings(context)
                                    }
                                }
                            ) {
                                Text("Enable")
                            }
                        } else {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = "Granted",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            // Exact alarm permission (Android 12+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                Card {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Exact Alarm Permission",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "Required for precise alarm timing",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Button(
                            onClick = {
                                com.lettingin.intervalAlarm.util.PermissionHandler.openExactAlarmSettings(context)
                            }
                        ) {
                            Text("Check")
                        }
                    }
                }
            }

            // Show on Lock Screen - Manufacturer-specific guidance
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Lock,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = "⚠️ Show on Lock Screen",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
                    Text(
                        text = "REQUIRED to show alarm on lock screen without unlocking!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Divider(color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.3f))
                    
                    Text(
                        text = "This permission varies by manufacturer:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "• Xiaomi/MIUI: Settings > Apps > Manage Apps > Letting In > Other Permissions > Show on Lock Screen > Always Allow",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Text(
                            text = "• Oppo/ColorOS: Settings > Apps > App Management > Letting In > Permissions > Display on Lock Screen",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Text(
                            text = "• Vivo/FuntouchOS: Settings > Apps > Letting In > Permissions > Display on Lock Screen",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Text(
                            text = "• Samsung/One UI: Usually works by default with notification permission",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                    
                    Button(
                        onClick = {
                            com.lettingin.intervalAlarm.util.PermissionHandler.openAppSettings(context)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Open App Settings")
                    }
                }
            }

            // Full-screen intent permission (Android 14+ - CRITICAL for lock screen alarms!)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                Card(
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
                                text = "⚠️ Full-Screen Alarm Permission",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Text(
                                text = "REQUIRED to show alarm on lock screen without unlocking!",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Button(
                            onClick = {
                                com.lettingin.intervalAlarm.util.PermissionHandler.openFullScreenIntentSettings(context)
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Text("Enable")
                        }
                    }
                }
            }

            // Battery optimization
            Card {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Battery Optimization",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Disable to ensure alarms work reliably",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Button(
                        onClick = {
                            com.lettingin.intervalAlarm.util.PermissionHandler.openBatteryOptimizationSettings(context)
                        }
                    ) {
                        Text("Manage")
                    }
                }
            }

            // Autostart permission (manufacturer-specific)
            if (com.lettingin.intervalAlarm.util.AutostartHelper.hasAutostartRestrictions()) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Warning,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                text = "Background Autostart",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        
                        Text(
                            text = "REQUIRED for alarms to work after device reboot!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                            fontWeight = FontWeight.Bold
                        )
                        
                        Divider(color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.3f))
                        
                        Text(
                            text = "Detected: ${com.lettingin.intervalAlarm.util.AutostartHelper.getManufacturerName()} device",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                            fontWeight = FontWeight.Bold
                        )
                        
                        Text(
                            text = com.lettingin.intervalAlarm.util.AutostartHelper.getAutostartInstructions(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                        
                        Button(
                            onClick = {
                                val success = com.lettingin.intervalAlarm.util.AutostartHelper.openAutostartSettings(context)
                                if (!success) {
                                    // Fallback to app settings
                                    com.lettingin.intervalAlarm.util.PermissionHandler.openAppSettings(context)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.tertiary
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Settings, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Open Autostart Settings")
                        }
                    }
                }
            }

            // About Section
            Text(
                text = "About",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary
            )

            Card {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "App Version",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = "1.0.0",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Divider()

                    Text(
                        text = "Letting In - Interval Alarm",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "A lightweight interval alarm app for Android",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
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
        NotificationType.FULL_SCREEN -> "Wakes screen when locked"
        NotificationType.NOTIFICATION_POPUP -> "Shows heads-up notification"
        NotificationType.SOUND_ONLY -> "Plays 3-second beep"
    }
}

private fun formatThemeMode(mode: ThemeMode): String {
    return when (mode) {
        ThemeMode.LIGHT -> "Light"
        ThemeMode.DARK -> "Dark"
        ThemeMode.SYSTEM -> "System Default"
    }
}
