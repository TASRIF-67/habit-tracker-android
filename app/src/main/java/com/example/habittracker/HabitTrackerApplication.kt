package com.example.habittracker
import android.app.Application
import com.example.habittracker.data.local.HabitDatabase
import com.example.habittracker.data.repository.HabitRepository
import com.example.habittracker.data.repository.PrayerRepository
import com.example.habittracker.data.repository.SleepRepository
import com.example.habittracker.data.repository.ActiveActivity
import com.example.habittracker.data.repository.ActiveActivityLookup
import com.example.habittracker.data.repository.PrayerTimeRepository
import com.example.habittracker.reminders.AndroidPrayerReminderScheduler
import com.example.habittracker.preferences.ThemePreferences
import com.example.habittracker.preferences.OnboardingPreferences
import com.example.habittracker.reminders.AndroidReminderScheduler
import com.example.habittracker.reminders.AndroidSleepReminderScheduler
import com.example.habittracker.activities.AndroidActivityServiceController
import kotlinx.coroutines.*
class HabitTrackerApplication : Application() {
    private val database by lazy { HabitDatabase.getInstance(this) }
    val repository by lazy { HabitRepository(database.habitDao(), AndroidReminderScheduler(this), AndroidActivityServiceController(this)) }
    val prayerRepository by lazy { PrayerRepository(database.prayerDao()) }
    val sleepRepository by lazy { SleepRepository(database.sleepDao(), ActiveActivityLookup { database.habitDao().activeSession()?.let { ActiveActivity(it.status, database.habitDao().habit(it.habitId)?.name) } }, AndroidSleepReminderScheduler(this)) }
    val prayerTimeRepository by lazy { PrayerTimeRepository(database.prayerTimeDao(), AndroidPrayerReminderScheduler(this)) }
    val themePreferences by lazy { ThemePreferences(this) }
    val onboardingPreferences by lazy { OnboardingPreferences(this) }
    override fun onCreate() { super.onCreate(); AndroidReminderScheduler.createChannel(this); AndroidSleepReminderScheduler.createChannel(this); CoroutineScope(SupervisorJob() + Dispatchers.IO).launch { if (repository.activeSessionSnapshot() != null) AndroidActivityServiceController(this@HabitTrackerApplication).refresh(); prayerTimeRepository.rescheduleReminders() } }
}
