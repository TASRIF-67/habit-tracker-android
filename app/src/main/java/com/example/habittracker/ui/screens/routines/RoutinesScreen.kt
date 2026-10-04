package com.example.habittracker.ui.screens.routines

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.habittracker.data.*
import com.example.habittracker.data.local.dao.HabitWithStatus
import com.example.habittracker.data.local.entity.*
import com.example.habittracker.ui.components.RoutineEditorDialog
import com.example.habittracker.ui.components.routineIcon
import com.example.habittracker.ui.components.routinePalette
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.coroutines.delay

private enum class RoutinePeriod(val title: String) { MORNING("Morning"), AFTERNOON("Afternoon"), EVENING("Evening"), ANYTIME("Anytime") }
private data class RoutineEntry(val habit: HabitWithStatus, val schedule: RoutineSchedule?)

@Composable
fun RoutinesScreen(
    allHabits: List<Habit>, todayHabits: List<HabitWithStatus>, schedules: List<RoutineSchedule>, activeSession: ActivitySession?,
    onAdd: (RoutineDraft, (Result<Unit>) -> Unit) -> Unit,
    onEdit: (Habit, RoutineDraft, (Result<Unit>) -> Unit) -> Unit,
    onArchive: (Habit, Boolean, (Result<Unit>) -> Unit) -> Unit,
    onToggle: (HabitWithStatus) -> Unit,
    onAddProgress: (HabitWithStatus, Int, (RoutineProgressUpdate) -> Unit) -> Unit,
    onStart: (Long, (StartSessionResult) -> Unit) -> Unit,
    onOpenActiveSession: () -> Unit,
) {
    val customHabits = allHabits.filter { !it.isBuiltIn }
    val habitEntities = customHabits.associateBy(Habit::id)
    val active = todayHabits.filter { it.active && !it.isBuiltIn }.map { status -> RoutineEntry(status, schedules.firstOrNull { it.habitId == status.id }) }
    val archived = customHabits.filterNot(Habit::active)
    val groups = active.groupBy { entry ->
        val schedule = entry.schedule
        if (schedule?.enabled != true || !ScheduleRules.includes(schedule.daysMask, LocalDate.now().dayOfWeek)) RoutinePeriod.ANYTIME
        else when (schedule.timeMinutes) { in 0 until 12 * 60 -> RoutinePeriod.MORNING; in 12 * 60 until 17 * 60 -> RoutinePeriod.AFTERNOON; else -> RoutinePeriod.EVENING }
    }.mapValues { (_, entries) -> entries.sortedWith(compareBy<RoutineEntry> { it.schedule?.timeMinutes ?: Int.MAX_VALUE }.thenBy { it.habit.name.lowercase() }) }

    var adding by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Habit?>(null) }
    var archiving by remember { mutableStateOf<Habit?>(null) }
    var showArchived by rememberSaveable { mutableStateOf(false) }
    var managementError by remember { mutableStateOf<String?>(null) }
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) { while (true) { now = System.currentTimeMillis(); delay(30_000) } }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 22.dp, bottom = 32.dp)) {
        item {
            Text("Routines", style = MaterialTheme.typography.headlineMedium)
            Text("Your daily rhythm", Modifier.padding(top = 10.dp), style = MaterialTheme.typography.titleLarge)
            Text("Plan and manage the routines that shape your day.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Button(onClick = { adding = true }, modifier = Modifier.padding(top = 18.dp).heightIn(min = 48.dp)) { Icon(Icons.Outlined.Add, null); Spacer(Modifier.width(6.dp)); Text("Add routine") }
        }
        if (active.isEmpty()) item {
            Surface(Modifier.fillMaxWidth().padding(top = 22.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .48f), shape = MaterialTheme.shapes.large) {
                Column(Modifier.padding(20.dp)) { Text("Your daily rhythm", style = MaterialTheme.typography.titleMedium); Text("Create routines for things you want to do consistently—walking, studying, reading, hydration, exercise, or anything else.", Modifier.padding(top = 6.dp), color = MaterialTheme.colorScheme.onSurfaceVariant); TextButton(onClick = { adding = true }, contentPadding = PaddingValues(0.dp)) { Text("Create your first routine") } }
            }
        } else {
            item { Text("Today", Modifier.padding(top = 28.dp, bottom = 4.dp), style = MaterialTheme.typography.titleLarge) }
            RoutinePeriod.entries.forEach { period ->
                val entries = groups[period].orEmpty()
                if (entries.isNotEmpty()) {
                    item { Text(period.title, Modifier.padding(top = 16.dp, bottom = 4.dp), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary) }
                    items(entries, key = { it.habit.id }) { entry ->
                        DailyRoutineRow(entry, activeSession?.takeIf { it.habitId == entry.habit.id }, now, { editing = habitEntities[entry.habit.id] }, { archiving = habitEntities[entry.habit.id] }, { onToggle(entry.habit) }, { onAddProgress(entry.habit, 1) {} }) {
                            val session = activeSession
                            when { session?.habitId == entry.habit.id -> onOpenActiveSession(); else -> onStart(entry.habit.id) { result -> when {
                                result is StartSessionResult.Started -> onOpenActiveSession()
                                result is StartSessionResult.AlreadyActive && result.session.habitId == entry.habit.id -> onOpenActiveSession()
                                result is StartSessionResult.AlreadyActive -> managementError = "Another routine is already active. Open or finish it first."
                            } } }
                        }
                    }
                }
            }
        }
        item {
            HorizontalDivider(Modifier.padding(top = 26.dp))
            ListItem(headlineContent = { Text("Archived routines") }, supportingContent = { Text(if (archived.isEmpty()) "No archived routines" else "${archived.size} archived") }, leadingContent = { Icon(Icons.Outlined.Inventory2, null) }, trailingContent = { Icon(if (showArchived) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore, if (showArchived) "Hide archived routines" else "Show archived routines") }, modifier = Modifier.clickable(enabled = archived.isNotEmpty()) { showArchived = !showArchived }, colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.background))
        }
        if (showArchived) items(archived, key = Habit::id) { habit -> ArchivedRoutineRow(habit, { editing = habit }) { onArchive(habit, false) { it.onFailure { error -> managementError = error.message } } } }
    }

    if (adding) RoutineEditorDialog(onDismiss = { adding = false }) { draft, report -> onAdd(draft) { result -> result.onSuccess { adding = false }; result.onFailure { report(it.message ?: "Could not create routine") } } }
    editing?.let { habit -> RoutineEditorDialog(habit, schedules.firstOrNull { it.habitId == habit.id }, { editing = null }) { draft, report -> onEdit(habit, draft) { result -> result.onSuccess { editing = null }; result.onFailure { report(it.message ?: "Could not save routine") } } } }
    archiving?.let { habit -> AlertDialog(onDismissRequest = { archiving = null }, icon = { Icon(Icons.Outlined.Archive, null) }, title = { Text("Archive ${habit.name}?") }, text = { Text("It will leave Today, while its history remains available.") }, confirmButton = { TextButton(onClick = { onArchive(habit, true) { result -> result.onSuccess { archiving = null }; result.onFailure { managementError = it.message; archiving = null } } }) { Text("Archive") } }, dismissButton = { TextButton(onClick = { archiving = null }) { Text("Cancel") } }) }
    managementError?.let { message -> AlertDialog(onDismissRequest = { managementError = null }, title = { Text("Unable to update routine") }, text = { Text(message) }, confirmButton = { TextButton(onClick = { managementError = null }) { Text("OK") } }) }
}

