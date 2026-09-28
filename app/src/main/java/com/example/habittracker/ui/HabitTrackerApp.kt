package com.example.habittracker.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.*
import com.example.habittracker.ui.screens.history.HistoryScreen
import com.example.habittracker.ui.screens.progress.ProgressScreen
import com.example.habittracker.ui.screens.settings.SettingsScreen
import com.example.habittracker.ui.screens.today.TodayScreen
import com.example.habittracker.viewmodel.HabitViewModel

private data class Destination(val route: String, val label: String, val icon: ImageVector, val selectedIcon: ImageVector)

@Composable
fun HabitTrackerApp(vm: HabitViewModel) {
    val nav = rememberNavController()
    val destinations = listOf(
        Destination("today", "Today", Icons.Outlined.Today, Icons.Filled.Today),
        Destination("history", "History", Icons.Outlined.CalendarMonth, Icons.Filled.CalendarMonth),
        Destination("progress", "Progress", Icons.Outlined.Insights, Icons.Filled.Insights),
        Destination("settings", "Settings", Icons.Outlined.Settings, Icons.Filled.Settings),
    )
    val entry by nav.currentBackStackEntryAsState()
    val route = entry?.destination?.route
    Scaffold(containerColor = MaterialTheme.colorScheme.background, bottomBar = {
        NavigationBar(containerColor = MaterialTheme.colorScheme.surface, tonalElevation = 2.dp) {
            destinations.forEach { destination ->
                val selected = route == destination.route
                NavigationBarItem(selected, onClick = { nav.navigate(destination.route) { popUpTo(nav.graph.startDestinationId) { saveState = true }; launchSingleTop = true; restoreState = true } }, icon = { Icon(if (selected) destination.selectedIcon else destination.icon, destination.label) }, label = { Text(destination.label) }, colors = NavigationBarItemDefaults.colors(indicatorColor = MaterialTheme.colorScheme.primaryContainer))
            }
        }
    }) { padding ->
        NavHost(nav, "today", Modifier.padding(padding)) {
            composable("today") { val habits by vm.todayHabits.collectAsStateWithLifecycle(); TodayScreen(habits, vm.today, vm::toggle) }
            composable("history") { val date by vm.historyDate.collectAsStateWithLifecycle(); val habits by vm.historyHabits.collectAsStateWithLifecycle(); HistoryScreen(date, habits, vm::selectHistoryDate) }
            composable("progress") { val state by vm.progress.collectAsStateWithLifecycle(); ProgressScreen(state) }
            composable("settings") { val habits by vm.allHabits.collectAsStateWithLifecycle(); val theme by vm.themeMode.collectAsStateWithLifecycle(); SettingsScreen(habits, theme, vm::addHabit, vm::editHabit, vm::setArchived, vm::setTheme) }
        }
    }
}
