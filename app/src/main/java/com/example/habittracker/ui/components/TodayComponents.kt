package com.example.habittracker.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.habittracker.data.TimeOfDayVisualResolver
import com.example.habittracker.data.TimeOfDayVisualState
import com.example.habittracker.data.local.dao.HabitWithStatus
import com.example.habittracker.data.local.dao.PrayerRecordDetails
import com.example.habittracker.data.local.entity.Prayer
import com.example.habittracker.data.local.entity.PrayerStatus
import com.example.habittracker.ui.theme.*
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.delay

@Composable
fun TodayHero(date: LocalDate, completed: Int, total: Int, timeState: TimeOfDayVisualState) {
    val target = if (total == 0) 0f else completed.toFloat() / total
    val animatedProgress by animateFloatAsState(target, tween(AppMotion.emphasized, easing = FastOutSlowInEasing), label = "daily progress")
    val percentage = (animatedProgress * 100).toInt()
    Column {
        Text(TimeOfDayVisualResolver.greeting(timeState), style = MaterialTheme.typography.headlineMedium)
        Text(date.format(DateTimeFormatter.ofPattern("EEEE, d MMMM")), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(Modifier.fillMaxWidth().padding(top = AppSpacing.xLarge), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text("$percentage%", style = MaterialTheme.typography.displaySmall, color = MaterialTheme.colorScheme.primary)
                Text("$completed of $total completed", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(if (percentage == 100) "A complete day" else "Today's progress", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary)
        }
        AppProgress(animatedProgress, Modifier.padding(top = AppSpacing.medium))
    }
}

data class PrayerJourneyItem(val habit: HabitWithStatus, val prayer: Prayer, val record: PrayerRecordDetails?) {
    val status: PrayerStatus get() = record?.status ?: PrayerStatus.UNRECORDED
    val inJamaah: Boolean get() = record?.inJamaah == true
    val countsAsCompleted: Boolean get() = status.countsAsCompleted
}

data class PrayerFeedback(val prayerName: String, val message: String, val xpGranted: Int = 0)

@Composable
fun PrayerJourney(prayers: List<PrayerJourneyItem>, timeState: TimeOfDayVisualState, celebratingJamaah: Prayer?, onPrayerClick: (PrayerJourneyItem) -> Unit) {
    val completed = prayers.count { it.countsAsCompleted }
    val incompleteConnector = MaterialTheme.colorScheme.outlineVariant
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        tonalElevation = 1.dp,
    ) {
        Column {
            PrayerVisual(timeState, Modifier.fillMaxWidth().height(132.dp))
            Column(Modifier.padding(horizontal = AppSpacing.large, vertical = AppSpacing.large)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Prayer journey", style = MaterialTheme.typography.titleMedium)
                    Text("$completed / ${prayers.size}", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                }
                Box(Modifier.fillMaxWidth().padding(top = AppSpacing.medium)) {
                    Canvas(Modifier.fillMaxWidth().height(36.dp).padding(horizontal = 22.dp)) {
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
                        prayers.forEach { prayer -> PrayerNode(prayer, celebratingJamaah == prayer.prayer, onPrayerClick) }
                    }
                }
            }
        }
    }
}

@Composable
private fun RowScope.PrayerNode(item: PrayerJourneyItem, celebratingJamaah: Boolean, onClick: (PrayerJourneyItem) -> Unit) {
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
        Box(Modifier.size(46.dp), contentAlignment = Alignment.Center) {
            if (radiance.value > 0f) Canvas(Modifier.fillMaxSize()) { val p = radiance.value; drawCircle(ForestLight.copy(alpha = .28f * (1f - p)), radius = size.minDimension * (.28f + p * .22f), style = androidx.compose.ui.graphics.drawscope.Stroke(2.dp.toPx())); repeat(4) { index -> val angle = index * Math.PI.toFloat() / 2; drawCircle(MoonCream.copy(alpha = 1f - p), 1.8.dp.toPx(), Offset(center.x + kotlin.math.cos(angle) * 20.dp.toPx() * p, center.y + kotlin.math.sin(angle) * 20.dp.toPx() * p)) } }
            Box(Modifier.size(40.dp).scale(scale).background(background, CircleShape), contentAlignment = Alignment.Center) {
                when { item.inJamaah -> Icon(Icons.Outlined.Groups, null, Modifier.size(21.dp), tint = foreground); item.status == PrayerStatus.COMPLETED -> Icon(Icons.Rounded.Check, null, Modifier.size(21.dp), tint = foreground); item.status == PrayerStatus.QAZA -> Icon(Icons.Outlined.History, null, Modifier.size(20.dp), tint = foreground); item.status == PrayerStatus.MISSED -> Icon(Icons.Outlined.Remove, null, Modifier.size(20.dp), tint = foreground) }
            }
        }
        Text(item.habit.name, Modifier.padding(top = 3.dp), style = MaterialTheme.typography.labelSmall, maxLines = 1, textAlign = TextAlign.Center)
        Text(item.stateLabel, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, textAlign = TextAlign.Center)
    }
}

