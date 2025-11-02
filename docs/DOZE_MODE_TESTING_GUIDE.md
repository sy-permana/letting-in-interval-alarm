# Doze Mode Testing Guide

## Overview

Doze mode is Android's aggressive battery-saving feature introduced in Android 6.0 (API 23). It restricts app activity when the device is:
- Stationary
- Screen off
- Not charging
- For an extended period

This guide helps test that alarms fire correctly even in Doze mode.

---

## Prerequisites

### Required:
- ✅ Android device with API 23+ (Android 6.0+)
- ✅ ADB installed and device connected
- ✅ Battery optimization disabled for the app
- ✅ Exact alarm permission granted

### Verify Prerequisites:
```bash
# Check Android version
adb shell getprop ro.build.version.sdk

# Should be 23 or higher
```

---

## Understanding Doze Mode

### Doze Mode Phases:

1. **Active** - Normal operation
2. **Idle** - Screen off, device stationary
3. **Idle Pending** - Preparing to enter Doze
4. **Idle** - Light Doze (network access restricted)
5. **Idle Maintenance** - Brief window for background work
6. **Deep Doze** - Full restrictions (our critical test)

### What's Restricted in Doze:
- ❌ Network access
- ❌ Wake locks ignored
- ❌ WiFi scans disabled
- ❌ Sync adapters don't run
- ❌ JobScheduler jobs deferred
- ✅ **High-priority alarms still fire** (our use case!)

---

## Test Scenarios

### Scenario 1: Force Doze Mode (Quick Test - 5 minutes)

This is the fastest way to test Doze mode without waiting.

**Setup:**
1. Create a test alarm:
   - Start time: Now
   - End time: 2 hours from now
   - Interval: 5 minutes
   - Activate it

2. Wait for the alarm to ring once (verify it works normally)

**Test Steps:**

```bash
# 1. Unplug device from charger (IMPORTANT!)
# Device must be unplugged for Doze to work

# 2. Turn off screen
# Press power button to turn off screen

# 3. Force device into Doze mode immediately
adb shell dumpsys deviceidle force-idle

# 4. Verify device is in Doze mode
adb shell dumpsys deviceidle get

# Should show: "IDLE" or "IDLE_MAINTENANCE"

# 5. Check if app is in standby
adb shell dumpsys battery

# 6. Wait for alarm to ring (should be within 5 minutes)
# The alarm SHOULD ring even in Doze mode

# 7. Check logs after alarm rings
adb logcat -d | grep "AlarmReceiver\|AlarmScheduler"
```

**Expected Results:**
- ✅ Device enters Doze mode
- ✅ Alarm still rings at scheduled time
- ✅ AlarmReceiver is triggered
- ✅ Next alarm is rescheduled

**Exit Doze Mode:**
```bash
# Wake device from Doze
adb shell dumpsys deviceidle unforce

# Or just turn on screen and use device
```

---

### Scenario 2: Natural Doze Mode (Real-world Test - 1-2 hours)

This tests Doze mode as it naturally occurs.

**Setup:**
1. Create a test alarm with 15-minute intervals
2. Activate the alarm
3. Wait for first ring to verify it works

**Test Steps:**

