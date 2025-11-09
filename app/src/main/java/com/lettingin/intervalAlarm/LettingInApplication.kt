package com.lettingin.intervalAlarm

import android.app.Application
import com.lettingin.intervalAlarm.data.repository.AlarmRepository
import com.lettingin.intervalAlarm.util.AlarmStateRecoveryManager
import com.lettingin.intervalAlarm.util.AppLogger
import com.lettingin.intervalAlarm.util.NotificationChannelManager
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import javax.inject.Inject

@HiltAndroidApp
class LettingInApplication : Application() {

    @Inject
    lateinit var notificationChannelManager: NotificationChannelManager

    @Inject
    lateinit var backupHelper: com.lettingin.intervalAlarm.backup.BackupHelper
    
    @Inject
    lateinit var appLogger: AppLogger
    
    @Inject
    lateinit var crashReporter: com.lettingin.intervalAlarm.util.CrashReporter
    
    @Inject
    lateinit var alarmRepository: AlarmRepository
    
    @Inject
    lateinit var alarmStateRecoveryManager: AlarmStateRecoveryManager
    
    // Application-scoped coroutine scope for startup operations
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        
        // Install global exception handler for crash reporting
        com.lettingin.intervalAlarm.util.GlobalExceptionHandler.install(appLogger, crashReporter)
        
        appLogger.i(
            AppLogger.CATEGORY_SYSTEM,
            "LettingInApplication",
            "Application started, global exception handler installed"
        )
        
        // Create notification channels
        notificationChannelManager.createNotificationChannels()
        
        // Handle post-restore operations if data was restored from backup
        backupHelper.handlePostRestore()
        
        // Validate and recover alarm state on startup
        validateAndRecoverAlarmState()
    }
    
    /**
     * Validates and recovers alarm state for active alarms on app startup.
     * Runs on a background thread with a 2-second timeout to avoid blocking UI.
     */
    private fun validateAndRecoverAlarmState() {
        applicationScope.launch {
            val startTime = System.currentTimeMillis()
            appLogger.i(
                AlarmStateRecoveryManager.CATEGORY_STATE_RECOVERY,
                "LettingInApplication",
                "Starting startup alarm state validation"
            )
            
            try {
                // Set 2-second timeout for validation operations
                withTimeout(2000) {
                    // Get active alarm if one exists
                    val activeAlarm = alarmRepository.getActiveAlarm().firstOrNull()
                    
                    if (activeAlarm != null) {
                        appLogger.i(
                            AlarmStateRecoveryManager.CATEGORY_STATE_RECOVERY,
                            "LettingInApplication",
                            "Active alarm found: alarmId=${activeAlarm.id}, validating state"
                        )
                        
                        // Perform state recovery
                        val recoveryResult = alarmStateRecoveryManager.recoverAlarmState(activeAlarm.id)
                        
                        if (recoveryResult.success) {
                            appLogger.i(
                                AlarmStateRecoveryManager.CATEGORY_STATE_RECOVERY,
                                "LettingInApplication",
                                "Startup validation successful: ${recoveryResult.action}"
                            )
                        } else {
                            appLogger.e(
                                AppLogger.CATEGORY_ERROR,
                                "LettingInApplication",
                                "Startup validation failed: ${recoveryResult.action}",
                                recoveryResult.error
                            )
                            // Note: User notification will be handled by HomeViewModel when it initializes
                        }
                    } else {
                        appLogger.i(
                            AlarmStateRecoveryManager.CATEGORY_STATE_RECOVERY,
                            "LettingInApplication",
                            "No active alarm found, skipping validation"
                        )
                    }
                }
                
                val duration = System.currentTimeMillis() - startTime
                appLogger.i(
                    AlarmStateRecoveryManager.CATEGORY_STATE_RECOVERY,
                    "LettingInApplication",
                    "Startup validation completed in ${duration}ms"
                )
            } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
                val duration = System.currentTimeMillis() - startTime
                appLogger.w(
                    AppLogger.CATEGORY_ERROR,
                    "LettingInApplication",
                    "Startup validation timed out after ${duration}ms"
                )
            } catch (e: Exception) {
                val duration = System.currentTimeMillis() - startTime
                appLogger.e(
                    AppLogger.CATEGORY_ERROR,
                    "LettingInApplication",
                    "Startup validation failed after ${duration}ms",
                    e
                )
            }
        }
    }
    
    override fun onTerminate() {
        super.onTerminate()
        // Cancel all application-scoped coroutines
        applicationScope.cancel()
    }
}
