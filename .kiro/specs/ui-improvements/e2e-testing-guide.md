# End-to-End Testing Guide - UI Improvements

**Date:** November 3, 2025  
**Task:** Task 10 - End-to-end testing  
**Spec:** ui-improvements

## Overview

This document provides comprehensive end-to-end testing scenarios for all UI improvements implemented in the "Letting In" interval alarm application. Each test scenario includes prerequisites, steps, and expected results.

## Test Environment Setup

### Prerequisites
- Android device or emulator running Android 8.0 (API 26) or higher
- App installed with latest UI improvements
- All required permissions granted:
  - Notification permission
  - Exact alarm permission
  - Battery optimization disabled (for reliable testing)

### Test Data Setup
1. Start with a clean app state (or known state)
2. Create at least 3 test alarms with different configurations
3. Ensure one alarm is active for testing activation switching

## Test Scenarios

### 1. 24-Hour Time Format Display

#### Test 1.1: Time Display in Alarm List
**Objective:** Verify all times display in 24-hour format (HH:mm)

**Steps:**
1. Open the app and navigate to home screen
2. Observe alarm list items

**Expected Results:**
- ✅ All start times display in HH:mm format (e.g., "09:00", "14:30")
- ✅ All end times display in HH:mm format
- ✅ Time ranges show as "HH:mm - HH:mm" (e.g., "09:00 - 17:00")
- ✅ No AM/PM indicators visible
- ✅ Midnight displays as "00:00"
- ✅ Noon displays as "12:00"
- ✅ Afternoon times display as 13:00-23:59

**Requirements Verified:** 1.1, 1.2, 1.3, 1.4, 1.5, 2.1, 2.2, 2.3

---

#### Test 1.2: Time Display in Active Alarm Card
**Objective:** Verify next ring time displays in 24-hour format

**Steps:**
1. Ensure an alarm is active
2. Observe the active alarm card on home screen
3. Check the "Next Ring" time display

**Expected Results:**
- ✅ Next ring time displays in HH:mm format
- ✅ No AM/PM indicator
- ✅ Time updates correctly as next ring approaches

**Requirements Verified:** 4.1

---

#### Test 1.3: Time Picker in Alarm Editor
**Objective:** Verify time picker uses 24-hour format

**Steps:**
1. Tap "+" button to create new alarm
2. Tap on start time field
3. Observe time picker dialog
4. Select a time (e.g., 15:30)
5. Confirm selection
6. Repeat for end time

**Expected Results:**
- ✅ Time picker shows hours 00-23
- ✅ No AM/PM toggle visible
- ✅ Selected time displays as HH:mm in editor
- ✅ Can select midnight (00:00) and noon (12:00) correctly

**Requirements Verified:** 3.1, 3.2, 3.3, 3.4

---

#### Test 1.4: Time Display in Alarm Ringing Screen
**Objective:** Verify current time displays in 24-hour format when alarm rings

**Steps:**
1. Create a test alarm that rings in 5 seconds (use test alarm feature)
2. Wait for alarm to ring
3. Observe alarm ringing screen

**Expected Results:**
- ✅ Current time displays in HH:mm format
- ✅ No AM/PM indicator

**Requirements Verified:** 4.2

---

#### Test 1.5: Time Display in Statistics Screen
**Objective:** Verify statistics times display in 24-hour format

**Steps:**
1. Navigate to an alarm with statistics
2. Tap three-dot menu → "View Statistics"
3. Observe time displays in statistics

**Expected Results:**
- ✅ All cycle times display in HH:mm format
- ✅ Ring times display in HH:mm format
- ✅ No AM/PM indicators

**Requirements Verified:** 4.3

---

### 2. Interval Selector with Slider and Quick Options

#### Test 2.1: Slider Functionality
**Objective:** Verify slider allows interval selection with 5-minute steps

**Steps:**
1. Create new alarm or edit existing alarm
2. Navigate to interval selector section
3. Drag slider left and right
4. Observe interval value changes

**Expected Results:**
- ✅ Slider moves smoothly
- ✅ Interval value updates in real-time
- ✅ Values snap to 5-minute increments (5, 10, 15, 20, etc.)
- ✅ Current interval displays prominently above slider
- ✅ Min label shows "5m"
- ✅ Max label shows calculated maximum (e.g., "240m" for 4-hour range)

