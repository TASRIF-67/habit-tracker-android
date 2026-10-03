package com.example.habittracker.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.habittracker.data.local.dao.HabitWithStatus
import com.example.habittracker.data.local.entity.HabitCategory
import com.example.habittracker.ui.theme.*

@Composable
fun HabitRow(habit: HabitWithStatus, enabled: Boolean = true, showLeadingIcon: Boolean = true, onClick: () -> Unit = {}) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val rowScale by animateFloatAsState(if (pressed) .985f else 1f, spring(stiffness = 700f), label = "habit press")
    val contentColor by animateColorAsState(if (habit.completed) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface, label = "habit color")
    Row(
        Modifier.fillMaxWidth().heightIn(min = AppSizes.habitRow).scale(rowScale).clickable(enabled = enabled, interactionSource = interaction, indication = null, role = Role.Checkbox, onClick = onClick).padding(horizontal = AppSpacing.xSmall),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (showLeadingIcon) {
            Box(Modifier.size(AppSizes.iconContainer).background(MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.shapes.small), contentAlignment = Alignment.Center) {
                Icon(habitIcon(habit), null, Modifier.size(AppSizes.icon), tint = MaterialTheme.colorScheme.secondary)
            }
            Spacer(Modifier.width(AppSpacing.medium))
        }
        Text(habit.name, Modifier.weight(1f), color = contentColor, fontWeight = if (habit.category == HabitCategory.SALAT) FontWeight.Medium else FontWeight.Normal)
        AnimatedCompletionControl(habit.completed)
    }
}

@Composable
fun AnimatedCompletionControl(completed: Boolean, modifier: Modifier = Modifier) {
    val background by animateColorAsState(if (completed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface, label = "completion background")
    val border by animateColorAsState(if (completed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline, label = "completion border")
    Box(modifier.size(28.dp).background(background, CircleShape), contentAlignment = Alignment.Center) {
        if (!completed) CanvasBorder(border)
        AnimatedVisibility(completed, enter = scaleIn() + fadeIn(), exit = fadeOut()) { Icon(Icons.Rounded.Check, "Completed", Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onPrimary) }
    }
}

@Composable
private fun CanvasBorder(color: androidx.compose.ui.graphics.Color) {
    androidx.compose.foundation.Canvas(Modifier.fillMaxSize()) { drawCircle(color, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5.dp.toPx())) }
}

@Composable
fun SectionHeader(title: String, count: String? = null) {
    Row(Modifier.fillMaxWidth().padding(top = AppSpacing.xxLarge, bottom = AppSpacing.small), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = MaterialTheme.typography.titleLarge)
        count?.let { Text(it, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary) }
    }
}

@Composable
fun AppProgress(value: Float, modifier: Modifier = Modifier) {
    LinearProgressIndicator(progress = { value.coerceIn(0f, 1f) }, modifier = modifier.fillMaxWidth().height(AppSizes.progressTrack), color = MaterialTheme.colorScheme.primary, trackColor = MaterialTheme.colorScheme.surfaceVariant, strokeCap = StrokeCap.Round)
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
