package com.example.habittracker.data

import com.example.habittracker.data.local.entity.SleepPlan
import com.example.habittracker.data.local.entity.SleepSession
import java.time.*
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import kotlin.math.abs

object SleepRules {
    val windDownOffsets = setOf(15, 30, 45, 60)
    const val unusuallyLongMillis = 24L * 60 * 60 * 1000

    fun valid(plan: SleepPlan) = plan.bedtimeMinutes in 0..1439 && plan.wakeTimeMinutes in 0..1439 &&
        plan.daysMask in 1..EVERY_DAY && (!plan.windDownEnabled || plan.windDownOffsetMinutes in windDownOffsets)

    fun plannedMinutes(plan: SleepPlan) = plannedMinutes(plan.bedtimeMinutes, plan.wakeTimeMinutes)
    fun plannedMinutes(bedtimeMinutes: Int, wakeMinutes: Int): Int =
        ((wakeMinutes - bedtimeMinutes + 24 * 60) % (24 * 60)).let { if (it == 0) 24 * 60 else it }

    /** Selected weekdays are bedtime/start weekdays. */
    fun nextBedtime(plan: SleepPlan, after: ZonedDateTime): ZonedDateTime? {
        if (!plan.enabled || !valid(plan)) return null
        val time = LocalTime.of(plan.bedtimeMinutes / 60, plan.bedtimeMinutes % 60)
        for (offset in 0..7) {
            val date = after.toLocalDate().plusDays(offset.toLong())
            if (!ScheduleRules.includes(plan.daysMask, date.dayOfWeek)) continue
            val candidate = ZonedDateTime.of(date, time, after.zone)
            if (candidate.isAfter(after)) return candidate
        }
        return null
    }

    fun nextWindDown(plan: SleepPlan, after: ZonedDateTime): ZonedDateTime? {
        if (!plan.enabled || !plan.windDownEnabled || !valid(plan)) return null
        val bedtime = nextPlanEventAfter(plan, after, plan.windDownOffsetMinutes) ?: return null
        return bedtime.minusMinutes(plan.windDownOffsetMinutes.toLong())
    }

    private fun nextPlanEventAfter(plan: SleepPlan, after: ZonedDateTime, leadMinutes: Int): ZonedDateTime? {
        val searchFrom = after.plusMinutes(leadMinutes.toLong())
        return nextBedtime(plan, searchFrom.minusNanos(1))
    }

    fun nextWake(plan: SleepPlan, after: ZonedDateTime): ZonedDateTime? = nextBedtime(plan, after)?.plusMinutes(plannedMinutes(plan).toLong())
    fun actualDurationMillis(start: Long, end: Long): Long? = (end - start).takeIf { it >= 0 }
    fun isUnusuallyLong(start: Long, end: Long) = end - start > unusuallyLongMillis
    fun bedtimeDeviationMinutes(session: SleepSession, zone: ZoneId = ZoneId.systemDefault()): Int {
        val actual = Instant.ofEpochMilli(session.wentToBedAt).atZone(zone).toLocalTime().let { it.hour * 60 + it.minute }
        var difference = actual - session.plannedBedtimeMinutes
        if (difference > 720) difference -= 1440
        if (difference < -720) difference += 1440
        return difference
    }
    fun formatDuration(millis: Long): String {
        val totalMinutes = millis.coerceAtLeast(0) / 60_000
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60
        return when { hours == 0L -> "$minutes min"; minutes == 0L -> "${hours}h"; else -> "${hours}h ${minutes}m" }
    }
    fun formatMinutes(minutes: Int): String = LocalTime.of(minutes / 60, minutes % 60).format(DateTimeFormatter.ofPattern("h:mm a"))
    fun formatInstant(timestamp: Long, zone: ZoneId = ZoneId.systemDefault()): String = Instant.ofEpochMilli(timestamp).atZone(zone).format(DateTimeFormatter.ofPattern("h:mm a"))
}

sealed interface StartSleepResult {
    data class Started(val session: SleepSession) : StartSleepResult
    data class AlreadySleeping(val session: SleepSession) : StartSleepResult
    data class Rejected(val reason: String) : StartSleepResult
}

data class FinishSleepResult(val session: SleepSession? = null, val needsLongConfirmation: Boolean = false, val alreadyFinished: Boolean = false)
