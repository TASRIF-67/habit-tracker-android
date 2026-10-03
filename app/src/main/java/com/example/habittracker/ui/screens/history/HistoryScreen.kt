package com.example.habittracker.ui.screens.history

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.RemoveCircleOutline
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
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
import com.example.habittracker.ui.components.HabitRow
import com.example.habittracker.ui.components.SectionHeader
import java.time.*
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(date: LocalDate, habits: List<HabitWithStatus>, prayerRecords: List<PrayerRecordDetails>, onDateSelected: (LocalDate) -> Unit) {
    var showPicker by remember { mutableStateOf(false) }
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
            if (items.isNotEmpty()) { SectionHeader(category.displayName, if (category == HabitCategory.GOOD_DEED) "Optional" else null); items.forEachIndexed { index, habit -> HabitRow(habit, enabled = false, showLeadingIcon = category != HabitCategory.SALAT); if (index < items.lastIndex) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = .55f)) } }
        }
        if (habits.isEmpty()) Text("Choose another past date to review your habits.", Modifier.padding(top = 16.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    if (showPicker) {
        val state = rememberDatePickerState(initialSelectedDateMillis = date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(), selectableDates = object : SelectableDates { override fun isSelectableDate(utcTimeMillis: Long) = Instant.ofEpochMilli(utcTimeMillis).atZone(ZoneOffset.UTC).toLocalDate().isBefore(LocalDate.now()) })
        DatePickerDialog(onDismissRequest = { showPicker = false }, confirmButton = { TextButton(onClick = { state.selectedDateMillis?.let { onDateSelected(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()) }; showPicker = false }) { Text("Select") } }, dismissButton = { TextButton(onClick = { showPicker = false }) { Text("Cancel") } }) { DatePicker(state, title = { Text("Select a past date", Modifier.padding(start = 24.dp, top = 18.dp)) }) }
    }
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
