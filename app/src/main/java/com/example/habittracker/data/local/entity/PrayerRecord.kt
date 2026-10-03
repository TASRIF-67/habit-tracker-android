package com.example.habittracker.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class Prayer(val displayName: String) {
    FAJR("Fajr"), DHUHR("Dhuhr"), ASR("Asr"), MAGHRIB("Maghrib"), ISHA("Isha");

    companion object {
        fun fromHabitName(name: String): Prayer? = entries.firstOrNull { it.displayName.equals(name, ignoreCase = true) }
    }
}

enum class PrayerStatus {
    UNRECORDED, COMPLETED, QAZA, MISSED;

    val countsAsCompleted: Boolean get() = this == COMPLETED || this == QAZA
}

@Entity(
    tableName = "prayer_records",
    foreignKeys = [ForeignKey(entity = PrayerReason::class, parentColumns = ["id"], childColumns = ["reasonId"], onDelete = ForeignKey.SET_NULL)],
    indices = [Index(value = ["date", "prayer"], unique = true), Index("reasonId")],
)
data class PrayerRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val prayer: Prayer,
    val date: String,
    val status: PrayerStatus,
    val inJamaah: Boolean = false,
    val reasonId: Long? = null,
    val updatedAt: Long = System.currentTimeMillis(),
    val xpAwarded: Int = 0,
)