1. **Unplug device** from charger
2. **Turn off screen**
3. **Place device on a flat surface** (don't move it)
4. **Wait 30-60 minutes** for natural Doze to activate
5. **Check if alarm rang** (should ring even in Doze)

**Monitor Doze State:**
```bash
# Check Doze state periodically
adb shell dumpsys deviceidle get

# States you'll see:
# ACTIVE → INACTIVE → IDLE_PENDING → IDLE → IDLE_MAINTENANCE → IDLE
```

**Expected Results:**
- ✅ Device enters Doze naturally
- ✅ Alarms continue to ring
- ✅ No missed alarms

---

### Scenario 3: Battery Saver Mode

Test with battery saver enabled (different from Doze).

**Setup:**
1. Create and activate a test alarm
2. Enable Battery Saver:
   - Settings → Battery → Battery Saver → Turn On
   - Or: `adb shell settings put global low_power 1`

**Test Steps:**

```bash
# Enable battery saver
adb shell settings put global low_power 1

# Verify it's enabled
adb shell settings get global low_power
# Should return: 1

# Wait for alarm to ring

# Disable battery saver
adb shell settings put global low_power 0
```

**Expected Results:**
- ✅ Alarm rings with battery saver enabled
- ✅ No delays or missed alarms

---

### Scenario 4: App Standby Buckets

Android 9+ (API 28+) uses app standby buckets to restrict background activity.

**Check Current Bucket:**
```bash
adb shell am get-standby-bucket com.lettingin.intervalAlarm

# Buckets:
# 5 = ACTIVE (no restrictions)
# 10 = WORKING_SET (mild restrictions)
# 20 = FREQUENT (moderate restrictions)
# 30 = RARE (heavy restrictions)
# 40 = RESTRICTED (maximum restrictions)
# 50 = NEVER (app disabled)
```

**Test in Different Buckets:**

```bash
# Force app to RARE bucket (heavy restrictions)
adb shell am set-standby-bucket com.lettingin.intervalAlarm rare

# Wait for alarm to ring

# Reset to ACTIVE
adb shell am set-standby-bucket com.lettingin.intervalAlarm active
```

**Expected Results:**
- ✅ Alarms ring in all buckets (except NEVER)
- ✅ AlarmManager.setExactAndAllowWhileIdle bypasses restrictions

---

### Scenario 5: Extreme Battery Saver (Android 9+)

Some devices have "Extreme Battery Saver" or "Ultra Power Saving Mode".

**Test Steps:**
1. Enable extreme battery saver in device settings
2. Verify alarm still rings
3. Check if app is whitelisted

**Expected Results:**
- ✅ Alarm rings (may need manual whitelisting)
- ⚠️ May require user to whitelist app

---

## Verification Commands

### Check Doze Whitelist:
```bash
# Check if app is whitelisted (shouldn't need to be)
adb shell dumpsys deviceidle whitelist | grep com.lettingin.intervalAlarm

# Our app should NOT be in whitelist
# AlarmManager.setExactAndAllowWhileIdle works without whitelisting
```

### Check Alarm State:
```bash
# List all scheduled alarms
adb shell dumpsys alarm | grep com.lettingin.intervalAlarm

# Should show scheduled alarm with RTC_WAKEUP type
```

### Monitor Battery Stats:
```bash
# Check battery stats
adb shell dumpsys batterystats com.lettingin.intervalAlarm

# Look for:
# - Wake locks
# - Alarm wakeups
# - Background activity
```

### Check Power Management:
```bash
# Check if battery optimization is disabled
adb shell dumpsys power | grep com.lettingin.intervalAlarm
```

---

## Common Issues & Solutions

### Issue 1: Alarm Doesn't Ring in Doze

**Possible Causes:**
- Using wrong AlarmManager method
- Battery optimization not disabled
- App in RESTRICTED standby bucket

**Solution:**
```kotlin
// Verify we're using the correct method
if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
    alarmManager.setExactAndAllowWhileIdle(
        AlarmManager.RTC_WAKEUP,
        triggerTime,
        pendingIntent
    )
}
```

**Check Implementation:**
```bash
# Search for setExactAndAllowWhileIdle in code
grep -r "setExactAndAllowWhileIdle" app/src/
```

### Issue 2: Delayed Alarms

**Symptoms:** Alarms ring but with delays

**Possible Causes:**
- Using setExact() instead of setExactAndAllowWhileIdle()
- Device manufacturer restrictions (MIUI, EMUI)

**Solution:**
- Verify exact alarm permission granted
- Check manufacturer-specific settings

### Issue 3: Doze Won't Activate

**Possible Causes:**
- Device is charging
- Device is moving
- Screen is on
- Recent user interaction

**Solution:**
```bash
# Force Doze immediately
adb shell dumpsys deviceidle force-idle

# Verify
adb shell dumpsys deviceidle get
```

---

## Testing Checklist

- [ ] **Scenario 1**: Force Doze mode (5 min test)
  - [ ] Device enters Doze
  - [ ] Alarm rings on time
  - [ ] No errors in logs
  
- [ ] **Scenario 2**: Natural Doze (1-2 hour test)
  - [ ] Device enters Doze naturally
  - [ ] Multiple alarms ring correctly
  - [ ] No missed alarms

- [ ] **Scenario 3**: Battery Saver mode
  - [ ] Alarm rings with battery saver on
  - [ ] No delays

- [ ] **Scenario 4**: App Standby buckets
  - [ ] Test in RARE bucket
  - [ ] Alarm still rings

- [ ] **Scenario 5**: Extreme battery saver
  - [ ] Test if available on device
  - [ ] Verify alarm behavior

---

## Success Criteria

Task 10.2.4 is complete when:

✅ Alarms fire correctly in forced Doze mode
✅ Alarms fire correctly in natural Doze mode
✅ Alarms fire with battery saver enabled
✅ Alarms fire in RARE standby bucket
✅ No errors or crashes in logs
✅ AlarmManager.setExactAndAllowWhileIdle is used correctly

---

## Implementation Verification

### Check Our Code:

```bash
# Verify we're using setExactAndAllowWhileIdle
cat app/src/main/java/com/lettingin/intervalAlarm/domain/scheduler/AlarmSchedulerImpl.kt | grep -A 5 "setExactAndAllowWhileIdle"
```

**Expected:**
```kotlin
if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
    alarmManager.setExactAndAllowWhileIdle(
        AlarmManager.RTC_WAKEUP,
        nextRingTime,
        pendingIntent
    )
}
```

✅ **Our implementation already uses the correct method!**

---

## Quick Test Script

Save this as `test_doze.sh`:

```bash
#!/bin/bash

echo "=== Doze Mode Test Script ==="
echo ""
echo "Prerequisites:"
echo "1. Unplug device from charger"
echo "2. Create and activate a test alarm (5-minute interval)"
echo "3. Wait for first alarm to ring"
echo ""
read -p "Press Enter when ready..."

echo ""
echo "Step 1: Turning off screen..."
adb shell input keyevent 26

echo "Step 2: Forcing Doze mode..."
adb shell dumpsys deviceidle force-idle

echo "Step 3: Checking Doze state..."
DOZE_STATE=$(adb shell dumpsys deviceidle get)
echo "Doze state: $DOZE_STATE"

echo ""
echo "Step 4: Waiting for alarm to ring (5 minutes)..."
echo "Watch your device - alarm should ring even in Doze mode"
echo ""
echo "After alarm rings, press Enter to check logs..."
read

echo ""
echo "Step 5: Checking logs..."
adb logcat -d | grep -E "AlarmReceiver|AlarmScheduler" | tail -20

echo ""
echo "Step 6: Exiting Doze mode..."
adb shell dumpsys deviceidle unforce

echo ""
echo "=== Test Complete ==="
echo ""
echo "Did the alarm ring? (yes/no)"
read RESULT

if [ "$RESULT" = "yes" ]; then
    echo "✅ TEST PASSED - Doze mode works correctly!"
else
    echo "❌ TEST FAILED - Alarm did not ring in Doze mode"
fi
```

**Usage:**
```bash
chmod +x test_doze.sh
./test_doze.sh
```

---

## Next Steps

After completing Doze mode testing:
1. Document results in `docs/DOZE_TEST_RESULTS.md`
2. Fix any issues found
3. Move to Task 10.2.5 (Do Not Disturb testing)
4. Or Task 10.2.6 (Error handling improvements)

---

## References

- [Android Doze Documentation](https://developer.android.com/training/monitoring-device-state/doze-standby)
- [AlarmManager Best Practices](https://developer.android.com/training/scheduling/alarms)
- [Testing Doze and App Standby](https://developer.android.com/training/monitoring-device-state/doze-standby#testing_doze)
