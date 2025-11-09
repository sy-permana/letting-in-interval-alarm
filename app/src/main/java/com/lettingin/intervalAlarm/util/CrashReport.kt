package com.lettingin.intervalAlarm.util

/**
 * Data model for storing crash information.
 * Used to persist crash details before app termination.
 */
data class CrashReport(
    val timestamp: Long,
    val exceptionType: String,
    val message: String,
    val stackTrace: String,
    val appState: String, // String representation of relevant app state
    val threadName: String = "",
    val deviceInfo: String = ""
) {
    companion object {
        /**
         * Create a CrashReport from a Throwable
         */
        fun fromThrowable(
            throwable: Throwable,
            appState: Map<String, Any?> = emptyMap(),
            deviceInfo: String = ""
        ): CrashReport {
            val stackTraceString = throwable.stackTraceToString()
            
            // Convert app state map to string representation
            val appStateString = if (appState.isEmpty()) {
                "{}"
            } else {
                appState.entries.joinToString(", ", "{", "}") { (key, value) ->
                    "\"$key\": \"$value\""
                }
            }
            
            return CrashReport(
                timestamp = System.currentTimeMillis(),
                exceptionType = throwable.javaClass.name,
                message = throwable.message ?: "No message",
                stackTrace = stackTraceString,
                appState = appStateString,
                threadName = Thread.currentThread().name,
                deviceInfo = deviceInfo
            )
        }
    }
    
    /**
     * Convert crash report to formatted string for logging
     */
    fun toFormattedString(): String {
        val timestamp = java.time.LocalDateTime.ofInstant(
            java.time.Instant.ofEpochMilli(timestamp),
            java.time.ZoneId.systemDefault()
        )
        
        return buildString {
            appendLine("=== CRASH REPORT ===")
            appendLine("Timestamp: $timestamp")
            appendLine("Exception Type: $exceptionType")
            appendLine("Message: $message")
            appendLine("Thread: $threadName")
            if (deviceInfo.isNotEmpty()) {
                appendLine("Device Info: $deviceInfo")
            }
            appendLine("\nStack Trace:")
            appendLine(stackTrace)
            appendLine("\nApp State:")
            appendLine(appState)
            appendLine("===================")
        }
    }
}
