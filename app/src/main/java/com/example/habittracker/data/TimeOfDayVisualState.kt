package com.example.habittracker.data

import java.time.Duration
import java.time.LocalTime

enum class TimeOfDayVisualState {
    DAWN,
    MORNING,
    DAY,
    AFTERNOON,
    SUNSET,
    NIGHT,
}

object TimeOfDayVisualResolver {
    private val dawn = LocalTime.of(4, 30)
    private val morning = LocalTime.of(7, 0)
    private val day = LocalTime.NOON
    private val afternoon = LocalTime.of(15, 30)
    private val sunset = LocalTime.of(18, 0)
    private val night = LocalTime.of(19, 30)
    private val boundaries = listOf(dawn, morning, day, afternoon, sunset, night)

    fun resolve(time: LocalTime): TimeOfDayVisualState = when {
        time < dawn -> TimeOfDayVisualState.NIGHT
        time < morning -> TimeOfDayVisualState.DAWN
        time < day -> TimeOfDayVisualState.MORNING
        time < afternoon -> TimeOfDayVisualState.DAY
        time < sunset -> TimeOfDayVisualState.AFTERNOON
        time < night -> TimeOfDayVisualState.SUNSET
        else -> TimeOfDayVisualState.NIGHT
    }

    fun durationUntilNextBoundary(time: LocalTime): Duration {
        val next = boundaries.firstOrNull { it > time } ?: dawn
        val seconds = if (next > time) {
            Duration.between(time, next).seconds
        } else {
            Duration.between(time, LocalTime.MAX).seconds + 1 + Duration.between(LocalTime.MIN, next).seconds
        }
        return Duration.ofSeconds(seconds.coerceAtLeast(1))
    }

    fun greeting(state: TimeOfDayVisualState): String = when (state) {
        TimeOfDayVisualState.DAWN,
        TimeOfDayVisualState.MORNING,
        -> "Good morning"
        TimeOfDayVisualState.DAY,
        TimeOfDayVisualState.AFTERNOON,
        -> "Good afternoon"
        TimeOfDayVisualState.SUNSET,
        TimeOfDayVisualState.NIGHT,
        -> "Good evening"
    }
}
