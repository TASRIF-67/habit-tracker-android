package com.example.habittracker.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class ReminderStyle { PROMINENT, ALARM }

@Entity(
    tableName = "routine_schedules",
    foreignKeys = [ForeignKey(entity = Habit::class, parentColumns = ["id"], childColumns = ["habitId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index(value = ["habitId"], unique = true)],
)
data class RoutineSchedule(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val habitId: Long,
    val enabled: Boolean,
    val timeMinutes: Int,
    val daysMask: Int,
    val reminderEnabled: Boolean,
    val reminderOffsetMinutes: Int,
    val reminderStyle: ReminderStyle = ReminderStyle.PROMINENT,
    val endTimeMinutes: Int? = null,
)
