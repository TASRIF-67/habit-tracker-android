package com.example.habittracker.data.local

import androidx.room.TypeConverter
import com.example.habittracker.data.local.entity.HabitCategory
import com.example.habittracker.data.local.entity.Prayer
import com.example.habittracker.data.local.entity.PrayerStatus

class HabitConverters {
    @TypeConverter fun fromCategory(value: HabitCategory) = value.name
    @TypeConverter fun toCategory(value: String) = HabitCategory.valueOf(value)
    @TypeConverter fun fromPrayer(value: Prayer) = value.name
    @TypeConverter fun toPrayer(value: String) = Prayer.valueOf(value)
    @TypeConverter fun fromPrayerStatus(value: PrayerStatus) = value.name
    @TypeConverter fun toPrayerStatus(value: String) = PrayerStatus.valueOf(value)
}
