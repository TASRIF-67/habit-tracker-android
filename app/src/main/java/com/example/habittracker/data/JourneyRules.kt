package com.example.habittracker.data

import com.example.habittracker.data.local.dao.PrayerRecordDetails
import com.example.habittracker.data.local.entity.*
import java.time.*

enum class JourneyDayLevel { NO_DATA, PARTIAL, COMPLETE, FUTURE }

data class JourneyDaySummary(
    val date: LocalDate,
    val completed: Int,
    val total: Int,
    val hasData: Boolean,
    val level: JourneyDayLevel,
) {
    val percentage get() = if (total == 0) 0 else completed * 100 / total
}

data class JourneyMonthOverview(val consistency: Int, val activeDays: Int, val completeDays: Int)
data class JourneyActivityTotal(val habitId: Long, val millis: Long)
data class JourneySleepSummary(val averageMillis: Long, val recordedNights: Int)

object JourneyRules {
    fun calendarOffset(month: YearMonth) = month.atDay(1).dayOfWeek.value - 1

    fun days(
        month: YearMonth,
        today: LocalDate,
        habits: List<Habit>,
        completions: List<HabitCompletion>,
        prayerRecords: List<PrayerRecordDetails>,
        routineProgress: List<RoutineProgress>,
        activities: List<ActivitySession>,
        sleep: List<SleepSession>,
        zone: ZoneId = ZoneId.systemDefault(),
    ): List<JourneyDaySummary> {
        val completionByDate = completions.groupBy { it.date }
        val prayerByDate = prayerRecords.groupBy { it.date }
        val progressByDate = routineProgress.groupBy { it.date }
        val activityDates = activities.mapTo(mutableSetOf()) { it.businessDate }
        val sleepDates = sleep.mapTo(mutableSetOf()) { it.sleepDate }
        val available = habits.filter { habit ->
            habit.active || completions.any { it.habitId == habit.id } || routineProgress.any { it.habitId == habit.id } || activities.any { it.habitId == habit.id }
        }.filter { HabitPolicy.isPrimary(it.category) }
        return (1..month.lengthOfMonth()).map { day ->
            val date = month.atDay(day)
            if (date.isAfter(today)) return@map JourneyDaySummary(date, 0, 0, false, JourneyDayLevel.FUTURE)
            val dateKey = date.toString()
            val eligible = available.filter { habit ->
                habit.createdAt == 0L || !Instant.ofEpochMilli(habit.createdAt).atZone(zone).toLocalDate().isAfter(date)
            }
            val checks = completionByDate[dateKey].orEmpty().mapTo(mutableSetOf()) { it.habitId }
            val progress = progressByDate[dateKey].orEmpty().associateBy { it.habitId }
            val prayers = prayerByDate[dateKey].orEmpty()
            val completed = eligible.count { habit ->
                if (habit.category == HabitCategory.SALAT) {
                    Prayer.fromHabitName(habit.name)?.let { prayer -> prayers.firstOrNull { it.prayer == prayer }?.status?.countsAsCompleted } == true
                } else when (habit.routineType) {
                    RoutineType.CHECK -> habit.id in checks
                    else -> progress[habit.id]?.let { RoutineRules.isComplete(habit.routineType, it.value, habit.target) } == true
                }
            }
            val hasData = checks.isNotEmpty() || progress.isNotEmpty() || prayers.isNotEmpty() || dateKey in activityDates || dateKey in sleepDates
            val level = when { !hasData -> JourneyDayLevel.NO_DATA; eligible.isNotEmpty() && completed == eligible.size -> JourneyDayLevel.COMPLETE; else -> JourneyDayLevel.PARTIAL }
            JourneyDaySummary(date, completed, eligible.size, hasData, level)
        }
    }

    fun overview(days: List<JourneyDaySummary>): JourneyMonthOverview {
        val elapsed = days.filter { it.level != JourneyDayLevel.FUTURE && it.total > 0 }
        val possible = elapsed.sumOf { it.total }
        val completed = elapsed.sumOf { it.completed }
        return JourneyMonthOverview(if (possible == 0) 0 else completed * 100 / possible, days.count { it.hasData }, days.count { it.level == JourneyDayLevel.COMPLETE })
    }

    fun activityTotals(sessions: List<ActivitySession>) = sessions
        .filter { it.status == ActivitySessionStatus.FINISHED && it.accumulatedActiveMillis > 0 }
        .groupBy { it.habitId }
        .map { JourneyActivityTotal(it.key, it.value.sumOf(ActivitySession::accumulatedActiveMillis)) }
        .sortedByDescending { it.millis }

    fun sleepSummary(sessions: List<SleepSession>): JourneySleepSummary? {
        val completed = sessions.filter { it.status == SleepSessionStatus.COMPLETED && it.wokeUpAt != null }
        if (completed.isEmpty()) return null
        return JourneySleepSummary(completed.sumOf { it.durationMillis() } / completed.size, completed.size)
    }
}
