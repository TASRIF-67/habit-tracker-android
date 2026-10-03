package com.example.habittracker

import com.example.habittracker.data.repository.PrayerRepository
import com.example.habittracker.data.repository.ReasonValidation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PrayerReasonValidationTest {
    @Test
    fun trimsAndCollapsesWhitespace() {
        val result = PrayerRepository.validateReason("  Family   emergency  ", emptyList())
        assertEquals(ReasonValidation.Valid("Family emergency"), result)
    }

    @Test
    fun rejectsBlankDuplicateAndOverlongReasons() {
        assertTrue(PrayerRepository.validateReason("   ", emptyList()) is ReasonValidation.Invalid)
        assertTrue(PrayerRepository.validateReason("travel", listOf("Travel")) is ReasonValidation.Invalid)
        assertTrue(
            PrayerRepository.validateReason("x".repeat(PrayerRepository.MAX_REASON_LENGTH + 1), emptyList())
                is ReasonValidation.Invalid,
        )
    }
}
