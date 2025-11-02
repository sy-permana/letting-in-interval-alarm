package com.lettingin.intervalAlarm.util

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts

/**
 * Helper class to handle permission requests and navigation to settings
 */
object PermissionHandler {

    /**
     * Open notification settings for the app
     */
    fun openNotificationSettings(context: Context) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }

    /**
     * Open exact alarm settings for the app (Android 12+)
     */
    fun openExactAlarmSettings(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                data = Uri.fromParts("package", context.packageName, null)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        }
    }

    /**
     * Open full-screen intent settings for the app (Android 14+)
     */
    fun openFullScreenIntentSettings(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            try {
                val intent = Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT).apply {
                    data = Uri.fromParts("package", context.packageName, null)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
            } catch (e: Exception) {
                // Fallback to app details if the specific setting doesn't exist
                openAppSettings(context)
            }
        } else {
            // For older versions, open app details
            openNotificationSettings(context)
        }
    }

    /**
     * Open battery optimization settings for the app
     */
    fun openBatteryOptimizationSettings(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.fromParts("package", context.packageName, null)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        }
    }

    /**
     * Open app settings page
     */
    fun openAppSettings(context: Context) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }

    /**
     * Get a user-friendly description for a permission type
     */
    fun getPermissionDescription(permissionType: PermissionType): String {
        return when (permissionType) {
            PermissionType.NOTIFICATION -> 
                "Notification permission is required to display alarm notifications."
            PermissionType.EXACT_ALARM -> 
                "Exact alarm permission is required for precise alarm timing."
            PermissionType.FULL_SCREEN_INTENT -> 
                "Full-screen intent permission is required to wake the screen when the phone is locked."
            PermissionType.BATTERY_OPTIMIZATION -> 
                "Disabling battery optimization ensures alarms work reliably in the background."
            PermissionType.DISPLAY_OVER_OTHER_APPS ->
                "Display over other apps permission is required to show alarms on the lock screen."
        }
    }

    /**
     * Get a user-friendly title for a permission type
     */
    fun getPermissionTitle(permissionType: PermissionType): String {
        return when (permissionType) {
            PermissionType.NOTIFICATION -> "Notification Permission"
            PermissionType.EXACT_ALARM -> "Exact Alarm Permission"
            PermissionType.FULL_SCREEN_INTENT -> "Full-Screen Intent Permission"
            PermissionType.BATTERY_OPTIMIZATION -> "Battery Optimization"
            PermissionType.DISPLAY_OVER_OTHER_APPS -> "Display Over Other Apps"
        }
    }

    /**
     * Check if a permission type requires user action in settings
     */
    fun requiresSettingsNavigation(permissionType: PermissionType): Boolean {
        return when (permissionType) {
            PermissionType.NOTIFICATION -> false // Can be requested directly on Android 13+
            PermissionType.EXACT_ALARM -> true
            PermissionType.FULL_SCREEN_INTENT -> true
            PermissionType.BATTERY_OPTIMIZATION -> true
            PermissionType.DISPLAY_OVER_OTHER_APPS -> true
        }
    }

    /**
     * Open display over other apps settings
     */
    fun openDisplayOverOtherAppsSettings(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).apply {
                data = Uri.fromParts("package", context.packageName, null)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        }
    }

    /**
     * Open the appropriate settings page for a permission type
     */
    fun openPermissionSettings(context: Context, permissionType: PermissionType) {
        when (permissionType) {
            PermissionType.NOTIFICATION -> openNotificationSettings(context)
            PermissionType.EXACT_ALARM -> openExactAlarmSettings(context)
            PermissionType.FULL_SCREEN_INTENT -> openFullScreenIntentSettings(context)
            PermissionType.BATTERY_OPTIMIZATION -> openBatteryOptimizationSettings(context)
            PermissionType.DISPLAY_OVER_OTHER_APPS -> openDisplayOverOtherAppsSettings(context)
        }
    }
}

/**
 * Extension function to create a notification permission launcher
 * This should be called in onCreate or initialization
 */
fun ComponentActivity.createNotificationPermissionLauncher(
    onResult: (Boolean) -> Unit
): ActivityResultLauncher<String>? {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            onResult(isGranted)
        }
    } else {
        null
    }
}

/**
 * Request notification permission (Android 13+)
 */
fun requestNotificationPermission(
    launcher: ActivityResultLauncher<String>?
) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        launcher?.launch(android.Manifest.permission.POST_NOTIFICATIONS)
    }
}
