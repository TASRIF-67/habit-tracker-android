package com.example.habittracker.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sleep_plan")
data class SleepPlan(
    @PrimaryKey val id: Int = SINGLE_PLAN_ID,
    val enabled: Boolean = true,
    val bedtimeMinutes: Int = 23 * 60 + 30,
    val wakeTimeMinutes: Int = 7 * 60,
    val daysMask: Int = 0b1111111,
    val windDownEnabled: Boolean = true,
    val windDownOffsetMinutes: Int = 30,
    val bedtimeReminderEnabled: Boolean = false,
) {
    companion object { const val SINGLE_PLAN_ID = 1 }
}
