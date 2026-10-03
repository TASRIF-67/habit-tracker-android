package com.example.habittracker.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.example.habittracker.data.local.entity.PrayerReason
import com.example.habittracker.data.local.entity.PrayerStatus
import com.example.habittracker.data.repository.PrayerRepository
import com.example.habittracker.ui.theme.AppSpacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrayerStatusSheet(
    item: PrayerJourneyItem,
    onSelect: (PrayerStatus, Boolean) -> Unit,
    onClear: () -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).navigationBarsPadding().padding(horizontal = AppSpacing.xLarge).padding(bottom = AppSpacing.large)) {
            Text(item.habit.name, style = MaterialTheme.typography.headlineMedium)
            Text("How did it go?", Modifier.padding(top = AppSpacing.xSmall, bottom = AppSpacing.large), color = MaterialTheme.colorScheme.onSurfaceVariant)
            StatusOption("Completed", "Prayed normally", Icons.Outlined.CheckCircle, item.status == PrayerStatus.COMPLETED && !item.inJamaah) { onSelect(PrayerStatus.COMPLETED, false) }
            StatusOption("In Jama'ah", "Prayed in congregation", Icons.Outlined.Groups, item.status == PrayerStatus.COMPLETED && item.inJamaah) { onSelect(PrayerStatus.COMPLETED, true) }
            StatusOption("Qaza", "Made up later", Icons.Outlined.History, item.status == PrayerStatus.QAZA) { onSelect(PrayerStatus.QAZA, false) }
            StatusOption("Missed", "Not prayed", Icons.Outlined.RemoveCircleOutline, item.status == PrayerStatus.MISSED) { onSelect(PrayerStatus.MISSED, false) }
            if (item.status != PrayerStatus.UNRECORDED) {
                TextButton(onClick = onClear, Modifier.padding(top = AppSpacing.small).align(Alignment.End)) { Icon(Icons.Outlined.RestartAlt, null); Spacer(Modifier.width(6.dp)); Text("Clear record") }
            }
        }
    }
}

@Composable
private fun StatusOption(title: String, subtitle: String, icon: ImageVector, selected: Boolean, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).heightIn(min = 64.dp).selectable(selected = selected, role = Role.RadioButton, onClick = onClick),
        shape = MaterialTheme.shapes.medium,
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(Modifier.padding(AppSpacing.large), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
            Column(Modifier.weight(1f).padding(start = AppSpacing.large)) { Text(title, style = MaterialTheme.typography.titleMedium); Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            if (selected) Icon(Icons.Outlined.Check, "Current selection", tint = MaterialTheme.colorScheme.primary)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrayerReasonSheet(
    status: PrayerStatus,
    reasons: List<PrayerReason>,
    initialReasonId: Long?,
    onSave: (Long?) -> Unit,
    onAddCustom: (String, (Result<Long>) -> Unit) -> Unit,
    onDismiss: () -> Unit,
) {
    var selectedId by remember(initialReasonId) { mutableStateOf(initialReasonId) }
    var showCustomReason by remember { mutableStateOf(false) }
    val visibleReasons = remember(reasons, status) {
        if (status == PrayerStatus.QAZA) reasons.filterNot { it.name in setOf("No suitable place", "Laziness / procrastination") } else reasons
    }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().imePadding().navigationBarsPadding().padding(horizontal = AppSpacing.xLarge).padding(bottom = AppSpacing.medium)) {
            Text(if (status == PrayerStatus.MISSED) "Why was it missed?" else "What caused the delay?", style = MaterialTheme.typography.titleLarge)
            Text("Optional — you can skip this.", Modifier.padding(top = AppSpacing.xSmall, bottom = AppSpacing.medium), color = MaterialTheme.colorScheme.onSurfaceVariant)
            LazyColumn(Modifier.heightIn(max = 330.dp)) {
                items(visibleReasons, key = { it.id }) { reason ->
                    Row(Modifier.fillMaxWidth().clickable { selectedId = if (selectedId == reason.id) null else reason.id }.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selectedId == reason.id, { selectedId = reason.id }); Text(reason.name, Modifier.padding(start = 6.dp))
                    }
                }
            }
            TextButton(onClick = { showCustomReason = true }) { Icon(Icons.Outlined.Add, null); Spacer(Modifier.width(6.dp)); Text("Add custom reason") }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = { onSave(null) }) { Text("Skip") }
                Button(onClick = { onSave(selectedId) }) { Text("Save") }
            }
        }
    }
    if (showCustomReason) CustomReasonDialog(onDismiss = { showCustomReason = false }) { name, report ->
        onAddCustom(name) { result ->
            result.onSuccess { selectedId = it; showCustomReason = false }
            result.onFailure { report(it.message ?: "Could not add reason") }
        }
    }
}

@Composable
private fun CustomReasonDialog(onDismiss: () -> Unit, onAdd: (String, (String) -> Unit) -> Unit) {
    var name by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    val valid = name.trim().isNotEmpty() && name.trim().length <= PrayerRepository.MAX_REASON_LENGTH
    AlertDialog(
        modifier = Modifier.imePadding(),
        onDismissRequest = onDismiss,
        title = { Text("Add custom reason") },
        text = { OutlinedTextField(name, { if (it.length <= PrayerRepository.MAX_REASON_LENGTH) { name = it; error = null } }, label = { Text("Reason") }, singleLine = true, isError = error != null, supportingText = { Text(error ?: "${name.length}/${PrayerRepository.MAX_REASON_LENGTH}") }) },
        confirmButton = { TextButton(onClick = { onAdd(name) { error = it } }, enabled = valid) { Text("Add") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
