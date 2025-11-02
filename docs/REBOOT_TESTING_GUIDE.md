# Device Reboot Testing Guide

## Overview
This guide provides comprehensive instructions for testing alarm restoration after device reboot scenarios. This is critical for ensuring alarms survive device restarts.

## Prerequisites

### Required Permissions
Ensure these permissions are granted:
- ✅ Notification permission
- ✅ Exact alarm permission (Android 12+)
- ✅ Battery optimization disabled
- ✅ Full-screen intent permission

### ADB Setup
Install Android Debug Bridge (ADB) for testing:
```bash
# Check ADB is installed
adb version

# Connect device
adb devices

# Should show your device
```

## Test Scenarios

### Scenario 1: Reboot During Active Time Window

**Setup:**
1. Create an alarm with:
   - Start time: 30 minutes ago
   - End time: 2 hours from now
   - Interval: 15 minutes
   - Selected days: Include today
   - Mode: Repeatable

2. Activate the alarm
3. Wait for at least one ring to occur
4. Verify alarm state in app (should show next ring time)

**Test:**
```bash
# Reboot device
adb reboot

# Wait for device to restart (2-3 minutes)
```

**Expected Results:**
- ✅ BootReceiver is triggered
- ✅ Active alarm is detected from database
- ✅ Next ring time is calculated based on current time
- ✅ Alarm is rescheduled with AlarmManager
- ✅ Alarm state is updated in database
- ✅ App shows correct next ring time when opened
- ✅ Alarm rings at the scheduled time

**Verification:**
```bash
# Check logs immediately after boot
adb logcat -d | grep "BootReceiver"
adb logcat -d | grep "AlarmScheduler"

# Should see:
# "Device booted, restoring active alarm"
# "Restoring active alarm: [id]"
# "Within active time window, calculating next ring time"
# "Alarm restored and scheduled for [time]"
```

---

### Scenario 2: Reboot Outside Active Time Window

**Setup:**
1. Create an alarm with:
   - Start time: Tomorrow at 9:00 AM
   - End time: Tomorrow at 5:00 PM
   - Interval: 30 minutes
   - Selected days: Include tomorrow
   - Mode: Repeatable

2. Activate the alarm
3. Verify alarm shows "Next ring: Tomorrow at 9:00 AM"

**Test:**
```bash
# Reboot device
adb reboot
```

**Expected Results:**
- ✅ BootReceiver is triggered
- ✅ Active alarm is detected
- ✅ System recognizes we're outside time window
- ✅ Next valid ring time is calculated (tomorrow 9:00 AM)
- ✅ Alarm is scheduled for tomorrow
- ✅ App shows correct next ring time

**Verification:**
```bash
# Check logs
adb logcat -d | grep "BootReceiver"

# Should see:
# "Not within active time window, scheduling for next valid time"
# "Alarm restored and scheduled for [tomorrow's date]"
```

---

### Scenario 3: Reboot When Alarm is Paused

**Setup:**
1. Create and activate an alarm (active time window)
2. Pause the alarm for 1 hour
3. Verify alarm shows "Paused until [time]"

**Test:**
```bash
# Reboot device while paused
adb reboot
```

**Expected Results:**
- ✅ BootReceiver is triggered
- ✅ Pause state is detected
- ✅ If pause time is in the past, pause is cleared
- ✅ If pause time is in the future, pause is maintained
- ✅ Alarm is rescheduled appropriately

**Verification:**
```bash
# Check logs
adb logcat -d | grep "BootReceiver"
adb logcat -d | grep "pause"

# Check alarm state in app
# Should show either "Paused" or "Active" depending on pause time
```

---

### Scenario 4: Reboot with One-Cycle Mode Alarm

**Setup:**
1. Create an alarm with:
   - Selected days: Monday, Wednesday, Friday
   - Mode: One-cycle (not repeatable)
   - Start time: 9:00 AM
   - End time: 5:00 PM
   - Interval: 1 hour

2. Activate on Monday
3. Let it ring a few times
4. Reboot on Monday (same day)

**Test:**
```bash
# Reboot device
adb reboot
```

**Expected Results:**
- ✅ Alarm is restored
- ✅ Continues on Monday
- ✅ After Monday ends, moves to Wednesday
- ✅ After Friday ends, alarm is deactivated (one-cycle complete)

**Verification:**
- Check that alarm continues through selected days
- Verify alarm deactivates after last day

---

### Scenario 5: Reboot with Repeatable Mode Alarm

**Setup:**
1. Create an alarm with:
   - Selected days: All weekdays
   - Mode: Repeatable
   - Start time: 9:00 AM
   - End time: 5:00 PM

2. Activate the alarm
3. Reboot on any weekday

**Test:**
```bash
# Reboot device
adb reboot
```

**Expected Results:**
- ✅ Alarm is restored
- ✅ Continues indefinitely on selected days
- ✅ Never deactivates automatically

---

### Scenario 6: Reboot with No Active Alarm

**Setup:**
1. Ensure no alarms are active
2. May have inactive alarms saved

**Test:**
```bash
# Reboot device
adb reboot
```

**Expected Results:**
- ✅ BootReceiver is triggered
- ✅ No active alarm found
- ✅ No alarms are scheduled
- ✅ No errors occur

**Verification:**
```bash
# Check logs
adb logcat -d | grep "BootReceiver"

# Should see:
# "No active alarm to restore"
```

