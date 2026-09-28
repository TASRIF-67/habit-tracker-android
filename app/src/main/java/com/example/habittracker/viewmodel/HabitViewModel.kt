package com.example.habittracker.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.habittracker.data.local.dao.HabitWithStatus
import com.example.habittracker.data.local.entity.Habit
import com.example.habittracker.data.local.entity.HabitCompletion
import com.example.habittracker.data.repository.HabitRepository
import com.example.habittracker.preferences.ThemeMode
import com.example.habittracker.preferences.ThemePreferences
import java.time.LocalDate
import java.time.YearMonth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ProgressState(
    val habits: List<Habit> = emptyList(),
    val completions: List<HabitCompletion> = emptyList(),
    val month: YearMonth = YearMonth.now(),
) {
    val elapsedDays: Int get() = if (month == YearMonth.now()) LocalDate.now().dayOfMonth else month.lengthOfMonth()
    val availableHabits get() = habits.filter { it.active || completions.any { c -> c.habitId == it.id } }
    val possible: Int get() = availableHabits.sumOf { habit ->
        val created = java.time.Instant.ofEpochMilli(habit.createdAt).atZone(java.time.ZoneId.systemDefault()).toLocalDate()
        if (habit.createdAt == 0L || created.isBefore(month.atDay(1))) elapsedDays
        else (elapsedDays - created.dayOfMonth + 1).coerceAtLeast(0)
    }
    val percentage: Int get() = if (possible == 0) 0 else completions.size * 100 / possible
    fun percentageFor(habit: Habit): Int {
        val count = completions.count { it.habitId == habit.id }
        return if (elapsedDays == 0) 0 else count * 100 / elapsedDays
    }
    fun streakFor(habit: Habit): Int {
        val dates = completions.filter { it.habitId == habit.id }.map { LocalDate.parse(it.date) }.toSet()
        var date = LocalDate.now()
        if (date !in dates) date = date.minusDays(1)
        var streak = 0
        while (date in dates) { streak++; date = date.minusDays(1) }
        return streak
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class HabitViewModel(
    private val repository: HabitRepository,
    private val preferences: ThemePreferences,
) : ViewModel() {
    val today = LocalDate.now()
    private val selectedDate = MutableStateFlow(today.minusDays(1))
    val todayHabits = repository.habitsForDate(today.toString()).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val historyHabits = selectedDate.flatMapLatest { repository.habitsForDate(it.toString()) }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val historyDate: StateFlow<LocalDate> = selectedDate
    val allHabits = repository.allHabits().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val themeMode = preferences.themeMode.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ThemeMode.SYSTEM)
    private val month = YearMonth.now()
    val progress = combine(
        repository.allHabits(),
        repository.completions(month.atDay(1).toString(), month.atEndOfMonth().toString())
    ) { habits, completions -> ProgressState(habits, completions, month) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProgressState())

    fun selectHistoryDate(date: LocalDate) { if (date.isBefore(today)) selectedDate.value = date }
    fun toggle(habit: HabitWithStatus, date: LocalDate = today) = viewModelScope.launch { repository.toggle(habit.id, date.toString(), !habit.completed) }
    fun addHabit(name: String) = viewModelScope.launch { if (name.isNotBlank()) repository.addHabit(name) }
    fun editHabit(habit: Habit, name: String) = viewModelScope.launch { if (name.isNotBlank()) repository.updateHabit(habit, name = name) }
    fun setArchived(habit: Habit, archived: Boolean) = viewModelScope.launch { repository.updateHabit(habit, active = !archived) }
    fun setTheme(mode: ThemeMode) = viewModelScope.launch { preferences.setThemeMode(mode) }

    class Factory(private val repository: HabitRepository, private val preferences: ThemePreferences) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = HabitViewModel(repository, preferences) as T
    }
}
