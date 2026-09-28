package com.example.habittracker.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.habittracker.data.local.dao.HabitWithStatus
import com.example.habittracker.data.local.entity.HabitCategory

@Composable
fun HabitRow(habit: HabitWithStatus, enabled: Boolean = true, showLeadingIcon: Boolean = true, onClick: () -> Unit = {}) {
    val contentColor by animateColorAsState(if (habit.completed) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface, label = "habit color")
    val checkScale by animateFloatAsState(if (habit.completed) 1f else .92f, label = "check scale")
    Row(
        Modifier.fillMaxWidth().heightIn(min = 56.dp).clickable(enabled = enabled, role = Role.Checkbox, onClick = onClick).padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (showLeadingIcon) {
            Box(Modifier.size(36.dp).background(MaterialTheme.colorScheme.surfaceVariant, CircleShape), contentAlignment = Alignment.Center) {
                Icon(habitIcon(habit), null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.secondary)
            }
            Spacer(Modifier.width(14.dp))
        }
        Text(habit.name, Modifier.weight(1f), color = contentColor, fontWeight = if (habit.category == HabitCategory.SALAT) FontWeight.Medium else FontWeight.Normal)
        Icon(
            if (habit.completed) Icons.Outlined.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
            if (habit.completed) "Completed" else "Not completed",
            Modifier.size(25.dp).scale(checkScale),
            tint = if (habit.completed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
        )
    }
}

@Composable
fun SectionHeader(title: String, count: String? = null) {
    Row(Modifier.fillMaxWidth().padding(top = 26.dp, bottom = 6.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(title.uppercase(), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        count?.let { Text(it, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}

@Composable
fun AppProgress(value: Float, modifier: Modifier = Modifier) {
    LinearProgressIndicator(
        progress = { value.coerceIn(0f, 1f) },
        modifier = modifier.fillMaxWidth().height(7.dp),
        color = MaterialTheme.colorScheme.primary,
        trackColor = MaterialTheme.colorScheme.surfaceVariant,
        strokeCap = androidx.compose.ui.graphics.StrokeCap.Round,
    )
}

private fun habitIcon(habit: HabitWithStatus): ImageVector = when {
    habit.category == HabitCategory.SALAT -> Icons.Outlined.AccessTime
    habit.name.contains("Quran", true) || habit.name.contains("Read", true) -> Icons.AutoMirrored.Outlined.MenuBook
    habit.name.contains("Adhkar", true) -> Icons.Outlined.AutoAwesome
    habit.name.contains("Sadaqah", true) -> Icons.Outlined.VolunteerActivism
    habit.name.contains("Study", true) -> Icons.Outlined.School
    habit.name.contains("Exercise", true) -> Icons.Outlined.FitnessCenter
    else -> Icons.Outlined.Checklist
}
