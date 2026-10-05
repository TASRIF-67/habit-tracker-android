package com.example.habittracker.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import com.example.habittracker.R
import com.example.habittracker.data.TimeOfDayVisualResolver
import com.example.habittracker.data.TimeOfDayVisualState
import com.example.habittracker.data.local.dao.HabitWithStatus
import com.example.habittracker.data.local.dao.PrayerRecordDetails
import com.example.habittracker.data.local.entity.Prayer
import com.example.habittracker.data.local.entity.PrayerStatus
import com.example.habittracker.data.local.entity.RoutineSchedule
import com.example.habittracker.data.local.entity.ActivitySession
import com.example.habittracker.data.local.entity.ActivitySessionStatus
import com.example.habittracker.data.local.entity.RoutineType
import com.example.habittracker.data.local.entity.PrayerTimeSettings
import com.example.habittracker.data.PrayerTimeRules
import com.example.habittracker.data.local.entity.elapsedMillis
import com.example.habittracker.data.ScheduleRules
import com.example.habittracker.ui.theme.*
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.delay

@Composable
fun TimeOfDayHero(date: LocalDate, timeState: TimeOfDayVisualState) {
    val shimmer = remember { Animatable(0f) }
    LaunchedEffect(timeState) {
        while (true) {
            delay(3_600)
            shimmer.animateTo(1f, tween(750))
            shimmer.animateTo(0f, tween(900))
        }
    }
    Box(Modifier.fillMaxWidth().height(200.dp)) {
        PrayerVisual(timeState, Modifier.matchParentSize(), shimmer.value)
        Box(Modifier.matchParentSize().background(Brush.verticalGradient(listOf(Color(0x66000000), Color.Transparent, Color(0x55000000)))))
        Column(Modifier.windowInsetsPadding(WindowInsets.statusBars.only(WindowInsetsSides.Top)).padding(horizontal = AppSpacing.xLarge, vertical = AppSpacing.medium)) {
            Text(TimeOfDayVisualResolver.greeting(timeState), style = MaterialTheme.typography.headlineMedium, color = NightText)
            Text(date.format(DateTimeFormatter.ofPattern("EEEE, d MMMM")), style = MaterialTheme.typography.bodyLarge, color = NightText.copy(alpha = if (timeState == TimeOfDayVisualState.MORNING) .96f else .9f))
        }
    }
}

@Composable
fun DailyProgressCard(completed: Int, total: Int, celebrating: Boolean, modifier: Modifier = Modifier) {
    val target = if (total == 0) 0f else completed.toFloat() / total
    val animatedProgress by animateFloatAsState(target, tween(AppMotion.emphasized, easing = FastOutSlowInEasing), label = "daily progress")
    val percentage = (animatedProgress * 100).toInt()
    val glow by animateColorAsState(if (celebrating) MaterialTheme.colorScheme.primaryContainer.copy(alpha = .9f) else MaterialTheme.colorScheme.surface, tween(AppMotion.emphasized), label = "daily completion glow")
    Surface(modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large, color = glow, border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant), tonalElevation = 3.dp, shadowElevation = 2.dp) {
        BoxWithConstraints(Modifier.padding(AppSpacing.large)) {
            val compact = maxWidth < 280.dp
            if (compact) Column(horizontalAlignment = Alignment.CenterHorizontally) {
                DailyProgressRing(animatedProgress, percentage, celebrating)
                ProgressMessage(completed, total, percentage, Modifier.padding(top = AppSpacing.medium).fillMaxWidth())
            } else Row(verticalAlignment = Alignment.CenterVertically) {
                DailyProgressRing(animatedProgress, percentage, celebrating)
                ProgressMessage(completed, total, percentage, Modifier.weight(1f).padding(start = AppSpacing.large))
            }
        }
    }
}

