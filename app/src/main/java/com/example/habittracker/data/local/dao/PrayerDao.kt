package com.example.habittracker.data.local.dao

import androidx.room.*
import com.example.habittracker.data.local.entity.*
import kotlinx.coroutines.flow.Flow

data class PrayerRecordDetails(
    val id: Long,
    val prayer: Prayer,
    val date: String,
    val status: PrayerStatus,
    val inJamaah: Boolean,
    val reasonId: Long?,
    val updatedAt: Long,
    val xpAwarded: Int,
    val reasonName: String?,
)

data class ReasonCount(val reason: String, val count: Int)

@Dao
interface PrayerDao {
    @Query("""SELECT pr.*, rr.name AS reasonName FROM prayer_records pr LEFT JOIN prayer_reasons rr ON rr.id = pr.reasonId WHERE pr.date = :date ORDER BY pr.prayer""")
    fun observeRecordsForDate(date: String): Flow<List<PrayerRecordDetails>>

    @Query("""SELECT pr.*, rr.name AS reasonName FROM prayer_records pr LEFT JOIN prayer_reasons rr ON rr.id = pr.reasonId WHERE pr.date BETWEEN :start AND :end ORDER BY pr.date, pr.prayer""")
    fun observeRecordsBetween(start: String, end: String): Flow<List<PrayerRecordDetails>>

    @Query("SELECT * FROM prayer_records WHERE date = :date AND prayer = :prayer LIMIT 1")
    suspend fun record(date: String, prayer: Prayer): PrayerRecord?

    @Insert suspend fun insertRecord(record: PrayerRecord): Long
    @Update suspend fun updateRecord(record: PrayerRecord)
    @Query("DELETE FROM prayer_records WHERE date = :date AND prayer = :prayer") suspend fun deleteRecord(date: String, prayer: Prayer)

    @Query("SELECT * FROM prayer_reasons ORDER BY isBuiltIn DESC, name COLLATE NOCASE")
    fun observeReasons(): Flow<List<PrayerReason>>

    @Query("SELECT * FROM prayer_reasons") suspend fun reasons(): List<PrayerReason>
    @Insert suspend fun insertReason(reason: PrayerReason): Long

    @Query("SELECT COALESCE(SUM(xpAwarded), 0) FROM prayer_records")
    fun observeTotalXp(): Flow<Int>

    @Query("""SELECT rr.name AS reason, COUNT(*) AS count FROM prayer_records pr JOIN prayer_reasons rr ON rr.id = pr.reasonId WHERE pr.status = :status GROUP BY pr.reasonId ORDER BY count DESC, rr.name LIMIT :limit""")
    fun observeReasonCounts(status: PrayerStatus, limit: Int = 10): Flow<List<ReasonCount>>
}
