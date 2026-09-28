# HabitTracker

HabitTracker is a local-first native Android application for tracking daily Islamic habits and personal routines. It was built as a native Android learning project using Kotlin and Jetpack Compose, with a deliberately small and understandable architecture.

**Current version:** 1.0.0

## Features

### Today

- Daily checklist grouped into Salat, good deeds, and personal habits
- Dedicated Salat group containing Fajr, Dhuhr, Asr, Maghrib, and Isha
- Built-in good deeds for Quran, Morning Adhkar, Evening Adhkar, and Sadaqah
- User-created personal habits alongside the built-in habits
- Completed-habit count, percentage, and progress indicator
- Per-section completion count for Salat
- Subtle color and check-state animations when completion changes
- Haptic feedback when a habit is toggled

Each completion belongs to the device's current local calendar date. Completing a habit today does not complete it on another day.

### History

History provides a Material 3 date picker for selecting a previous date. The selected day's habits are grouped by category and shown with their completed or incomplete state plus a daily completion summary.

The history screen is read-only. Today and future dates cannot be selected from it, so reviewing past records cannot alter the current day's checklist.

### Progress

The Progress screen summarizes consistency for the current month:

- Overall monthly completion percentage
- Total habit completions across the elapsed days of the month
- Per-habit completion percentages
- Per-habit current streaks

Statistics are calculated locally from Room completion records. The streak logic counts consecutive completed dates through today, or through yesterday when the habit has not yet been completed today.

### Custom Habits

- Create daily personal habits
- Rename existing personal habits
- Archive habits so they leave the active checklist
- Restore archived habits
- Preserve stored completion history when a habit is archived
- Protect built-in Salat and good-deed habits from editing or archiving

Custom habit names are validated in the UI and limited to 40 characters. All V1 habits are daily; custom schedules are not implemented.

### Appearance

HabitTracker includes three appearance modes:

- System default
- Light
- Dark

The selected mode is stored with DataStore Preferences and restored after the app restarts. The app uses its own restrained green Material 3 color schemes rather than dynamic device colors.

## Screens / Navigation

The app has four main destinations:

1. **Today** — daily checklist and completion summary
2. **History** — read-only review of a selected past date
3. **Progress** — current-month completion statistics and streaks
4. **Settings** — theme selection, personal-habit management, and app information

Navigation Compose hosts the destinations, while a Material 3 bottom navigation bar provides labeled icons and preserves destination state when switching screens.

Screenshots are not included yet because the repository does not currently contain screenshot assets.

## Technology Stack

- **Kotlin** — application and build-configuration language.
- **Jetpack Compose** — declarative implementation of the entire application UI.
- **Material 3** — colors, typography, dialogs, date picker, navigation bar, icons, and other native UI components.
- **Navigation Compose** — navigation between Today, History, Progress, and Settings.
- **ViewModel** — owns screen state and launches data-changing operations in `viewModelScope`.
- **StateFlow and Flow** — expose observable database records and preferences to the UI.
- **Room** — stores habits and date-specific completion records in a local SQLite database.
- **DataStore Preferences** — persists the selected System, Light, or Dark appearance mode.
- **Gradle Kotlin DSL** — configures the Android application and dependency catalog.
- **KSP** — generates Room database implementation code at build time.
- **`java.time` API desugaring** — makes the date APIs used by history and statistics available on the minimum supported Android version.

## Architecture

HabitTracker uses manual dependency creation and a small repository-based architecture. `HabitTrackerApplication` creates the Room database, repository, and theme preferences; `MainActivity` supplies them to `HabitViewModel` through a simple factory. No dependency-injection framework is used.

```text
Compose UI
    ↓
ViewModel / StateFlow
    ↓
Repository
    ↓
Room DAO
    ↓
SQLite

DataStore
    ↓
Application preferences / theme
```

Observed database state flows toward the UI as follows:

```text
Room DAO Flow
→ Repository
→ ViewModel StateFlow
→ lifecycle-aware Compose collection
→ UI recomposition
```

User actions follow the reverse path before producing updated observable state:

```text
User interaction
→ Compose
→ ViewModel
→ Repository
→ Room
→ updated Flow
→ UI recomposition
```

Compose collects the ViewModel flows using `collectAsStateWithLifecycle`, so collection respects the Android lifecycle.

## Project Structure

```text
HabitTracker/
├── app/
│   ├── src/main/
│   │   ├── java/com/example/habittracker/
│   │   │   ├── data/
│   │   │   │   ├── local/
│   │   │   │   │   ├── dao/
│   │   │   │   │   │   └── HabitDao.kt
│   │   │   │   │   ├── entity/
│   │   │   │   │   │   ├── Habit.kt
│   │   │   │   │   │   └── HabitCompletion.kt
│   │   │   │   │   ├── HabitConverters.kt
│   │   │   │   │   └── HabitDatabase.kt
│   │   │   │   └── repository/
│   │   │   │       └── HabitRepository.kt
│   │   │   ├── preferences/
│   │   │   │   └── ThemePreferences.kt
│   │   │   ├── ui/
│   │   │   │   ├── components/
│   │   │   │   │   └── HabitComponents.kt
│   │   │   │   ├── screens/
│   │   │   │   │   ├── today/TodayScreen.kt
│   │   │   │   │   ├── history/HistoryScreen.kt
│   │   │   │   │   ├── progress/ProgressScreen.kt
│   │   │   │   │   └── settings/SettingsScreen.kt
│   │   │   │   ├── theme/
│   │   │   │   │   ├── Color.kt
│   │   │   │   │   ├── Theme.kt
│   │   │   │   │   └── Type.kt
│   │   │   │   └── HabitTrackerApp.kt
│   │   │   ├── viewmodel/
│   │   │   │   └── HabitViewModel.kt
│   │   │   ├── HabitTrackerApplication.kt
│   │   │   └── MainActivity.kt
│   │   ├── res/
│   │   └── AndroidManifest.xml
│   └── build.gradle.kts
├── gradle/
│   ├── libs.versions.toml
│   └── wrapper/
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
├── gradlew
├── gradlew.bat
└── README.md
```

Major responsibilities:

- `data/local` defines Room entities, converters, queries, and database creation, including first-use default data.
- `data/repository` gives the ViewModel a small API for observing and changing habits and completions.
- `preferences` stores and observes the selected theme mode.
- `viewmodel` combines database and preference flows into UI-ready state and calculates monthly statistics.
- `ui/screens` contains the four destination composables.
- `ui/components` contains reusable habit rows, section headers, progress presentation, and habit-icon selection.
- `ui/theme` defines the intentional light and dark Material 3 design.
- `HabitTrackerApp.kt` owns the navigation host and bottom navigation bar.
- `res` contains Android resources, including adaptive and fallback launcher icons.
- `gradle/libs.versions.toml` centralizes plugin and dependency versions.
- `gradle/wrapper` allows the project to build with its declared Gradle version without a separate Gradle installation.

## Data Model

Room database version 1 contains two entities.

### `Habit`

| Field | Type | Purpose |
| --- | --- | --- |
| `id` | `Long` | Auto-generated primary key. |
| `name` | `String` | Display name of the habit. |
| `category` | `HabitCategory` | `SALAT`, `GOOD_DEED`, or `PERSONAL`; stored through a Room type converter. |
| `active` | `Boolean` | Controls whether the habit appears in the active daily checklist. |
| `createdAt` | `Long` | Creation timestamp in epoch milliseconds. |
| `sortOrder` | `Int` | Stable ordering for categories and habits. |
| `isBuiltIn` | `Boolean` | Protects seeded habits from custom-habit edits and archive operations. |

### `HabitCompletion`

