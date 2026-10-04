package com.example.habittracker.data

import com.example.habittracker.data.local.dao.PrayerRecordDetails
import com.example.habittracker.data.local.entity.*
import java.time.*
import java.time.format.DateTimeFormatter

data class InsightPeriod(val start: LocalDate, val endInclusive: LocalDate) {
    val days: Int get() = (endInclusive.toEpochDay() - start.toEpochDay() + 1).toInt().coerceAtLeast(0)
    operator fun contains(date: LocalDate) = !date.isBefore(start) && !date.isAfter(endInclusive)
}
data class InsightPeriods(val current: InsightPeriod, val previous: InsightPeriod)
data class WeeklyInsightSummary(val activeDays: Int, val elapsedDays: Int, val consistency: Int, val completeDays: Int)
enum class InsightDomain { CONSISTENCY, ROUTINE, ACTIVITY, PRAYER, SLEEP }
data class LocalInsight(val domain: InsightDomain, val title: String, val primary: String, val secondary: String? = null)

data class InsightInput(
    val habits: List<Habit>,
    val completions: List<HabitCompletion>,
    val prayers: List<PrayerRecordDetails>,
    val progress: List<RoutineProgress>,
    val schedules: List<RoutineSchedule>,
    val activities: List<ActivitySession>,
    val sleep: List<SleepSession>,
)

object InsightRules {
    fun periods(today: LocalDate): InsightPeriods {
        val start = today.minusDays((today.dayOfWeek.value - DayOfWeek.MONDAY.value).toLong())
        val elapsed = (today.toEpochDay() - start.toEpochDay()).toInt()
        val previousStart = start.minusWeeks(1)
        return InsightPeriods(InsightPeriod(start, today), InsightPeriod(previousStart, previousStart.plusDays(elapsed.toLong())))
    }

    fun weeklySummary(input: InsightInput, today: LocalDate, zone: ZoneId = ZoneId.systemDefault()): WeeklyInsightSummary {
        val period = periods(today).current
        val days = dailySummaries(input, period, today, zone)
        val overview = JourneyRules.overview(days)
        return WeeklyInsightSummary(overview.activeDays, period.days, overview.consistency, overview.completeDays)
    }

    fun insights(input: InsightInput, today: LocalDate, zone: ZoneId = ZoneId.systemDefault(), limit: Int = 5): List<LocalInsight> {
        val periods = periods(today)
        val currentDays = dailySummaries(input, periods.current, today, zone)
        val previousDays = dailySummaries(input, periods.previous, today, zone)
        val candidates = mutableListOf<LocalInsight>()

        val currentOverview = JourneyRules.overview(currentDays)
        val previousOverview = JourneyRules.overview(previousDays)
        if (currentDays.any { it.hasData }) {
            candidates += LocalInsight(InsightDomain.CONSISTENCY, "Core consistency", "${currentOverview.consistency}% this week", if (previousDays.any { it.hasData }) "${previousOverview.consistency}% in the equivalent previous period" else null)
        }

        prayerInsight(input.prayers, periods.current)?.let(candidates::add)
        routineInsight(input, periods.current)?.let(candidates::add)
        activityInsight(input, periods)?.let(candidates::add)
        sleepInsight(input.sleep, periods, zone)?.let(candidates::add)
        return candidates.distinctBy { it.domain }.take(limit)
    }

    private fun dailySummaries(input: InsightInput, period: InsightPeriod, today: LocalDate, zone: ZoneId): List<JourneyDaySummary> =
        generateSequence(YearMonth.from(period.start)) { it.plusMonths(1) }.takeWhile { !it.atDay(1).isAfter(period.endInclusive) }
            .flatMap { month -> JourneyRules.days(month, today, input.habits, input.completions, input.prayers, input.progress, input.activities, input.sleep, zone).asSequence() }
            .filter { it.date in period }.toList()

    private fun prayerInsight(records: List<PrayerRecordDetails>, period: InsightPeriod): LocalInsight? {
        val inPeriod = records.filter { LocalDate.parse(it.date) in period && it.status != PrayerStatus.UNRECORDED }
        if (inPeriod.isEmpty()) return null
        val possible = period.days * Prayer.entries.size
        val completed = inPeriod.count { it.status == PrayerStatus.COMPLETED }
        val jamaah = inPeriod.count { it.status == PrayerStatus.COMPLETED && it.inJamaah }
        val qaza = inPeriod.count { it.status == PrayerStatus.QAZA }
        val missed = inPeriod.count { it.status == PrayerStatus.MISSED }
        return LocalInsight(InsightDomain.PRAYER, "Prayer records", "${inPeriod.size} of $possible prayers have a recorded status", "Completed $completed · Jama'ah $jamaah · Qaza $qaza · Missed $missed")
    }

