# MIUI Autostart Permission Fix

## Problem

Your device is running MIUI (Xiaomi's Android skin), which has aggressive autostart restrictions. The system is blocking your app from starting on boot:

```
Unable to launch app com.lettingin.intervalAlarm for broadcast Intent { act=android.intent.action.BOOT_COMPLETED }: process is not permitted to auto start
```

This prevents the BootReceiver from running, which means alarms won't be restored after device reboot.

## Solution

You need to manually enable **Autostart** permission for the app.

### Method 1: Through Settings (Recommended)

1. Open **Settings** on your device
2. Go to **Apps** → **Manage apps**
3. Find and tap **Letting In** (your app)
4. Look for **Autostart** permission
5. **Toggle it ON** (enable it)
6. Reboot your device to test

### Method 2: Through Security App

1. Open **Security** app (MIUI Security)
2. Tap **Permissions** or **App permissions**
3. Find and tap **Autostart**
4. Find **Letting In** in the list
5. **Toggle it ON** (enable it)
6. Reboot your device to test

### Method 3: Through App Info

1. Long-press the **Letting In** app icon
2. Tap **App info**
3. Scroll down to find **Autostart**
4. **Enable it**
5. Reboot your device to test

## Verification

After enabling autostart permission:

1. Reboot your device:
   ```bash
   adb reboot
   ```

2. Wait 2-3 minutes for boot to complete

3. Check logs:
   ```bash
   adb logcat -d | grep "BootReceiver"
   ```

4. You should now see:
   ```
   Device booted, restoring active alarm
   Restoring active alarm: [id]
   Alarm restored and scheduled for [time]
   ```

## Why This Happens

MIUI has strict battery optimization and autostart controls to improve battery life. By default, most apps are not allowed to start automatically on boot. This is a security and battery-saving feature, but it breaks apps that need to run background services or restore alarms after reboot.

## Alternative: Test Without Reboot

If you can't enable autostart permission right now, you can still test other scenarios:

1. **Test alarm functionality** without rebooting
2. **Test pause/resume** features
3. **Test stop for day** functionality
4. **Test Doze mode** (Task 10.2.4)

The BootReceiver code is solid - it just needs the autostart permission to run on MIUI devices.

## For Production App

When releasing the app, you should:

1. **Detect MIUI devices** in the app
2. **Show a warning** if autostart is not enabled
3. **Provide a button** to open autostart settings
4. **Add to onboarding** flow for MIUI users

The `MiuiAutoStartHelper.kt` utility has been created to help with this.

## Other Manufacturers

Similar restrictions exist on:
- **Huawei/Honor**: "Protected apps" setting
- **Oppo/Realme**: "Startup manager"
- **Vivo**: "Background apps"
- **Samsung**: Usually works without extra permissions

Each manufacturer has their own battery optimization system.

## Next Steps

1. **Enable autostart permission** using one of the methods above
2. **Reboot and test** again
3. **Verify logs** show BootReceiver running
4. **Continue with other test scenarios** from Task 10.2.3

---

**Note**: This is a known limitation of MIUI and other Chinese Android skins. It's not a bug in your app - the BootReceiver implementation is correct and will work once the permission is granted.
