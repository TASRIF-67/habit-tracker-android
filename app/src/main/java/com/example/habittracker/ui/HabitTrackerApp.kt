package com.example.habittracker.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.*
import com.example.habittracker.ui.screens.journey.JourneyScreen
import com.example.habittracker.ui.screens.journey.JourneyDayScreen
import com.example.habittracker.ui.screens.activity.ActiveSessionScreen
import com.example.habittracker.ui.screens.routines.RoutinesScreen
import com.example.habittracker.ui.screens.settings.SettingsScreen
import com.example.habittracker.ui.screens.settings.SleepSettingsScreen
import com.example.habittracker.ui.screens.settings.PrayerTimeSettingsScreen
import com.example.habittracker.ui.screens.today.TodayScreen
import com.example.habittracker.viewmodel.HabitViewModel
import com.example.habittracker.viewmodel.SleepViewModel
import com.example.habittracker.viewmodel.PrayerTimeViewModel
import java.time.LocalDate
import com.example.habittracker.data.ActiveSessionNavigationDecision
import com.example.habittracker.data.ActiveSessionNavigationRules

private data class Destination(val route: String, val label: String, val icon: ImageVector, val selectedIcon: ImageVector)

@Composable
fun HabitTrackerApp(vm: HabitViewModel, sleepVm: SleepViewModel, prayerTimeVm: PrayerTimeViewModel, activeSessionRequest: Int = 0) {
    val nav = rememberNavController()
    val rootActiveSession by vm.activeSession.collectAsStateWithLifecycle()
    val activeSessionLoaded by vm.activeSessionLoaded.collectAsStateWithLifecycle()
    var handledActiveSessionRequest by rememberSaveable { mutableIntStateOf(0) }
    val destinations = listOf(
        Destination("today", "Today", Icons.Outlined.Today, Icons.Filled.Today),
        Destination("routines", "Routines", Icons.Outlined.Checklist, Icons.Filled.Checklist),
        Destination("journey", "Journey", Icons.Outlined.CalendarMonth, Icons.Filled.CalendarMonth),
        Destination("settings", "Settings", Icons.Outlined.Settings, Icons.Filled.Settings),
    )
    val entry by nav.currentBackStackEntryAsState()
    val route = entry?.destination?.route
    var todayUsesDarkStatusContent by rememberSaveable { mutableStateOf(false) }
    fun openActiveSession() { nav.navigate("active_session") { launchSingleTop = true } }
    LaunchedEffect(activeSessionRequest, activeSessionLoaded, rootActiveSession?.id) {
        when (ActiveSessionNavigationRules.notificationRequest(activeSessionRequest, handledActiveSessionRequest, activeSessionLoaded, rootActiveSession != null)) {
            ActiveSessionNavigationDecision.OpenSession -> { handledActiveSessionRequest = activeSessionRequest; openActiveSession() }
            ActiveSessionNavigationDecision.FallBackToToday -> { handledActiveSessionRequest = activeSessionRequest; nav.navigate("today") { launchSingleTop = true; popUpTo(nav.graph.startDestinationId) } }
            ActiveSessionNavigationDecision.Ignore, ActiveSessionNavigationDecision.Wait -> Unit
        }
    }
    val view = LocalView.current
    val lightPage = MaterialTheme.colorScheme.background.luminance() > .5f
    val useDarkStatusContent = if (route == "today") todayUsesDarkStatusContent else lightPage
    DisposableEffect(entry, useDarkStatusContent, view) {
        fun applyStatusBarAppearance() {
            view.context.findActivity()?.window?.let {
                WindowCompat.getInsetsController(it, view).apply {
                    isAppearanceLightStatusBars = useDarkStatusContent
                    isAppearanceLightNavigationBars = lightPage
                }
            }
        }
        applyStatusBarAppearance()
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_START || event == Lifecycle.Event.ON_RESUME) applyStatusBarAppearance() }
        entry?.lifecycle?.addObserver(observer)
        onDispose { entry?.lifecycle?.removeObserver(observer) }
    }
    Scaffold(containerColor = MaterialTheme.colorScheme.background, contentWindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal), bottomBar = { if (route !in setOf("sleep_settings", "prayer_time_settings", "active_session") && route?.startsWith("journey_day/") != true) {
        Column {
            Surface(Modifier.padding(horizontal = 8.dp, vertical = 2.dp), color = MaterialTheme.colorScheme.surface, tonalElevation = 2.dp, shadowElevation = 2.dp, shape = MaterialTheme.shapes.extraLarge) {
                NavigationBar(Modifier.height(64.dp), containerColor = MaterialTheme.colorScheme.surface, tonalElevation = 0.dp, windowInsets = WindowInsets(0, 0, 0, 0)) {
                    destinations.forEach { destination ->
                        val selected = route == destination.route
                        NavigationBarItem(selected, onClick = { nav.navigate(destination.route) { popUpTo(nav.graph.startDestinationId) { saveState = true }; launchSingleTop = true; restoreState = true } }, icon = { Icon(if (selected) destination.selectedIcon else destination.icon, destination.label, Modifier.size(22.dp)) }, label = { Text(destination.label, style = MaterialTheme.typography.labelSmall, maxLines = 1) }, colors = NavigationBarItemDefaults.colors(selectedIconColor = MaterialTheme.colorScheme.primary, selectedTextColor = MaterialTheme.colorScheme.primary, indicatorColor = MaterialTheme.colorScheme.primaryContainer, unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant, unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant))
                    }
                }
            }
            Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
        } }
    }) { padding ->
        NavHost(nav, "today", Modifier.padding(padding)) {
            composable("today") { val habits by vm.todayHabits.collectAsStateWithLifecycle(); val schedules by vm.schedules.collectAsStateWithLifecycle(); val activeSession by vm.activeSession.collectAsStateWithLifecycle(); val sleepPlan by sleepVm.plan.collectAsStateWithLifecycle(); val activeSleep by sleepVm.activeSession.collectAsStateWithLifecycle(); val latestSleep by sleepVm.latestCompleted.collectAsStateWithLifecycle(); val records by vm.todayPrayerRecords.collectAsStateWithLifecycle(); val reasons by vm.prayerReasons.collectAsStateWithLifecycle(); val prayerTimes by prayerTimeVm.settings.collectAsStateWithLifecycle(); TodayScreen(habits, schedules, activeSession, sleepPlan, activeSleep, latestSleep, records, reasons, prayerTimes, vm.today, vm::toggle, vm::recordPrayer, vm::clearPrayer, vm::addPrayerReason, vm::addRoutineProgress, vm::startSession, vm::pauseSession, vm::resumeSession, vm::discardSession, { openActiveSession() }, sleepVm::startSleep, sleepVm::finishSleep, { nav.navigate("sleep_settings") }, { nav.navigate("prayer_time_settings") }, { sleepPlan?.let { sleepVm.savePlan(it.copy(enabled = true)) } }, { todayUsesDarkStatusContent = it }) }
            composable("routines") { Box(Modifier.statusBarsPadding()) { val habits by vm.allHabits.collectAsStateWithLifecycle(); val todayHabits by vm.todayHabits.collectAsStateWithLifecycle(); val schedules by vm.schedules.collectAsStateWithLifecycle(); val activeSession by vm.activeSession.collectAsStateWithLifecycle(); RoutinesScreen(habits, todayHabits, schedules, activeSession, vm::addHabit, vm::editHabit, vm::setArchived, vm::toggle, vm::addRoutineProgress, vm::startSession) { openActiveSession() } } }
            composable("active_session") {
                val session by vm.activeSession.collectAsStateWithLifecycle()
                val habit by vm.activeSessionHabit.collectAsStateWithLifecycle()
                val loaded by vm.activeSessionLoaded.collectAsStateWithLifecycle()
                LaunchedEffect(loaded, session?.id) {
                    if (loaded && session == null) nav.navigate("today") { popUpTo("active_session") { inclusive = true }; launchSingleTop = true }
                }
                when {
                    session != null && habit != null -> ActiveSessionScreen(habit!!, session!!, { if (!nav.popBackStack()) nav.navigate("today") }, vm::pauseSession, vm::resumeSession, vm::finishSession, vm::discardSession)
                    else -> Box(Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) { CircularProgressIndicator() }
                }
            }
            composable("journey") { Box(Modifier.statusBarsPadding()) { val state by vm.journey.collectAsStateWithLifecycle(); val sleep by sleepVm.journeySessions.collectAsStateWithLifecycle(); val insightData by vm.insightData.collectAsStateWithLifecycle(); val insightSleep by sleepVm.insightSessions.collectAsStateWithLifecycle(); androidx.compose.runtime.LaunchedEffect(state.month) { sleepVm.selectJourneyMonth(state.month) }; JourneyScreen(state, sleep, insightData.copy(sleep = insightSleep), vm.today, { vm.selectJourneyMonth(state.month.minusMonths(1)) }, { vm.selectJourneyMonth(state.month.plusMonths(1)) }) { date -> nav.navigate("journey_day/$date") } } }
            composable("journey_day/{date}") { backStack -> val date = LocalDate.parse(requireNotNull(backStack.arguments?.getString("date"))); val habits by vm.historyHabits.collectAsStateWithLifecycle(); val records by vm.historyPrayerRecords.collectAsStateWithLifecycle(); val sleep by sleepVm.historySessions.collectAsStateWithLifecycle(); val journey by vm.journey.collectAsStateWithLifecycle(); androidx.compose.runtime.LaunchedEffect(date) { vm.selectHistoryDate(date); sleepVm.selectHistoryDate(date) }; Box(Modifier.statusBarsPadding()) { JourneyDayScreen(date, habits, records, journey.activities.filter { it.businessDate == date.toString() }, sleep) { nav.popBackStack() } } }
            composable("settings") { Box(Modifier.statusBarsPadding()) { val theme by vm.themeMode.collectAsStateWithLifecycle(); val sleepPlan by sleepVm.plan.collectAsStateWithLifecycle(); val prayerTimes by prayerTimeVm.settings.collectAsStateWithLifecycle(); SettingsScreen(theme, sleepPlan, prayerTimes, vm::setTheme, { nav.navigate("sleep_settings") }, { nav.navigate("prayer_time_settings") }) } }
            composable("sleep_settings") { val sleepPlan by sleepVm.plan.collectAsStateWithLifecycle(); SleepSettingsScreen(sleepPlan, { nav.popBackStack() }, sleepVm::savePlan) }
            composable("prayer_time_settings") { val settings by prayerTimeVm.settings.collectAsStateWithLifecycle(); val reminders by prayerTimeVm.reminders.collectAsStateWithLifecycle(); PrayerTimeSettingsScreen(settings, reminders, { nav.popBackStack() }, prayerTimeVm::saveSettings) { prayerTimeVm.saveReminder(it) } }
        }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) { is Activity -> this; is ContextWrapper -> baseContext.findActivity(); else -> null }
