#!/bin/bash

# Debug Test Alarm Script
# This script helps debug the 5-second test alarm functionality

echo "=== Letting In Test Alarm Debug ==="
echo ""
echo "Clearing logcat..."
adb logcat -c

echo "Monitoring logs for test alarm..."
echo "Press Ctrl+C to stop"
echo ""
echo "Looking for:"
echo "  - AlarmEditorViewModel test alarm logs"
echo "  - AlarmReceiver alarm ring logs"
echo "  - AlarmScheduler scheduling logs"
echo "  - Permission issues"
echo ""

# Monitor relevant logs
adb logcat | grep -E "AlarmEditorViewModel|AlarmReceiver|AlarmSchedulerImpl|ALARM|SCHEDULING" --line-buffered
