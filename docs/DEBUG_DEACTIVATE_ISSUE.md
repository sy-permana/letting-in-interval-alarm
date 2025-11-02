# Debug Guide: Deactivate Creates New Alarm Issue

## Quick Start

Run this script and follow the steps:

```bash
./scripts/watch_alarm_list.sh
```

## What to Look For

### Step 1: Before Clicking X

Note the current state:
- How many alarms are shown in the app?
- What are their IDs?
- Which one is active?

Example log output:
```
HomeViewModel: getAllAlarms: Received 2 alarms from repository
HomeViewModel:   [0] id=1, label='Morning Reminder', isActive=true
HomeViewModel:   [1] id=2, label='Afternoon Break', isActive=false
HomeViewModel: getAllAlarms: After filtering, 2 alarms (removed 0 test alarms)
```

### Step 2: Click the X Button

Watch for these log messages:

**Expected (correct behavior):**
```
HomeViewModel: deactivateAlarm: Starting deactivation for alarmId=1
AlarmRepositoryImpl: deactivateAlarm: Setting alarm 1 to inactive
AlarmRepositoryImpl: deactivateAlarm: Successfully set alarm 1 to inactive
HomeViewModel: deactivateAlarm: Repository deactivation complete
HomeViewModel: cleanupTestAlarms: Removing test alarm 1730000000000  (if any test alarms exist)
HomeViewModel: getAllAlarms: Received 2 alarms from repository
HomeViewModel:   [0] id=1, label='Morning Reminder', isActive=false  <-- Changed to false
HomeViewModel:   [1] id=2, label='Afternoon Break', isActive=false
```

**Unexpected (bug - new alarm appears):**
```
HomeViewModel: deactivateAlarm: Starting deactivation for alarmId=1
AlarmRepositoryImpl: insertAlarm: Inserting alarm - id=3, label='???', isActive=false  <-- NEW ALARM!
HomeViewModel: getAllAlarms: Received 3 alarms from repository  <-- Count increased!
HomeViewModel:   [0] id=1, label='Morning Reminder', isActive=false
HomeViewModel:   [1] id=2, label='Afternoon Break', isActive=false
HomeViewModel:   [2] id=3, label='???', isActive=false  <-- NEW ALARM
```

### Step 3: Identify the Issue

If you see a new alarm being inserted, look for:

1. **What triggered the insert?**
   - Look at the stack trace in the logs
   - The stack trace will show which code called `insertAlarm`

2. **What are the alarm details?**
   - ID: Is it a timestamp (test alarm) or sequential (real alarm)?
   - Label: Does it have `[TEST]` prefix?
   - isActive: Is it active or inactive?

3. **When did it happen?**
   - During deactivation?
   - After deactivation?
   - From a different component?

## Key Log Patterns to Share

Please capture and share these specific log lines:

### 1. Alarm List Before Deactivation
```
HomeViewModel: getAllAlarms: Received X alarms from repository
HomeViewModel:   [0] id=?, label='?', isActive=?
HomeViewModel:   [1] id=?, label='?', isActive=?
```

### 2. Deactivation Process
```
HomeViewModel: deactivateAlarm: Starting deactivation for alarmId=?
AlarmRepositoryImpl: deactivateAlarm: Setting alarm ? to inactive
```

### 3. Any Insert Operations
```
AlarmRepositoryImpl: insertAlarm: Inserting alarm - id=?, label='?', isActive=?
AlarmRepositoryImpl: insertAlarm: Stack trace:
    at com.lettingin.intervalAlarm.data.repository.AlarmRepositoryImpl.insertAlarm(...)
    at ...  <-- This shows WHO called insertAlarm
```

### 4. Alarm List After Deactivation
```
HomeViewModel: getAllAlarms: Received X alarms from repository
HomeViewModel:   [0] id=?, label='?', isActive=?
```

## Alternative: Full Debug Script

For more detailed output with colors:

```bash
./scripts/debug_deactivate_issue.sh
```

This shows:
- Yellow: Deactivation operations
- Red: Insert operations (potential issue)
- Cyan: Test alarm operations
- Green: Alarm list updates

## What to Report

Please share:

1. **The complete log sequence** from clicking X until the new alarm appears
2. **The alarm count** before and after
3. **The new alarm's details** (ID, label, isActive)
4. **The stack trace** from any `insertAlarm` calls

## Common Scenarios

### Scenario A: Test Alarm Not Cleaned Up

**Symptoms:**
- New alarm has timestamp ID (e.g., 1730000000000)
- New alarm has `[TEST]` prefix
- Happens within 10 seconds of testing

**Cause:** Test alarm cleanup hasn't run yet

**Expected:** Should be filtered from UI, but you might see it in logs

### Scenario B: Duplicate Alarm Created

**Symptoms:**
- New alarm has sequential ID (e.g., 3, 4, 5)
- New alarm has same label as existing alarm
- New alarm is inactive

**Cause:** Something is calling `insertAlarm` during deactivation

**Need:** Stack trace to identify the caller

### Scenario C: Active Alarm Not Deactivated

**Symptoms:**
- Alarm count stays the same
- Active alarm still shows as active
- No new alarm appears

**Cause:** Deactivation failed or wasn't called

**Need:** Deactivation logs to see if it was called

## Quick Test

To quickly test if the issue is related to test alarms:

1. Create a new alarm (don't test it)
2. Activate it
3. Wait 5 minutes (no testing)
4. Click X to deactivate
5. Does a new alarm appear?

If NO: Issue is related to test alarms
If YES: Issue is in deactivation logic itself

## Database Query

You can also check the database directly:

```bash
# Pull the database
adb exec-out run-as com.lettingin.intervalAlarm cat databases/letting_in_database > /tmp/letting_in.db

# Query all alarms
sqlite3 /tmp/letting_in.db "SELECT id, label, isActive, createdAt FROM interval_alarms ORDER BY id;"
```

This shows the actual database state vs what the UI shows.
