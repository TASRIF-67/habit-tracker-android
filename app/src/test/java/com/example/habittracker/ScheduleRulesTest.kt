package com.example.habittracker

import com.example.habittracker.data.ScheduleRules
import com.example.habittracker.data.ScheduleDraft
import com.example.habittracker.data.RoutineWindowState
import com.example.habittracker.data.EVERY_DAY
import com.example.habittracker.data.local.entity.RoutineSchedule
import com.example.habittracker.data.local.dao.HabitWithStatus
import com.example.habittracker.data.local.entity.HabitCategory
import com.example.habittracker.data.local.entity.RoutineType
import com.example.habittracker.data.local.entity.ReminderStyle
import java.time.*
import org.junit.Assert.*
import org.junit.Test

class ScheduleRulesTest {
    private val zone = ZoneId.of("Asia/Dhaka")
    private fun schedule(days: Int, hour: Int, minute: Int, reminder: Boolean = true, offset: Int = 0, enabled: Boolean = true, end: Int? = null) = RoutineSchedule(1, 7, enabled, hour * 60 + minute, days, reminder, offset, endTimeMinutes = end)

    @Test fun existingSchedulesDefaultToProminentStyle() {
        assertEquals(ReminderStyle.PROMINENT, schedule(127, 9, 0).reminderStyle)
        assertNull(schedule(127, 9, 0).endTimeMinutes)
    }

    @Test fun weekdayBitsMapMondayThroughSundayIndependently() {
        assertEquals(1, ScheduleRules.dayBit(DayOfWeek.MONDAY))
        assertEquals(2, ScheduleRules.dayBit(DayOfWeek.TUESDAY))
        assertEquals(4, ScheduleRules.dayBit(DayOfWeek.WEDNESDAY))
        assertEquals(8, ScheduleRules.dayBit(DayOfWeek.THURSDAY))
        assertEquals(16, ScheduleRules.dayBit(DayOfWeek.FRIDAY))
        assertEquals(32, ScheduleRules.dayBit(DayOfWeek.SATURDAY))
        assertEquals(64, ScheduleRules.dayBit(DayOfWeek.SUNDAY))
        assertEquals(EVERY_DAY, DayOfWeek.entries.fold(0) { mask, day -> ScheduleRules.withDaySelected(mask, day, true) })
    }

    @Test fun weekendSelectionsRemainDistinctAndCanCoexist() {
        val saturday = ScheduleRules.withDaySelected(0, DayOfWeek.SATURDAY, true)
        val sunday = ScheduleRules.withDaySelected(0, DayOfWeek.SUNDAY, true)
        val weekend = ScheduleRules.withDaySelected(saturday, DayOfWeek.SUNDAY, true)
        assertEquals(32, saturday)
        assertEquals(64, sunday)
        assertEquals(96, weekend)
        assertTrue(ScheduleRules.includes(weekend, DayOfWeek.SATURDAY))
        assertTrue(ScheduleRules.includes(weekend, DayOfWeek.SUNDAY))
    }

