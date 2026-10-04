package com.example.habittracker.data.repository

import com.example.habittracker.data.RoutineDraft
import com.example.habittracker.data.RoutineProgressUpdate
import com.example.habittracker.data.RoutineRules
import com.example.habittracker.data.local.dao.HabitDao
import com.example.habittracker.data.local.dao.HabitWithStatus
import com.example.habittracker.data.local.entity.Habit
import com.example.habittracker.data.local.entity.HabitCategory
import com.example.habittracker.data.local.entity.HabitCompletion
import com.example.habittracker.data.local.entity.RoutineProgress
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.flow.Flow
import com.example.habittracker.data.local.entity.RoutineSchedule
import com.example.habittracker.reminders.ReminderScheduler
import com.example.habittracker.activities.ActivityServiceController
import com.example.habittracker.data.StartSessionResult
import com.example.habittracker.data.local.entity.ActivitySession
import com.example.habittracker.data.local.entity.ActivitySessionStatus
import com.example.habittracker.data.local.dao.SessionFinishResult
import com.example.habittracker.data.local.entity.RoutineType
import java.time.LocalDate

class HabitRepository(private val dao: HabitDao, private val reminderScheduler: ReminderScheduler? = null, private val activityService: ActivityServiceController? = null) {
    private val progressMutex = Mutex()
    private val sessionMutex = Mutex()
    fun habitsForDate(date: String): Flow<List<HabitWithStatus>> = dao.observeHabitsForDate(date)
    fun allHabits(): Flow<List<Habit>> = dao.observeAllHabits()
    fun completions(start: String, end: String): Flow<List<HabitCompletion>> = dao.observeCompletions(start, end)
    fun progress(start: String, end: String): Flow<List<RoutineProgress>> = dao.observeProgress(start, end)
    fun schedules(): Flow<List<RoutineSchedule>> = dao.observeSchedules()
    fun activeSession(): Flow<ActivitySession?> = dao.observeActiveSession()
    fun finishedSessions(start: String, end: String): Flow<List<ActivitySession>> = dao.observeFinishedSessions(start, end)

    suspend fun toggle(habitId: Long, date: String, completed: Boolean) {
        if (completed) dao.upsertCompletion(HabitCompletion(habitId = habitId, date = date))
        else dao.deleteCompletion(habitId, date)
    }

    suspend fun addHabit(draft: RoutineDraft): Result<Unit> = RoutineRules.normalized(draft).mapCatching { valid ->
        val habit = Habit(name = valid.name, category = HabitCategory.PERSONAL, sortOrder = 1000 + (System.currentTimeMillis() % 100000).toInt(), routineType = valid.type, target = valid.target, unit = valid.unit, iconKey = valid.iconKey, themeKey = valid.themeKey, quantityPerCount = valid.quantityPerCount, measurementUnit = valid.measurementUnit)
        val habitId = dao.insertHabit(habit)
        valid.schedule?.let { draft -> val schedule = RoutineSchedule(habitId = habitId, enabled = draft.enabled, timeMinutes = draft.timeMinutes, daysMask = draft.daysMask, reminderEnabled = draft.reminderEnabled, reminderOffsetMinutes = draft.reminderOffsetMinutes, reminderStyle = draft.reminderStyle, endTimeMinutes = draft.endTimeMinutes); val id = dao.upsertSchedule(schedule); reminderScheduler?.sync(habit.copy(id = habitId), schedule.copy(id = id)) }
    }

