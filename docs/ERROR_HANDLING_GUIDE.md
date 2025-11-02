# Error Handling Guide

## Overview

The Letting In app implements a comprehensive error handling system that provides:

- **Centralized error handling** with classification and severity levels
- **Retry logic** with exponential backoff for transient failures
- **Data validation** and integrity checking
- **User-friendly error messages** with suggested actions
- **Automatic data cleanup** on app startup
- **Comprehensive logging** of all errors

## Components

### 1. ErrorHandler

Centralized error handling utility that classifies errors, provides retry logic, and generates user-friendly messages.

**Location**: `app/src/main/java/com/lettingin/intervalAlarm/util/ErrorHandler.kt`

#### Error Types

- **VALIDATION**: Input validation errors
- **PERMISSION**: Missing or denied permissions
- **SCHEDULING**: Alarm scheduling failures
- **DATABASE**: Database operation failures
- **NETWORK**: Network-related errors (future use)
- **SYSTEM**: System-level errors
- **UNKNOWN**: Unclassified errors

#### Error Severity Levels

- **LOW**: Non-critical, can be ignored
- **MEDIUM**: Important but recoverable
- **HIGH**: Critical, requires user action
- **CRITICAL**: App-breaking, requires immediate attention

#### Usage

```kotlin
@Inject
lateinit var errorHandler: ErrorHandler

// Handle an error
try {
    // Some operation
} catch (e: Exception) {
    val errorResult = errorHandler.handleError(e, "operation name")
    showErrorToUser(errorResult.userMessage)
}

// Execute with retry logic
val result = errorHandler.executeWithRetry(
    maxAttempts = 3,
    operation = "schedule alarm"
) {
    alarmScheduler.scheduleAlarm(alarm)
}

result.fold(
    onSuccess = { /* Success */ },
    onFailure = { exception ->
        val errorResult = errorHandler.handleError(exception)
        showError(errorResult.userMessage)
    }
)

// Execute safely with error handling
val result = errorHandler.executeSafely(
    operation = "save alarm",
    onError = { errorResult ->
        showError(errorResult.userMessage)
    }
) {
    alarmRepository.insertAlarm(alarm)
}
```

### 2. DataValidator

Validates alarm configurations and business rules.

**Location**: `app/src/main/java/com/lettingin/intervalAlarm/util/DataValidator.kt`

#### Validation Rules

- **Label**: Maximum 60 characters
- **Times**: Start time must be before end time
- **Interval**: Minimum 5 minutes, cannot exceed time range
- **Days**: At least one day must be selected
- **Ringtone**: Must be selected

#### Usage

```kotlin
@Inject
lateinit var dataValidator: DataValidator

// Validate an alarm
val validationResult = dataValidator.validateAlarm(alarm)
if (!validationResult.isValid) {
    showError(validationResult.getErrorMessage())
    return
}

// Validate pause duration
val pauseValidation = dataValidator.validatePauseDuration(
    alarm, pauseDurationMillis, currentTimeMillis
)

// Check if alarm is corrupted
if (dataValidator.isAlarmCorrupted(alarm)) {
    // Handle corrupted alarm
}

// Sanitize alarm data
val sanitizedAlarm = dataValidator.sanitizeAlarm(alarm)
```

### 3. DataIntegrityChecker

Validates and cleans up data on app startup.

**Location**: `app/src/main/java/com/lettingin/intervalAlarm/util/DataIntegrityChecker.kt`

#### Integrity Checks

1. **Corrupted Alarms**: Removes alarms with invalid data
2. **Orphaned States**: Removes alarm states without corresponding alarms
3. **Old Statistics**: Keeps only last 5 cycles per alarm

#### Usage

```kotlin
@Inject
lateinit var dataIntegrityChecker: DataIntegrityChecker

// Run full integrity check (on app startup)
lifecycleScope.launch {
    val result = dataIntegrityChecker.runIntegrityCheck()
    
    if (result.corruptedAlarmsRemoved > 0) {
        showNotification("Removed ${result.corruptedAlarmsRemoved} corrupted alarms")
    }
}

// Validate active alarm state
val isValid = dataIntegrityChecker.validateActiveAlarmState()
if (!isValid) {
    // Handle invalid active alarm
}
```

### 4. Custom Exceptions

Specific exception types for different error scenarios.

