package com.example.habittracker.ui.screens.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.app.AlarmManager
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import com.example.habittracker.data.local.entity.Habit
import com.example.habittracker.data.local.entity.RoutineType
import com.example.habittracker.data.RoutineDraft
import com.example.habittracker.data.RoutineRules
import com.example.habittracker.ui.components.RoutineEditorDialog
import com.example.habittracker.ui.components.routineIcon
import com.example.habittracker.ui.components.routinePalette
import com.example.habittracker.preferences.ThemeMode
import com.example.habittracker.data.SleepRules
import com.example.habittracker.data.local.entity.SleepPlan
import com.example.habittracker.data.local.entity.PrayerTimeSettings

@Composable
fun SettingsScreen(theme: ThemeMode, sleepPlan: SleepPlan?, prayerTimes: PrayerTimeSettings?, onTheme: (ThemeMode) -> Unit, onSleepSettings: () -> Unit, onPrayerTimeSettings: () -> Unit) {
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
        item {
            SettingsHeader("Notifications", Icons.Outlined.Notifications)
            val allowed = Build.VERSION.SDK_INT < 33 || context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
            ListItem(headlineContent = { Text("Routine reminders") }, supportingContent = { Text(if (allowed) "Allowed" else "Not allowed") }, leadingContent = { Icon(if (allowed) Icons.Outlined.NotificationsActive else Icons.Outlined.NotificationsOff, null) }, trailingContent = { if (!allowed) TextButton(onClick = { context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}"))) }) { Text("Settings") } }, colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.background))
            val exactAllowed = Build.VERSION.SDK_INT < 31 || context.getSystemService(AlarmManager::class.java).canScheduleExactAlarms()
            ListItem(headlineContent = { Text("Alarm capability") }, supportingContent = { Text(if (exactAllowed) "Precise alarms available" else "Using inexact alarm fallback") }, leadingContent = { Icon(Icons.Outlined.Alarm, null) }, trailingContent = { if (!exactAllowed) TextButton(onClick = { context.startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${context.packageName}"))) }) { Text("Allow") } }, colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.background))
            TextButton(onClick = { context.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)) }) { Text("Manage Android notification settings") }
        }
        item {
            SettingsHeader("Prayer", Icons.Outlined.Mosque)
            ListItem(
                headlineContent = { Text("Prayer times & reminders") },
                supportingContent = { Text(prayerTimes?.locationLabel ?: "Set location and calculation method") },
                leadingContent = { Icon(Icons.Outlined.Schedule, null) },
                trailingContent = { Icon(Icons.Outlined.ChevronRight, "Open prayer time settings") },
                modifier = Modifier.clickable(onClick = onPrayerTimeSettings),
                colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.background),
            )
        }
        item {
            SettingsHeader("Sleep", Icons.Outlined.Bedtime)
            ListItem(
                headlineContent = { Text("Sleep & bedtime") },
                supportingContent = {
                    Text(if (sleepPlan?.enabled == true) "${SleepRules.formatMinutes(sleepPlan.bedtimeMinutes)} → ${SleepRules.formatMinutes(sleepPlan.wakeTimeMinutes)}${if (sleepPlan.windDownEnabled) " · Wind-down ${sleepPlan.windDownOffsetMinutes} min before" else ""}" else "Set up sleep & bedtime")
                },
                leadingContent = { Icon(Icons.Outlined.Bedtime, null) },
                trailingContent = { Icon(Icons.Outlined.ChevronRight, "Open sleep settings") },
                modifier = Modifier.clickable(onClick = onSleepSettings),
                colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.background),
            )
        }
        item {
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
}

private const val LINKED_IN_URL = "https://www.linkedin.com/in/imam-hasan-tasrif-38501b274/"

@Composable private fun SettingsHeader(text: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier = Modifier) { Row(modifier.padding(top = 26.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) { Icon(icon, null, tint = MaterialTheme.colorScheme.primary); Spacer(Modifier.width(10.dp)); Text(text, style = MaterialTheme.typography.titleLarge) } }
private val ThemeMode.label get() = name.lowercase().replaceFirstChar { it.uppercase() }
private val ThemeMode.description get() = when (this) { ThemeMode.SYSTEM -> "Follow device"; ThemeMode.LIGHT -> "Always light"; ThemeMode.DARK -> "Always dark" }
