# Backup and Restore

This document describes the backup and restore functionality in the Letting In app.

## Overview

The app uses Android's Auto Backup feature to automatically backup and restore user data. This includes:
- All alarm configurations
- Alarm states and statistics
- App settings (theme, default interval, etc.)

## How It Works

### Automatic Backup

Android automatically backs up app data to the user's Google Drive account:
- **Frequency**: Once every 24 hours when the device is idle, charging, and connected to Wi-Fi
- **Data Included**: Room database, shared preferences
- **Data Excluded**: Cache files, temporary files

### Backup Configuration

The backup is configured in `AndroidManifest.xml`:
```xml
<application
    android:allowBackup="true"
    android:dataExtractionRules="@xml/data_extraction_rules"
    android:fullBackupContent="@xml/backup_rules"
    ...>
```

### Backup Rules

**For Android 11 and below** (`backup_rules.xml`):
- Includes the Room database and all related files
- Includes shared preferences
- Excludes cache and external storage

**For Android 12+** (`data_extraction_rules.xml`):
- Supports both cloud backup and device-to-device transfer
- Same inclusion/exclusion rules as older versions

## Restore Process

### Automatic Restore

When a user reinstalls the app or sets up a new device:
1. Android automatically restores the backed-up data
2. The app detects the restore on first launch
3. `BackupHelper.handlePostRestore()` is called
4. Active alarms are rescheduled

### Post-Restore Operations

The `BackupHelper` class handles post-restore operations:
- **Reschedule Active Alarms**: Ensures the active alarm is scheduled with AlarmManager
- **Restore Flag**: Tracks whether post-restore operations have been completed
- **Error Handling**: Logs errors and continues gracefully

## Testing Backup and Restore

### Test Backup

```bash
# Trigger a backup manually
adb shell bmgr backupnow com.lettingin.intervalAlarm

# Check backup status
adb shell dumpsys backup
```

### Test Restore

```bash
# Clear app data
adb shell pm clear com.lettingin.intervalAlarm

# Restore from backup
adb shell bmgr restore com.lettingin.intervalAlarm

# Launch the app to verify restore
adb shell am start -n com.lettingin.intervalAlarm/.ui.MainActivity
```

### Verify Restore

After restore, verify:
1. All alarms are present in the database
2. Active alarm is properly scheduled
3. App settings are restored
4. Statistics data is intact

## Limitations

### What's Backed Up
✅ Alarm configurations (time, interval, days, etc.)
✅ Alarm states (paused, stopped, etc.)
✅ Statistics (last 5 cycles per alarm)
✅ App settings (theme, defaults, etc.)

### What's NOT Backed Up
❌ Notification channels (recreated automatically)
❌ Scheduled alarms in AlarmManager (rescheduled on restore)
❌ Cache files
❌ Temporary files

## User-Facing Behavior

### First Install
- No data to restore
- User creates alarms from scratch

### Reinstall (Same Device)
- Data is automatically restored
- Alarms appear as they were
- Active alarm resumes (if within time window)

### New Device Setup
- Data is restored from Google Drive
- All alarms and settings are transferred
- Active alarm is rescheduled

## Privacy and Security

- Backup data is encrypted by Android
- Stored in user's private Google Drive space
- Only accessible to the same app on the same Google account
- Automatically deleted when app is uninstalled (after 60 days)

## Troubleshooting

### Backup Not Working
1. Check if Auto Backup is enabled in device settings
2. Ensure device is connected to Wi-Fi
3. Verify device is charging and idle
4. Check Google account is properly configured

### Restore Not Working
1. Verify backup exists: `adb shell bmgr list transports`
2. Check restore logs: `adb logcat | grep BackupHelper`
3. Manually trigger restore: `adb shell bmgr restore com.lettingin.intervalAlarm`

### Alarms Not Rescheduled After Restore
1. Check logs for errors in `BackupHelper`
2. Verify permissions are granted (notifications, exact alarms)
3. Manually deactivate and reactivate the alarm

## Development Notes

### Disabling Backup for Testing
To test fresh installs without restore:
```xml
<!-- In AndroidManifest.xml -->
<application
    android:allowBackup="false"
    ...>
```

### Forcing Backup
```bash
# Force immediate backup
adb shell bmgr backupnow com.lettingin.intervalAlarm
```

### Clearing Backup Data
```bash
# Wipe backup data (for testing)
adb shell bmgr wipe com.lettingin.intervalAlarm
```

## Future Enhancements

Potential improvements for backup/restore:
- Manual export/import functionality
- Backup to local storage
- Selective restore (choose which alarms to restore)
- Backup encryption with user password
- Cross-platform backup (export to file)
