package com.example.habittracker.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.habittracker.data.local.dao.HabitWithStatus
import com.example.habittracker.data.local.dao.PrayerRecordDetails
import com.example.habittracker.data.local.dao.ReasonCount
import com.example.habittracker.data.HabitPolicy
import com.example.habittracker.data.local.entity.Habit
import com.example.habittracker.data.local.entity.HabitCategory
import com.example.habittracker.data.local.entity.HabitCompletion
import com.example.habittracker.data.local.entity.Prayer
import com.example.habittracker.data.local.entity.PrayerReason
import com.example.habittracker.data.local.entity.PrayerStatus
import com.example.habittracker.data.repository.HabitRepository
import com.example.habittracker.data.repository.PrayerRepository
import com.example.habittracker.data.repository.PrayerUpdateResult
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
    val prayerRecords: List<PrayerRecordDetails> = emptyList(),
    val month: YearMonth = YearMonth.now(),
) {
    val elapsedDays: Int get() = if (month == YearMonth.now()) LocalDate.now().dayOfMonth else month.lengthOfMonth()
    val availableHabits get() = habits.filter { it.active || completions.any { c -> c.habitId == it.id } }
    val primaryHabits get() = availableHabits.filter { HabitPolicy.isPrimary(it.category) }
    private val personalHabitIds get() = primaryHabits.filter { it.category == HabitCategory.PERSONAL }.mapTo(mutableSetOf()) { it.id }
    val primaryCompletionCount: Int get() = completions.count { it.habitId in personalHabitIds } + prayerRecords.count { it.status.countsAsCompleted }
    val possible: Int get() = primaryHabits.sumOf { habit ->
        val created = java.time.Instant.ofEpochMilli(habit.createdAt).atZone(java.time.ZoneId.systemDefault()).toLocalDate()
        if (habit.createdAt == 0L || created.isBefore(month.atDay(1))) elapsedDays
        else (elapsedDays - created.dayOfMonth + 1).coerceAtLeast(0)
    }
    val percentage: Int get() = if (possible == 0) 0 else primaryCompletionCount * 100 / possible
    fun percentageFor(habit: Habit): Int {
        val prayer = Prayer.fromHabitName(habit.name)
        val count = if (habit.category == HabitCategory.SALAT && prayer != null) prayerRecords.count { it.prayer == prayer && it.status.countsAsCompleted } else completions.count { it.habitId == habit.id }
        return if (elapsedDays == 0) 0 else count * 100 / elapsedDays
    }
    fun streakFor(habit: Habit): Int {
        val prayer = Prayer.fromHabitName(habit.name)
        val dates = if (habit.category == HabitCategory.SALAT && prayer != null) prayerRecords.filter { it.prayer == prayer && it.status.countsAsCompleted }.map { LocalDate.parse(it.date) }.toSet() else completions.filter { it.habitId == habit.id }.map { LocalDate.parse(it.date) }.toSet()
        var date = LocalDate.now()
        if (date !in dates) date = date.minusDays(1)
        var streak = 0
        while (date in dates) { streak++; date = date.minusDays(1) }
        return streak
    }
}

data class PrayerMonthStats(val completed: Int, val jamaah: Int, val qaza: Int, val missed: Int)

val ProgressState.prayerStats: PrayerMonthStats get() = PrayerMonthStats(
    completed = prayerRecords.count { it.status == PrayerStatus.COMPLETED },
    jamaah = prayerRecords.count { it.status == PrayerStatus.COMPLETED && it.inJamaah },
    qaza = prayerRecords.count { it.status == PrayerStatus.QAZA },
    missed = prayerRecords.count { it.status == PrayerStatus.MISSED },
)

@OptIn(ExperimentalCoroutinesApi::class)
class HabitViewModel(
    private val repository: HabitRepository,
    private val prayerRepository: PrayerRepository,
    private val preferences: ThemePreferences,
) : ViewModel() {
    val today = LocalDate.now()
    private val selectedDate = MutableStateFlow(today.minusDays(1))
    val todayHabits = repository.habitsForDate(today.toString()).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val historyHabits = selectedDate.flatMapLatest { repository.habitsForDate(it.toString()) }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val historyDate: StateFlow<LocalDate> = selectedDate
    val todayPrayerRecords = prayerRepository.recordsForDate(today.toString()).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val historyPrayerRecords = selectedDate.flatMapLatest { prayerRepository.recordsForDate(it.toString()) }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val prayerReasons = prayerRepository.reasons().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val totalPrayerXp = prayerRepository.totalXp().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)
    val missedReasonCounts: StateFlow<List<ReasonCount>> = prayerRepository.missedReasonCounts().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val qazaReasonCounts: StateFlow<List<ReasonCount>> = prayerRepository.qazaReasonCounts().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val allHabits = repository.allHabits().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val themeMode = preferences.themeMode.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ThemeMode.SYSTEM)
    private val month = YearMonth.now()
    val progress = combine(
        repository.allHabits(),
        repository.completions(month.atDay(1).toString(), month.atEndOfMonth().toString()),
        prayerRepository.recordsBetween(month.atDay(1).toString(), month.atEndOfMonth().toString()),
    ) { habits, completions, prayerRecords -> ProgressState(habits, completions, prayerRecords, month) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProgressState())

    fun selectHistoryDate(date: LocalDate) { if (date.isBefore(today)) selectedDate.value = date }
    fun toggle(habit: HabitWithStatus, date: LocalDate = today) = viewModelScope.launch { repository.toggle(habit.id, date.toString(), !habit.completed) }
    fun recordPrayer(prayer: Prayer, status: PrayerStatus, inJamaah: Boolean = false, reasonId: Long? = null, onSaved: (PrayerUpdateResult) -> Unit = {}) = viewModelScope.launch {
        onSaved(prayerRepository.recordPrayer(today.toString(), prayer, status, inJamaah, reasonId))
    }
    fun clearPrayer(prayer: Prayer) = viewModelScope.launch { prayerRepository.clearRecord(today.toString(), prayer) }
    fun addPrayerReason(name: String, onResult: (Result<Long>) -> Unit) = viewModelScope.launch { onResult(prayerRepository.addCustomReason(name)) }
    fun addHabit(name: String) = viewModelScope.launch { if (name.isNotBlank()) repository.addHabit(name) }
    fun editHabit(habit: Habit, name: String) = viewModelScope.launch { if (name.isNotBlank()) repository.updateHabit(habit, name = name) }
    fun setArchived(habit: Habit, archived: Boolean) = viewModelScope.launch { repository.updateHabit(habit, active = !archived) }
    fun setTheme(mode: ThemeMode) = viewModelScope.launch { preferences.setThemeMode(mode) }

    class Factory(private val repository: HabitRepository, private val prayerRepository: PrayerRepository, private val preferences: ThemePreferences) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = HabitViewModel(repository, prayerRepository, preferences) as T
    }
}