@Composable
private fun DailyProgressRing(progress: Float, percentage: Int, celebrating: Boolean) {
    val primary = MaterialTheme.colorScheme.primary
    val track = MaterialTheme.colorScheme.outlineVariant.copy(alpha = .82f)
    Box(Modifier.size(112.dp).semantics { contentDescription = "Today's progress, $percentage percent" }, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = 10.dp.toPx()
            drawArc(track, -90f, 360f, false, style = androidx.compose.ui.graphics.drawscope.Stroke(stroke, cap = StrokeCap.Round), topLeft = Offset(stroke / 2, stroke / 2), size = Size(size.width - stroke, size.height - stroke))
            if (progress > 0f) drawArc(primary, -90f, 360f * progress.coerceIn(0f, 1f), false, style = androidx.compose.ui.graphics.drawscope.Stroke(stroke, cap = StrokeCap.Round), topLeft = Offset(stroke / 2, stroke / 2), size = Size(size.width - stroke, size.height - stroke))
            val growth = if (celebrating) 1f else .28f + progress * .72f
            val branches = when { progress >= .75f -> 4; progress >= .5f -> 3; progress >= .25f -> 2; else -> 1 }
            val stemStart = Offset(size.width * .12f, size.height * .86f)
            val stemEnd = Offset(size.width * (.12f + .31f * growth), size.height * (.86f - .42f * growth))
            drawLine(primary.copy(alpha = .74f), stemStart, stemEnd, 2.dp.toPx(), cap = StrokeCap.Round)
            val leaves = listOf(
                Triple(.12f, .72f, -.18f), Triple(.25f, .63f, .18f),
                Triple(.19f, .53f, -.2f), Triple(.34f, .45f, .2f),
            )
            leaves.take(branches).forEachIndexed { index, (x, y, direction) ->
                val leafScale = (growth * (1f - index * .05f)).coerceAtLeast(.2f)
                drawOval(primary.copy(alpha = .48f + index * .1f), Offset(size.width * (x + direction * .04f), size.height * y), Size(15.dp.toPx() * leafScale, 8.dp.toPx() * leafScale))
            }
        }
        Text("$percentage%", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun ProgressMessage(completed: Int, total: Int, percentage: Int, modifier: Modifier) {
    Column(modifier.semantics { contentDescription = "Today's progress, $percentage percent, $completed of $total completed" }) {
        Text("Today's progress", style = MaterialTheme.typography.titleLarge)
        Text("$completed of $total completed", Modifier.padding(top = AppSpacing.xSmall), color = MaterialTheme.colorScheme.onSurfaceVariant)
        Surface(Modifier.padding(top = AppSpacing.medium), color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = .7f), shape = MaterialTheme.shapes.medium) {
            Text(progressMessage(percentage), Modifier.padding(horizontal = AppSpacing.medium, vertical = AppSpacing.small), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSecondaryContainer)
        }
    }
}

private fun progressMessage(percentage: Int) = when (percentage) { 0 -> "Start with one small step."; in 1..49 -> "Every small step is progress."; in 50..79 -> "You're building momentum."; in 80..99 -> "Almost there for today."; else -> "Today's routine is complete." }

data class PrayerJourneyItem(val habit: HabitWithStatus, val prayer: Prayer, val record: PrayerRecordDetails?) {
    val status: PrayerStatus get() = record?.status ?: PrayerStatus.UNRECORDED
    val inJamaah: Boolean get() = record?.inJamaah == true
    val countsAsCompleted: Boolean get() = status.countsAsCompleted
}

data class PrayerFeedback(val prayerName: String, val message: String, val xpGranted: Int = 0)

@Composable
fun PrayerTimesSetupCard(onSetup: () -> Unit) {
    Surface(Modifier.fillMaxWidth().padding(bottom = AppSpacing.small), shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = .55f)) {
        Row(Modifier.padding(horizontal = AppSpacing.medium, vertical = AppSpacing.small), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.Schedule, null, tint = MaterialTheme.colorScheme.primary)
            Column(Modifier.weight(1f).padding(horizontal = AppSpacing.small)) {
                Text("Prayer times aren't set up", style = MaterialTheme.typography.titleSmall)
                Text("Add your location to see calculated times.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            TextButton(onClick = onSetup) { Text("Set up") }
        }
    }
}

@Composable
fun NextPrayerCard(settings: PrayerTimeSettings, onEdit: () -> Unit) {
    var now by remember { mutableStateOf(java.time.ZonedDateTime.now()) }
    LaunchedEffect(settings) { while (true) { now = java.time.ZonedDateTime.now(); delay(30_000) } }
    val next = remember(settings, now.minute, now.dayOfYear, now.year, now.zone) { runCatching { PrayerTimeRules.nextPrayer(settings, now) }.getOrNull() } ?: return
    Surface(Modifier.fillMaxWidth().padding(bottom = AppSpacing.small), shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = .62f)) {
        Row(Modifier.padding(horizontal = AppSpacing.medium, vertical = AppSpacing.small), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.Schedule, null, tint = MaterialTheme.colorScheme.primary)
            Column(Modifier.weight(1f).padding(horizontal = AppSpacing.small)) {
                Text("Next prayer", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("${next.prayer.displayName} · ${PrayerTimeRules.formatTime(next.time)}", style = MaterialTheme.typography.titleMedium, maxLines = 2)
                Text((if (next.tomorrow) "Tomorrow · " else "") + PrayerTimeRules.countdown(now, next.time), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
            }
            IconButton(onClick = onEdit) { Icon(Icons.Outlined.Tune, "Edit prayer time settings") }
        }
    }
}

@Composable
fun PrayerJourney(prayers: List<PrayerJourneyItem>, celebratingJamaah: Prayer?, times: Map<Prayer, java.time.ZonedDateTime>? = null, onPrayerClick: (PrayerJourneyItem) -> Unit) {
    val completed = prayers.count { it.countsAsCompleted }
    val incompleteConnector = MaterialTheme.colorScheme.outlineVariant
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        tonalElevation = 1.dp,
    ) {
        Column(Modifier.padding(horizontal = AppSpacing.medium, vertical = AppSpacing.medium)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Prayer journey", style = MaterialTheme.typography.titleMedium)
                    Text("$completed / ${prayers.size}", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                }
                Box(Modifier.fillMaxWidth().padding(top = AppSpacing.small)) {
                    Canvas(Modifier.fillMaxWidth().height(32.dp).padding(horizontal = 20.dp)) {
                        val y = size.height / 2
                        val segment = size.width / 4
                        for (index in 0 until 4) {
                            drawLine(
                                color = if (index < completed) Forest else incompleteConnector,
                                start = Offset(segment * index, y),
                                end = Offset(segment * (index + 1), y),
                                strokeWidth = 4.dp.toPx(),
                                cap = StrokeCap.Round,
                            )
                        }
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        prayers.forEach { prayer -> PrayerNode(prayer, celebratingJamaah == prayer.prayer, times?.get(prayer.prayer), onPrayerClick) }
                    }
                }
        }
    }
}

