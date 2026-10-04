package com.example.habittracker.ui.components

import android.Manifest
import android.app.TimePickerDialog
import android.app.AlarmManager
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.habittracker.data.RoutineDraft
import com.example.habittracker.data.ScheduleDraft
import com.example.habittracker.data.ScheduleRules
import com.example.habittracker.data.EVERY_DAY
import com.example.habittracker.data.SUPPORTED_REMINDER_OFFSETS
import com.example.habittracker.data.RoutineIdentity
import com.example.habittracker.data.RoutineRules
import com.example.habittracker.data.local.entity.Habit
import com.example.habittracker.data.local.entity.RoutineType
import com.example.habittracker.data.local.entity.RoutineSchedule
import com.example.habittracker.data.local.entity.ReminderStyle
import java.time.DayOfWeek

private enum class ScheduleMode { ANYTIME, AT_TIME, TIME_BLOCK }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoutineEditorDialog(existing: Habit? = null, existingSchedule: RoutineSchedule? = null, onDismiss: () -> Unit, onSave: (RoutineDraft, (String) -> Unit) -> Unit) {
    var name by remember(existing) { mutableStateOf(existing?.name.orEmpty()) }
    var type by remember(existing) { mutableStateOf(existing?.routineType ?: RoutineType.CHECK) }
    var target by remember(existing) { mutableStateOf(existing?.target?.toString().orEmpty()) }
    var unit by remember(existing) { mutableStateOf(existing?.unit.orEmpty()) }
    var iconKey by remember(existing) { mutableStateOf(RoutineIdentity.iconOrDefault(existing?.iconKey)) }
    var themeKey by remember(existing) { mutableStateOf(RoutineIdentity.themeOrDefault(existing?.themeKey)) }
    var quantity by remember(existing) { mutableStateOf(existing?.quantityPerCount?.let(::plainNumber).orEmpty()) }
    var measurementUnit by remember(existing) { mutableStateOf(existing?.measurementUnit ?: "mL") }
    var secondaryEnabled by remember(existing) { mutableStateOf(existing?.quantityPerCount != null) }
    var choosingIcon by remember { mutableStateOf(false) }
    var choosingTheme by remember { mutableStateOf(false) }
    var scheduleEnabled by remember(existingSchedule) { mutableStateOf(existingSchedule?.enabled == true) }
    var timeMinutes by remember(existingSchedule) { mutableIntStateOf(existingSchedule?.timeMinutes ?: 9 * 60) }
    var endTimeMinutes by remember(existingSchedule) { mutableIntStateOf(existingSchedule?.endTimeMinutes ?: ((existingSchedule?.timeMinutes ?: 9 * 60) + 60) % 1440) }
    var scheduleMode by remember(existingSchedule) { mutableStateOf(when { existingSchedule?.enabled != true -> ScheduleMode.ANYTIME; existingSchedule.endTimeMinutes != null -> ScheduleMode.TIME_BLOCK; else -> ScheduleMode.AT_TIME }) }
    var daysMask by remember(existingSchedule) { mutableIntStateOf(existingSchedule?.daysMask ?: EVERY_DAY) }
    var reminderEnabled by remember(existingSchedule) { mutableStateOf(existingSchedule?.reminderEnabled == true) }
    var reminderOffset by remember(existingSchedule) { mutableIntStateOf(existingSchedule?.reminderOffsetMinutes ?: 0) }
    var reminderStyle by remember(existingSchedule) { mutableStateOf(existingSchedule?.reminderStyle ?: ReminderStyle.PROMINENT) }
    var scheduleExpanded by rememberSaveable { mutableStateOf(false) }
    var reminderExpanded by rememberSaveable { mutableStateOf(false) }
    val context = LocalContext.current
    var permissionDenied by remember { mutableStateOf(Build.VERSION.SDK_INT >= 33 && reminderEnabled && context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted -> permissionDenied = !granted }
    var error by remember { mutableStateOf<String?>(null) }
    val scheduleDraft = ScheduleDraft(scheduleEnabled, timeMinutes, daysMask, reminderEnabled, reminderOffset, reminderStyle, endTimeMinutes.takeIf { scheduleMode == ScheduleMode.TIME_BLOCK })
    val draft = RoutineDraft(name, type, target.toIntOrNull(), unit, iconKey, themeKey, quantity.toDoubleOrNull().takeIf { secondaryEnabled && type == RoutineType.COUNT }, measurementUnit.takeIf { secondaryEnabled && type == RoutineType.COUNT }, scheduleDraft)
    val valid = RoutineRules.normalized(draft).isSuccess
    AlertDialog(
        modifier = Modifier.imePadding(), onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "New routine" else "Edit routine") },
        text = { Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(name, { if (it.length <= RoutineRules.MAX_NAME_LENGTH) { name = it; error = null } }, Modifier.fillMaxWidth(), label = { Text("Name") }, supportingText = { Text("${name.length}/${RoutineRules.MAX_NAME_LENGTH}") }, singleLine = true)
            Text("Icon", style = MaterialTheme.typography.labelLarge)
            OutlinedCard(Modifier.fillMaxWidth().clickable(role = Role.Button) { choosingIcon = true }) { Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) { Icon(routineIcon(iconKey), null, tint = routinePalette(themeKey).icon); Text(routineIconLabel(iconKey), Modifier.weight(1f).padding(start = 12.dp)); Text("Choose", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge) } }
            Text("Theme", style = MaterialTheme.typography.labelLarge)
            OutlinedCard(Modifier.fillMaxWidth().clickable(role = Role.Button) { choosingTheme = true }) { val palette = routinePalette(themeKey); Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) { Box(Modifier.size(18.dp).background(palette.icon, CircleShape)); Text(routineThemeOptions.first { it.key == themeKey }.label, Modifier.weight(1f).padding(start = 12.dp)); Text("Choose", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge) } }
            Text("Tracking", style = MaterialTheme.typography.labelLarge)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) { RoutineType.entries.forEach { option -> FilterChip(selected = type == option, onClick = { if (existing == null) { type = option; error = null } }, enabled = existing == null, label = { Text(option.friendlyName) }) } }
            Text(type.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (existing != null) Text("Tracking type cannot be changed after creation.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (type != RoutineType.CHECK) OutlinedTextField(target, { target = it.filter(Char::isDigit).take(7); error = null }, Modifier.fillMaxWidth(), label = { Text(if (type == RoutineType.DURATION) "Daily target" else "Daily goal") }, suffix = { Text(if (type == RoutineType.DURATION) "min" else unit.ifBlank { "items" }) }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
            if (type == RoutineType.COUNT) {
                OutlinedTextField(unit, { if (it.length <= RoutineRules.MAX_UNIT_LENGTH) { unit = it; error = null } }, Modifier.fillMaxWidth(), label = { Text("Count unit") }, placeholder = { Text("bottles, pages, reps") }, singleLine = true)
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { Checkbox(secondaryEnabled, { secondaryEnabled = it; error = null }); Text("Secondary measurement (optional)") }
                if (secondaryEnabled) Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(quantity, { input -> quantity = input.filter { it.isDigit() || it == '.' }.take(12); error = null }, Modifier.weight(1f), label = { Text("Each count") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                    var expanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(expanded, { expanded = it }, Modifier.width(100.dp)) { OutlinedTextField(measurementUnit, {}, Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable).fillMaxWidth(), readOnly = true, label = { Text("Unit") }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) }); ExposedDropdownMenu(expanded, { expanded = false }) { RoutineRules.measurementUnits.forEach { option -> DropdownMenuItem({ Text(option) }, { measurementUnit = option; expanded = false }) } } }
                }
            }
            HorizontalDivider()
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text("When", style = MaterialTheme.typography.titleMedium); Text(if (scheduleEnabled) "${daySummary(daysMask)} · ${scheduleTimeLabel(timeMinutes, endTimeMinutes.takeIf { scheduleMode == ScheduleMode.TIME_BLOCK })}" else "Anytime · No schedule", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }; TextButton(onClick = { scheduleExpanded = !scheduleExpanded }) { Text(if (scheduleExpanded) "Done" else "Change schedule") } }
            if (scheduleExpanded) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    ScheduleMode.entries.forEach { mode -> Row(Modifier.fillMaxWidth().heightIn(min = 44.dp).clickable(role = Role.RadioButton) { scheduleMode = mode; scheduleEnabled = mode != ScheduleMode.ANYTIME; if (!scheduleEnabled) reminderEnabled = false }, verticalAlignment = Alignment.CenterVertically) { RadioButton(scheduleMode == mode, null); Text(when (mode) { ScheduleMode.ANYTIME -> "Anytime"; ScheduleMode.AT_TIME -> "At a time"; ScheduleMode.TIME_BLOCK -> "Time block" }) } }
                }
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text("Reminder", style = MaterialTheme.typography.titleMedium); Text(if (scheduleEnabled) reminderEditorSummary(reminderEnabled, reminderOffset, reminderStyle) else "Add a schedule first", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }; TextButton(onClick = { reminderExpanded = !reminderExpanded; scheduleExpanded = true }, enabled = scheduleEnabled) { Text(if (reminderExpanded) "Done" else "Change reminder") } }
            if (scheduleExpanded && scheduleEnabled) {
                Text("Days", style = MaterialTheme.typography.labelLarge)
                WeekdaySelector(daysMask) { day, selected -> daysMask = ScheduleRules.withDaySelected(daysMask, day, selected) }
                OutlinedButton(onClick = { TimePickerDialog(context, { _, hour, minute -> timeMinutes = hour * 60 + minute }, timeMinutes / 60, timeMinutes % 60, false).show() }, Modifier.fillMaxWidth()) { Text("${if (scheduleMode == ScheduleMode.TIME_BLOCK) "Starts" else "Time"} · ${ScheduleRules.formatTime(timeMinutes)}") }
                if (scheduleMode == ScheduleMode.TIME_BLOCK) {
                    OutlinedButton(onClick = { TimePickerDialog(context, { _, hour, minute -> endTimeMinutes = hour * 60 + minute }, endTimeMinutes / 60, endTimeMinutes % 60, false).show() }, Modifier.fillMaxWidth()) { Text("Ends · ${ScheduleRules.formatTime(endTimeMinutes)}") }
                    if (endTimeMinutes == timeMinutes) Text("Start and end times must be different.", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    else if (endTimeMinutes < timeMinutes) Text("Ends the next day · belongs to the start day", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                }
                if (reminderExpanded) Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { Text("Enable reminder", Modifier.weight(1f)); Switch(reminderEnabled, { enabled -> reminderEnabled = enabled; if (enabled && !permissionDenied && Build.VERSION.SDK_INT >= 33 && context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) }) }
                if (reminderExpanded && reminderEnabled) {
                    var reminderMenu by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(reminderMenu, { reminderMenu = it }) { OutlinedTextField(reminderOffsetLabel(reminderOffset), {}, Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable).fillMaxWidth(), readOnly = true, label = { Text("Remind me") }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(reminderMenu) }); ExposedDropdownMenu(reminderMenu, { reminderMenu = false }) { SUPPORTED_REMINDER_OFFSETS.sorted().forEach { offset -> DropdownMenuItem({ Text(reminderOffsetLabel(offset)) }, { reminderOffset = offset; reminderMenu = false }) } } }
                    Text("Reminder style", style = MaterialTheme.typography.labelLarge)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ReminderStyle.entries.forEach { style -> FilterChip(selected = reminderStyle == style, onClick = { reminderStyle = style }, label = { Text(if (style == ReminderStyle.PROMINENT) "Prominent" else "Alarm") }, leadingIcon = { RadioButton(reminderStyle == style, null) }) }
                    }
                    if (reminderStyle == ReminderStyle.ALARM && Build.VERSION.SDK_INT >= 31 && !context.getSystemService(AlarmManager::class.java).canScheduleExactAlarms()) {
                        Text("Without Alarms & reminders access, HabitTracker will still alert you using an inexact high-priority alarm notification.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        TextButton(onClick = { context.startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${context.packageName}"))) }) { Text("Allow precise alarms") }
                    }
                    if (permissionDenied) TextButton(onClick = { context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}"))) }) { Text("Notifications are unavailable · Open settings") }
                }
            }
            RoutinePreview(draft)
            error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
        } },
        confirmButton = { TextButton(onClick = { onSave(draft) { error = it } }, enabled = valid) { Text(if (existing == null) "Create routine" else "Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
    if (choosingIcon) ModalBottomSheet(onDismissRequest = { choosingIcon = false }) {
        Text("Choose icon", Modifier.padding(horizontal = 24.dp, vertical = 8.dp), style = MaterialTheme.typography.titleLarge)
        LazyVerticalGrid(GridCells.Fixed(4), Modifier.fillMaxWidth().heightIn(max = 420.dp).padding(16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(routineIconOptions) { option -> val selected = iconKey == option.key; Surface(Modifier.aspectRatio(1f).clickable(role = Role.RadioButton) { iconKey = option.key; choosingIcon = false }, color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant, shape = MaterialTheme.shapes.medium) { Column(Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) { Icon(option.icon, "Select ${option.label} icon"); Text(option.label, style = MaterialTheme.typography.labelSmall, maxLines = 1); if (selected) Icon(Icons.Rounded.Check, null, Modifier.size(14.dp)) } } }
        }
    }
    if (choosingTheme) ModalBottomSheet(onDismissRequest = { choosingTheme = false }) {
        Text("Choose theme", Modifier.padding(horizontal = 24.dp, vertical = 8.dp), style = MaterialTheme.typography.titleLarge)
        LazyVerticalGrid(GridCells.Fixed(3), Modifier.fillMaxWidth().heightIn(max = 360.dp).padding(16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(routineThemeOptions) { option -> val selected = themeKey == option.key; val palette = routinePalette(option.key); Surface(Modifier.height(64.dp).clickable(role = Role.RadioButton) { themeKey = option.key; choosingTheme = false }.then(if (selected) Modifier.border(2.dp, palette.icon, MaterialTheme.shapes.medium) else Modifier), color = palette.container, shape = MaterialTheme.shapes.medium) { Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) { Box(Modifier.size(14.dp).background(palette.icon, CircleShape)); Text(option.label, Modifier.padding(start = 7.dp), style = MaterialTheme.typography.labelSmall) } } }
        }
    }
}

@Composable
private fun WeekdaySelector(daysMask: Int, onSelectionChanged: (DayOfWeek, Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        DayOfWeek.entries.forEach { day ->
            val selected = ScheduleRules.includes(daysMask, day)
            Surface(
                modifier = Modifier.weight(1f).heightIn(min = 48.dp).semantics { contentDescription = day.name.lowercase().replaceFirstChar(Char::titlecase) }
                    .toggleable(selected, role = Role.Checkbox) { onSelectionChanged(day, it) },
                color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                contentColor = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                shape = CircleShape,
                border = if (selected) null else androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            ) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(day.name.take(1), style = MaterialTheme.typography.labelLarge) }
            }
        }
    }
}

@Composable
private fun RoutinePreview(draft: RoutineDraft) {
    val palette = routinePalette(draft.themeKey); val target = draft.target ?: 0
    val secondary = RoutineRules.derivedMeasurement(0, target, draft.quantityPerCount, draft.measurementUnit)
    Surface(Modifier.fillMaxWidth(), color = palette.container, shape = MaterialTheme.shapes.medium, border = androidx.compose.foundation.BorderStroke(1.dp, palette.border)) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(42.dp).background(palette.container, MaterialTheme.shapes.small), contentAlignment = Alignment.Center) { Icon(routineIcon(draft.iconKey), null, tint = palette.icon) }
            Column(Modifier.weight(1f).padding(start = 12.dp)) { Text(draft.name.ifBlank { "Routine preview" }, style = MaterialTheme.typography.titleMedium); when (draft.type) { RoutineType.CHECK -> Text("Tap to complete", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant); RoutineType.DURATION -> Text("0 / $target min", color = MaterialTheme.colorScheme.onSurfaceVariant); RoutineType.COUNT -> { Text("0 / $target ${draft.unit.orEmpty()}", color = MaterialTheme.colorScheme.onSurfaceVariant); secondary?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) } } } }
            if (draft.type == RoutineType.CHECK) Box(Modifier.size(26.dp).border(1.5.dp, palette.icon, CircleShape))
        }
    }
}

