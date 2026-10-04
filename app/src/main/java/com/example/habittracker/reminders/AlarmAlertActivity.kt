package com.example.habittracker.reminders

import android.app.PendingIntent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.example.habittracker.HabitTrackerApplication
import com.example.habittracker.data.ScheduleRules
import com.example.habittracker.data.local.entity.Habit
import com.example.habittracker.data.local.entity.RoutineSchedule
import com.example.habittracker.data.local.entity.RoutineType
import com.example.habittracker.ui.theme.HabitTrackerTheme
import kotlinx.coroutines.launch

class AlarmAlertActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setShowWhenLocked(true)
        setTurnScreenOn(true)
        val habitId = intent.getLongExtra(ReminderPresenter.EXTRA_HABIT_ID, -1)
        val occurrenceId = intent.getLongExtra(ReminderPresenter.EXTRA_OCCURRENCE_ID, 0)
        var data by mutableStateOf<Pair<Habit, RoutineSchedule>?>(null)
        lifecycleScope.launch {
            val pair = (application as HabitTrackerApplication).repository.reminderData(habitId)
            if (pair.first != null && pair.second != null) data = pair.first!! to pair.second!! else finish()
        }
        setContent { HabitTrackerTheme(dynamicColor = false) { AlarmScreen(data, { action, minutes -> ReminderPresenter.actionIntent(this, habitId, occurrenceId, action, minutes).send(); finish() }) } }
    }
}

@Composable
private fun AlarmScreen(data: Pair<Habit, RoutineSchedule>?, onAction: (String, Int) -> Unit) {
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        if (data == null) Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        else {
            val (habit, schedule) = data
            Column(Modifier.fillMaxSize().safeDrawingPadding().padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Icon(Icons.Outlined.Alarm, null, Modifier.size(72.dp), tint = MaterialTheme.colorScheme.primary)
                Text(habit.name, Modifier.padding(top = 20.dp), style = MaterialTheme.typography.headlineLarge)
                Text(ScheduleRules.formatTime(schedule.timeMinutes), style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
                ReminderPresenter.targetSummary(habit)?.let { Text(it, Modifier.padding(top = 8.dp), color = MaterialTheme.colorScheme.onSurfaceVariant) }
                Text("Snooze", Modifier.padding(top = 36.dp), style = MaterialTheme.typography.labelLarge)
                Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) { listOf(5, 10, 15).forEach { minutes -> OutlinedButton(onClick = { onAction(ReminderPresenter.ACTION_SNOOZE, minutes) }) { Text("$minutes min") } } }
                if (habit.routineType == RoutineType.DURATION) Button(onClick = { onAction(ReminderPresenter.ACTION_START, 0) }, Modifier.fillMaxWidth().padding(top = 24.dp).heightIn(min = 52.dp)) { Icon(Icons.Outlined.PlayArrow, null); Spacer(Modifier.width(8.dp)); Text("Start") }
                TextButton(onClick = { onAction(ReminderPresenter.ACTION_DISMISS, 0) }, Modifier.fillMaxWidth().padding(top = 8.dp).heightIn(min = 52.dp)) { Text("Dismiss") }
            }
        }
    }
}