    private fun routineInsight(input: InsightInput, period: InsightPeriod): LocalInsight? {
        val schedules = input.schedules.filter { it.enabled }.associateBy { it.habitId }
        return input.habits.asSequence().filter { it.active && it.category == HabitCategory.PERSONAL }.mapNotNull { habit ->
            val schedule = schedules[habit.id] ?: return@mapNotNull null
            val opportunities = dates(period).count { ScheduleRules.includes(schedule.daysMask, it.dayOfWeek) && existed(habit, it) }
            if (opportunities == 0) return@mapNotNull null
            val achieved = dates(period).count { date ->
                if (!ScheduleRules.includes(schedule.daysMask, date.dayOfWeek) || !existed(habit, date)) false
                else when (habit.routineType) {
                    RoutineType.CHECK -> input.completions.any { it.habitId == habit.id && it.date == date.toString() }
                    else -> input.progress.firstOrNull { it.habitId == habit.id && it.date == date.toString() }?.let { RoutineRules.isComplete(habit.routineType, it.value, habit.target) } == true
                }
            }
            val total = input.progress.filter { it.habitId == habit.id && LocalDate.parse(it.date) in period }.sumOf { it.value }
            val primary = when (habit.routineType) {
                RoutineType.CHECK -> "Completed on $achieved of $opportunities scheduled days"
                RoutineType.DURATION -> "${formatMinutes(total)} recorded this week"
                RoutineType.COUNT -> "Reached the daily target on $achieved of $opportunities scheduled days"
            }
            LocalInsight(InsightDomain.ROUTINE, habit.name, primary, if (habit.routineType == RoutineType.DURATION) "Target reached on $achieved of $opportunities scheduled days" else null) to opportunities
        }.sortedWith(compareByDescending<Pair<LocalInsight, Int>> { it.second }.thenBy { it.first.title.lowercase() }).map { it.first }.firstOrNull()
    }

    private fun activityInsight(input: InsightInput, periods: InsightPeriods): LocalInsight? {
        fun sessions(period: InsightPeriod) = input.activities.filter { it.status == ActivitySessionStatus.FINISHED && LocalDate.parse(it.businessDate) in period && it.accumulatedActiveMillis > 0 }
        val current = sessions(periods.current)
        if (current.isEmpty()) return null
        val byHabit = current.groupBy { it.habitId }.maxByOrNull { (_, rows) -> rows.sumOf { it.accumulatedActiveMillis } } ?: return null
        val total = byHabit.value.sumOf { it.accumulatedActiveMillis }
        val previous = sessions(periods.previous).filter { it.habitId == byHabit.key }.sumOf { it.accumulatedActiveMillis }
        val name = input.habits.firstOrNull { it.id == byHabit.key }?.name ?: "Timed activity"
        val comparison = if (previous > 0) "Equivalent previous period: ${formatDuration(previous)}" else null
        return LocalInsight(InsightDomain.ACTIVITY, name, "${byHabit.value.size} ${if (byHabit.value.size == 1) "session" else "sessions"} · ${formatDuration(total)} total", comparison)
    }

    private fun sleepInsight(sessions: List<SleepSession>, periods: InsightPeriods, zone: ZoneId): LocalInsight? {
        fun completed(period: InsightPeriod) = sessions.filter { it.status == SleepSessionStatus.COMPLETED && it.wokeUpAt != null && LocalDate.parse(it.sleepDate) in period }
        val current = completed(periods.current)
        if (current.isEmpty()) return null
        val average = current.sumOf { it.durationMillis() } / current.size
        val bed = averageClockMinutes(current.map { Instant.ofEpochMilli(it.wentToBedAt).atZone(zone).toLocalTime() }, bedtime = true)
        val wake = averageClockMinutes(current.map { Instant.ofEpochMilli(it.wokeUpAt!!).atZone(zone).toLocalTime() }, bedtime = false)
        val previous = completed(periods.previous)
        val comparison = if (previous.isNotEmpty()) "Previous average: ${formatDuration(previous.sumOf { it.durationMillis() } / previous.size)}" else "Average ${formatClock(bed)} – ${formatClock(wake)}"
        return LocalInsight(InsightDomain.SLEEP, "Recorded sleep", "Average ${formatDuration(average)} · ${current.size} ${if (current.size == 1) "night" else "nights"}", comparison)
    }

    fun averageClockMinutes(times: List<LocalTime>, bedtime: Boolean): Int {
        require(times.isNotEmpty())
        return (times.map { it.hour * 60 + it.minute }.map { if (bedtime && it < 12 * 60) it + 1440 else it }.average().toInt() % 1440)
    }
    private fun dates(period: InsightPeriod) = (0 until period.days).map { period.start.plusDays(it.toLong()) }
    private fun existed(habit: Habit, date: LocalDate, zone: ZoneId = ZoneId.systemDefault()) = habit.createdAt == 0L || !Instant.ofEpochMilli(habit.createdAt).atZone(zone).toLocalDate().isAfter(date)
    private fun formatMinutes(value: Int) = if (value < 60) "$value min" else "${value / 60}h ${value % 60}m"
    private fun formatDuration(value: Long) = formatMinutes((value / 60_000L).toInt())
    private fun formatClock(minutes: Int) = LocalTime.of(minutes / 60, minutes % 60).format(DateTimeFormatter.ofPattern("h:mm a"))
}