@Composable
fun PrayerVisual(timeState: TimeOfDayVisualState, modifier: Modifier = Modifier) {
    Crossfade(timeState, animationSpec = tween(AppMotion.emphasized), label = "time of day sky") { state ->
        val description = "${state.name.lowercase().replaceFirstChar { it.uppercase() }} prayer scene"
        Canvas(modifier.semantics { contentDescription = description }) { drawPrayerScene(state) }
    }
}

private fun DrawScope.drawPrayerScene(state: TimeOfDayVisualState) {
    val sky = when (state) { TimeOfDayVisualState.DAWN -> Color(0xFF405A78); TimeOfDayVisualState.MORNING -> Color(0xFFA8CFCA); TimeOfDayVisualState.DAY -> DaySky; TimeOfDayVisualState.AFTERNOON -> AfternoonSky; TimeOfDayVisualState.SUNSET -> SunsetSky; TimeOfDayVisualState.NIGHT -> Color(0xFF102B31) }
    val horizon = when (state) { TimeOfDayVisualState.DAWN -> Color(0xFFB58B70); TimeOfDayVisualState.MORNING -> Color(0xFF6C9A79); TimeOfDayVisualState.DAY -> Color(0xFF53806D); TimeOfDayVisualState.AFTERNOON -> Color(0xFF6F7651); TimeOfDayVisualState.SUNSET -> Color(0xFF534C43); TimeOfDayVisualState.NIGHT -> Color(0xFF143F38) }
    drawRect(sky)
    if (state == TimeOfDayVisualState.NIGHT) {
        drawCircle(MoonCream, radius = 11.dp.toPx(), center = Offset(size.width * .82f, size.height * .25f))
        listOf(.12f to .22f, .25f to .35f, .61f to .18f, .72f to .42f).forEach { (x, y) -> drawCircle(MoonCream.copy(alpha = .8f), 1.4.dp.toPx(), Offset(size.width * x, size.height * y)) }
    } else {
        if (state == TimeOfDayVisualState.DAWN) listOf(.13f to .2f, .67f to .17f).forEach { (x, y) -> drawCircle(MoonCream.copy(alpha = .5f), 1.2.dp.toPx(), Offset(size.width * x, size.height * y)) }
        val sunX = when (state) { TimeOfDayVisualState.DAWN -> .18f; TimeOfDayVisualState.MORNING -> .27f; TimeOfDayVisualState.DAY -> .6f; TimeOfDayVisualState.AFTERNOON -> .76f; else -> .84f }
        val sunY = when (state) { TimeOfDayVisualState.DAWN -> .72f; TimeOfDayVisualState.MORNING -> .48f; TimeOfDayVisualState.DAY -> .24f; TimeOfDayVisualState.AFTERNOON -> .43f; else -> .7f }
        drawCircle(MoonCream.copy(alpha = .9f), radius = 17.dp.toPx(), center = Offset(size.width * sunX, size.height * sunY))
    }
    val hills = Path().apply { moveTo(0f, size.height * .66f); quadraticTo(size.width * .22f, size.height * .45f, size.width * .45f, size.height * .72f); quadraticTo(size.width * .72f, size.height * .5f, size.width, size.height * .65f); lineTo(size.width, size.height); lineTo(0f, size.height); close() }
    drawPath(hills, horizon)
    val silhouette = if (state == TimeOfDayVisualState.NIGHT) Color(0xFF082A25) else Color(0xFF16483B)
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

private val PrayerJourneyItem.stateLabel: String get() = when { inJamaah -> "Jama'ah"; status == PrayerStatus.COMPLETED -> "Done"; status == PrayerStatus.QAZA -> "Qaza"; status == PrayerStatus.MISSED -> "Missed"; else -> "" }
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
