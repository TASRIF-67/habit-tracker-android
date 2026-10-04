package com.example.habittracker.ui.screens.journey

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.habittracker.data.HabitPolicy
import com.example.habittracker.data.RoutineRules
import com.example.habittracker.data.SleepRules
import com.example.habittracker.data.local.dao.HabitWithStatus
import com.example.habittracker.data.local.dao.PrayerRecordDetails
import com.example.habittracker.data.local.entity.*
import com.example.habittracker.ui.components.routineIcon
import com.example.habittracker.ui.components.routinePalette
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun JourneyDayScreen(date: LocalDate, habits: List<HabitWithStatus>, prayers: List<PrayerRecordDetails>, activities: List<ActivitySession>, sleep: List<SleepSession>, onBack: () -> Unit) {
    val primary = habits.filter { HabitPolicy.isPrimary(it.category) }
    val completed = primary.count { habit -> if (habit.category == HabitCategory.SALAT) Prayer.fromHabitName(habit.name)?.let { prayer -> prayers.firstOrNull { it.prayer == prayer }?.status?.countsAsCompleted } == true else habit.completed }
    val percent = if (primary.isEmpty()) 0 else completed * 100 / primary.size
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 32.dp)) {
        item { IconButton(onBack) { Icon(Icons.Outlined.ArrowBack, "Back") }; Text(date.format(DateTimeFormatter.ofPattern("MMMM d")).uppercase(), style = MaterialTheme.typography.headlineMedium); Text("$percent% daily completion", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp)) }
        val salat = habits.filter { it.category == HabitCategory.SALAT }
        if (salat.isNotEmpty() || prayers.isNotEmpty()) {
            item { DaySection("Prayers") }
            Prayer.entries.forEach { prayer -> item { val record = prayers.firstOrNull { it.prayer == prayer }; DetailRow(prayer.displayName, prayerLabel(record)) } }
        }
        val routines = habits.filter { it.category == HabitCategory.PERSONAL }
        if (routines.isNotEmpty()) {
            item { DaySection("Routines") }
            routines.forEach { habit -> item { RoutineDetail(habit) } }
        }
        val deeds = habits.filter { it.category == HabitCategory.GOOD_DEED && it.completed }
        if (deeds.isNotEmpty()) { item { DaySection("Good deeds", "Optional") }; deeds.forEach { habit -> item { DetailRow(habit.name, "Completed") } } }
        if (activities.isNotEmpty()) {
            item { DaySection("Activity") }
            activities.filter { it.status == ActivitySessionStatus.FINISHED }.forEach { session -> item { val name = habits.firstOrNull { it.id == session.habitId }?.name ?: "Timed activity"; IconDetail(Icons.Outlined.Timer, name, formatDuration(session.accumulatedActiveMillis)) } }
        }
        if (sleep.isNotEmpty()) {
            item { DaySection("Sleep") }
            sleep.forEach { session -> item { IconDetail(Icons.Outlined.Bedtime, SleepRules.formatDuration(session.durationMillis()), "${SleepRules.formatInstant(session.wentToBedAt)} – ${SleepRules.formatInstant(session.wokeUpAt!!)}") } }
        }
        if (habits.isEmpty() && prayers.isEmpty() && activities.isEmpty() && sleep.isEmpty()) item { Text("Nothing was recorded on this day.", Modifier.padding(top = 32.dp), color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}

@Composable private fun DaySection(title: String, trailing: String? = null) { Row(Modifier.fillMaxWidth().padding(top = 28.dp, bottom = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) { Text(title, style = MaterialTheme.typography.titleLarge); trailing?.let { Text(it, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) } } }
@Composable private fun DetailRow(name: String, value: String) { ListItem(headlineContent = { Text(name) }, trailingContent = { Text(value, color = MaterialTheme.colorScheme.onSurfaceVariant) }, colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.background)) }
@Composable private fun IconDetail(icon: androidx.compose.ui.graphics.vector.ImageVector, name: String, value: String) { ListItem(headlineContent = { Text(name, maxLines = 1, overflow = TextOverflow.Ellipsis) }, supportingContent = { Text(value) }, leadingContent = { Icon(icon, null, tint = MaterialTheme.colorScheme.primary) }, colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.background)) }
@Composable private fun RoutineDetail(habit: HabitWithStatus) { val palette = routinePalette(habit.themeKey); val value = when (habit.routineType) { RoutineType.CHECK -> if (habit.completed) "Completed" else "Not completed"; else -> "${habit.value} / ${habit.target ?: 0}${habit.unit?.let { " $it" }.orEmpty()}" }; ListItem(headlineContent = { Text(habit.name, maxLines = 1, overflow = TextOverflow.Ellipsis) }, supportingContent = { RoutineRules.derivedMeasurement(habit.value, habit.target ?: 0, habit.quantityPerCount, habit.measurementUnit)?.let { Text(it) } }, trailingContent = { Text(value) }, leadingContent = { Icon(routineIcon(habit.iconKey), null, tint = palette.icon) }, colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.background)) }
private fun prayerLabel(record: PrayerRecordDetails?) = when { record == null -> "Unrecorded"; record.inJamaah -> "Jama'ah"; record.status == PrayerStatus.COMPLETED -> "Completed"; record.status == PrayerStatus.QAZA -> "Qaza"; record.status == PrayerStatus.MISSED -> "Missed"; else -> "Unrecorded" }
private fun formatDuration(value: Long): String { val minutes = value / 60_000L; return if (minutes < 60) "$minutes min" else "${minutes / 60}h ${minutes % 60}m" }
