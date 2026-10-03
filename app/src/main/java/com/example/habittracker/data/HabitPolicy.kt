package com.example.habittracker.data

import com.example.habittracker.data.local.entity.HabitCategory
import com.example.habittracker.data.local.dao.HabitWithStatus

data class DailyCompletionSummary(val completed: Int, val total: Int) {
    val percentage: Int get() = if (total == 0) 0 else completed * 100 / total
}

/** V2 policy derived from the existing category; no persisted schema change is required. */
object HabitPolicy {
    fun isOptional(category: HabitCategory): Boolean = category == HabitCategory.GOOD_DEED
    fun isPrimary(category: HabitCategory): Boolean = !isOptional(category)
    fun primarySummary(habits: List<HabitWithStatus>): DailyCompletionSummary {
        val primary = habits.filter { isPrimary(it.category) }
        return DailyCompletionSummary(primary.count { it.completed }, primary.size)
    }
}
