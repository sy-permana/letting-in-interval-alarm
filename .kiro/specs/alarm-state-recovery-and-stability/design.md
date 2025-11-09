# Design Document

## Overview

This design addresses two critical reliability issues in the Letting In interval alarm application:

1. **Stale Alarm State After App Restart**: When the app crashes or is force-closed, the displayed "next ring time" becomes stale and doesn't reflect the actual scheduled alarm, causing missed alarms.

2. **App Crashes and Performance Issues**: The app experiences unexpected crashes and performance degradation that impact user experience.

The solution implements a comprehensive state recovery mechanism that validates and synchronizes alarm state on app startup, along with defensive programming practices to prevent crashes and resource leaks.

## Architecture

### State Recovery Flow

```
App Start/Resume
    ↓
HomeViewModel.init()
    ↓
validateAndRecoverAlarmState()
    ↓
┌─────────────────────────────────────┐
│ 1. Check for active alarm in DB    │
└─────────────────────────────────────┘
    ↓
┌─────────────────────────────────────┐
│ 2. Validate next ring time          │
│    - Is it in the past?             │
│    - Does AlarmManager have it?     │
└─────────────────────────────────────┘
    ↓
┌─────────────────────────────────────┐
│ 3. Recalculate if needed            │
│    - Use current time               │
│    - Apply alarm interval logic     │
└─────────────────────────────────────┘
    ↓
┌─────────────────────────────────────┐
│ 4. Reschedule with AlarmManager     │
└─────────────────────────────────────┘
    ↓
┌─────────────────────────────────────┐
│ 5. Update database state            │
└─────────────────────────────────────┘
```

### Crash Prevention Architecture

```
ViewModel Layer
    ↓
Exception Handlers (try-catch)
    ↓
Coroutine Exception Handlers
    ↓
Repository Layer (with error wrapping)
    ↓
Database Layer
```

## Components and Interfaces

### 1. AlarmStateValidator

New utility class responsible for validating alarm state consistency.

```kotlin
interface AlarmStateValidator {
    /**
     * Validates that the displayed alarm state matches actual scheduled alarms
     * Returns validation result with any inconsistencies found
     */
    suspend fun validateAlarmState(alarmId: Long): ValidationResult
    
    /**
     * Checks if a next ring time is stale (in the past)
     */
    fun isNextRingTimeStale(nextRingTime: Long?): Boolean
    
    /**
     * Verifies that an alarm is actually scheduled in AlarmManager
     */
    fun isAlarmScheduledInSystem(alarmId: Long): Boolean
}

data class ValidationResult(
    val isValid: Boolean,
    val issues: List<ValidationIssue>,
    val suggestedAction: RecoveryAction
)

enum class ValidationIssue {
    STALE_NEXT_RING_TIME,
    MISSING_ALARM_MANAGER_ENTRY,
    MISSING_DATABASE_STATE,
    TIME_WINDOW_MISMATCH
}

enum class RecoveryAction {
    RECALCULATE_AND_RESCHEDULE,
    DEACTIVATE_ALARM,
    NO_ACTION_NEEDED
}
```

### 2. AlarmStateRecoveryManager

New component that handles automatic recovery of alarm state.

```kotlin
interface AlarmStateRecoveryManager {
    /**
     * Performs full state recovery for an active alarm
     * Called on app startup and resume
     */
    suspend fun recoverAlarmState(alarmId: Long): RecoveryResult
    
    /**
     * Synchronizes database state with AlarmManager
     */
    suspend fun synchronizeWithAlarmManager(alarmId: Long)
}

data class RecoveryResult(
    val success: Boolean,
    val action: String,
    val newNextRingTime: Long?,
    val error: Throwable?
)
```

### 3. Enhanced HomeViewModel

Modified to include state validation on initialization.

```kotlin
class HomeViewModel {
    init {
        viewModelScope.launch {
            // Validate and recover alarm state on startup
            validateAndRecoverActiveAlarm()
            
            // Then start observing
            observeActiveAlarm()
        }
    }
    
    private suspend fun validateAndRecoverActiveAlarm() {
        val activeAlarm = alarmRepository.getActiveAlarm().firstOrNull()
        if (activeAlarm != null) {
            alarmStateRecoveryManager.recoverAlarmState(activeAlarm.id)
        }
    }
}
```

