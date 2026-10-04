package com.example.habittracker
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.SystemBarStyle
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.habittracker.preferences.ThemeMode
import com.example.habittracker.ui.HabitTrackerApp
import com.example.habittracker.ui.theme.HabitTrackerTheme
import com.example.habittracker.viewmodel.HabitViewModel
import com.example.habittracker.viewmodel.SleepViewModel
import com.example.habittracker.viewmodel.PrayerTimeViewModel
import com.example.habittracker.viewmodel.OnboardingViewModel
import com.example.habittracker.ui.onboarding.OnboardingFlow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
class MainActivity : ComponentActivity() {
    private var activeSessionRequest by mutableIntStateOf(0)
    private val viewModel: HabitViewModel by viewModels { val app = application as HabitTrackerApplication; HabitViewModel.Factory(app.repository, app.prayerRepository, app.themePreferences) }
    private val sleepViewModel: SleepViewModel by viewModels { SleepViewModel.Factory((application as HabitTrackerApplication).sleepRepository) }
    private val prayerTimeViewModel: PrayerTimeViewModel by viewModels { PrayerTimeViewModel.Factory((application as HabitTrackerApplication).prayerTimeRepository) }
    private val onboardingViewModel: OnboardingViewModel by viewModels { val app = application as HabitTrackerApplication; OnboardingViewModel.Factory(app.onboardingPreferences, this) }
    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); consumeActiveSessionIntent(intent); enableEdgeToEdge(); setContent {
        val mode by viewModel.themeMode.collectAsStateWithLifecycle()
        val dark = when (mode) { ThemeMode.SYSTEM -> isSystemInDarkTheme(); ThemeMode.LIGHT -> false; ThemeMode.DARK -> true }
        val view = LocalView.current
        SideEffect {
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !dark
                isAppearanceLightNavigationBars = !dark
            }
        }
        HabitTrackerTheme(darkTheme = dark, dynamicColor = false) {
            val systemBarColor = androidx.compose.material3.MaterialTheme.colorScheme.background.toArgb()
            SideEffect {
                enableEdgeToEdge(
                    statusBarStyle = if (dark) SystemBarStyle.dark(systemBarColor) else SystemBarStyle.light(systemBarColor, systemBarColor),
                    navigationBarStyle = if (dark) SystemBarStyle.dark(systemBarColor) else SystemBarStyle.light(systemBarColor, systemBarColor),
                )
            }
            val onboarding by onboardingViewModel.state.collectAsStateWithLifecycle()
            when {
                !onboarding.ready -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                onboarding.completed -> HabitTrackerApp(viewModel, sleepViewModel, prayerTimeViewModel, activeSessionRequest)
                else -> {
                    val habits by viewModel.allHabits.collectAsStateWithLifecycle()
                    val prayerSettings by prayerTimeViewModel.settings.collectAsStateWithLifecycle()
                    val prayerReminders by prayerTimeViewModel.reminders.collectAsStateWithLifecycle()
                    val sleepPlan by sleepViewModel.plan.collectAsStateWithLifecycle()
                    OnboardingFlow(onboarding, habits, prayerSettings, prayerReminders, sleepPlan, onboardingViewModel::next, onboardingViewModel::back, onboardingViewModel::complete, viewModel::addHabit, prayerTimeViewModel::saveSettings, { prayerTimeViewModel.saveReminder(it) }, sleepViewModel::savePlan)
                }
            }
        }
    } }
    override fun onNewIntent(intent: Intent) { super.onNewIntent(intent); setIntent(intent); consumeActiveSessionIntent(intent) }
    private fun consumeActiveSessionIntent(intent: Intent?) {
        if (intent?.getBooleanExtra(EXTRA_OPEN_ACTIVE_SESSION, false) == true) {
            activeSessionRequest++
            intent.removeExtra(EXTRA_OPEN_ACTIVE_SESSION)
        }
    }
    companion object { const val EXTRA_OPEN_ACTIVE_SESSION = "com.example.habittracker.OPEN_ACTIVE_SESSION" }
}
