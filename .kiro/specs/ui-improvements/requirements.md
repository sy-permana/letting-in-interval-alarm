# Requirements Document

## Introduction

This specification defines UI/UX improvements for the "Letting In" interval alarm application. The improvements focus on enhancing user experience through better time display formatting (24-hour format), more intuitive interval selection controls (sliders with quick options), and clearer alarm activation controls (toggle instead of X button). These changes maintain all existing functionality while improving usability and reducing user confusion.

## Glossary

- **Letting_In_App**: The Android interval alarm application system
- **Time_Display**: Visual representation of time values shown to users in the UI
- **24_Hour_Format**: Time display format showing hours from 00:00 to 23:59 without AM/PM indicators
- **Interval_Selector**: UI component for choosing the alarm interval duration
- **Slider_Control**: UI component allowing users to select values by dragging along a continuous range
- **Quick_Option_Button**: Preset interval value button for rapid selection
- **Activation_Toggle**: UI component for switching alarm between active and inactive states
- **Time_Picker**: UI component for selecting start and end times
- **Alarm_List_Item**: Individual alarm display in the home screen list

## Requirements

### Requirement 1: 24-Hour Time Format Display

**User Story:** As a user, I want to see all times in 24-hour format so that I can quickly understand the time without AM/PM confusion.

#### Acceptance Criteria

1. THE Letting_In_App SHALL display all Time_Display values in 24_Hour_Format throughout the application
2. THE Letting_In_App SHALL format hours as two digits from 00 to 23 in all Time_Display instances
3. THE Letting_In_App SHALL format minutes as two digits from 00 to 59 in all Time_Display instances
4. THE Letting_In_App SHALL use colon separator between hours and minutes (HH:mm format)
5. THE Letting_In_App SHALL NOT display AM or PM indicators in any Time_Display

### Requirement 2: 24-Hour Format in Alarm List

**User Story:** As a user, I want to see alarm start and end times in 24-hour format in the alarm list so that I can quickly scan my alarms.

#### Acceptance Criteria

1. WHEN displaying an Alarm_List_Item, THE Letting_In_App SHALL show the start time in 24_Hour_Format
2. WHEN displaying an Alarm_List_Item, THE Letting_In_App SHALL show the end time in 24_Hour_Format
3. THE Letting_In_App SHALL display the time range as "HH:mm - HH:mm" format in Alarm_List_Item

### Requirement 3: 24-Hour Format in Time Picker

**User Story:** As a user, I want to select times using 24-hour format in the alarm editor so that I can set times without AM/PM selection.

#### Acceptance Criteria

1. WHEN a user opens the Time_Picker for start time, THE Letting_In_App SHALL display hours from 00 to 23
2. WHEN a user opens the Time_Picker for end time, THE Letting_In_App SHALL display hours from 00 to 23
3. THE Letting_In_App SHALL NOT display AM/PM toggle in the Time_Picker
4. THE Letting_In_App SHALL use the system's 24-hour time picker when available

### Requirement 4: 24-Hour Format in Active Alarm Display

**User Story:** As a user, I want to see the next ring time in 24-hour format on the home screen so that I know exactly when the alarm will ring.

#### Acceptance Criteria

1. WHEN displaying the next ring time for the active alarm, THE Letting_In_App SHALL use 24_Hour_Format
2. WHEN displaying the current time in the alarm ringing interface, THE Letting_In_App SHALL use 24_Hour_Format
3. WHEN displaying time-related statistics, THE Letting_In_App SHALL use 24_Hour_Format

### Requirement 5: Slider-Based Interval Selection

**User Story:** As a user, I want to use a slider to select alarm intervals so that I can quickly adjust the interval with visual feedback.

#### Acceptance Criteria

1. THE Letting_In_App SHALL provide a Slider_Control for interval selection in the alarm editor
2. THE Letting_In_App SHALL allow the Slider_Control to select intervals from 5 minutes to the maximum valid interval
3. WHEN a user drags the Slider_Control, THE Letting_In_App SHALL update the displayed interval value in real-time
4. THE Letting_In_App SHALL display the currently selected interval value above or beside the Slider_Control
5. THE Letting_In_App SHALL use appropriate step increments for the Slider_Control based on the interval range

