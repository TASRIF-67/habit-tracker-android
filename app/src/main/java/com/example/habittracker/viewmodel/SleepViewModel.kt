package com.example.habittracker.viewmodel

import androidx.lifecycle.*
import com.example.habittracker.data.*
import com.example.habittracker.data.local.entity.SleepPlan
import com.example.habittracker.data.local.entity.SleepSession
import com.example.habittracker.data.repository.SleepRepository
import java.time.LocalDate
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class SleepViewModel(private val repository: SleepRepository) : ViewModel() {
    val plan = repository.plan().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    val activeSession = repository.activeSession().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    val latestCompleted = repository.latestCompleted().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    private val historyDate = MutableStateFlow(LocalDate.now().minusDays(1))
    val historySessions = historyDate.flatMapLatest { repository.sessionsForDate(it.toString()) }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    private val journeyMonth = MutableStateFlow(java.time.YearMonth.now())
    val journeySessions = journeyMonth.flatMapLatest { month -> repository.sessionsBetween(month.atDay(1).toString(), month.atEndOfMonth().toString()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    private val insightPeriods = InsightRules.periods(LocalDate.now())
    val insightSessions = repository.sessionsBetween(insightPeriods.previous.start.toString(), insightPeriods.current.endInclusive.toString())
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun selectHistoryDate(date: LocalDate) { historyDate.value = date }
    fun selectJourneyMonth(month: java.time.YearMonth) { journeyMonth.value = month }
    fun savePlan(plan: SleepPlan, onResult: (Result<Unit>) -> Unit = {}) = viewModelScope.launch { onResult(repository.savePlan(plan)) }
    fun startSleep(onResult: (StartSleepResult) -> Unit = {}) = viewModelScope.launch { onResult(repository.startSleep()) }
    fun finishSleep(allowLong: Boolean = false, onResult: (FinishSleepResult) -> Unit = {}) = viewModelScope.launch { onResult(repository.finishSleep(allowLong = allowLong)) }
    fun correctSession(id: Long, bedtime: Long, wake: Long, onResult: (Result<Unit>) -> Unit = {}) = viewModelScope.launch { onResult(repository.correctSession(id, bedtime, wake)) }

    class Factory(private val repository: SleepRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = SleepViewModel(repository) as T
    }
}
