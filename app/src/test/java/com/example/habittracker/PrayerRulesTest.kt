package com.example.habittracker

import com.example.habittracker.data.PrayerRules
import com.example.habittracker.data.local.entity.PrayerStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PrayerRulesTest {
    @Test
    fun completionSemanticsAreStatusAware() {
        assertTrue(PrayerRules.countsAsCompleted(PrayerStatus.COMPLETED))
        assertTrue(PrayerRules.countsAsCompleted(PrayerStatus.QAZA))
        assertFalse(PrayerRules.countsAsCompleted(PrayerStatus.MISSED))
        assertFalse(PrayerRules.countsAsCompleted(PrayerStatus.UNRECORDED))
    }

    @Test
    fun jamaahIsOneCompletionWithTheHigherAward() {
        assertEquals(10, PrayerRules.xpFor(PrayerStatus.COMPLETED, inJamaah = false))
        assertEquals(15, PrayerRules.xpFor(PrayerStatus.COMPLETED, inJamaah = true))
        assertEquals(10, PrayerRules.xpFor(PrayerStatus.QAZA, inJamaah = true))
    }

    @Test
    fun previouslyAwardedXpCannotBeFarmedByResettingOrReRecording() {
        assertEquals(10, PrayerRules.additionalXp(0, PrayerStatus.COMPLETED, inJamaah = false))
        assertEquals(0, PrayerRules.additionalXp(10, PrayerStatus.UNRECORDED, inJamaah = false))
        assertEquals(0, PrayerRules.additionalXp(10, PrayerStatus.COMPLETED, inJamaah = false))
        assertEquals(5, PrayerRules.additionalXp(10, PrayerStatus.COMPLETED, inJamaah = true))
        assertEquals(0, PrayerRules.additionalXp(15, PrayerStatus.COMPLETED, inJamaah = true))
    }
}