### 4. Enhanced AlarmScheduler

Add method to check if alarm is actually scheduled.

```kotlin
interface AlarmScheduler {
    // Existing methods...
    
    /**
     * Checks if an alarm is currently scheduled in AlarmManager
     */
    fun isAlarmScheduled(alarmId: Long): Boolean
    
    /**
     * Gets the scheduled time for an alarm from AlarmManager
     * Returns null if not scheduled
     */
    fun getScheduledTime(alarmId: Long): Long?
}
```

### 5. Crash Prevention Utilities

Enhanced error handling throughout the app.

```kotlin
// Global coroutine exception handler
class GlobalExceptionHandler : CoroutineExceptionHandler {
    override fun handleException(context: CoroutineContext, exception: Throwable) {
        appLogger.e(CATEGORY_ERROR, "GlobalHandler", "Uncaught exception", exception)
        // Persist crash info
        crashReporter.logCrash(exception)
    }
}

// Repository-level error wrapping
abstract class SafeRepository {
    protected suspend fun <T> safeDbOperation(
        operation: String,
        block: suspend () -> T
    ): Result<T> {
        return try {
            Result.success(block())
        } catch (e: Exception) {
            appLogger.e(CATEGORY_DATABASE, "Repository", "DB operation failed: $operation", e)
            Result.failure(RepositoryException("Failed: $operation", e))
        }
    }
}
```

## Data Models

### Enhanced AlarmState

No changes needed to the data model itself, but we'll add validation logic.

```kotlin
data class AlarmState(
    val alarmId: Long,
    val lastRingTime: Long?,
    val nextScheduledRingTime: Long?,  // This is what we validate
    val isPaused: Boolean,
    val pauseUntilTime: Long?,
    val isStoppedForDay: Boolean,
    val currentDayStartTime: Long,
    val todayRingCount: Int,
    val todayUserDismissCount: Int,
    val todayAutoDismissCount: Int
) {
    /**
     * Validates if the next ring time is stale
     */
    fun isNextRingTimeStale(): Boolean {
        val nextRing = nextScheduledRingTime ?: return false
        return nextRing < System.currentTimeMillis()
    }
    
    /**
     * Checks if state is consistent
     */
    fun isConsistent(alarm: IntervalAlarm): Boolean {
        // Validate that next ring time is within alarm's time window
        if (nextScheduledRingTime == null) return true
        
        val nextRingDateTime = LocalDateTime.ofInstant(
            Instant.ofEpochMilli(nextScheduledRingTime),
            ZoneId.systemDefault()
        )
        
        val isValidDay = alarm.selectedDays.contains(nextRingDateTime.dayOfWeek)
        val isWithinWindow = nextRingDateTime.toLocalTime() >= alarm.startTime &&
                            nextRingDateTime.toLocalTime() <= alarm.endTime
        
        return isValidDay && isWithinWindow
    }
}
```

### CrashReport

New model for persisting crash information.

```kotlin
data class CrashReport(
    val timestamp: Long,
    val exceptionType: String,
    val message: String,
    val stackTrace: String,
    val appState: String  // JSON of relevant app state
)
```

## Error Handling

### Layered Error Handling Strategy

1. **ViewModel Layer**: Catch all exceptions, update UI error state, log errors
2. **Repository Layer**: Wrap database exceptions in domain exceptions
3. **Scheduler Layer**: Handle AlarmManager exceptions gracefully
4. **Global Handler**: Catch any uncaught exceptions, log and persist

### Error Recovery Strategies

| Error Type | Recovery Strategy |
|------------|------------------|
| Stale next ring time | Recalculate based on current time and reschedule |
| Missing AlarmManager entry | Reschedule alarm using database state |
| Database corruption | Attempt to recreate state from alarm configuration |
| Permission denied | Notify user, disable alarm until permission granted |
| Null pointer | Use safe calls and elvis operators, log warning |

### Memory Leak Prevention

1. **Coroutine Scoping**: All coroutines tied to ViewModel lifecycle
2. **Flow Collection**: Use `stateIn` with `WhileSubscribed` to auto-cancel
3. **Job Cancellation**: Explicitly cancel jobs in `onCleared()`
4. **Resource Cleanup**: Release all resources when ViewModels are destroyed

