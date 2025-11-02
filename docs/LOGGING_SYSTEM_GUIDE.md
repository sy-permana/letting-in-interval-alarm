# Logging System Guide

## Overview

The Letting In app now includes a comprehensive logging system that provides structured logging with different log levels and categories. This system helps with debugging, monitoring, and troubleshooting.

## Features

### 1. Centralized Logging (`AppLogger`)

All logging goes through the `AppLogger` singleton class, which provides:

- **Structured logging** with categories and log levels
- **In-memory log buffer** (last 500 entries)
- **File export** capability
- **Log filtering** by category and level
- **Automatic logcat integration**

### 2. Log Levels

- **VERBOSE**: Detailed information for debugging
- **DEBUG**: General debugging information
- **INFO**: Informational messages about normal operation
- **WARNING**: Warning messages about potential issues
- **ERROR**: Error messages about failures

### 3. Log Categories

- **ALARM**: Alarm ring, dismiss, pause, resume events
- **SCHEDULING**: Alarm scheduling and calculation events
- **NOTIFICATION**: Notification display and management
- **PERMISSION**: Permission requests and status changes
- **DATABASE**: Database operations
- **UI**: UI interactions and navigation
- **SYSTEM**: System events (boot, time changes, etc.)
- **ERROR**: General errors and exceptions

## Usage

### Basic Logging

```kotlin
@Inject
lateinit var appLogger: AppLogger

// Log an info message
appLogger.i(AppLogger.CATEGORY_ALARM, "MyClass", "Alarm activated")

// Log an error with exception
appLogger.e(AppLogger.CATEGORY_ERROR, "MyClass", "Failed to save alarm", exception)

// Log a warning
appLogger.w(AppLogger.CATEGORY_SCHEDULING, "MyClass", "No valid next ring time")
```

### Convenience Methods

```kotlin
// Log alarm scheduled
appLogger.logAlarmScheduled(alarmId, nextRingTime)

// Log alarm ring
appLogger.logAlarmRing(alarmId, label)

// Log alarm dismissed
appLogger.logAlarmDismissed(alarmId, isUserDismissal = true)

// Log alarm paused
appLogger.logAlarmPaused(alarmId, pauseDurationMinutes)

// Log alarm resumed
appLogger.logAlarmResumed(alarmId)

// Log permission change
appLogger.logPermissionChange("NOTIFICATION", granted = true)

// Log database operation
appLogger.logDatabaseOperation("INSERT", "alarms", success = true)

// Log system event
appLogger.logSystemEvent("BOOT_COMPLETED", "Restoring active alarm")
```

## Debug Screen

The app includes an enhanced Debug Screen accessible from Settings that shows:

### System Information
- Current time
- Device uptime
- App version
- Android version

### Active Alarm Details
- Alarm ID and label
- Start/end times
- Interval and selected days
- Notification type

### Alarm State
- Last ring time
- Next scheduled ring time
- Pause status
- Today's statistics

### Permissions Status
- All critical permissions
- Grant/deny status

### Log Statistics
- Total logs
- Error count
- Warning count
- Alarm events count
- Scheduling events count

### Recent Logs
- Last 20 log entries
- Color-coded by level
- Shows timestamp, category, and message
- Displays exceptions if present

### Debug Actions
- **Refresh Debug Info**: Update all debug information
- **Export Logs to File**: Save logs to external storage
- **Clear Logs**: Clear the in-memory log buffer
- **Simulate Boot Receiver**: Test boot receiver functionality

## Viewing Logs

### 1. Via Debug Screen

1. Open the app
2. Go to Settings
3. Tap "Debug Information"
4. View recent logs and statistics
5. Use "Export Logs to File" to save logs

### 2. Via Logcat (Terminal)

```bash
# View all app logs
adb logcat | grep "com.lettingin.intervalAlarm"

# View logs by category
adb logcat | grep "\[ALARM\]"
adb logcat | grep "\[SCHEDULING\]"
adb logcat | grep "\[ERROR\]"

# View logs from specific component
adb logcat -s "AlarmSchedulerImpl:*"
adb logcat -s "AlarmReceiver:*"
adb logcat -s "AlarmService:*"

# View all structured logs
adb logcat | grep -E "\[ALARM\]|\[SCHEDULING\]|\[NOTIFICATION\]|\[PERMISSION\]|\[DATABASE\]|\[UI\]|\[SYSTEM\]|\[ERROR\]"
```

### 3. Via Exported Log File

```bash
# Export logs from app (via Debug Screen)
# Then pull the file
adb pull /sdcard/Android/data/com.lettingin.intervalAlarm/files/letting_in_logs.txt

# View the file
cat letting_in_logs.txt
```

## Log File Format

Exported logs are formatted as:

```
=== Letting In Application Logs ===
Generated: 2025-11-02T21:45:30.123
Total entries: 150

2025-11-02 21:45:30.123 | INFO    | SCHEDULING   | AlarmSchedulerImpl   | Scheduling alarm: id=1, label='Morning Reminder'
2025-11-02 21:45:30.456 | INFO    | SCHEDULING   | AlarmSchedulerImpl   | Alarm scheduled: id=1, nextRing=2025-11-03T08:00:00
2025-11-02 21:50:00.789 | INFO    | ALARM        | AlarmReceiver        | Alarm ring triggered: id=1
2025-11-02 21:50:00.890 | INFO    | ALARM        | AlarmReceiver        | Alarm ringing: id=1, label='Morning Reminder'
2025-11-02 21:50:15.123 | INFO    | ALARM        | AlarmService         | Alarm dismissed: id=1, type=user
```

## Integration Points

The logging system is integrated into:

### 1. AlarmSchedulerImpl
- Alarm scheduling events
- Pause/resume operations
- Next ring time calculations
- Errors and warnings

### 2. AlarmReceiver
- Alarm ring triggers
- Resume triggers
- Error handling

### 3. AlarmNotificationService
- Alarm dismissals (user and auto)
- Stop for day actions
- Service lifecycle

### 4. DebugViewModel
- Debug screen initialization
- Log export operations
- Boot receiver simulation

### 5. Future Integration Points
- BootReceiver (system events)
- Permission changes
- Database operations
- UI navigation events

## Best Practices

### 1. Choose Appropriate Log Levels

- Use **VERBOSE** for very detailed debugging (rarely needed)
- Use **DEBUG** for general debugging information
- Use **INFO** for normal operational events
- Use **WARNING** for recoverable issues
- Use **ERROR** for failures and exceptions

### 2. Use Descriptive Messages

```kotlin
// Good
appLogger.i(CATEGORY_ALARM, TAG, "Alarm scheduled: id=$alarmId, nextRing=$nextRingTime")

// Bad
appLogger.i(CATEGORY_ALARM, TAG, "Alarm scheduled")
```

### 3. Include Context

Always include relevant IDs, timestamps, and state information:

```kotlin
appLogger.e(CATEGORY_ERROR, TAG, 
    "Failed to schedule alarm: id=$alarmId, reason=${e.message}", e)
```

### 4. Use Categories Consistently

Stick to the predefined categories for consistency:
- ALARM, SCHEDULING, NOTIFICATION, PERMISSION, DATABASE, UI, SYSTEM, ERROR

### 5. Log Important State Changes

Log all significant state changes:
- Alarm activation/deactivation
- Pause/resume
- Permission grants/denials
- System events (boot, time changes)

## Troubleshooting

### No Logs Appearing

1. Check if AppLogger is injected properly
2. Verify Hilt dependency injection is working
3. Check logcat filters
4. Ensure app is running

### Log Buffer Full

The buffer keeps the last 500 entries. If you need more:
1. Export logs to file regularly
2. Increase `maxBufferSize` in AppLogger
3. Use logcat for real-time monitoring

### Export Fails

1. Check storage permissions
2. Verify external storage is available
3. Check logcat for error messages

## Testing Logging

### 1. Test Log Creation

```bash
# Clear logcat
adb logcat -c

# Trigger an alarm event (create/activate alarm)
# Then check logs
adb logcat -d | grep "\[ALARM\]"
```

### 2. Test Log Export

1. Open Debug Screen
2. Tap "Export Logs to File"
3. Pull the file:
```bash
adb pull /sdcard/Android/data/com.lettingin.intervalAlarm/files/letting_in_logs.txt
cat letting_in_logs.txt
```

### 3. Test Log Filtering

```kotlin
// In DebugViewModel or test code
val errorLogs = appLogger.getLogsByLevel(AppLogger.LogLevel.ERROR)
val alarmLogs = appLogger.getLogsByCategory(AppLogger.CATEGORY_ALARM)
val recentLogs = appLogger.getRecentLogs(50)
```

## Performance Considerations

- **In-memory buffer**: Limited to 500 entries (configurable)
- **Async operations**: Log export runs on IO dispatcher
- **Minimal overhead**: Logging is lightweight and non-blocking
- **Production**: Consider reducing log verbosity in release builds

## Future Enhancements

Potential improvements:

1. **Log rotation**: Automatically rotate log files
2. **Remote logging**: Send logs to analytics service
3. **Log levels per category**: Different verbosity per category
4. **Crash reporting**: Integration with Firebase Crashlytics
5. **Performance metrics**: Track timing and performance
6. **User feedback**: Allow users to submit logs with bug reports

## Summary

The logging system provides comprehensive debugging and monitoring capabilities:

✅ Structured logging with categories and levels
✅ In-memory buffer for recent logs
✅ File export for detailed analysis
✅ Debug screen for easy access
✅ Integration with key components
✅ Logcat compatibility

This system makes it easy to troubleshoot issues, monitor app behavior, and understand what's happening in production.

---

**Related Documentation**:
- Debug Screen Guide
- Troubleshooting Guide
- Development Best Practices
