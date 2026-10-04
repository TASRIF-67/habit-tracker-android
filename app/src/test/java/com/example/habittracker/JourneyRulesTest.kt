package com.example.habittracker

import com.example.habittracker.data.*
import com.example.habittracker.data.local.dao.PrayerRecordDetails
import com.example.habittracker.data.local.entity.*
import java.time.*
import org.junit.Assert.*
import org.junit.Test

class JourneyRulesTest {
    private val zone = ZoneId.of("UTC")
    private val fajr = Habit(1, "Fajr", HabitCategory.SALAT, createdAt = 0)

    @Test fun `calendar offset and leap month length are correct`() {
        val month = YearMonth.of(2024, 2)
        assertEquals(3, JourneyRules.calendarOffset(month))
        assertEquals(29, JourneyRules.days(month, LocalDate.of(2024, 2, 29), listOf(fajr), emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), zone).size)
    }

    @Test fun `future days are disabled and do not count in overview`() {
        val days = JourneyRules.days(YearMonth.of(2026, 10), LocalDate.of(2026, 10, 4), listOf(fajr), emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), zone)
        assertEquals(JourneyDayLevel.NO_DATA, days[3].level)
        assertEquals(JourneyDayLevel.FUTURE, days[4].level)
        assertEquals(0, JourneyRules.overview(days).activeDays)
    }

    @Test fun `unrecorded prayer is distinct from explicitly missed`() {
        val date = "2026-10-04"
        val missed = PrayerRecordDetails(1, Prayer.FAJR, date, PrayerStatus.MISSED, false, null, 0, 0, null)
        val blank = JourneyRules.days(YearMonth.of(2026, 10), LocalDate.parse(date), listOf(fajr), emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), zone)[3]
        val recorded = JourneyRules.days(YearMonth.of(2026, 10), LocalDate.parse(date), listOf(fajr), emptyList(), listOf(missed), emptyList(), emptyList(), emptyList(), zone)[3]
        assertEquals(JourneyDayLevel.NO_DATA, blank.level)
        assertEquals(JourneyDayLevel.PARTIAL, recorded.level)
        assertEquals(0, recorded.completed)
    }

    @Test fun `activity and sleep summaries use only completed records`() {
        val finished = ActivitySession(1, 7, "2026-10-04", 0, 90_000, null, ActivitySessionStatus.FINISHED, 90_000, null)
        val discarded = finished.copy(id = 2, accumulatedActiveMillis = 50_000, status = ActivitySessionStatus.DISCARDED)
        assertEquals(90_000, JourneyRules.activityTotals(listOf(finished, discarded)).single().millis)
        val completeSleep = SleepSession(1, "2026-10-04", 0, 28_800_000, SleepSessionStatus.COMPLETED, null, 0, 0)
        val activeSleep = completeSleep.copy(id = 2, status = SleepSessionStatus.SLEEPING, wokeUpAt = null, activeSlot = 1)
        val summary = requireNotNull(JourneyRules.sleepSummary(listOf(completeSleep, activeSleep)))
        assertEquals(1, summary.recordedNights)
        assertEquals(28_800_000, summary.averageMillis)
    }
}
