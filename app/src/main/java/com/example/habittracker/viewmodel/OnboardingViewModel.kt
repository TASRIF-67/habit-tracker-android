package com.example.habittracker.viewmodel

import android.content.Context
import androidx.lifecycle.*
import com.example.habittracker.preferences.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class OnboardingViewModel(private val preferences: OnboardingPreferences, context: Context) : ViewModel() {
    val state = preferences.state.stateIn(viewModelScope, SharingStarted.Eagerly, OnboardingState())
    init {
        val info = context.packageManager.getPackageInfo(context.packageName, 0)
        viewModelScope.launch { preferences.initialize(info.firstInstallTime == info.lastUpdateTime) }
    }
    fun next() = set(OnboardingRules.next(state.value.step))
    fun back() = set(OnboardingRules.previous(state.value.step))
    fun complete() = set(OnboardingStep.COMPLETE)
    private fun set(step: OnboardingStep) = viewModelScope.launch { preferences.setStep(step) }
    class Factory(private val preferences: OnboardingPreferences, private val context: Context) : ViewModelProvider.Factory { @Suppress("UNCHECKED_CAST") override fun <T : ViewModel> create(modelClass: Class<T>): T = OnboardingViewModel(preferences, context) as T }
}
