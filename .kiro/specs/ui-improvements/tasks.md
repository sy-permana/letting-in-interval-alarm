# Implementation Plan

- [x] 1. Create TimeFormatter utility for 24-hour format
  - Create `TimeFormatter` object in `util` package with `format24Hour()` methods for LocalTime and timestamp
  - Add `formatTimeRange()` method for displaying time ranges
  - Add unit tests for TimeFormatter covering midnight, noon, afternoon, and range formatting
  - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5_

- [x] 2. Update time displays to use 24-hour format
- [x] 2.1 Update HomeScreen time displays
  - Replace time formatting in alarm list items to use `TimeFormatter.formatTimeRange()`
  - Update next ring time display to use `TimeFormatter.format24Hour()`
  - Update any other time-related displays in HomeScreen
  - _Requirements: 2.1, 2.2, 2.3, 4.1_

- [x] 2.2 Update AlarmEditorScreen time pickers
  - Configure TimePicker to use 24-hour format by setting `is24Hour = true`
  - Update time display labels to show 24-hour format
  - Test time picker behavior for edge cases (midnight, noon)
  - _Requirements: 3.1, 3.2, 3.3, 3.4_

- [x] 2.3 Update AlarmRingingActivity time display
  - Update current time display to use `TimeFormatter.format24Hour()`
  - _Requirements: 4.2_

- [x] 2.4 Update StatisticsScreen time displays
  - Update any time-related displays in statistics to use 24-hour format
  - _Requirements: 4.3_

- [x] 3. Create IntervalSelector component
- [x] 3.1 Implement IntervalSlider composable
  - Create slider component with fixed 5-minute step size for all time ranges
  - Implement snap-to-step logic for 5-minute increments
  - Add min/max labels below slider
  - _Requirements: 5.1, 5.2, 5.3, 8.1, 8.2_

- [x] 3.2 Implement QuickIntervalOptions composable
  - Create horizontal row of FilterChip components for 15, 30, 45, 60 minute presets
  - Filter options based on maxInterval (only show valid options)
  - Implement selection highlighting for current interval
  - Add click handlers to update interval value
  - _Requirements: 6.1, 6.2, 6.3, 6.4, 6.5, 6.6, 6.7_

- [x] 3.3 Implement IntervalSelector composite component
  - Combine current interval display, quick options, and slider into single component
  - Add interval formatting helper function (handles minutes, hours, mixed)
  - Ensure proper spacing and layout per design specifications
  - _Requirements: 5.4, 7.1, 7.2, 7.3, 7.4, 7.5_

- [x] 3.4 Integrate IntervalSelector into AlarmEditorScreen
  - Replace existing interval text field with IntervalSelector component
  - Connect to AlarmEditorViewModel's updateInterval method
  - Ensure maxInterval is calculated from start/end time difference, capped at 12 hours (720 minutes)
  - Update interval when start/end time changes
  - Automatically adjust current interval if it exceeds new maximum
  - _Requirements: 5.1, 5.2, 8.3, 8.4, 8.5_

- [x] 4. Implement toggle-based alarm activation
- [x] 4.1 Add confirmation dialog state to HomeViewModel
  - Add `showActivationConfirmation` boolean to HomeUiState
  - Add `pendingActivationAlarmId` to HomeUiState
  - Implement `onToggleAlarm` method with confirmation logic
  - Implement `confirmActivation` and `dismissActivationConfirmation` methods
  - _Requirements: 10.1, 10.6_

- [x] 4.2 Create ActivationConfirmationDialog composable
  - Create AlertDialog with swap icon, title, and message
  - Display current active alarm label and new alarm label in message
  - Add confirm and cancel buttons
  - _Requirements: 10.1, 10.2, 10.3, 10.4, 10.5_

