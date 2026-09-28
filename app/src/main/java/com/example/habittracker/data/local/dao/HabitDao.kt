package com.example.habittracker.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.habittracker.data.local.entity.Habit
import com.example.habittracker.data.local.entity.HabitCompletion
import kotlinx.coroutines.flow.Flow

data class HabitWithStatus(
    val id: Long,
    val name: String,
    val category: com.example.habittracker.data.local.entity.HabitCategory,
    val active: Boolean,
    val createdAt: Long,
    val sortOrder: Int,
    val isBuiltIn: Boolean,
    val completed: Boolean,
)

data class CompletionCount(val habitId: Long, val count: Int)

@Dao
interface HabitDao {
    @Query("SELECT * FROM habits ORDER BY sortOrder, id")
    fun observeAllHabits(): Flow<List<Habit>>

    @Query("SELECT COUNT(*) FROM habits")
    suspend fun habitCount(): Int

    @Insert suspend fun insertHabits(habits: List<Habit>)
    @Insert suspend fun insertHabit(habit: Habit)
    @Update suspend fun updateHabit(habit: Habit)

    @Query("""
        SELECT h.*, CASE WHEN c.completed = 1 THEN 1 ELSE 0 END AS completed
        FROM habits h LEFT JOIN habit_completions c ON h.id = c.habitId AND c.date = :date
        WHERE h.active = 1 OR c.id IS NOT NULL
        ORDER BY h.sortOrder, h.id
    """)
    fun observeHabitsForDate(date: String): Flow<List<HabitWithStatus>>

    @Query("SELECT * FROM habit_completions WHERE date BETWEEN :start AND :end AND completed = 1")
    fun observeCompletions(start: String, end: String): Flow<List<HabitCompletion>>

    @Query("SELECT * FROM habit_completions WHERE habitId = :habitId AND date = :date LIMIT 1")
    suspend fun completion(habitId: Long, date: String): HabitCompletion?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCompletion(completion: HabitCompletion)

    @Query("DELETE FROM habit_completions WHERE habitId = :habitId AND date = :date")
    suspend fun deleteCompletion(habitId: Long, date: String)
}
