package com.example.habittracker

import com.example.habittracker.data.local.dao.HabitDao
import com.example.habittracker.data.local.dao.HabitWithStatus
import com.example.habittracker.data.local.entity.Habit
import com.example.habittracker.data.local.entity.HabitCompletion
import com.example.habittracker.data.repository.HabitRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class HabitRepositoryTest {
    @Test
    fun genericHabitCanStillBeCompletedAndUncompleted() = runBlocking {
        val dao = FakeHabitDao()
        val repository = HabitRepository(dao)
        repository.toggle(7, "2026-10-03", completed = true)
        assertNotNull(dao.completion(7, "2026-10-03"))
        repository.toggle(7, "2026-10-03", completed = false)
        assertNull(dao.completion(7, "2026-10-03"))
    }

    private class FakeHabitDao : HabitDao {
        private val completions = mutableListOf<HabitCompletion>()
        override fun observeAllHabits(): Flow<List<Habit>> = flowOf(emptyList())
        override suspend fun habitCount() = 0
        override suspend fun insertHabits(habits: List<Habit>) = Unit
        override suspend fun insertHabit(habit: Habit) = Unit
        override suspend fun updateHabit(habit: Habit) = Unit
        override fun observeHabitsForDate(date: String): Flow<List<HabitWithStatus>> = flowOf(emptyList())
        override fun observeCompletions(start: String, end: String): Flow<List<HabitCompletion>> = flowOf(completions)
        override suspend fun completion(habitId: Long, date: String) = completions.singleOrNull { it.habitId == habitId && it.date == date }
        override suspend fun upsertCompletion(completion: HabitCompletion) {
            completions.removeAll { it.habitId == completion.habitId && it.date == completion.date }
            completions += completion
        }
        override suspend fun deleteCompletion(habitId: Long, date: String) {
            completions.removeAll { it.habitId == habitId && it.date == date }
        }
    }
}
