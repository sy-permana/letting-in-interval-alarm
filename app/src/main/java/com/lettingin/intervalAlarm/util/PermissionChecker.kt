package com.lettingin.intervalAlarm.util

import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.PowerManager
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Utility class to check various permissions required by the app
 */
@Singleton
class PermissionChecker @Inject constructor(
    @ApplicationContext private val context: Context
) {

    /**
     * Check if notification permission is granted
     * Required for Android 13+ (API 33+)
     */
    fun isNotificationPermissionGranted(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            // Notification permission is automatically granted on Android 12 and below
            true
        }
    }

    /**
     * Check if exact alarm permission is granted
     * Required for Android 12+ (API 31+)
     */
    fun isExactAlarmPermissionGranted(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.canScheduleExactAlarms()
        } else {
            // Exact alarm permission is automatically granted on Android 11 and below
            true
        }
    }

    /**
     * Check if full-screen intent permission is granted
     * Required for Android 14+ (API 34+) for full-screen notifications
     */
    fun isFullScreenIntentPermissionGranted(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            // On Android 14+, check if we can use full-screen intents
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
            notificationManager.canUseFullScreenIntent()
        } else {
            // Full-screen intent permission is automatically granted on Android 13 and below
            true
        }
    }

    /**
     * Check if battery optimization is disabled for the app
     * Recommended for reliable alarm delivery
     */
    fun isBatteryOptimizationDisabled(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            powerManager.isIgnoringBatteryOptimizations(context.packageName)
        } else {
            // Battery optimization doesn't exist on Android 5 and below
            true
        }
    }

    /**
     * Check if device is running MIUI
     */
    fun isMiuiDevice(): Boolean {
        return !android.os.Build.MANUFACTURER.isNullOrEmpty() && 
               android.os.Build.MANUFACTURER.equals("Xiaomi", ignoreCase = true)
    }

    /**
     * Check if "Display over other apps" permission is granted
     * CRITICAL for showing alarm on lock screen without unlocking
     * 
     * Note: On MIUI devices, there's an additional "Show on Lock Screen" permission
     * that cannot be checked programmatically. Users must enable it manually in:
     * Settings > Apps > Manage Apps > [App Name] > Other Permissions > Show on Lock Screen
     */
    fun canDrawOverlays(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            android.provider.Settings.canDrawOverlays(context)
        } else {
            // Permission automatically granted on Android 5 and below
            true
        }
    }

    /**
     * Check if all critical permissions are granted
     * Critical permissions are those required for basic alarm functionality
     */
    fun areAllCriticalPermissionsGranted(): Boolean {
        return isNotificationPermissionGranted() && 
               isExactAlarmPermissionGranted()
    }

    /**
     * Check if all recommended permissions are granted
     * Includes critical permissions plus optional ones for better functionality
     */
    fun areAllRecommendedPermissionsGranted(): Boolean {
        return areAllCriticalPermissionsGranted() &&
                isFullScreenIntentPermissionGranted() &&
                isBatteryOptimizationDisabled()
    }

    /**
     * Get a list of missing critical permissions
     */
    fun getMissingCriticalPermissions(): List<PermissionType> {
        val missing = mutableListOf<PermissionType>()
        
        if (!isNotificationPermissionGranted()) {
            missing.add(PermissionType.NOTIFICATION)
        }
        
        if (!isExactAlarmPermissionGranted()) {
            missing.add(PermissionType.EXACT_ALARM)
        }
        
        return missing
    }

    /**
     * Get a list of missing recommended permissions
     */
    fun getMissingRecommendedPermissions(): List<PermissionType> {
        val missing = mutableListOf<PermissionType>()
        
        if (!isFullScreenIntentPermissionGranted()) {
            missing.add(PermissionType.FULL_SCREEN_INTENT)
        }
        
        if (!isBatteryOptimizationDisabled()) {
            missing.add(PermissionType.BATTERY_OPTIMIZATION)
        }
        
        return missing
    }

    /**
     * Get permission status summary
     */
    fun getPermissionStatus(): PermissionStatus {
        return PermissionStatus(
            notificationGranted = isNotificationPermissionGranted(),
            exactAlarmGranted = isExactAlarmPermissionGranted(),
            fullScreenIntentGranted = isFullScreenIntentPermissionGranted(),
            batteryOptimizationDisabled = isBatteryOptimizationDisabled(),
            displayOverOtherAppsGranted = canDrawOverlays()
        )
    }
}

/**
 * Types of permissions used by the app
 */
enum class PermissionType {
    NOTIFICATION,
    EXACT_ALARM,
    FULL_SCREEN_INTENT,
    BATTERY_OPTIMIZATION,
    DISPLAY_OVER_OTHER_APPS
}

/**
 * Data class representing the status of all permissions
 */
data class PermissionStatus(
    val notificationGranted: Boolean,
    val exactAlarmGranted: Boolean,
    val fullScreenIntentGranted: Boolean,
    val batteryOptimizationDisabled: Boolean,
    val displayOverOtherAppsGranted: Boolean
) {
    val allCriticalGranted: Boolean
        get() = notificationGranted && exactAlarmGranted
    
    val allRecommendedGranted: Boolean
        get() = allCriticalGranted && fullScreenIntentGranted && batteryOptimizationDisabled
}
