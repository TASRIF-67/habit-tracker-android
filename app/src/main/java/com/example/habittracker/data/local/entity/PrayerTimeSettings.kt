package com.example.habittracker.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class PrayerCalculationMethod { MUSLIM_WORLD_LEAGUE, EGYPTIAN, KARACHI, UMM_AL_QURA, DUBAI, QATAR, KUWAIT, MOON_SIGHTING_COMMITTEE, SINGAPORE, NORTH_AMERICA, TURKEY }
enum class AsrMethod { SHAFI, HANAFI }

@Entity(tableName = "prayer_time_settings")
data class PrayerTimeSettings(
    @PrimaryKey val id: Int = 1,
    val locationLabel: String,
    val latitude: Double,
    val longitude: Double,
    val calculationMethod: PrayerCalculationMethod = PrayerCalculationMethod.KARACHI,
    val asrMethod: AsrMethod = AsrMethod.HANAFI,
)

@Entity(tableName = "prayer_reminder_config")
data class PrayerReminderConfig(
    @PrimaryKey val prayer: Prayer,
    val enabled: Boolean = false,
    val offsetMinutes: Int = 0,
)
