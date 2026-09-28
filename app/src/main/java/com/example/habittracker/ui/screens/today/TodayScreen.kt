package com.example.habittracker.ui.screens.today

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import com.example.habittracker.data.local.dao.HabitWithStatus
import com.example.habittracker.data.local.entity.HabitCategory
import com.example.habittracker.ui.components.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun TodayScreen(habits: List<HabitWithStatus>, date: LocalDate, onToggle: (HabitWithStatus) -> Unit) {
    val done = habits.count { it.completed }
    val percentage = if (habits.isEmpty()) 0 else done * 100 / habits.size
    val haptics = LocalHapticFeedback.current
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(top = 22.dp, bottom = 28.dp)) {
        Text("Today", style = MaterialTheme.typography.headlineMedium)
        Text(date.format(DateTimeFormatter.ofPattern("EEEE, MMMM d")), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyLarge)
        Surface(Modifier.fillMaxWidth().padding(top = 22.dp), color = MaterialTheme.colorScheme.primaryContainer, shape = MaterialTheme.shapes.large) {
            Column(Modifier.padding(18.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("$done of ${habits.size} completed", style = MaterialTheme.typography.titleMedium)
                    Text("$percentage%", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                }
                AppProgress(if (habits.isEmpty()) 0f else done.toFloat() / habits.size, Modifier.padding(top = 13.dp))
            }
        }
        HabitCategory.entries.forEach { category ->
            val items = habits.filter { it.category == category }
            if (items.isNotEmpty()) {
                SectionHeader(category.label, if (category == HabitCategory.SALAT) "${items.count { it.completed }} / ${items.size}" else null)
                items.forEachIndexed { index, habit ->
                    HabitRow(habit, showLeadingIcon = category != HabitCategory.SALAT) {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        onToggle(habit)
                    }
                    if (index < items.lastIndex) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = .55f))
                }
            }
        }
    }
}

private val HabitCategory.label get() = when (this) { HabitCategory.SALAT -> "Salat"; HabitCategory.GOOD_DEED -> "Good deeds"; HabitCategory.PERSONAL -> "Personal" }