**Requirements Verified:** 5.1, 5.2, 5.3, 5.4, 8.1, 8.2

---

#### Test 2.2: Quick Option Buttons
**Objective:** Verify quick preset buttons work correctly

**Steps:**
1. In alarm editor, observe quick option buttons
2. Tap "15m" button
3. Observe slider and display update
4. Tap "30m" button
5. Tap "45m" button
6. Tap "60m" button

**Expected Results:**
- ✅ Four quick option buttons visible: 15m, 30m, 45m, 60m
- ✅ Tapping button sets interval to that value
- ✅ Slider position updates to match selected value
- ✅ Selected button is visually highlighted
- ✅ Display shows formatted interval (e.g., "30 minutes", "1 hour")

**Requirements Verified:** 6.1, 6.2, 6.3, 6.4, 6.5, 6.6, 6.7

---

#### Test 2.3: Quick Options Filtering
**Objective:** Verify quick options filter based on max interval

**Steps:**
1. Create alarm with short time range (e.g., 7:00 AM - 7:30 AM)
2. Observe quick option buttons

**Expected Results:**
- ✅ Only valid options shown (e.g., only 15m if max is 25 minutes)
- ✅ Options exceeding max interval are hidden
- ✅ At least one option always available

**Requirements Verified:** 6.1, 7.1, 7.2

---

#### Test 2.4: Interval Display Formatting
**Objective:** Verify interval displays in readable format

**Steps:**
1. Set interval to 30 minutes
2. Set interval to 60 minutes
3. Set interval to 90 minutes
4. Set interval to 120 minutes

**Expected Results:**
- ✅ 30 minutes → "30 minutes"
- ✅ 60 minutes → "1 hour"
- ✅ 90 minutes → "1 hour 30 minutes"
- ✅ 120 minutes → "2 hours"

**Requirements Verified:** 7.3, 7.4

---

#### Test 2.5: Maximum Interval Calculation
**Objective:** Verify max interval is capped at 12 hours or time range

**Steps:**
1. Create alarm with 4-hour range (7 AM - 11 AM)
2. Observe max interval
3. Create alarm with 15-hour range (7 AM - 10 PM)
4. Observe max interval

**Expected Results:**
- ✅ 4-hour range: max interval = 240 minutes (4 hours)
- ✅ 15-hour range: max interval = 720 minutes (12 hours, capped)
- ✅ Slider cannot exceed calculated maximum
- ✅ Quick options filter based on maximum

**Requirements Verified:** 8.3, 8.4, 8.5

---

#### Test 2.6: Interval Adjustment on Time Change
**Objective:** Verify interval adjusts when time range changes

**Steps:**
1. Create alarm with 8-hour range and 240-minute interval
2. Change end time to create 3-hour range
3. Observe interval value

**Expected Results:**
- ✅ Interval automatically reduces to fit new max (180 minutes)
- ✅ Slider updates to new position
- ✅ Display shows adjusted interval

**Requirements Verified:** 3.4, 8.3

---

### 3. Toggle-Based Alarm Activation

#### Test 3.1: Toggle Switch Display
**Objective:** Verify toggle switch displays correctly for active and inactive alarms

**Steps:**
1. Navigate to home screen
2. Observe active alarm card
3. Observe inactive alarm cards

**Expected Results:**
- ✅ Active alarm has toggle in ON position (right side, colored)
- ✅ Inactive alarms have toggle in OFF position (left side, gray)
- ✅ Toggle is prominently positioned to the right of alarm info
- ✅ Toggle has sufficient touch target size (easy to tap)
- ✅ No X button or delete icon visible for activation/deactivation

**Requirements Verified:** 9.1, 9.2, 9.3, 11.1, 11.2, 11.3, 11.4

---

#### Test 3.2: Activate Alarm Without Confirmation
**Objective:** Verify alarm activates directly when no other alarm is active

**Steps:**
1. Ensure no alarm is currently active
2. Tap toggle switch on an inactive alarm to ON position
3. Observe result

