# Implementation Plan

- [x] 1. Set up project structure and dependencies
  - Create new Android project with Kotlin and Jetpack Compose
  - Configure build.gradle with required dependencies (Compose, Room, Hilt, WorkManager, Coroutines)
  - Set up Hilt dependency injection
  - Configure minimum SDK 26 and target SDK 34
  - Add required permissions to AndroidManifest.xml
  - _Requirements: All requirements depend on proper project setup_

- [x] 2. Implement data layer with Room database
  - _Requirements: 1.1, 1.2, 1.3, 2.1, 2.2, 2.3, 2.4, 2.5, 2.6, 2.7, 2.8, 11.1, 11.2, 11.3, 11.4, 11.5, 11.6, 12.1, 12.2, 16.1, 16.2, 16.3_

- [x] 2.1 Create database entities
  - Write IntervalAlarm entity with all fields (id, label, times, interval, days, notification type, ringtone)
  - Write AlarmCycleStatistics entity for tracking ring counts and dismissals
  - Write AlarmState entity for tracking current alarm execution state
  - Write AppSettings entity for app configuration
  - Create type converters for LocalTime, LocalDate, DayOfWeek Set, and enums
  - _Requirements: 2.1, 2.2, 2.3, 2.4, 2.5, 2.6, 2.7, 2.8, 11.2, 11.3, 11.4, 11.5, 11.6, 12.1, 12.2_

- [x] 2.2 Create Room database and DAOs
  - Write AlarmDao with CRUD operations and query for active alarm
  - Write StatisticsDao with insert, update, and query operations
  - Write AlarmStateDao with state management operations
  - Write SettingsDao with settings CRUD operations
  - Create AppDatabase class with all DAOs and entities
  - _Requirements: 1.1, 1.2, 1.3, 16.1, 16.2_

- [x] 2.3 Implement repository layer
  - Write AlarmRepositoryImpl with Flow-based data access
  - Write StatisticsRepositoryImpl with statistics management
  - Write AlarmStateRepositoryImpl with state tracking
  - Write SettingsRepositoryImpl with settings management
  - Implement repository interfaces with proper error handling
  - _Requirements: 1.1, 1.2, 1.3, 11.7, 12.1, 12.2, 12.3, 12.4, 16.1, 16.2, 16.3_

- [ ]* 2.4 Write unit tests for repositories
  - Create unit tests for AlarmRepository CRUD operations
  - Create unit tests for StatisticsRepository with in-memory database
  - Create unit tests for AlarmStateRepository state management
  - Test data transformations and edge cases
  - _Requirements: 1.1, 1.2, 1.3, 12.1, 12.2, 16.1, 16.2_

- [x] 3. Implement alarm scheduling system
  - _Requirements: 5.1, 5.2, 5.3, 5.4, 5.5, 6.1, 6.2, 6.3, 6.4, 14.1, 14.2, 14.3, 14.4, 15.1, 15.2, 15.3_

- [x] 3.1 Create AlarmScheduler service
  - Write AlarmScheduler interface with schedule, cancel, pause, resume methods
  - Implement AlarmSchedulerImpl using AlarmManager
  - Write logic to calculate next ring time based on start time, interval, and current time
  - Implement day-of-week filtering logic
  - Implement end time validation to prevent scheduling beyond end time
  - Handle one-cycle vs repeatable mode logic
  - _Requirements: 5.1, 5.2, 5.3, 5.4, 5.5, 6.1, 6.2, 6.3, 6.4_

- [x] 3.2 Implement AlarmReceiver
  - Create AlarmReceiver BroadcastReceiver to handle alarm events
  - Extract alarm ID from intent extras
  - Start AlarmNotificationService as foreground service
  - Update statistics (increment ring count)
  - Calculate and schedule next ring time
  - Handle time change detection and validation
  - _Requirements: 5.1, 5.2, 5.3, 5.4, 5.5, 11.3, 12.2, 15.1, 15.2, 15.3_

- [x] 3.3 Implement BootReceiver
  - Create BootReceiver to handle BOOT_COMPLETED action
  - Query active alarm from database
  - Calculate next ring time based on current time and alarm configuration
  - Restore alarm scheduling if within active time window
  - _Requirements: 14.2, 14.3, 16.3_

- [ ]* 3.4 Write unit tests for alarm scheduling
  - Test next ring time calculation with various intervals
  - Test day-of-week filtering logic
  - Test end time boundary conditions
  - Test one-cycle vs repeatable mode
  - Test pause and resume calculations
  - Mock AlarmManager for testing
  - _Requirements: 5.1, 5.2, 5.3, 5.4, 5.5, 6.1, 6.2, 6.3, 6.4_


