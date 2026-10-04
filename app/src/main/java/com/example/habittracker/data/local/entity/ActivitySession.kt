package com.example.habittracker.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class ActivitySessionStatus { RUNNING, PAUSED, FINISHED, DISCARDED }

@Entity(
    tableName = "activity_sessions",
    foreignKeys = [ForeignKey(entity = Habit::class, parentColumns = ["id"], childColumns = ["habitId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("habitId"), Index("status"), Index(value = ["activeSlot"], unique = true)],
)
data class ActivitySession(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val habitId: Long,
    val businessDate: String,
    val startedAt: Long,
    val accumulatedActiveMillis: Long = 0,
    val resumedAt: Long?,
    val status: ActivitySessionStatus,
    val finishedAt: Long? = null,
    val activeSlot: Int? = 1,
)

fun ActivitySession.elapsedMillis(now: Long): Long = accumulatedActiveMillis + if (status == ActivitySessionStatus.RUNNING && resumedAt != null) (now - resumedAt).coerceAtLeast(0) else 0
