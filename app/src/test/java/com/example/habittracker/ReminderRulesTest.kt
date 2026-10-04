package com.example.habittracker

import com.example.habittracker.data.*
import com.example.habittracker.data.local.entity.ReminderStyle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class ReminderRulesTest {
    @Test fun prominentAndAlarmDispatchAreExplicit() {
        assertEquals(ReminderDeliveryMode.PROMINENT, ReminderRules.deliveryMode(ReminderStyle.PROMINENT, true))
        assertEquals(ReminderDeliveryMode.ALARM_EXACT, ReminderRules.deliveryMode(ReminderStyle.ALARM, true))
        assertEquals(ReminderDeliveryMode.ALARM_FALLBACK, ReminderRules.deliveryMode(ReminderStyle.ALARM, false))
    }

    @Test fun snoozeUsesTemporarySupportedOffset() {
        assertEquals(301_000L, ReminderRules.snoozeAt(1_000L, 5))
        assertEquals(601_000L, ReminderRules.snoozeAt(1_000L, 10))
        assertEquals(901_000L, ReminderRules.snoozeAt(1_000L, 15))
        assertThrows(IllegalArgumentException::class.java) { ReminderRules.snoozeAt(1_000L, 30) }
    }
}