- [x] 4. Implement notification and ringtone system
  - _Requirements: 7.1, 7.2, 7.3, 7.4, 7.5, 7.6, 8.1, 8.2, 8.3, 8.4, 9.1, 9.2, 9.3, 9.4, 17.1, 17.2_

- [x] 4.1 Create RingtoneManager
  - Write RingtoneManager interface for ringtone operations
  - Implement RingtoneManagerImpl with MediaPlayer
  - Create 3-5 built-in ringtone resources
  - Implement playRingtone method with volume control matching system alarm volume
  - Implement stopRingtone method
  - Implement playBeep method for 3-second sound-only notification
  - _Requirements: 7.5, 7.6_

- [x] 4.2 Create notification channels
  - Create notification channel for full-screen alarms with high priority
  - Create notification channel for popup notifications with default priority
  - Configure notification channel settings (sound, vibration, importance)
  - _Requirements: 7.1, 7.2, 7.3_

- [x] 4.3 Implement AlarmNotificationService
  - Create AlarmNotificationService as foreground service
  - Implement notification display based on notification type (full-screen, popup, sound-only)
  - Create full-screen intent for locked screen alarms
  - Implement ringtone playback using RingtoneManager
  - Implement 15-second auto-dismiss timer
  - Handle dismiss action and update statistics
  - Handle stop-for-day action with confirmation
  - Display alarm label in notification
  - _Requirements: 7.1, 7.2, 7.3, 7.4, 7.5, 7.6, 8.1, 8.2, 8.3, 8.4, 9.1, 9.2, 9.3, 9.4, 17.1, 17.2_

- [x] 4.4 Create AlarmRingingActivity
  - Create full-screen activity for alarm display
  - Display alarm label and current time
  - Implement large dismiss button
  - Implement stop-for-day button with slide or double-tap confirmation
  - Show auto-dismiss countdown indicator
  - Handle dismiss action and communicate with service
  - Handle stop-for-day action and communicate with service
  - _Requirements: 7.1, 7.2, 8.1, 8.2, 9.1, 9.2, 9.3, 17.1, 17.2_

- [ ]* 4.5 Write tests for notification system
  - Test notification channel creation
  - Test ringtone playback and stopping
  - Test auto-dismiss timer
  - Test dismiss and stop-for-day actions
  - _Requirements: 7.1, 7.2, 7.3, 7.4, 7.5, 8.1, 8.2, 8.3, 8.4_

- [x] 5. Implement pause and resume functionality
  - _Requirements: 10.1, 10.2, 10.3, 10.4, 10.5, 10.6, 10.7_

- [x] 5.1 Add pause logic to AlarmScheduler
  - Implement pauseAlarm method to cancel current alarm and set pause state
  - Calculate pause end time based on duration options (1x interval, 30 min, 1 hour, custom)
  - Validate that pause end time plus interval doesn't exceed alarm end time
  - Schedule resume alarm at pause end time
  - Update AlarmState with pause information
  - _Requirements: 10.2, 10.3, 10.5_

- [x] 5.2 Add resume logic to AlarmScheduler
  - Implement resumeAlarm method to clear pause state
  - Calculate next ring time from current time
  - Validate next ring time doesn't exceed end time
  - Schedule next alarm ring
  - Handle automatic resume after pause duration
  - Reset pause state at end of day
  - _Requirements: 10.4, 10.6, 10.7_

- [ ]* 5.3 Write tests for pause and resume
  - Test pause duration calculations
  - Test pause validation (exceeding end time)
  - Test automatic resume after pause duration
  - Test pause state reset at day end
  - _Requirements: 10.2, 10.3, 10.4, 10.5, 10.6, 10.7_

- [x] 6. Implement ViewModels and business logic
  - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5, 2.1, 2.2, 2.3, 2.4, 2.5, 2.6, 2.7, 2.8, 3.1, 3.2, 3.3, 3.4, 4.1, 4.2, 4.3, 4.4, 10.1, 11.1, 11.2, 11.3, 11.4, 11.5, 11.6, 11.7, 13.1, 13.2, 13.3, 13.4, 13.5, 13.6_

