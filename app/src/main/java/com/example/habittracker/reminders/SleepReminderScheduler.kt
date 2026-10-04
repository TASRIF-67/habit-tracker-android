package com.example.habittracker.reminders

import android.Manifest
import android.app.*
import android.content.*
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.habittracker.HabitTrackerApplication
import com.example.habittracker.MainActivity
import com.example.habittracker.R
import com.example.habittracker.data.SleepRules
import com.example.habittracker.data.local.entity.SleepPlan
import java.time.ZonedDateTime
import kotlinx.coroutines.*

interface SleepReminderScheduler {
    fun sync(plan: SleepPlan?)
    fun cancel()
}

class AndroidSleepReminderScheduler(private val context: Context) : SleepReminderScheduler {
    private val alarms = context.getSystemService(AlarmManager::class.java)

    override fun sync(plan: SleepPlan?) {
        cancel()
        if (plan?.enabled != true) return
        val now = ZonedDateTime.now()
        if (plan.windDownEnabled) SleepRules.nextWindDown(plan, now)?.let { schedule(TYPE_WIND_DOWN, it.toInstant().toEpochMilli()) }
        if (plan.bedtimeReminderEnabled) SleepRules.nextBedtime(plan, now)?.let { schedule(TYPE_BEDTIME, it.toInstant().toEpochMilli()) }
    }

    override fun cancel() { TYPES.forEach { type -> alarms.cancel(pendingIntent(context, type)); pendingIntent(context, type).cancel() } }
    private fun schedule(type: String, at: Long) = alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pendingIntent(context, type))

    companion object {
        const val CHANNEL_ID = ReminderPresenter.SLEEP_CHANNEL_ID
        const val TYPE_WIND_DOWN = "wind_down"
        const val TYPE_BEDTIME = "bedtime"
        private val TYPES = listOf(TYPE_WIND_DOWN, TYPE_BEDTIME)
        fun requestCode(type: String) = if (type == TYPE_WIND_DOWN) 700_001 else 700_002
        fun pendingIntent(context: Context, type: String) = PendingIntent.getBroadcast(
            context,
            requestCode(type),
            Intent(context, SleepReminderReceiver::class.java).putExtra("sleepReminderType", type),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        fun createChannel(context: Context) = ReminderPresenter.createChannels(context)
    }
}

class SleepReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val type = intent.getStringExtra("sleepReminderType") ?: return
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val app = context.applicationContext as HabitTrackerApplication
                val plan = app.sleepRepository.planSnapshot()
                val enabled = plan?.enabled == true && when (type) {
                    AndroidSleepReminderScheduler.TYPE_WIND_DOWN -> plan.windDownEnabled
                    AndroidSleepReminderScheduler.TYPE_BEDTIME -> plan.bedtimeReminderEnabled
                    else -> false
                }
                if (enabled) {
                    val title = if (type == AndroidSleepReminderScheduler.TYPE_WIND_DOWN) "Wind-down time" else "Bedtime"
                    val text = if (type == AndroidSleepReminderScheduler.TYPE_WIND_DOWN) "Your planned bedtime is in ${plan.windDownOffsetMinutes} minutes." else "Your planned bedtime is ${SleepRules.formatMinutes(plan.bedtimeMinutes)}."
                    ReminderPresenter(context).showSleep(title, text, AndroidSleepReminderScheduler.requestCode(type))
                }
                app.sleepRepository.rescheduleReminders()
            } finally { pending.finish() }
        }
    }
}
