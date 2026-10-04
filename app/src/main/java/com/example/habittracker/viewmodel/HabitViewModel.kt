package com.example.habittracker.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.habittracker.data.local.dao.HabitWithStatus
import com.example.habittracker.data.local.dao.PrayerRecordDetails
import com.example.habittracker.data.local.dao.ReasonCount
import com.example.habittracker.data.HabitPolicy
import com.example.habittracker.data.RoutineDraft
import com.example.habittracker.data.RoutineProgressUpdate
import com.example.habittracker.data.RoutineRules
import com.example.habittracker.data.StartSessionResult
import com.example.habittracker.data.InsightInput
import com.example.habittracker.data.InsightRules
import com.example.habittracker.data.local.dao.SessionFinishResult
import com.example.habittracker.data.local.entity.Habit
import com.example.habittracker.data.local.entity.HabitCategory
import com.example.habittracker.data.local.entity.HabitCompletion
import com.example.habittracker.data.local.entity.Prayer
import com.example.habittracker.data.local.entity.PrayerReason
import com.example.habittracker.data.local.entity.PrayerStatus
import com.example.habittracker.data.local.entity.RoutineProgress
import com.example.habittracker.data.local.entity.RoutineType
import com.example.habittracker.data.local.entity.ActivitySession
import com.example.habittracker.data.local.entity.RoutineSchedule
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
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

data class ProgressState(
    val habits: List<Habit> = emptyList(),
    val completions: List<HabitCompletion> = emptyList(),
    val prayerRecords: List<PrayerRecordDetails> = emptyList(),
    val routineProgress: List<RoutineProgress> = emptyList(),
    val month: YearMonth = YearMonth.now(),
) {
    val elapsedDays: Int get() = if (month == YearMonth.now()) LocalDate.now().dayOfMonth else month.lengthOfMonth()
    val availableHabits get() = habits.filter { it.active || completions.any { c -> c.habitId == it.id } || routineProgress.any { p -> p.habitId == it.id } }
    val primaryHabits get() = availableHabits.filter { HabitPolicy.isPrimary(it.category) }
    private val personalHabits get() = primaryHabits.filter { it.category == HabitCategory.PERSONAL }
    private val checkHabitIds get() = personalHabits.filter { it.routineType == RoutineType.CHECK }.mapTo(mutableSetOf()) { it.id }
    val primaryCompletionCount: Int get() = completions.count { it.habitId in checkHabitIds } + personalHabits.filter { it.routineType != RoutineType.CHECK }.sumOf { habit -> routineProgress.count { it.habitId == habit.id && RoutineRules.isComplete(habit.routineType, it.value, habit.target) } } + prayerRecords.count { it.status.countsAsCompleted }
    val possible: Int get() = primaryHabits.sumOf { habit ->
        val created = java.time.Instant.ofEpochMilli(habit.createdAt).atZone(java.time.ZoneId.systemDefault()).toLocalDate()
        if (habit.createdAt == 0L || created.isBefore(month.atDay(1))) elapsedDays
        else (elapsedDays - created.dayOfMonth + 1).coerceAtLeast(0)
    }
    val percentage: Int get() = if (possible == 0) 0 else primaryCompletionCount * 100 / possible
    fun percentageFor(habit: Habit): Int {
        val prayer = Prayer.fromHabitName(habit.name)
        val count = when { habit.category == HabitCategory.SALAT && prayer != null -> prayerRecords.count { it.prayer == prayer && it.status.countsAsCompleted }; habit.routineType == RoutineType.CHECK -> completions.count { it.habitId == habit.id }; else -> routineProgress.count { it.habitId == habit.id && RoutineRules.isComplete(habit.routineType, it.value, habit.target) } }
        return if (elapsedDays == 0) 0 else count * 100 / elapsedDays
    }
    fun streakFor(habit: Habit): Int {
        val prayer = Prayer.fromHabitName(habit.name)
        val dates = when { habit.category == HabitCategory.SALAT && prayer != null -> prayerRecords.filter { it.prayer == prayer && it.status.countsAsCompleted }.map { LocalDate.parse(it.date) }.toSet(); habit.routineType == RoutineType.CHECK -> completions.filter { it.habitId == habit.id }.map { LocalDate.parse(it.date) }.toSet(); else -> routineProgress.filter { it.habitId == habit.id && RoutineRules.isComplete(habit.routineType, it.value, habit.target) }.map { LocalDate.parse(it.date) }.toSet() }
        var date = LocalDate.now()
        if (date !in dates) date = date.minusDays(1)
        var streak = 0
        while (date in dates) { streak++; date = date.minusDays(1) }
        return streak
    }
}

