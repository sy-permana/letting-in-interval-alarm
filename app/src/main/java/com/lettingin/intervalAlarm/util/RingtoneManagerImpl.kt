package com.lettingin.intervalAlarm.util

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.ToneGenerator
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RingtoneManagerImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : RingtoneManager {

    private var mediaPlayer: MediaPlayer? = null
    private var toneGenerator: ToneGenerator? = null
    private val job = kotlinx.coroutines.SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.Main + job)

    private val builtInRingtones: List<RingtoneInfo>
        get() {
            val ringtones = mutableListOf<RingtoneInfo>()
            
            try {
                // Get system alarm ringtones
                val ringtoneManager = android.media.RingtoneManager(context)
                ringtoneManager.setType(android.media.RingtoneManager.TYPE_ALARM)
                val cursor = ringtoneManager.cursor
                
                var count = 0
                while (cursor.moveToNext() && count < 5) {
                    val title = cursor.getString(android.media.RingtoneManager.TITLE_COLUMN_INDEX)
                    val uri = ringtoneManager.getRingtoneUri(cursor.position).toString()
                    
                    ringtones.add(
                        RingtoneInfo(
                            id = "alarm_$count",
                            name = title,
                            uri = uri,
                            durationSeconds = 5
                        )
                    )
                    count++
                }
                
                cursor.close()
            } catch (e: Exception) {
                e.printStackTrace()
                // Fallback to default alarm sound
                val defaultUri = android.provider.Settings.System.DEFAULT_ALARM_ALERT_URI?.toString() 
                    ?: "content://settings/system/alarm_alert"
                ringtones.add(
                    RingtoneInfo(
                        id = "system_default",
                        name = "System Default",
                        uri = defaultUri,
                        durationSeconds = 5
                    )
                )
            }
            
            return ringtones
        }

    override fun getAvailableRingtones(): List<RingtoneInfo> {
        return builtInRingtones
    }

    override fun playRingtone(ringtoneUri: String, volumeLevel: Float) {
        stopRingtone()

        try {
            android.util.Log.d("RingtoneManager", "Playing ringtone: $ringtoneUri")
            
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            val alarmVolume = audioManager.getStreamVolume(AudioManager.STREAM_ALARM)
            val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM)
            val volume = (alarmVolume.toFloat() / maxVolume) * volumeLevel

            android.util.Log.d("RingtoneManager", "Volume: $alarmVolume/$maxVolume (${volume * 100}%)")

            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                setDataSource(context, Uri.parse(ringtoneUri))
                setVolume(volume, volume)
                isLooping = true
                prepare()
                start()
            }
            
            android.util.Log.d("RingtoneManager", "Ringtone playing successfully")
        } catch (e: Exception) {
            android.util.Log.e("RingtoneManager", "Failed to play ringtone: ${e.message}", e)
            e.printStackTrace()
            // Ensure MediaPlayer is released on error
            mediaPlayer?.release()
            mediaPlayer = null
            // Fallback to beep if ringtone fails
            playBeep()
        }
    }

    override fun stopRingtone() {
        mediaPlayer?.apply {
            if (isPlaying) {
                stop()
            }
            release()
        }
        mediaPlayer = null

        toneGenerator?.release()
        toneGenerator = null
    }

    override fun playBeep() {
        stopRingtone()

        try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            val alarmVolume = audioManager.getStreamVolume(AudioManager.STREAM_ALARM)
            val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM)
            val volumePercent = (alarmVolume * 100) / maxVolume

            toneGenerator = ToneGenerator(AudioManager.STREAM_ALARM, volumePercent)
            
            // Play beep for 3 seconds
            scope.launch {
                try {
                    toneGenerator?.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 3000)
                    delay(3000)
                    stopRingtone()
                } catch (e: Exception) {
                    e.printStackTrace()
                    toneGenerator?.release()
                    toneGenerator = null
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // Ensure ToneGenerator is released on error
            toneGenerator?.release()
            toneGenerator = null
        }
    }

    override fun release() {
        stopRingtone()
        // Cancel all coroutines to prevent leaks
        job.cancel()
    }
}
