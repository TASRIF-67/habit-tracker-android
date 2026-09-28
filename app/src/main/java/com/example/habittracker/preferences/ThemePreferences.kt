package com.example.habittracker.preferences

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.map

enum class ThemeMode { SYSTEM, LIGHT, DARK }

private val Context.dataStore by preferencesDataStore("settings")

class ThemePreferences(private val context: Context) {
    private val key = stringPreferencesKey("theme_mode")
    val themeMode = context.dataStore.data.map { preferences ->
        preferences[key]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() } ?: ThemeMode.SYSTEM
    }
    suspend fun setThemeMode(mode: ThemeMode) { context.dataStore.edit { it[key] = mode.name } }
}
