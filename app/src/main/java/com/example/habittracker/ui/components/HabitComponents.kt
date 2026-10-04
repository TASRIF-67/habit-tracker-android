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
import com.example.habittracker.data.local.entity.RoutineType
import com.example.habittracker.data.RoutineRules
import com.example.habittracker.ui.theme.*

@Composable
fun HabitRow(habit: HabitWithStatus, enabled: Boolean = true, showLeadingIcon: Boolean = true, onClick: () -> Unit = {}) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val rowScale by animateFloatAsState(if (pressed) .985f else 1f, spring(stiffness = 700f), label = "habit press")
    val contentColor by animateColorAsState(if (habit.completed) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface, label = "habit color")
    val identity = if (habit.category == HabitCategory.PERSONAL) routinePalette(habit.themeKey) else null
    Row(
        Modifier.fillMaxWidth().heightIn(min = AppSizes.habitRow).scale(rowScale).clickable(enabled = enabled, interactionSource = interaction, indication = null, role = Role.Checkbox, onClick = onClick).padding(horizontal = AppSpacing.xSmall),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (showLeadingIcon) {
            Box(Modifier.size(AppSizes.iconContainer).background(identity?.container ?: MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.shapes.small), contentAlignment = Alignment.Center) {
                Icon(if (identity != null) routineIcon(habit.iconKey) else habitIcon(habit), null, Modifier.size(AppSizes.icon), tint = identity?.icon ?: MaterialTheme.colorScheme.secondary)
            }
            Spacer(Modifier.width(AppSpacing.medium))
        }
        Text(habit.name, Modifier.weight(1f), color = contentColor, fontWeight = if (habit.category == HabitCategory.SALAT) FontWeight.Medium else FontWeight.Normal)
        AnimatedCompletionControl(habit.completed)
    }
}

@Composable
fun RoutineProgressRow(habit: HabitWithStatus, celebrating: Boolean = false, onLogTime: () -> Unit = {}, onStart: () -> Unit = {}, onAdjustCount: (Int) -> Unit = {}) {
    val target = habit.target ?: 1
    val unit = habit.unit.orEmpty()
    val palette = routinePalette(habit.themeKey)
    val secondaryMeasurement = RoutineRules.derivedMeasurement(habit.value, target, habit.quantityPerCount, habit.measurementUnit)
    val glow by animateColorAsState(if (celebrating) MaterialTheme.colorScheme.primaryContainer.copy(alpha = .7f) else androidx.compose.ui.graphics.Color.Transparent, label = "routine completion glow")
    Column(Modifier.fillMaxWidth().background(glow, MaterialTheme.shapes.medium).padding(horizontal = AppSpacing.xSmall, vertical = AppSpacing.small)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(AppSizes.iconContainer).background(palette.container, MaterialTheme.shapes.small), contentAlignment = Alignment.Center) {
                Icon(routineIcon(habit.iconKey), null, Modifier.size(AppSizes.icon), tint = palette.icon)
            }
            Column(Modifier.weight(1f).padding(start = AppSpacing.medium)) {
                Text(habit.name.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }, style = MaterialTheme.typography.titleMedium)
                Text("${habit.value} / $target $unit", color = MaterialTheme.colorScheme.onSurfaceVariant)
                secondaryMeasurement?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                AppProgress(habit.value.toFloat() / target, Modifier.padding(top = AppSpacing.xSmall), palette.progress)
            }
            if (habit.routineType == RoutineType.DURATION) Column(horizontalAlignment = Alignment.End) { FilledTonalButton(onClick = onStart, contentPadding = PaddingValues(horizontal = 12.dp), modifier = Modifier.heightIn(min = 40.dp)) { Icon(Icons.Outlined.PlayArrow, null); Text("Start") }; TextButton(onClick = onLogTime, contentPadding = PaddingValues(horizontal = 8.dp), modifier = Modifier.heightIn(min = 40.dp)) { Icon(Icons.Outlined.Add, null); Text("Log") } }
            else Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { onAdjustCount(-1) }, enabled = habit.value > 0) { Icon(Icons.Outlined.Remove, "Decrease ${habit.name}") }
                Text(habit.value.toString(), Modifier.widthIn(min = 30.dp), style = MaterialTheme.typography.titleMedium, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                IconButton(onClick = { onAdjustCount(1) }) { Icon(Icons.Outlined.Add, "Increase ${habit.name}") }
            }
        }
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
fun AppProgress(value: Float, modifier: Modifier = Modifier, color: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.primary) {
    LinearProgressIndicator(progress = { value.coerceIn(0f, 1f) }, modifier = modifier.fillMaxWidth().height(AppSizes.progressTrack), color = color, trackColor = MaterialTheme.colorScheme.surfaceVariant, strokeCap = StrokeCap.Round, drawStopIndicator = {})
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