```kotlin
// Validation error
throw ValidationException("Start time must be before end time")

// Permission error
throw PermissionException("NOTIFICATION", "Notification permission required")

// Scheduling error
throw SchedulingException("Failed to schedule alarm", cause)

// Database error
throw DatabaseException("Failed to insert alarm", cause)

// Corrupted data
throw CorruptedDataException("Alarm data is corrupted")
```

## Error Handling Patterns

### Pattern 1: ViewModel Error Handling

```kotlin
@HiltViewModel
class MyViewModel @Inject constructor(
    private val repository: Repository,
    private val errorHandler: ErrorHandler,
    private val appLogger: AppLogger
) : ViewModel() {
    
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()
    
    fun performOperation() {
        viewModelScope.launch {
            val result = errorHandler.executeWithRetry(
                operation = "perform operation"
            ) {
                repository.doSomething()
            }
            
            result.fold(
                onSuccess = {
                    _errorMessage.value = null
                    appLogger.i(AppLogger.CATEGORY_UI, "MyViewModel", "Operation successful")
                },
                onFailure = { exception ->
                    val errorResult = errorHandler.handleError(exception, "perform operation")
                    _errorMessage.value = errorResult.userMessage
                }
            )
        }
    }
    
    fun clearError() {
        _errorMessage.value = null
    }
}
```

### Pattern 2: Repository Error Handling

```kotlin
@Singleton
class MyRepositoryImpl @Inject constructor(
    private val dao: MyDao
) : MyRepository {
    
    override suspend fun insertItem(item: Item): Long {
        return try {
            dao.insertItem(item)
        } catch (e: Exception) {
            throw DatabaseException("Failed to insert item", e)
        }
    }
}
```

### Pattern 3: Service Error Handling

```kotlin
class MyService : Service() {
    
    @Inject
    lateinit var errorHandler: ErrorHandler
    
    @Inject
    lateinit var appLogger: AppLogger
    
    private fun performCriticalOperation() {
        try {
            // Critical operation
        } catch (e: Exception) {
            val errorResult = errorHandler.handleError(e, "critical operation")
            
            if (errorResult.severity == ErrorHandler.ErrorSeverity.CRITICAL) {
                // Handle critical error
                appLogger.e(AppLogger.CATEGORY_ERROR, "MyService", 
                    "Critical error: ${errorResult.message}", e)
            }
        }
    }
}
```

## Retry Logic

The error handler provides exponential backoff retry logic:

### Default Configuration

- **Max Attempts**: 3
- **Initial Delay**: 1000ms (1 second)
- **Max Delay**: 10000ms (10 seconds)
- **Backoff Factor**: 2.0

### Retry Behavior

1. **Attempt 1**: Immediate
2. **Attempt 2**: After 1 second
3. **Attempt 3**: After 2 seconds
4. **Attempt 4**: After 4 seconds (if max attempts > 3)

### Custom Retry Configuration

```kotlin
val result = errorHandler.executeWithRetry(
    maxAttempts = 5,
    initialDelayMs = 500L,
    maxDelayMs = 5000L,
    factor = 1.5,
    operation = "custom operation"
) {
    // Operation that may fail
}
```

## Data Integrity

### Startup Integrity Check

The app automatically runs an integrity check on startup:

1. **Checks for corrupted alarms**
   - Invalid time ranges
   - Invalid intervals
   - Missing required fields

2. **Removes orphaned states**
   - Alarm states without corresponding alarms

3. **Cleans up old statistics**
   - Keeps only last 5 cycles per alarm

### Manual Integrity Check

```kotlin
// Run integrity check manually
val result = dataIntegrityChecker.runIntegrityCheck()

println("Corrupted alarms removed: ${result.corruptedAlarmsRemoved}")
println("Orphaned states removed: ${result.orphanedStatesRemoved}")
println("Old statistics removed: ${result.oldStatisticsRemoved}")
```

## User-Friendly Error Messages

The error handler automatically generates user-friendly messages:

| Error Type | User Message | Suggested Action |
|------------|--------------|------------------|
| Validation | "Please check your input" | "Review and correct the highlighted fields" |
| Permission | "Permission required: [name]" | "Grant permission in Settings" |
| Scheduling | "Failed to schedule alarm" | "Check alarm settings and try again" |
| Database | "Failed to save data" | "Restart the app if problem persists" |
| Corrupted Data | "Alarm data is corrupted" | "The corrupted alarm has been removed" |

## Error Logging

All errors are automatically logged with appropriate severity:

