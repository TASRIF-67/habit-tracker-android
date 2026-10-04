package com.example.habittracker

import com.example.habittracker.data.*
import com.example.habittracker.data.local.dao.SleepDao
import com.example.habittracker.data.local.entity.*
import com.example.habittracker.data.repository.*
import com.example.habittracker.reminders.SleepReminderScheduler
import java.time.ZoneId
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class SleepRepositoryTest {
    @Test fun `fresh user has no plan and can save and read one`() = runBlocking {
        val dao = FakeSleepDao(); val repository = SleepRepository(dao)
        assertNull(repository.planSnapshot())
        val plan = SleepPlan(bedtimeMinutes = 1320)
        assertTrue(repository.savePlan(plan).isSuccess)
        assertEquals(1320, repository.planSnapshot()?.bedtimeMinutes)
    }

    @Test fun `duplicate start creates only one active session and snapshots plan`() = runBlocking {
        val dao = FakeSleepDao(); val repository = SleepRepository(dao); repository.savePlan(SleepPlan(bedtimeMinutes = 1380, wakeTimeMinutes = 420))
        val first = repository.startSleep(1_000, ZoneId.of("UTC"))
        val second = repository.startSleep(2_000, ZoneId.of("UTC"))
        assertTrue(first is StartSleepResult.Started)
        assertTrue(second is StartSleepResult.AlreadySleeping)
        assertEquals(1, dao.sessions.size)
        assertEquals(1380, dao.sessions.single().plannedBedtimeMinutes)
    }

    @Test fun `running activity blocks sleep but paused activity does not`() = runBlocking {
        val runningDao = FakeSleepDao().also { it.savePlan(SleepPlan()) }
        val running = SleepRepository(runningDao, ActiveActivityLookup { ActiveActivity(ActivitySessionStatus.RUNNING, "Walking") })
        assertTrue(running.startSleep() is StartSleepResult.Rejected)
        val pausedDao = FakeSleepDao().also { it.savePlan(SleepPlan()) }
        val paused = SleepRepository(pausedDao, ActiveActivityLookup { ActiveActivity(ActivitySessionStatus.PAUSED, "Walking") })
        assertTrue(paused.startSleep() is StartSleepResult.Started)
    }

    @Test fun `finish is idempotent and records actual duration`() = runBlocking {
        val dao = FakeSleepDao(); val repository = SleepRepository(dao); repository.savePlan(SleepPlan()); repository.startSleep(1_000, ZoneId.of("UTC"))
        val finished = repository.finishSleep(3_601_000).session!!
        assertEquals(3_600_000L, finished.durationMillis())
        assertEquals(SleepSessionStatus.COMPLETED, finished.status)
        assertTrue(repository.finishSleep(4_000_000).alreadyFinished)
        assertEquals(1, dao.sessions.size)
    }

    @Test fun `unusually long session requires explicit confirmation`() = runBlocking {
        val dao = FakeSleepDao(); val repository = SleepRepository(dao); repository.savePlan(SleepPlan()); repository.startSleep(1_000, ZoneId.of("UTC"))
        val end = 1_000 + SleepRules.unusuallyLongMillis + 1
        assertTrue(repository.finishSleep(end).needsLongConfirmation)
        assertNotNull(dao.activeSession())
        assertNotNull(repository.finishSleep(end, allowLong = true).session)
    }

    @Test fun `completed session correction validates interval and retains plan snapshot`() = runBlocking {
        val dao = FakeSleepDao(); val repository = SleepRepository(dao); repository.savePlan(SleepPlan(bedtimeMinutes = 1320, wakeTimeMinutes = 360)); val started = repository.startSleep(10_000, ZoneId.of("UTC")) as StartSleepResult.Started; repository.finishSleep(20_000)
        assertTrue(repository.correctSession(started.session.id, 30_000, 29_000).isFailure)
        assertTrue(repository.correctSession(started.session.id, 30_000, 50_000).isSuccess)
        val corrected = dao.session(started.session.id)!!
        assertEquals(1320, corrected.plannedBedtimeMinutes)
        repository.savePlan(SleepPlan(bedtimeMinutes = 60, wakeTimeMinutes = 600))
        assertEquals(1320, dao.session(started.session.id)?.plannedBedtimeMinutes)
    }

    @Test fun `disabled plan cancels reminders and keeps history`() = runBlocking {
        val dao = FakeSleepDao(); val scheduler = FakeSleepScheduler(); val repository = SleepRepository(dao, scheduler = scheduler)
        repository.savePlan(SleepPlan()); repository.startSleep(1_000, ZoneId.of("UTC")); repository.finishSleep(2_000)
        repository.savePlan(SleepPlan(enabled = false))
        assertEquals(1, dao.sessions.size)
        assertFalse(scheduler.lastPlan?.enabled ?: true)
    }
}

private class FakeSleepScheduler : SleepReminderScheduler {
    var lastPlan: SleepPlan? = null
    override fun sync(plan: SleepPlan?) { lastPlan = plan }
    override fun cancel() { lastPlan = null }
}

private class FakeSleepDao : SleepDao {
    private val planFlow = MutableStateFlow<SleepPlan?>(null)
    private val activeFlow = MutableStateFlow<SleepSession?>(null)
    private val latestFlow = MutableStateFlow<SleepSession?>(null)
    val sessions = mutableListOf<SleepSession>()
    private var nextId = 1L
    override fun observePlan(): Flow<SleepPlan?> = planFlow
    override suspend fun plan() = planFlow.value
    override suspend fun savePlan(plan: SleepPlan) { planFlow.value = plan }
    override fun observeActiveSession(): Flow<SleepSession?> = activeFlow
    override suspend fun activeSession() = activeFlow.value
    override suspend fun session(id: Long) = sessions.firstOrNull { it.id == id }
    override suspend fun insertSession(session: SleepSession): Long {
        check(activeFlow.value == null)
        val inserted = session.copy(id = nextId++)
        sessions += inserted; activeFlow.value = inserted; return inserted.id
    }
    override suspend fun updateSession(session: SleepSession) {
        val index = sessions.indexOfFirst { it.id == session.id }; sessions[index] = session
        activeFlow.value = session.takeIf { it.activeSlot == 1 }
        if (session.status == SleepSessionStatus.COMPLETED) latestFlow.value = session
    }
    override fun observeForDate(date: String): Flow<List<SleepSession>> = flowOf(sessions.filter { it.sleepDate == date && it.status == SleepSessionStatus.COMPLETED })
    override fun observeBetween(start: String, end: String): Flow<List<SleepSession>> = flowOf(sessions.filter { it.sleepDate in start..end && it.status == SleepSessionStatus.COMPLETED })
    override fun observeLatestCompleted(): Flow<SleepSession?> = latestFlow
}