- [x] 6.1 Create HomeViewModel
  - Implement StateFlow for all alarms list
  - Implement StateFlow for active alarm
  - Implement StateFlow for active alarm state
  - Implement StateFlow for today's statistics
  - Implement activateAlarm method with single active alarm enforcement
  - Implement deactivateAlarm method
  - Implement deleteAlarm method with active alarm check
  - Implement pauseAlarm method with duration options
  - Implement resumeAlarm method
  - _Requirements: 1.1, 1.2, 1.3, 1.4, 3.1, 3.2, 3.3, 3.4, 4.1, 4.2, 4.3, 4.4, 10.1, 11.1, 11.2, 11.3, 11.4, 11.5, 11.6_

- [x] 6.2 Create AlarmEditorViewModel
  - Implement state management for alarm being edited
  - Load default settings from SettingsRepository
  - Implement validation logic for all alarm fields
  - Validate start time is before end time
  - Validate interval is between 5 minutes and time range
  - Validate at least one day is selected
  - Validate label length (max 60 characters)
  - Check alarm count limit (max 10)
  - Implement saveAlarm method with validation
  - _Requirements: 1.5, 2.1, 2.2, 2.3, 2.4, 2.5, 2.6, 2.7, 2.8_

- [x] 6.3 Create SettingsViewModel
  - Implement StateFlow for app settings
  - Implement updateDefaultInterval method
  - Implement updateDefaultNotificationType method
  - Implement updateThemeMode method
  - Load and save settings from SettingsRepository
  - _Requirements: 13.1, 13.2, 13.3, 13.4_

- [x] 6.4 Create StatisticsViewModel
  - Implement StateFlow for alarm statistics (last 5 cycles)
  - Implement loadStatistics method for specific alarm
  - Format statistics data for display
  - _Requirements: 12.1, 12.2, 12.3, 12.4_

- [ ]* 6.5 Write unit tests for ViewModels
  - Test HomeViewModel alarm activation and deactivation
  - Test HomeViewModel pause and resume logic
  - Test AlarmEditorViewModel validation logic
  - Test SettingsViewModel settings updates
  - Test StatisticsViewModel data loading
  - Mock repositories for testing
  - _Requirements: 1.1, 1.2, 1.3, 2.1, 2.2, 2.3, 2.4, 2.5, 2.6, 2.7, 2.8, 10.1, 13.1, 13.2, 13.3_

- [x] 7. Implement UI with Jetpack Compose
  - _Requirements: 1.1, 1.2, 1.3, 1.4, 2.1, 2.2, 2.3, 2.4, 2.5, 2.6, 2.7, 2.8, 10.1, 11.1, 11.2, 11.3, 11.4, 11.5, 11.6, 12.3, 13.1, 13.2, 13.3, 13.4, 13.5, 13.6_