```kotlin
// Critical errors
appLogger.e(AppLogger.CATEGORY_ERROR, TAG, "CRITICAL ERROR: ${errorResult.message}")

// High severity
appLogger.e(AppLogger.CATEGORY_ERROR, TAG, "HIGH SEVERITY: ${errorResult.message}")

// Medium severity
appLogger.w(AppLogger.CATEGORY_ERROR, TAG, "MEDIUM SEVERITY: ${errorResult.message}")

// Low severity
appLogger.i(AppLogger.CATEGORY_ERROR, TAG, "LOW SEVERITY: ${errorResult.message}")
```

## Testing Error Handling

### Unit Tests

```kotlin
@Test
fun `test error classification`() {
    val exception = ValidationException("Invalid input")
    val result = errorHandler.handleError(exception)
    
    assertEquals(ErrorHandler.ErrorType.VALIDATION, result.type)
    assertEquals(ErrorHandler.ErrorSeverity.LOW, result.severity)
    assertTrue(result.isRecoverable)
}

@Test
fun `test retry logic`() = runTest {
    var attempts = 0
    
    val result = errorHandler.executeWithRetry(maxAttempts = 3) {
        attempts++
        if (attempts < 3) throw Exception("Fail")
        "Success"
    }
    
    assertTrue(result.isSuccess)
    assertEquals(3, attempts)
}
```

### Integration Tests

```kotlin
@Test
fun `test data integrity check`() = runTest {
    // Insert corrupted alarm
    val corruptedAlarm = IntervalAlarm(
        id = 1,
        startTime = LocalTime.of(10, 0),
        endTime = LocalTime.of(9, 0), // Invalid: end before start
        intervalMinutes = 30,
        selectedDays = emptySet(), // Invalid: no days selected
        ringtoneUri = ""
    )
    
    alarmRepository.insertAlarm(corruptedAlarm)
    
    // Run integrity check
    val result = dataIntegrityChecker.runIntegrityCheck()
    
    assertEquals(1, result.corruptedAlarmsRemoved)
}
```

## Best Practices

### 1. Always Use Error Handler

```kotlin
// Good
val result = errorHandler.executeSafely("save alarm") {
    repository.saveAlarm(alarm)
}

// Bad
try {
    repository.saveAlarm(alarm)
} catch (e: Exception) {
    // Manual error handling
}
```

### 2. Provide Context

```kotlin
// Good
errorHandler.handleError(exception, "activate alarm for user")

// Bad
errorHandler.handleError(exception)
```

### 3. Use Appropriate Exception Types

```kotlin
// Good
if (alarm.startTime >= alarm.endTime) {
    throw ValidationException("Start time must be before end time")
}

// Bad
if (alarm.startTime >= alarm.endTime) {
    throw Exception("Invalid time")
}
```

### 4. Log Errors

```kotlin
// Good
result.onFailure { exception ->
    val errorResult = errorHandler.handleError(exception)
    appLogger.e(AppLogger.CATEGORY_ERROR, TAG, errorResult.message, exception)
}

// Bad
result.onFailure { exception ->
    // Silent failure
}
```

### 5. Show User-Friendly Messages

```kotlin
// Good
_errorMessage.value = errorResult.userMessage

// Bad
_errorMessage.value = exception.message
```

## Troubleshooting

### Issue: Errors Not Being Logged

**Solution**: Ensure ErrorHandler is injected and used:

```kotlin
@Inject
lateinit var errorHandler: ErrorHandler

// Use it
errorHandler.handleError(exception, "operation")
```

### Issue: Retry Logic Not Working

**Solution**: Ensure operation is suspend function:

```kotlin
// Correct
errorHandler.executeWithRetry {
    suspendFunction()
}

// Incorrect
errorHandler.executeWithRetry {
    blockingFunction() // Won't work
}
```

### Issue: Corrupted Data Not Removed

**Solution**: Check integrity checker is running on startup:

```kotlin
// In MainActivity onCreate
lifecycleScope.launch {
    dataIntegrityChecker.runIntegrityCheck()
}
```

## Summary

The error handling system provides:

✅ Centralized error handling with classification
✅ Automatic retry logic with exponential backoff
✅ Data validation and integrity checking
✅ User-friendly error messages
✅ Comprehensive error logging
✅ Automatic data cleanup on startup

This system significantly improves app reliability and user experience by handling errors gracefully and providing clear feedback.

---

**Related Documentation**:
- Logging System Guide
- Data Validation Guide
- Testing Guide
