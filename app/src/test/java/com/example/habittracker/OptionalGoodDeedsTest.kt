package com.example.habittracker

import com.example.habittracker.data.HabitPolicy
import com.example.habittracker.data.local.dao.HabitWithStatus
import com.example.habittracker.data.local.dao.PrayerRecordDetails
import com.example.habittracker.data.local.entity.HabitCategory
import com.example.habittracker.data.local.entity.Habit
import com.example.habittracker.data.local.entity.Prayer
import com.example.habittracker.data.local.entity.PrayerStatus
import com.example.habittracker.viewmodel.ProgressState
import java.time.YearMonth
import org.junit.Assert.assertEquals
import org.junit.Test

class OptionalGoodDeedsTest {
    @Test
    fun allPrayersCompleteWithNoGoodDeedsIsComplete() {
        val summary = HabitPolicy.primarySummary(prayers(completed = 5) + goodDeeds(completed = 0))
        assertEquals(5, summary.completed)
        assertEquals(5, summary.total)
        assertEquals(100, summary.percentage)
    }

    @Test
    fun optionalGoodDeedsDoNotCompensateForMissingPrayer() {
        val summary = HabitPolicy.primarySummary(prayers(completed = 4) + goodDeeds(completed = 4))
        assertEquals(4, summary.completed)
        assertEquals(5, summary.total)
        assertEquals(80, summary.percentage)
    }

    @Test
    fun personalHabitsRemainPartOfPrimaryCompletion() {
        val personal = habit(20, "Study", HabitCategory.PERSONAL, completed = false)
        val summary = HabitPolicy.primarySummary(prayers(completed = 5) + goodDeeds(completed = 4) + personal)
        assertEquals(5, summary.completed)
        assertEquals(6, summary.total)
        assertEquals(83, summary.percentage)
    }

    @Test
    fun optionalCompletionsDoNotChangeMonthlyPrimaryPercentage() {
        val prayer = Habit(id = 1, name = "Fajr", category = HabitCategory.SALAT, createdAt = 0L)
        val optional = Habit(id = 2, name = "Quran", category = HabitCategory.GOOD_DEED, createdAt = 0L)
        val prayerRecord = PrayerRecordDetails(1, Prayer.FAJR, "2025-02-01", PrayerStatus.COMPLETED, false, null, 0, 0, null)
        val primaryOnly = ProgressState(habits = listOf(prayer, optional), prayerRecords = listOf(prayerRecord), month = YearMonth.of(2025, 2))
        val withOptional = ProgressState(habits = listOf(prayer, optional), prayerRecords = listOf(prayerRecord), month = YearMonth.of(2025, 2))
        assertEquals(primaryOnly.percentage, withOptional.percentage)
        assertEquals(1, withOptional.primaryCompletionCount)
    }

    @Test
    fun checkingAndUncheckingOptionalHabitLeavesPrimarySummaryUnchanged() {
        val unchecked = HabitPolicy.primarySummary(prayers(completed = 5) + goodDeeds(completed = 0))
        val checked = HabitPolicy.primarySummary(prayers(completed = 5) + goodDeeds(completed = 1))
        assertEquals(unchecked, checked)
    }

    private fun prayers(completed: Int) = listOf("Fajr", "Dhuhr", "Asr", "Maghrib", "Isha").mapIndexed { index, name -> habit(index.toLong() + 1, name, HabitCategory.SALAT, index < completed) }
    private fun goodDeeds(completed: Int) = listOf("Quran", "Morning Adhkar", "Evening Adhkar", "Sadaqah").mapIndexed { index, name -> habit(index.toLong() + 10, name, HabitCategory.GOOD_DEED, index < completed) }
    private fun habit(id: Long, name: String, category: HabitCategory, completed: Boolean) = HabitWithStatus(id = id, name = name, category = category, active = true, createdAt = 0L, sortOrder = id.toInt(), isBuiltIn = category != HabitCategory.PERSONAL, routineType = com.example.habittracker.data.local.entity.RoutineType.CHECK, target = null, unit = null, value = 0, completed = completed)
}
