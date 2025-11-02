# Product Overview

"Letting In" is an Android interval alarm application that allows users to create recurring alarms that ring at specified intervals throughout a defined time window.

## Core Concept

Users can create up to 10 saved alarm configurations, with only one active at a time. Each alarm rings repeatedly at customizable intervals (e.g., every 30 minutes) between a start time and end time on selected days of the week.

## Key Features

- **Interval-based alarms**: Ring repeatedly at specified intervals (5 minutes to several hours)
- **Time window scheduling**: Define start and end times for each alarm
- **Day-of-week selection**: Choose which days the alarm should be active
- **Single active alarm**: Only one alarm runs at a time for lightweight operation
- **Flexible notifications**: Full-screen, popup, or sound-only notification types
- **Pause/resume**: Temporarily suspend alarms without deactivating
- **Stop for day**: Cancel remaining rings for the current day
- **Statistics tracking**: View historical data for the last 5 alarm cycles
- **Cycle modes**: One-time cycle through selected days or repeatable weekly schedule

## Target Use Cases

- Regular medication reminders throughout the day
- Interval-based work/break timers (e.g., Pomodoro technique)
- Hydration reminders at consistent intervals
- Posture check reminders during work hours
- Any scenario requiring consistent, repeated reminders within a time window

## Design Philosophy

- **Lightweight**: Minimal battery usage with efficient background scheduling
- **Reliable**: Uses Android AlarmManager for precise timing even in Doze mode
- **Offline-first**: All data stored locally, no network dependencies
- **Simple**: Clean, focused UI following Material Design 3 guidelines
