package com.example.habittracker.data.local.entity

import androidx.room.Entity
import androidx.room.ColumnInfo
import androidx.room.PrimaryKey

enum class HabitCategory { SALAT, GOOD_DEED, PERSONAL }
enum class RoutineType { CHECK, DURATION, COUNT }

@Entity(tableName = "habits")
data class Habit(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val category: HabitCategory,
    val active: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val sortOrder: Int = 0,
    val isBuiltIn: Boolean = false,
    val routineType: RoutineType = RoutineType.CHECK,
    val target: Int? = null,
    val unit: String? = null,
    @ColumnInfo(defaultValue = "'CHECK'") val iconKey: String = "CHECK",
    @ColumnInfo(defaultValue = "'FOREST'") val themeKey: String = "FOREST",
    val quantityPerCount: Double? = null,
    val measurementUnit: String? = null,
)
