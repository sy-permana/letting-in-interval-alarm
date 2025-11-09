package com.lettingin.intervalAlarm.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.os.Build
import androidx.core.app.NotificationManagerCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationChannelManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        const val CHANNEL_ID_FULL_SCREEN = "alarm_full_screen_v2"
        const val CHANNEL_ID_POPUP = "alarm_popup_v2"
        const val CHANNEL_NAME_FULL_SCREEN = "Full Screen Alarms"
        const val CHANNEL_NAME_POPUP = "Popup Notifications"
        const val CHANNEL_DESCRIPTION_FULL_SCREEN = "High priority alarms that display full screen"
        const val CHANNEL_DESCRIPTION_POPUP = "Standard alarm notifications"
    }

    fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // Full-screen alarm channel with high priority
            val fullScreenChannel = NotificationChannel(
                CHANNEL_ID_FULL_SCREEN,
                CHANNEL_NAME_FULL_SCREEN,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = CHANNEL_DESCRIPTION_FULL_SCREEN
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 250, 500)
                setSound(
                    null, // We'll handle sound through MediaPlayer
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
                setBypassDnd(true)
            }

            // Popup notification channel with default priority
            val popupChannel = NotificationChannel(
                CHANNEL_ID_POPUP,
                CHANNEL_NAME_POPUP,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = CHANNEL_DESCRIPTION_POPUP
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 250, 250, 250)
                setSound(
                    null, // We'll handle sound through MediaPlayer
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            }

            notificationManager.createNotificationChannel(fullScreenChannel)
            notificationManager.createNotificationChannel(popupChannel)
        }
    }

    fun areNotificationsEnabled(): Boolean {
        return NotificationManagerCompat.from(context).areNotificationsEnabled()
    }
}
