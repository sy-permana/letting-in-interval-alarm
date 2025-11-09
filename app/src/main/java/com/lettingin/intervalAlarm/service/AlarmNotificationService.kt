package com.lettingin.intervalAlarm.service

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.lettingin.intervalAlarm.R
import com.lettingin.intervalAlarm.data.model.NotificationType
import com.lettingin.intervalAlarm.data.repository.AlarmRepository
import com.lettingin.intervalAlarm.data.repository.AlarmStateRepository
import com.lettingin.intervalAlarm.data.repository.StatisticsRepository
import com.lettingin.intervalAlarm.domain.scheduler.AlarmScheduler
import com.lettingin.intervalAlarm.ui.MainActivity
import com.lettingin.intervalAlarm.util.NotificationChannelManager
import com.lettingin.intervalAlarm.util.RingtoneManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class AlarmNotificationService : Service() {

    @Inject
    lateinit var alarmRepository: AlarmRepository

    @Inject
    lateinit var alarmStateRepository: AlarmStateRepository

    @Inject
    lateinit var statisticsRepository: StatisticsRepository

    @Inject
    lateinit var alarmScheduler: AlarmScheduler

    @Inject
    lateinit var ringtoneManager: RingtoneManager
    
    @Inject
    lateinit var appLogger: com.lettingin.intervalAlarm.util.AppLogger

    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    private var autoDismissJob: Job? = null
    private var currentAlarmId: Long = -1

    companion object {
        const val EXTRA_ALARM_ID = "extra_alarm_id"
        const val ACTION_DISMISS = "action_dismiss"
        const val ACTION_STOP_FOR_DAY = "action_stop_for_day"
        const val NOTIFICATION_ID = 1001
        private const val AUTO_DISMISS_DELAY_MS = 15000L

        fun startService(context: Context, alarmId: Long) {
            val intent = Intent(context, AlarmNotificationService::class.java).apply {
                putExtra(EXTRA_ALARM_ID, alarmId)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        android.util.Log.d("AlarmNotificationService", "onStartCommand: intent=$intent, action=${intent?.action}")
        
        // Use the correct extra key from AlarmSchedulerImpl
        val alarmId = intent?.getLongExtra(com.lettingin.intervalAlarm.domain.scheduler.AlarmSchedulerImpl.EXTRA_ALARM_ID, -1L) ?: -1L
        android.util.Log.d("AlarmNotificationService", "onStartCommand: alarmId=$alarmId")

        when (intent?.action) {
            ACTION_DISMISS -> {
                android.util.Log.d("AlarmNotificationService", "Action: DISMISS")
                handleDismiss()
                return START_NOT_STICKY
            }
            ACTION_STOP_FOR_DAY -> {
                android.util.Log.d("AlarmNotificationService", "Action: STOP_FOR_DAY")
                handleStopForDay()
                return START_NOT_STICKY
            }
            else -> {
                android.util.Log.d("AlarmNotificationService", "Action: Show notification")
                if (alarmId != -1L) {
                    currentAlarmId = alarmId
                    serviceScope.launch {
                        showAlarmNotification(alarmId)
                    }
                } else {
                    android.util.Log.e("AlarmNotificationService", "Invalid alarm ID: $alarmId")
                }
            }
        }

        return START_NOT_STICKY
    }

    private suspend fun showAlarmNotification(alarmId: Long) {
        android.util.Log.d("AlarmNotificationService", "showAlarmNotification: alarmId=$alarmId")
        
        val alarm = alarmRepository.getAlarmById(alarmId).first() ?: run {
            android.util.Log.e("AlarmNotificationService", "Alarm not found: $alarmId")
            stopSelf()
            return
        }

        android.util.Log.d("AlarmNotificationService", "Alarm found: ${alarm.label}, type=${alarm.notificationType}")

        // Start foreground service with notification
        val notification = createNotification(alarm.label, alarm.notificationType)
        android.util.Log.d("AlarmNotificationService", "Starting foreground with notification")
        startForeground(NOTIFICATION_ID, notification)

        // For FULL_SCREEN type, the notification's full-screen intent will automatically launch the activity
        // No need to manually start the activity here - Android will do it for us
        android.util.Log.d("AlarmNotificationService", "Full-screen intent set in notification")

        // Play ringtone based on notification type
        android.util.Log.d("AlarmNotificationService", "Playing ringtone: ${alarm.ringtoneUri}")
        when (alarm.notificationType) {
            NotificationType.FULL_SCREEN, NotificationType.NOTIFICATION_POPUP -> {
                ringtoneManager.playRingtone(alarm.ringtoneUri, 1.0f)
            }
            NotificationType.SOUND_ONLY -> {
                ringtoneManager.playBeep()
            }
        }

        // Start auto-dismiss timer
        android.util.Log.d("AlarmNotificationService", "Starting auto-dismiss timer")
        startAutoDismissTimer()
    }

    private fun createNotification(label: String, notificationType: NotificationType): Notification {
        val channelId = when (notificationType) {
            NotificationType.FULL_SCREEN -> NotificationChannelManager.CHANNEL_ID_FULL_SCREEN
            NotificationType.NOTIFICATION_POPUP, NotificationType.SOUND_ONLY -> NotificationChannelManager.CHANNEL_ID_POPUP
        }

        val displayLabel = label.ifEmpty { "Interval Alarm" }

        // Create full-screen intent - use AlarmRingingActivity for full-screen
        val fullScreenIntent = Intent(this, com.lettingin.intervalAlarm.ui.alarm.AlarmRingingActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
            putExtra(com.lettingin.intervalAlarm.domain.scheduler.AlarmSchedulerImpl.EXTRA_ALARM_ID, currentAlarmId)
        }
        val fullScreenPendingIntent = PendingIntent.getActivity(
            this,
            0,
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Create dismiss action
        val dismissIntent = Intent(this, AlarmNotificationService::class.java).apply {
            action = ACTION_DISMISS
        }
        val dismissPendingIntent = PendingIntent.getService(
            this,
            1,
            dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Create stop for day action
        val stopForDayIntent = Intent(this, AlarmNotificationService::class.java).apply {
            action = ACTION_STOP_FOR_DAY
        }
        val stopForDayPendingIntent = PendingIntent.getService(
            this,
            2,
            stopForDayIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Get app icon as large icon for better branding
        val largeIcon = android.graphics.BitmapFactory.decodeResource(
            resources,
            R.mipmap.ic_launcher
        )
        
        val builder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setLargeIcon(largeIcon)
            .setContentTitle(displayLabel)
            .setContentText("Tap to dismiss")
            .setPriority(NotificationCompat.PRIORITY_MAX) // Changed to MAX for full-screen
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(false) // Don't auto-cancel for alarms
            .setOngoing(true)
            .setContentIntent(fullScreenPendingIntent)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC) // Show on lock screen
            .addAction(R.drawable.ic_notification, "Dismiss", dismissPendingIntent)
            .addAction(R.drawable.ic_notification, "Stop for Day", stopForDayPendingIntent)

        // Add full-screen intent for locked screen - this is what makes it show over lock screen
        if (notificationType == NotificationType.FULL_SCREEN) {
            android.util.Log.d("AlarmNotificationService", "Setting full-screen intent with high priority")
            builder.setFullScreenIntent(fullScreenPendingIntent, true)
        }

        return builder.build()
    }

    private fun startAutoDismissTimer() {
        autoDismissJob?.cancel()
        autoDismissJob = serviceScope.launch {
            delay(AUTO_DISMISS_DELAY_MS)
            handleAutoDismiss()
        }
    }

    private fun handleDismiss() {
        serviceScope.launch {
            appLogger.logAlarmDismissed(currentAlarmId, true)
            
            // Update statistics - increment user dismiss count
            val state = alarmStateRepository.getAlarmState(currentAlarmId).first()
            state?.let {
                alarmStateRepository.updateAlarmState(
                    it.copy(todayUserDismissCount = it.todayUserDismissCount + 1)
                )
            }

            // Update statistics in database
            val todayStats = statisticsRepository.getTodayStatistics(currentAlarmId)
            if (todayStats != null) {
                statisticsRepository.updateStatistics(
                    todayStats.copy(userDismissals = todayStats.userDismissals + 1)
                )
            }

            stopAlarmAndService()
        }
    }

    private fun handleAutoDismiss() {
        serviceScope.launch {
            appLogger.logAlarmDismissed(currentAlarmId, false)
            
            // Update statistics - increment auto dismiss count
            val state = alarmStateRepository.getAlarmState(currentAlarmId).first()
            state?.let {
                alarmStateRepository.updateAlarmState(
                    it.copy(todayAutoDismissCount = it.todayAutoDismissCount + 1)
                )
            }

            // Update statistics in database
            val todayStats = statisticsRepository.getTodayStatistics(currentAlarmId)
            if (todayStats != null) {
                statisticsRepository.updateStatistics(
                    todayStats.copy(autoDismissals = todayStats.autoDismissals + 1)
                )
            }

            stopAlarmAndService()
        }
    }

    private fun handleStopForDay() {
        serviceScope.launch {
            appLogger.i(com.lettingin.intervalAlarm.util.AppLogger.CATEGORY_ALARM, "AlarmService",
                "Stop for day requested: id=$currentAlarmId")
            alarmScheduler.stopForDay(currentAlarmId)
            stopAlarmAndService()
        }
    }

    private fun stopAlarmAndService() {
        ringtoneManager.stopRingtone()
        autoDismissJob?.cancel()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        ringtoneManager.stopRingtone()
        autoDismissJob?.cancel()
        // Cancel the service scope to prevent memory leaks
        serviceScope.coroutineContext.cancelChildren()
    }
}
