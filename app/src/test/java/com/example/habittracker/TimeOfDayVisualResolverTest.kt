package com.example.habittracker

import com.example.habittracker.data.TimeOfDayVisualResolver
import com.example.habittracker.data.TimeOfDayVisualState
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Test

class TimeOfDayVisualResolverTest {
    @Test
    fun resolvesEveryBoundaryDeterministically() {
        val cases = mapOf(
            "04:29" to TimeOfDayVisualState.NIGHT,
            "04:30" to TimeOfDayVisualState.DAWN,
            "06:59" to TimeOfDayVisualState.DAWN,
            "07:00" to TimeOfDayVisualState.MORNING,
            "11:59" to TimeOfDayVisualState.MORNING,
            "12:00" to TimeOfDayVisualState.DAY,
            "15:29" to TimeOfDayVisualState.DAY,
            "15:30" to TimeOfDayVisualState.AFTERNOON,
            "17:59" to TimeOfDayVisualState.AFTERNOON,
            "18:00" to TimeOfDayVisualState.SUNSET,
            "19:29" to TimeOfDayVisualState.SUNSET,
            "19:30" to TimeOfDayVisualState.NIGHT,
            "23:59" to TimeOfDayVisualState.NIGHT,
            "00:00" to TimeOfDayVisualState.NIGHT,
        )
        cases.forEach { (time, expected) -> assertEquals(time, expected, TimeOfDayVisualResolver.resolve(LocalTime.parse(time))) }
    }

    @Test
    fun resolverHasNoPrayerStateInput() {
        val time = LocalTime.of(21, 0)
        assertEquals(TimeOfDayVisualState.NIGHT, TimeOfDayVisualResolver.resolve(time))
    }
}
