package com.example.habittracker
import android.app.Application
import com.example.habittracker.data.local.HabitDatabase
import com.example.habittracker.data.repository.HabitRepository
import com.example.habittracker.preferences.ThemePreferences
class HabitTrackerApplication : Application() {
    val repository by lazy { HabitRepository(HabitDatabase.getInstance(this).habitDao()) }
    val themePreferences by lazy { ThemePreferences(this) }
}
