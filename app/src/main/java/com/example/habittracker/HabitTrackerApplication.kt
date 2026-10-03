package com.example.habittracker
import android.app.Application
import com.example.habittracker.data.local.HabitDatabase
import com.example.habittracker.data.repository.HabitRepository
import com.example.habittracker.data.repository.PrayerRepository
import com.example.habittracker.preferences.ThemePreferences
class HabitTrackerApplication : Application() {
    private val database by lazy { HabitDatabase.getInstance(this) }
    val repository by lazy { HabitRepository(database.habitDao()) }
    val prayerRepository by lazy { PrayerRepository(database.prayerDao()) }
    val themePreferences by lazy { ThemePreferences(this) }
}
