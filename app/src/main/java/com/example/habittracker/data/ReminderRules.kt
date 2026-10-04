package com.example.habittracker.data

import com.example.habittracker.data.local.entity.ReminderStyle

enum class ReminderDeliveryMode { PROMINENT, ALARM_EXACT, ALARM_FALLBACK }

object ReminderRules {
    fun deliveryMode(style: ReminderStyle, exactAlarmAvailable: Boolean) = when {
        style == ReminderStyle.PROMINENT -> ReminderDeliveryMode.PROMINENT
        exactAlarmAvailable -> ReminderDeliveryMode.ALARM_EXACT
        else -> ReminderDeliveryMode.ALARM_FALLBACK
    }
    fun snoozeAt(now: Long, minutes: Int): Long {
        require(minutes in setOf(5, 10, 15))
        return now + minutes * 60_000L
    }
}
