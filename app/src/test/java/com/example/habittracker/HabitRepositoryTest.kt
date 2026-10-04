package com.example.habittracker

import com.example.habittracker.data.local.dao.HabitDao
import com.example.habittracker.data.local.dao.HabitWithStatus
import com.example.habittracker.data.local.entity.Habit
import com.example.habittracker.data.local.entity.HabitCompletion
import com.example.habittracker.data.local.entity.RoutineProgress
import com.example.habittracker.data.local.entity.RoutineSchedule
import com.example.habittracker.data.local.entity.ActivitySession
import com.example.habittracker.data.local.entity.ActivitySessionStatus
import com.example.habittracker.data.repository.HabitRepository
import com.example.habittracker.data.local.entity.HabitCategory
import com.example.habittracker.data.local.entity.RoutineType
import com.example.habittracker.data.local.entity.ReminderStyle
import com.example.habittracker.data.RoutineDraft
import com.example.habittracker.data.ScheduleDraft
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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

    @Test
    fun durationAdditionsAccumulateAndCountNeverBecomesNegative() = runBlocking {
        val dao = FakeHabitDao()
        val repository = HabitRepository(dao)
        val duration = Habit(8, "Walk", HabitCategory.PERSONAL, routineType = RoutineType.DURATION, target = 30, unit = "min")
        repository.addProgress(duration, "2026-10-03", 10)
        repository.addProgress(duration, "2026-10-03", 5)
        val result = repository.addProgress(duration, "2026-10-03", 12)
        assertTrue(result.current == 27 && dao.progress(8, "2026-10-03")?.value == 27)

        val count = Habit(9, "Water", HabitCategory.PERSONAL, routineType = RoutineType.COUNT, target = 8, unit = "glasses")
        repository.addProgress(count, "2026-10-03", -1)
        assertEquals(0, dao.progress(9, "2026-10-03")?.value)
        assertEquals(2, dao.progressRows.size)
    }

    @Test
    fun editingTargetDoesNotAlterStoredDailyValue() = runBlocking {
        val dao = FakeHabitDao()
        val repository = HabitRepository(dao)
        val habit = Habit(12, "Study", HabitCategory.PERSONAL, routineType = RoutineType.DURATION, target = 30, unit = "min")
        dao.habits += habit
        repository.addProgress(habit, "2026-10-03", 42)
        repository.updateHabit(habit, RoutineDraft("Study", RoutineType.DURATION, 60, "min")).getOrThrow()
        assertEquals(42, dao.progress(12, "2026-10-03")?.value)
        assertEquals(60, dao.habits.single().target)
    }

    @Test fun schedulePersistsSeparatelyWhenRoutineIsCreated() = runBlocking {
        val dao = FakeHabitDao(); val repository = HabitRepository(dao)
        repository.addHabit(RoutineDraft("Walk", RoutineType.DURATION, 30, schedule = ScheduleDraft(true, 17 * 60 + 30, 127, true, 10, ReminderStyle.ALARM))).getOrThrow()
        val schedule = dao.schedule(1)
        assertEquals(17 * 60 + 30, schedule?.timeMinutes)
        assertEquals(10, schedule?.reminderOffsetMinutes)
        assertEquals(ReminderStyle.ALARM, schedule?.reminderStyle)
    }

    @Test fun timedSessionPausesResumesAndAccumulatesProgressOnce() = runBlocking {
        val dao = FakeHabitDao(); val repository = HabitRepository(dao)
        dao.habits += Habit(40, "Walking", HabitCategory.PERSONAL, routineType = RoutineType.DURATION, target = 30, unit = "min")
        assertTrue(repository.startSession(40, java.time.LocalDate.of(2026, 10, 4), 1_000) is com.example.habittracker.data.StartSessionResult.Started)
        repository.pauseSession(61_000)
        repository.pauseSession(90_000)
        assertEquals(60_000L, dao.activeSession()?.accumulatedActiveMillis)
        repository.resumeSession(121_000); repository.resumeSession(150_000)
        val result = repository.finishSession(now = 181_000)
        assertEquals(2, result?.loggedMinutes)
        assertEquals(2, dao.progress(40, "2026-10-04")?.value)
        assertNull(repository.finishSession(now = 200_000))
        assertEquals(2, dao.progress(40, "2026-10-04")?.value)
    }

    @Test fun sessionRulesRejectInvalidStartsAndProtectActiveRoutine() = runBlocking {
        val dao = FakeHabitDao(); val repository = HabitRepository(dao)
        val walking = Habit(50, "Walking", HabitCategory.PERSONAL, routineType = RoutineType.DURATION, target = 30, unit = "min")
        dao.habits += walking
        dao.habits += Habit(51, "Water", HabitCategory.PERSONAL, routineType = RoutineType.COUNT, target = 4, unit = "bottles")
        dao.habits += Habit(52, "Archived", HabitCategory.PERSONAL, active = false, routineType = RoutineType.DURATION, target = 20, unit = "min")
        assertTrue(repository.startSession(51, java.time.LocalDate.now(), 1_000) is com.example.habittracker.data.StartSessionResult.Rejected)
        assertTrue(repository.startSession(52, java.time.LocalDate.now(), 1_000) is com.example.habittracker.data.StartSessionResult.Rejected)
        repository.startSession(50, java.time.LocalDate.now(), 1_000)
        assertTrue(repository.startSession(51, java.time.LocalDate.now(), 2_000) is com.example.habittracker.data.StartSessionResult.AlreadyActive)
        assertTrue(repository.updateHabit(walking, active = false).isFailure)
    }

    @Test fun shortAndCrossMidnightSessionsUseStartingBusinessDate() = runBlocking {
        val dao = FakeHabitDao(); val repository = HabitRepository(dao)
        dao.habits += Habit(60, "Study", HabitCategory.PERSONAL, routineType = RoutineType.DURATION, target = 30, unit = "min")
        val startDate = java.time.LocalDate.of(2026, 10, 4)
        repository.startSession(60, startDate, 1_000)
        assertTrue(repository.finishSession(now = 50_000)?.needsShortConfirmation == true)
        repository.finishSession(allowShort = true, now = 50_000)
        assertNull(dao.progress(60, startDate.toString()))
        repository.startSession(60, startDate, 100_000)
        repository.finishSession(now = 100_000 + 30 * 60_000)
        assertEquals(30, dao.progress(60, startDate.toString())?.value)
    }

    private class FakeHabitDao : HabitDao {
        val habits = mutableListOf<Habit>()
        private val completions = mutableListOf<HabitCompletion>()
        override fun observeAllHabits(): Flow<List<Habit>> = flowOf(emptyList())
        override suspend fun habitCount() = 0
        override suspend fun insertHabits(habits: List<Habit>) { this.habits += habits }
        override suspend fun insertHabit(habit: Habit): Long { habits += habit; return habit.id.takeIf { it != 0L } ?: habits.size.toLong() }
        override suspend fun updateHabit(habit: Habit) { habits.replaceAll { if (it.id == habit.id) habit else it } }
        override suspend fun habit(id: Long) = habits.singleOrNull { it.id == id }
        private val scheduleRows = mutableListOf<RoutineSchedule>()
        private val sessionRows = mutableListOf<ActivitySession>()
        override fun observeSchedules(): Flow<List<RoutineSchedule>> = flowOf(scheduleRows)
        override suspend fun schedules() = scheduleRows.toList()
        override suspend fun schedule(habitId: Long) = scheduleRows.singleOrNull { it.habitId == habitId }
        override suspend fun upsertSchedule(schedule: RoutineSchedule): Long { scheduleRows.removeAll { it.habitId == schedule.habitId }; val id = schedule.id.takeIf { it != 0L } ?: (scheduleRows.size + 1L); scheduleRows += schedule.copy(id = id); return id }
        override suspend fun deleteSchedule(habitId: Long) { scheduleRows.removeAll { it.habitId == habitId } }
        override fun observeActiveSession(): Flow<ActivitySession?> = flowOf(sessionRows.singleOrNull { it.activeSlot == 1 })
        override fun observeFinishedSessions(start: String, end: String): Flow<List<ActivitySession>> = flowOf(sessionRows.filter { it.status == ActivitySessionStatus.FINISHED && it.businessDate in start..end })
        override suspend fun activeSession() = sessionRows.singleOrNull { it.activeSlot == 1 }
        override suspend fun session(id: Long) = sessionRows.singleOrNull { it.id == id }
        override suspend fun insertSession(session: ActivitySession): Long { check(activeSession() == null); val id = session.id.takeIf { it != 0L } ?: sessionRows.size + 1L; sessionRows += session.copy(id = id); return id }
        override suspend fun updateSession(session: ActivitySession) { sessionRows.replaceAll { if (it.id == session.id) session else it } }
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
        val progressRows = mutableListOf<RoutineProgress>()
        override suspend fun progress(habitId: Long, date: String) = progressRows.singleOrNull { it.habitId == habitId && it.date == date }
        override suspend fun upsertProgress(progress: RoutineProgress) { progressRows.removeAll { it.habitId == progress.habitId && it.date == progress.date }; progressRows += progress }
        override fun observeProgress(start: String, end: String): Flow<List<RoutineProgress>> = flowOf(progressRows)
    }
}
