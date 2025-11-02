# Testing Guide - Letting In Interval Alarm

This guide explains how to test the Letting In app, including the built-in testing features.

## Built-In Testing Features

The app includes several testing features to help you verify alarm functionality without waiting for scheduled times.

### 1. Test Alarm (5-Second Test)

**Location**: Alarm Editor Screen → Testing & Preview section

**What it does**:
- Triggers a test alarm that will ring in exactly 5 seconds
- Uses the current alarm configuration (ringtone, notification type, etc.)
- Allows you to test the full alarm experience quickly

**How to use**:
1. Open the Alarm Editor (create new or edit existing alarm)
2. Configure your alarm settings (ringtone, notification type, etc.)
3. Scroll to the "Testing & Preview" section
4. Tap "Test Alarm (5 sec)" button
5. Wait 5 seconds for the alarm to ring
6. Test dismissing, stopping for day, or auto-dismiss (15 seconds)

**What to verify**:
- ✅ Alarm rings after 5 seconds
- ✅ Correct notification type appears (full-screen, popup, or sound-only)
- ✅ Selected ringtone plays
- ✅ Dismiss button works
- ✅ Stop for day button works
- ✅ Auto-dismiss works after 15 seconds

### 2. Preview Ringtone

**Location**: Alarm Editor Screen → Testing & Preview section

**What it does**:
- Plays the selected ringtone for 3 seconds
- Allows you to hear what the alarm will sound like
- Stops automatically after 3 seconds

**How to use**:
1. Open the Alarm Editor
2. Select a ringtone from the dropdown
3. Tap "Preview Ringtone" button
4. Listen to the ringtone for 3 seconds

**What to verify**:
- ✅ Ringtone plays immediately
- ✅ Correct ringtone is played
- ✅ Volume is audible
- ✅ Ringtone stops after 3 seconds

## Manual Testing Checklist

### Alarm Creation and Configuration

- [ ] Create a new alarm
- [ ] Set custom label (max 60 characters)
- [ ] Set start and end time
- [ ] Set interval (minimum 5 minutes)
- [ ] Select active days
- [ ] Choose notification type (full-screen, popup, sound-only)
- [ ] Select ringtone
- [ ] Toggle cycle type (repeatable vs one-cycle)
- [ ] Verify validation errors for invalid inputs
- [ ] Save alarm successfully

### Alarm Activation

- [ ] Activate an alarm
- [ ] Verify only one alarm can be active at a time
- [ ] Check that permissions are required before activation
- [ ] Verify active alarm appears at top of home screen
- [ ] Check active alarm statistics display

### Alarm Ringing

- [ ] Wait for alarm to ring at scheduled time (or use test alarm)
- [ ] Verify correct notification type appears
- [ ] Verify ringtone plays
- [ ] Test user dismiss
- [ ] Test stop for day
- [ ] Test auto-dismiss (wait 15 seconds)
- [ ] Verify statistics update after dismissal

### Pause and Resume

- [ ] Pause active alarm
- [ ] Select pause duration (1x interval, 30 min, 1 hour)
- [ ] Verify alarm doesn't ring during pause
- [ ] Resume alarm manually
- [ ] Verify alarm resumes ringing

### Permissions

- [ ] Test without notification permission (Android 13+)
- [ ] Test without exact alarm permission (Android 12+)
- [ ] Test without battery optimization disabled
- [ ] Verify permission warning banner appears
- [ ] Grant permissions and verify alarm works

### Settings

- [ ] Change default interval
- [ ] Change default notification type
- [ ] Change theme (light/dark/system)
- [ ] Verify settings persist after app restart

### Statistics

- [ ] View statistics for an alarm
- [ ] Verify last 5 cycles are shown
- [ ] Check total rings, user dismissals, auto-dismissals
- [ ] Verify percentages are calculated correctly

### Edge Cases

- [ ] Test with device reboot (alarm should reschedule)
- [ ] Test with time zone change
- [ ] Test with system time change
- [ ] Test in Doze mode
- [ ] Test with Do Not Disturb mode
- [ ] Test with low battery
- [ ] Test on low-end device

### Data Persistence

- [ ] Create alarms and close app
- [ ] Reopen app and verify alarms are saved
- [ ] Activate alarm and close app
- [ ] Verify alarm still rings when app is closed
- [ ] Test backup and restore (reinstall app)

## Testing on Physical Device

### Prerequisites