data class PrayerMonthStats(val completed: Int, val jamaah: Int, val qaza: Int, val missed: Int)

data class JourneyState(
    val habits: List<Habit> = emptyList(),
    val completions: List<HabitCompletion> = emptyList(),
    val prayerRecords: List<PrayerRecordDetails> = emptyList(),
    val routineProgress: List<RoutineProgress> = emptyList(),
    val schedules: List<RoutineSchedule> = emptyList(),
    val activities: List<ActivitySession> = emptyList(),
    val month: YearMonth = YearMonth.now(),
)

private data class JourneyBase(
    val habits: List<Habit>, val completions: List<HabitCompletion>,
    val prayers: List<PrayerRecordDetails>, val progress: List<RoutineProgress>,
    val schedules: List<RoutineSchedule>, val month: YearMonth,
)

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
    val schedules = repository.schedules().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    private val activeSessionLoadedState = MutableStateFlow(false)
    val activeSessionLoaded: StateFlow<Boolean> = activeSessionLoadedState
    val activeSession = repository.activeSession().onEach { activeSessionLoadedState.value = true }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    val activeSessionHabit = activeSession.flatMapLatest { session ->
        if (session == null) kotlinx.coroutines.flow.flowOf(null)
        else repository.habitsForDate(session.businessDate).map { habits -> habits.firstOrNull { it.id == session.habitId } }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    val themeMode = preferences.themeMode.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ThemeMode.SYSTEM)
    private val month = YearMonth.now()
    val progress = combine(
        repository.allHabits(),
        repository.completions(month.atDay(1).toString(), month.atEndOfMonth().toString()),
        prayerRepository.recordsBetween(month.atDay(1).toString(), month.atEndOfMonth().toString()),
        repository.progress(month.atDay(1).toString(), month.atEndOfMonth().toString()),
    ) { habits, completions, prayerRecords, routineProgress -> ProgressState(habits, completions, prayerRecords, routineProgress, month) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProgressState())

    private val journeyMonth = MutableStateFlow(YearMonth.now())
    val selectedJourneyMonth: StateFlow<YearMonth> = journeyMonth
    private val journeyBase = journeyMonth.flatMapLatest { selected ->
        combine(
            repository.allHabits(),
            repository.completions(selected.atDay(1).toString(), selected.atEndOfMonth().toString()),
            prayerRepository.recordsBetween(selected.atDay(1).toString(), selected.atEndOfMonth().toString()),
            repository.progress(selected.atDay(1).toString(), selected.atEndOfMonth().toString()),
            repository.schedules(),
        ) { habits, completions, prayers, progress, schedules -> JourneyBase(habits, completions, prayers, progress, schedules, selected) }
    }
    val journey = journeyBase.flatMapLatest { base ->
        repository.finishedSessions(base.month.atDay(1).toString(), base.month.atEndOfMonth().toString()).combine(kotlinx.coroutines.flow.flowOf(base)) { activities, value ->
            JourneyState(value.habits, value.completions, value.prayers, value.progress, value.schedules, activities, value.month)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), JourneyState())
    private val insightPeriods = InsightRules.periods(today)
    private val insightBase = combine(
        repository.allHabits(),
        repository.completions(insightPeriods.previous.start.toString(), insightPeriods.current.endInclusive.toString()),
        prayerRepository.recordsBetween(insightPeriods.previous.start.toString(), insightPeriods.current.endInclusive.toString()),
        repository.progress(insightPeriods.previous.start.toString(), insightPeriods.current.endInclusive.toString()),
        repository.schedules(),
    ) { habits, completions, prayers, progress, schedules -> InsightInput(habits, completions, prayers, progress, schedules, emptyList(), emptyList()) }
    val insightData = insightBase.flatMapLatest { base ->
        repository.finishedSessions(insightPeriods.previous.start.toString(), insightPeriods.current.endInclusive.toString()).combine(kotlinx.coroutines.flow.flowOf(base)) { sessions, value -> value.copy(activities = sessions) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), InsightInput(emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList()))

    fun selectJourneyMonth(value: YearMonth) { if (!value.isAfter(YearMonth.now())) journeyMonth.value = value }
    fun selectHistoryDate(date: LocalDate) { if (!date.isAfter(today)) selectedDate.value = date }
    fun toggle(habit: HabitWithStatus, date: LocalDate = today) = viewModelScope.launch { repository.toggle(habit.id, date.toString(), !habit.completed) }
    fun recordPrayer(prayer: Prayer, status: PrayerStatus, inJamaah: Boolean = false, reasonId: Long? = null, onSaved: (PrayerUpdateResult) -> Unit = {}) = viewModelScope.launch {
        onSaved(prayerRepository.recordPrayer(today.toString(), prayer, status, inJamaah, reasonId))
    }
    fun clearPrayer(prayer: Prayer) = viewModelScope.launch { prayerRepository.clearRecord(today.toString(), prayer) }
    fun addPrayerReason(name: String, onResult: (Result<Long>) -> Unit) = viewModelScope.launch { onResult(prayerRepository.addCustomReason(name)) }
    fun addHabit(draft: RoutineDraft, onResult: (Result<Unit>) -> Unit = {}) = viewModelScope.launch { onResult(repository.addHabit(draft)) }
    fun editHabit(habit: Habit, draft: RoutineDraft, onResult: (Result<Unit>) -> Unit = {}) = viewModelScope.launch { onResult(repository.updateHabit(habit, draft = draft)) }
    fun setArchived(habit: Habit, archived: Boolean, onResult: (Result<Unit>) -> Unit = {}) = viewModelScope.launch { onResult(repository.updateHabit(habit, active = !archived)) }
    fun addRoutineProgress(habit: HabitWithStatus, delta: Int, onSaved: (RoutineProgressUpdate) -> Unit = {}) = viewModelScope.launch {
        onSaved(repository.addProgress(Habit(habit.id, habit.name, habit.category, habit.active, habit.createdAt, habit.sortOrder, habit.isBuiltIn, habit.routineType, habit.target, habit.unit, habit.iconKey, habit.themeKey, habit.quantityPerCount, habit.measurementUnit), today.toString(), delta))
    }
    fun startSession(habitId: Long, onResult: (StartSessionResult) -> Unit = {}) = viewModelScope.launch { onResult(repository.startSession(habitId, today)) }
    fun pauseSession() = viewModelScope.launch { repository.pauseSession() }
    fun resumeSession() = viewModelScope.launch { repository.resumeSession() }
    fun finishSession(allowShort: Boolean = false, onResult: (SessionFinishResult?) -> Unit = {}) = viewModelScope.launch { onResult(repository.finishSession(allowShort)) }
    fun discardSession() = viewModelScope.launch { repository.discardSession() }
    fun setTheme(mode: ThemeMode) = viewModelScope.launch { preferences.setThemeMode(mode) }

    class Factory(private val repository: HabitRepository, private val prayerRepository: PrayerRepository, private val preferences: ThemePreferences) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = HabitViewModel(repository, prayerRepository, preferences) as T
    }
}