### Requirement 6: Quick Interval Option Buttons

**User Story:** As a user, I want quick preset buttons for common intervals so that I can rapidly select frequently used values without fine-tuning the slider.

#### Acceptance Criteria

1. THE Letting_In_App SHALL provide Quick_Option_Button controls for common interval values
2. THE Letting_In_App SHALL include Quick_Option_Button for 15 minutes interval
3. THE Letting_In_App SHALL include Quick_Option_Button for 30 minutes interval
4. THE Letting_In_App SHALL include Quick_Option_Button for 45 minutes interval
5. THE Letting_In_App SHALL include Quick_Option_Button for 60 minutes interval
6. WHEN a user taps a Quick_Option_Button, THE Letting_In_App SHALL set the interval to that value and update the Slider_Control position
7. THE Letting_In_App SHALL visually highlight the Quick_Option_Button that matches the current interval value

### Requirement 7: Interval Selector Layout

**User Story:** As a user, I want the interval selector to be well-organized so that I can easily understand and use both the slider and quick options.

#### Acceptance Criteria

1. THE Letting_In_App SHALL display Quick_Option_Button controls above or beside the Slider_Control
2. THE Letting_In_App SHALL arrange Quick_Option_Button controls in a horizontal row
3. THE Letting_In_App SHALL display the current interval value with units (e.g., "30 minutes") prominently
4. THE Letting_In_App SHALL provide visual feedback when the slider value changes
5. THE Letting_In_App SHALL maintain the slider and quick options in the same visual section

### Requirement 8: Slider Step Increments

**User Story:** As a user, I want the slider to move in reasonable increments so that I can select precise intervals without excessive fine-tuning.

#### Acceptance Criteria

1. WHEN the maximum interval is less than 60 minutes, THE Letting_In_App SHALL use 5-minute step increments for the Slider_Control
2. WHEN the maximum interval is between 60 and 180 minutes, THE Letting_In_App SHALL use 15-minute step increments for the Slider_Control
3. WHEN the maximum interval is greater than 180 minutes, THE Letting_In_App SHALL use 30-minute step increments for the Slider_Control
4. THE Letting_In_App SHALL snap the Slider_Control to the nearest step increment when the user releases the slider

### Requirement 9: Toggle-Based Alarm Activation

**User Story:** As a user, I want to use a toggle switch to activate or deactivate alarms so that the action is clear and doesn't feel like deletion.

#### Acceptance Criteria

1. THE Letting_In_App SHALL provide an Activation_Toggle control for each Alarm_List_Item
2. THE Letting_In_App SHALL display the Activation_Toggle in the ON position when the alarm is active
3. THE Letting_In_App SHALL display the Activation_Toggle in the OFF position when the alarm is inactive
4. WHEN a user taps the Activation_Toggle to ON position, THE Letting_In_App SHALL activate that alarm
5. WHEN a user taps the Activation_Toggle to OFF position, THE Letting_In_App SHALL deactivate that alarm
6. THE Letting_In_App SHALL NOT display an X button or delete icon for alarm activation/deactivation

### Requirement 10: Toggle Behavior with Single Active Alarm

**User Story:** As a user, I want to be warned before switching active alarms so that I understand the currently active alarm will be deactivated.

#### Acceptance Criteria

1. WHEN a user toggles an inactive alarm to ON and another alarm is currently active, THE Letting_In_App SHALL display a confirmation dialog
2. THE Letting_In_App SHALL include the currently active alarm label or identifier in the confirmation dialog message
3. THE Letting_In_App SHALL provide confirm and cancel options in the confirmation dialog
4. WHEN a user confirms the dialog, THE Letting_In_App SHALL deactivate the currently active alarm and activate the newly selected alarm
5. WHEN a user cancels the dialog, THE Letting_In_App SHALL revert the Activation_Toggle to OFF position and maintain the current active alarm
6. WHEN a user toggles an inactive alarm to ON and no other alarm is active, THE Letting_In_App SHALL activate the alarm immediately without showing a confirmation dialog
7. THE Letting_In_App SHALL update the alarm list order to show the newly active alarm first after successful activation

