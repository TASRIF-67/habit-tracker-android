package com.example.habittracker.ui.screens.journey

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.habittracker.data.*
import com.example.habittracker.data.local.entity.*
import com.example.habittracker.ui.components.AppProgress
import com.example.habittracker.ui.components.routineIcon
import com.example.habittracker.ui.components.routinePalette
import com.example.habittracker.viewmodel.JourneyState
import com.example.habittracker.viewmodel.ProgressState
import com.example.habittracker.viewmodel.prayerStats
import java.time.*
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun JourneyScreen(
    state: JourneyState,
    sleepSessions: List<SleepSession>,
    insightInput: InsightInput,
    today: LocalDate,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onOpenDay: (LocalDate) -> Unit,
) {
    val days = JourneyRules.days(state.month, today, state.habits, state.completions, state.prayerRecords, state.routineProgress, state.activities, sleepSessions)
    val overview = JourneyRules.overview(days)
    val hasRecords = days.any { it.hasData }
    val progressState = ProgressState(state.habits, state.completions, state.prayerRecords, state.routineProgress, state.month)
    val weekly = InsightRules.weeklySummary(insightInput, today)
    val insights = InsightRules.insights(insightInput, today)
    val hasRecentRecords = insightInput.completions.isNotEmpty() || insightInput.prayers.isNotEmpty() || insightInput.progress.isNotEmpty() || insightInput.activities.isNotEmpty() || insightInput.sleep.isNotEmpty()
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 22.dp, bottom = 32.dp)) {
        item {
            Text("Journey", style = MaterialTheme.typography.headlineMedium)
            Text("A calm view of your days and routines", color = MaterialTheme.colorScheme.onSurfaceVariant)
            MonthHeader(state.month, state.month < YearMonth.now(), onPreviousMonth, onNextMonth)
        }
        if (!hasRecords) {
            item { EmptyJourney(state.month == YearMonth.now()) }
            if (hasRecentRecords) { item { WeeklySummary(weekly) }; item { InsightsSection(insights) } }
        } else {
            item { MonthlyOverview(overview) }
            item { SectionTitle("Monthly calendar", "Core daily completion") }
            item { JourneyCalendar(state.month, days, today, onOpenDay) }
            if (hasRecentRecords) { item { WeeklySummary(weekly) }; item { InsightsSection(insights) } }
            if (state.prayerRecords.isNotEmpty()) item { PrayerSummary(progressState) }
            val routines = progressState.availableHabits.filter { it.category == HabitCategory.PERSONAL }
            if (routines.isNotEmpty()) item { RoutineSummary(routines, progressState) }
            val activities = JourneyRules.activityTotals(state.activities)
            if (activities.isNotEmpty()) item { ActivitySummary(activities, state.habits) }
            JourneyRules.sleepSummary(sleepSessions)?.let { summary -> item { SleepSummary(summary) } }
            val recent = days.filter { it.hasData && !it.date.isAfter(today) }.takeLast(5).reversed()
            if (recent.isNotEmpty()) item { RecentDays(recent, today, onOpenDay) }
        }
    }
}

@Composable private fun WeeklySummary(summary: WeeklyInsightSummary) {
    SectionTitle("This week", "Monday through today")
    Surface(Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .45f), shape = MaterialTheme.shapes.large) {
        Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Metric("${summary.activeDays} of ${summary.elapsedDays}", "Active days")
            Metric("${summary.consistency}%", "Core consistency")
            Metric(summary.completeDays.toString(), "Complete days")
        }
    }
}

@Composable private fun InsightsSection(insights: List<LocalInsight>) {
    SectionTitle("Insights", "Local patterns from your recent records")
    if (insights.isEmpty()) {
        Text("Keep tracking to see patterns here.", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 8.dp))
        return
    }
    Surface(Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .32f), shape = MaterialTheme.shapes.large) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
            insights.forEachIndexed { index, insight ->
                Column(Modifier.fillMaxWidth().padding(vertical = 12.dp).semantics { contentDescription = "${insight.title}. ${insight.primary}. ${insight.secondary.orEmpty()}" }) {
                    Text(insight.title, style = MaterialTheme.typography.titleSmall)
                    Text(insight.primary, Modifier.padding(top = 2.dp), style = MaterialTheme.typography.bodyMedium)
                    insight.secondary?.let { Text(it, Modifier.padding(top = 2.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                }
                if (index < insights.lastIndex) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = .55f))
            }
        }
    }
}

