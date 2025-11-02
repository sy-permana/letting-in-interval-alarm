# Requirements Document

## Introduction

"Letting In" is an Android interval alarm application built with Kotlin that allows users to create recurring alarms that ring at specified intervals throughout a defined time window. The application supports up to 10 saved alarms with only one active at a time, customizable notification types, day-of-week scheduling, and cycle management. The app is designed to be lightweight and efficient for background operation while providing comprehensive alarm management and statistics tracking.

## Glossary

- **Letting_In_App**: The Android interval alarm application system
- **Interval_Alarm**: An alarm configuration that rings repeatedly at specified time intervals between a start time and end time
- **Active_Alarm**: The single alarm that is currently running and will trigger notifications
- **Inactive_Alarm**: A saved alarm configuration that is not currently running
- **Alarm_Cycle**: One complete execution of an alarm through all selected days (for one-cycle mode) or one day's execution (for repeatable mode)
- **Ring_Event**: A single occurrence when the alarm triggers and notifies the user
- **Dismiss_Action**: User interaction to acknowledge and close the current ring event
- **Auto_Dismiss**: System action that closes a ring event after 15 seconds without user interaction
- **Stop_For_Day**: User action that prevents further rings until the next scheduled day
- **Pause_Duration**: A temporary suspension of the alarm for a specified time period
- **Notification_Type**: The method of alerting the user (full-screen, notification popup, or sound-only)
- **Cycle_Statistics**: Historical data tracking rings, dismissals, and auto-dismissals for the last 5 cycles
- **Background_Job**: The system service that manages alarm scheduling and triggering while the app is not in foreground

## Requirements

### Requirement 1: Alarm List Management

**User Story:** As a user, I want to create and manage multiple alarm configurations so that I can save different interval alarm setups for various purposes.

#### Acceptance Criteria

1. THE Letting_In_App SHALL allow users to create up to 10 Interval_Alarms
2. THE Letting_In_App SHALL display all Interval_Alarms in a list with the Active_Alarm positioned first
3. THE Letting_In_App SHALL display each Inactive_Alarm with its label, start time, end time, interval, and active days
4. THE Letting_In_App SHALL visually distinguish the Active_Alarm from Inactive_Alarms in the list display
5. WHEN a user attempts to create an Interval_Alarm and 10 alarms already exist, THE Letting_In_App SHALL prevent creation and display an error message

### Requirement 2: Alarm Creation and Configuration

**User Story:** As a user, I want to configure alarm settings including timing, intervals, and scheduling so that the alarm rings according to my specific needs.

#### Acceptance Criteria

1. THE Letting_In_App SHALL require users to specify start time, end time, interval duration, selected days, and cycle type when creating an Interval_Alarm
2. THE Letting_In_App SHALL allow users to optionally specify a label up to 60 characters for an Interval_Alarm
3. THE Letting_In_App SHALL set a default interval of 30 minutes when creating a new Interval_Alarm
4. THE Letting_In_App SHALL allow users to select interval durations from 5 minutes to the difference between start time and end time in 1-minute increments
5. THE Letting_In_App SHALL allow users to select one or more days of the week for the Interval_Alarm to be active
6. THE Letting_In_App SHALL allow users to specify whether the Interval_Alarm operates in one-cycle mode or repeatable mode
7. THE Letting_In_App SHALL allow users to select a Notification_Type (full-screen, notification popup, or sound-only) for each Interval_Alarm
8. THE Letting_In_App SHALL allow users to select a ringtone from 3-5 built-in options for each Interval_Alarm

### Requirement 3: Single Active Alarm Enforcement

**User Story:** As a user, I want only one alarm to be active at a time so that the app remains lightweight and alarms don't conflict.

#### Acceptance Criteria

1. THE Letting_In_App SHALL allow only one Interval_Alarm to be Active_Alarm at any time
2. WHEN a user activates an Inactive_Alarm, THE Letting_In_App SHALL deactivate the current Active_Alarm if one exists
3. THE Letting_In_App SHALL start the Background_Job when an Interval_Alarm becomes Active_Alarm
4. THE Letting_In_App SHALL stop the Background_Job when the Active_Alarm is deactivated

### Requirement 4: Alarm Editing and Deletion

**User Story:** As a user, I want to edit or delete alarm configurations so that I can update my alarm settings or remove alarms I no longer need.

#### Acceptance Criteria

1. WHEN a user attempts to edit the Active_Alarm, THE Letting_In_App SHALL require the user to deactivate it first
2. THE Letting_In_App SHALL allow users to edit any Inactive_Alarm without restrictions
3. THE Letting_In_App SHALL allow users to delete any Inactive_Alarm
4. WHEN a user attempts to delete the Active_Alarm, THE Letting_In_App SHALL require the user to deactivate it first

### Requirement 5: Interval Timing and Ring Scheduling