@Composable
private fun RowScope.PrayerNode(item: PrayerJourneyItem, celebratingJamaah: Boolean, time: java.time.ZonedDateTime?, onClick: (PrayerJourneyItem) -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) .88f else 1f, spring(stiffness = Spring.StiffnessMedium), label = "prayer press")
    val backgroundTarget = when { item.inJamaah -> MaterialTheme.colorScheme.primaryContainer; item.status == PrayerStatus.COMPLETED -> MaterialTheme.colorScheme.primary; item.status == PrayerStatus.QAZA -> Color(0xFFB27A36); item.status == PrayerStatus.MISSED -> MaterialTheme.colorScheme.surfaceVariant; else -> MaterialTheme.colorScheme.surface }
    val foregroundTarget = when { item.inJamaah -> MaterialTheme.colorScheme.primary; item.status == PrayerStatus.COMPLETED || item.status == PrayerStatus.QAZA -> MaterialTheme.colorScheme.onPrimary; else -> MaterialTheme.colorScheme.onSurfaceVariant }
    val background by animateColorAsState(backgroundTarget, tween(AppMotion.standard), label = "prayer state")
    val foreground by animateColorAsState(foregroundTarget, tween(AppMotion.standard), label = "prayer icon")
    val radiance = remember { Animatable(0f) }
    LaunchedEffect(celebratingJamaah) { if (celebratingJamaah) { radiance.snapTo(0f); radiance.animateTo(1f, tween(850)); radiance.animateTo(0f, tween(250)) } }
    Column(
        Modifier.weight(1f).clip(MaterialTheme.shapes.small).semantics(mergeDescendants = true) { contentDescription = "${item.habit.name}, ${item.accessibilityStateLabel}" }.clickable(interactionSource = interaction, indication = null, role = Role.Button) { onClick(item) }.padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.size(42.dp), contentAlignment = Alignment.Center) {
            if (radiance.value > 0f) Canvas(Modifier.fillMaxSize()) { val p = radiance.value; drawCircle(ForestLight.copy(alpha = .28f * (1f - p)), radius = size.minDimension * (.28f + p * .22f), style = androidx.compose.ui.graphics.drawscope.Stroke(2.dp.toPx())); repeat(4) { index -> val angle = index * Math.PI.toFloat() / 2; drawCircle(MoonCream.copy(alpha = 1f - p), 1.8.dp.toPx(), Offset(center.x + kotlin.math.cos(angle) * 20.dp.toPx() * p, center.y + kotlin.math.sin(angle) * 20.dp.toPx() * p)) } }
            Box(Modifier.size(36.dp).scale(scale).background(background, CircleShape).then(if (item.status == PrayerStatus.UNRECORDED) Modifier.border(1.5.dp, MaterialTheme.colorScheme.outline, CircleShape) else Modifier), contentAlignment = Alignment.Center) {
                when { item.inJamaah -> Icon(Icons.Outlined.Groups, null, Modifier.size(21.dp), tint = foreground); item.status == PrayerStatus.COMPLETED -> Icon(Icons.Rounded.Check, null, Modifier.size(21.dp), tint = foreground); item.status == PrayerStatus.QAZA -> Icon(Icons.Outlined.History, null, Modifier.size(20.dp), tint = foreground); item.status == PrayerStatus.MISSED -> Icon(Icons.Outlined.Close, null, Modifier.size(19.dp), tint = foreground); else -> Icon(Icons.Outlined.Remove, null, Modifier.size(20.dp), tint = foreground) }
            }
        }
        Text(item.habit.name, Modifier.padding(top = 3.dp), style = MaterialTheme.typography.labelSmall, maxLines = 1, textAlign = TextAlign.Center)
        if (time != null) Text(com.example.habittracker.data.PrayerTimeRules.formatTime(time), style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, lineHeight = 11.sp), color = MaterialTheme.colorScheme.primary, maxLines = 1, textAlign = TextAlign.Center)
        Text(item.stateLabel, style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, lineHeight = 11.sp), color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, textAlign = TextAlign.Center)
    }
}

@Composable
fun PrayerVisual(timeState: TimeOfDayVisualState, modifier: Modifier = Modifier, shimmer: Float = 0f) {
    Crossfade(timeState, modifier = modifier, animationSpec = tween(AppMotion.emphasized), label = "time of day sky") { state ->
        if (state == TimeOfDayVisualState.NIGHT) {
            Box(Modifier.fillMaxSize()) {
                Image(
                    painter = painterResource(R.drawable.night_hero_landscape),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    alignment = Alignment.Center,
                )
                Canvas(Modifier.fillMaxSize()) { drawNightAtmosphere(shimmer) }
            }
        } else {
            val description = "${state.name.lowercase().replaceFirstChar { it.uppercase() }} prayer scene"
            Canvas(Modifier.fillMaxSize().semantics { contentDescription = description }) { drawPrayerScene(state) }
        }
    }
}

