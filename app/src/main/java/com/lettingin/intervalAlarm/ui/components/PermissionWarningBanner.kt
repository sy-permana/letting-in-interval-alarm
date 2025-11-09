package com.lettingin.intervalAlarm.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.lettingin.intervalAlarm.util.PermissionChecker
import com.lettingin.intervalAlarm.util.PermissionHandler
import com.lettingin.intervalAlarm.util.PermissionType

/**
 * Banner that displays a warning when critical permissions are missing
 */
@Composable
fun PermissionWarningBanner(
    permissionChecker: PermissionChecker,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val missingPermissions = remember(permissionChecker) {
        permissionChecker.getMissingCriticalPermissions()
    }

    if (missingPermissions.isNotEmpty()) {
        Card(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.size(24.dp)
                )
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Permissions Required",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = buildPermissionMessage(missingPermissions),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Spacer(Modifier.height(8.dp))
                    TextButton(
                        onClick = onNavigateToSettings,
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                        ),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text("Enable Permissions")
                    }
                }
            }
        }
    }
}

private fun buildPermissionMessage(missingPermissions: List<PermissionType>): String {
    val permissionNames = missingPermissions.map { permission ->
        when (permission) {
            PermissionType.NOTIFICATION -> "Notifications"
            PermissionType.EXACT_ALARM -> "Exact Alarms"
            PermissionType.FULL_SCREEN_INTENT -> "Full-Screen Intents"
            PermissionType.BATTERY_OPTIMIZATION -> "Battery Optimization"
            PermissionType.DISPLAY_OVER_OTHER_APPS -> "Show on Lock Screen"
        }
    }
    
    return when (permissionNames.size) {
        1 -> "${permissionNames[0]} permission is required for alarms to work."
        2 -> "${permissionNames[0]} and ${permissionNames[1]} permissions are required for alarms to work."
        else -> {
            val last = permissionNames.last()
            val others = permissionNames.dropLast(1).joinToString(", ")
            "$others, and $last permissions are required for alarms to work."
        }
    }
}
