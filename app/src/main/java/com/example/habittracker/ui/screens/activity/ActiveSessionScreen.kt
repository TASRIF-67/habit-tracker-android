package com.example.habittracker.ui.screens.activity

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.habittracker.data.local.dao.HabitWithStatus
import com.example.habittracker.data.local.dao.SessionFinishResult
import com.example.habittracker.data.local.entity.ActivitySession
import com.example.habittracker.data.local.entity.ActivitySessionStatus
import com.example.habittracker.data.local.entity.elapsedMillis
import com.example.habittracker.ui.components.AppProgress
import com.example.habittracker.ui.components.routineIcon
import com.example.habittracker.ui.components.routinePalette
import kotlinx.coroutines.delay

@Composable
fun ActiveSessionScreen(
    habit: HabitWithStatus,
    session: ActivitySession,
    onBack: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onFinish: (Boolean, (SessionFinishResult?) -> Unit) -> Unit,
    onDiscard: () -> Unit,
) {
    val palette = routinePalette(habit.themeKey)
    var now by remember(session.id, session.status, session.resumedAt) { mutableLongStateOf(System.currentTimeMillis()) }
    var confirmShortFinish by remember { mutableStateOf(false) }
    var confirmDiscard by remember { mutableStateOf(false) }
    LaunchedEffect(session.id, session.status, session.resumedAt) {
        while (session.status == ActivitySessionStatus.RUNNING) { delay(1_000); now = System.currentTimeMillis() }
    }
    val elapsed = session.elapsedMillis(now)
    val elapsedMinutes = (elapsed / 60_000L).toInt()
    val target = habit.target ?: 0
    val totalProgress = habit.value + elapsedMinutes
    val paused = session.status == ActivitySessionStatus.PAUSED
    fun finish(allowShort: Boolean = false) { onFinish(allowShort) { if (it?.needsShortConfirmation == true) confirmShortFinish = true } }

    Scaffold(topBar = { Box(Modifier.statusBarsPadding().fillMaxWidth().height(56.dp)) { IconButton(onBack, Modifier.align(Alignment.CenterStart)) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Back while session continues") } } }, containerColor = MaterialTheme.colorScheme.background) { padding ->
        Column(
            Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 20.dp)
                .semantics { contentDescription = "${habit.name}, ${formatElapsed(elapsed)} elapsed, ${if (paused) "paused" else "running"}" },
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(Modifier.size(88.dp).background(palette.container, CircleShape), contentAlignment = Alignment.Center) { Icon(routineIcon(habit.iconKey), null, Modifier.size(42.dp), tint = palette.icon) }
            Text(habit.name.uppercase(), Modifier.padding(top = 24.dp), style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(formatElapsed(elapsed), Modifier.fillMaxWidth().padding(top = 24.dp), style = MaterialTheme.typography.displayLarge, color = palette.icon, textAlign = TextAlign.Center, maxLines = 1)
            if (paused) Text("PAUSED", Modifier.padding(top = 10.dp), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(if (target > 0) "Target: $target ${habit.unit ?: "min"}" else "Timed session", Modifier.padding(top = 14.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (target > 0) {
                AppProgress(totalProgress.toFloat() / target, Modifier.fillMaxWidth().padding(top = 18.dp), palette.progress)
                Text("$totalProgress / $target ${habit.unit ?: "min"}", Modifier.padding(top = 8.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Button(if (paused) onResume else onPause, Modifier.fillMaxWidth().padding(top = 36.dp).heightIn(min = 54.dp), colors = ButtonDefaults.buttonColors(containerColor = palette.icon)) {
                Icon(if (paused) Icons.Outlined.PlayArrow else Icons.Outlined.Pause, null); Spacer(Modifier.width(8.dp)); Text(if (paused) "Resume" else "Pause")
            }
            OutlinedButton({ finish() }, Modifier.fillMaxWidth().padding(top = 12.dp).heightIn(min = 52.dp)) { Text("Finish session") }
            TextButton({ confirmDiscard = true }, Modifier.fillMaxWidth().padding(top = 6.dp).heightIn(min = 48.dp)) { Text("Discard session") }
        }
    }
    if (confirmShortFinish) AlertDialog(onDismissRequest = { confirmShortFinish = false }, title = { Text("Session under 1 minute") }, text = { Text("This session won't add duration progress.") }, confirmButton = { TextButton({ confirmShortFinish = false; finish(true) }) { Text("Finish anyway") } }, dismissButton = { TextButton({ confirmShortFinish = false }) { Text("Continue") } })
    if (confirmDiscard) AlertDialog(onDismissRequest = { confirmDiscard = false }, title = { Text("Discard session?") }, text = { Text("Elapsed time will not be added to your progress.") }, confirmButton = { TextButton({ confirmDiscard = false; onDiscard() }) { Text("Discard") } }, dismissButton = { TextButton({ confirmDiscard = false }) { Text("Cancel") } })
}

private fun formatElapsed(millis: Long): String { val seconds = millis / 1_000; val hours = seconds / 3_600; val minutes = seconds % 3_600 / 60; val remaining = seconds % 60; return if (hours > 0) "$hours:${minutes.toString().padStart(2, '0')}:${remaining.toString().padStart(2, '0')}" else "${minutes.toString().padStart(2, '0')}:${remaining.toString().padStart(2, '0')}" }
