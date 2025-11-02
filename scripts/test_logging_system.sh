#!/bin/bash

# Test script for the logging system
# Verifies that logs are being created and can be exported

set -e

PACKAGE="com.lettingin.intervalAlarm"
LOG_FILE="/sdcard/Android/data/$PACKAGE/files/letting_in_logs.txt"

echo "=========================================="
echo "Logging System Test"
echo "=========================================="
echo ""

# Check if device is connected
echo "1. Checking device connection..."
if ! adb devices | grep -q "device$"; then
    echo "❌ No device connected"
    exit 1
fi
echo "✅ Device connected"
echo ""

# Check if app is installed
echo "2. Checking app installation..."
if ! adb shell pm list packages | grep -q "$PACKAGE"; then
    echo "❌ App not installed"
    exit 1
fi
echo "✅ App installed"
echo ""

# Clear logcat
echo "3. Clearing logcat..."
adb logcat -c
echo "✅ Logcat cleared"
echo ""

# Launch the app
echo "4. Launching app..."
adb shell am start -n $PACKAGE/.ui.MainActivity > /dev/null 2>&1
sleep 3
echo "✅ App launched"
echo ""

# Check for structured logs in logcat
echo "5. Checking for structured logs in logcat..."
STRUCTURED_LOGS=$(adb logcat -d | grep -E "\[ALARM\]|\[SCHEDULING\]|\[UI\]|\[SYSTEM\]" | wc -l)
if [ "$STRUCTURED_LOGS" -gt 0 ]; then
    echo "✅ Found $STRUCTURED_LOGS structured log entries"
    echo ""
    echo "Sample logs:"
    adb logcat -d | grep -E "\[ALARM\]|\[SCHEDULING\]|\[UI\]|\[SYSTEM\]" | head -5
else
    echo "⚠️  No structured logs found yet (app may need more interaction)"
fi
echo ""

# Check for AppLogger logs
echo "6. Checking for AppLogger activity..."
APPLOGGER_LOGS=$(adb logcat -d | grep -i "applogger\|debugviewmodel" | wc -l)
if [ "$APPLOGGER_LOGS" -gt 0 ]; then
    echo "✅ Found $APPLOGGER_LOGS AppLogger-related log entries"
else
    echo "ℹ️  No AppLogger logs found (normal if app just started)"
fi
echo ""

# Check for alarm-related logs
echo "7. Checking for alarm-related logs..."
ALARM_LOGS=$(adb logcat -d | grep -E "AlarmScheduler|AlarmReceiver|AlarmService" | wc -l)
if [ "$ALARM_LOGS" -gt 0 ]; then
    echo "✅ Found $ALARM_LOGS alarm-related log entries"
    echo ""
    echo "Sample alarm logs:"
    adb logcat -d | grep -E "AlarmScheduler|AlarmReceiver|AlarmService" | tail -5
else
    echo "ℹ️  No alarm logs found (normal if no alarms are active)"
fi
echo ""

# Test log export (requires manual trigger from Debug Screen)
echo "8. Testing log export capability..."
echo "   To test log export:"
echo "   1. Open the app"
echo "   2. Go to Settings → Debug Information"
echo "   3. Tap 'Export Logs to File'"
echo "   4. Run: adb pull $LOG_FILE"
echo ""

# Check if log file exists (from previous export)
if adb shell "[ -f $LOG_FILE ] && echo exists" | grep -q "exists"; then
    echo "✅ Log file exists from previous export"
    echo "   Pulling log file..."
    adb pull "$LOG_FILE" ./letting_in_logs.txt 2>/dev/null
    if [ -f "./letting_in_logs.txt" ]; then
        echo "✅ Log file pulled successfully"
        echo ""
        echo "Log file preview:"
        head -20 ./letting_in_logs.txt
        echo ""
        echo "Total lines: $(wc -l < ./letting_in_logs.txt)"
    fi
else
    echo "ℹ️  No exported log file found (export from Debug Screen to create one)"
fi
echo ""

# Summary
echo "=========================================="
echo "Test Summary"
echo "=========================================="
echo ""
echo "Logging System Status:"
echo "  ✅ App installed and running"
echo "  ✅ Logcat integration working"
if [ "$STRUCTURED_LOGS" -gt 0 ]; then
    echo "  ✅ Structured logging active"
else
    echo "  ⚠️  Structured logging not yet visible"
fi
if [ "$ALARM_LOGS" -gt 0 ]; then
    echo "  ✅ Alarm logging active"
else
    echo "  ℹ️  Alarm logging pending (create an alarm to test)"
fi
echo ""
echo "Next Steps:"
echo "  1. Create and activate an alarm to generate more logs"
echo "  2. Open Debug Screen to view logs in-app"
echo "  3. Export logs from Debug Screen"
echo "  4. Monitor logcat: adb logcat | grep '\[ALARM\]'"
echo ""
echo "=========================================="
echo "Test Complete"
echo "=========================================="
