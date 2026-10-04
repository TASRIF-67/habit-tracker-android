package com.example.habittracker.data.repository

import android.database.sqlite.SQLiteConstraintException
import com.example.habittracker.data.*
import com.example.habittracker.data.local.dao.SleepDao
import com.example.habittracker.data.local.entity.*
import com.example.habittracker.reminders.SleepReminderScheduler
import java.time.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class SleepRepository(
    private val dao: SleepDao,
    private val activeActivityLookup: ActiveActivityLookup = ActiveActivityLookup { null },
    private val scheduler: SleepReminderScheduler? = null,
) {
    private val mutex = Mutex()
    fun plan(): Flow<SleepPlan?> = dao.observePlan()
    fun activeSession(): Flow<SleepSession?> = dao.observeActiveSession()
    fun latestCompleted(): Flow<SleepSession?> = dao.observeLatestCompleted()
    fun sessionsForDate(date: String): Flow<List<SleepSession>> = dao.observeForDate(date)
    fun sessionsBetween(start: String, end: String): Flow<List<SleepSession>> = dao.observeBetween(start, end)
    suspend fun planSnapshot() = dao.plan()

    suspend fun savePlan(plan: SleepPlan): Result<Unit> = mutex.withLock {
        if (!SleepRules.valid(plan)) return@withLock Result.failure(IllegalArgumentException("Check the sleep schedule and selected days"))
        if (!plan.enabled && dao.activeSession() != null) return@withLock Result.failure(IllegalStateException("Finish the active sleep session before disabling the plan"))
        dao.savePlan(plan.copy(id = SleepPlan.SINGLE_PLAN_ID)); scheduler?.sync(plan); Result.success(Unit)
    }

    suspend fun startSleep(now: Long = System.currentTimeMillis(), zone: ZoneId = ZoneId.systemDefault()): StartSleepResult = mutex.withLock {
        dao.activeSession()?.let { return@withLock StartSleepResult.AlreadySleeping(it) }
        val activity = activeActivityLookup.active()
        if (activity?.status == ActivitySessionStatus.RUNNING) {
            val name = activity.name ?: "An activity"
            return@withLock StartSleepResult.Rejected("$name is currently active. Pause, finish, or discard it before going to bed.")
        }
        val plan = dao.plan()?.takeIf { it.enabled } ?: return@withLock StartSleepResult.Rejected("Set up and enable Sleep & bedtime first")
        val date = Instant.ofEpochMilli(now).atZone(zone).toLocalDate().toString()
        val session = SleepSession(sleepDate = date, wentToBedAt = now, plannedBedtimeMinutes = plan.bedtimeMinutes, plannedWakeTimeMinutes = plan.wakeTimeMinutes)
        val id = try { dao.insertSession(session) } catch (_: SQLiteConstraintException) { return@withLock StartSleepResult.AlreadySleeping(dao.activeSession() ?: session) }
        StartSleepResult.Started(session.copy(id = id))
    }

    suspend fun finishSleep(now: Long = System.currentTimeMillis(), allowLong: Boolean = false): FinishSleepResult = mutex.withLock {
        val current = dao.activeSession() ?: return@withLock FinishSleepResult(alreadyFinished = true)
        if (SleepRules.isUnusuallyLong(current.wentToBedAt, now) && !allowLong) return@withLock FinishSleepResult(current, needsLongConfirmation = true)
        requireNotNull(SleepRules.actualDurationMillis(current.wentToBedAt, now))
        val completed = current.copy(wokeUpAt = now, status = SleepSessionStatus.COMPLETED, activeSlot = null)
        dao.updateSession(completed)
        FinishSleepResult(completed)
    }

    suspend fun correctSession(id: Long, wentToBedAt: Long, wokeUpAt: Long): Result<Unit> = mutex.withLock {
        val session = dao.session(id) ?: return@withLock Result.failure(IllegalArgumentException("Sleep record not found"))
        if (session.status != SleepSessionStatus.COMPLETED) return@withLock Result.failure(IllegalStateException("Only completed sleep records can be corrected"))
        if (SleepRules.actualDurationMillis(wentToBedAt, wokeUpAt) == null) return@withLock Result.failure(IllegalArgumentException("Wake time must be after bedtime"))
        dao.updateSession(session.copy(sleepDate = Instant.ofEpochMilli(wentToBedAt).atZone(ZoneId.systemDefault()).toLocalDate().toString(), wentToBedAt = wentToBedAt, wokeUpAt = wokeUpAt))
        Result.success(Unit)
    }

    suspend fun rescheduleReminders() { scheduler?.sync(dao.plan()) }
}

data class ActiveActivity(val status: ActivitySessionStatus, val name: String?)
fun interface ActiveActivityLookup { suspend fun active(): ActiveActivity? }
