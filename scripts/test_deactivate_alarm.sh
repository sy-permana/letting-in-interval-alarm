#!/bin/bash

# Test Deactivate Alarm Script
# This script helps test the alarm deactivation functionality

echo "=== Testing Alarm Deactivation ==="
echo ""
echo "Clearing logcat..."
adb logcat -c

echo ""
echo "Monitoring deactivation logs..."
echo "Now click the X button on an active alarm in the app"
echo "Press Ctrl+C to stop"
echo ""

# Monitor relevant logs
adb logcat | grep -E "HomeViewModel.*deactivate|AlarmRepository.*deactivate|TEST|cleanupTestAlarms" --line-buffered
