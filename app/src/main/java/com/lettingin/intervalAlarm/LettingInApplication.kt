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

    override fun onCreate() {
        super.onCreate()
        
        // Create notification channels
        notificationChannelManager.createNotificationChannels()
        
        // Handle post-restore operations if data was restored from backup
        backupHelper.handlePostRestore()
    }
}
