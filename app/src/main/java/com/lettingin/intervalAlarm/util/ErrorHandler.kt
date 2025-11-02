package com.lettingin.intervalAlarm.util

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.min
import kotlin.math.pow

/**
 * Centralized error handling utility for the Letting In app.
 * Provides error classification, retry logic, and user-friendly error messages.
 */
@Singleton
class ErrorHandler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val appLogger: AppLogger
) {
    
    companion object {
        private const val TAG = "ErrorHandler"
        private const val MAX_RETRY_ATTEMPTS = 3
        private const val INITIAL_RETRY_DELAY_MS = 1000L
        private const val MAX_RETRY_DELAY_MS = 10000L
    }
    
    /**
     * Error types for classification
     */
    enum class ErrorType {
        VALIDATION,
        PERMISSION,
        SCHEDULING,
        DATABASE,
        NETWORK,
        SYSTEM,
        UNKNOWN
    }
    
    /**
     * Error severity levels
     */
    enum class ErrorSeverity {
        LOW,      // Non-critical, can be ignored
        MEDIUM,   // Important but recoverable
        HIGH,     // Critical, requires user action
        CRITICAL  // App-breaking, requires immediate attention
    }
    
    /**
     * Structured error result
     */
    data class ErrorResult(
        val type: ErrorType,
        val severity: ErrorSeverity,
        val message: String,
        val userMessage: String,
        val isRecoverable: Boolean,
        val suggestedAction: String? = null,
        val originalException: Throwable? = null
    )
    
    /**
     * Handle an exception and return a structured error result
     */
    fun handleError(
        exception: Throwable,
        context: String = "",
        customMessage: String? = null
    ): ErrorResult {
        appLogger.e(AppLogger.CATEGORY_ERROR, TAG, 
            "Error in $context: ${exception.message}", exception)
        
        val errorResult = classifyError(exception, customMessage)
        
        // Log based on severity
        when (errorResult.severity) {
            ErrorSeverity.CRITICAL -> {
                appLogger.e(AppLogger.CATEGORY_ERROR, TAG,
                    "CRITICAL ERROR: ${errorResult.message}")
            }
            ErrorSeverity.HIGH -> {
                appLogger.e(AppLogger.CATEGORY_ERROR, TAG,
                    "HIGH SEVERITY: ${errorResult.message}")
            }
            ErrorSeverity.MEDIUM -> {
                appLogger.w(AppLogger.CATEGORY_ERROR, TAG,
                    "MEDIUM SEVERITY: ${errorResult.message}")
            }
            ErrorSeverity.LOW -> {
                appLogger.i(AppLogger.CATEGORY_ERROR, TAG,
                    "LOW SEVERITY: ${errorResult.message}")
            }
        }
        
        return errorResult
    }
    
    /**
     * Classify an error and determine its type, severity, and user message
     */
    private fun classifyError(
        exception: Throwable,
        customMessage: String? = null
    ): ErrorResult {
        return when (exception) {
            is ValidationException -> ErrorResult(
                type = ErrorType.VALIDATION,
                severity = ErrorSeverity.LOW,
                message = exception.message ?: "Validation failed",
                userMessage = exception.message ?: "Please check your input",
                isRecoverable = true,
                suggestedAction = "Review and correct the highlighted fields"
            )
            
            is PermissionException -> ErrorResult(
                type = ErrorType.PERMISSION,
                severity = ErrorSeverity.HIGH,
                message = exception.message ?: "Permission denied",
                userMessage = "Permission required: ${exception.permissionName}",
                isRecoverable = true,
                suggestedAction = "Grant permission in Settings"
            )
            
            is SchedulingException -> ErrorResult(
                type = ErrorType.SCHEDULING,
                severity = ErrorSeverity.HIGH,
                message = exception.message ?: "Scheduling failed",
                userMessage = customMessage ?: "Failed to schedule alarm. Please try again.",
                isRecoverable = true,
                suggestedAction = "Check alarm settings and try again"
            )
            
            is DatabaseException -> ErrorResult(
                type = ErrorType.DATABASE,
                severity = ErrorSeverity.MEDIUM,
                message = exception.message ?: "Database operation failed",
                userMessage = customMessage ?: "Failed to save data. Please try again.",
                isRecoverable = true,
                suggestedAction = "Restart the app if problem persists"
            )
            
            is CorruptedDataException -> ErrorResult(
                type = ErrorType.DATABASE,
                severity = ErrorSeverity.HIGH,
                message = exception.message ?: "Data corrupted",
                userMessage = "Alarm data is corrupted and will be removed",
                isRecoverable = true,
                suggestedAction = "The corrupted alarm has been removed. Please create a new one."
            )
            
            is IllegalArgumentException -> ErrorResult(
                type = ErrorType.VALIDATION,
                severity = ErrorSeverity.LOW,
                message = exception.message ?: "Invalid argument",
                userMessage = customMessage ?: "Invalid input provided",
                isRecoverable = true,
                suggestedAction = "Please check your input"
            )
            
            is IllegalStateException -> ErrorResult(
                type = ErrorType.SYSTEM,
                severity = ErrorSeverity.MEDIUM,
                message = exception.message ?: "Invalid state",
                userMessage = customMessage ?: "Operation not allowed in current state",
                isRecoverable = true,
                suggestedAction = "Please try again"
            )
            
            else -> ErrorResult(
                type = ErrorType.UNKNOWN,
                severity = ErrorSeverity.MEDIUM,
                message = exception.message ?: "Unknown error",
                userMessage = customMessage ?: "An unexpected error occurred",
                isRecoverable = true,
                suggestedAction = "Please try again or restart the app",
                originalException = exception
            )
        }
    }
    
    /**
     * Execute an operation with retry logic and exponential backoff
     */
    suspend fun <T> executeWithRetry(
        maxAttempts: Int = MAX_RETRY_ATTEMPTS,
        initialDelayMs: Long = INITIAL_RETRY_DELAY_MS,
        maxDelayMs: Long = MAX_RETRY_DELAY_MS,
        factor: Double = 2.0,
        operation: String = "operation",
        block: suspend () -> T
    ): Result<T> {
        var currentAttempt = 0
        var currentDelay = initialDelayMs
        var lastException: Throwable? = null
        
        while (currentAttempt < maxAttempts) {
            try {
                appLogger.d(AppLogger.CATEGORY_SYSTEM, TAG,
                    "Executing $operation (attempt ${currentAttempt + 1}/$maxAttempts)")
                
                val result = block()
                
                if (currentAttempt > 0) {
                    appLogger.i(AppLogger.CATEGORY_SYSTEM, TAG,
                        "$operation succeeded after ${currentAttempt + 1} attempts")
                }
                
                return Result.success(result)
            } catch (e: Exception) {
                lastException = e
                currentAttempt++
                
                if (currentAttempt >= maxAttempts) {
                    appLogger.e(AppLogger.CATEGORY_ERROR, TAG,
                        "$operation failed after $maxAttempts attempts", e)
                    break
                }
                
                appLogger.w(AppLogger.CATEGORY_SYSTEM, TAG,
                    "$operation failed (attempt $currentAttempt/$maxAttempts), retrying in ${currentDelay}ms")
                
                delay(currentDelay)
                currentDelay = min((currentDelay * factor).toLong(), maxDelayMs)
            }
        }
        
        return Result.failure(lastException ?: Exception("Operation failed after $maxAttempts attempts"))
    }
    
    /**
     * Execute an operation with error handling
     */
    suspend fun <T> executeSafely(
        operation: String = "operation",
        onError: ((ErrorResult) -> Unit)? = null,
        block: suspend () -> T
    ): Result<T> {
        return try {
            Result.success(block())
        } catch (e: Exception) {
            val errorResult = handleError(e, operation)
            onError?.invoke(errorResult)
            Result.failure(e)
        }
    }
    
    /**
     * Check if an error is recoverable
     */
    fun isRecoverable(exception: Throwable): Boolean {
        return when (exception) {
            is ValidationException,
            is PermissionException,
            is SchedulingException,
            is DatabaseException -> true
            else -> false
        }
    }
    
    /**
     * Get user-friendly error message
     */
    fun getUserMessage(exception: Throwable): String {
        return classifyError(exception).userMessage
    }
}

/**
 * Custom exception types
 */
class ValidationException(message: String) : Exception(message)
class PermissionException(val permissionName: String, message: String) : Exception(message)
class SchedulingException(message: String, cause: Throwable? = null) : Exception(message, cause)
class DatabaseException(message: String, cause: Throwable? = null) : Exception(message, cause)
class CorruptedDataException(message: String) : Exception(message)
