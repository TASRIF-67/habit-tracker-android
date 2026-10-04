package com.example.habittracker.reminders

import android.Manifest
import android.app.*
import android.content.*
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.habittracker.MainActivity
import com.example.habittracker.R
import com.example.habittracker.data.SleepRules
import com.example.habittracker.data.local.entity.*

class ReminderPresenter(private val context: Context) {
    fun showRoutine(habit: Habit, schedule: RoutineSchedule, occurrenceId: Long) {
        if (!notificationsAllowed()) return
        createChannels(context)
        val alarm = schedule.reminderStyle == ReminderStyle.ALARM
        val openIntent = if (alarm) Intent(context, AlarmAlertActivity::class.java).putExtra(EXTRA_HABIT_ID, habit.id).putExtra(EXTRA_OCCURRENCE_ID, occurrenceId)
        else Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        val open = PendingIntent.getActivity(context, actionRequestCode(habit.id, "open", occurrenceId), openIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val text = targetSummary(habit)?.let { "Scheduled now · $it" } ?: "Scheduled now"
        val builder = NotificationCompat.Builder(context, if (alarm) ALARM_CHANNEL_ID else PROMINENT_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_habit).setContentTitle(habit.name).setContentText(text).setContentIntent(open)
            .setPriority(if (alarm) NotificationCompat.PRIORITY_MAX else NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC).setCategory(if (alarm) NotificationCompat.CATEGORY_ALARM else NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(!alarm).setOngoing(alarm).setOnlyAlertOnce(false)
        if (alarm) {
            builder.addAction(0, "Snooze 10 min", actionIntent(context, habit.id, occurrenceId, ACTION_SNOOZE, 10))
            builder.addAction(0, "Dismiss", actionIntent(context, habit.id, occurrenceId, ACTION_DISMISS))
            if (habit.routineType == RoutineType.DURATION) builder.addAction(0, "Start", actionIntent(context, habit.id, occurrenceId, ACTION_START))
        }
        val notification = builder.build().apply { if (alarm) flags = flags or Notification.FLAG_INSISTENT }
        NotificationManagerCompat.from(context).notify(routineNotificationId(habit.id), notification)
    }

    fun showSleep(title: String, text: String, notificationId: Int) {
        if (!notificationsAllowed()) return
        createChannels(context)
        val open = PendingIntent.getActivity(context, notificationId, Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        NotificationManagerCompat.from(context).notify(notificationId, NotificationCompat.Builder(context, SLEEP_CHANNEL_ID).setSmallIcon(R.drawable.ic_notification_habit).setContentTitle(title).setContentText(text).setAutoCancel(true).setContentIntent(open).setPriority(NotificationCompat.PRIORITY_HIGH).setVisibility(NotificationCompat.VISIBILITY_PUBLIC).setCategory(NotificationCompat.CATEGORY_REMINDER).build())
    }

    private fun notificationsAllowed() = Build.VERSION.SDK_INT < 33 || ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    companion object {
        const val PROMINENT_CHANNEL_ID = "routine_reminders_v2"
        const val ALARM_CHANNEL_ID = "routine_alarms_v1"
        const val SLEEP_CHANNEL_ID = "sleep_reminders_v2"
        const val PRAYER_CHANNEL_ID = "prayer_reminders_v1"
        const val ACTION_SNOOZE = "reminder.snooze"
        const val ACTION_DISMISS = "reminder.dismiss"
        const val ACTION_START = "reminder.start"
        const val EXTRA_HABIT_ID = "habitId"
        const val EXTRA_OCCURRENCE_ID = "occurrenceId"
        const val EXTRA_SNOOZE_MINUTES = "snoozeMinutes"

        fun routineNotificationId(habitId: Long) = 200_000 + ((habitId xor (habitId ushr 32)).toInt() and 0xFFFF)
        fun targetSummary(habit: Habit): String? = when (habit.routineType) { RoutineType.CHECK -> null; RoutineType.DURATION -> habit.target?.let { "$it min" }; RoutineType.COUNT -> habit.target?.let { "$it ${habit.unit.orEmpty()}".trim() } }
        fun actionRequestCode(habitId: Long, action: String, occurrenceId: Long) = (habitId.hashCode() * 31 + action.hashCode() * 17 + occurrenceId.hashCode()) and Int.MAX_VALUE
        fun actionIntent(context: Context, habitId: Long, occurrenceId: Long, action: String, snoozeMinutes: Int = 0): PendingIntent = PendingIntent.getBroadcast(context, actionRequestCode(habitId, action, occurrenceId), Intent(context, ReminderActionReceiver::class.java).setAction(action).putExtra(EXTRA_HABIT_ID, habitId).putExtra(EXTRA_OCCURRENCE_ID, occurrenceId).putExtra(EXTRA_SNOOZE_MINUTES, snoozeMinutes), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        fun createChannels(context: Context) {
            if (Build.VERSION.SDK_INT < 26) return
            val manager = context.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(NotificationChannel(PROMINENT_CHANNEL_ID, "Prominent reminders", NotificationManager.IMPORTANCE_HIGH).apply { description = "Heads-up routine reminders"; enableVibration(true); lockscreenVisibility = Notification.VISIBILITY_PUBLIC })
            manager.createNotificationChannel(NotificationChannel(ALARM_CHANNEL_ID, "Routine alarms", NotificationManager.IMPORTANCE_HIGH).apply { description = "Persistent alarms for explicitly selected routines"; enableVibration(true); lockscreenVisibility = Notification.VISIBILITY_PUBLIC })
            manager.createNotificationChannel(NotificationChannel(SLEEP_CHANNEL_ID, "Sleep reminders", NotificationManager.IMPORTANCE_HIGH).apply { description = "Wind-down and bedtime reminders"; enableVibration(true); lockscreenVisibility = Notification.VISIBILITY_PUBLIC })
            manager.createNotificationChannel(NotificationChannel(PRAYER_CHANNEL_ID, "Prayer reminders", NotificationManager.IMPORTANCE_HIGH).apply { description = "Calculated prayer-time reminders"; enableVibration(true); lockscreenVisibility = Notification.VISIBILITY_PUBLIC })
        }
    }

    fun showPrayer(prayer: Prayer, timeText: String, notificationId: Int) {
        if (!notificationsAllowed()) return
        createChannels(context)
        val open = PendingIntent.getActivity(context, notificationId, Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        NotificationManagerCompat.from(context).notify(notificationId, NotificationCompat.Builder(context, PRAYER_CHANNEL_ID).setSmallIcon(R.drawable.ic_notification_habit).setContentTitle(prayer.displayName).setContentText("Prayer time is $timeText").setContentIntent(open).setAutoCancel(true).setPriority(NotificationCompat.PRIORITY_HIGH).setVisibility(NotificationCompat.VISIBILITY_PUBLIC).setCategory(NotificationCompat.CATEGORY_REMINDER).build())
    }
}

internal class ReminderActionClaims(context: Context) {
    private val preferences = context.getSharedPreferences("reminder_action_claims", Context.MODE_PRIVATE)
    @Synchronized fun claim(key: String): Boolean {
        if (preferences.getBoolean(key, false)) return false
        return preferences.edit().putBoolean(key, true).commit()
    }
}
