package com.example.habittracker.data.local

import androidx.room.TypeConverter
import com.example.habittracker.data.local.entity.HabitCategory
import com.example.habittracker.data.local.entity.Prayer
import com.example.habittracker.data.local.entity.PrayerStatus
import com.example.habittracker.data.local.entity.RoutineType
import com.example.habittracker.data.local.entity.ActivitySessionStatus
import com.example.habittracker.data.local.entity.SleepSessionStatus
import com.example.habittracker.data.local.entity.ReminderStyle
import com.example.habittracker.data.local.entity.PrayerCalculationMethod
import com.example.habittracker.data.local.entity.AsrMethod

class HabitConverters {
    @TypeConverter fun fromCategory(value: HabitCategory) = value.name
    @TypeConverter fun toCategory(value: String) = HabitCategory.valueOf(value)
    @TypeConverter fun fromPrayer(value: Prayer) = value.name
    @TypeConverter fun toPrayer(value: String) = Prayer.valueOf(value)
    @TypeConverter fun fromPrayerStatus(value: PrayerStatus) = value.name
    @TypeConverter fun toPrayerStatus(value: String) = PrayerStatus.valueOf(value)
    @TypeConverter fun fromRoutineType(value: RoutineType) = value.name
    @TypeConverter fun toRoutineType(value: String) = RoutineType.valueOf(value)
    @TypeConverter fun fromActivityStatus(value: ActivitySessionStatus) = value.name
    @TypeConverter fun toActivityStatus(value: String) = ActivitySessionStatus.valueOf(value)
    @TypeConverter fun fromSleepStatus(value: SleepSessionStatus) = value.name
    @TypeConverter fun toSleepStatus(value: String) = SleepSessionStatus.valueOf(value)
    @TypeConverter fun fromReminderStyle(value: ReminderStyle) = value.name
    @TypeConverter fun toReminderStyle(value: String) = ReminderStyle.valueOf(value)
    @TypeConverter fun fromPrayerCalculationMethod(value: PrayerCalculationMethod) = value.name
    @TypeConverter fun toPrayerCalculationMethod(value: String) = PrayerCalculationMethod.valueOf(value)
    @TypeConverter fun fromAsrMethod(value: AsrMethod) = value.name
    @TypeConverter fun toAsrMethod(value: String) = AsrMethod.valueOf(value)
}
