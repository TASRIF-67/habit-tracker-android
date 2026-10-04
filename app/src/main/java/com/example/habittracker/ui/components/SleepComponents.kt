package com.example.habittracker.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.WbTwilight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.habittracker.data.SleepRules
import com.example.habittracker.data.local.entity.SleepPlan
import com.example.habittracker.data.local.entity.SleepSession
import com.example.habittracker.data.local.entity.durationMillis
import com.example.habittracker.ui.theme.AppSpacing
import java.time.*
import kotlinx.coroutines.delay

@Composable
fun TonightSection(
    plan: SleepPlan?,
    activeSession: SleepSession?,
    latestCompleted: SleepSession?,
    onGoingToBed: () -> Unit,
    onWake: () -> Unit,
    onOpenSettings: () -> Unit,
    onResumePlan: () -> Unit,
) {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(activeSession?.id) {
        while (activeSession != null) { delay(60_000); now = System.currentTimeMillis() }
    }
    val current = Instant.ofEpochMilli(now).atZone(ZoneId.systemDefault())
    val morningSummary = latestCompleted?.takeIf { session ->
        session.wokeUpAt?.let { Instant.ofEpochMilli(it).atZone(current.zone).toLocalDate() == current.toLocalDate() && current.hour < 12 } == true
    }
    Column(Modifier.padding(top = AppSpacing.xxLarge)) {
        SectionHeader(if (morningSummary != null && activeSession == null) "Last night" else "Tonight")
        Surface(
            Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = .48f),
            shape = MaterialTheme.shapes.large,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        ) {
            when {
                activeSession != null -> SleepingContent(activeSession, now, onWake)
                morningSummary != null -> CompletedSleepContent(morningSummary)
                plan == null -> SleepSetupContent(onOpenSettings)
                !plan.enabled -> PausedSleepContent(plan, onResumePlan, onOpenSettings)
                else -> PlannedSleepContent(plan, current, onGoingToBed, onOpenSettings)
            }
        }
    }
}

@Composable private fun SleepSetupContent(onOpenSettings: () -> Unit) {
    Column(Modifier.padding(AppSpacing.large)) {
        Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Outlined.Bedtime, null, tint = MaterialTheme.colorScheme.primary); Text("Plan your sleep", Modifier.padding(start = AppSpacing.small), style = MaterialTheme.typography.titleMedium) }
        Text("Set a bedtime and wake-up time.", Modifier.padding(top = AppSpacing.small), color = MaterialTheme.colorScheme.onSurfaceVariant)
        FilledTonalButton(onClick = onOpenSettings, Modifier.align(Alignment.End).padding(top = AppSpacing.medium).heightIn(min = 48.dp)) { Text("Set up sleep") }
    }
}

@Composable private fun PausedSleepContent(plan: SleepPlan, onResume: () -> Unit, onOpenSettings: () -> Unit) {
    Column(Modifier.padding(AppSpacing.large)) {
        Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Outlined.Bedtime, null, tint = MaterialTheme.colorScheme.onSurfaceVariant); Text("Sleep schedule paused", Modifier.padding(start = AppSpacing.small), style = MaterialTheme.typography.titleMedium) }
        Text("${SleepRules.formatMinutes(plan.bedtimeMinutes)} → ${SleepRules.formatMinutes(plan.wakeTimeMinutes)}", Modifier.padding(top = AppSpacing.small), color = MaterialTheme.colorScheme.onSurfaceVariant)
        FlowRow(Modifier.fillMaxWidth().padding(top = AppSpacing.medium), horizontalArrangement = Arrangement.End, verticalArrangement = Arrangement.Center) {
            TextButton(onClick = onOpenSettings, modifier = Modifier.heightIn(min = 48.dp)) { Text("Edit schedule") }
            FilledTonalButton(onClick = onResume, modifier = Modifier.heightIn(min = 48.dp)) { Text("Resume") }
        }
    }
}

