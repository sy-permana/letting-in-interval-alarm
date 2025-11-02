# Project Structure

## Package Organization

The codebase follows a feature-based package structure under `com.lettingin.intervalAlarm`:

```
app/src/main/java/com/lettingin/intervalAlarm/
├── data/                    # Data layer
│   ├── database/           # Room entities, DAOs, database, converters, migrations
│   ├── model/              # Data models (IntervalAlarm, AlarmState, etc.)
│   └── repository/         # Repository interfaces and implementations
├── domain/                  # Business logic layer
│   └── scheduler/          # Alarm scheduling logic (AlarmScheduler)
├── ui/                      # Presentation layer
│   ├── home/               # Home screen (alarm list, active alarm status)
│   ├── editor/             # Alarm creation/editing screen
│   ├── settings/           # App settings screen
│   ├── statistics/         # Statistics viewing screen
│   ├── alarm/              # Alarm ringing activity
│   ├── components/         # Reusable UI components
│   ├── navigation/         # Navigation setup
│   ├── onboarding/         # Permission onboarding
│   └── theme/              # Material 3 theme (colors, typography, shapes)
├── service/                 # Foreground services (AlarmNotificationService)
├── receiver/                # Broadcast receivers (AlarmReceiver, BootReceiver)
├── util/                    # Utility classes (permissions, notifications, ringtones)
├── backup/                  # Backup and restore functionality
└── di/                      # Hilt dependency injection modules

app/src/main/res/
├── drawable/                # Vector drawables
├── mipmap-*/               # App icons (various densities)
├── raw/                    # Raw resources (ringtone files)
├── values/                 # Strings, themes, colors
└── xml/                    # Backup rules, data extraction rules
```

## Key Architectural Components

### Data Layer
- **Entities**: Room database tables (`@Entity` annotated classes)
- **DAOs**: Database access objects with query methods
- **Repositories**: Abstract data sources, provide clean API to ViewModels
- **Models**: Data classes representing domain objects

### Domain Layer
- **AlarmScheduler**: Core business logic for scheduling alarms with AlarmManager
- Calculates next ring times, handles intervals, manages alarm state

### UI Layer
- **Screens**: Composable functions for each screen
- **ViewModels**: Manage UI state, coordinate between UI and repositories
- Each feature has its own screen and ViewModel pair

### Services & Receivers
- **AlarmNotificationService**: Displays notifications and plays ringtones
- **AlarmReceiver**: Receives alarm events from AlarmManager
- **BootReceiver**: Restores alarms after device reboot

## Naming Conventions

### Files
- **Screens**: `[Feature]Screen.kt` (e.g., `HomeScreen.kt`)
- **ViewModels**: `[Feature]ViewModel.kt` (e.g., `HomeViewModel.kt`)
- **Repositories**: `[Entity]Repository.kt` (interface) and `[Entity]RepositoryImpl.kt` (implementation)
- **DAOs**: `[Entity]Dao.kt` (e.g., `AlarmDao.kt`)
- **Services**: `[Purpose]Service.kt` (e.g., `AlarmNotificationService.kt`)
- **Receivers**: `[Purpose]Receiver.kt` (e.g., `AlarmReceiver.kt`)

### Classes
- Use PascalCase for class names
- Interfaces don't use "I" prefix
- Implementations use "Impl" suffix

### Functions
- Use camelCase for function names
- Composable functions use PascalCase
- Repository methods use verb prefixes: `get`, `insert`, `update`, `delete`

## Database Schema

Room database schemas are version-controlled in `app/schemas/com.lettingin.intervalAlarm.data.database.AppDatabase/`.

Current schema version: 1

### Tables
- `interval_alarms`: Alarm configurations
- `alarm_state`: Current state of active alarm
- `alarm_statistics`: Historical cycle statistics
- `app_settings`: Global app settings

## Resource Organization

- **Strings**: All user-facing text in `res/values/strings.xml`
- **Themes**: Material 3 theme in `res/values/themes.xml`
- **Icons**: Vector drawables in `res/drawable/`
- **Ringtones**: Audio files in `res/raw/`

## Testing Structure

```
app/src/
├── test/                    # Unit tests (JVM)
│   └── java/com/lettingin/intervalAlarm/
│       ├── viewmodel/      # ViewModel tests
│       ├── repository/     # Repository tests
│       └── util/           # Utility tests
└── androidTest/            # Instrumented tests (Android)
    └── java/com/lettingin/intervalAlarm/
        ├── database/       # Database tests
        └── ui/             # UI tests
```

## Documentation

- **Specs**: Detailed requirements and design in `.kiro/specs/letting-in-interval-alarm/`
- **Guides**: Testing and backup documentation in `docs/`
- **Schema README**: Database schema documentation in `app/schemas/README.md`