@Composable
private fun DailyRoutineRow(entry: RoutineEntry, activeSession: ActivitySession?, now: Long, onEdit: () -> Unit, onArchive: () -> Unit, onToggle: () -> Unit, onIncrement: () -> Unit, onDuration: () -> Unit) {
    val habit = entry.habit
    val palette = routinePalette(habit.themeKey)
    val activeMinutes = activeSession?.elapsedMillis(now)?.div(60_000)
    Surface(Modifier.fillMaxWidth().padding(vertical = 4.dp).alpha(if (habit.completed) .68f else 1f), color = palette.container, shape = MaterialTheme.shapes.large, border = androidx.compose.foundation.BorderStroke(1.dp, palette.border)) {
        Row(Modifier.clickable(onClick = onEdit).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(color = palette.container, shape = MaterialTheme.shapes.medium) { Icon(routineIcon(habit.iconKey), null, Modifier.padding(10.dp), tint = palette.icon) }
            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(habit.name, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(listOfNotNull(scheduleState(entry.schedule, habit.completed), targetSummary(habit)).joinToString(" · "), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(if (activeMinutes != null) "${if (activeSession.status == ActivitySessionStatus.PAUSED) "Paused" else "Active"} · $activeMinutes min" else progressSummary(habit), style = MaterialTheme.typography.bodySmall, color = if (activeMinutes != null) palette.icon else MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            Column(horizontalAlignment = Alignment.End) {
                when (habit.routineType) {
                    RoutineType.CHECK -> Checkbox(habit.completed, { onToggle() }, modifier = Modifier.size(48.dp))
                    RoutineType.COUNT -> FilledTonalButton(onClick = onIncrement, enabled = !habit.completed, contentPadding = PaddingValues(horizontal = 13.dp), modifier = Modifier.heightIn(min = 44.dp)) { Text(if (habit.completed) "Done" else "+1") }
                    RoutineType.DURATION -> FilledTonalButton(onClick = onDuration, enabled = !habit.completed, contentPadding = PaddingValues(horizontal = 13.dp), modifier = Modifier.heightIn(min = 44.dp)) { Text(if (habit.completed) "Done" else if (activeSession != null) "Continue" else "Start") }
                }
                IconButton(onClick = onArchive, modifier = Modifier.size(40.dp)) { Icon(Icons.Outlined.Archive, "Archive ${habit.name}", Modifier.size(20.dp)) }
            }
        }
    }
}

@Composable
private fun ArchivedRoutineRow(habit: Habit, onEdit: () -> Unit, onRestore: () -> Unit) {
    ListItem(headlineContent = { Text(habit.name) }, supportingContent = { Text(targetSummary(habit)) }, leadingContent = { Icon(routineIcon(habit.iconKey), null) }, trailingContent = { IconButton(onClick = onRestore) { Icon(Icons.Outlined.Unarchive, "Restore ${habit.name}") } }, modifier = Modifier.clickable(onClick = onEdit), colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.background))
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = .55f))
}

