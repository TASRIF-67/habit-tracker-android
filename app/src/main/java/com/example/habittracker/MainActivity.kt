package com.example.habittracker
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.habittracker.preferences.ThemeMode
import com.example.habittracker.ui.HabitTrackerApp
import com.example.habittracker.ui.theme.HabitTrackerTheme
import com.example.habittracker.viewmodel.HabitViewModel
class MainActivity : ComponentActivity() {
    private val viewModel: HabitViewModel by viewModels { val app = application as HabitTrackerApplication; HabitViewModel.Factory(app.repository, app.prayerRepository, app.themePreferences) }
    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); enableEdgeToEdge(); setContent {
        val mode by viewModel.themeMode.collectAsStateWithLifecycle()
        val dark = when (mode) { ThemeMode.SYSTEM -> isSystemInDarkTheme(); ThemeMode.LIGHT -> false; ThemeMode.DARK -> true }
        val view = LocalView.current
        SideEffect {
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !dark
                isAppearanceLightNavigationBars = !dark
            }
        }
        HabitTrackerTheme(darkTheme = dark, dynamicColor = false) { HabitTrackerApp(viewModel) }
    } }
}
