# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

"Letting In" is an Android interval alarm application built with Kotlin and Jetpack Compose. Users can create recurring alarms that ring at specified intervals throughout a defined time window (e.g., every 30 minutes from 8:00 AM to 6:00 PM on weekdays).

**Key Characteristics**:
- Single active alarm at a time for lightweight operation
- Up to 10 saved alarm configurations
- Offline-first architecture with Room database
- Material Design 3 UI
- Minimum Android 8.0 (API 26), Target Android 14 (API 34)

## Build Commands

```bash
# Clean build
./gradlew clean

# Build project
./gradlew build

# Install debug version on connected device/emulator
./gradlew installDebug

# Generate debug APK
./gradlew assembleDebug

# Generate release APK
./gradlew assembleRelease
```

## Testing Commands

```bash
# Run all unit tests (JVM)
./gradlew test

# Run specific test class
./gradlew test --tests "HomeViewModelTest"

# Run all instrumented tests (requires connected device/emulator)
./gradlew connectedAndroidTest

# Run specific instrumented test
./gradlew connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.lettingin.intervalAlarm.AlarmFlowTest
```

## Architecture

### MVVM + Clean Architecture

The codebase follows a layered architecture:

**Data Layer** (`data/`):
- **Entities**: Room database tables (`@Entity` annotated classes)
- **DAOs**: Database access objects with suspend query methods
- **Repositories**: Abstraction over data sources (interfaces + implementations with `Impl` suffix)
- **Models**: Data classes representing domain objects

**Domain Layer** (`domain/scheduler/`):
- **AlarmScheduler**: Core business logic for calculating next ring times, handling intervals, and scheduling alarms with Android's AlarmManager
- Encapsulates all alarm timing calculations and AlarmManager interactions

**UI Layer** (`ui/`):
- **Screens**: Composable functions (PascalCase naming)
- **ViewModels**: `@HiltViewModel` classes managing UI state with StateFlow/MutableStateFlow
- **Navigation**: Centralized navigation setup with NavController

**Services & Receivers**:
- **AlarmNotificationService**: Foreground service that displays notifications and plays ringtones when alarms ring
- **AlarmReceiver**: BroadcastReceiver that handles alarm triggers from AlarmManager
- **BootReceiver**: Restores active alarms after device reboot

### Dependency Injection

- Uses **Hilt** for dependency injection
- All repositories, schedulers, and utilities are provided as singletons via `AppModule.kt`
- ViewModels use `@HiltViewModel` annotation and constructor injection

### Database

- **Room** database with schema versioning
- Schema exports: `app/schemas/com.lettingin.intervalAlarm.data.database.AppDatabase/`
- Always commit schema files to version control
- Migrations defined in `DatabaseMigrations.kt`

**Key Tables**:
- `interval_alarms`: Alarm configurations
- `alarm_state`: Current state of the active alarm
- `alarm_statistics`: Historical cycle statistics (last 5 cycles per alarm)
- `app_settings`: Global app settings

### State Management

- ViewModels expose UI state via `StateFlow<T>`
- UI state is updated immutably using data class `copy()`
- Repository methods are `suspend` functions called from `viewModelScope.launch`
- Database operations use Kotlin Coroutines and Flow

## Critical Patterns

### Error Handling

The app uses a centralized error handling system:

**ErrorHandler** (`util/ErrorHandler.kt`):
- Classifies errors into types (VALIDATION, PERMISSION, SCHEDULING, DATABASE, SYSTEM, UNKNOWN)
- Assigns severity levels (LOW, MEDIUM, HIGH, CRITICAL)
- Provides exponential backoff retry logic
- Generates user-friendly error messages

**Usage Pattern**:
```kotlin
@Inject
lateinit var errorHandler: ErrorHandler

val result = errorHandler.executeWithRetry(
    maxAttempts = 3,
    operation = "schedule alarm"
) {
    alarmScheduler.scheduleAlarm(alarm)
}

result.fold(
    onSuccess = { /* handle success */ },
    onFailure = { exception ->
        val errorResult = errorHandler.handleError(exception, "schedule alarm")
        _errorMessage.value = errorResult.userMessage
    }
)
```

**DataValidator** (`util/DataValidator.kt`):
- Validates alarm configurations before saving
- Checks: label length (≤60 chars), start < end time, interval ≥5 min, at least one day selected
- Use `dataValidator.validateAlarm(alarm)` before database operations

**DataIntegrityChecker** (`util/DataIntegrityChecker.kt`):
- Runs automatically on app startup
- Removes corrupted alarms, orphaned states, and old statistics
- Critical for maintaining database health

### Logging System

**AppLogger** (`util/AppLogger.kt`):
- Centralized logging singleton
- Categories: ALARM, SCHEDULING, NOTIFICATION, PERMISSION, DATABASE, UI, SYSTEM, ERROR
- Levels: VERBOSE, DEBUG, INFO, WARNING, ERROR
- In-memory buffer (last 500 entries) + logcat integration
- File export capability

