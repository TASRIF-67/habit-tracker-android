package com.example.habittracker.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Transaction
import com.example.habittracker.data.local.entity.Habit
import com.example.habittracker.data.local.entity.HabitCompletion
import com.example.habittracker.data.local.entity.RoutineProgress
import com.example.habittracker.data.local.entity.RoutineType
import com.example.habittracker.data.local.entity.RoutineSchedule
import com.example.habittracker.data.local.entity.ActivitySession
import com.example.habittracker.data.local.entity.ActivitySessionStatus
import com.example.habittracker.data.local.entity.elapsedMillis
import kotlinx.coroutines.flow.Flow

data class HabitWithStatus(
    val id: Long,
    val name: String,
    val category: com.example.habittracker.data.local.entity.HabitCategory,
    val active: Boolean,
    val createdAt: Long,
    val sortOrder: Int,
    val isBuiltIn: Boolean,
    val routineType: RoutineType,
    val target: Int?,
    val unit: String?,
    val value: Int,
    val completed: Boolean,
    val iconKey: String = "CHECK",
    val themeKey: String = "FOREST",
    val quantityPerCount: Double? = null,
    val measurementUnit: String? = null,
)

data class CompletionCount(val habitId: Long, val count: Int)
data class SessionFinishResult(val sessionId: Long, val loggedMinutes: Int, val alreadyFinished: Boolean = false, val needsShortConfirmation: Boolean = false)

@Dao
interface HabitDao {
    @Query("SELECT * FROM habits ORDER BY sortOrder, id")
    fun observeAllHabits(): Flow<List<Habit>>

    @Query("SELECT COUNT(*) FROM habits")
    suspend fun habitCount(): Int

    @Insert suspend fun insertHabits(habits: List<Habit>)
    @Insert suspend fun insertHabit(habit: Habit): Long
    @Update suspend fun updateHabit(habit: Habit)
    @Query("SELECT * FROM habits WHERE id = :id LIMIT 1") suspend fun habit(id: Long): Habit?
    @Query("SELECT * FROM routine_schedules ORDER BY habitId") fun observeSchedules(): Flow<List<RoutineSchedule>>
    @Query("SELECT * FROM routine_schedules ORDER BY habitId") suspend fun schedules(): List<RoutineSchedule>
    @Query("SELECT * FROM routine_schedules WHERE habitId = :habitId LIMIT 1") suspend fun schedule(habitId: Long): RoutineSchedule?
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsertSchedule(schedule: RoutineSchedule): Long
    @Query("DELETE FROM routine_schedules WHERE habitId = :habitId") suspend fun deleteSchedule(habitId: Long)
    @Query("SELECT * FROM activity_sessions WHERE activeSlot = 1 LIMIT 1") fun observeActiveSession(): Flow<ActivitySession?>
    @Query("SELECT * FROM activity_sessions WHERE businessDate BETWEEN :start AND :end AND status = 'FINISHED' ORDER BY businessDate DESC, startedAt DESC")
    fun observeFinishedSessions(start: String, end: String): Flow<List<ActivitySession>>
    @Query("SELECT * FROM activity_sessions WHERE activeSlot = 1 LIMIT 1") suspend fun activeSession(): ActivitySession?
    @Query("SELECT * FROM activity_sessions WHERE id = :id LIMIT 1") suspend fun session(id: Long): ActivitySession?
    @Insert suspend fun insertSession(session: ActivitySession): Long
    @Update suspend fun updateSession(session: ActivitySession)

    @Transaction
    suspend fun pauseActiveSession(now: Long): ActivitySession? {
        val current = activeSession() ?: return null
        if (current.status != ActivitySessionStatus.RUNNING) return current
        val paused = current.copy(accumulatedActiveMillis = current.elapsedMillis(now), resumedAt = null, status = ActivitySessionStatus.PAUSED)
        updateSession(paused); return paused
    }

    @Transaction
    suspend fun resumeActiveSession(now: Long): ActivitySession? {
        val current = activeSession() ?: return null
        if (current.status != ActivitySessionStatus.PAUSED) return current
        val resumed = current.copy(resumedAt = now, status = ActivitySessionStatus.RUNNING)
        updateSession(resumed); return resumed
    }

    @Transaction
    suspend fun finishActiveSession(now: Long, allowShort: Boolean): SessionFinishResult? {
        val current = activeSession() ?: return null
        val elapsed = current.elapsedMillis(now)
        val minutes = (elapsed / 60_000L).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
        if (minutes == 0 && !allowShort) return SessionFinishResult(current.id, 0, needsShortConfirmation = true)
        val existing = progress(current.habitId, current.businessDate)
        if (minutes > 0) upsertProgress(RoutineProgress(existing?.id ?: 0, current.habitId, current.businessDate, ((existing?.value ?: 0).toLong() + minutes).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()))
        updateSession(current.copy(accumulatedActiveMillis = elapsed, resumedAt = null, status = ActivitySessionStatus.FINISHED, finishedAt = now, activeSlot = null))
        return SessionFinishResult(current.id, minutes)
    }

    @Transaction
    suspend fun discardActiveSession(now: Long): Boolean {
        val current = activeSession() ?: return false
        updateSession(current.copy(accumulatedActiveMillis = current.elapsedMillis(now), resumedAt = null, status = ActivitySessionStatus.DISCARDED, finishedAt = now, activeSlot = null)); return true
    }

    @Query("""
        SELECT h.*, COALESCE(rp.value, 0) AS value,
            CASE WHEN h.routineType = 'CHECK' THEN CASE WHEN c.completed = 1 THEN 1 ELSE 0 END
                 ELSE CASE WHEN COALESCE(rp.value, 0) >= COALESCE(h.target, 1) THEN 1 ELSE 0 END END AS completed
        FROM habits h
        LEFT JOIN habit_completions c ON h.id = c.habitId AND c.date = :date
        LEFT JOIN routine_progress rp ON h.id = rp.habitId AND rp.date = :date
        WHERE h.active = 1 OR c.id IS NOT NULL OR rp.id IS NOT NULL
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

    @Query("SELECT * FROM routine_progress WHERE habitId = :habitId AND date = :date LIMIT 1")
    suspend fun progress(habitId: Long, date: String): RoutineProgress?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProgress(progress: RoutineProgress)

    @Query("SELECT * FROM routine_progress WHERE date BETWEEN :start AND :end")
    fun observeProgress(start: String, end: String): Flow<List<RoutineProgress>>
}
