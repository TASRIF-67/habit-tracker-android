package com.example.habittracker.reminders

import com.example.habittracker.data.local.entity.Habit
import com.example.habittracker.data.local.entity.RoutineSchedule

interface ReminderScheduler {
    fun sync(habit: Habit, schedule: RoutineSchedule?)
    fun cancel(habitId: Long)
}