| Field | Type | Purpose |
| --- | --- | --- |
| `id` | `Long` | Auto-generated primary key. |
| `habitId` | `Long` | Foreign key referencing `Habit.id`. |
| `date` | `String` | Local calendar date stored in ISO-8601 form, such as `2026-09-29`. |
| `completed` | `Boolean` | Whether that habit is complete for that date. |

`HabitCompletion.habitId` references `Habit.id` with a cascading delete relationship. A unique Room index on `(habitId, date)` prevents multiple completion rows for the same habit on the same date. Toggling a habit on inserts or replaces that date's record; toggling it off removes that record.

This date-specific model means that completing Fajr today has no effect on Fajr tomorrow. Archiving a custom habit changes its `active` flag rather than deleting it, so existing completion rows remain stored.

The database seeds the five Salat and four good-deed habits when the database is first created.

## Local-First / Privacy

- No account is required.
- There is no application backend.
- Firebase is not used.
- There is no cloud database or cloud sync.
- No analytics SDK is included.
- Core tracking functionality has no internet dependency.
- Habit and completion information is stored locally in the Room database on the Android device.
- The theme preference is stored locally with DataStore.
- The manifest does not request Android's internet permission. Opening the developer's LinkedIn link delegates the URL to an external browser.

## Requirements

- Android Studio with support for the project's Android Gradle Plugin
- Android SDK 37 for compilation
- JDK 25, as selected by the repository's Gradle daemon JVM criteria (Android Studio can manage or provision the required runtime)
- An Android device or emulator running Android 7.0/API 24 or newer

The repository includes the Gradle Wrapper for Gradle 9.6, so developers normally do not need to install Gradle manually.

## Building From Source

1. Clone the repository:

   ```bash
   git clone https://github.com/TASRIF-67/habit-tracker-android.git
   cd habit-tracker-android
   ```

2. Open the cloned directory in Android Studio.

3. Allow Android Studio to install any required SDK components and complete Gradle sync.

4. Build the debug variant.

   Windows PowerShell or Command Prompt:

   ```powershell
   .\gradlew.bat assembleDebug
   ```

   macOS or Linux:

   ```bash
   ./gradlew assembleDebug
   ```

5. Find the generated debug APK at:

   ```text
   app/build/outputs/apk/debug/app-debug.apk
   ```

Debug builds do not require private release-signing credentials.

## Running on an Android Device

1. Enable Developer Options on the Android device.
2. Enable USB debugging.
3. Connect the device to the development computer over USB.
4. Accept the device's debugging authorization prompt.
5. Select the device in Android Studio.
6. Run the `app` configuration.

To confirm that Android Debug Bridge can see the device, optionally run:

```bash
adb devices
```

Command-line ADB usage is not required when running the app through Android Studio.

## Release Builds

Public or distribution APKs must be signed with a private Android signing key. Signing keys, passwords, credential property files, and generated release packages must never be committed to the repository.

This README intentionally does not document private signing paths, aliases, credentials, or local release configuration.

## Current V1 Limitations

- Every habit is daily; custom recurring schedules are not supported.
- There are no notifications or reminders.
- Prayer times are not calculated, downloaded, or displayed.
- There is no cloud sync, backup, or export.
- There are no user accounts or authentication.
- The history screen is read-only and only accepts dates before today.
- The current-month Progress screen cannot browse statistics for earlier months.
- Archiving preserves existing completion records, but the V1 schema does not store activation/archive date ranges. Consequently, an archived habit appears on a historical date only when it has a completion record for that date; an archived habit that was missed cannot be reconstructed for that historical checklist.

## Roadmap

Potential future improvements may include:

- Optional notifications and reminders
- Flexible daily or weekly schedules
- Improved history visualization
- Home-screen widgets
- Local backup and export
- Further accessibility review and improvements

These are possible directions, not promised features.

## Developer

Developed by **Md. Imam Hasan**

LinkedIn: [Md. Imam Hasan](https://www.linkedin.com/in/imam-hasan-tasrif-38501b274/)

## License

No open-source license has been added yet.
