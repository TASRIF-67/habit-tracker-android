package com.example.habittracker

import com.example.habittracker.data.RoutineDraft
import com.example.habittracker.data.RoutineRules
import com.example.habittracker.data.RoutineIdentity
import com.example.habittracker.data.local.entity.Habit
import com.example.habittracker.data.local.entity.HabitCategory
import com.example.habittracker.data.local.entity.RoutineProgress
import com.example.habittracker.data.local.entity.RoutineType
import com.example.habittracker.viewmodel.ProgressState
import java.time.YearMonth
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RoutineRulesTest {
    @Test fun checkSemanticsFollowCheckedState() {
        assertFalse(RoutineRules.isComplete(RoutineType.CHECK, 0, null, false))
        assertTrue(RoutineRules.isComplete(RoutineType.CHECK, 0, null, true))
    }

    @Test fun durationThresholdAllowsProgressBeyondTarget() {
        assertFalse(RoutineRules.isComplete(RoutineType.DURATION, 29, 30))
        assertTrue(RoutineRules.isComplete(RoutineType.DURATION, 30, 30))
        assertTrue(RoutineRules.isComplete(RoutineType.DURATION, 45, 30))
    }

    @Test fun countThresholdAllowsProgressBeyondTarget() {
        assertFalse(RoutineRules.isComplete(RoutineType.COUNT, 7, 8))
        assertTrue(RoutineRules.isComplete(RoutineType.COUNT, 8, 8))
        assertTrue(RoutineRules.isComplete(RoutineType.COUNT, 10, 8))
    }

    @Test fun validationNormalizesInputsAndRejectsInvalidTargets() {
        assertEquals(RoutineDraft("Drink water", RoutineType.COUNT, 8, "glasses"), RoutineRules.normalized(RoutineDraft(" Drink   water ", RoutineType.COUNT, 8, " glasses ")).getOrThrow())
        assertTrue(RoutineRules.normalized(RoutineDraft("Walk", RoutineType.DURATION, 0)).isFailure)
        assertTrue(RoutineRules.normalized(RoutineDraft("Water", RoutineType.COUNT, 8, " ")).isFailure)
    }

    @Test fun optionalMeasurementRequiresAValidPair() {
        assertTrue(RoutineRules.normalized(RoutineDraft("Water", RoutineType.COUNT, 4, "bottles", quantityPerCount = 500.0, measurementUnit = "mL")).isSuccess)
        assertTrue(RoutineRules.normalized(RoutineDraft("Water", RoutineType.COUNT, 4, "bottles", quantityPerCount = 500.0)).isFailure)
        assertTrue(RoutineRules.normalized(RoutineDraft("Water", RoutineType.COUNT, 4, "bottles", quantityPerCount = 0.0, measurementUnit = "mL")).isFailure)
        assertTrue(RoutineRules.normalized(RoutineDraft("Water", RoutineType.COUNT, 4, "bottles", quantityPerCount = Double.NaN, measurementUnit = "mL")).isFailure)
    }

    @Test fun derivedMeasurementFormatsConversionsAndOverTargetValues() {
        assertEquals("0 mL / 2 L", RoutineRules.derivedMeasurement(0, 4, 500.0, "mL"))
        assertEquals("500 mL / 2 L", RoutineRules.derivedMeasurement(1, 4, 500.0, "mL"))
        assertEquals("1 L / 2 L", RoutineRules.derivedMeasurement(2, 4, 500.0, "mL"))
        assertEquals("2.5 L / 2 L", RoutineRules.derivedMeasurement(5, 4, 500.0, "mL"))
        assertEquals("750 g / 1.5 kg", RoutineRules.derivedMeasurement(1, 2, 750.0, "g"))
    }

    @Test fun unknownIdentityKeysHaveSafeFallbacks() {
        assertEquals("CHECK", RoutineIdentity.iconOrDefault("UNKNOWN"))
        assertEquals("FOREST", RoutineIdentity.themeOrDefault("UNKNOWN"))
    }

    @Test fun primaryCompletionCountsNumericRoutineOnceAndKeepsArchivedHistory() {
        val routine = Habit(7, "Walk", HabitCategory.PERSONAL, active = false, createdAt = 0, routineType = RoutineType.DURATION, target = 30, unit = "min")
        val state = ProgressState(habits = listOf(routine), routineProgress = listOf(RoutineProgress(1, 7, "2026-10-01", 45)), month = YearMonth.of(2026, 10))
        assertEquals(1, state.primaryCompletionCount)
        assertTrue(routine in state.availableHabits)
    }
}