**Expected Results:**
- ✅ Alarm activates immediately
- ✅ No confirmation dialog shown
- ✅ Toggle moves to ON position
- ✅ Alarm card moves to top of list
- ✅ Card becomes elevated with primary container color
- ✅ "ACTIVE" label appears

**Requirements Verified:** 9.4, 10.6

---

#### Test 3.3: Activate Alarm With Confirmation
**Objective:** Verify confirmation dialog appears when switching active alarms

**Steps:**
1. Ensure one alarm is active (e.g., "Morning Routine")
2. Tap toggle switch on different inactive alarm (e.g., "Medication Reminder") to ON position
3. Observe confirmation dialog

**Expected Results:**
- ✅ Confirmation dialog appears
- ✅ Dialog shows swap icon (⇄)
- ✅ Dialog title: "Switch Active Alarm?"
- ✅ Dialog message includes current active alarm label: "The alarm 'Morning Routine' is currently active."
- ✅ Dialog message includes new alarm label: "Activating 'Medication Reminder' will deactivate the current alarm."
- ✅ Two buttons visible: "Cancel" and "Switch"

**Requirements Verified:** 10.1, 10.2, 10.3

---

#### Test 3.4: Confirm Activation Switch
**Objective:** Verify alarm switches when user confirms

**Steps:**
1. Trigger confirmation dialog (as in Test 3.3)
2. Tap "Switch" button
3. Observe result

**Expected Results:**
- ✅ Dialog closes
- ✅ Previous active alarm deactivates (toggle moves to OFF)
- ✅ New alarm activates (toggle moves to ON)
- ✅ Alarm list reorders (new active alarm moves to top)
- ✅ New active alarm card becomes elevated
- ✅ Previous active alarm card returns to normal elevation

**Requirements Verified:** 10.4, 10.7

---

#### Test 3.5: Cancel Activation Switch
**Objective:** Verify toggle reverts when user cancels

**Steps:**
1. Trigger confirmation dialog (as in Test 3.3)
2. Tap "Cancel" button
3. Observe result

**Expected Results:**
- ✅ Dialog closes
- ✅ Toggle reverts to OFF position
- ✅ Current active alarm remains active
- ✅ No changes to alarm list order
- ✅ No alarms are activated or deactivated

**Requirements Verified:** 10.5

---

#### Test 3.6: Deactivate Active Alarm
**Objective:** Verify alarm deactivates when toggle switched to OFF

**Steps:**
1. Ensure an alarm is active
2. Tap toggle switch to OFF position
3. Observe result

**Expected Results:**
- ✅ Alarm deactivates immediately
- ✅ No confirmation dialog shown
- ✅ Toggle moves to OFF position
- ✅ Card loses elevation and primary container color
- ✅ "ACTIVE" label disappears
- ✅ Alarm moves down in list (inactive alarms section)

**Requirements Verified:** 9.5, 9.6

---

#### Test 3.7: Toggle Animation
**Objective:** Verify toggle provides smooth visual feedback

**Steps:**
1. Toggle an alarm ON and OFF multiple times
2. Observe animation

**Expected Results:**
- ✅ Smooth animation when switching states
- ✅ Color transition is smooth
- ✅ No lag or stuttering
- ✅ Visual feedback is immediate

**Requirements Verified:** 11.5

---

### 4. Three-Dot Menu for Alarm Actions

#### Test 4.1: Menu Display
**Objective:** Verify three-dot menu displays correctly

**Steps:**
1. Navigate to home screen
2. Observe alarm list items
3. Tap three-dot icon on any alarm

**Expected Results:**
- ✅ Three-dot icon (⋮) visible to the right of toggle switch
- ✅ Icon has sufficient touch target size
- ✅ Tapping icon opens dropdown menu
- ✅ Menu displays three options with icons:
  - 📊 View Statistics
  - ✏️ Edit
  - 🗑️ Delete

**Requirements Verified:** 12.1, 12.2, 12.3

---

#### Test 4.2: View Statistics Action
**Objective:** Verify "View Statistics" navigates to statistics screen

**Steps:**
1. Open three-dot menu on alarm with statistics
2. Tap "View Statistics"

**Expected Results:**
- ✅ Menu closes
- ✅ App navigates to statistics screen for that alarm
- ✅ Statistics display correctly

