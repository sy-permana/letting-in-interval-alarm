#!/bin/bash

# Simple script to watch alarm list changes
# This will show you exactly when alarms are added/removed/changed

echo "=== Watching Alarm List Changes ==="
echo ""
echo "This will show:"
echo "  - Current alarm count"
echo "  - Each alarm's ID, label, and active status"
echo "  - When alarms are inserted/updated/deleted"
echo ""
echo "Instructions:"
echo "  1. Start this script"
echo "  2. Note the alarm IDs and count"
echo "  3. Click X to deactivate"
echo "  4. Watch for any NEW alarms appearing"
echo ""
echo "Press Ctrl+C to stop"
echo ""

adb logcat -c
adb logcat | grep -E "HomeViewModel.*getAllAlarms|AlarmRepositoryImpl.*(insert|update|delete)Alarm" --line-buffered
