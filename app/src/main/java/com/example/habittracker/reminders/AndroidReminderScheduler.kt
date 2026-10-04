package com.example.habittracker.reminders

import android.app.*
import android.content.*
import android.os.Build
import androidx.core.app.NotificationManagerCompat
import com.example.habittracker.HabitTrackerApplication
import com.example.habittracker.data.ScheduleRules
import com.example.habittracker.data.ReminderRules
import com.example.habittracker.data.ReminderDeliveryMode
import com.example.habittracker.data.local.entity.*
import java.time.*
import kotlinx.coroutines.*

class AndroidReminderScheduler(private val context: Context) : ReminderScheduler {
    private val alarms = context.getSystemService(AlarmManager::class.java)
    override fun sync(habit: Habit, schedule: RoutineSchedule?) {
        cancelScheduled(habit.id)
        if (!habit.active || schedule == null) return
        val next = ScheduleRules.nextReminder(schedule, ZonedDateTime.now()) ?: return
        scheduleOccurrence(habit.id, next.toInstant().toEpochMilli(), schedule.reminderStyle, false)
    }
    override fun cancel(habitId: Long) {
        cancelScheduled(habitId)
        NotificationManagerCompat.from(context).cancel(ReminderPresenter.routineNotificationId(habitId))
    }
    private fun cancelScheduled(habitId: Long) {
        val intent = reminderIntent(context, habitId, 0, false)
        alarms.cancel(intent); intent.cancel()
    }
    fun scheduleSnooze(habitId: Long, occurrenceId: Long, minutes: Int) = scheduleOccurrence(habitId, ReminderRules.snoozeAt(System.currentTimeMillis(), minutes), ReminderStyle.ALARM, true, occurrenceId + minutes * 60_000L)
    private fun scheduleOccurrence(habitId: Long, at: Long, style: ReminderStyle, snooze: Boolean, occurrenceId: Long = at) {
        val intent = reminderIntent(context, habitId, occurrenceId, snooze)
        if (ReminderRules.deliveryMode(style, canScheduleExact()) == ReminderDeliveryMode.ALARM_EXACT) alarms.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, intent)
        else alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, intent)
    }
    fun canScheduleExact() = Build.VERSION.SDK_INT < 31 || alarms.canScheduleExactAlarms()

    companion object {
        fun requestCode(habitId: Long, snooze: Boolean) = ((habitId xor (habitId ushr 32)).toInt() and 0x3FFFFFFF) + if (snooze) 0x40000000 else 0
        fun reminderIntent(context: Context, habitId: Long, occurrenceId: Long, snooze: Boolean) = PendingIntent.getBroadcast(context, requestCode(habitId, snooze), Intent(context, RoutineReminderReceiver::class.java).putExtra(ReminderPresenter.EXTRA_HABIT_ID, habitId).putExtra(ReminderPresenter.EXTRA_OCCURRENCE_ID, occurrenceId).putExtra("snooze", snooze), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        fun createChannels(context: Context) = ReminderPresenter.createChannels(context)
        fun createChannel(context: Context) = createChannels(context)
    }
}

class RoutineReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val habitId = intent.getLongExtra(ReminderPresenter.EXTRA_HABIT_ID, -1); if (habitId < 0) return
        val occurrenceId = intent.getLongExtra(ReminderPresenter.EXTRA_OCCURRENCE_ID, 0)
        val snooze = intent.getBooleanExtra("snooze", false)
        if (!ReminderActionClaims(context).claim("delivery:$habitId:$occurrenceId")) return
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val app = context.applicationContext as HabitTrackerApplication
                val (habit, schedule) = app.repository.reminderData(habitId)
                if (habit != null && habit.active && schedule?.enabled == true && schedule.reminderEnabled) ReminderPresenter(context).showRoutine(habit, if (snooze) schedule.copy(reminderStyle = ReminderStyle.ALARM) else schedule, occurrenceId)
                if (!snooze) app.repository.reschedule(habitId)
            } finally { pending.finish() }
        }
    }
}

class ReminderActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val habitId = intent.getLongExtra(ReminderPresenter.EXTRA_HABIT_ID, -1); if (habitId < 0) return
        val occurrenceId = intent.getLongExtra(ReminderPresenter.EXTRA_OCCURRENCE_ID, 0)
        val action = intent.action ?: return
        if (!ReminderActionClaims(context).claim("action:$action:$habitId:$occurrenceId")) return
        NotificationManagerCompat.from(context).cancel(ReminderPresenter.routineNotificationId(habitId))
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val app = context.applicationContext as HabitTrackerApplication
                when (action) {
                    ReminderPresenter.ACTION_SNOOZE -> AndroidReminderScheduler(context).scheduleSnooze(habitId, occurrenceId, intent.getIntExtra(ReminderPresenter.EXTRA_SNOOZE_MINUTES, 10).takeIf { it in setOf(5, 10, 15) } ?: 10)
                    ReminderPresenter.ACTION_START -> app.repository.startSession(habitId, LocalDate.now())
                    ReminderPresenter.ACTION_DISMISS -> Unit
                }
            } finally { pending.finish() }
        }
    }
}

class ReminderRescheduleReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch { try { val app = context.applicationContext as HabitTrackerApplication; app.repository.rescheduleAll(); app.sleepRepository.rescheduleReminders(); app.prayerTimeRepository.rescheduleReminders(); if (intent.action == Intent.ACTION_BOOT_COMPLETED || intent.action == Intent.ACTION_MY_PACKAGE_REPLACED) app.repository.recoverSessionAfterReboot() } finally { pending.finish() } }
    }
}