**Requirements Verified:** 12.4

---

#### Test 4.3: Edit Inactive Alarm
**Objective:** Verify "Edit" navigates to editor for inactive alarm

**Steps:**
1. Open three-dot menu on inactive alarm
2. Tap "Edit"

**Expected Results:**
- ✅ Menu closes
- ✅ App navigates to alarm editor screen
- ✅ Alarm details pre-populated in editor
- ✅ Can modify and save alarm

**Requirements Verified:** 12.5

---

#### Test 4.4: Edit Active Alarm (Restricted)
**Objective:** Verify "Edit" shows message for active alarm

**Steps:**
1. Open three-dot menu on active alarm
2. Tap "Edit"

**Expected Results:**
- ✅ Menu closes
- ✅ Snackbar message appears: "Deactivate the alarm before editing"
- ✅ Does NOT navigate to editor
- ✅ Alarm remains active

**Requirements Verified:** 12.6

---

#### Test 4.5: Delete Inactive Alarm
**Objective:** Verify "Delete" shows confirmation for inactive alarm

**Steps:**
1. Open three-dot menu on inactive alarm
2. Tap "Delete"
3. Observe confirmation dialog
4. Tap "Delete" to confirm

**Expected Results:**
- ✅ Menu closes
- ✅ Confirmation dialog appears
- ✅ Dialog asks for confirmation
- ✅ Tapping "Delete" removes alarm from list
- ✅ Alarm is permanently deleted

**Requirements Verified:** 12.7

---

#### Test 4.6: Delete Active Alarm (Restricted)
**Objective:** Verify "Delete" shows message for active alarm

**Steps:**
1. Open three-dot menu on active alarm
2. Tap "Delete"

**Expected Results:**
- ✅ Menu closes
- ✅ Snackbar message appears: "Deactivate the alarm before deleting"
- ✅ No confirmation dialog shown
- ✅ Alarm remains in list and active

**Requirements Verified:** 12.8

---

### 5. Swipe-to-Delete Gesture

#### Test 5.1: Swipe Inactive Alarm
**Objective:** Verify swipe left reveals delete button for inactive alarm

**Steps:**
1. Navigate to home screen
2. Swipe left on an inactive alarm card
3. Observe revealed delete button
4. Tap delete button
5. Confirm deletion

**Expected Results:**
- ✅ Swiping left reveals red background with delete icon
- ✅ Delete button is clearly visible
- ✅ Tapping delete shows confirmation dialog
- ✅ Confirming deletes the alarm
- ✅ Alarm removed from list

**Requirements Verified:** 13.1, 13.2, 13.3

---

#### Test 5.2: Swipe Active Alarm (Disabled)
**Objective:** Verify swipe is disabled for active alarm

**Steps:**
1. Navigate to home screen
2. Attempt to swipe left on active alarm card

**Expected Results:**
- ✅ Swipe gesture does not work
- ✅ Card does not move
- ✅ No delete button revealed
- ✅ Visual feedback indicates swipe is disabled

**Requirements Verified:** 13.4, 13.5

---

#### Test 5.3: Dismiss Swipe
**Objective:** Verify swipe can be dismissed without deleting

**Steps:**
1. Swipe left on inactive alarm to reveal delete
2. Swipe right to dismiss OR tap elsewhere

**Expected Results:**
- ✅ Delete button hides
- ✅ Card returns to normal position
- ✅ Alarm remains in list

**Requirements Verified:** 13.6

---

### 6. Edge Cases and Error Scenarios

#### Test 6.1: Midnight Time Selection
**Objective:** Verify midnight (00:00) handles correctly

**Steps:**
1. Create alarm with start time 00:00
2. Create alarm with end time 00:00 (next day)
3. Observe time displays

**Expected Results:**
- ✅ Midnight displays as "00:00"
- ✅ Time range calculations work correctly
- ✅ Interval validation works for cross-midnight ranges

**Requirements Verified:** 1.2, 3.1, 14.4

---

#### Test 6.2: Noon Time Selection
**Objective:** Verify noon (12:00) handles correctly

**Steps:**
1. Create alarm with start time 12:00
2. Create alarm with end time 12:00
3. Observe time displays