**User Story:** As a user, I want the alarm to ring at precise intervals starting from the configured start time so that I receive consistent reminders throughout the day.

#### Acceptance Criteria

1. WHEN the current time matches the start time on a selected day, THE Letting_In_App SHALL trigger the first Ring_Event
2. WHEN a Ring_Event occurs, THE Letting_In_App SHALL schedule the next Ring_Event at exactly the interval duration from the current Ring_Event time
3. THE Letting_In_App SHALL calculate the next Ring_Event time based on the scheduled ring time, not the Dismiss_Action time
4. WHEN the next scheduled Ring_Event time plus the interval duration exceeds the end time, THE Letting_In_App SHALL not schedule further Ring_Events for that day
5. WHEN the end time is reached or passed, THE Letting_In_App SHALL trigger the final Ring_Event if scheduled and stop further rings for that day

### Requirement 6: Day-of-Week Scheduling

**User Story:** As a user, I want to specify which days of the week the alarm should be active so that it only rings on my selected days.

#### Acceptance Criteria

1. THE Letting_In_App SHALL trigger Ring_Events only on days selected in the Interval_Alarm configuration
2. THE Letting_In_App SHALL not trigger Ring_Events on days not selected in the Interval_Alarm configuration
3. WHEN the Active_Alarm is in repeatable mode, THE Letting_In_App SHALL continue the weekly schedule indefinitely until deactivated
4. WHEN the Active_Alarm is in one-cycle mode, THE Letting_In_App SHALL execute the schedule through all selected days once and then deactivate the alarm

### Requirement 7: Notification Types and Display

**User Story:** As a user, I want to choose how the alarm notifies me so that it fits my usage context and preferences.

#### Acceptance Criteria

1. WHEN a Ring_Event occurs and the Notification_Type is full-screen and the phone is locked, THE Letting_In_App SHALL wake the screen and display a full-screen alarm interface
2. WHEN a Ring_Event occurs and the Notification_Type is full-screen and the phone is unlocked, THE Letting_In_App SHALL display a popup notification requiring user interaction
3. WHEN a Ring_Event occurs and the Notification_Type is notification popup, THE Letting_In_App SHALL display a heads-up notification that can be dismissed from the notification shade
4. WHEN a Ring_Event occurs and the Notification_Type is sound-only, THE Letting_In_App SHALL play a 3-second beep sound without displaying any visual notification
5. WHEN a Ring_Event occurs with full-screen or notification popup type, THE Letting_In_App SHALL play the configured ringtone
6. THE Letting_In_App SHALL use the phone's alarm volume level for all Ring_Event sounds

### Requirement 8: Alarm Dismissal

**User Story:** As a user, I want to quickly dismiss alarm notifications so that I can acknowledge the reminder and continue with my activities.

#### Acceptance Criteria

1. WHEN a Ring_Event displays a full-screen or notification popup interface, THE Letting_In_App SHALL provide a one-tap dismiss button
2. WHEN a user taps the dismiss button, THE Letting_In_App SHALL close the alarm interface and increment the dismiss counter
3. WHEN a Ring_Event is not dismissed within 15 seconds, THE Letting_In_App SHALL automatically close the alarm interface and increment the auto-dismiss counter
4. WHEN a Ring_Event is dismissed or auto-dismissed, THE Letting_In_App SHALL maintain the scheduled next Ring_Event time without modification

### Requirement 9: Stop for the Day

**User Story:** As a user, I want to stop the alarm for the rest of the day so that I can prevent further rings without fully deactivating the alarm.

#### Acceptance Criteria

1. WHEN a Ring_Event displays an alarm interface, THE Letting_In_App SHALL provide a "stop for the day" option with slide or double-tap confirmation
2. WHEN a user confirms stop for the day, THE Letting_In_App SHALL cancel all remaining Ring_Events for the current day
3. WHEN a user confirms stop for the day, THE Letting_In_App SHALL display a confirmation message showing when the alarm will ring next
4. WHEN the next scheduled day arrives, THE Letting_In_App SHALL resume Ring_Events at the configured start time

### Requirement 10: Pause and Resume

**User Story:** As a user, I want to temporarily pause the alarm for a specific duration so that I can suspend rings without stopping for the entire day.

#### Acceptance Criteria

1. THE Letting_In_App SHALL provide a pause button on the home page for the Active_Alarm
2. WHEN a user taps the pause button, THE Letting_In_App SHALL present pause duration options: 1x interval, 30 minutes, 1 hour, or custom time
3. WHEN a user selects a pause duration, THE Letting_In_App SHALL suspend Ring_Events for the specified duration
4. WHEN the pause duration expires, THE Letting_In_App SHALL automatically resume the Active_Alarm and schedule the next Ring_Event
5. WHEN the current time plus interval duration plus pause duration exceeds the end time, THE Letting_In_App SHALL prevent the pause action and display an error message
6. WHEN a paused alarm reaches the end time for the day, THE Letting_In_App SHALL automatically resume on the next scheduled day at the start time
7. THE Letting_In_App SHALL reset pause status at the end of each day