**Usage**:
```kotlin
@Inject
lateinit var appLogger: AppLogger

// Basic logging
appLogger.i(AppLogger.CATEGORY_ALARM, "MyClass", "Alarm activated")
appLogger.e(AppLogger.CATEGORY_ERROR, "MyClass", "Failed to save", exception)

// Convenience methods
appLogger.logAlarmScheduled(alarmId, nextRingTime)
appLogger.logAlarmDismissed(alarmId, isUserDismissal = true)
```

**Always log**:
- Alarm state changes (activate, deactivate, pause, resume)
- Scheduling events (next ring time calculated)
- Errors and exceptions
- Permission changes
- System events (boot, time changes)

### Alarm Scheduling

**AlarmScheduler Interface** (`domain/scheduler/AlarmScheduler.kt`):
```kotlin
suspend fun scheduleAlarm(alarmId: Long)
suspend fun cancelAlarm(alarmId: Long)
suspend fun pauseAlarm(alarmId: Long, pauseDurationMillis: Long)
suspend fun resumeAlarm(alarmId: Long)
```

**Key Points**:
- Uses `AlarmManager.setExactAndAllowWhileIdle()` for precise timing even in Doze mode
- Calculates next ring times based on interval, time window, and selected days
- Handles edge cases: overnight alarms, day transitions, pause/resume logic
- All scheduling operations are logged with `appLogger`

### Testing Features

The app includes built-in testing tools (accessible in Alarm Editor):

**Test Alarm**: Triggers a test alarm in 5 seconds with current settings (useful for rapid iteration)
**Preview Ringtone**: Plays selected ringtone for 3 seconds

For debugging: Settings → Debug Information shows system info, active alarm details, permissions, logs, and statistics.

## Code Style

### Naming Conventions

- **Files**: `[Feature]Screen.kt`, `[Feature]ViewModel.kt`, `[Entity]Repository.kt`, `[Entity]RepositoryImpl.kt`
- **Classes**: PascalCase (no "I" prefix for interfaces)
- **Functions**: camelCase (Composables use PascalCase)
- **Repository methods**: Verb prefixes - `get`, `insert`, `update`, `delete`
- **Constants**: UPPER_SNAKE_CASE in companion objects

### Composables

- Use descriptive names: `AlarmListItem`, `IntervalSelector`, `TimeRangePicker`
- Extract reusable components to `ui/components/`
- Keep screen-level composables in feature-specific directories
- Use `remember`, `rememberSaveable`, and `derivedStateOf` appropriately

## Important Files

- `.kiro/steering/tech.md`: Technology stack and decisions
- `.kiro/steering/structure.md`: Detailed package organization
- `.kiro/steering/product.md`: Product vision and use cases
- `docs/TESTING_GUIDE.md`: Comprehensive manual and automated testing guide
- `docs/ERROR_HANDLING_GUIDE.md`: Error handling patterns and best practices
- `docs/LOGGING_SYSTEM_GUIDE.md`: Logging system usage and debugging
- `app/schemas/README.md`: Database schema versioning

## Permissions

The app requires several critical permissions for reliable alarm functionality:

- `SCHEDULE_EXACT_ALARM` / `USE_EXACT_ALARM`: Precise alarm scheduling
- `POST_NOTIFICATIONS`: Notification display (Android 13+)
- `USE_FULL_SCREEN_INTENT`: Full-screen alarm activity
- `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`: Reliable background operation

Permission checks are handled via `PermissionChecker` utility. The app displays warning banners on the home screen when critical permissions are missing.

## Development Workflow

1. **Before making changes**: Read relevant files in `.kiro/specs/` for feature context
2. **Data model changes**: Update Room entities, create migrations in `DatabaseMigrations.kt`, export new schema
3. **Add error handling**: Use `ErrorHandler.executeSafely()` or `executeWithRetry()` for operations that can fail
4. **Add logging**: Use `AppLogger` to log state changes and errors
5. **Validate data**: Use `DataValidator` before database operations
6. **Test thoroughly**: Use built-in test alarm feature, manual testing checklist, and automated tests
7. **Update documentation**: Update relevant `.md` files in `docs/` or `.kiro/specs/` if adding significant features

## Common Tasks

**Add a new alarm field**:
1. Update `IntervalAlarm` entity in `data/model/`
2. Create Room migration in `data/database/DatabaseMigrations.kt`
3. Update `AlarmDao` queries if needed
4. Update editor UI and ViewModel
5. Export new schema: `./gradlew build`
6. Test migration thoroughly

**Add a new screen**:
1. Create `[Feature]Screen.kt` in `ui/[feature]/`
2. Create `[Feature]ViewModel.kt` with `@HiltViewModel`
3. Add navigation route in `ui/navigation/`
4. Add screen to navigation graph
5. Handle error states with `ErrorHandler`
6. Add logging with `AppLogger`

**Debug alarm not ringing**:
1. Check Debug Screen (Settings → Debug Information)
2. Verify permissions granted
3. Check logcat for `[ALARM]` and `[SCHEDULING]` categories
4. Use test alarm feature to isolate issue
5. Check battery optimization settings
6. Review `AlarmSchedulerImpl` logs
