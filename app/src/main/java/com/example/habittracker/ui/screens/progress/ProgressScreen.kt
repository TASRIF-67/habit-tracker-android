package com.example.habittracker.ui.screens.progress

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.habittracker.ui.components.AppProgress
import com.example.habittracker.ui.components.routineIcon
import com.example.habittracker.ui.components.routinePalette
import com.example.habittracker.data.local.entity.HabitCategory
import com.example.habittracker.viewmodel.ProgressState
import com.example.habittracker.viewmodel.prayerStats
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun ProgressScreen(state: ProgressState) {
    val prayerStats = state.prayerStats
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(top = 22.dp, bottom = 28.dp)) {
        Text("Progress", style = MaterialTheme.typography.headlineMedium)
        Text("Your consistency this month", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Surface(Modifier.fillMaxWidth().padding(top = 22.dp), color = MaterialTheme.colorScheme.primaryContainer, shape = MaterialTheme.shapes.large) {
            Column(Modifier.padding(20.dp)) {
                Text(state.month.month.getDisplayName(TextStyle.FULL, Locale.getDefault()), style = MaterialTheme.typography.titleMedium)
                Row(Modifier.fillMaxWidth().padding(top = 4.dp), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("${state.percentage}%", style = MaterialTheme.typography.displayMedium, color = MaterialTheme.colorScheme.primary)
                    Text("${state.primaryCompletionCount} core completions", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 8.dp))
                }
                AppProgress(state.percentage / 100f, Modifier.padding(top = 10.dp))
                Text("Across ${state.elapsedDays} days so far", Modifier.padding(top = 10.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Text("Prayer records", Modifier.padding(top = 24.dp, bottom = 8.dp), style = MaterialTheme.typography.titleMedium)
        Surface(Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.surfaceVariant, shape = MaterialTheme.shapes.medium) {
            Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Stat("Completed", prayerStats.completed); Stat("Jama'ah", prayerStats.jamaah); Stat("Qaza", prayerStats.qaza); Stat("Missed", prayerStats.missed)
            }
        }
        Text("By habit", Modifier.padding(top = 28.dp, bottom = 4.dp), style = MaterialTheme.typography.titleLarge)
        if (state.availableHabits.isEmpty()) Text("Your habit statistics will appear here.", Modifier.padding(top = 16.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
        state.availableHabits.forEach { habit ->
            val percent = state.percentageFor(habit); val streak = state.streakFor(habit)
            val palette = routinePalette(habit.themeKey)
            Column(Modifier.padding(top = 18.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { Row(verticalAlignment = Alignment.CenterVertically) { if (habit.category == HabitCategory.PERSONAL) { Icon(routineIcon(habit.iconKey), null, Modifier.size(22.dp), tint = palette.icon); Spacer(Modifier.width(8.dp)) }; Text(habit.name, style = MaterialTheme.typography.titleMedium) }; Text("$percent%", color = if (habit.category == HabitCategory.PERSONAL) palette.progress else MaterialTheme.colorScheme.primary) }
                AppProgress(percent / 100f, Modifier.padding(top = 9.dp), if (habit.category == HabitCategory.PERSONAL) palette.progress else MaterialTheme.colorScheme.primary)
                Row(Modifier.padding(top = 7.dp), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Outlined.LocalFireDepartment, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant); Spacer(Modifier.width(5.dp)); Text(if (streak == 1) "1 day current streak" else "$streak day current streak", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
        }
    }
}

@Composable private fun Stat(label: String, value: Int) { Column(horizontalAlignment = Alignment.CenterHorizontally) { Text(value.toString(), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary); Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) } }
