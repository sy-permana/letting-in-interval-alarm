package com.lettingin.intervalAlarm.util

import android.util.Log
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CoroutineExceptionHandler

/**
 * Global exception handler for uncaught exceptions.
 * Logs exceptions and persists crash information before app termination.
 */
class GlobalExceptionHandler(
    private val appLogger: AppLogger,
    private val crashReporter: CrashReporter,
    private val defaultHandler: Thread.UncaughtExceptionHandler?
) : Thread.UncaughtExceptionHandler {
    
    companion object {
        private const val TAG = "GlobalExceptionHandler"
        
        /**
         * Install the global exception handler
         */
        fun install(appLogger: AppLogger, crashReporter: CrashReporter) {
            val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
            val globalHandler = GlobalExceptionHandler(appLogger, crashReporter, defaultHandler)
            Thread.setDefaultUncaughtExceptionHandler(globalHandler)
            Log.i(TAG, "Global exception handler installed")
        }
    }
    
    override fun uncaughtException(thread: Thread, throwable: Throwable) {
        try {
            Log.e(TAG, "Uncaught exception on thread: ${thread.name}", throwable)
            
            // Log to AppLogger
            appLogger.e(
                AppLogger.CATEGORY_ERROR,
                TAG,
                "Uncaught exception on thread: ${thread.name}",
                throwable
            )
            
            // Collect app state
            val appState = mapOf(
                "thread" to thread.name,
                "threadId" to thread.id,
                "timestamp" to System.currentTimeMillis()
            )
            
            // Log crash with state
            crashReporter.logCrash(throwable, appState)
            
            // Give some time for crash report to be written
            Thread.sleep(500)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error in global exception handler", e)
        } finally {
            // Call the default handler to let the system handle the crash
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }
}

/**
 * Coroutine exception handler that integrates with crash reporting
 */
class GlobalCoroutineExceptionHandler(
    private val appLogger: AppLogger,
    private val crashReporter: CrashReporter
) : CoroutineExceptionHandler {
    
    companion object {
        private const val TAG = "GlobalCoroutineExceptionHandler"
    }
    
    override val key: CoroutineContext.Key<*>
        get() = CoroutineExceptionHandler
    
    override fun handleException(context: CoroutineContext, exception: Throwable) {
        try {
            Log.e(TAG, "Uncaught coroutine exception", exception)
            
            // Log to AppLogger
            appLogger.e(
                AppLogger.CATEGORY_ERROR,
                TAG,
                "Uncaught coroutine exception in context: $context",
                exception
            )
            
            // Collect app state
            val appState = mapOf(
                "coroutineContext" to context.toString(),
                "timestamp" to System.currentTimeMillis()
            )
            
            // Log crash with state
            crashReporter.logCrash(exception, appState)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error in coroutine exception handler", e)
        }
    }
}