1. **Android device** running Android 8.0 (API 26) or higher
2. **USB debugging** enabled
3. **ADB** installed on your computer

### Installation

```bash
# Build and install the app
./gradlew installDebug

# Or install APK directly
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### Viewing Logs

```bash
# View all app logs
adb logcat | grep "LettingIn"

# View alarm-specific logs
adb logcat | grep "AlarmReceiver\|AlarmScheduler\|AlarmNotificationService"

# View permission logs
adb logcat | grep "PermissionChecker"
```

### Testing Permissions

```bash
# Grant notification permission (Android 13+)
adb shell pm grant com.lettingin.intervalAlarm android.permission.POST_NOTIFICATIONS

# Check exact alarm permission status
adb shell dumpsys alarm | grep "com.lettingin.intervalAlarm"

# Open battery optimization settings
adb shell am start -a android.settings.IGNORE_BATTERY_OPTIMIZATION_SETTINGS
```

### Testing Boot Receiver

```bash
# Reboot device
adb reboot

# Or simulate boot completed broadcast (requires root)
adb shell am broadcast -a android.intent.action.BOOT_COMPLETED
```

### Testing Backup and Restore

```bash
# Trigger backup
adb shell bmgr backupnow com.lettingin.intervalAlarm

# Clear app data
adb shell pm clear com.lettingin.intervalAlarm

# Restore from backup
adb shell bmgr restore com.lettingin.intervalAlarm

# Verify data restored
adb shell run-as com.lettingin.intervalAlarm ls /data/data/com.lettingin.intervalAlarm/databases/
```

## Common Issues and Solutions

### Alarm Doesn't Ring

**Possible causes**:
1. Missing permissions (notification, exact alarm)
2. Battery optimization enabled
3. App killed by system
4. Do Not Disturb mode enabled

**Solutions**:
- Check permission warning banner on home screen
- Go to Settings → Enable all permissions
- Disable battery optimization
- Add app to Do Not Disturb exceptions

### Test Alarm Doesn't Work

**Possible causes**:
1. Exact alarm permission not granted
2. App doesn't have notification permission

**Solutions**:
- Grant exact alarm permission in Settings
- Grant notification permission
- Check logcat for error messages

### Ringtone Doesn't Play

**Possible causes**:
1. Device volume is muted
2. Ringtone file not found
3. Audio focus issue

**Solutions**:
- Increase device volume
- Try different ringtone
- Check logcat for audio errors

### Statistics Not Updating

**Possible causes**:
1. Database write error
2. Alarm not actually ringing

**Solutions**:
- Check logcat for database errors
- Verify alarm is ringing (use test alarm)
- Clear app data and try again

## Performance Testing

### Memory Usage

```bash
# Check memory usage
adb shell dumpsys meminfo com.lettingin.intervalAlarm

# Monitor memory over time
adb shell top | grep com.lettingin.intervalAlarm
```

### Battery Usage

```bash
# Check battery stats
adb shell dumpsys batterystats com.lettingin.intervalAlarm

# Reset battery stats
adb shell dumpsys batterystats --reset
```

### Wake Locks

```bash
# Check wake locks
adb shell dumpsys power | grep com.lettingin.intervalAlarm
```

## Automated Testing

### Unit Tests

```bash
# Run all unit tests
./gradlew test

# Run specific test class
./gradlew test --tests "HomeViewModelTest"
```

### Instrumented Tests

```bash
# Run all instrumented tests
./gradlew connectedAndroidTest

# Run specific test class
./gradlew connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.lettingin.intervalAlarm.AlarmFlowTest
```

## Reporting Issues

When reporting issues, please include:

1. **Device information**: Model, Android version
2. **App version**: Check in Settings → About
3. **Steps to reproduce**: Detailed steps
4. **Expected behavior**: What should happen
5. **Actual behavior**: What actually happens
6. **Logs**: Relevant logcat output
7. **Screenshots**: If applicable

## Testing Checklist Summary

Before releasing:

- [ ] All manual tests pass
- [ ] Test alarm feature works
- [ ] Ringtone preview works
- [ ] Permissions are properly requested
- [ ] Alarms ring at correct times
- [ ] Statistics are accurate
- [ ] App survives reboot
- [ ] Backup and restore works
- [ ] No memory leaks
- [ ] Battery usage is acceptable
- [ ] Works on low-end devices
- [ ] Works in Doze mode
- [ ] UI is responsive and smooth