---

### Scenario 7: Reboot with "Stopped for Day" Alarm

**Setup:**
1. Create and activate an alarm
2. When alarm rings, use "Stop for Day" action
3. Verify alarm shows "Stopped for today, resumes tomorrow"

**Test:**
```bash
# Reboot device
adb reboot
```

**Expected Results:**
- ✅ BootReceiver is triggered
- ✅ "Stopped for day" flag is reset on boot
- ✅ Alarm is rescheduled for next valid time
- ✅ If still today, alarm remains stopped
- ✅ If moved to next day, alarm resumes

---

## Advanced Testing

### Test with ADB Commands

**Simulate Boot Without Rebooting:**
```bash
# Send BOOT_COMPLETED broadcast (requires root)
adb shell am broadcast -a android.intent.action.BOOT_COMPLETED
```

**Check Alarm State:**
```bash
# Dump alarm manager state
adb shell dumpsys alarm | grep "com.lettingin.intervalAlarm"
```

**Check Database State:**
```bash
# Pull database from device
adb pull /data/data/com.lettingin.intervalAlarm/databases/alarm_database.db

# Use SQLite browser to inspect:
# - interval_alarms table (check isActive)
# - alarm_state table (check nextScheduledRingTime, isPaused)
```

---

## Automated Testing Script

Create a test script to automate reboot testing:

```bash
#!/bin/bash
# reboot_test.sh

echo "=== Reboot Testing Script ==="
echo "1. Ensure alarm is active before running"
echo "2. Press Enter to start test"
read

echo "Capturing pre-reboot state..."
adb logcat -c
adb shell dumpsys alarm | grep "com.lettingin.intervalAlarm" > pre_reboot_state.txt

echo "Rebooting device..."
adb reboot

echo "Waiting for device to come back online (60 seconds)..."
sleep 60
adb wait-for-device

echo "Waiting for boot to complete (30 seconds)..."
sleep 30

echo "Capturing post-reboot logs..."
adb logcat -d > post_reboot_logs.txt

echo "Checking for BootReceiver logs..."
grep "BootReceiver" post_reboot_logs.txt

echo "Checking alarm state..."
adb shell dumpsys alarm | grep "com.lettingin.intervalAlarm" > post_reboot_state.txt

echo "=== Test Complete ==="
echo "Check post_reboot_logs.txt for details"
```

---

## Common Issues & Solutions

### Issue 1: BootReceiver Not Triggered
**Symptoms:** No logs from BootReceiver after reboot

**Possible Causes:**
- RECEIVE_BOOT_COMPLETED permission not granted
- BootReceiver not registered in manifest
- App is force-stopped (some devices don't trigger boot receivers for force-stopped apps)

**Solution:**
```bash
# Check if receiver is registered
adb shell dumpsys package com.lettingin.intervalAlarm | grep "BootReceiver"

# Ensure app is not force-stopped
adb shell am start -n com.lettingin.intervalAlarm/.ui.MainActivity
```

### Issue 2: Alarm Not Rescheduled
**Symptoms:** BootReceiver runs but alarm doesn't ring

**Possible Causes:**
- Exact alarm permission not granted (Android 12+)
- Battery optimization enabled
- AlarmManager scheduling failed

**Solution:**
```bash
# Check exact alarm permission
adb shell dumpsys alarm | grep "com.lettingin.intervalAlarm"

# Check battery optimization
adb shell dumpsys deviceidle whitelist | grep "com.lettingin.intervalAlarm"
```

### Issue 3: Database Not Accessible
**Symptoms:** BootReceiver crashes or can't read alarm data

**Possible Causes:**
- Database not initialized
- Hilt dependency injection failed
- Database corruption

**Solution:**
- Check logs for database errors
- Verify Hilt setup is correct
- Test database access in app before reboot

---

## Success Criteria

For Task 10.2.3 to be considered complete, all scenarios must pass:

- ✅ Scenario 1: Reboot during active window
- ✅ Scenario 2: Reboot outside active window
- ✅ Scenario 3: Reboot when paused
- ✅ Scenario 4: One-cycle mode behavior
- ✅ Scenario 5: Repeatable mode behavior
- ✅ Scenario 6: No active alarm
- ✅ Scenario 7: Stopped for day

**Additional Checks:**
- ✅ No crashes or errors in logs
- ✅ Alarm state is correctly restored
- ✅ Next ring time is accurate
- ✅ Alarms ring at expected times
- ✅ UI shows correct alarm status

---

## Logging Best Practices

Add these log statements to help with debugging:

```kotlin
// In BootReceiver
Log.i(TAG, "=== BOOT RECEIVER STARTED ===")
Log.i(TAG, "Active alarm ID: ${activeAlarm.id}")
Log.i(TAG, "Current time: $now")
Log.i(TAG, "Alarm start: ${activeAlarm.startTime}, end: ${activeAlarm.endTime}")
Log.i(TAG, "Selected days: ${activeAlarm.selectedDays}")
Log.i(TAG, "Is repeatable: ${activeAlarm.isRepeatable}")
Log.i(TAG, "Next ring time: $nextRingTime")
Log.i(TAG, "=== BOOT RECEIVER COMPLETED ===")
```

---

## Next Steps

After completing reboot testing:
1. Document any issues found
2. Fix any bugs discovered
3. Update BootReceiver if needed
4. Move to Task 10.2.4 (Doze mode testing)
