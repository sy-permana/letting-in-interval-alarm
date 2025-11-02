# Technology Stack

## Platform

- **Language**: Kotlin 1.9.20
- **Minimum SDK**: Android 8.0 (API 26)
- **Target SDK**: Android 14 (API 34)
- **Build System**: Gradle with Kotlin DSL
- **Java Version**: 17

## Core Libraries

### UI
- **Jetpack Compose**: Modern declarative UI framework
- **Material 3**: Latest Material Design components
- **Compose BOM**: 2023.10.01
- **Navigation Compose**: 2.7.5 for screen navigation
- **Material Icons Extended**: For comprehensive icon set

### Architecture
- **Hilt**: 2.48 for dependency injection
- **ViewModel**: Lifecycle-aware UI state management
- **Kotlin Coroutines**: 1.7.3 for asynchronous operations
- **Kotlin Flow**: Reactive data streams

### Data Persistence
- **Room**: 2.6.1 for local SQLite database
- **DataStore**: 1.0.0 for settings/preferences
- Room schema exports to `app/schemas/` directory

### Background Processing
- **WorkManager**: 2.9.0 for reliable background tasks
- **AlarmManager**: For precise alarm scheduling

### Testing
- **JUnit**: 4.13.2 for unit tests
- **MockK**: 1.13.8 for mocking
- **Coroutines Test**: 1.7.3 for testing coroutines
- **Espresso**: 3.5.1 for UI tests
- **Compose UI Test**: For Compose-specific testing

### Other
- **Accompanist Permissions**: 0.32.0 for permission handling
- **KSP**: 1.9.20-1.0.14 for annotation processing (replaces kapt)

## Architecture Pattern

**MVVM (Model-View-ViewModel)** with clean architecture principles:
- **UI Layer**: Jetpack Compose screens
- **ViewModel Layer**: State management and business logic
- **Repository Layer**: Data abstraction
- **Data Layer**: Room database and DAOs
- **Domain Layer**: Alarm scheduling logic

## Common Commands

### Build
```bash
./gradlew build
```

### Run on device/emulator
```bash
./gradlew installDebug
```

### Run tests
```bash
# Unit tests
./gradlew test

# Instrumented tests
./gradlew connectedAndroidTest
```

### Clean build
```bash
./gradlew clean
```

### Generate APK
```bash
./gradlew assembleDebug
./gradlew assembleRelease
```

### Check dependencies
```bash
./gradlew dependencies
```

## Code Generation

- **Room**: Generates DAO implementations and database code
- **Hilt**: Generates dependency injection code
- **KSP**: Used for annotation processing (faster than kapt)

Room schemas are exported to `app/schemas/` for version control and migration tracking.
