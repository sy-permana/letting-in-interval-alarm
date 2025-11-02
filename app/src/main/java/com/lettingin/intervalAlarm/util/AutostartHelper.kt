package com.lettingin.intervalAlarm.util

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log

/**
 * Helper class to open autostart/background restriction settings
 * for different Android manufacturers
 */
object AutostartHelper {
    
    private const val TAG = "AutostartHelper"
    
    /**
     * Check if the device manufacturer has autostart restrictions
     */
    fun hasAutostartRestrictions(): Boolean {
        val manufacturer = Build.MANUFACTURER.lowercase()
        return when {
            manufacturer.contains("xiaomi") -> true
            manufacturer.contains("redmi") -> true
            manufacturer.contains("huawei") -> true
            manufacturer.contains("honor") -> true
            manufacturer.contains("oppo") -> true
            manufacturer.contains("vivo") -> true
            manufacturer.contains("realme") -> true
            manufacturer.contains("oneplus") -> true
            manufacturer.contains("asus") -> true
            manufacturer.contains("letv") -> true
            else -> false
        }
    }
    
    /**
     * Get the manufacturer name for display
     */
    fun getManufacturerName(): String {
        return Build.MANUFACTURER.replaceFirstChar { it.uppercase() }
    }
    
    /**
     * Open autostart settings for the current device
     * Returns true if successful, false if not supported
     */
    fun openAutostartSettings(context: Context): Boolean {
        val manufacturer = Build.MANUFACTURER.lowercase()
        
        return try {
            when {
                // Xiaomi / MIUI
                manufacturer.contains("xiaomi") || manufacturer.contains("redmi") -> {
                    openMiuiAutostartSettings(context)
                }
                // Huawei / EMUI
                manufacturer.contains("huawei") || manufacturer.contains("honor") -> {
                    openHuaweiAutostartSettings(context)
                }
                // Oppo / ColorOS
                manufacturer.contains("oppo") -> {
                    openOppoAutostartSettings(context)
                }
                // Vivo
                manufacturer.contains("vivo") -> {
                    openVivoAutostartSettings(context)
                }
                // Realme
                manufacturer.contains("realme") -> {
                    openRealmeAutostartSettings(context)
                }
                // OnePlus
                manufacturer.contains("oneplus") -> {
                    openOnePlusAutostartSettings(context)
                }
                // Asus
                manufacturer.contains("asus") -> {
                    openAsusAutostartSettings(context)
                }
                // Letv
                manufacturer.contains("letv") -> {
                    openLetvAutostartSettings(context)
                }
                // Default: Open app settings
                else -> {
                    openAppSettings(context)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open autostart settings", e)
            // Fallback to app settings
            try {
                openAppSettings(context)
            } catch (e2: Exception) {
                Log.e(TAG, "Failed to open app settings", e2)
                false
            }
        }
    }
    
    /**
     * Xiaomi / MIUI autostart settings
     */
    private fun openMiuiAutostartSettings(context: Context): Boolean {
        return try {
            // Try MIUI autostart manager
            val intent = Intent().apply {
                component = ComponentName(
                    "com.miui.securitycenter",
                    "com.miui.permcenter.autostart.AutoStartManagementActivity"
                )
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            Log.w(TAG, "MIUI autostart activity not found, trying alternative", e)
            try {
                // Alternative MIUI intent
                val intent = Intent("miui.intent.action.APP_PERM_EDITOR").apply {
                    setClassName(
                        "com.miui.securitycenter",
                        "com.miui.permcenter.permissions.PermissionsEditorActivity"
                    )
                    putExtra("extra_pkgname", context.packageName)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
                true
            } catch (e2: Exception) {
                Log.e(TAG, "All MIUI intents failed", e2)
                false
            }
        }
    }
    
    /**
     * Huawei / EMUI autostart settings
     */
    private fun openHuaweiAutostartSettings(context: Context): Boolean {
        return try {
            val intent = Intent().apply {
                component = ComponentName(
                    "com.huawei.systemmanager",
                    "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity"
                )
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            Log.w(TAG, "Huawei autostart activity not found, trying alternative", e)
            try {
                val intent = Intent().apply {
                    component = ComponentName(
                        "com.huawei.systemmanager",
                        "com.huawei.systemmanager.appcontrol.activity.StartupAppControlActivity"
                    )
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
                true
            } catch (e2: Exception) {
                Log.e(TAG, "All Huawei intents failed", e2)
                false
            }
        }
    }
    
    /**
     * Oppo / ColorOS autostart settings
     */
    private fun openOppoAutostartSettings(context: Context): Boolean {
        return try {
            val intent = Intent().apply {
                component = ComponentName(
                    "com.coloros.safecenter",
                    "com.coloros.safecenter.permission.startup.StartupAppListActivity"
                )
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            Log.w(TAG, "Oppo autostart activity not found, trying alternative", e)
            try {
                val intent = Intent().apply {
                    component = ComponentName(
                        "com.oppo.safe",
                        "com.oppo.safe.permission.startup.StartupAppListActivity"
                    )
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
                true
            } catch (e2: Exception) {
                Log.e(TAG, "All Oppo intents failed", e2)
                false
            }
        }
    }
    
    /**
     * Vivo autostart settings
     */
    private fun openVivoAutostartSettings(context: Context): Boolean {
        return try {
            val intent = Intent().apply {
                component = ComponentName(
                    "com.vivo.permissionmanager",
                    "com.vivo.permissionmanager.activity.BgStartUpManagerActivity"
                )
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            Log.w(TAG, "Vivo autostart activity not found, trying alternative", e)
            try {
                val intent = Intent().apply {
                    component = ComponentName(
                        "com.iqoo.secure",
                        "com.iqoo.secure.ui.phoneoptimize.AddWhiteListActivity"
                    )
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
                true
            } catch (e2: Exception) {
                Log.e(TAG, "All Vivo intents failed", e2)
                false
            }
        }
    }
    
    /**
     * Realme autostart settings
     */
    private fun openRealmeAutostartSettings(context: Context): Boolean {
        return try {
            val intent = Intent().apply {
                component = ComponentName(
                    "com.coloros.safecenter",
                    "com.coloros.safecenter.permission.startup.StartupAppListActivity"
                )
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Realme autostart activity not found", e)
            false
        }
    }
    
    /**
     * OnePlus autostart settings
     */
    private fun openOnePlusAutostartSettings(context: Context): Boolean {
        return try {
            val intent = Intent().apply {
                component = ComponentName(
                    "com.oneplus.security",
                    "com.oneplus.security.chainlaunch.view.ChainLaunchAppListActivity"
                )
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            Log.e(TAG, "OnePlus autostart activity not found", e)
            false
        }
    }
    
    /**
     * Asus autostart settings
     */
    private fun openAsusAutostartSettings(context: Context): Boolean {
        return try {
            val intent = Intent().apply {
                component = ComponentName(
                    "com.asus.mobilemanager",
                    "com.asus.mobilemanager.MainActivity"
                )
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Asus autostart activity not found", e)
            false
        }
    }
    
    /**
     * Letv autostart settings
     */
    private fun openLetvAutostartSettings(context: Context): Boolean {
        return try {
            val intent = Intent().apply {
                component = ComponentName(
                    "com.letv.android.letvsafe",
                    "com.letv.android.letvsafe.AutobootManageActivity"
                )
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Letv autostart activity not found", e)
            false
        }
    }
    
    /**
     * Fallback: Open app settings
     */
    private fun openAppSettings(context: Context): Boolean {
        return try {
            val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = android.net.Uri.fromParts("package", context.packageName, null)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open app settings", e)
            false
        }
    }
    
    /**
     * Get user-friendly instructions for enabling autostart
     */
    fun getAutostartInstructions(): String {
        val manufacturer = Build.MANUFACTURER.lowercase()
        
        return when {
            manufacturer.contains("xiaomi") || manufacturer.contains("redmi") -> {
                "Enable 'Background autostart' in Security app → Permissions → Autostart"
            }
            manufacturer.contains("huawei") || manufacturer.contains("honor") -> {
                "Enable 'Auto-launch' in Phone Manager → App launch → Your app"
            }
            manufacturer.contains("oppo") -> {
                "Enable 'Auto-startup' in Settings → Security → Privacy → Startup manager"
            }
            manufacturer.contains("vivo") -> {
                "Enable 'Background run' in Settings → Battery → Background activity manager"
            }
            manufacturer.contains("realme") -> {
                "Enable 'Auto-startup' in Settings → App management → Startup manager"
            }
            manufacturer.contains("oneplus") -> {
                "Enable 'Auto-launch' in Settings → Battery → Battery optimization"
            }
            manufacturer.contains("asus") -> {
                "Enable 'Auto-start' in Mobile Manager → Auto-start manager"
            }
            else -> {
                "No autostart restrictions on this device"
            }
        }
    }
}