```kotlin
class HomeViewModel {
    private var stateValidationJob: Job? = null
    
    override fun onCleared() {
        super.onCleared()
        stateValidationJob?.cancel()
        // Cancel all child coroutines
        viewModelScope.coroutineContext.cancelChildren()
    }
}
```

## Testing Strategy

### Unit Tests

1. **AlarmStateValidator Tests**
   - Test stale time detection
   - Test validation logic for various scenarios
   - Test edge cases (midnight, DST changes)

2. **AlarmStateRecoveryManager Tests**
   - Test recovery from stale state
   - Test recovery from missing AlarmManager entry
   - Test recovery failure scenarios

3. **ViewModel Tests**
   - Test initialization with valid state
   - Test initialization with stale state
   - Test exception handling

### Integration Tests

1. **State Recovery Flow**
   - Simulate app crash and restart
   - Verify state is recovered correctly
   - Verify alarm reschedules properly

2. **AlarmManager Integration**
   - Verify alarms are actually scheduled
   - Verify scheduled times match database

### Crash Testing

1. **Stress Testing**
   - Rapid alarm activation/deactivation
   - Multiple concurrent operations
   - Low memory scenarios

2. **Edge Case Testing**
   - Time zone changes
   - System time changes
   - Device reboot during alarm ring

## Performance Optimizations

### Startup Performance

1. **Lazy Validation**: Only validate if active alarm exists
2. **Background Thread**: Run validation on IO dispatcher
3. **Timeout**: Set 2-second timeout for validation to prevent blocking

### Memory Optimization

1. **Limit Flow Collectors**: Use `stateIn` to share single upstream
2. **Cancel Unused Jobs**: Aggressively cancel when not needed
3. **Avoid Leaks**: Use `viewModelScope` for all coroutines

### Database Optimization

1. **Batch Operations**: Group related database updates
2. **Transaction Wrapping**: Use transactions for multi-step operations
3. **Index Usage**: Ensure queries use proper indexes

## Implementation Phases

### Phase 1: State Validation (Core Fix)
- Implement AlarmStateValidator
- Add validation to HomeViewModel init
- Add logging for validation results

### Phase 2: State Recovery
- Implement AlarmStateRecoveryManager
- Integrate with AlarmScheduler
- Add automatic recovery on validation failure

### Phase 3: Crash Prevention
- Add comprehensive exception handling
- Implement global exception handler
- Add crash reporting

### Phase 4: Memory Leak Prevention
- Audit all coroutine usage
- Implement proper cleanup in onCleared()
- Add lifecycle logging

### Phase 5: Testing & Validation
- Write unit tests
- Perform integration testing
- Stress test the app

## Monitoring and Logging

### Key Metrics to Log

1. **State Validation Events**
   - Validation triggered
   - Validation result (pass/fail)
   - Issues found
   - Recovery action taken

2. **Recovery Events**
   - Recovery initiated
   - Recovery success/failure
   - New next ring time
   - Time taken

3. **Crash Events**
   - Exception type and message
   - Stack trace
   - App state at crash time
   - User actions leading to crash

### Log Categories

- `STATE_VALIDATION`: All validation-related logs
- `STATE_RECOVERY`: All recovery-related logs
- `CRASH`: All crash-related logs
- `PERFORMANCE`: Performance metrics

## Security Considerations

1. **Permission Validation**: Always check permissions before scheduling
2. **Input Validation**: Validate all time calculations
3. **Safe Defaults**: Use safe defaults when state is corrupted
4. **Data Integrity**: Validate database state before using

## Backward Compatibility

This design maintains full backward compatibility:
- No database schema changes required
- Existing alarm configurations work unchanged
- Recovery is transparent to users
- No API changes to public interfaces

## Success Criteria

1. **State Recovery**: 100% of stale states detected and recovered within 2 seconds of app start
2. **Crash Reduction**: 90% reduction in crash rate
3. **Memory Leaks**: Zero memory leaks detected in profiling
4. **Performance**: No noticeable impact on app startup time (<100ms overhead)
5. **User Experience**: Users never see stale next ring times after app restart
