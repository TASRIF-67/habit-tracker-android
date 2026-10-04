package com.example.habittracker

import com.example.habittracker.data.PrayerTimeRules
import com.example.habittracker.data.local.entity.AsrMethod
import com.example.habittracker.data.local.entity.Prayer
import com.example.habittracker.data.local.entity.PrayerReminderConfig
import com.example.habittracker.data.local.entity.PrayerTimeSettings
import com.example.habittracker.data.local.entity.PrayerCalculationMethod
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Assert.*
import org.junit.Test

class PrayerTimeRulesTest {
    private val dhaka = PrayerTimeSettings(locationLabel = "Dhaka", latitude = 23.8103, longitude = 90.4125)
    private val zone = ZoneId.of("Asia/Dhaka")

    @Test fun calculatedPrayersAreChronologicalAndUseRequestedZone() {
        val times = PrayerTimeRules.calculate(dhaka, LocalDate.of(2026, 10, 4), zone)
        assertEquals(Prayer.entries, times.map { it.prayer })
        assertTrue(times.zipWithNext().all { (a, b) -> a.time.isBefore(b.time) })
        assertTrue(times.all { it.time.zone == zone })
    }

    @Test fun afterIshaNextPrayerIsTomorrowFajr() {
        val date = LocalDate.of(2026, 10, 4)
        val isha = PrayerTimeRules.calculate(dhaka, date, zone).last().time
        val next = PrayerTimeRules.nextPrayer(dhaka, isha.plusMinutes(1))
        assertEquals(Prayer.FAJR, next.prayer)
        assertTrue(next.tomorrow)
        assertEquals(date.plusDays(1), next.time.toLocalDate())
    }

    @Test fun reminderOffsetCanCrossToPreviousDate() {
        val date = LocalDate.of(2026, 10, 4)
        val midnight = ZonedDateTime.of(date, java.time.LocalTime.MIDNIGHT, zone)
        val reminder = PrayerTimeRules.nextReminder(dhaka, PrayerReminderConfig(Prayer.FAJR, true, 15), midnight)
        assertNotNull(reminder)
        val fajr = PrayerTimeRules.calculate(dhaka, date, zone).first().time
        assertEquals(fajr.minusMinutes(15), reminder)
    }

    @Test fun hanafiAsrIsLaterThanShafiAsr() {
        val date = LocalDate.of(2026, 10, 4)
        fun asr(method: AsrMethod) = PrayerTimeRules.calculate(dhaka.copy(asrMethod = method), date, zone).first { it.prayer == Prayer.ASR }.time
        assertTrue(asr(AsrMethod.HANAFI).isAfter(asr(AsrMethod.SHAFI)))
    }

    @Test fun nextPrayerFollowsTimeNotCompletionState() {
        val times = PrayerTimeRules.calculate(dhaka, LocalDate.of(2026, 10, 4), zone)
        val now = times.first { it.prayer == Prayer.ASR }.time.plusMinutes(1)
        assertEquals(Prayer.MAGHRIB, PrayerTimeRules.nextPrayer(dhaka, now).prayer)
    }

    @Test fun calculationMethodAndDateChangesRecalculateSchedule() {
        val date = LocalDate.of(2026, 10, 4)
        val karachi = PrayerTimeRules.calculate(dhaka, date, zone)
        val mwl = PrayerTimeRules.calculate(dhaka.copy(calculationMethod = PrayerCalculationMethod.MUSLIM_WORLD_LEAGUE), date, zone)
        val tomorrow = PrayerTimeRules.calculate(dhaka, date.plusDays(1), zone)
        assertNotEquals(karachi.map { it.time }, mwl.map { it.time })
        assertEquals(date.plusDays(1), tomorrow.first().time.toLocalDate())
        assertNotEquals(karachi.first().time, tomorrow.first().time)
    }

    @Test fun reminderPlanIsEmptyWhenUnconfiguredAndUniquePerPrayer() {
        val now = ZonedDateTime.of(LocalDate.of(2026, 10, 4), java.time.LocalTime.MIDNIGHT, zone)
        val duplicate = listOf(PrayerReminderConfig(Prayer.FAJR, true, 0), PrayerReminderConfig(Prayer.FAJR, true, 15))
        assertTrue(PrayerTimeRules.reminderSchedule(null, duplicate, now).isEmpty())
        assertEquals(setOf(Prayer.FAJR), PrayerTimeRules.reminderSchedule(dhaka, duplicate, now).keys)
    }

    @Test fun changingLocationReschedulesReminderTime() {
        val now = ZonedDateTime.of(LocalDate.of(2026, 10, 4), java.time.LocalTime.MIDNIGHT, zone)
        val config = listOf(PrayerReminderConfig(Prayer.FAJR, true, 10))
        val original = PrayerTimeRules.reminderSchedule(dhaka, config, now)[Prayer.FAJR]
        val changed = PrayerTimeRules.reminderSchedule(dhaka.copy(latitude = 24.8949, longitude = 91.8687), config, now)[Prayer.FAJR]
        assertNotEquals(original, changed)
    }
}