**Expected Results:**
- ✅ Noon displays as "12:00"
- ✅ No confusion with midnight
- ✅ Time calculations work correctly

**Requirements Verified:** 1.2, 3.1, 14.4

---

#### Test 6.3: Cross-Day Time Range
**Objective:** Verify time ranges spanning midnight work correctly

**Steps:**
1. Create alarm with start time 22:00 and end time 02:00
2. Observe max interval calculation
3. Set interval and save

**Expected Results:**
- ✅ Time range displays as "22:00 - 02:00"
- ✅ Max interval calculated correctly (4 hours = 240 minutes)
- ✅ Interval validation works
- ✅ Alarm saves and schedules correctly

**Requirements Verified:** 2.3, 8.3, 14.4

---

#### Test 6.4: Maximum Alarm Limit
**Objective:** Verify behavior when 10 alarms exist

**Steps:**
1. Create 10 alarms
2. Observe FAB (floating action button)
3. Attempt to create 11th alarm

**Expected Results:**
- ✅ FAB disappears when 10 alarms exist
- ✅ Cannot create more than 10 alarms
- ✅ Appropriate message shown if attempted

**Requirements Verified:** 14.1

---

#### Test 6.5: Rapid Toggle Switching
**Objective:** Verify app handles rapid toggle switches gracefully

**Steps:**
1. Rapidly toggle an alarm ON and OFF multiple times
2. Observe behavior

**Expected Results:**
- ✅ App responds to each toggle
- ✅ No crashes or freezes
- ✅ Final state matches last toggle action
- ✅ No orphaned alarms or states

**Requirements Verified:** 9.4, 9.5, 14.2

---

#### Test 6.6: Confirmation Dialog Dismissal
**Objective:** Verify confirmation dialog can be dismissed by tapping outside

**Steps:**
1. Trigger activation confirmation dialog
2. Tap outside dialog area (on dimmed background)

**Expected Results:**
- ✅ Dialog closes
- ✅ Toggle reverts to OFF
- ✅ No alarm activation occurs
- ✅ Same behavior as tapping "Cancel"

**Requirements Verified:** 10.5

---

### 7. Integration Tests

#### Test 7.1: Complete Alarm Creation Flow
**Objective:** Verify complete flow from creation to activation

**Steps:**
1. Tap "+" button
2. Enter label: "Test Alarm"
3. Select start time: 09:00
4. Select end time: 17:00
5. Use slider to set interval: 30 minutes
6. Select days: Mon, Tue, Wed, Thu, Fri
7. Save alarm
8. Toggle alarm ON
9. Observe active alarm

**Expected Results:**
- ✅ All times display in 24-hour format
- ✅ Interval selector works smoothly
- ✅ Alarm saves successfully
- ✅ Toggle activates alarm
- ✅ Alarm appears at top of list as active
- ✅ Next ring time displays correctly in 24-hour format

**Requirements Verified:** All requirements (integration)

---

#### Test 7.2: Complete Alarm Editing Flow
**Objective:** Verify complete flow for editing inactive alarm

**Steps:**
1. Select inactive alarm
2. Open three-dot menu
3. Tap "Edit"
4. Change start time to 10:00
5. Change interval using quick option: 45m
6. Save alarm
7. Verify changes

**Expected Results:**
- ✅ Editor opens with current values
- ✅ Time picker shows 24-hour format
- ✅ Quick option button works
- ✅ Changes save successfully
- ✅ Alarm list shows updated values in 24-hour format

**Requirements Verified:** 3.1-3.4, 6.1-6.7, 12.5

---

#### Test 7.3: Complete Alarm Deletion Flow
**Objective:** Verify complete flow for deleting alarm

**Steps:**
1. Select inactive alarm
2. Swipe left to reveal delete
3. Tap delete button
4. Confirm deletion
5. Verify alarm removed

**Expected Results:**
- ✅ Swipe reveals delete button
- ✅ Confirmation dialog appears
- ✅ Alarm deleted after confirmation
- ✅ Alarm removed from list
- ✅ No errors or crashes

**Requirements Verified:** 13.1-13.3, 12.7

---

#### Test 7.4: Alarm Switch Flow
**Objective:** Verify complete flow for switching active alarms