    @Test fun togglingOneDayDoesNotChangeOtherSelections() {
        val original = ScheduleRules.dayBit(DayOfWeek.MONDAY) or ScheduleRules.dayBit(DayOfWeek.WEDNESDAY) or ScheduleRules.dayBit(DayOfWeek.SUNDAY)
        val withFriday = ScheduleRules.withDaySelected(original, DayOfWeek.FRIDAY, true)
        val withoutWednesday = ScheduleRules.withDaySelected(withFriday, DayOfWeek.WEDNESDAY, false)
        assertEquals(setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY, DayOfWeek.SUNDAY), DayOfWeek.entries.filter { ScheduleRules.includes(withFriday, it) }.toSet())
        assertEquals(setOf(DayOfWeek.MONDAY, DayOfWeek.FRIDAY, DayOfWeek.SUNDAY), DayOfWeek.entries.filter { ScheduleRules.includes(withoutWednesday, it) }.toSet())
    }

    @Test fun nextOccurrenceUsesSameDayThenNextSelectedDay() {
        val mondayWednesday = ScheduleRules.dayBit(DayOfWeek.MONDAY) or ScheduleRules.dayBit(DayOfWeek.WEDNESDAY)
        val monday = ZonedDateTime.of(2026, 10, 5, 16, 0, 0, 0, zone)
        assertEquals(DayOfWeek.MONDAY, ScheduleRules.nextOccurrence(schedule(mondayWednesday, 17, 30), monday)?.dayOfWeek)
        assertEquals(DayOfWeek.WEDNESDAY, ScheduleRules.nextOccurrence(schedule(mondayWednesday, 17, 30), monday.withHour(19))?.dayOfWeek)
    }

    @Test fun nextOccurrenceWrapsAcrossWeek() {
        val friday = ZonedDateTime.of(2026, 10, 9, 20, 0, 0, 0, zone)
        val next = ScheduleRules.nextOccurrence(schedule(ScheduleRules.dayBit(DayOfWeek.MONDAY), 8, 0), friday)
        assertEquals(LocalDate.of(2026, 10, 12), next?.toLocalDate())
    }

    @Test fun reminderCanCrossMidnight() {
        val monday = ScheduleRules.dayBit(DayOfWeek.MONDAY)
        val sunday = ZonedDateTime.of(2026, 10, 4, 22, 0, 0, 0, zone)
        val reminder = ScheduleRules.nextReminder(schedule(monday, 0, 15, offset = 30), sunday)
        assertEquals(ZonedDateTime.of(2026, 10, 4, 23, 45, 0, 0, zone), reminder)
    }

    @Test fun disabledScheduleOrReminderHasNoOccurrence() {
        val now = ZonedDateTime.now(zone)
        assertNull(ScheduleRules.nextOccurrence(schedule(127, 9, 0, enabled = false), now))
        assertNull(ScheduleRules.nextReminder(schedule(127, 9, 0, reminder = false), now))
    }

    @Test fun upNextExcludesCompletedAndArchivedRoutines() {
        val now = ZonedDateTime.of(2026, 10, 5, 8, 0, 0, 0, zone)
        fun habit(id: Long, active: Boolean, completed: Boolean) = HabitWithStatus(id, "Routine $id", HabitCategory.PERSONAL, active, 0, 0, false, RoutineType.CHECK, null, null, 0, completed)
        val schedules = listOf(schedule(127, 9, 0).copy(habitId = 1), schedule(127, 10, 0).copy(habitId = 2), schedule(127, 11, 0).copy(habitId = 3))
        assertEquals(3L, ScheduleRules.upNext(listOf(habit(1, true, true), habit(2, false, false), habit(3, true, false)), schedules, now)?.habit?.id)
    }

    @Test fun sameDayBlockResolvesBeforeCurrentAndAfter() {
        val monday = LocalDate.of(2026, 10, 5)
        val block = schedule(ScheduleRules.dayBit(DayOfWeek.MONDAY), 19, 0, end = 20 * 60 + 30)
        assertEquals(RoutineWindowState.UPCOMING, ScheduleRules.windowState(block, monday, ZonedDateTime.of(2026, 10, 5, 18, 0, 0, 0, zone)))
        assertEquals(RoutineWindowState.CURRENT, ScheduleRules.windowState(block, monday, ZonedDateTime.of(2026, 10, 5, 19, 30, 0, 0, zone)))
        assertEquals(RoutineWindowState.PAST, ScheduleRules.windowState(block, monday, ZonedDateTime.of(2026, 10, 5, 21, 0, 0, 0, zone)))
    }

    @Test fun zeroDurationBlockIsRejected() {
        assertFalse(ScheduleRules.isValid(ScheduleDraft(true, 19 * 60, EVERY_DAY, endTimeMinutes = 19 * 60)))
    }

    @Test fun overnightBlockBelongsToStartWeekday() {
        val sundayMask = ScheduleRules.dayBit(DayOfWeek.SUNDAY)
        val block = schedule(sundayMask, 23, 0, end = 60)
        val monday = ZonedDateTime.of(2026, 10, 5, 0, 30, 0, 0, zone)
        val window = ScheduleRules.currentWindow(block, monday)
        assertEquals(LocalDate.of(2026, 10, 4), window?.start?.toLocalDate())
        assertEquals(LocalDate.of(2026, 10, 5), window?.end?.toLocalDate())
    }

    @Test fun overlappingBlocksOrderByEarliestEndThenStart() {
        fun habit(id: Long) = HabitWithStatus(id, "Routine $id", HabitCategory.PERSONAL, true, 0, id.toInt(), false, RoutineType.DURATION, 30, null, 0, false)
        val now = ZonedDateTime.of(2026, 10, 5, 20, 5, 0, 0, zone)
        val schedules = listOf(schedule(EVERY_DAY, 19, 0, end = 21 * 60).copy(habitId = 1), schedule(EVERY_DAY, 20, 0, end = 20 * 60 + 15).copy(habitId = 2))
        assertEquals(listOf(2L, 1L), ScheduleRules.activeNow(listOf(habit(1), habit(2)), schedules, now).map { it.habit.id })
    }

    @Test fun reminderForBlockRemainsAnchoredToStart() {
        val monday = ScheduleRules.dayBit(DayOfWeek.MONDAY)
        val before = ZonedDateTime.of(2026, 10, 5, 17, 0, 0, 0, zone)
        val block = schedule(monday, 19, 0, offset = 10, end = 20 * 60 + 30)
        assertEquals(ZonedDateTime.of(2026, 10, 5, 18, 50, 0, 0, zone), ScheduleRules.nextReminder(block, before))
    }

    @Test fun upNextOrdersBlocksByStartNotEnd() {
        fun habit(id: Long) = HabitWithStatus(id, "Routine $id", HabitCategory.PERSONAL, true, 0, id.toInt(), false, RoutineType.DURATION, 30, null, 0, false)
        val now = ZonedDateTime.of(2026, 10, 5, 8, 0, 0, 0, zone)
        val earlierLongBlock = schedule(EVERY_DAY, 9, 0, end = 12 * 60).copy(habitId = 1)
        val laterShortBlock = schedule(EVERY_DAY, 10, 0, end = 10 * 60 + 15).copy(habitId = 2)
        assertEquals(1L, ScheduleRules.upNext(listOf(habit(1), habit(2)), listOf(laterShortBlock, earlierLongBlock), now)?.habit?.id)
    }

    @Test fun upNextDoesNotRepeatRoutineWhoseBlockIsCurrent() {
        fun habit(id: Long) = HabitWithStatus(id, "Routine $id", HabitCategory.PERSONAL, true, 0, id.toInt(), false, RoutineType.DURATION, 30, null, 0, false)
        val now = ZonedDateTime.of(2026, 10, 5, 9, 30, 0, 0, zone)
        val current = schedule(EVERY_DAY, 9, 0, end = 10 * 60).copy(habitId = 1)
        val later = schedule(EVERY_DAY, 11, 0, end = 12 * 60).copy(habitId = 2)
        assertEquals(2L, ScheduleRules.upNext(listOf(habit(1), habit(2)), listOf(current, later), now)?.habit?.id)
    }
}
