package com.example.habittracker.preferences

import android.content.Context
import androidx.datastore.preferences.core.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

enum class OnboardingStep { WELCOME, PRAYER, ROUTINE, NOTIFICATIONS, SLEEP, FINISH, COMPLETE }
data class OnboardingState(val ready: Boolean = false, val step: OnboardingStep = OnboardingStep.WELCOME) { val completed get() = step == OnboardingStep.COMPLETE }

object OnboardingRules {
    fun initialStep(markerExists: Boolean, freshInstall: Boolean, stored: OnboardingStep?): OnboardingStep = when {
        markerExists -> stored ?: OnboardingStep.COMPLETE
        freshInstall -> OnboardingStep.WELCOME
        else -> OnboardingStep.COMPLETE
    }
    fun next(step: OnboardingStep) = when (step) { OnboardingStep.WELCOME -> OnboardingStep.PRAYER; OnboardingStep.PRAYER -> OnboardingStep.ROUTINE; OnboardingStep.ROUTINE -> OnboardingStep.NOTIFICATIONS; OnboardingStep.NOTIFICATIONS -> OnboardingStep.SLEEP; OnboardingStep.SLEEP -> OnboardingStep.FINISH; OnboardingStep.FINISH, OnboardingStep.COMPLETE -> OnboardingStep.COMPLETE }
    fun previous(step: OnboardingStep) = when (step) { OnboardingStep.PRAYER -> OnboardingStep.WELCOME; OnboardingStep.ROUTINE -> OnboardingStep.PRAYER; OnboardingStep.NOTIFICATIONS -> OnboardingStep.ROUTINE; OnboardingStep.SLEEP -> OnboardingStep.NOTIFICATIONS; OnboardingStep.FINISH -> OnboardingStep.SLEEP; else -> step }
}

class OnboardingPreferences(private val context: Context) {
    private val initializedKey = booleanPreferencesKey("onboarding_initialized")
    private val stepKey = stringPreferencesKey("onboarding_step")
    val state: Flow<OnboardingState> = context.dataStore.data.map { values ->
        val initialized = values[initializedKey] == true
        if (!initialized) OnboardingState() else OnboardingState(true, values[stepKey]?.let { runCatching { OnboardingStep.valueOf(it) }.getOrNull() } ?: OnboardingStep.COMPLETE)
    }
    suspend fun initialize(freshInstall: Boolean) { context.dataStore.edit { values -> if (values[initializedKey] != true) { values[initializedKey] = true; values[stepKey] = OnboardingRules.initialStep(false, freshInstall, null).name } } }
    suspend fun setStep(step: OnboardingStep) { context.dataStore.edit { it[initializedKey] = true; it[stepKey] = step.name } }
}