@Composable private fun PlannedSleepContent(plan: SleepPlan, now: ZonedDateTime, onGoingToBed: () -> Unit, onOpenSettings: () -> Unit) {
    val nextBedtime = SleepRules.nextBedtime(plan, now.minusMinutes(1))
    val minutesUntil = nextBedtime?.let { Duration.between(now, it).toMinutes() }
    Column(Modifier.padding(AppSpacing.large)) {
        Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Outlined.DarkMode, null, tint = MaterialTheme.colorScheme.primary); Text("Sleep plan", Modifier.padding(start = AppSpacing.small), style = MaterialTheme.typography.titleMedium) }
        Row(Modifier.fillMaxWidth().padding(top = AppSpacing.medium), horizontalArrangement = Arrangement.SpaceBetween) { SleepValue("Bedtime", SleepRules.formatMinutes(plan.bedtimeMinutes)); SleepValue("Wake up", SleepRules.formatMinutes(plan.wakeTimeMinutes), Alignment.End) }
        if (plan.windDownEnabled) Text("Wind down · ${SleepRules.formatMinutes((plan.bedtimeMinutes - plan.windDownOffsetMinutes + 1440) % 1440)}", Modifier.padding(top = AppSpacing.small), color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("${SleepRules.formatDuration(SleepRules.plannedMinutes(plan).toLong() * 60_000)} planned", Modifier.padding(top = 4.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (minutesUntil != null && minutesUntil in 0..60) Text("Bedtime in $minutesUntil min", Modifier.padding(top = AppSpacing.small), color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
        FlowRow(Modifier.fillMaxWidth().padding(top = AppSpacing.medium), horizontalArrangement = Arrangement.End, verticalArrangement = Arrangement.Center) {
            TextButton(onClick = onOpenSettings, modifier = Modifier.heightIn(min = 48.dp)) { Text("Edit schedule") }
            Button(onClick = onGoingToBed, Modifier.heightIn(min = 48.dp)) { Icon(Icons.Outlined.Bedtime, null); Spacer(Modifier.width(8.dp)); Text("Going to bed") }
        }
    }
}

@Composable private fun SleepingContent(session: SleepSession, now: Long, onWake: () -> Unit) {
    Column(Modifier.padding(AppSpacing.large)) {
        Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Outlined.DarkMode, null, tint = MaterialTheme.colorScheme.primary); Text("Sleeping", Modifier.padding(start = AppSpacing.small), style = MaterialTheme.typography.titleLarge) }
        Text("Since ${SleepRules.formatInstant(session.wentToBedAt)}", Modifier.padding(top = AppSpacing.medium), style = MaterialTheme.typography.titleMedium)
        Text("${SleepRules.formatDuration(session.durationMillis(now))} recorded · ${SleepRules.formatDuration(SleepRules.plannedMinutes(session.plannedBedtimeMinutes, session.plannedWakeTimeMinutes).toLong() * 60_000)} planned", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Button(onClick = onWake, Modifier.align(Alignment.End).padding(top = AppSpacing.medium).heightIn(min = 48.dp)) { Text("I'm awake") }
    }
}

@Composable private fun CompletedSleepContent(session: SleepSession) {
    Column(Modifier.padding(AppSpacing.large)) {
        Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Outlined.WbTwilight, null, tint = MaterialTheme.colorScheme.primary); Text(SleepRules.formatDuration(session.durationMillis()), Modifier.padding(start = AppSpacing.small), style = MaterialTheme.typography.headlineSmall) }
        Text("${SleepRules.formatInstant(session.wentToBedAt)} → ${SleepRules.formatInstant(session.wokeUpAt!!)}", Modifier.padding(top = AppSpacing.small), color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable private fun SleepValue(label: String, value: String, alignment: Alignment.Horizontal = Alignment.Start) {
    Column(horizontalAlignment = alignment) { Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant); Text(value, style = MaterialTheme.typography.titleMedium) }
}
