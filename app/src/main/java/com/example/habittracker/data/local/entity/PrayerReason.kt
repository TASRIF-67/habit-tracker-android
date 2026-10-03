package com.example.habittracker.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "prayer_reasons", indices = [Index(value = ["name"], unique = true)])
data class PrayerReason(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val isBuiltIn: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
)
