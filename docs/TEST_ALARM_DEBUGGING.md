# Test Alarm Debugging Guide

## Overview

The 5-second test alarm feature allows you to quickly test alarm functionality without waiting for scheduled times. This guide helps you debug issues when the test alarm doesn't ring.

## Common Issues and Solutions

### 1. Time Window Validation Issue

**Problem**: The test alarm doesn't ring because the current time is outside the alarm's configured time window.

**Why it happens**: 
- The AlarmReceiver validates that the current time is between `startTime` and `endTime`
- If you're testing at the end of the day (e.g., 11:00 PM) and your alarm's end time is 5:00 PM, the test alarm will be rejected

**Solution** (Fixed in latest version):
- Test alarms now automatically adjust their time window to cover the current time + 5 seconds
- Test start time: current time - 5 minutes
- Test end time: current time + 10 minutes

### 2. Exact Alarm Permission (Android 12+)

**Problem**: On Android 12 and above, the app needs explicit permission to schedule exact alarms.

**Check permission**:
```bash
adb shell dumpsys alarm | grep "com.lettingin.intervalAlarm"
```

**Grant permission manually**:
1. Go to Settings → Apps → Letting In
2. Find "Alarms & reminders" permission
3. Enable it

### 3. Test Alarm Cleanup

**Problem**: Old test alarms accumulate in the database and appear as new alarms.

**Solution** (Fixed in latest version):
- Test alarms are now automatically cleaned up after 10 seconds
- Test alarms are marked with `[TEST]` prefix in the label
- Test alarms are filtered out from the home screen alarm list
- Test alarms are automatically deleted when deactivating any alarm
- Test alarms skip statistics tracking and next ring scheduling

### 4. Battery Optimization

**Problem**: System battery optimization may delay or prevent alarms.

**Check battery optimization**:
```bash
adb shell dumpsys deviceidle whitelist | grep "com.lettingin.intervalAlarm"
```

**Disable battery optimization**:
1. Go to Settings → Apps → Letting In
2. Battery → Unrestricted

## Debugging Steps

### Step 1: Check Logs

Run the debug script:
```bash
./scripts/debug_test_alarm.sh
```

Or manually:
```bash
# Clear logs
adb logcat -c

# Monitor test alarm logs
adb logcat | grep -E "AlarmEditorViewModel|AlarmReceiver|AlarmSchedulerImpl"
```

### Step 2: Look for Key Log Messages

**Successful test alarm flow**:
```
AlarmEditorViewModel: testAlarm: Starting test alarm
AlarmEditorViewModel: testAlarm: Test alarm ID = 1730000000000
AlarmEditorViewModel: testAlarm: Time window = 22:55 to 23:10
AlarmEditorViewModel: testAlarm: Saved to database
AlarmEditorViewModel: testAlarm: Scheduled with setExactAndAllowWhileIdle
AlarmEditorViewModel: testAlarm: Success!
[5 seconds later]
AlarmReceiver: onReceive: action=null
AlarmReceiver: Alarm ring triggered: id=1730000000000
AlarmReceiver: handleAlarmRing: isTestAlarm=true, label=[TEST] Morning Reminder
AlarmReceiver: handleAlarmRing: Time validation passed, proceeding with alarm ring
AlarmReceiver: handleAlarmRing: Starting notification service
```

**Failed test alarm (time window issue)**:
```
AlarmEditorViewModel: testAlarm: Success!
[5 seconds later]
AlarmReceiver: Alarm ring triggered: id=1730000000000
AlarmReceiver: handleAlarmRing: currentTime=23:05, startTime=09:00, endTime=17:00
AlarmReceiver: Current time (23:05) exceeds alarm end time (17:00)
AlarmReceiver: Alarm ring skipped - outside time window
```

**Failed test alarm (permission issue)**:
```
AlarmEditorViewModel: testAlarm: Cannot schedule exact alarms
AlarmEditorViewModel: Exact alarm permission required
```

### Step 3: Check Alarm Manager

```bash
# List all scheduled alarms for the app
adb shell dumpsys alarm | grep -A 20 "com.lettingin.intervalAlarm"
```

Look for:
- Pending alarms with your test alarm ID
- Trigger time (should be ~5 seconds from when you pressed test)

