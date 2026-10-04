package com.example.habittracker.data

import com.batoulapps.adhan2.CalculationMethod
import com.batoulapps.adhan2.Coordinates
import com.batoulapps.adhan2.Madhab
import com.batoulapps.adhan2.PrayerTimes
import com.batoulapps.adhan2.data.DateComponents
import com.example.habittracker.data.local.entity.AsrMethod
import com.example.habittracker.data.local.entity.Prayer
import com.example.habittracker.data.local.entity.PrayerCalculationMethod
import com.example.habittracker.data.local.entity.PrayerReminderConfig
import com.example.habittracker.data.local.entity.PrayerTimeSettings
import java.time.*
import java.time.format.DateTimeFormatter

data class CalculatedPrayerTime(val prayer: Prayer, val time: ZonedDateTime)
data class NextPrayer(val prayer: Prayer, val time: ZonedDateTime, val tomorrow: Boolean)

@OptIn(kotlin.time.ExperimentalTime::class)
object PrayerTimeRules {
    val reminderOffsets = setOf(0, 5, 10, 15)

    fun calculate(settings: PrayerTimeSettings, date: LocalDate, zone: ZoneId): List<CalculatedPrayerTime> {
        require(settings.latitude in -90.0..90.0 && settings.longitude in -180.0..180.0)
        val parameters = libraryMethod(settings.calculationMethod).parameters.copy(madhab = if (settings.asrMethod == AsrMethod.HANAFI) Madhab.HANAFI else Madhab.SHAFI)
        val times = PrayerTimes(Coordinates(settings.latitude, settings.longitude), DateComponents(date.year, date.monthValue, date.dayOfMonth), parameters)
        fun local(prayer: Prayer, instant: kotlin.time.Instant) = CalculatedPrayerTime(prayer, Instant.ofEpochMilli(instant.toEpochMilliseconds()).atZone(zone))
        return listOf(local(Prayer.FAJR, times.fajr), local(Prayer.DHUHR, times.dhuhr), local(Prayer.ASR, times.asr), local(Prayer.MAGHRIB, times.maghrib), local(Prayer.ISHA, times.isha))
    }

    fun nextPrayer(settings: PrayerTimeSettings, now: ZonedDateTime): NextPrayer {
        calculate(settings, now.toLocalDate(), now.zone).firstOrNull { it.time.isAfter(now) }?.let { return NextPrayer(it.prayer, it.time, false) }
        val tomorrow = calculate(settings, now.toLocalDate().plusDays(1), now.zone).first { it.prayer == Prayer.FAJR }
        return NextPrayer(Prayer.FAJR, tomorrow.time, true)
    }

    fun nextReminder(settings: PrayerTimeSettings, config: PrayerReminderConfig, after: ZonedDateTime): ZonedDateTime? {
        if (!config.enabled || config.offsetMinutes !in reminderOffsets) return null
        for (day in 0..1) calculate(settings, after.toLocalDate().plusDays(day.toLong()), after.zone).first { it.prayer == config.prayer }.time.minusMinutes(config.offsetMinutes.toLong()).let { if (it.isAfter(after)) return it }
        return null
    }

    fun reminderSchedule(settings: PrayerTimeSettings?, configs: List<PrayerReminderConfig>, after: ZonedDateTime): Map<Prayer, ZonedDateTime> {
        if (settings == null) return emptyMap()
        return configs.distinctBy { it.prayer }.mapNotNull { config -> nextReminder(settings, config, after)?.let { config.prayer to it } }.toMap()
    }

    fun formatTime(time: ZonedDateTime): String = time.format(DateTimeFormatter.ofPattern("h:mm a"))
    fun countdown(now: ZonedDateTime, target: ZonedDateTime): String {
        val minutes = Duration.between(now, target).toMinutes().coerceAtLeast(0)
        return when { minutes < 2 -> "Soon"; minutes < 60 -> "In ${minutes}m"; minutes % 60 == 0L -> "In ${minutes / 60}h"; else -> "In ${minutes / 60}h ${minutes % 60}m" }
    }

    private fun libraryMethod(method: PrayerCalculationMethod) = when (method) {
        PrayerCalculationMethod.MUSLIM_WORLD_LEAGUE -> CalculationMethod.MUSLIM_WORLD_LEAGUE
        PrayerCalculationMethod.EGYPTIAN -> CalculationMethod.EGYPTIAN
        PrayerCalculationMethod.KARACHI -> CalculationMethod.KARACHI
        PrayerCalculationMethod.UMM_AL_QURA -> CalculationMethod.UMM_AL_QURA
        PrayerCalculationMethod.DUBAI -> CalculationMethod.DUBAI
        PrayerCalculationMethod.QATAR -> CalculationMethod.QATAR
        PrayerCalculationMethod.KUWAIT -> CalculationMethod.KUWAIT
        PrayerCalculationMethod.MOON_SIGHTING_COMMITTEE -> CalculationMethod.MOON_SIGHTING_COMMITTEE
        PrayerCalculationMethod.SINGAPORE -> CalculationMethod.SINGAPORE
        PrayerCalculationMethod.NORTH_AMERICA -> CalculationMethod.NORTH_AMERICA
        PrayerCalculationMethod.TURKEY -> CalculationMethod.TURKEY
    }
}
