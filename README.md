# Letting In - Interval Alarm App

An Android interval alarm application built with Kotlin and Jetpack Compose that allows users to create recurring alarms that ring at specified intervals throughout a defined time window.

## Features

- Create up to 10 interval alarms with customizable settings
- Single active alarm at a time for lightweight operation
- Configurable notification types (full-screen, popup, sound-only)
- Day-of-week scheduling
- Pause and resume functionality
- Statistics tracking
- Material Design 3 UI

## Tech Stack

- **Language**: Kotlin 1.9.20
- **Minimum SDK**: Android 8.0 (API 26)
- **Target SDK**: Android 14 (API 34)
- **UI Framework**: Jetpack Compose with Material 3
- **Database**: Room
- **Dependency Injection**: Hilt
- **Background Tasks**: WorkManager
- **Async**: Kotlin Coroutines & Flow

## Project Structure

```
app/src/main/java/com/lettingin/intervalAlarm/
├── data/
│   ├── database/      # Room entities and DAOs
│   ├── repository/    # Repository implementations
│   └── model/         # Data models
├── domain/
│   └── scheduler/     # Alarm scheduling logic
├── ui/
│   ├── home/          # Home screen
│   ├── editor/        # Alarm editor screen
│   ├── settings/      # Settings screen
│   ├── statistics/    # Statistics screen
│   ├── alarm/         # Alarm ringing activity
│   └── theme/         # Material 3 theme
├── service/           # Foreground services
├── receiver/          # Broadcast receivers
├── util/              # Utility classes
└── di/                # Hilt modules
```

## Required Permissions

- `SCHEDULE_EXACT_ALARM` - For precise alarm scheduling
- `USE_EXACT_ALARM` - For exact alarm timing
- `POST_NOTIFICATIONS` - For displaying notifications
- `VIBRATE` - For alarm vibration
- `WAKE_LOCK` - For waking device on alarm
- `RECEIVE_BOOT_COMPLETED` - For restoring alarms after reboot
- `FOREGROUND_SERVICE` - For alarm notification service
- `USE_FULL_SCREEN_INTENT` - For full-screen alarm display
- `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` - For reliable background operation

## Building the Project

1. Clone the repository
2. Open the project in Android Studio
3. Sync Gradle files
4. Run the app on an emulator or physical device

## Development Status

This project is currently in development. The basic project structure and dependencies have been set up.

## License

[To be determined]