@Composable private fun MonthHeader(month: YearMonth, canGoForward: Boolean, previous: () -> Unit, next: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(top = 24.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(month.format(DateTimeFormatter.ofPattern("MMMM yyyy")), style = MaterialTheme.typography.titleLarge)
            Text("Your journey this month", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        IconButton(previous) { Icon(Icons.Outlined.ChevronLeft, "Previous month") }
        IconButton(next, enabled = canGoForward) { Icon(Icons.Outlined.ChevronRight, "Next month") }
    }
}

@Composable private fun MonthlyOverview(value: JourneyMonthOverview) {
    Surface(Modifier.fillMaxWidth().padding(top = 20.dp), color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = .65f), shape = MaterialTheme.shapes.large) {
        Column(Modifier.padding(18.dp)) {
            Text("Monthly overview", style = MaterialTheme.typography.titleMedium)
            Row(Modifier.fillMaxWidth().padding(top = 14.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Metric("${value.consistency}%", "Overall consistency")
                Metric(value.activeDays.toString(), "Active days")
                Metric(value.completeDays.toString(), "Complete days")
            }
            AppProgress(value.consistency / 100f, Modifier.padding(top = 14.dp))
        }
    }
}

@Composable private fun Metric(value: String, label: String) { Column(Modifier.widthIn(max = 100.dp)) { Text(value, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary); Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) } }
@Composable private fun SectionTitle(title: String, subtitle: String? = null) { Column(Modifier.padding(top = 28.dp, bottom = 10.dp)) { Text(title, style = MaterialTheme.typography.titleLarge); subtitle?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) } } }

@Composable private fun JourneyCalendar(month: YearMonth, days: List<JourneyDaySummary>, today: LocalDate, onOpenDay: (LocalDate) -> Unit) {
    Surface(Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .45f), shape = MaterialTheme.shapes.large) {
        Column(Modifier.padding(12.dp)) {
            Row(Modifier.fillMaxWidth()) { listOf("M", "T", "W", "T", "F", "S", "S").forEach { Text(it, Modifier.weight(1f), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = androidx.compose.ui.text.style.TextAlign.Center) } }
            val cells = List(JourneyRules.calendarOffset(month)) { null } + days
            cells.chunked(7).forEach { week ->
                Row(Modifier.fillMaxWidth().padding(top = 5.dp)) {
                    week.forEach { day -> CalendarCell(day, today, onOpenDay, Modifier.weight(1f)) }
                    repeat(7 - week.size) { Spacer(Modifier.weight(1f)) }
                }
            }
            Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) { Legend("○", "No data"); Legend("◐", "Partial"); Legend("✓", "Complete") }
        }
    }
}

@Composable private fun CalendarCell(day: JourneyDaySummary?, today: LocalDate, onOpenDay: (LocalDate) -> Unit, modifier: Modifier) {
    if (day == null) { Spacer(modifier.aspectRatio(1f)); return }
    val container = when (day.level) { JourneyDayLevel.COMPLETE -> MaterialTheme.colorScheme.primary; JourneyDayLevel.PARTIAL -> MaterialTheme.colorScheme.secondaryContainer; else -> MaterialTheme.colorScheme.surface }
    val content = if (day.level == JourneyDayLevel.COMPLETE) MaterialTheme.colorScheme.onPrimary else if (day.level == JourneyDayLevel.FUTURE) MaterialTheme.colorScheme.onSurface.copy(alpha = .35f) else MaterialTheme.colorScheme.onSurface
    val state = when (day.level) { JourneyDayLevel.NO_DATA -> "no activity"; JourneyDayLevel.PARTIAL -> "${day.percentage} percent complete"; JourneyDayLevel.COMPLETE -> "complete"; JourneyDayLevel.FUTURE -> "future" }
    Box(modifier.aspectRatio(1f).padding(2.dp).clip(CircleShape).background(container).then(if (day.date == today) Modifier.padding(2.dp).background(MaterialTheme.colorScheme.background, CircleShape).padding(2.dp).background(container, CircleShape) else Modifier).clickable(enabled = day.level != JourneyDayLevel.FUTURE) { onOpenDay(day.date) }.semantics { contentDescription = "${day.date.format(DateTimeFormatter.ofPattern("MMMM d"))}, $state" }, contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) { Text(day.date.dayOfMonth.toString(), color = content, style = MaterialTheme.typography.labelMedium); if (day.level == JourneyDayLevel.COMPLETE) Text("✓", color = content, style = MaterialTheme.typography.labelSmall) else if (day.level == JourneyDayLevel.PARTIAL) Text("•", color = content, style = MaterialTheme.typography.labelSmall) }
    }
}

@Composable private fun Legend(symbol: String, label: String) { Row(verticalAlignment = Alignment.CenterVertically) { Text(symbol, color = MaterialTheme.colorScheme.primary); Spacer(Modifier.width(4.dp)); Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) } }