**Steps:**
1. Activate "Alarm A"
2. Toggle "Alarm B" ON
3. Confirm switch in dialog
4. Verify "Alarm A" deactivated
5. Verify "Alarm B" activated
6. Check alarm list order

**Expected Results:**
- ✅ Confirmation dialog shows both alarm labels
- ✅ "Alarm A" deactivates after confirmation
- ✅ "Alarm B" activates after confirmation
- ✅ "Alarm B" moves to top of list
- ✅ Only one alarm active at a time

**Requirements Verified:** 10.1-10.7, 14.2

---

## Test Results Summary

### Test Execution Checklist

| Test ID | Test Name | Status | Notes |
|---------|-----------|--------|-------|
| 1.1 | Time Display in Alarm List | ⬜ | |
| 1.2 | Time Display in Active Alarm Card | ⬜ | |
| 1.3 | Time Picker in Alarm Editor | ⬜ | |
| 1.4 | Time Display in Alarm Ringing Screen | ⬜ | |
| 1.5 | Time Display in Statistics Screen | ⬜ | |
| 2.1 | Slider Functionality | ⬜ | |
| 2.2 | Quick Option Buttons | ⬜ | |
| 2.3 | Quick Options Filtering | ⬜ | |
| 2.4 | Interval Display Formatting | ⬜ | |
| 2.5 | Maximum Interval Calculation | ⬜ | |
| 2.6 | Interval Adjustment on Time Change | ⬜ | |
| 3.1 | Toggle Switch Display | ⬜ | |
| 3.2 | Activate Alarm Without Confirmation | ⬜ | |
| 3.3 | Activate Alarm With Confirmation | ⬜ | |
| 3.4 | Confirm Activation Switch | ⬜ | |
| 3.5 | Cancel Activation Switch | ⬜ | |
| 3.6 | Deactivate Active Alarm | ⬜ | |
| 3.7 | Toggle Animation | ⬜ | |
| 4.1 | Menu Display | ⬜ | |
| 4.2 | View Statistics Action | ⬜ | |
| 4.3 | Edit Inactive Alarm | ⬜ | |
| 4.4 | Edit Active Alarm (Restricted) | ⬜ | |
| 4.5 | Delete Inactive Alarm | ⬜ | |
| 4.6 | Delete Active Alarm (Restricted) | ⬜ | |
| 5.1 | Swipe Inactive Alarm | ⬜ | |
| 5.2 | Swipe Active Alarm (Disabled) | ⬜ | |
| 5.3 | Dismiss Swipe | ⬜ | |
| 6.1 | Midnight Time Selection | ⬜ | |
| 6.2 | Noon Time Selection | ⬜ | |
| 6.3 | Cross-Day Time Range | ⬜ | |
| 6.4 | Maximum Alarm Limit | ⬜ | |
| 6.5 | Rapid Toggle Switching | ⬜ | |
| 6.6 | Confirmation Dialog Dismissal | ⬜ | |
| 7.1 | Complete Alarm Creation Flow | ⬜ | |
| 7.2 | Complete Alarm Editing Flow | ⬜ | |
| 7.3 | Complete Alarm Deletion Flow | ⬜ | |
| 7.4 | Alarm Switch Flow | ⬜ | |

**Legend:**
- ⬜ Not Started
- 🔄 In Progress
- ✅ Passed
- ❌ Failed
- ⚠️ Passed with Issues

## Known Issues and Limitations

*To be filled during testing*

## Recommendations

1. **Manual Testing Required:** These tests require manual execution on a physical device or emulator
2. **Test on Multiple Devices:** Test on different Android versions and screen sizes
3. **Test in Different Locales:** Verify 24-hour format works across different system locales
4. **Performance Testing:** Monitor app performance during rapid interactions
5. **Accessibility Testing:** Verify all new UI elements are accessible with TalkBack

## Conclusion

This comprehensive end-to-end testing guide covers all UI improvements implemented in the spec. Each test scenario is designed to verify specific requirements and ensure the features work correctly in real-world usage.

**Next Steps:**
1. Execute all test scenarios manually
2. Document results in the checklist
3. Report any issues found
4. Verify fixes for any failed tests
5. Sign off on task completion

**Task 10 Status: READY FOR EXECUTION**
