package com.example.habittracker

import com.example.habittracker.data.*
import com.example.habittracker.data.local.dao.PrayerRecordDetails
import com.example.habittracker.data.local.entity.*
import java.time.*
import org.junit.Assert.*
import org.junit.Test

class InsightRulesTest {
    private val monday = LocalDate.of(2026, 10, 5)
    private val thursday = LocalDate.of(2026, 10, 8)
    private val zone = ZoneId.of("UTC")
    private fun habit(id: Long, type: RoutineType, target: Int? = null) = Habit(id, "Routine $id", HabitCategory.PERSONAL, createdAt = 0, routineType = type, target = target, unit = if (type == RoutineType.DURATION) "min" else "items")
    private fun schedule(id: Long, mask: Int = EVERY_DAY) = RoutineSchedule(id, id, true, 9 * 60, mask, false, 0)
    private fun input(habits: List<Habit> = emptyList(), completions: List<HabitCompletion> = emptyList(), prayers: List<PrayerRecordDetails> = emptyList(), progress: List<RoutineProgress> = emptyList(), schedules: List<RoutineSchedule> = emptyList(), activities: List<ActivitySession> = emptyList(), sleep: List<SleepSession> = emptyList()) = InsightInput(habits, completions, prayers, progress, schedules, activities, sleep)

    @Test fun `current and previous periods use equivalent elapsed Monday based days`() {
        val periods = InsightRules.periods(thursday)
        assertEquals(InsightPeriod(monday, thursday), periods.current)
        assertEquals(InsightPeriod(LocalDate.of(2026, 9, 28), LocalDate.of(2026, 10, 1)), periods.previous)
        assertEquals(4, periods.current.days)
        assertEquals(4, periods.previous.days)
    }

    @Test fun `check routine counts only selected scheduled days`() {
        val h = habit(1, RoutineType.CHECK)
        val mask = ScheduleRules.dayBit(DayOfWeek.MONDAY) or ScheduleRules.dayBit(DayOfWeek.WEDNESDAY)
        val result = InsightRules.insights(input(listOf(h), listOf(HabitCompletion(habitId = 1, date = monday.toString())), schedules = listOf(schedule(1, mask))), thursday, zone)
        assertEquals("Completed on 1 of 2 scheduled days", result.single { it.domain == InsightDomain.ROUTINE }.primary)
    }

    @Test fun `duration totals and count target achievements retain existing semantics`() {
        val duration = habit(1, RoutineType.DURATION, 60)
        val durationResult = InsightRules.insights(input(listOf(duration), progress = listOf(RoutineProgress(habitId = 1, date = monday.toString(), value = 75)), schedules = listOf(schedule(1))), thursday, zone)
        assertEquals("1h 15m recorded this week", durationResult.single { it.domain == InsightDomain.ROUTINE }.primary)
        val count = habit(2, RoutineType.COUNT, 4)
        val countResult = InsightRules.insights(input(listOf(count), progress = listOf(RoutineProgress(habitId = 2, date = monday.toString(), value = 4), RoutineProgress(habitId = 2, date = monday.plusDays(1).toString(), value = 3)), schedules = listOf(schedule(2))), thursday, zone)
        assertEquals("Reached the daily target on 1 of 4 scheduled days", countResult.single { it.domain == InsightDomain.ROUTINE }.primary)
    }

    @Test fun `routine without scheduled opportunities is suppressed`() {
        val h = habit(1, RoutineType.CHECK)
        val sundayOnly = ScheduleRules.dayBit(DayOfWeek.SUNDAY)
        assertFalse(InsightRules.insights(input(listOf(h), schedules = listOf(schedule(1, sundayOnly))), thursday, zone).any { it.domain == InsightDomain.ROUTINE })
    }

    @Test fun `prayer coverage counts records and never infers unrecorded as missed`() {
        val completed = PrayerRecordDetails(1, Prayer.FAJR, monday.toString(), PrayerStatus.COMPLETED, false, null, 0, 0, null)
        val missed = PrayerRecordDetails(2, Prayer.DHUHR, monday.toString(), PrayerStatus.MISSED, false, null, 0, 0, null)
        val unrecorded = PrayerRecordDetails(3, Prayer.ASR, monday.toString(), PrayerStatus.UNRECORDED, false, null, 0, 0, null)
        val insight = InsightRules.insights(input(prayers = listOf(completed, missed, unrecorded)), thursday, zone).single { it.domain == InsightDomain.PRAYER }
        assertEquals("2 of 20 prayers have a recorded status", insight.primary)
        assertTrue(insight.secondary!!.contains("Missed 1"))
    }

    @Test fun `finished activity totals exclude discarded sessions`() {
        val h = habit(1, RoutineType.DURATION, 30)
        val finished = ActivitySession(1, 1, monday.toString(), 0, 3_600_000, null, ActivitySessionStatus.FINISHED, 0, null)
        val discarded = finished.copy(id = 2, accumulatedActiveMillis = 9_000_000, status = ActivitySessionStatus.DISCARDED)
        val insight = InsightRules.insights(input(listOf(h), activities = listOf(finished, discarded)), thursday, zone).single { it.domain == InsightDomain.ACTIVITY }
        assertEquals("1 session · 1h 0m total", insight.primary)
    }

    @Test fun `sleep averages completed nights and handles bedtime across midnight`() {
        fun epoch(date: LocalDate, hour: Int, minute: Int) = ZonedDateTime.of(date, LocalTime.of(hour, minute), zone).toInstant().toEpochMilli()
        val first = SleepSession(1, monday.toString(), epoch(monday, 23, 30), epoch(monday.plusDays(1), 7, 30), SleepSessionStatus.COMPLETED, null, 0, 0)
        val second = SleepSession(2, monday.plusDays(1).toString(), epoch(monday.plusDays(1), 0, 30), epoch(monday.plusDays(1), 8, 30), SleepSessionStatus.COMPLETED, null, 0, 0)
        val insight = InsightRules.insights(input(sleep = listOf(first, second)), thursday, zone).single { it.domain == InsightDomain.SLEEP }
        assertEquals("Average 8h 0m · 2 nights", insight.primary)
        assertTrue(insight.secondary!!.contains("12:00 AM"))
        assertEquals(0, InsightRules.averageClockMinutes(listOf(LocalTime.of(23, 30), LocalTime.of(0, 30)), true))
    }

    @Test fun `insufficient inputs yield no manufactured insights and priority is stable`() {
        assertTrue(InsightRules.insights(input(), thursday, zone).isEmpty())
        val prayer = PrayerRecordDetails(1, Prayer.FAJR, monday.toString(), PrayerStatus.COMPLETED, false, null, 0, 0, null)
        val sleep = SleepSession(1, monday.toString(), 0, 3_600_000, SleepSessionStatus.COMPLETED, null, 0, 0)
        val domains = InsightRules.insights(input(prayers = listOf(prayer), sleep = listOf(sleep)), thursday, zone).map { it.domain }
        assertEquals(listOf(InsightDomain.CONSISTENCY, InsightDomain.PRAYER, InsightDomain.SLEEP), domains)
    }
}