### Step 4: Check Database

```bash
# Pull database
adb exec-out run-as com.lettingin.intervalAlarm cat databases/letting_in_database > /tmp/letting_in.db

# Query test alarms
sqlite3 /tmp/letting_in.db "SELECT id, label, startTime, endTime, isActive FROM interval_alarms WHERE label LIKE '[TEST]%';"
```

### Step 5: Check Permissions

```bash
# Check all app permissions
adb shell dumpsys package com.lettingin.intervalAlarm | grep permission
```

Required permissions:
- `android.permission.SCHEDULE_EXACT_ALARM` (Android 12+)
- `android.permission.USE_EXACT_ALARM`
- `android.permission.POST_NOTIFICATIONS`
- `android.permission.WAKE_LOCK`

## Testing Checklist

Before reporting a bug, verify:

- [ ] App has exact alarm permission (Android 12+)
- [ ] App has notification permission
- [ ] Battery optimization is disabled
- [ ] Device is not in extreme battery saver mode
- [ ] System time is correct (not significantly off)
- [ ] Logs show test alarm was scheduled
- [ ] Logs show AlarmReceiver received the alarm
- [ ] No time window validation errors in logs

## Manual Test

To manually verify the alarm system works:

1. Create a simple alarm with:
   - Start time: 5 minutes ago
   - End time: 10 minutes from now
   - Interval: 30 minutes
   - Today selected

2. Activate the alarm

3. Check logs for scheduling confirmation

4. Wait for the alarm to ring

## Code Changes (Latest Fix)

### AlarmEditorViewModel.kt

**Changes**:
- Test alarms now use dynamic time window (current time ± buffer)
- Test alarms are marked with `[TEST]` prefix
- Test alarms are automatically cleaned up after 30 seconds
- Added more detailed logging

**Key code**:
```kotlin
// Create test alarm with time window that covers current time + 5 seconds
val now = java.time.LocalTime.now()
val testStartTime = now.minusMinutes(5) // Start 5 minutes ago
val testEndTime = now.plusMinutes(10)   // End 10 minutes from now

val testAlarm = alarm.copy(
    id = testAlarmId,
    isActive = true,
    ringtoneUri = validRingtoneUri,
    startTime = testStartTime,
    endTime = testEndTime,
    label = "[TEST] ${alarm.label}"
)
```

### AlarmReceiver.kt

**Changes**:
- Detects test alarms by `[TEST]` prefix
- Skips time window validation for test alarms
- Skips statistics tracking for test alarms
- Skips next ring scheduling for test alarms
- Added detailed logging for time validation

**Key code**:
```kotlin
// Check if this is a test alarm
val isTestAlarm = alarm.label.startsWith("[TEST]")

// Validate we haven't exceeded end time (skip for test alarms)
if (!isTestAlarm && currentTime.isAfter(alarm.endTime)) {
    Log.w(TAG, "Current time ($currentTime) exceeds alarm end time (${alarm.endTime})")
    return
}
```

## Known Limitations

1. **Doze Mode**: On some devices, alarms may be delayed by a few seconds in Doze mode
2. **MIUI/ColorOS**: Some custom Android skins require additional autostart permissions
3. **Test Alarm Cleanup**: Test alarms are cleaned up after 10 seconds, but may persist if app is force-closed (they will be cleaned up on next deactivation)

## Related Documentation

- [Logging System Guide](LOGGING_SYSTEM_GUIDE.md)
- [Testing Guide](TESTING_GUIDE.md)
- [Doze Mode Testing](DOZE_MODE_TESTING_GUIDE.md)
- [Error Handling Guide](ERROR_HANDLING_GUIDE.md)

## Support

If the test alarm still doesn't work after following this guide:

1. Export logs from Debug Screen
2. Check for errors in the logs
3. Verify all permissions are granted
4. Try rebooting the device
5. Try on a different device/emulator

## Quick Fix Summary

The main issue was **time window validation**. The AlarmReceiver was rejecting test alarms when the current time was outside the alarm's configured time window. 

**Fix**: Test alarms now dynamically adjust their time window to always include the current time + 5 seconds, ensuring they can ring regardless of when you test them during the day.