- [x] 4.3 Update AlarmListItem with Switch component
  - Replace activation/deactivation buttons with Material3 Switch
  - Position switch to the right of alarm info
  - Connect switch to onToggleActive callback
  - Ensure switch reflects alarm's active state
  - Apply proper styling (colors, size, touch target)
  - _Requirements: 9.1, 9.2, 9.3, 9.4, 9.5, 9.6, 11.1, 11.2, 11.3, 11.4, 11.5_

- [x] 4.4 Integrate confirmation dialog in HomeScreen
  - Display ActivationConfirmationDialog when showActivationConfirmation is true
  - Pass current active alarm label and pending alarm label to dialog
  - Handle confirm action by calling viewModel.confirmActivation()
  - Handle dismiss action by calling viewModel.dismissActivationConfirmation()
  - _Requirements: 10.4, 10.5, 10.7_

- [x] 5. Implement three-dot menu for alarm actions
- [x] 5.1 Add three-dot menu to AlarmListItem
  - Add IconButton with MoreVert icon to the right of Switch
  - Add menu state management (showMenu boolean)
  - Position menu button with proper spacing
  - _Requirements: 12.1, 12.2_

- [x] 5.2 Implement DropdownMenu with actions
  - Create DropdownMenu with three options: View Statistics, Edit, Delete
  - Add appropriate icons for each menu item
  - Implement click handlers for each option
  - _Requirements: 12.3, 12.4_

- [x] 5.3 Add edit restriction logic for active alarms
  - Check if alarm is active when Edit is selected
  - Show Snackbar message "Deactivate the alarm before editing" if active
  - Navigate to editor screen if inactive
  - _Requirements: 12.5, 12.6_

- [x] 5.4 Add delete restriction logic for active alarms
  - Check if alarm is active when Delete is selected
  - Show Snackbar message "Deactivate the alarm before deleting" if active
  - Show confirmation dialog and delete if inactive
  - _Requirements: 12.7, 12.8_

- [x] 6. Implement swipe-to-delete gesture
- [x] 6.1 Create SwipeToDeleteWrapper composable
  - Implement SwipeToDismiss with EndToStart direction
  - Add red background with delete icon
  - Make swipe conditional based on alarm active state (enabled parameter)
  - _Requirements: 13.1, 13.2_

- [x] 6.2 Integrate swipe gesture in AlarmListItem
  - Wrap AlarmListItem content with SwipeToDeleteWrapper
  - Pass enabled=false for active alarms
  - Connect onDelete callback to delete confirmation flow
  - _Requirements: 13.3, 13.4, 13.5, 13.6_

- [x] 7. Add string resources for new UI elements
  - Add confirmation dialog strings (title, message, buttons)
  - Add menu item strings (View Statistics, Edit, Delete)
  - Add error message strings (deactivate before edit/delete)
  - Add plural resources for interval formatting (minutes, hours)
  - _Requirements: All requirements (localization support)_

- [x] 8. Update HomeScreen layout
  - Integrate new AlarmListItem component in alarm list
  - Ensure proper ordering (active alarm first)
  - Add Snackbar host for error messages
  - Test list performance with multiple alarms
  - _Requirements: 9.1, 10.7, 12.5, 12.6, 12.7, 12.8_

- [x] 9. Verify backward compatibility
  - Test that existing alarms load and display correctly
  - Verify alarm scheduling logic unchanged
  - Confirm single active alarm enforcement still works
  - Test alarm editing restrictions (cannot edit active alarm)
  - Verify all time calculations work with 24-hour format
  - _Requirements: 14.1, 14.2, 14.3, 14.4, 14.5_

- [x] 10. End-to-end testing
  - Test complete flow: create alarm, activate with toggle, confirm switch
  - Test interval selection with both slider and quick options
  - Test swipe-to-delete on inactive alarms
  - Test three-dot menu actions (view stats, edit, delete)
  - Test all time displays show 24-hour format
  - Test edge cases: midnight, noon, cross-day time ranges
  - _Requirements: All requirements_
