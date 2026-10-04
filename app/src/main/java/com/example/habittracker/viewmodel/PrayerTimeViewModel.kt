package com.example.habittracker.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.habittracker.data.local.entity.PrayerReminderConfig
import com.example.habittracker.data.local.entity.PrayerTimeSettings
import com.example.habittracker.data.repository.PrayerTimeRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PrayerTimeViewModel(private val repository: PrayerTimeRepository) : ViewModel() {
    val settings = repository.settings().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    val reminders = repository.reminders().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun saveSettings(settings: PrayerTimeSettings, onResult: (Result<Unit>) -> Unit = {}) =
        viewModelScope.launch { onResult(repository.saveSettings(settings)) }

    fun saveReminder(config: PrayerReminderConfig, onResult: (Result<Unit>) -> Unit = {}) =
        viewModelScope.launch { onResult(repository.saveReminder(config)) }

    class Factory(private val repository: PrayerTimeRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = PrayerTimeViewModel(repository) as T
    }
}
