package com.example.habittracker.ui.screens.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import com.example.habittracker.data.local.entity.Habit
import com.example.habittracker.preferences.ThemeMode

@Composable
fun SettingsScreen(habits: List<Habit>, theme: ThemeMode, onAdd: (String) -> Unit, onEdit: (Habit, String) -> Unit, onArchive: (Habit, Boolean) -> Unit, onTheme: (ThemeMode) -> Unit) {
    var editing by remember { mutableStateOf<Habit?>(null) }
    var adding by remember { mutableStateOf(false) }
    var archiving by remember { mutableStateOf<Habit?>(null) }
    val custom = habits.filter { !it.isBuiltIn }
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val versionName = remember(context) {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "1.0"
    }
    androidx.compose.foundation.lazy.LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 22.dp, bottom = 32.dp)) {
        item { Text("Settings", style = MaterialTheme.typography.headlineMedium); Text("Habits and appearance", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        item { SettingsHeader("Appearance", Icons.Outlined.Palette) }
        items(ThemeMode.entries.size) { index ->
            val mode = ThemeMode.entries[index]
            Row(Modifier.fillMaxWidth().heightIn(min = 52.dp).clickable { onTheme(mode) }, verticalAlignment = Alignment.CenterVertically) {
                RadioButton(theme == mode, { onTheme(mode) }); Text(mode.label, Modifier.weight(1f)); Text(mode.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        item { HorizontalDivider(Modifier.padding(top = 18.dp)); Row(Modifier.fillMaxWidth().padding(top = 18.dp), verticalAlignment = Alignment.CenterVertically) { SettingsHeader("Personal habits", Icons.Outlined.Checklist, Modifier.weight(1f)); FilledTonalButton(onClick = { adding = true }) { Icon(Icons.Outlined.Add, null); Spacer(Modifier.width(6.dp)); Text("Add") } } }
        if (custom.isEmpty()) item { Surface(Modifier.fillMaxWidth().padding(top = 12.dp), color = MaterialTheme.colorScheme.surfaceVariant, shape = MaterialTheme.shapes.medium) { Text("No personal habits yet. Add one to include it in your daily checklist.", Modifier.padding(18.dp), color = MaterialTheme.colorScheme.onSurfaceVariant) } }
        items(custom.size, key = { custom[it].id }) { index ->
            val habit = custom[index]
            ListItem(headlineContent = { Text(habit.name) }, supportingContent = { Text(if (habit.active) "Active" else "Archived") }, leadingContent = { Icon(if (habit.active) Icons.Outlined.CheckCircle else Icons.Outlined.Inventory2, null) }, trailingContent = {
                Row { IconButton(onClick = { editing = habit }) { Icon(Icons.Outlined.Edit, "Rename ${habit.name}") }; IconButton(onClick = { if (habit.active) archiving = habit else onArchive(habit, false) }) { Icon(if (habit.active) Icons.Outlined.Archive else Icons.Outlined.Unarchive, if (habit.active) "Archive ${habit.name}" else "Restore ${habit.name}") } }
            }, colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.background))
            if (index < custom.lastIndex) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = .55f))
        }
        item {
            Text("Built-in Salat and good-deed habits are protected and cannot be archived.", Modifier.padding(top = 22.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            HorizontalDivider(Modifier.padding(top = 24.dp))
            SettingsHeader("About", Icons.Outlined.Info)
            ListItem(
                headlineContent = { Text("HabitTracker") },
                supportingContent = { Text("Version $versionName") },
                leadingContent = { Icon(Icons.Outlined.CheckCircle, null) },
                colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.background),
            )
            ListItem(
                headlineContent = { Text("Md. Imam Hasan") },
                supportingContent = { Text("Developed by") },
                leadingContent = { Icon(Icons.Outlined.Person, null) },
                colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.background),
            )
            ListItem(
                headlineContent = { Text("LinkedIn") },
                supportingContent = { Text("View developer profile") },
                leadingContent = { Icon(Icons.Outlined.Link, null) },
                trailingContent = { Icon(Icons.AutoMirrored.Outlined.OpenInNew, "Opens externally") },
                modifier = Modifier.clickable { uriHandler.openUri(LINKED_IN_URL) },
                colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.background),
            )
        }
    }
    if (adding) NameDialog("Create habit", "", "Create", { adding = false }) { onAdd(it); adding = false }
    editing?.let { habit -> NameDialog("Rename habit", habit.name, "Save", { editing = null }) { onEdit(habit, it); editing = null } }
    archiving?.let { habit -> AlertDialog(onDismissRequest = { archiving = null }, icon = { Icon(Icons.Outlined.Archive, null) }, title = { Text("Archive ${habit.name}?") }, text = { Text("It will leave today’s checklist. Its completion history will be preserved.") }, confirmButton = { TextButton(onClick = { onArchive(habit, true); archiving = null }) { Text("Archive") } }, dismissButton = { TextButton(onClick = { archiving = null }) { Text("Cancel") } }) }
}

private const val LINKED_IN_URL = "https://www.linkedin.com/in/imam-hasan-tasrif-38501b274/"

@Composable private fun SettingsHeader(text: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier = Modifier) { Row(modifier.padding(top = 26.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) { Icon(icon, null, tint = MaterialTheme.colorScheme.primary); Spacer(Modifier.width(10.dp)); Text(text, style = MaterialTheme.typography.titleLarge) } }
private val ThemeMode.label get() = name.lowercase().replaceFirstChar { it.uppercase() }
private val ThemeMode.description get() = when (this) { ThemeMode.SYSTEM -> "Follow device"; ThemeMode.LIGHT -> "Always light"; ThemeMode.DARK -> "Always dark" }

@Composable
private fun NameDialog(title: String, initial: String, action: String, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var name by remember(initial) { mutableStateOf(initial) }
    val valid = name.trim().isNotEmpty() && name.trim().length <= 40
    AlertDialog(onDismissRequest = onDismiss, title = { Text(title) }, text = { OutlinedTextField(name, { if (it.length <= 40) name = it }, label = { Text("Habit name") }, supportingText = { Text("${name.length}/40") }, isError = name.isNotEmpty() && !valid, singleLine = true, keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done), keyboardActions = KeyboardActions(onDone = { if (valid) onSave(name.trim()) })) }, confirmButton = { TextButton(onClick = { onSave(name.trim()) }, enabled = valid) { Text(action) } }, dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } })
}