private fun DrawScope.drawNightAtmosphere(shimmer: Float) {
    drawRect(Brush.verticalGradient(listOf(Color(0x4D001316), Color.Transparent, Color(0x26000A09))))
    val moonCenter = Offset(size.width * .8f, size.height * .36f)
    drawCircle(MoonCream.copy(alpha = .1f + .07f * shimmer), radius = (25 + 3 * shimmer).dp.toPx(), center = moonCenter)
    drawCircle(MoonCream.copy(alpha = .96f), radius = 12.dp.toPx(), center = moonCenter)
    listOf(.17f to .24f, .29f to .39f, .61f to .2f, .7f to .43f, .9f to .22f).forEachIndexed { index, (x, y) ->
        val pulse = if (index % 2 == 0) shimmer else 1f - shimmer
        drawCircle(MoonCream.copy(alpha = .42f + .38f * pulse), 1.15.dp.toPx(), Offset(size.width * x, size.height * y))
    }
}

private fun DrawScope.drawPrayerScene(state: TimeOfDayVisualState) {
    val sky = when (state) { TimeOfDayVisualState.DAWN -> Color(0xFF405A78); TimeOfDayVisualState.MORNING -> Color(0xFFA8CFCA); TimeOfDayVisualState.DAY -> DaySky; TimeOfDayVisualState.AFTERNOON -> AfternoonSky; TimeOfDayVisualState.SUNSET -> SunsetSky; TimeOfDayVisualState.NIGHT -> Color(0xFF0A2635) }
    val horizon = when (state) { TimeOfDayVisualState.DAWN -> Color(0xFFB58B70); TimeOfDayVisualState.MORNING -> Color(0xFF6C9A79); TimeOfDayVisualState.DAY -> Color(0xFF53806D); TimeOfDayVisualState.AFTERNOON -> Color(0xFF6F7651); TimeOfDayVisualState.SUNSET -> Color(0xFF534C43); TimeOfDayVisualState.NIGHT -> Color(0xFF24584E) }
    drawRect(Brush.verticalGradient(listOf(sky, horizon.copy(alpha = .9f))))
    if (state != TimeOfDayVisualState.NIGHT) {
        if (state == TimeOfDayVisualState.DAWN) listOf(.13f to .2f, .67f to .17f).forEach { (x, y) -> drawCircle(MoonCream.copy(alpha = .5f), 1.2.dp.toPx(), Offset(size.width * x, size.height * y)) }
        val sunX = when (state) { TimeOfDayVisualState.DAWN -> .18f; TimeOfDayVisualState.MORNING -> .82f; TimeOfDayVisualState.DAY -> .6f; TimeOfDayVisualState.AFTERNOON -> .76f; else -> .84f }
        val sunY = when (state) { TimeOfDayVisualState.DAWN -> .72f; TimeOfDayVisualState.MORNING -> .34f; TimeOfDayVisualState.DAY -> .24f; TimeOfDayVisualState.AFTERNOON -> .43f; else -> .7f }
        drawCircle(MoonCream.copy(alpha = .9f), radius = 17.dp.toPx(), center = Offset(size.width * sunX, size.height * sunY))
    }
    if (state == TimeOfDayVisualState.MORNING) {
        val farHills = Path().apply { moveTo(0f, size.height * .59f); quadraticTo(size.width * .18f, size.height * .49f, size.width * .36f, size.height * .6f); quadraticTo(size.width * .58f, size.height * .47f, size.width * .78f, size.height * .58f); quadraticTo(size.width * .9f, size.height * .51f, size.width, size.height * .56f); lineTo(size.width, size.height); lineTo(0f, size.height); close() }
        drawPath(farHills, Color(0xFF86B7A6).copy(alpha = .52f))
    }
    val distant = Path().apply { moveTo(0f, size.height * .64f); quadraticTo(size.width * .2f, size.height * .46f, size.width * .4f, size.height * .65f); quadraticTo(size.width * .68f, size.height * .42f, size.width, size.height * .62f); lineTo(size.width, size.height); lineTo(0f, size.height); close() }
    drawPath(distant, if (state == TimeOfDayVisualState.NIGHT) Color(0xFF2D6258) else horizon.copy(alpha = .72f))
    val hills = Path().apply { moveTo(0f, size.height * .74f); quadraticTo(size.width * .22f, size.height * .57f, size.width * .45f, size.height * .77f); quadraticTo(size.width * .72f, size.height * .57f, size.width, size.height * .7f); lineTo(size.width, size.height); lineTo(0f, size.height); close() }
    drawPath(hills, horizon)
    val silhouette = if (state == TimeOfDayVisualState.NIGHT) Color(0xFF0A332D) else Color(0xFF16483B)
    val baseY = size.height * .9f
    drawRect(silhouette, Offset(size.width * .39f, baseY - 24.dp.toPx()), Size(size.width * .22f, 24.dp.toPx()))
    drawCircle(silhouette, 25.dp.toPx(), Offset(size.width * .5f, baseY - 24.dp.toPx()))
    drawRect(silhouette, Offset(size.width * .67f, baseY - 48.dp.toPx()), Size(7.dp.toPx(), 48.dp.toPx()))
    drawCircle(silhouette, 7.dp.toPx(), Offset(size.width * .67f + 3.5.dp.toPx(), baseY - 48.dp.toPx()))
    drawRect(silhouette, Offset(0f, baseY), Size(size.width, size.height - baseY))
}