private fun scheduleState(schedule: RoutineSchedule?, completed: Boolean): String? {
    if (schedule?.enabled != true) return null
    val now = java.time.ZonedDateTime.now()
    val currentWindow = ScheduleRules.currentWindow(schedule, now)
    val scheduledToday = ScheduleRules.includes(schedule.daysMask, now.dayOfWeek)
    if (!scheduledToday && currentWindow == null) return null
    val time = ScheduleRules.timeRange(schedule)
    if (completed) return time
    if (currentWindow != null) return "Now · until ${ScheduleRules.formatTime(currentWindow.end.hour * 60 + currentWindow.end.minute)}"
    if (schedule.endTimeMinutes != null) {
        val window = ScheduleRules.window(schedule, now.toLocalDate(), now.zone) ?: return null
        return if (now.isBefore(window.start)) "Upcoming · $time" else "Scheduled · $time"
    }
    val difference = schedule.timeMinutes - (LocalTime.now().hour * 60 + LocalTime.now().minute)
    return when { difference in -30..30 -> "Now · $time"; difference < 0 -> "Scheduled · $time"; else -> time }
}

private fun targetSummary(habit: Habit): String = when (habit.routineType) { RoutineType.CHECK -> "Done / not done"; RoutineType.DURATION -> "${habit.target ?: 0} min"; RoutineType.COUNT -> "${habit.target ?: 0} ${habit.unit.orEmpty()}".trim() }
private fun targetSummary(habit: HabitWithStatus): String = when (habit.routineType) { RoutineType.CHECK -> "Done / not done"; RoutineType.DURATION -> "${habit.target ?: 0} min"; RoutineType.COUNT -> "${habit.target ?: 0} ${habit.unit.orEmpty()}".trim() }
private fun progressSummary(habit: HabitWithStatus): String = when (habit.routineType) {
    RoutineType.CHECK -> if (habit.completed) "Completed" else "Not completed"
    RoutineType.DURATION -> "${habit.value} / ${habit.target ?: 0} min"
    RoutineType.COUNT -> buildString { append("${habit.value} / ${habit.target ?: 0} ${habit.unit.orEmpty()}".trim()); RoutineRules.derivedMeasurement(habit.value, habit.target ?: 0, habit.quantityPerCount, habit.measurementUnit)?.let { append(" · $it") } }
}