private fun plainNumber(value: Double): String = java.math.BigDecimal.valueOf(value).stripTrailingZeros().toPlainString()
private fun reminderOffsetLabel(offset: Int) = when (offset) { 0 -> "At scheduled time"; 60 -> "1 hour before"; else -> "$offset minutes before" }
private val RoutineType.friendlyName get() = when (this) { RoutineType.CHECK -> "Done"; RoutineType.DURATION -> "Time"; RoutineType.COUNT -> "Quantity" }
private val RoutineType.description get() = when (this) { RoutineType.CHECK -> "Done / not done"; RoutineType.DURATION -> "Track time"; RoutineType.COUNT -> "Track quantity" }
private fun daySummary(mask: Int) = if (mask == EVERY_DAY) "Every day" else DayOfWeek.entries.filter { ScheduleRules.includes(mask, it) }.joinToString(" · ") { it.name.take(3).lowercase().replaceFirstChar(Char::titlecase) }
private fun reminderEditorSummary(enabled: Boolean, offset: Int, style: ReminderStyle) = if (!enabled) "Off" else "${reminderOffsetLabel(offset)} · ${if (style == ReminderStyle.PROMINENT) "Prominent" else "Alarm"}"
private fun scheduleTimeLabel(start: Int, end: Int?) = end?.let { "${ScheduleRules.formatTime(start)} – ${ScheduleRules.formatTime(it)}" } ?: ScheduleRules.formatTime(start)