### Requirement 11: Home Page Display

**User Story:** As a user, I want to see the status and statistics of my active alarm on the home page so that I can monitor its operation at a glance.

#### Acceptance Criteria

1. WHEN no alarm is active, THE Letting_In_App SHALL display a "Create Interval Alarm" button on the home page
2. WHEN an Active_Alarm exists, THE Letting_In_App SHALL display the next ring time on the home page
3. WHEN an Active_Alarm exists, THE Letting_In_App SHALL display the total rings for the current day on the home page
4. WHEN an Active_Alarm exists, THE Letting_In_App SHALL display the time until the end time on the home page
5. WHEN an Active_Alarm exists, THE Letting_In_App SHALL display the total user dismiss count for the current day on the home page
6. WHEN an Active_Alarm exists, THE Letting_In_App SHALL display the total auto-dismiss count for the current day on the home page
7. THE Letting_In_App SHALL reset daily statistics at midnight

### Requirement 12: Statistics Tracking

**User Story:** As a user, I want to view historical statistics for my alarms so that I can track my usage patterns over time.

#### Acceptance Criteria

1. THE Letting_In_App SHALL store Cycle_Statistics for each Interval_Alarm for the last 5 cycles
2. THE Letting_In_App SHALL record the total Ring_Events, user dismissals, and auto-dismissals for each cycle
3. THE Letting_In_App SHALL provide access to view Cycle_Statistics for each Interval_Alarm
4. WHEN Cycle_Statistics exceed 5 cycles, THE Letting_In_App SHALL remove the oldest cycle data

### Requirement 13: Settings Page

**User Story:** As a user, I want to configure global app settings so that I can customize the default behavior and appearance of the app.

#### Acceptance Criteria

1. THE Letting_In_App SHALL provide a settings page accessible from the home page
2. THE Letting_In_App SHALL allow users to set a default interval duration that applies to new Interval_Alarms
3. THE Letting_In_App SHALL allow users to set a default Notification_Type that applies to new Interval_Alarms
4. THE Letting_In_App SHALL allow users to select an app theme (light or dark mode)
5. THE Letting_In_App SHALL display notification permission status and provide guidance for enabling permissions
6. THE Letting_In_App SHALL display battery optimization status and provide guidance for disabling battery optimization

### Requirement 14: Background Operation

**User Story:** As a user, I want the alarm to work reliably in the background so that it rings even when the app is not open or the phone is restarted.

#### Acceptance Criteria

1. THE Letting_In_App SHALL maintain the Background_Job while the Active_Alarm is running
2. WHEN the phone is restarted and an Active_Alarm exists within its active time window, THE Letting_In_App SHALL resume the Background_Job and schedule the next Ring_Event
3. THE Letting_In_App SHALL trigger Ring_Events even when the app is not in the foreground
4. THE Letting_In_App SHALL trigger Ring_Events even when the app has been force-closed by the user

### Requirement 15: Time Change Handling

**User Story:** As a user, I want the alarm to handle time changes gracefully so that it continues to function correctly if I change my phone's time or timezone.

#### Acceptance Criteria

1. WHEN the phone's time or timezone changes and the Active_Alarm has already triggered at least one Ring_Event, THE Letting_In_App SHALL continue the interval cycle based on elapsed time
2. WHEN continuing after a time change, THE Letting_In_App SHALL verify that the next scheduled Ring_Event does not exceed the end time
3. WHEN the next scheduled Ring_Event would exceed the end time due to time changes, THE Letting_In_App SHALL stop Ring_Events for the current day

### Requirement 16: Data Persistence

**User Story:** As a user, I want my alarm configurations and statistics to be saved locally so that they persist across app restarts and device reboots.

#### Acceptance Criteria

1. THE Letting_In_App SHALL store all Interval_Alarm configurations in local storage
2. THE Letting_In_App SHALL store all Cycle_Statistics in local storage
3. THE Letting_In_App SHALL restore all Interval_Alarm configurations and Cycle_Statistics when the app is launched
4. THE Letting_In_App SHALL preserve data if the app is uninstalled and reinstalled, where supported by the device

### Requirement 17: Alarm Label Display

**User Story:** As a user, I want to see the alarm label in notifications so that I can quickly identify which alarm is ringing.

#### Acceptance Criteria

1. WHEN a Ring_Event occurs and the Interval_Alarm has a label, THE Letting_In_App SHALL display the label in the alarm interface
2. WHEN a Ring_Event occurs and the Interval_Alarm has no label, THE Letting_In_App SHALL display default text in the alarm interface
