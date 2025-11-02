package com.lettingin.intervalAlarm.util

data class RingtoneInfo(
    val id: String,
    val name: String,
    val uri: String,
    val durationSeconds: Int
)

interface RingtoneManager {
    fun getAvailableRingtones(): List<RingtoneInfo>
    fun playRingtone(ringtoneUri: String, volumeLevel: Float)
    fun stopRingtone()
    fun playBeep()
    fun release()
}
