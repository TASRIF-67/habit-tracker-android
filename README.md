# HabitTracker

HabitTracker is a private, local-first Android routine and daily-life tracker with Islamic habits as first-class features. It combines prayer tracking, flexible personal routines, timed activities, sleep planning, history, and local insights in a calm native Android experience.

> Current status: **HabitTracker V2 beta** (`2.0.0-beta.2`)

## Highlights

- A time-aware Today experience for prayer, routines, active activities, and sleep
- Prayer Journey with detailed Salat records and locally calculated prayer times
- Flexible CHECK, DURATION, and COUNT routines with schedules and time blocks
- Local reminders, prominent notifications, and alarm-style alerts
- Foreground timed activity sessions with notification and full-screen controls
- Bedtime planning and manual sleep-session tracking
- Calendar-based Journey history and deterministic local Insights
- First-run onboarding with upgrade-safe handling for existing users
- Warm light theme, forest-toned dark theme, and adaptive/themed launcher icons
- Fully local Room and DataStore persistence for core tracking data

## Screens and Core Experience

### Today

Today brings the most relevant parts of the day into one screen: daily progress, the next prayer, active and upcoming routines, Prayer Journey, Good Deeds, personal routines, and Tonight. Time-aware presentation helps surface current activity blocks without changing routine completion semantics.

### Routines

Create and manage personal routines, choose an icon and color theme, configure tracking targets, and optionally add weekday schedules, single times, flexible time blocks, reminders, or alarm-style alerts. Archived routines remain separate while their recorded history is preserved.

### Journey

Journey provides a calendar-based view of past days and detailed daily records for prayers, routines, timed activities, and sleep. It is designed for reviewing local history rather than editing past data.

### Settings

Settings contains appearance, prayer-time configuration, sleep planning, notification/alarm guidance, routine management entry points, and application information.

### Onboarding

The six-step first-run flow introduces privacy, prayer-time setup, routines, notifications, and sleep. Existing users upgrading from an earlier version are not incorrectly treated as fresh installations.

## Prayer and Islamic Habits

- A connected five-prayer Prayer Journey for Fajr, Dhuhr, Asr, Maghrib, and Isha
- Prayer records for Completed, In Jama'ah, Qaza, Missed, and unrecorded states
- Optional reasons for Qaza or missed records
- Prayer times calculated locally from configured location and calculation settings
- Per-prayer local reminders
- Built-in Good Deeds for Quran, Morning Adhkar, Evening Adhkar, and Sadaqah

Prayer records are presented factually. The app does not assign religious judgment to a user's records.

## Routines and Activity

HabitTracker supports three routine tracking modes:

| Mode | Purpose |
| --- | --- |
| CHECK | Done/not-done routines |
| DURATION | Minute-based goals and timed activity sessions |
| COUNT | Quantity goals with optional secondary measurements |

Routines can be scheduled on selected weekdays at a single time or across a time block. Reminder offsets and prominent or alarm-style delivery can be configured locally.

Duration routines use one authoritative `ActivitySession`. Sessions can be paused, resumed, finished, or discarded from the dedicated Active Session screen. While the app is in the background, an Android foreground service keeps the activity visible and provides direct notification controls. Finishing a session records its eligible duration through the existing routine-progress transaction.

## Sleep

Sleep planning supports a usual bedtime, wake-up time, selected weekdays, wind-down timing, and bedtime reminders. Users can manually begin and finish sleep sessions from Tonight, then review completed sessions in Journey and local Insights.

## Journey and Insights

Journey combines calendar history with daily detail. Insights summarize consistency, routine performance, prayer records, activity, and sleep from the data stored on the device.

Insights are deterministic calculations over the user's own local records. They are not AI-generated and are not produced by a cloud analysis service.

## Privacy and Local-First Design

- No account is required.
- No application backend or cloud database is required for core functionality.
- Core tracking records remain in the local Room database.
- Preferences and onboarding state are stored locally with DataStore.
- The application does not request the Android `INTERNET` permission.
- Location is used only when the user chooses location-based prayer-time setup; configured coordinates remain local to the app.

Opening the developer's LinkedIn profile is delegated to the user's external browser.

## Tech Stack

- Kotlin
- Jetpack Compose
- Material 3
- Navigation Compose
- ViewModel, StateFlow, and Flow
- Room with exported migration schemas
- DataStore Preferences
- Android foreground services, notifications, alarms, and broadcast receivers
- KSP
- Gradle Kotlin DSL and version catalogs
- Adhan2 for local prayer-time calculation
- `java.time` API desugaring

## Architecture

HabitTracker uses a repository-based architecture with manually constructed dependencies:

```text
Compose UI
    ↓
ViewModel / StateFlow
    ↓
Repositories
    ↓
Room / DataStore
```

Local Android services and receivers support active-session notifications, scheduled reminders, alarms, prayer reminders, sleep reminders, and rescheduling after relevant system events. Room flows remain the authoritative source for persisted tracking state.

## Requirements

- Android 7.0 (API 24) or newer
- Android SDK 37 for compilation
- A compatible JDK and Android Studio version for the project's Android Gradle Plugin

The repository includes its Gradle Wrapper; a separate Gradle installation is normally unnecessary.

## Building

Clone the repository:

```bash
git clone https://github.com/TASRIF-67/habit-tracker-android.git
cd habit-tracker-android
```

Build a debug APK on macOS or Linux:

```bash
./gradlew assembleDebug
```

On Windows:

```powershell
.\gradlew.bat assembleDebug
```

The generated debug APK is written under `app/build/outputs/apk/debug/`. Debug builds do not require the private production-signing configuration.

## Release Status

HabitTracker is currently in V2 beta testing. The repository contains source code and build configuration; it does not currently advertise a public GitHub Release APK.

Production signing keys, signing properties, passwords, and generated APK/AAB files are intentionally excluded from source control.

## Developer

Developed by **Md. Imam Hasan**

LinkedIn: [Md. Imam Hasan](https://www.linkedin.com/in/imam-hasan-tasrif-38501b274/)

## License

No open-source license has been added yet.
