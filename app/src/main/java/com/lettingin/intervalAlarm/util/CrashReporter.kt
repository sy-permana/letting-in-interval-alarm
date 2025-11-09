package com.lettingin.intervalAlarm.util

import android.content.Context
import android.os.Build
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Handles crash reporting and persistence.
 * Stores crash information to local storage before app termination.
 */
@Singleton
class CrashReporter @Inject constructor(
    @ApplicationContext private val context: Context,
    private val appLogger: AppLogger
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val crashReportsDir: File by lazy {
        File(context.filesDir, "crash_reports").apply {
            if (!exists()) {
                mkdirs()
            }
        }
    }
    
    companion object {
        private const val TAG = "CrashReporter"
        private const val MAX_CRASH_REPORTS = 10 // Keep last 10 crash reports
        const val CATEGORY_CRASH = "CRASH"
    }
    
    /**
     * Log a crash with app state snapshot
     */
    fun logCrash(
        throwable: Throwable,
        appState: Map<String, Any?> = emptyMap()
    ) {
        scope.launch {
            try {
                val deviceInfo = getDeviceInfo()
                val crashReport = CrashReport.fromThrowable(
                    throwable = throwable,
                    appState = appState,
                    deviceInfo = deviceInfo
                )
                
                // Log to AppLogger
                appLogger.e(
                    CATEGORY_CRASH,
                    TAG,
                    "Crash detected: ${crashReport.exceptionType} - ${crashReport.message}",
                    throwable
                )
                
                // Persist to file
                persistCrashReport(crashReport)
                
                // Clean up old crash reports
                cleanupOldCrashReports()
                
                Log.i(TAG, "Crash report saved successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to save crash report", e)
            }
        }
    }
    
    /**
     * Persist crash report to file
     */
    private fun persistCrashReport(crashReport: CrashReport) {
        try {
            val timestamp = LocalDateTime.ofInstant(
                java.time.Instant.ofEpochMilli(crashReport.timestamp),
                java.time.ZoneId.systemDefault()
            )
            val fileName = "crash_${timestamp.format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))}.txt"
            val file = File(crashReportsDir, fileName)
            
            file.writeText(crashReport.toFormattedString())
            
            Log.d(TAG, "Crash report persisted to: ${file.absolutePath}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to persist crash report", e)
        }
    }
    
    /**
     * Clean up old crash reports, keeping only the most recent ones
     */
    private fun cleanupOldCrashReports() {
        try {
            val crashFiles = crashReportsDir.listFiles()?.sortedByDescending { it.lastModified() }
            
            if (crashFiles != null && crashFiles.size > MAX_CRASH_REPORTS) {
                crashFiles.drop(MAX_CRASH_REPORTS).forEach { file ->
                    file.delete()
                    Log.d(TAG, "Deleted old crash report: ${file.name}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to cleanup old crash reports", e)
        }
    }
    
    /**
     * Get all crash reports
     */
    fun getAllCrashReports(): List<File> {
        return try {
            crashReportsDir.listFiles()?.sortedByDescending { it.lastModified() }?.toList() 
                ?: emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get crash reports", e)
            emptyList()
        }
    }
    
    /**
     * Get the most recent crash report
     */
    fun getLatestCrashReport(): File? {
        return try {
            crashReportsDir.listFiles()?.maxByOrNull { it.lastModified() }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get latest crash report", e)
            null
        }
    }
    
    /**
     * Delete all crash reports
     */
    fun clearAllCrashReports() {
        try {
            crashReportsDir.listFiles()?.forEach { it.delete() }
            Log.i(TAG, "All crash reports cleared")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear crash reports", e)
        }
    }
    
    /**
     * Export crash reports to a single file
     */
    fun exportCrashReports(): File? {
        return try {
            val exportFile = File(context.getExternalFilesDir(null), "crash_reports_export.txt")
            val reports = getAllCrashReports()
            
            val content = buildString {
                appendLine("=== LETTING IN CRASH REPORTS EXPORT ===")
                appendLine("Generated: ${LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)}")
                appendLine("Total reports: ${reports.size}")
                appendLine()
                
                reports.forEach { file ->
                    appendLine("--- Report: ${file.name} ---")
                    appendLine(file.readText())
                    appendLine()
                }
            }
            
            exportFile.writeText(content)
            Log.i(TAG, "Crash reports exported to: ${exportFile.absolutePath}")
            exportFile
        } catch (e: Exception) {
            Log.e(TAG, "Failed to export crash reports", e)
            null
        }
    }
    
    /**
     * Get device information for crash reports
     */
    private fun getDeviceInfo(): String {
        return buildString {
            append("Device: ${Build.MANUFACTURER} ${Build.MODEL}")
            append(", Android: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
            append(", App: ${context.packageName}")
        }
    }
}