- [x] 7.1 Create HomeScreen composable
  - Display list of all alarms with LazyColumn
  - Show active alarm at top with distinct visual design
  - Display alarm details (label, start time, end time, interval, active days)
  - Show active alarm statistics (next ring, today's rings, time until end, dismiss counts)
  - Implement pause/resume button for active alarm
  - Implement activate/deactivate toggle for each alarm
  - Implement delete button for inactive alarms
  - Show "Create Interval Alarm" FAB when under 10 alarms
  - Navigate to AlarmEditorScreen on create or edit
  - Navigate to SettingsScreen
  - Navigate to StatisticsScreen
  - _Requirements: 1.1, 1.2, 1.3, 1.4, 4.1, 4.2, 4.3, 10.1, 11.1, 11.2, 11.3, 11.4, 11.5, 11.6_

- [x] 7.2 Create AlarmEditorScreen composable
  - Create form with text field for label (max 60 chars)
  - Add time pickers for start and end time
  - Add interval selector with 1-minute increments (5 min to time range)
  - Add day-of-week selector with chip group
  - Add notification type selector with radio buttons
  - Add ringtone selector dropdown
  - Add cycle type toggle (one-cycle vs repeatable)
  - Display validation errors inline
  - Implement save button with validation
  - Implement cancel button
  - Show error if trying to edit active alarm
  - _Requirements: 2.1, 2.2, 2.3, 2.4, 2.5, 2.6, 2.7, 2.8, 4.1_

- [x] 7.3 Create SettingsScreen composable
  - Display default interval setting with number picker
  - Display default notification type selector
  - Display theme mode selector (light/dark/system)
  - Show notification permission status with action button
  - Show battery optimization status with action button
  - Display app version and about information
  - Implement navigation back to home
  - _Requirements: 13.1, 13.2, 13.3, 13.4, 13.5, 13.6_

- [x] 7.4 Create StatisticsScreen composable
  - Display list of last 5 cycles for selected alarm
  - Show cycle date, total rings, user dismissals, auto-dismissals for each cycle
  - Create simple visual representation (bar chart or progress indicators)
  - Implement navigation back to home
  - _Requirements: 12.1, 12.2, 12.3, 12.4_

- [x] 7.5 Implement navigation
  - Set up NavHost with all screen destinations
  - Implement navigation from HomeScreen to AlarmEditorScreen
  - Implement navigation from HomeScreen to SettingsScreen
  - Implement navigation from HomeScreen to StatisticsScreen
  - Pass alarm ID parameter to AlarmEditorScreen for editing
  - Pass alarm ID parameter to StatisticsScreen
  - Handle back navigation properly
  - _Requirements: All UI requirements_

- [x] 7.6 Implement Material 3 theming
  - Create theme with light and dark color schemes
  - Apply Material 3 components throughout app
  - Implement dynamic color support for Android 12+
  - Ensure proper contrast and accessibility
  - _Requirements: 13.4_

- [ ]* 7.7 Write UI tests
  - Test HomeScreen alarm list display and interactions
  - Test AlarmEditorScreen form validation and submission
  - Test SettingsScreen settings updates
  - Test navigation between screens
  - Use Compose Testing framework2
  - _Requirements: 1.1, 1.2, 1.3, 2.1, 2.2, 2.3, 2.4, 2.5, 2.6, 2.7, 2.8, 13.1, 13.2, 13.3, 13.4_

- [x] 8. Implement permission handling
  - _Requirements: 13.5, 13.6, 14.1, 14.2, 14.3, 14.4_

- [x] 8.1 Create permission checker utility
  - Write utility to check notification permission status
  - Write utility to check exact alarm permission status (Android 12+)
  - Write utility to check full-screen intent permission status
  - Write utility to check battery optimization status
  - _Requirements: 13.5, 13.6_

- [x] 8.2 Implement permission request flows
  - Request notification permission with rationale dialog
  - Request exact alarm permission with deep link to settings
  - Request full-screen intent permission with deep link to settings
  - Request battery optimization exemption with deep link to settings
  - Show permission status in SettingsScreen
  - _Requirements: 13.5, 13.6_

- [x] 8.3 Handle permission denial scenarios
  - Show persistent warning when critical permissions are denied
  - Prevent alarm activation without required permissions
  - Provide guidance to enable permissions manually
  - _Requirements: 13.5, 13.6, 14.1, 14.2, 14.3, 14.4_

- [x] 9. Implement data persistence and migration
  - _Requirements: 16.1, 16.2, 16.3, 16.4_

- [x] 9.1 Set up Room database migrations
  - Create initial database schema (version 1)
  - Implement database migration strategy for future versions
  - Test database migrations with sample data
  - _Requirements: 16.1, 16.2, 16.3_

- [x] 9.2 Implement data backup and restore
  - Use Android Auto Backup for app data
  - Configure backup rules in XML
  - Test data restoration after app reinstall
  - _Requirements: 16.4_

- [-] 10. Final integration and polish
  - _Requirements: All requirements_

- [x] 10.1 Integrate all components
  - Wire up all ViewModels with repositories and schedulers
  - Connect UI screens with ViewModels
  - Test end-to-end alarm creation, activation, and ringing flow
  - Test pause, resume, and stop-for-day flows
  - Verify all navigation paths work correctly
  - _Requirements: All requirements_

- [ ] 10.2 Handle edge cases and error scenarios
  - _Requirements: 14.2, 14.3, 14.4, 15.1, 15.2, 15.3_

- [ ] 10.2.1 Test and handle system time changes
  - Test alarm behavior when system time is changed forward
  - Test alarm behavior when system time is changed backward
  - Verify alarm continues correctly after time change
  - Ensure next ring time is recalculated properly
  - Validate that alarm doesn't ring multiple times or skip rings
  - _Requirements: 15.1, 15.2, 15.3_

- [ ] 10.2.2 Test and handle timezone changes
  - Test alarm behavior when timezone changes during active alarm
  - Verify alarm time adjusts correctly to new timezone
  - Test timezone change outside active time window
  - Ensure proper time conversion and scheduling
  - _Requirements: 15.1, 15.2, 15.3_

- [x] 10.2.3 Test device reboot scenarios
  - Verify BootReceiver restores active alarm correctly
  - Test reboot during active time window
  - Test reboot outside active time window
  - Test reboot when alarm is paused
  - Verify alarm state is restored properly
  - Test with one-cycle mode vs repeatable mode
  - _Requirements: 14.2, 14.3_

- [x] 10.2.4 Test Doze mode and battery restrictions
  - Verify alarms fire correctly in Doze mode
  - Test with battery saver enabled
  - Verify AlarmManager.setExactAndAllowWhileIdle works as expected
  - Test app behavior when battery optimization is enabled
  - Verify foreground service continues during Doze
  - _Requirements: 14.1, 14.2, 14.3, 14.4_

- [ ] 10.2.5 Test Do Not Disturb mode
  - Verify alarm behavior with DND enabled
  - Check if alarms bypass DND (as they should for alarm category)
  - Test different DND priority settings
  - Verify notification display during DND
  - _Requirements: 7.1, 7.2, 7.3, 14.3_

- [x] 10.2.6 Implement comprehensive error handling
  - Add retry logic for failed alarm scheduling with exponential backoff
  - Handle AlarmManager scheduling failures gracefully
  - Implement database transaction error recovery
  - Add error logging for debugging
  - Show user-friendly error messages for critical failures
  - Handle corrupted alarm data scenarios
  - _Requirements: All requirements_

- [ ] 10.3 Optimize performance
  - _Requirements: All requirements_

- [x] 10.3.1 Profile and fix memory leaks
  - Use Android Profiler to detect memory leaks
  - Check ViewModel lifecycle management
  - Verify coroutine cancellation in all ViewModels
  - Check for leaked contexts in services and receivers
  - Verify MediaPlayer and wake lock release
  - Fix any identified memory leaks
  - _Requirements: All requirements_

- [x] 10.3.2 Optimize database performance
  - Add index on alarm_state.alarmId column
  - Add index on alarm_statistics.alarmId column
  - Add index on alarm_statistics.cycleDate column
  - Add index on interval_alarms.isActive column
  - Review and optimize complex queries
  - Test database performance with 10 alarms and 50 statistics entries
  - _Requirements: 1.1, 1.2, 1.3, 12.1, 12.2, 16.1, 16.2_

- [x] 10.3.3 Minimize wake lock usage
  - Audit all wake lock acquisition and release points
  - Ensure wake locks are released on errors and exceptions
  - Use partial wake locks where full wake locks aren't needed
  - Verify wake lock timeout values are appropriate
  - Test wake lock behavior during alarm ring and dismissal
  - _Requirements: 14.1, 14.3, 14.4_

- [ ] 10.3.4 Test on low-end devices
  - Test on devices with API 26-28 (Android 8.0-9.0)
  - Verify smooth UI performance on low-end hardware
  - Check memory usage under resource constraints
  - Test alarm reliability on devices with aggressive battery optimization
  - Verify app doesn't crash on low memory conditions
  - _Requirements: All requirements_

- [ ]* 10.4 Accessibility improvements
  - Add content descriptions for all interactive elements
  - Test with TalkBack screen reader
  - Ensure minimum touch target sizes (48dp)
  - Verify color contrast ratios
  - Test keyboard navigation
  - _Requirements: All requirements_

- [ ]* 10.5 Create app icon and branding
  - Design app icon following Material Design guidelines
  - Create adaptive icon for Android 8+
  - Add app name and branding to launcher
  - _Requirements: All requirements_

- [x] 10.6 Add comprehensive logging and debugging
  - Add structured logging throughout the app with appropriate log levels
  - Create debug screen to view current alarm state and scheduled times
  - Add logging for alarm scheduling, ringing, and dismissal events
  - Log permission status changes
  - Add crash reporting mechanism (optional: Firebase Crashlytics)
  - Create log export functionality for debugging
  - _Requirements: All requirements_

- [x] 10.7 Add data validation and integrity checks
  - Validate alarm data on app startup
  - Check for orphaned alarm states and clean them up
  - Implement data cleanup routines for old statistics (keep last 5 cycles)
  - Add database integrity checks
  - Handle migration failures gracefully
  - Validate alarm state consistency after device reboot
  - _Requirements: 12.4, 16.1, 16.2, 16.3_

- [ ]* 10.8 Add user onboarding flow
  - Create first-time user tutorial explaining app features
  - Add permission explanation screens before requesting permissions
  - Show example alarm setup walkthrough
  - Add tips for battery optimization settings
  - Create help/FAQ section in settings
  - Add tooltips for complex features (pause, stop for day)
  - _Requirements: 13.5, 13.6_
