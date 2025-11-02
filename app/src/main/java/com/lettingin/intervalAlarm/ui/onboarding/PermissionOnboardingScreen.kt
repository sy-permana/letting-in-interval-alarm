package com.lettingin.intervalAlarm.ui.onboarding

import android.Manifest
import android.os.Build
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.lettingin.intervalAlarm.util.PermissionChecker
import com.lettingin.intervalAlarm.util.PermissionHandler

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun PermissionOnboardingScreen(
    onComplete: () -> Unit
) {
    val context = LocalContext.current
    val permissionChecker = remember { PermissionChecker(context) }
    
    // Track permission states
    var notificationGranted by remember { mutableStateOf(permissionChecker.isNotificationPermissionGranted()) }
    var exactAlarmGranted by remember { mutableStateOf(permissionChecker.isExactAlarmPermissionGranted()) }
    var batteryOptimizationDisabled by remember { mutableStateOf(permissionChecker.isBatteryOptimizationDisabled()) }
    
    // Notification permission state
    val notificationPermissionState = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        rememberPermissionState(Manifest.permission.POST_NOTIFICATIONS) { granted ->
            notificationGranted = granted
        }
    } else {
        null
    }
    
    // Check if all critical permissions are granted
    val allCriticalGranted = notificationGranted && exactAlarmGranted
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Setup Permissions") }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Header
            Icon(
                Icons.Default.Notifications,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            
            Text(
                text = "Welcome to Letting In!",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            
            Text(
                text = "To ensure your alarms work reliably, we need a few permissions:",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Divider()
            
            // Permission 1: Notifications
            PermissionCard(
                icon = Icons.Default.Notifications,
                title = "Notifications",
                description = "Required to show alarm notifications",
                isGranted = notificationGranted,
                isRequired = true,
                onGrant = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        notificationPermissionState?.launchPermissionRequest()
                    } else {
                        PermissionHandler.openNotificationSettings(context)
                    }
                }
            )
            
            // Permission 2: Exact Alarms
            PermissionCard(
                icon = Icons.Default.Schedule,
                title = "Exact Alarms",
                description = "Required for precise alarm timing",
                isGranted = exactAlarmGranted,
                isRequired = true,
                onGrant = {
                    PermissionHandler.openExactAlarmSettings(context)
                }
            )
            
            // Permission 3: Battery Optimization (Recommended)
            PermissionCard(
                icon = Icons.Default.BatteryFull,
                title = "Battery Optimization",
                description = "Recommended: Ensures alarms work in background",
                isGranted = batteryOptimizationDisabled,
                isRequired = false,
                onGrant = {
                    PermissionHandler.openBatteryOptimizationSettings(context)
                }
            )
            
            // Important Note for Lock Screen Display
            if (allCriticalGranted) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                            Text(
                                text = "One More Thing!",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = "Some devices (Xiaomi, Oppo, Vivo) require an additional \"Show on Lock Screen\" permission. Check Settings if alarms don't appear on your lock screen.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                }
            }
            
            Spacer(Modifier.height(16.dp))
            
            // Continue button
            Button(
                onClick = onComplete,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = allCriticalGranted
            ) {
                if (allCriticalGranted) {
                    Icon(Icons.Default.Check, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Continue", style = MaterialTheme.typography.titleMedium)
                } else {
                    Text("Grant Required Permissions", style = MaterialTheme.typography.titleMedium)
                }
            }
            
            if (!allCriticalGranted) {
                Text(
                    text = "Please grant all required permissions to continue",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center
                )
            }
            
            // Refresh button
            TextButton(
                onClick = {
                    notificationGranted = permissionChecker.isNotificationPermissionGranted()
                    exactAlarmGranted = permissionChecker.isExactAlarmPermissionGranted()
                    batteryOptimizationDisabled = permissionChecker.isBatteryOptimizationDisabled()
                }
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Refresh Status")
            }
        }
    }
}

@Composable
fun PermissionCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
    isGranted: Boolean,
    isRequired: Boolean,
    onGrant: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = if (isGranted) {
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        } else if (isRequired) {
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer
            )
        } else {
            CardDefaults.cardColors()
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = if (isGranted) {
                        MaterialTheme.colorScheme.primary
                    } else if (isRequired) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                )
                
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        if (isRequired) {
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = "*",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            if (isGranted) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = "Granted",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
            } else {
                Button(
                    onClick = onGrant,
                    colors = if (isRequired) {
                        ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    } else {
                        ButtonDefaults.buttonColors()
                    }
                ) {
                    Text("Grant")
                }
            }
        }
    }
}
