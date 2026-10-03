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
        Surface(color = MaterialTheme.colorScheme.surface, tonalElevation = 1.dp) {
            androidx.compose.foundation.layout.Column {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = .55f))
                NavigationBar(containerColor = MaterialTheme.colorScheme.surface, tonalElevation = 0.dp) {
                    destinations.forEach { destination ->
                        val selected = route == destination.route
                        NavigationBarItem(selected, onClick = { nav.navigate(destination.route) { popUpTo(nav.graph.startDestinationId) { saveState = true }; launchSingleTop = true; restoreState = true } }, icon = { Icon(if (selected) destination.selectedIcon else destination.icon, destination.label) }, label = { Text(destination.label) }, colors = NavigationBarItemDefaults.colors(selectedIconColor = MaterialTheme.colorScheme.primary, selectedTextColor = MaterialTheme.colorScheme.primary, indicatorColor = MaterialTheme.colorScheme.primaryContainer, unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant, unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant))
                    }
                }
            }
        }
    }) { padding ->
        NavHost(nav, "today", Modifier.padding(padding)) {
            composable("today") { val habits by vm.todayHabits.collectAsStateWithLifecycle(); val records by vm.todayPrayerRecords.collectAsStateWithLifecycle(); val reasons by vm.prayerReasons.collectAsStateWithLifecycle(); TodayScreen(habits, records, reasons, vm.today, vm::toggle, vm::recordPrayer, vm::clearPrayer, vm::addPrayerReason) }
            composable("history") { val date by vm.historyDate.collectAsStateWithLifecycle(); val habits by vm.historyHabits.collectAsStateWithLifecycle(); val records by vm.historyPrayerRecords.collectAsStateWithLifecycle(); HistoryScreen(date, habits, records, vm::selectHistoryDate) }
            composable("progress") { val state by vm.progress.collectAsStateWithLifecycle(); ProgressScreen(state) }
            composable("settings") { val habits by vm.allHabits.collectAsStateWithLifecycle(); val theme by vm.themeMode.collectAsStateWithLifecycle(); SettingsScreen(habits, theme, vm::addHabit, vm::editHabit, vm::setArchived, vm::setTheme) }
        }
    }
}
