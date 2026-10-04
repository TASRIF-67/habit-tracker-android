package com.example.habittracker.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

enum class SleepSessionStatus { SLEEPING, COMPLETED }

@Entity(
    tableName = "sleep_sessions",
    indices = [Index("sleepDate"), Index("status"), Index(value = ["activeSlot"], unique = true)],
)
data class SleepSession(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sleepDate: String,
    val wentToBedAt: Long,
    val wokeUpAt: Long? = null,
    val status: SleepSessionStatus = SleepSessionStatus.SLEEPING,
    val activeSlot: Int? = 1,
    val plannedBedtimeMinutes: Int,
    val plannedWakeTimeMinutes: Int,
)

fun SleepSession.durationMillis(now: Long = System.currentTimeMillis()): Long =
    ((wokeUpAt ?: now) - wentToBedAt).coerceAtLeast(0)
