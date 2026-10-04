package com.example.habittracker.data

import com.example.habittracker.data.local.entity.RoutineSchedule
import com.example.habittracker.data.local.dao.HabitWithStatus
import java.time.*
import com.example.habittracker.data.local.entity.ReminderStyle

data class ScheduleDraft(val enabled: Boolean = false, val timeMinutes: Int = 9 * 60, val daysMask: Int = EVERY_DAY, val reminderEnabled: Boolean = false, val reminderOffsetMinutes: Int = 0, val reminderStyle: ReminderStyle = ReminderStyle.PROMINENT, val endTimeMinutes: Int? = null)

const val EVERY_DAY = 0b1111111
val SUPPORTED_REMINDER_OFFSETS = setOf(0, 5, 10, 15, 30, 60)

object ScheduleRules {
    fun dayBit(day: DayOfWeek) = 1 shl (day.value - 1)
    fun includes(mask: Int, day: DayOfWeek) = mask and dayBit(day) != 0
    fun withDaySelected(mask: Int, day: DayOfWeek, selected: Boolean): Int =
        if (selected) mask or dayBit(day) else mask and dayBit(day).inv()
    fun isValid(draft: ScheduleDraft) = !draft.enabled || (draft.daysMask in 1..EVERY_DAY && draft.timeMinutes in 0..1439 && (draft.endTimeMinutes == null || draft.endTimeMinutes in 0..1439 && draft.endTimeMinutes != draft.timeMinutes) && (!draft.reminderEnabled || draft.reminderOffsetMinutes in SUPPORTED_REMINDER_OFFSETS))

    fun window(schedule: RoutineSchedule, startDate: LocalDate, zone: ZoneId): RoutineWindow? {
        val endMinutes = schedule.endTimeMinutes ?: return null
        if (!schedule.enabled || endMinutes !in 0..1439 || endMinutes == schedule.timeMinutes || !includes(schedule.daysMask, startDate.dayOfWeek)) return null
        val start = ZonedDateTime.of(startDate, LocalTime.of(schedule.timeMinutes / 60, schedule.timeMinutes % 60), zone)
        var end = ZonedDateTime.of(startDate, LocalTime.of(endMinutes / 60, endMinutes % 60), zone)
        if (!end.isAfter(start)) end = end.plusDays(1)
        return RoutineWindow(start, end)
    }

    fun currentWindow(schedule: RoutineSchedule, now: ZonedDateTime): RoutineWindow? =
        listOf(now.toLocalDate(), now.toLocalDate().minusDays(1)).mapNotNull { window(schedule, it, now.zone) }.firstOrNull { !now.isBefore(it.start) && now.isBefore(it.end) }

    fun windowState(schedule: RoutineSchedule, startDate: LocalDate, now: ZonedDateTime): RoutineWindowState? = window(schedule, startDate, now.zone)?.let {
        when { now.isBefore(it.start) -> RoutineWindowState.UPCOMING; now.isBefore(it.end) -> RoutineWindowState.CURRENT; else -> RoutineWindowState.PAST }
    }

    fun activeNow(habits: List<HabitWithStatus>, schedules: List<RoutineSchedule>, now: ZonedDateTime): List<ActiveRoutineWindow> {
        val scheduleByHabit = schedules.associateBy(RoutineSchedule::habitId)
        return habits.asSequence().filter { it.active && !it.completed }.mapNotNull { habit ->
            scheduleByHabit[habit.id]?.let { schedule -> currentWindow(schedule, now)?.let { ActiveRoutineWindow(habit, schedule, it) } }
        }.sortedWith(compareBy<ActiveRoutineWindow> { it.window.end }.thenBy { it.window.start }.thenBy { it.habit.sortOrder }.thenBy { it.habit.id }).toList()
    }

    fun nextOccurrence(schedule: RoutineSchedule, after: ZonedDateTime): ZonedDateTime? {
        if (!schedule.enabled || schedule.daysMask !in 1..EVERY_DAY || schedule.timeMinutes !in 0..1439) return null
        val time = LocalTime.of(schedule.timeMinutes / 60, schedule.timeMinutes % 60)
        for (offset in 0..7) {
            val date = after.toLocalDate().plusDays(offset.toLong())
            if (!includes(schedule.daysMask, date.dayOfWeek)) continue
            val candidate = ZonedDateTime.of(date, time, after.zone)
            if (candidate.isAfter(after)) return candidate
        }
        return null
    }

    fun nextReminder(schedule: RoutineSchedule, after: ZonedDateTime): ZonedDateTime? {
        if (!schedule.enabled || !schedule.reminderEnabled || schedule.reminderOffsetMinutes !in SUPPORTED_REMINDER_OFFSETS) return null
        val time = LocalTime.of(schedule.timeMinutes / 60, schedule.timeMinutes % 60)
        for (offset in 0..8) {
            val date = after.toLocalDate().plusDays(offset.toLong())
            if (!includes(schedule.daysMask, date.dayOfWeek)) continue
            val reminder = ZonedDateTime.of(date, time, after.zone).minusMinutes(schedule.reminderOffsetMinutes.toLong())
            if (reminder.isAfter(after)) return reminder
        }
        return null
    }

    fun summary(schedule: RoutineSchedule): String {
        if (!schedule.enabled) return "No schedule"
        val days = if (schedule.daysMask == EVERY_DAY) "Every day" else DayOfWeek.entries.filter { includes(schedule.daysMask, it) }.joinToString(" · ") { it.name.take(3).lowercase().replaceFirstChar(Char::titlecase) }
        return "$days · ${timeRange(schedule)}"
    }

    fun reminderSummary(schedule: RoutineSchedule) = when { !schedule.reminderEnabled -> null; schedule.reminderOffsetMinutes == 0 -> "Reminder at scheduled time"; schedule.reminderOffsetMinutes == 60 -> "Reminder 1 hour before"; else -> "Reminder ${schedule.reminderOffsetMinutes} min before" }
    fun formatTime(minutes: Int): String = LocalTime.of(minutes / 60, minutes % 60).format(java.time.format.DateTimeFormatter.ofPattern("h:mm a"))
    fun timeRange(schedule: RoutineSchedule): String = schedule.endTimeMinutes?.let { "${formatTime(schedule.timeMinutes)} – ${formatTime(it)}" } ?: formatTime(schedule.timeMinutes)

    fun upNext(habits: List<HabitWithStatus>, schedules: List<RoutineSchedule>, now: ZonedDateTime): UpcomingRoutine? {
        val scheduleByHabit = schedules.associateBy(RoutineSchedule::habitId)
        val currentBlockHabitIds = activeNow(habits, schedules, now).mapTo(mutableSetOf()) { it.habit.id }
        return habits.asSequence().filter { it.active && !it.completed && it.id !in currentBlockHabitIds }.mapNotNull { habit ->
            scheduleByHabit[habit.id]?.let { schedule -> nextOccurrence(schedule, now)?.let { UpcomingRoutine(habit, schedule, it) } }
        }.minByOrNull(UpcomingRoutine::occurrence)
    }
}

data class UpcomingRoutine(val habit: HabitWithStatus, val schedule: RoutineSchedule, val occurrence: ZonedDateTime)
data class RoutineWindow(val start: ZonedDateTime, val end: ZonedDateTime)
data class ActiveRoutineWindow(val habit: HabitWithStatus, val schedule: RoutineSchedule, val window: RoutineWindow)
enum class RoutineWindowState { UPCOMING, CURRENT, PAST }
