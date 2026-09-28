package com.example.habittracker.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class HabitCategory { SALAT, GOOD_DEED, PERSONAL }

@Entity(tableName = "habits")
data class Habit(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val category: HabitCategory,
    val active: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val sortOrder: Int = 0,
    val isBuiltIn: Boolean = false,
)
