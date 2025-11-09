package com.lettingin.intervalAlarm.util

import android.content.Context
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
 * Centralized logging utility for the Letting In app.
 * Provides structured logging with different log levels and file export capability.
 */
@Singleton
class AppLogger @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val logBuffer = mutableListOf<LogEntry>()
    private val maxBufferSize = 500 // Keep last 500 log entries in memory
    
    companion object {
        private const val TAG = "AppLogger"
        private const val LOG_FILE_NAME = "letting_in_logs.txt"
        
        // Log categories
        const val CATEGORY_ALARM = "ALARM"
        const val CATEGORY_SCHEDULING = "SCHEDULING"
        const val CATEGORY_NOTIFICATION = "NOTIFICATION"
        const val CATEGORY_PERMISSION = "PERMISSION"
        const val CATEGORY_DATABASE = "DATABASE"
        const val CATEGORY_UI = "UI"
        const val CATEGORY_SYSTEM = "SYSTEM"
        const val CATEGORY_ERROR = "ERROR"
        const val CATEGORY_STATE_TRANSITION = "STATE_TRANSITION"
    }
    
    data class LogEntry(
        val timestamp: LocalDateTime,
        val level: LogLevel,
        val category: String,
        val tag: String,
        val message: String,
        val throwable: Throwable? = null
    )
    
    enum class LogLevel {
        VERBOSE, DEBUG, INFO, WARNING, ERROR
    }
    
    /**
     * Log a verbose message
     */
    fun v(category: String, tag: String, message: String) {
        log(LogLevel.VERBOSE, category, tag, message)
    }
    
    /**
     * Log a debug message
     */
    fun d(category: String, tag: String, message: String) {
        log(LogLevel.DEBUG, category, tag, message)
    }
    
    /**
     * Log an info message
     */
    fun i(category: String, tag: String, message: String) {
        log(LogLevel.INFO, category, tag, message)
    }
    
    /**
     * Log a warning message
     */
    fun w(category: String, tag: String, message: String, throwable: Throwable? = null) {
        log(LogLevel.WARNING, category, tag, message, throwable)
    }
    
    /**
     * Log an error message
     */
    fun e(category: String, tag: String, message: String, throwable: Throwable? = null) {
        log(LogLevel.ERROR, category, tag, message, throwable)
    }
    
    /**
     * Log alarm scheduling event
     */
    fun logAlarmScheduled(alarmId: Long, nextRingTime: Long) {
        val dateTime = LocalDateTime.ofInstant(
            java.time.Instant.ofEpochMilli(nextRingTime),
            java.time.ZoneId.systemDefault()
        )
        i(CATEGORY_SCHEDULING, "AlarmScheduler", 
            "Alarm scheduled: id=$alarmId, nextRing=$dateTime")
    }
    
    /**
     * Log alarm ring event
     */
    fun logAlarmRing(alarmId: Long, label: String) {
        i(CATEGORY_ALARM, "AlarmReceiver", 
            "Alarm ringing: id=$alarmId, label='$label'")
    }
    
    /**
     * Log alarm dismissal
     */
    fun logAlarmDismissed(alarmId: Long, isUserDismissal: Boolean) {
        val dismissType = if (isUserDismissal) "user" else "auto"
        i(CATEGORY_ALARM, "AlarmService", 
            "Alarm dismissed: id=$alarmId, type=$dismissType")
    }
    
    /**
     * Log alarm pause
     */
    fun logAlarmPaused(alarmId: Long, pauseDurationMinutes: Long) {
        i(CATEGORY_ALARM, "AlarmScheduler", 
            "Alarm paused: id=$alarmId, duration=${pauseDurationMinutes}min")
    }
    
    /**
     * Log alarm resume
     */
    fun logAlarmResumed(alarmId: Long) {
        i(CATEGORY_ALARM, "AlarmScheduler", 
            "Alarm resumed: id=$alarmId")
    }
    
    /**
     * Log permission change
     */
    fun logPermissionChange(permission: String, granted: Boolean) {
        val status = if (granted) "granted" else "denied"
        i(CATEGORY_PERMISSION, "PermissionChecker", 
            "Permission $status: $permission")
    }
    
    /**
     * Log database operation
     */
    fun logDatabaseOperation(operation: String, table: String, success: Boolean) {
        val status = if (success) "success" else "failed"
        d(CATEGORY_DATABASE, "Repository", 
            "Database $operation on $table: $status")
    }
    
    /**
     * Log system event
     */
    fun logSystemEvent(event: String, details: String) {
        i(CATEGORY_SYSTEM, "System", 
            "System event: $event - $details")
    }
    
    private fun log(
        level: LogLevel,
        category: String,
        tag: String,
        message: String,
        throwable: Throwable? = null
    ) {
        val entry = LogEntry(
            timestamp = LocalDateTime.now(),
            level = level,
            category = category,
            tag = tag,
            message = message,
            throwable = throwable
        )
        
        // Add to buffer
        synchronized(logBuffer) {
            logBuffer.add(entry)
            if (logBuffer.size > maxBufferSize) {
                logBuffer.removeAt(0)
            }
        }
        
        // Log to Android logcat
        val logMessage = "[$category] $message"
        when (level) {
            LogLevel.VERBOSE -> Log.v(tag, logMessage, throwable)
            LogLevel.DEBUG -> Log.d(tag, logMessage, throwable)
            LogLevel.INFO -> Log.i(tag, logMessage, throwable)
            LogLevel.WARNING -> Log.w(tag, logMessage, throwable)
            LogLevel.ERROR -> Log.e(tag, logMessage, throwable)
        }
    }
    
    /**
     * Export logs to file
     */
    fun exportLogsToFile(): File? {
        return try {
            val logFile = File(context.getExternalFilesDir(null), LOG_FILE_NAME)
            val logs = StringBuilder()
            
            logs.appendLine("=== Letting In Application Logs ===")
            logs.appendLine("Generated: ${LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)}")
            logs.appendLine("Total entries: ${logBuffer.size}")
            logs.appendLine()
            
            synchronized(logBuffer) {
                logBuffer.forEach { entry ->
                    logs.appendLine(formatLogEntry(entry))
                }
            }
            
            logFile.writeText(logs.toString())
            Log.i(TAG, "Logs exported to: ${logFile.absolutePath}")
            logFile
        } catch (e: Exception) {
            Log.e(TAG, "Failed to export logs", e)
            null
        }
    }
    
    /**
     * Get recent logs
     */
    fun getRecentLogs(count: Int = 100): List<LogEntry> {
        return synchronized(logBuffer) {
            logBuffer.takeLast(count)
        }
    }
    
    /**
     * Get logs by category
     */
    fun getLogsByCategory(category: String): List<LogEntry> {
        return synchronized(logBuffer) {
            logBuffer.filter { it.category == category }
        }
    }
    
    /**
     * Get logs by level
     */
    fun getLogsByLevel(level: LogLevel): List<LogEntry> {
        return synchronized(logBuffer) {
            logBuffer.filter { it.level == level }
        }
    }
    
    /**
     * Clear all logs
     */
    fun clearLogs() {
        synchronized(logBuffer) {
            logBuffer.clear()
        }
        Log.i(TAG, "Logs cleared")
    }
    
    private fun formatLogEntry(entry: LogEntry): String {
        val timestamp = entry.timestamp.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS"))
        val level = entry.level.name.padEnd(7)
        val category = entry.category.padEnd(12)
        val tag = entry.tag.padEnd(20)
        
        val builder = StringBuilder()
        builder.append("$timestamp | $level | $category | $tag | ${entry.message}")
        
        if (entry.throwable != null) {
            builder.append("\n")
            builder.append("  Exception: ${entry.throwable.javaClass.simpleName}: ${entry.throwable.message}")
            entry.throwable.stackTrace.take(5).forEach { stackElement ->
                builder.append("\n    at $stackElement")
            }
        }
        
        return builder.toString()
    }
}