@Composable
fun PrayerConfirmation(feedback: PrayerFeedback?, modifier: Modifier = Modifier) {
    AnimatedVisibility(feedback != null, modifier, enter = fadeIn(tween(AppMotion.standard)) + slideInVertically { it / 2 }, exit = fadeOut(tween(AppMotion.quick))) {
        Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = MaterialTheme.shapes.medium) {
            Row(Modifier.fillMaxWidth().padding(horizontal = AppSpacing.large, vertical = AppSpacing.medium), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column { Text(feedback?.prayerName.orEmpty(), style = MaterialTheme.typography.titleMedium); Text(feedback?.message.orEmpty(), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                if ((feedback?.xpGranted ?: 0) > 0) Text("+${feedback?.xpGranted} XP", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
fun FullPrayerCompletion(hasQaza: Boolean, timeState: TimeOfDayVisualState, onContinue: () -> Unit) {
    Dialog(onDismissRequest = onContinue, properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = false)) {
        var visible by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) { visible = true }
        AnimatedVisibility(visible, enter = fadeIn(tween(AppMotion.standard)) + scaleIn(tween(AppMotion.standard), initialScale = .94f)) {
            Surface(shape = MaterialTheme.shapes.extraLarge, color = Color(0xFF0B2927), contentColor = NightText) {
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    PrayerVisual(timeState, Modifier.fillMaxWidth().height(150.dp))
                    Text("5 / 5", style = MaterialTheme.typography.displaySmall, color = ForestLight)
                    Text("5 / 5 prayers completed", style = MaterialTheme.typography.titleLarge)
                    Text(if (hasQaza) "All prayers are now on record." else "A complete day.", Modifier.padding(top = AppSpacing.small), color = NightMuted)
                    Button(onClick = onContinue, Modifier.fillMaxWidth().padding(AppSpacing.xLarge), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F936F))) { Text("Continue") }
                }
            }
        }
    }
}

@Composable
fun GoodDeedRow(habit: HabitWithStatus, celebrating: Boolean, onClick: () -> Unit) {
    val growth = remember { Animatable(0f) }
    val glow by animateColorAsState(
        if (celebrating) MaterialTheme.colorScheme.secondaryContainer.copy(alpha = .72f) else Color.Transparent,
        tween(AppMotion.standard),
        label = "good deed glow",
    )
    LaunchedEffect(celebrating) {
        if (celebrating) {
            growth.snapTo(0f)
            growth.animateTo(1f, tween(720, easing = FastOutSlowInEasing))
            growth.animateTo(0f, tween(220))
        }
    }
    Box(Modifier.fillMaxWidth().clip(MaterialTheme.shapes.medium).background(glow)) {
        HabitRow(habit, onClick = onClick)
        if (growth.value > 0f) {
            BotanicalMotif(growth.value, Modifier.align(Alignment.CenterEnd).padding(end = 27.dp).size(58.dp, 42.dp))
        }
    }
}

@Composable
fun GoodDeedsSection(habits: List<HabitWithStatus>, celebratingId: Long?, onToggle: (HabitWithStatus) -> Unit) {
    Column {
        SectionHeader("Good deeds", "Optional")
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val columns = if (maxWidth >= 430.dp) 4 else 2
            Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.small)) {
                habits.chunked(columns).forEach { rowHabits ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(AppSpacing.small)) {
                        rowHabits.forEach { habit -> GoodDeedTile(habit, celebratingId == habit.id, Modifier.weight(1f)) { onToggle(habit) } }
                        repeat(columns - rowHabits.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun GoodDeedTile(habit: HabitWithStatus, celebrating: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) .96f else 1f, spring(stiffness = 700f), label = "good deed press")
    val dark = MaterialTheme.colorScheme.background.luminance() < .3f
    val base = if (dark) when {
        habit.name.contains("Morning", true) -> Color(0xFF293126)
        habit.name.contains("Evening", true) -> Color(0xFF203335)
        habit.name.contains("Sadaqah", true) -> Color(0xFF31272B)
        else -> Color(0xFF142A23)
    } else when {
        habit.name.contains("Morning", true) -> Color(0xFFF5E8CF)
        habit.name.contains("Evening", true) -> Color(0xFFDDE8E9)
        habit.name.contains("Sadaqah", true) -> Color(0xFFEEDFE2)
        else -> MaterialTheme.colorScheme.secondaryContainer
    }
    val surface by animateColorAsState(if (celebrating) MaterialTheme.colorScheme.primaryContainer else base, label = "good deed tile")
    Surface(modifier.scale(scale).heightIn(min = 84.dp).clickable(interactionSource = interaction, indication = null, role = Role.Checkbox, onClick = onClick), color = surface, shape = MaterialTheme.shapes.medium) {
        Box(Modifier.padding(AppSpacing.small)) {
            Column {
                Icon(goodDeedIcon(habit), null, tint = MaterialTheme.colorScheme.primary)
                Text(habit.name, Modifier.padding(top = AppSpacing.small), style = MaterialTheme.typography.labelLarge, maxLines = 2)
            }
            AnimatedCompletionControl(habit.completed, Modifier.align(Alignment.TopEnd))
            if (celebrating) BotanicalMotif(1f, Modifier.align(Alignment.BottomEnd).size(38.dp, 28.dp))
        }
    }
}

private fun goodDeedIcon(habit: HabitWithStatus) = when {
    habit.name.contains("Quran", true) -> Icons.AutoMirrored.Outlined.MenuBook
    habit.name.contains("Sadaqah", true) -> Icons.Outlined.FavoriteBorder
    else -> Icons.Outlined.AutoAwesome
}

@Composable
fun MyRoutinesSection(
    habits: List<HabitWithStatus>,
    celebratingId: Long?,
    onToggleCheck: (HabitWithStatus) -> Unit,
    onLogDuration: (HabitWithStatus) -> Unit,
    onStartDuration: (HabitWithStatus) -> Unit,
    onAdjustCount: (HabitWithStatus, Int) -> Unit,
) {
    Column {
        Row(Modifier.fillMaxWidth().padding(top = AppSpacing.xxLarge, bottom = AppSpacing.small), verticalAlignment = Alignment.CenterVertically) { Text("Today's routines", Modifier.weight(1f), style = MaterialTheme.typography.titleLarge) }
        if (habits.isEmpty()) {
            Surface(Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .55f), shape = MaterialTheme.shapes.large) {
                Row(Modifier.padding(AppSpacing.large), verticalAlignment = Alignment.CenterVertically) {
                    BotanicalMotif(1f, Modifier.size(64.dp, 52.dp))
                    Column(Modifier.weight(1f).padding(start = AppSpacing.medium)) { Text("No routines for today", style = MaterialTheme.typography.titleMedium); Text("Create and manage routines from the Routines tab.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                }
            }
        } else {
            Column(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface.copy(alpha = .55f), MaterialTheme.shapes.large).padding(horizontal = AppSpacing.medium)) {
                habits.forEachIndexed { index, habit ->
                    if (habit.routineType == com.example.habittracker.data.local.entity.RoutineType.CHECK) HabitRow(habit) { onToggleCheck(habit) }
                    else RoutineProgressRow(habit, celebratingId == habit.id, onLogTime = { onLogDuration(habit) }, onStart = { onStartDuration(habit) }, onAdjustCount = { onAdjustCount(habit, it) })
                    if (index < habits.lastIndex) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = .55f))
                }
            }
        }
    }
}

@Composable
fun UpNextRoutine(habit: HabitWithStatus, schedule: RoutineSchedule, occurrence: java.time.ZonedDateTime, now: java.time.ZonedDateTime, onStart: () -> Unit) {
    val palette = routinePalette(habit.themeKey)
    val scheduleLabel = upNextScheduleLabel(schedule, occurrence, now)
    val targetSummary = upNextTargetSummary(habit)
    Column {
        SectionHeader("Up next")
        Surface(
            Modifier.fillMaxWidth().semantics {
                contentDescription = listOfNotNull("Up next", habit.name, targetSummary, scheduleLabel).joinToString(", ")
            },
            color = palette.container,
            border = androidx.compose.foundation.BorderStroke(1.dp, palette.border),
            shape = MaterialTheme.shapes.large,
        ) {
            Row(Modifier.padding(AppSpacing.medium), verticalAlignment = Alignment.Top) {
                Icon(routineIcon(habit.iconKey), null, tint = palette.icon, modifier = Modifier.padding(top = 2.dp))
                BoxWithConstraints(Modifier.weight(1f).padding(start = AppSpacing.medium)) {
                    val fontScale = LocalDensity.current.fontScale
                    val stackHeader = maxWidth < 220.dp || fontScale >= 1.3f
                    val stackAction = maxWidth < 250.dp || fontScale >= 1.5f
                    Column {
                        if (stackHeader) {
                            Text(habit.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            targetSummary?.let {
                                Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        } else {
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Text(habit.name, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                targetSummary?.let {
                                    Spacer(Modifier.width(AppSpacing.medium))
                                    Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                                }
                            }
                        }
                        if (stackAction && habit.routineType == RoutineType.DURATION) {
                            Text(scheduleLabel, Modifier.padding(top = 2.dp), style = MaterialTheme.typography.labelLarge, color = palette.icon)
                            FilledTonalButton(onClick = onStart, modifier = Modifier.align(Alignment.End).padding(top = AppSpacing.small)) {
                                Icon(Icons.Outlined.PlayArrow, null)
                                Text("Start")
                            }
                        } else {
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Text(scheduleLabel, Modifier.weight(1f), style = MaterialTheme.typography.labelLarge, color = palette.icon, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                if (habit.routineType == RoutineType.DURATION) {
                                    Spacer(Modifier.width(AppSpacing.small))
                                    FilledTonalButton(onClick = onStart) {
                                        Icon(Icons.Outlined.PlayArrow, null)
                                        Text("Start")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun upNextTargetSummary(habit: HabitWithStatus): String? = when (habit.routineType) {
    RoutineType.CHECK -> null
    RoutineType.DURATION -> habit.target?.let { "$it min" }
    RoutineType.COUNT -> habit.target?.let { target -> habit.unit?.trim()?.takeIf(String::isNotEmpty)?.let { "$target $it" } ?: target.toString() }
}

private fun upNextScheduleLabel(schedule: RoutineSchedule, occurrence: java.time.ZonedDateTime, now: java.time.ZonedDateTime): String {
    val day = when (java.time.temporal.ChronoUnit.DAYS.between(now.toLocalDate(), occurrence.toLocalDate())) {
        0L -> "Today"
        1L -> "Tomorrow"
        else -> occurrence.format(DateTimeFormatter.ofPattern("EEE"))
    }
    val startsIn = java.time.Duration.between(now, occurrence).toMinutes().coerceAtLeast(0).let { minutes -> when { minutes < 2 -> "Soon"; minutes < 60 -> "Starts in ${minutes}m"; else -> "Starts in ${minutes / 60}h ${minutes % 60}m" } }
    return "$day · ${ScheduleRules.timeRange(schedule)} · $startsIn"
}

@Composable
fun ScheduledActiveNowSection(block: com.example.habittracker.data.ActiveRoutineWindow, onStart: () -> Unit, onToggle: () -> Unit, onIncrement: () -> Unit) {
    val habit = block.habit
    val palette = routinePalette(habit.themeKey)
    val end = ScheduleRules.formatTime(block.window.end.hour * 60 + block.window.end.minute)
    Column {
        SectionHeader("Active now")
        Surface(Modifier.fillMaxWidth(), color = palette.container, border = androidx.compose.foundation.BorderStroke(1.dp, palette.border), shape = MaterialTheme.shapes.large) {
            Column(Modifier.padding(AppSpacing.medium)) {
                Row(verticalAlignment = Alignment.Top) {
                    Icon(routineIcon(habit.iconKey), null, tint = palette.icon, modifier = Modifier.padding(top = 2.dp))
                    Column(Modifier.weight(1f).padding(start = AppSpacing.medium)) {
                        Text(habit.name, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Text(ScheduleRules.timeRange(block.schedule), color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
                        Text("Until $end", style = MaterialTheme.typography.labelLarge, color = palette.icon)
                        if (habit.routineType != RoutineType.CHECK) Text(upNextTargetSummary(habit)?.let { "${habit.value} / $it" } ?: "", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
                    }
                }
                FlowRow(Modifier.fillMaxWidth().padding(top = AppSpacing.small), horizontalArrangement = Arrangement.End) {
                    when (habit.routineType) {
                        RoutineType.DURATION -> FilledTonalButton(onClick = onStart) { Icon(Icons.Outlined.PlayArrow, null); Text("Start") }
                        RoutineType.COUNT -> FilledTonalButton(onClick = onIncrement) { Text("+1") }
                        RoutineType.CHECK -> Checkbox(habit.completed, { onToggle() })
                    }
                }
            }
        }
    }
}

@Composable
fun ActiveNowSection(habit: HabitWithStatus, session: ActivitySession, onPause: () -> Unit, onResume: () -> Unit, onOpen: () -> Unit, onDiscard: () -> Unit) {
    val palette = routinePalette(habit.themeKey)
    var now by remember(session.id, session.status, session.resumedAt) { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(session.id, session.status, session.resumedAt) { while (session.status == ActivitySessionStatus.RUNNING) { delay(1_000); now = System.currentTimeMillis() } }
    val elapsed = session.elapsedMillis(now)
    val elapsedMinutes = (elapsed / 60_000L).toInt()
    val displayedProgress = habit.value + elapsedMinutes
    Column {
        SectionHeader("Active now")
        Surface(Modifier.fillMaxWidth().semantics { contentDescription = "Active now, ${habit.name}, ${if (session.status == ActivitySessionStatus.PAUSED) "paused" else "running"}, ${formatActivityElapsed(elapsed)} elapsed" }, color = palette.container, border = androidx.compose.foundation.BorderStroke(1.dp, palette.border), shape = MaterialTheme.shapes.large) {
            Column(Modifier.padding(AppSpacing.large)) {
                Row(verticalAlignment = Alignment.CenterVertically) { Icon(routineIcon(habit.iconKey), null, tint = palette.icon); Text(habit.name, Modifier.weight(1f).padding(start = AppSpacing.medium), style = MaterialTheme.typography.titleLarge, maxLines = 2, overflow = TextOverflow.Ellipsis); Spacer(Modifier.width(AppSpacing.small)); Text(formatActivityElapsed(elapsed), style = MaterialTheme.typography.titleLarge, color = palette.icon) }
                Text(if (session.status == ActivitySessionStatus.PAUSED) "Paused" else "Running", Modifier.padding(top = 4.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("$displayedProgress / ${habit.target ?: 0} min", Modifier.padding(top = AppSpacing.medium), color = MaterialTheme.colorScheme.onSurfaceVariant)
                AppProgress(displayedProgress.toFloat() / (habit.target ?: 1), Modifier.padding(top = AppSpacing.small), palette.progress)
                Row(Modifier.fillMaxWidth().padding(top = AppSpacing.medium).height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Button(onClick = if (session.status == ActivitySessionStatus.RUNNING) onPause else onResume, modifier = Modifier.weight(1f).fillMaxHeight().heightIn(min = 48.dp), contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)) { Text(if (session.status == ActivitySessionStatus.RUNNING) "Pause" else "Resume", maxLines = 2, textAlign = TextAlign.Center) }
                    OutlinedButton(onClick = onOpen, modifier = Modifier.weight(1f).fillMaxHeight().heightIn(min = 48.dp), contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)) { Text("Open session", maxLines = 2, textAlign = TextAlign.Center) }
                    OutlinedButton(onClick = onDiscard, modifier = Modifier.weight(1f).fillMaxHeight().heightIn(min = 48.dp), contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp), border = BorderStroke(1.dp, MaterialTheme.colorScheme.error), colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)) { Text("Discard", maxLines = 2, textAlign = TextAlign.Center) }
                }
            }
        }
    }
}

private fun formatActivityElapsed(millis: Long): String { val seconds = millis / 1000; val hours = seconds / 3600; val minutes = seconds % 3600 / 60; val secs = seconds % 60; return if (hours > 0) "$hours:${minutes.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}" else "${minutes.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}" }

@Composable
fun OptionalSectionCompletion(visible: Boolean) {
    AnimatedVisibility(visible, enter = fadeIn(tween(AppMotion.standard)) + expandVertically(tween(AppMotion.standard)), exit = fadeOut(tween(AppMotion.quick)) + shrinkVertically()) {
        Surface(Modifier.fillMaxWidth().padding(top = AppSpacing.medium), color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = .7f), shape = MaterialTheme.shapes.medium) {
            Row(Modifier.padding(horizontal = AppSpacing.large, vertical = AppSpacing.medium), verticalAlignment = Alignment.CenterVertically) {
                BotanicalMotif(1f, Modifier.size(46.dp, 34.dp))
                Spacer(Modifier.width(AppSpacing.medium))
                Column { Text("A little extra for today", style = MaterialTheme.typography.titleMedium); Text("All optional practices completed", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
        }
    }
}

@Composable
private fun BotanicalMotif(progress: Float, modifier: Modifier = Modifier) {
    val leafColor = MaterialTheme.colorScheme.primary
    val lightColor = MaterialTheme.colorScheme.secondary
    Canvas(modifier) {
        val p = progress.coerceIn(0f, 1f)
        val stemStart = Offset(size.width * .18f, size.height * .86f)
        val stemEnd = Offset(size.width * (.18f + .48f * p), size.height * (.86f - .62f * p))
        drawLine(leafColor.copy(alpha = p), stemStart, stemEnd, 2.dp.toPx(), cap = StrokeCap.Round)
        drawOval(leafColor.copy(alpha = p), topLeft = Offset(size.width * .3f, size.height * .42f), size = Size(size.width * .24f * p, size.height * .22f * p))
        drawOval(lightColor.copy(alpha = p), topLeft = Offset(size.width * .52f, size.height * .27f), size = Size(size.width * .25f * p, size.height * .2f * p))
        drawCircle(MoonCream.copy(alpha = p), 2.dp.toPx() * p, Offset(size.width * .84f, size.height * .18f))
        drawCircle(lightColor.copy(alpha = .75f * p), 1.5.dp.toPx() * p, Offset(size.width * .9f, size.height * .48f))
    }
}

private val PrayerJourneyItem.stateLabel: String get() = when { inJamaah -> "Jama'ah"; status == PrayerStatus.COMPLETED -> "Completed"; status == PrayerStatus.QAZA -> "Qaza"; status == PrayerStatus.MISSED -> "Missed"; else -> "Unrecorded" }
private val PrayerJourneyItem.accessibilityStateLabel: String get() = when { inJamaah -> "completed in Jama'ah"; status == PrayerStatus.COMPLETED -> "completed"; status == PrayerStatus.QAZA -> "Qaza"; status == PrayerStatus.MISSED -> "missed"; else -> "unrecorded" }

@Composable
fun rememberTimeOfDayVisualState(): TimeOfDayVisualState {
    val lifecycleOwner = LocalLifecycleOwner.current
    var currentTime by remember { mutableStateOf(LocalTime.now()) }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_RESUME) currentTime = LocalTime.now() }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    val state = TimeOfDayVisualResolver.resolve(currentTime)
    LaunchedEffect(state, currentTime) {
        delay(TimeOfDayVisualResolver.durationUntilNextBoundary(currentTime).toMillis() + 250L)
        currentTime = LocalTime.now()
    }
    return state
}
