#!/bin/bash

# Comprehensive Deactivate Issue Debug Script
# This captures all relevant information about the deactivation issue

echo "=== Deactivate Alarm Debug Script ==="
echo ""
echo "This script will monitor:"
echo "  1. Alarm list changes (getAllAlarms)"
echo "  2. Deactivation calls"
echo "  3. Database operations (insert/update/delete)"
echo "  4. Test alarm activity"
echo "  5. Repository operations"
echo ""
echo "Instructions:"
echo "  1. Start this script"
echo "  2. Note the current alarm count"
echo "  3. Click the X button to deactivate"
echo "  4. Watch for new alarms appearing"
echo "  5. Press Ctrl+C when done"
echo ""
echo "Clearing logcat..."
adb logcat -c

echo "Starting monitoring..."
echo "================================================"
echo ""

# Monitor all relevant logs
adb logcat | grep -E \
  "HomeViewModel|AlarmRepository|AlarmDao|getAllAlarms|deactivate|setAlarmInactive|insertAlarm|updateAlarm|deleteAlarm|\[TEST\]|cleanupTestAlarms|interval_alarms" \
  --line-buffered | while read line; do
    # Add timestamp and highlight important lines
    timestamp=$(date '+%H:%M:%S')
    
    if echo "$line" | grep -q "deactivate"; then
        echo -e "\033[1;33m[$timestamp] $line\033[0m"  # Yellow for deactivate
    elif echo "$line" | grep -q "insertAlarm"; then
        echo -e "\033[1;31m[$timestamp] $line\033[0m"  # Red for insert (potential issue)
    elif echo "$line" | grep -q "\[TEST\]"; then
        echo -e "\033[1;36m[$timestamp] $line\033[0m"  # Cyan for test alarms
    elif echo "$line" | grep -q "getAllAlarms"; then
        echo -e "\033[1;32m[$timestamp] $line\033[0m"  # Green for getAllAlarms
    else
        echo "[$timestamp] $line"
    fi
done
