package com.example.habittracker.ui.screens.history

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.RemoveCircleOutline
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.habittracker.data.local.dao.HabitWithStatus
import com.example.habittracker.data.local.dao.PrayerRecordDetails
import com.example.habittracker.data.local.entity.HabitCategory
import com.example.habittracker.data.local.entity.Prayer
import com.example.habittracker.data.local.entity.PrayerStatus
import com.example.habittracker.data.local.entity.RoutineType
import com.example.habittracker.ui.components.HabitRow
import com.example.habittracker.ui.components.SectionHeader
import com.example.habittracker.ui.components.routineIcon
import com.example.habittracker.ui.components.routinePalette
import com.example.habittracker.data.RoutineRules
import com.example.habittracker.data.SleepRules
import com.example.habittracker.data.local.entity.SleepSession
import com.example.habittracker.data.local.entity.durationMillis
import java.time.*
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(date: LocalDate, habits: List<HabitWithStatus>, prayerRecords: List<PrayerRecordDetails>, sleepSessions: List<SleepSession>, onDateSelected: (LocalDate) -> Unit, onCorrectSleep: (Long, Long, Long, (Result<Unit>) -> Unit) -> Unit) {
    var showPicker by remember { mutableStateOf(false) }
    var editingSleep by remember { mutableStateOf<SleepSession?>(null) }
    val salatHabits = habits.filter { it.category == HabitCategory.SALAT }
    val personalHabits = habits.filter { it.category == HabitCategory.PERSONAL }
    val primaryTotal = salatHabits.size + personalHabits.size
    val primaryDone = prayerRecords.count { it.status.countsAsCompleted } + personalHabits.count { it.completed }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(top = 22.dp, bottom = 28.dp)) {
        Text("History", style = MaterialTheme.typography.headlineMedium)
        Text("Review previous days", color = MaterialTheme.colorScheme.onSurfaceVariant)
        OutlinedButton(onClick = { showPicker = true }, modifier = Modifier.padding(top = 20.dp), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)) {
            Icon(Icons.Outlined.CalendarMonth, null); Spacer(Modifier.width(10.dp)); Text(date.format(DateTimeFormatter.ofPattern("EEEE, MMMM d")))
        }
        Surface(Modifier.fillMaxWidth().padding(top = 16.dp), color = MaterialTheme.colorScheme.surfaceVariant, shape = MaterialTheme.shapes.medium) {
            Text(if (habits.isEmpty()) "No habits recorded" else "$primaryDone of $primaryTotal core habits completed", Modifier.padding(16.dp), style = MaterialTheme.typography.titleMedium)
        }
        if (salatHabits.isNotEmpty()) {
            SectionHeader("Salat")
            salatHabits.forEachIndexed { index, habit -> val prayer = Prayer.fromHabitName(habit.name); PrayerHistoryRow(habit.name, prayerRecords.firstOrNull { it.prayer == prayer }); if (index < salatHabits.lastIndex) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = .55f)) }
        }
        HabitCategory.entries.filter { it != HabitCategory.SALAT }.forEach { category ->
            val items = habits.filter { it.category == category }
            if (items.isNotEmpty()) { SectionHeader(category.displayName, if (category == HabitCategory.GOOD_DEED) "Optional" else null); items.forEachIndexed { index, habit -> if (habit.routineType == RoutineType.CHECK) HabitRow(habit, enabled = false, showLeadingIcon = true) else RoutineHistoryRow(habit); if (index < items.lastIndex) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = .55f)) } }
        }
        if (sleepSessions.isNotEmpty()) {
            SectionHeader("Sleep")
            sleepSessions.forEach { session ->
                ListItem(
                    headlineContent = { Text(SleepRules.formatDuration(session.durationMillis())) },
                    supportingContent = { Text("${SleepRules.formatInstant(session.wentToBedAt)} → ${SleepRules.formatInstant(session.wokeUpAt!!)}") },
                    leadingContent = { Icon(Icons.Outlined.Bedtime, null, tint = MaterialTheme.colorScheme.primary) },
                    trailingContent = { TextButton(onClick = { editingSleep = session }) { Text("Edit") } },
                    colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.background),
                )
            }
        }
        if (habits.isEmpty() && sleepSessions.isEmpty()) Text("Choose another past date to review your habits.", Modifier.padding(top = 16.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    if (showPicker) {
        val state = rememberDatePickerState(initialSelectedDateMillis = date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(), selectableDates = object : SelectableDates { override fun isSelectableDate(utcTimeMillis: Long) = Instant.ofEpochMilli(utcTimeMillis).atZone(ZoneOffset.UTC).toLocalDate().isBefore(LocalDate.now()) })
        DatePickerDialog(onDismissRequest = { showPicker = false }, confirmButton = { TextButton(onClick = { state.selectedDateMillis?.let { onDateSelected(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()) }; showPicker = false }) { Text("Select") } }, dismissButton = { TextButton(onClick = { showPicker = false }) { Text("Cancel") } }) { DatePicker(state, title = { Text("Select a past date", Modifier.padding(start = 24.dp, top = 18.dp)) }) }
    }
    editingSleep?.let { session -> SleepCorrectionDialog(session, { editingSleep = null }) { bedtime, wake -> onCorrectSleep(session.id, bedtime, wake) { result -> if (result.isSuccess) editingSleep = null } } }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SleepCorrectionDialog(session: SleepSession, onDismiss: () -> Unit, onSave: (Long, Long) -> Unit) {
    val zone = ZoneId.systemDefault()
    val originalBed = Instant.ofEpochMilli(session.wentToBedAt).atZone(zone)
    val originalWake = Instant.ofEpochMilli(session.wokeUpAt!!).atZone(zone)
    var bedMinutes by remember(session.id) { mutableIntStateOf(originalBed.hour * 60 + originalBed.minute) }
    var wakeMinutes by remember(session.id) { mutableIntStateOf(originalWake.hour * 60 + originalWake.minute) }
    var picker by remember { mutableStateOf<String?>(null) }
    val bedDate = LocalDate.parse(session.sleepDate)
    val bedInstant = ZonedDateTime.of(bedDate, LocalTime.of(bedMinutes / 60, bedMinutes % 60), zone).toInstant().toEpochMilli()
    var wakeDate = bedDate
    if (wakeMinutes <= bedMinutes) wakeDate = wakeDate.plusDays(1)
    val wakeInstant = ZonedDateTime.of(wakeDate, LocalTime.of(wakeMinutes / 60, wakeMinutes % 60), zone).toInstant().toEpochMilli()
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Correct sleep record") }, text = { Column { ListItem(headlineContent = { Text("Went to bed") }, trailingContent = { Text(SleepRules.formatMinutes(bedMinutes)) }, modifier = Modifier.clickable { picker = "bed" }); ListItem(headlineContent = { Text("Woke up") }, trailingContent = { Text(SleepRules.formatMinutes(wakeMinutes)) }, modifier = Modifier.clickable { picker = "wake" }); Text(SleepRules.formatDuration(wakeInstant - bedInstant), Modifier.padding(top = 8.dp), color = MaterialTheme.colorScheme.onSurfaceVariant) } }, confirmButton = { TextButton(onClick = { onSave(bedInstant, wakeInstant) }) { Text("Save") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } })
    picker?.let { field -> val initial = if (field == "bed") bedMinutes else wakeMinutes; val state = rememberTimePickerState(initial / 60, initial % 60); AlertDialog(onDismissRequest = { picker = null }, title = { Text(if (field == "bed") "Went to bed" else "Woke up") }, text = { TimePicker(state) }, confirmButton = { TextButton(onClick = { if (field == "bed") bedMinutes = state.hour * 60 + state.minute else wakeMinutes = state.hour * 60 + state.minute; picker = null }) { Text("Set") } }, dismissButton = { TextButton(onClick = { picker = null }) { Text("Cancel") } }) }
}

@Composable
private fun RoutineHistoryRow(habit: HabitWithStatus) {
    val palette = routinePalette(habit.themeKey)
    val secondary = RoutineRules.derivedMeasurement(habit.value, habit.target ?: 0, habit.quantityPerCount, habit.measurementUnit)
    ListItem(
        headlineContent = { Text(habit.name) },
        supportingContent = { Column { Text("${habit.value} / ${habit.target ?: 0} ${habit.unit.orEmpty()} · ${if (habit.completed) "Completed" else "Incomplete"}"); secondary?.let { Text(it, style = MaterialTheme.typography.bodySmall) } } },
        leadingContent = { Icon(routineIcon(habit.iconKey), null, tint = palette.icon) },
        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.background),
    )
}

private val HabitCategory.displayName get() = when (this) { HabitCategory.SALAT -> "Salat"; HabitCategory.GOOD_DEED -> "Good deeds"; HabitCategory.PERSONAL -> "Personal" }

@Composable
private fun PrayerHistoryRow(name: String, record: PrayerRecordDetails?) {
    val label = when { record == null -> "Unrecorded"; record.inJamaah -> "Jama'ah"; else -> record.status.name.lowercase().replaceFirstChar { it.uppercase() } }
    val icon = when { record == null -> Icons.Outlined.RadioButtonUnchecked; record.inJamaah -> Icons.Outlined.Groups; record.status == PrayerStatus.COMPLETED -> Icons.Rounded.CheckCircle; record.status == PrayerStatus.QAZA -> Icons.Outlined.History; else -> Icons.Outlined.RemoveCircleOutline }
    ListItem(
        headlineContent = { Text(name) },
        supportingContent = { Column { Text(label); if (record?.reasonName != null) Text("Reason: ${record.reasonName}", style = MaterialTheme.typography.bodySmall) } },
        leadingContent = { Icon(icon, label, tint = if (record?.status?.countsAsCompleted == true) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant) },
        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.background),
    )
}
