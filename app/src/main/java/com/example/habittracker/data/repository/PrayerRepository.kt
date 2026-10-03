package com.example.habittracker.data.repository

import com.example.habittracker.data.PrayerRules
import com.example.habittracker.data.local.dao.PrayerDao
import com.example.habittracker.data.local.entity.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class PrayerUpdateResult(val xpGranted: Int)

sealed interface ReasonValidation {
    data class Valid(val name: String) : ReasonValidation
    data class Invalid(val message: String) : ReasonValidation
}

class PrayerRepository(private val dao: PrayerDao) {
    private val updateMutex = Mutex()

    fun recordsForDate(date: String) = dao.observeRecordsForDate(date)
    fun recordsBetween(start: String, end: String) = dao.observeRecordsBetween(start, end)
    fun reasons() = dao.observeReasons()
    fun totalXp() = dao.observeTotalXp()
    fun missedReasonCounts() = dao.observeReasonCounts(PrayerStatus.MISSED)
    fun qazaReasonCounts() = dao.observeReasonCounts(PrayerStatus.QAZA)

    suspend fun recordPrayer(date: String, prayer: Prayer, status: PrayerStatus, inJamaah: Boolean, reasonId: Long?): PrayerUpdateResult = updateMutex.withLock {
        if (status == PrayerStatus.UNRECORDED) {
            clearRecordLocked(date, prayer)
            return@withLock PrayerUpdateResult(0)
        }
        val existing = dao.record(date, prayer)
        val jamaah = status == PrayerStatus.COMPLETED && inJamaah
        val previousAward = existing?.xpAwarded ?: 0
        val xpGranted = PrayerRules.additionalXp(previousAward, status, jamaah)
        val record = PrayerRecord(
            id = existing?.id ?: 0,
            prayer = prayer,
            date = date,
            status = status,
            inJamaah = jamaah,
            reasonId = if (status == PrayerStatus.QAZA || status == PrayerStatus.MISSED) reasonId else null,
            updatedAt = System.currentTimeMillis(),
            xpAwarded = previousAward + xpGranted,
        )
        if (existing == null) dao.insertRecord(record) else dao.updateRecord(record)
        PrayerUpdateResult(xpGranted)
    }

    suspend fun clearRecord(date: String, prayer: Prayer) = updateMutex.withLock { clearRecordLocked(date, prayer) }

    private suspend fun clearRecordLocked(date: String, prayer: Prayer) {
        val existing = dao.record(date, prayer) ?: return
        dao.updateRecord(
            existing.copy(
                status = PrayerStatus.UNRECORDED,
                inJamaah = false,
                reasonId = null,
                updatedAt = System.currentTimeMillis(),
            ),
        )
    }

    suspend fun addCustomReason(rawName: String): Result<Long> {
        return when (val validation = validateReason(rawName, dao.reasons().map { it.name })) {
            is ReasonValidation.Invalid -> Result.failure(IllegalArgumentException(validation.message))
            is ReasonValidation.Valid -> runCatching { dao.insertReason(PrayerReason(name = validation.name)) }
        }
    }

    companion object {
        const val MAX_REASON_LENGTH = 50
        fun validateReason(rawName: String, existingNames: List<String>): ReasonValidation {
            val name = rawName.trim().replace(Regex("\\s+"), " ")
            return when {
                name.isBlank() -> ReasonValidation.Invalid("Reason cannot be blank")
                name.length > MAX_REASON_LENGTH -> ReasonValidation.Invalid("Reason must be $MAX_REASON_LENGTH characters or fewer")
                existingNames.any { it.equals(name, ignoreCase = true) } -> ReasonValidation.Invalid("That reason already exists")
                else -> ReasonValidation.Valid(name)
            }
        }
    }
}
