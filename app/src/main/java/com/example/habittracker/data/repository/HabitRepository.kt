package com.example.habittracker.data.repository

import com.example.habittracker.data.local.dao.HabitDao
import com.example.habittracker.data.local.dao.HabitWithStatus
import com.example.habittracker.data.local.entity.Habit
import com.example.habittracker.data.local.entity.HabitCategory
import com.example.habittracker.data.local.entity.HabitCompletion
import kotlinx.coroutines.flow.Flow

class HabitRepository(private val dao: HabitDao) {
    fun habitsForDate(date: String): Flow<List<HabitWithStatus>> = dao.observeHabitsForDate(date)
    fun allHabits(): Flow<List<Habit>> = dao.observeAllHabits()
    fun completions(start: String, end: String): Flow<List<HabitCompletion>> = dao.observeCompletions(start, end)

    suspend fun toggle(habitId: Long, date: String, completed: Boolean) {
        if (completed) dao.upsertCompletion(HabitCompletion(habitId = habitId, date = date))
        else dao.deleteCompletion(habitId, date)
    }

    suspend fun addHabit(name: String) = dao.insertHabit(
        Habit(name = name.trim(), category = HabitCategory.PERSONAL, sortOrder = 1000 + (System.currentTimeMillis() % 100000).toInt())
    )

    suspend fun updateHabit(habit: Habit, name: String = habit.name, active: Boolean = habit.active) {
        if (!habit.isBuiltIn) dao.updateHabit(habit.copy(name = name.trim(), active = active))
    }
}