### Requirement 11: Toggle Visual Design

**User Story:** As a user, I want the toggle to be visually distinct and easy to understand so that I know which alarms are active.

#### Acceptance Criteria

1. THE Letting_In_App SHALL use Material Design 3 Switch component for the Activation_Toggle
2. THE Letting_In_App SHALL use distinct colors for ON and OFF states of the Activation_Toggle
3. THE Letting_In_App SHALL position the Activation_Toggle prominently in each Alarm_List_Item
4. THE Letting_In_App SHALL ensure the Activation_Toggle has sufficient touch target size (minimum 48dp)
5. THE Letting_In_App SHALL provide smooth animation when the Activation_Toggle changes state

### Requirement 12: Alarm Actions Menu

**User Story:** As a user, I want to access alarm actions through a menu so that I can edit, delete, or view statistics without cluttering the alarm list interface.

#### Acceptance Criteria

1. THE Letting_In_App SHALL provide a three-dot menu icon button for each Alarm_List_Item
2. THE Letting_In_App SHALL position the three-dot menu icon button to the right of the Activation_Toggle
3. WHEN a user taps the three-dot menu icon, THE Letting_In_App SHALL display a menu with options: "View Statistics", "Edit", and "Delete"
4. WHEN a user selects "View Statistics" from the menu, THE Letting_In_App SHALL navigate to the statistics screen for that alarm
5. WHEN a user selects "Edit" from the menu and the alarm is inactive, THE Letting_In_App SHALL navigate to the alarm editor screen
6. WHEN a user selects "Edit" from the menu and the alarm is active, THE Letting_In_App SHALL display a message requiring deactivation first
7. WHEN a user selects "Delete" from the menu and the alarm is inactive, THE Letting_In_App SHALL show a confirmation dialog and delete the alarm upon confirmation
8. WHEN a user selects "Delete" from the menu and the alarm is active, THE Letting_In_App SHALL display a message requiring deactivation first

### Requirement 13: Swipe to Delete Gesture

**User Story:** As a user, I want to swipe left on an alarm to quickly delete it so that I can remove alarms with a familiar gesture.

#### Acceptance Criteria

1. THE Letting_In_App SHALL support left swipe gesture on each Alarm_List_Item to reveal delete action
2. WHEN a user swipes left on an inactive Alarm_List_Item, THE Letting_In_App SHALL reveal a delete button
3. WHEN a user taps the revealed delete button, THE Letting_In_App SHALL show a confirmation dialog and delete the alarm upon confirmation
4. WHEN a user swipes left on an active Alarm_List_Item, THE Letting_In_App SHALL NOT reveal the delete button
5. THE Letting_In_App SHALL provide visual feedback indicating that active alarms cannot be deleted via swipe
6. THE Letting_In_App SHALL allow the user to dismiss the revealed delete button by swiping right or tapping elsewhere

### Requirement 14: Backward Compatibility with Existing Logic

**User Story:** As a user, I want the UI improvements to maintain all existing alarm functionality so that my alarms continue to work as expected.

#### Acceptance Criteria

1. THE Letting_In_App SHALL maintain all existing alarm scheduling logic when using the new Interval_Selector
2. THE Letting_In_App SHALL maintain all existing single active alarm enforcement when using the Activation_Toggle
3. THE Letting_In_App SHALL maintain all existing alarm editing restrictions (cannot edit active alarm) regardless of UI changes
4. THE Letting_In_App SHALL maintain all existing time calculation logic when using 24_Hour_Format
5. THE Letting_In_App SHALL maintain all existing alarm state management when using the Activation_Toggle
