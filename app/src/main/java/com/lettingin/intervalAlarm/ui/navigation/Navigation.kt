package com.lettingin.intervalAlarm.ui.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.lettingin.intervalAlarm.ui.editor.AlarmEditorScreen
import com.lettingin.intervalAlarm.ui.home.HomeScreen
import com.lettingin.intervalAlarm.ui.settings.SettingsScreen
import com.lettingin.intervalAlarm.ui.statistics.StatisticsScreen

/**
 * Navigation routes for the app
 */
sealed class Screen(val route: String) {
    object PermissionOnboarding : Screen("permission_onboarding")
    object Home : Screen("home")
    object AlarmEditor : Screen("alarm_editor/{alarmId}") {
        fun createRoute(alarmId: Long?) = if (alarmId == null) {
            "alarm_editor/new"
        } else {
            "alarm_editor/$alarmId"
        }
    }
    object Settings : Screen("settings")
    object Statistics : Screen("statistics/{alarmId}") {
        fun createRoute(alarmId: Long) = "statistics/$alarmId"
    }
    object Debug : Screen("debug")
}

/**
 * Main navigation host for the app
 */
@Composable
fun LettingInNavHost(
    navController: NavHostController = rememberNavController(),
    startDestination: String = Screen.Home.route,
    showOnboarding: Boolean = false
) {
    // Determine actual start destination
    val actualStartDestination = if (showOnboarding) {
        Screen.PermissionOnboarding.route
    } else {
        startDestination
    }
    
    NavHost(
        navController = navController,
        startDestination = actualStartDestination
    ) {
        // Permission onboarding screen
        composable(Screen.PermissionOnboarding.route) {
            com.lettingin.intervalAlarm.ui.onboarding.PermissionOnboardingScreen(
                onComplete = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.PermissionOnboarding.route) { inclusive = true }
                    }
                }
            )
        }
        
        // Home screen
        composable(Screen.Home.route) {
            HomeScreen(
                onNavigateToEditor = { alarmId ->
                    navController.navigate(Screen.AlarmEditor.createRoute(alarmId))
                },
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route)
                },
                onNavigateToStatistics = { alarmId ->
                    navController.navigate(Screen.Statistics.createRoute(alarmId))
                }
            )
        }

        // Alarm editor screen
        composable(
            route = Screen.AlarmEditor.route,
            arguments = listOf(
                navArgument("alarmId") {
                    type = NavType.StringType
                    nullable = false
                }
            ),
            enterTransition = {
                slideInHorizontally(
                    initialOffsetX = { it },
                    animationSpec = tween(200)
                ) + fadeIn(animationSpec = tween(200))
            },
            exitTransition = {
                slideOutHorizontally(
                    targetOffsetX = { -it / 4 },
                    animationSpec = tween(200)
                ) + fadeOut(animationSpec = tween(200))
            },
            popEnterTransition = {
                slideInHorizontally(
                    initialOffsetX = { -it / 4 },
                    animationSpec = tween(200)
                ) + fadeIn(animationSpec = tween(200))
            },
            popExitTransition = {
                slideOutHorizontally(
                    targetOffsetX = { it },
                    animationSpec = tween(200)
                ) + fadeOut(animationSpec = tween(200))
            }
        ) { backStackEntry ->
            val alarmIdString = backStackEntry.arguments?.getString("alarmId")
            val alarmId = if (alarmIdString == "new") null else alarmIdString?.toLongOrNull()
            
            AlarmEditorScreen(
                alarmId = alarmId,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // Settings screen
        composable(
            route = Screen.Settings.route,
            enterTransition = {
                slideInHorizontally(
                    initialOffsetX = { it },
                    animationSpec = tween(200)
                ) + fadeIn(animationSpec = tween(200))
            },
            exitTransition = {
                slideOutHorizontally(
                    targetOffsetX = { -it / 4 },
                    animationSpec = tween(200)
                ) + fadeOut(animationSpec = tween(200))
            },
            popEnterTransition = {
                slideInHorizontally(
                    initialOffsetX = { -it / 4 },
                    animationSpec = tween(200)
                ) + fadeIn(animationSpec = tween(200))
            },
            popExitTransition = {
                slideOutHorizontally(
                    targetOffsetX = { it },
                    animationSpec = tween(200)
                ) + fadeOut(animationSpec = tween(200))
            }
        ) {
            SettingsScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // Statistics screen
        composable(
            route = Screen.Statistics.route,
            arguments = listOf(
                navArgument("alarmId") {
                    type = NavType.LongType
                    nullable = false
                }
            ),
            enterTransition = {
                slideInHorizontally(
                    initialOffsetX = { it },
                    animationSpec = tween(200)
                ) + fadeIn(animationSpec = tween(200))
            },
            exitTransition = {
                slideOutHorizontally(
                    targetOffsetX = { -it / 4 },
                    animationSpec = tween(200)
                ) + fadeOut(animationSpec = tween(200))
            },
            popEnterTransition = {
                slideInHorizontally(
                    initialOffsetX = { -it / 4 },
                    animationSpec = tween(200)
                ) + fadeIn(animationSpec = tween(200))
            },
            popExitTransition = {
                slideOutHorizontally(
                    targetOffsetX = { it },
                    animationSpec = tween(200)
                ) + fadeOut(animationSpec = tween(200))
            }
        ) { backStackEntry ->
            val alarmId = backStackEntry.arguments?.getLong("alarmId") ?: 0L
            
            StatisticsScreen(
                alarmId = alarmId,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // Debug screen
        composable(Screen.Debug.route) {
            com.lettingin.intervalAlarm.ui.debug.DebugScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}
