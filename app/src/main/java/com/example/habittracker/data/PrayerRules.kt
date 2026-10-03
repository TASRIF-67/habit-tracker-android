package com.example.habittracker.data

import com.example.habittracker.data.local.entity.PrayerStatus

object PrayerRules {
    fun countsAsCompleted(status: PrayerStatus): Boolean = status.countsAsCompleted

    fun xpFor(status: PrayerStatus, inJamaah: Boolean): Int = when {
        status == PrayerStatus.COMPLETED && inJamaah -> 15
        status == PrayerStatus.COMPLETED || status == PrayerStatus.QAZA -> 10
        else -> 0
    }

    fun additionalXp(previouslyAwarded: Int, status: PrayerStatus, inJamaah: Boolean): Int =
        (xpFor(status, inJamaah) - previouslyAwarded).coerceAtLeast(0)
}
