package com.example.habittracker.ui.screens.settings

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material.icons.outlined.NotificationsOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.habittracker.data.ScheduleRules
import com.example.habittracker.data.SleepRules
import com.example.habittracker.data.local.entity.SleepPlan
import java.time.DayOfWeek

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SleepSettingsScreen(plan: SleepPlan?, onBack: () -> Unit, onSave: (SleepPlan, (Result<Unit>) -> Unit) -> Unit) {
    var draft by remember(plan) { mutableStateOf(plan ?: SleepPlan()) }
    var timeField by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val notificationsAllowed = Build.VERSION.SDK_INT < 33 || context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { TopAppBar(title = { Text("Sleep & bedtime", color = MaterialTheme.colorScheme.onSurface) }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Back") } }, colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)) },
        bottomBar = {
            Surface(color = MaterialTheme.colorScheme.surface, tonalElevation = 2.dp) {
                Column(Modifier.navigationBarsPadding().imePadding()) {
                    Button(
                        onClick = { onSave(draft) { result -> result.onSuccess { onBack() }.onFailure { error = it.message } } },
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp).heightIn(min = 52.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary),
                    ) { Text("Save sleep plan") }
                }
            }
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 12.dp)) {
            SettingSwitch("Enable sleep plan", draft.enabled) { draft = draft.copy(enabled = it); error = null }
            HorizontalDivider(Modifier.padding(vertical = 12.dp))
            TimeSetting("Bedtime", SleepRules.formatMinutes(draft.bedtimeMinutes)) { timeField = "bedtime" }
            TimeSetting("Wake up", SleepRules.formatMinutes(draft.wakeTimeMinutes)) { timeField = "wake" }
            Text("Days", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 20.dp, bottom = 8.dp))
            FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                DayOfWeek.entries.forEach { day ->
                    val selected = ScheduleRules.includes(draft.daysMask, day)
                    FilterChip(
                        selected = selected,
                        onClick = { val bit = ScheduleRules.dayBit(day); val updated = if (selected) draft.daysMask and bit.inv() else draft.daysMask or bit; if (updated != 0) draft = draft.copy(daysMask = updated) },
                        label = { Text(day.name.take(1)) },
                        modifier = Modifier.semantics { this.selected = selected },
                        colors = FilterChipDefaults.filterChipColors(containerColor = MaterialTheme.colorScheme.surface, labelColor = MaterialTheme.colorScheme.onSurface, selectedContainerColor = MaterialTheme.colorScheme.primaryContainer, selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer),
                    )
                }
            }
            Text("Selected days refer to the night you go to bed.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            HorizontalDivider(Modifier.padding(vertical = 18.dp))
            Text("Wind down", style = MaterialTheme.typography.titleLarge)
            SettingSwitch("Wind-down reminder", draft.windDownEnabled) { draft = draft.copy(windDownEnabled = it); error = null }
            if (draft.windDownEnabled) {
                Text("Remind me before bedtime", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    SleepRules.windDownOffsets.forEach { offset -> FilterChip(selected = draft.windDownOffsetMinutes == offset, onClick = { draft = draft.copy(windDownOffsetMinutes = offset) }, label = { Text(if (offset == 60) "1 hour" else "$offset min") }, colors = FilterChipDefaults.filterChipColors(containerColor = MaterialTheme.colorScheme.surface, labelColor = MaterialTheme.colorScheme.onSurface, selectedContainerColor = MaterialTheme.colorScheme.primaryContainer, selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer)) }
                }
            }
            SettingSwitch("Bedtime reminder", draft.bedtimeReminderEnabled) { draft = draft.copy(bedtimeReminderEnabled = it); error = null }
            if (!notificationsAllowed && (draft.windDownEnabled || draft.bedtimeReminderEnabled)) {
                ListItem(
                    headlineContent = { Text("Android notifications unavailable") },
                    supportingContent = { Text("Your sleep plan and reminder preferences will still be saved.") },
                    leadingContent = { Icon(Icons.Outlined.NotificationsOff, null) },
                    trailingContent = { TextButton(onClick = { context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}"))) }) { Text("Settings") } },
                    colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                )
            }
            Surface(Modifier.fillMaxWidth().padding(top = 20.dp), color = MaterialTheme.colorScheme.surfaceVariant, shape = MaterialTheme.shapes.large) {
                Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Outlined.Bedtime, null, tint = MaterialTheme.colorScheme.primary)
                    Text("${SleepRules.formatMinutes(draft.bedtimeMinutes)} → ${SleepRules.formatMinutes(draft.wakeTimeMinutes)}", style = MaterialTheme.typography.titleMedium)
                    Text("${SleepRules.formatDuration(SleepRules.plannedMinutes(draft).toLong() * 60_000)} planned", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 12.dp)) }
            Spacer(Modifier.height(12.dp))
        }
    }

    timeField?.let { field ->
        val initial = if (field == "bedtime") draft.bedtimeMinutes else draft.wakeTimeMinutes
        val state = rememberTimePickerState(initialHour = initial / 60, initialMinute = initial % 60)
        AlertDialog(onDismissRequest = { timeField = null }, title = { Text(if (field == "bedtime") "Bedtime" else "Wake up") }, text = { TimePicker(state) }, confirmButton = { TextButton(onClick = { val minutes = state.hour * 60 + state.minute; draft = if (field == "bedtime") draft.copy(bedtimeMinutes = minutes) else draft.copy(wakeTimeMinutes = minutes); timeField = null }) { Text("Set") } }, dismissButton = { TextButton(onClick = { timeField = null }) { Text("Cancel") } })
    }
}

@Composable private fun SettingSwitch(label: String, checked: Boolean, onChecked: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().heightIn(min = 56.dp).clickable(role = Role.Switch) { onChecked(!checked) }, verticalAlignment = Alignment.CenterVertically) { Text(label, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface); Switch(checked, onChecked, colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colorScheme.onPrimary, checkedTrackColor = MaterialTheme.colorScheme.primary, uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant, uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant, uncheckedBorderColor = MaterialTheme.colorScheme.outline)) }
}

@Composable private fun TimeSetting(label: String, value: String, onClick: () -> Unit) {
    ListItem(headlineContent = { Text(label, color = MaterialTheme.colorScheme.onSurface) }, trailingContent = { Text(value, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary) }, modifier = Modifier.clickable(onClick = onClick), colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.background))
}