    suspend fun updateHabit(habit: Habit, draft: RoutineDraft = RoutineDraft(habit.name, habit.routineType, habit.target, habit.unit, habit.iconKey, habit.themeKey, habit.quantityPerCount, habit.measurementUnit), active: Boolean = habit.active): Result<Unit> {
        if (habit.isBuiltIn) return Result.failure(IllegalArgumentException("Built-in habits cannot be edited"))
        if (draft.type != habit.routineType) return Result.failure(IllegalArgumentException("Tracking type cannot be changed"))
        if (!active && dao.activeSession()?.habitId == habit.id) return Result.failure(IllegalStateException("Finish or discard the active session first"))
        if (dao.activeSession()?.habitId == habit.id && draft.target != habit.target) return Result.failure(IllegalStateException("Finish the active session before changing its target"))
        return RoutineRules.normalized(draft).mapCatching { valid ->
            val updated = habit.copy(name = valid.name, target = valid.target, unit = valid.unit, iconKey = valid.iconKey, themeKey = valid.themeKey, quantityPerCount = valid.quantityPerCount, measurementUnit = valid.measurementUnit, active = active)
            dao.updateHabit(updated)
            val existingSchedule = dao.schedule(habit.id)
            val schedule = valid.schedule?.let { s -> RoutineSchedule(existingSchedule?.id ?: 0, habit.id, s.enabled, s.timeMinutes, s.daysMask, s.reminderEnabled, s.reminderOffsetMinutes, s.reminderStyle, s.endTimeMinutes).also { dao.upsertSchedule(it) } } ?: existingSchedule
            reminderScheduler?.sync(updated, schedule)
        }
    }

    suspend fun addProgress(habit: Habit, date: String, delta: Int): RoutineProgressUpdate = progressMutex.withLock {
        require(habit.routineType != com.example.habittracker.data.local.entity.RoutineType.CHECK)
        val existing = dao.progress(habit.id, date)
        val previous = existing?.value ?: 0
        val current = (previous.toLong() + delta).coerceAtLeast(0).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
        dao.upsertProgress(RoutineProgress(existing?.id ?: 0, habit.id, date, current))
        RoutineProgressUpdate(previous, current, !RoutineRules.isComplete(habit.routineType, previous, habit.target) && RoutineRules.isComplete(habit.routineType, current, habit.target))
    }

    suspend fun rescheduleAll() { dao.schedules().forEach { schedule -> dao.habit(schedule.habitId)?.let { reminderScheduler?.sync(it, schedule) } } }
    suspend fun reschedule(habitId: Long) { reminderScheduler?.sync(dao.habit(habitId) ?: return, dao.schedule(habitId)) }
    suspend fun reminderData(habitId: Long) = dao.habit(habitId) to dao.schedule(habitId)

    suspend fun startSession(habitId: Long, businessDate: LocalDate, now: Long = System.currentTimeMillis()): StartSessionResult = sessionMutex.withLock {
        dao.activeSession()?.let { return@withLock StartSessionResult.AlreadyActive(it) }
        val habit = dao.habit(habitId) ?: return@withLock StartSessionResult.Rejected("Routine not found")
        if (!habit.active) return@withLock StartSessionResult.Rejected("Archived routines cannot be started")
        if (habit.routineType != RoutineType.DURATION) return@withLock StartSessionResult.Rejected("Only duration routines can be timed")
        val id = try { dao.insertSession(ActivitySession(habitId = habitId, businessDate = businessDate.toString(), startedAt = now, resumedAt = now, status = ActivitySessionStatus.RUNNING)) } catch (_: android.database.sqlite.SQLiteConstraintException) { return@withLock StartSessionResult.AlreadyActive(dao.activeSession() ?: return@withLock StartSessionResult.Rejected("Another activity is active")) }
        val session = dao.session(id)!!; activityService?.refresh(); StartSessionResult.Started(session)
    }
    suspend fun pauseSession(now: Long = System.currentTimeMillis()) = sessionMutex.withLock { dao.pauseActiveSession(now).also { activityService?.refresh() } }
    suspend fun resumeSession(now: Long = System.currentTimeMillis()) = sessionMutex.withLock { dao.resumeActiveSession(now).also { activityService?.refresh() } }
    suspend fun finishSession(allowShort: Boolean = false, now: Long = System.currentTimeMillis()): SessionFinishResult? = sessionMutex.withLock { dao.finishActiveSession(now, allowShort).also { if (it?.needsShortConfirmation != true) activityService?.stop() } }
    suspend fun discardSession(now: Long = System.currentTimeMillis()) = sessionMutex.withLock { dao.discardActiveSession(now).also { if (it) activityService?.stop() } }
    suspend fun activeSessionSnapshot() = dao.activeSession()
    suspend fun recoverSessionAfterReboot(now: Long = System.currentTimeMillis()) { dao.pauseActiveSession(now); activityService?.stop() }
}