@Composable private fun PrayerSummary(state: ProgressState) {
    val stats = state.prayerStats
    SectionTitle("Prayer journey", "Recorded prayer states")
    Surface(Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .45f), shape = MaterialTheme.shapes.medium) {
        Column(Modifier.padding(16.dp)) {
            Text("${state.prayerRecords.size} recorded prayers", style = MaterialTheme.typography.titleMedium)
            Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.SpaceBetween) { SmallMetric("Completed", stats.completed); SmallMetric("Jama'ah", stats.jamaah); SmallMetric("Qaza", stats.qaza); SmallMetric("Missed", stats.missed) }
            Text("Unrecorded prayers are not counted as missed.", Modifier.padding(top = 12.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
@Composable private fun SmallMetric(label: String, value: Int) { Column(horizontalAlignment = Alignment.CenterHorizontally) { Text(value.toString(), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary); Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) } }

@Composable private fun RoutineSummary(routines: List<Habit>, state: ProgressState) {
    SectionTitle("Routine progress", "This month's recorded progress")
    routines.forEachIndexed { index, habit ->
        val palette = routinePalette(habit.themeKey)
        val percent = state.percentageFor(habit)
        val total = state.routineProgress.filter { it.habitId == habit.id }.sumOf { it.value }
        val supporting = when (habit.routineType) { RoutineType.DURATION -> "${formatMinutes(total)} recorded"; else -> "$percent% of days completed" }
        Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(routineIcon(habit.iconKey), null, Modifier.size(24.dp), tint = palette.icon); Spacer(Modifier.width(12.dp)); Column(Modifier.weight(1f)) { Text(habit.name, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Medium); Text(supporting, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant); AppProgress(percent / 100f, Modifier.padding(top = 7.dp), palette.progress) }; Spacer(Modifier.width(10.dp)); Text("$percent%", color = palette.icon)
        }
        if (index < routines.lastIndex) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = .55f))
    }
}

@Composable private fun ActivitySummary(totals: List<JourneyActivityTotal>, habits: List<Habit>) {
    SectionTitle("Activity", "Completed timed sessions")
    totals.forEach { total -> val habit = habits.firstOrNull { it.id == total.habitId } ?: return@forEach; ListItem(headlineContent = { Text(habit.name, maxLines = 1, overflow = TextOverflow.Ellipsis) }, trailingContent = { Text(formatDuration(total.millis), color = MaterialTheme.colorScheme.primary) }, leadingContent = { Icon(Icons.Outlined.Timer, null, tint = MaterialTheme.colorScheme.primary) }, colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.background)) }
}

@Composable private fun SleepSummary(summary: JourneySleepSummary) {
    SectionTitle("Sleep", "Completed sleep records")
    ListItem(headlineContent = { Text("${formatDuration(summary.averageMillis)} average") }, supportingContent = { Text("${summary.recordedNights} recorded ${if (summary.recordedNights == 1) "night" else "nights"}") }, leadingContent = { Icon(Icons.Outlined.Bedtime, null, tint = MaterialTheme.colorScheme.primary) }, colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .35f)), modifier = Modifier.clip(MaterialTheme.shapes.medium))
}

@Composable private fun RecentDays(days: List<JourneyDaySummary>, today: LocalDate, open: (LocalDate) -> Unit) {
    SectionTitle("Recent days", "Open a day for its recorded details")
    days.forEach { day -> ListItem(headlineContent = { Text(when (day.date) { today -> "Today"; today.minusDays(1) -> "Yesterday"; else -> day.date.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault()) }) }, supportingContent = { Text(day.date.format(DateTimeFormatter.ofPattern("MMMM d"))) }, trailingContent = { Text("${day.percentage}%") }, modifier = Modifier.clickable { open(day.date) }, colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.background)) }
}

@Composable private fun EmptyJourney(current: Boolean) { Column(Modifier.fillMaxWidth().padding(vertical = 56.dp), horizontalAlignment = Alignment.CenterHorizontally) { Text(if (current) "Your journey starts here" else "No journey recorded", style = MaterialTheme.typography.titleLarge); Text(if (current) "As you use HabitTracker, your routines, prayer records, activity and sleep history will appear here." else "There are no recorded days in this month.", Modifier.padding(top = 8.dp).widthIn(max = 320.dp), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium) } }
private fun formatMinutes(value: Int) = if (value < 60) "$value min" else "${value / 60}h ${value % 60}m"
private fun formatDuration(value: Long) = formatMinutes((value / 60_000L).toInt())
