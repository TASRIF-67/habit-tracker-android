package com.example.habittracker.data.local

import androidx.room.TypeConverter
import com.example.habittracker.data.local.entity.HabitCategory

class HabitConverters {
    @TypeConverter fun fromCategory(value: HabitCategory) = value.name
    @TypeConverter fun toCategory(value: String) = HabitCategory.valueOf(value)
}
