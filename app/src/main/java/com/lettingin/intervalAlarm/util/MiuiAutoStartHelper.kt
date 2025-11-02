package com.lettingin.intervalAlarm.util

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log

/**
 * Helper class to handle MIUI autostart permission
 */
object MiuiAutoStartHelper {
    
    private const val TAG = "MiuiAutoStartHelper"
    
    /**
     * Check if device is running MIUI
     */
    fun isMiuiDevice(): Boolean {
        return !getSystemProperty("ro.miui.ui.version.name").isNullOrEmpty()
    }
    
    /**
     * Open MIUI autostart settings for the app
     */
    fun openAutoStartSettings(context: Context): Boolean {
        try {
            Log.d(TAG, "Attempting to open MIUI autostart settings")
            
            val intent = Intent().apply {
                component = ComponentName(
                    "com.miui.securitycenter",
                    "com.miui.permcenter.autostart.AutoStartManagementActivity"
                )
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            
            context.startActivity(intent)
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open MIUI autostart settings", e)
            
            // Try alternative intent
            try {
                val intent = Intent("miui.intent.action.APP_PERM_EDITOR").apply {
                    putExtra("extra_pkgname", context.packageName)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                return true
            } catch (e2: Exception) {
                Log.e(TAG, "Failed to open alternative MIUI settings", e2)
                return false
            }
        }
    }
    
    /**
     * Get system property value
     */
    private fun getSystemProperty(key: String): String? {
        return try {
            val process = Runtime.getRuntime().exec("getprop $key")
            process.inputStream.bufferedReader().use { it.readText().trim() }
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Show dialog explaining autostart permission requirement
     */
    fun getAutoStartExplanation(): String {
        return """
            For alarms to work reliably after device reboot, you need to enable Autostart permission.
            
            Steps:
            1. Tap "Open Settings" below
            2. Find "Letting In" in the list
            3. Enable the toggle next to it
            4. Return to the app
            
            This allows the app to restore your active alarms when your device restarts.
        """.trimIndent()
    }
}
