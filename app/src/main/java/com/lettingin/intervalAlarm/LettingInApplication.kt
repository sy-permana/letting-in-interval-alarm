package com.lettingin.intervalAlarm

import android.app.Application
import com.lettingin.intervalAlarm.util.NotificationChannelManager
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class LettingInApplication : Application() {

    @Inject
    lateinit var notificationChannelManager: NotificationChannelManager

    @Inject
    lateinit var backupHelper: com.lettingin.intervalAlarm.backup.BackupHelper
    
    @Inject
    lateinit var appLogger: com.lettingin.intervalAlarm.util.AppLogger
    
    @Inject
    lateinit var crashReporter: com.lettingin.intervalAlarm.util.CrashReporter

    override fun onCreate() {
        super.onCreate()
        
        // Install global exception handler for crash reporting
        com.lettingin.intervalAlarm.util.GlobalExceptionHandler.install(appLogger, crashReporter)
        
        appLogger.i(
            com.lettingin.intervalAlarm.util.AppLogger.CATEGORY_SYSTEM,
            "LettingInApplication",
            "Application started, global exception handler installed"
        )
        
        // Create notification channels
        notificationChannelManager.createNotificationChannels()
        
        // Handle post-restore operations if data was restored from backup
        backupHelper.handlePostRestore()
    }
}
