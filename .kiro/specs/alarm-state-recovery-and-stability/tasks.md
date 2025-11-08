# Implementation Plan

- [x] 1. Implement alarm state validation utilities
  - Create AlarmStateValidator utility class with methods to detect stale next ring times and validate alarm state consistency
  - Add method to check if alarm is scheduled in AlarmManager by attempting to retrieve the PendingIntent
  - Implement validation logic that compares database state with AlarmManager state
  - Add validation for time window consistency (next ring time within alarm's start/end time)
  - _Requirements: 1.1, 1.2, 1.3, 3.1, 3.2_

- [ ] 2. Implement alarm state recovery manager
  - [-] 2.1 Create AlarmStateRecoveryManager class with recovery orchestration logic
    - Implement recoverAlarmState() method that validates and fixes inconsistent state
    - Add logic to recalculate next ring time when stale time is detected
    - Implement synchronization between database state and AlarmManager
    - Add recovery result tracking with success/failure status
    - _Requirements: 1.2, 1.4, 3.2_
  
  - [ ] 2.2 Integrate recovery manager with AlarmScheduler
    - Add isAlarmScheduled() method to AlarmScheduler interface and implementation
    - Implement getScheduledTime() method to retrieve scheduled time from AlarmManager
    - Update AlarmSchedulerImpl to support state validation queries
    - _Requirements: 1.3, 3.1_
  
  - [ ] 2.3 Add automatic state recovery to HomeViewModel initialization
    - Modify HomeViewModel init block to call validateAndRecoverActiveAlarm()
    - Implement validation check that runs before starting alarm state observation
    - Add error handling for recovery failures with user notification
    - Ensure recovery completes within 2 seconds with timeout handling
    - _Requirements: 1.1, 1.2, 3.4_

- [ ] 3. Enhance error handling and crash prevention
  - [ ] 3.1 Add comprehensive exception handling to ViewModels
    - Wrap all coroutine launches in try-catch blocks in HomeViewModel
    - Add exception handling to AlarmEditorViewModel operations
    - Implement error state updates in UI when exceptions occur
    - Add logging for all caught exceptions with context
    - _Requirements: 2.1, 2.2, 2.5, 4.1, 4.4_
  
  - [ ] 3.2 Implement safe database operation wrappers in repositories
    - Create SafeRepository base class with safeDbOperation() helper method
    - Update AlarmRepositoryImpl to extend SafeRepository and use safe wrappers
    - Update AlarmStateRepositoryImpl with exception handling
    - Add error logging for all database operation failures
    - _Requirements: 2.1, 2.5, 4.4_
  
  - [ ] 3.3 Add null safety checks throughout alarm operations
    - Add null checks before accessing alarm state properties
    - Use safe call operators (?.) and elvis operators (?:) for nullable values
    - Implement default values for critical operations when data is null
    - Add validation before passing data to AlarmManager
    - _Requirements: 2.2, 2.3_

- [ ] 4. Implement memory leak prevention
  - [ ] 4.1 Audit and fix coroutine lifecycle management
    - Review all viewModelScope.launch calls for proper exception handling
    - Ensure all Flow collectors use stateIn with WhileSubscribed strategy
    - Add explicit job cancellation in ViewModel onCleared() methods
    - Implement coroutine timeout for long-running operations
    - _Requirements: 2.3, 2.4, 5.1, 5.2_
  
  - [ ] 4.2 Implement proper resource cleanup in ViewModels
    - Override onCleared() in all ViewModels to cancel jobs
    - Add cleanup for any cached data or listeners
    - Implement cancelChildren() call on viewModelScope context
    - Add logging for cleanup operations
    - _Requirements: 2.3, 5.1, 5.2_
  
  - [ ] 4.3 Optimize Flow collection patterns
    - Replace multiple Flow collectors with single shared upstream using stateIn
    - Limit concurrent Flow collectors to prevent memory buildup
    - Use conflated channels where appropriate to drop intermediate values
    - Add lifecycle-aware collection in UI layer
    - _Requirements: 2.4, 5.3, 5.4_

- [ ] 5. Add comprehensive logging and monitoring
  - [ ] 5.1 Implement state validation logging
    - Add log entries when validation is triggered on app start
    - Log validation results including any issues found
    - Log recovery actions taken with timestamps
    - Add performance metrics for validation duration
    - _Requirements: 3.3, 4.1, 4.3_
  
  - [ ] 5.2 Add crash reporting and persistence
    - Create CrashReport data model for storing crash information
    - Implement crash persistence to local storage before app termination
    - Add global exception handler that logs uncaught exceptions
    - Store app state snapshot with crash reports
    - _Requirements: 4.1, 4.2, 4.4_
  
  - [ ] 5.3 Enhance alarm state transition logging
    - Log all alarm state changes with before/after values
    - Add logging for AlarmManager scheduling operations
    - Log time calculations and next ring time updates
    - Add context information (current time, alarm config) to logs
    - _Requirements: 4.3, 4.4_

- [ ] 6. Implement startup state recovery flow
  - Integrate AlarmStateValidator and AlarmStateRecoveryManager into app startup
  - Add state validation call in LettingInApplication onCreate() for active alarms
  - Implement background thread execution for validation to avoid blocking UI
  - Add timeout mechanism (2 seconds) for validation operations
  - Handle validation failures gracefully with user notification
  - _Requirements: 1.1, 1.2, 1.4, 3.4, 3.5_

- [ ] 7. Add validation to BootReceiver
  - Integrate state validation into BootReceiver alarm restoration logic
  - Ensure restored alarms have valid next ring times based on current time
  - Add validation that restored alarms are actually scheduled in AlarmManager
  - Implement recovery if boot restoration fails
  - _Requirements: 3.3, 3.4_

- [ ] 8. Implement defensive programming in AlarmScheduler
  - Add validation before all AlarmManager operations
  - Implement retry logic for failed scheduling operations (already exists, verify)
  - Add checks for permission availability before scheduling
  - Validate calculated next ring times before scheduling
  - Add bounds checking for time calculations
  - _Requirements: 2.1, 2.2, 2.5_

- [ ] 9. Add state consistency validation helpers
  - Implement AlarmState.isNextRingTimeStale() extension method
  - Add AlarmState.isConsistent() method to validate against alarm configuration
  - Create validation utilities for time window checks
  - Add helper methods for detecting time zone and system time changes
  - _Requirements: 1.1, 1.2, 3.1_

- [ ] 10. Performance optimization and testing
  - [ ] 10.1 Optimize startup validation performance
    - Implement lazy validation that only runs when active alarm exists
    - Add caching for validation results to avoid redundant checks
    - Optimize database queries used in validation
    - Profile validation code to ensure <100ms overhead
    - _Requirements: 5.3, 5.5_
  
  - [ ] 10.2 Add diagnostic tools for debugging
    - Create debug screen section showing alarm state validation status
    - Add button to manually trigger state validation
    - Display last validation result and timestamp
    - Show AlarmManager scheduled alarms vs database state comparison
    - _Requirements: 4.1, 4.3_
