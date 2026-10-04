package com.example.habittracker

import com.example.habittracker.data.*
import com.example.habittracker.data.local.entity.SleepPlan
import com.example.habittracker.data.local.entity.SleepSession
import java.time.*
import org.junit.Assert.*
import org.junit.Test

class SleepRulesTest {
    private val zone = ZoneId.of("Asia/Dhaka")

    @Test fun `planned duration crosses midnight`() {
        assertEquals(450, SleepRules.plannedMinutes(23 * 60 + 30, 7 * 60))
        assertEquals(480, SleepRules.plannedMinutes(2 * 60, 10 * 60))
    }

    @Test fun `weekday mask represents bedtime day`() {
        val monday = ScheduleRules.dayBit(DayOfWeek.MONDAY)
        assertTrue(ScheduleRules.includes(monday, DayOfWeek.MONDAY))
        assertFalse(ScheduleRules.includes(monday, DayOfWeek.TUESDAY))
    }

    @Test fun `next bedtime uses selected local weekday`() {
        val plan = SleepPlan(bedtimeMinutes = 23 * 60 + 30, daysMask = ScheduleRules.dayBit(DayOfWeek.MONDAY))
        val after = ZonedDateTime.of(2026, 10, 4, 20, 0, 0, 0, zone) // Sunday
        assertEquals(ZonedDateTime.of(2026, 10, 5, 23, 30, 0, 0, zone), SleepRules.nextBedtime(plan, after))
    }

    @Test fun `wind down can occur on previous calendar day`() {
        val plan = SleepPlan(bedtimeMinutes = 15, daysMask = ScheduleRules.dayBit(DayOfWeek.MONDAY), windDownOffsetMinutes = 30)
        val after = ZonedDateTime.of(2026, 10, 4, 22, 0, 0, 0, zone)
        assertEquals(ZonedDateTime.of(2026, 10, 4, 23, 45, 0, 0, zone), SleepRules.nextWindDown(plan, after))
    }

    @Test fun `actual duration and invalid interval are deterministic`() {
        assertEquals(27_000_000L, SleepRules.actualDurationMillis(1_000L, 27_001_000L))
        assertNull(SleepRules.actualDurationMillis(2_000L, 1_000L))
    }

    @Test fun `cross midnight session remains attributed to start date`() {
        val start = ZonedDateTime.of(2026, 10, 4, 23, 40, 0, 0, zone).toInstant().toEpochMilli()
        val end = ZonedDateTime.of(2026, 10, 5, 7, 10, 0, 0, zone).toInstant().toEpochMilli()
        val session = SleepSession(sleepDate = "2026-10-04", wentToBedAt = start, wokeUpAt = end, plannedBedtimeMinutes = 23 * 60 + 30, plannedWakeTimeMinutes = 7 * 60)
        assertEquals("2026-10-04", session.sleepDate)
        assertEquals(450L * 60_000L, SleepRules.actualDurationMillis(start, end))
    }

    @Test fun `bedtime deviation uses historical snapshot across midnight`() {
        val actual = ZonedDateTime.of(2026, 10, 5, 0, 10, 0, 0, zone).toInstant().toEpochMilli()
        val session = SleepSession(sleepDate = "2026-10-05", wentToBedAt = actual, plannedBedtimeMinutes = 23 * 60 + 50, plannedWakeTimeMinutes = 7 * 60)
        assertEquals(20, SleepRules.bedtimeDeviationMinutes(session, zone))
    }

    @Test fun `unusually long threshold is over 24 hours`() {
        assertFalse(SleepRules.isUnusuallyLong(0, SleepRules.unusuallyLongMillis))
        assertTrue(SleepRules.isUnusuallyLong(0, SleepRules.unusuallyLongMillis + 1))
    }

    @Test fun `duration formatting is concise`() {
        assertEquals("45 min", SleepRules.formatDuration(45 * 60_000L))
        assertEquals("7h 30m", SleepRules.formatDuration(450 * 60_000L))
        assertEquals("10h", SleepRules.formatDuration(600 * 60_000L))
    }
}
