package com.example.habittracker.reminders

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.*
import com.example.habittracker.HabitTrackerApplication
import com.example.habittracker.data.PrayerTimeRules
import com.example.habittracker.data.local.entity.*
import java.time.*
import kotlinx.coroutines.*

interface PrayerReminderScheduler { fun sync(settings: PrayerTimeSettings?, configs: List<PrayerReminderConfig>); fun cancelAll() }

class AndroidPrayerReminderScheduler(private val context: Context) : PrayerReminderScheduler {
    private val alarms = context.getSystemService(AlarmManager::class.java)
    override fun sync(settings: PrayerTimeSettings?, configs: List<PrayerReminderConfig>) {
        cancelAll(); if (settings == null) return
        PrayerTimeRules.reminderSchedule(settings, configs, ZonedDateTime.now()).forEach { (prayer, at) -> alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at.toInstant().toEpochMilli(), intent(context, prayer, at.toInstant().toEpochMilli())) }
    }
    override fun cancelAll() { Prayer.entries.forEach { prayer -> intent(context, prayer, 0).let { alarms.cancel(it); it.cancel() } } }
    companion object {
        fun requestCode(prayer: Prayer) = 850_000 + prayer.ordinal
        fun intent(context: Context, prayer: Prayer, occurrence: Long) = PendingIntent.getBroadcast(context, requestCode(prayer), Intent(context, PrayerReminderReceiver::class.java).putExtra("prayer", prayer.name).putExtra("occurrence", occurrence), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }
}

class PrayerReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val prayer = intent.getStringExtra("prayer")?.let { runCatching { Prayer.valueOf(it) }.getOrNull() } ?: return
        val occurrence = intent.getLongExtra("occurrence", 0)
        if (!ReminderActionClaims(context).claim("prayer:$prayer:$occurrence")) return
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val app = context.applicationContext as HabitTrackerApplication
                val settings = app.prayerTimeRepository.settingsSnapshot()
                val config = app.prayerTimeRepository.reminderSnapshot().firstOrNull { it.prayer == prayer }
                if (settings != null && config?.enabled == true) {
                    val occurrenceTime = Instant.ofEpochMilli(occurrence).atZone(ZoneId.systemDefault()).plusMinutes(config.offsetMinutes.toLong())
                    ReminderPresenter(context).showPrayer(prayer, PrayerTimeRules.formatTime(occurrenceTime), AndroidPrayerReminderScheduler.requestCode(prayer))
                }
                app.prayerTimeRepository.rescheduleReminders()
            } finally { pending.finish() }
        }
    }
}
