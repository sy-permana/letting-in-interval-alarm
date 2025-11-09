package com.lettingin.intervalAlarm.ui.alarm

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lettingin.intervalAlarm.data.repository.AlarmRepository
import com.lettingin.intervalAlarm.service.AlarmNotificationService
import com.lettingin.intervalAlarm.ui.theme.LettingInTheme
import com.lettingin.intervalAlarm.util.TimeFormatter
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@AndroidEntryPoint
class AlarmRingingActivity : ComponentActivity() {

    @Inject
    lateinit var alarmRepository: AlarmRepository

    private var alarmId: Long = -1
    private var wakeLock: android.os.PowerManager.WakeLock? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // CRITICAL: Set these flags BEFORE setContentView
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O_MR1) {
            // Android 8.1+
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            // For older Android versions, use deprecated flags
            @Suppress("DEPRECATION")
            window.addFlags(
                android.view.WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                android.view.WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                android.view.WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
            )
        }
        
        // Keep screen on flag works on all versions
        window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // Acquire wake lock to ensure screen turns on
        // Note: Window flags handle most of the screen wake, this is a backup
        try {
            val powerManager = getSystemService(android.content.Context.POWER_SERVICE) as android.os.PowerManager
            @Suppress("DEPRECATION")
            wakeLock = powerManager.newWakeLock(
                android.os.PowerManager.SCREEN_DIM_WAKE_LOCK or  // Use DIM instead of BRIGHT for battery savings
                android.os.PowerManager.ACQUIRE_CAUSES_WAKEUP,
                "LettingIn:AlarmWakeLock"
            )
            // Reduced from 10 minutes to 30 seconds (2x auto-dismiss time)
            // This prevents excessive battery drain if alarm is not dismissed
            wakeLock?.acquire(30 * 1000L) // 30 seconds max
        } catch (e: Exception) {
            android.util.Log.e("AlarmRingingActivity", "Failed to acquire wake lock", e)
            // Activity will still work with window flags as fallback
        }

        alarmId = intent.getLongExtra(AlarmNotificationService.EXTRA_ALARM_ID, -1L)

        setContent {
            LettingInTheme {
                AlarmRingingScreen(
                    alarmId = alarmId,
                    alarmRepository = alarmRepository,
                    onDismiss = { handleDismiss() },
                    onStopForDay = { handleStopForDay() }
                )
            }
        }
    }

    private fun handleDismiss() {
        val intent = Intent(this, AlarmNotificationService::class.java).apply {
            action = AlarmNotificationService.ACTION_DISMISS
        }
        startService(intent)
        finish()
    }

    private fun handleStopForDay() {
        val intent = Intent(this, AlarmNotificationService::class.java).apply {
            action = AlarmNotificationService.ACTION_STOP_FOR_DAY
        }
        startService(intent)
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        // Release wake lock with proper error handling
        try {
            wakeLock?.let {
                if (it.isHeld) {
                    it.release()
                    android.util.Log.d("AlarmRingingActivity", "Wake lock released")
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("AlarmRingingActivity", "Error releasing wake lock", e)
        } finally {
            wakeLock = null
        }
    }
}

@Composable
fun AlarmRingingScreen(
    alarmId: Long,
    alarmRepository: AlarmRepository,
    onDismiss: () -> Unit,
    onStopForDay: () -> Unit
) {
    var alarmLabel by remember { mutableStateOf("Interval Alarm") }
    var currentTime by remember { mutableStateOf(LocalTime.now()) }
    var countdown by remember { mutableStateOf(15) }
    var showStopConfirmation by remember { mutableStateOf(false) }

    // Load alarm details
    LaunchedEffect(alarmId) {
        val alarm = alarmRepository.getAlarmById(alarmId).first()
        alarm?.let {
            alarmLabel = it.label.ifEmpty { "Interval Alarm" }
        }
    }

    // Update current time every second
    LaunchedEffect(Unit) {
        while (true) {
            currentTime = LocalTime.now()
            delay(1000)
        }
    }

    // Countdown timer
    LaunchedEffect(Unit) {
        while (countdown > 0) {
            delay(1000)
            countdown--
        }
        if (countdown == 0) {
            onDismiss()
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top section - Countdown
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(top = 48.dp)
                ) {
                    Text(
                        text = "Auto-dismiss in",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                    )
                    Text(
                        text = "${countdown}s",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                // Middle section - Alarm info
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = currentTime.format(DateTimeFormatter.ofPattern("HH:mm:ss")),
                        style = MaterialTheme.typography.displayLarge,
                        fontWeight = FontWeight.Bold,
                        fontSize = 72.sp,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = alarmLabel,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                // Bottom section - Action buttons
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(bottom = 48.dp)
                ) {
                    // Large Dismiss Button
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(120.dp)
                            .clip(CircleShape),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text(
                            text = "Dismiss",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    // Stop for Day Button with Slide Confirmation
                    if (!showStopConfirmation) {
                        OutlinedButton(
                            onClick = { showStopConfirmation = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Text(
                                text = "Stop for Day",
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    } else {
                        SlideToConfirm(
                            onConfirm = onStopForDay,
                            onCancel = { showStopConfirmation = false }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SlideToConfirm(
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    var offsetX by remember { mutableStateOf(0f) }
    val maxOffset = 250f

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(MaterialTheme.colorScheme.errorContainer)
    ) {
        // Background text
        Text(
            text = "Slide to confirm stop for day",
            modifier = Modifier.align(Alignment.Center),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onErrorContainer
        )

        // Sliding button
        Box(
            modifier = Modifier
                .offset(x = offsetX.dp)
                .size(56.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.error)
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            if (offsetX >= maxOffset) {
                                onConfirm()
                            } else {
                                offsetX = 0f
                            }
                        },
                        onHorizontalDrag = { _, dragAmount ->
                            offsetX = (offsetX + dragAmount / density).coerceIn(0f, maxOffset)
                        }
                    )
                }
                .align(Alignment.CenterStart),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "→",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }

    // Cancel button
    LaunchedEffect(Unit) {
        delay(5000)
        onCancel()
    }
}
