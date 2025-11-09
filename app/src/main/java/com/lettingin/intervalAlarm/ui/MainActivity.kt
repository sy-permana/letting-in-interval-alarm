package com.lettingin.intervalAlarm.ui

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.lettingin.intervalAlarm.ui.navigation.LettingInNavHost
import com.lettingin.intervalAlarm.ui.theme.LettingInTheme
import com.lettingin.intervalAlarm.util.PermissionChecker
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var permissionChecker: PermissionChecker
    
    @Inject
    lateinit var dataIntegrityChecker: com.lettingin.intervalAlarm.util.DataIntegrityChecker
    
    @Inject
    lateinit var appLogger: com.lettingin.intervalAlarm.util.AppLogger

    private var notificationPermissionLauncher: ActivityResultLauncher<String>? = null

    companion object {
        private const val PREFS_NAME = "app_prefs"
        private const val KEY_ONBOARDING_COMPLETED = "onboarding_completed"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        appLogger.i(com.lettingin.intervalAlarm.util.AppLogger.CATEGORY_SYSTEM, "MainActivity",
            "App started")

        // Run data integrity check on startup
        runDataIntegrityCheck()

        // Register notification permission launcher (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher = registerForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) { _ ->
                // Permission result is handled by the ViewModel/UI
                // This is just for registration
            }
        }

        // Check if onboarding has been completed
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val onboardingCompleted = prefs.getBoolean(KEY_ONBOARDING_COMPLETED, false)
        
        // Show onboarding if not completed OR if critical permissions are missing
        val showOnboarding = !onboardingCompleted || !permissionChecker.areAllCriticalPermissionsGranted()

        setContent {
            LettingInTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    LettingInNavHost(showOnboarding = showOnboarding)
                }
            }
        }
        
        // Mark onboarding as completed after first launch
        if (!onboardingCompleted) {
            prefs.edit().putBoolean(KEY_ONBOARDING_COMPLETED, true).apply()
        }
    }
    
    override fun onResume() {
        super.onResume()
        
        appLogger.d(com.lettingin.intervalAlarm.util.AppLogger.CATEGORY_SYSTEM, "MainActivity",
            "App resumed - triggering alarm state validation")
        
        // Trigger validation when app resumes
        // This will be picked up by HomeViewModel through the activeAlarm flow
        lifecycleScope.launch {
            try {
                // The validation will happen automatically in HomeViewModel
                // when it observes the activeAlarm flow
                appLogger.d(com.lettingin.intervalAlarm.util.AppLogger.CATEGORY_SYSTEM, "MainActivity",
                    "App resume validation triggered")
            } catch (e: Exception) {
                appLogger.e(com.lettingin.intervalAlarm.util.AppLogger.CATEGORY_ERROR, "MainActivity",
                    "Error during resume validation", e)
            }
        }
    }
    
    private fun runDataIntegrityCheck() {
        // Run integrity check in background using lifecycleScope
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val result = dataIntegrityChecker.runIntegrityCheck()
                
                if (result.corruptedAlarmsRemoved > 0 || 
                    result.orphanedStatesRemoved > 0 || 
                    result.oldStatisticsRemoved > 0) {
                    appLogger.i(com.lettingin.intervalAlarm.util.AppLogger.CATEGORY_SYSTEM, "MainActivity",
                        "Integrity check: removed ${result.corruptedAlarmsRemoved} corrupted alarms, " +
                        "${result.orphanedStatesRemoved} orphaned states, " +
                        "${result.oldStatisticsRemoved} old statistics")
                }
            } catch (e: Exception) {
                appLogger.e(com.lettingin.intervalAlarm.util.AppLogger.CATEGORY_ERROR, "MainActivity",
                    "Failed to run integrity check", e)
            }
        }
    }

    /**
     * Request notification permission
     * This can be called from anywhere in the app
     */
    fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher?.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
