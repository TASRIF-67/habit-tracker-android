package com.example.habittracker

import com.example.habittracker.data.local.dao.PrayerDao
import com.example.habittracker.data.local.dao.PrayerRecordDetails
import com.example.habittracker.data.local.dao.ReasonCount
import com.example.habittracker.data.local.entity.Prayer
import com.example.habittracker.data.local.entity.PrayerReason
import com.example.habittracker.data.local.entity.PrayerRecord
import com.example.habittracker.data.local.entity.PrayerStatus
import com.example.habittracker.data.repository.PrayerRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class PrayerRepositoryTest {
    @Test
    fun statusUpdatesKeepOneRecordAndAwardXpOnlyOnce() = runBlocking {
        val dao = FakePrayerDao()
        val repository = PrayerRepository(dao)

        assertEquals(10, repository.recordPrayer(DATE, Prayer.FAJR, PrayerStatus.COMPLETED, false, null).xpGranted)
        repository.clearRecord(DATE, Prayer.FAJR)
        assertEquals(0, repository.recordPrayer(DATE, Prayer.FAJR, PrayerStatus.COMPLETED, false, null).xpGranted)
        assertEquals(5, repository.recordPrayer(DATE, Prayer.FAJR, PrayerStatus.COMPLETED, true, null).xpGranted)

        assertEquals(1, dao.records.size)
        assertEquals(15, dao.records.single().xpAwarded)
        assertEquals(PrayerStatus.COMPLETED, dao.records.single().status)
        assertFalse(repository.recordPrayer(DATE, Prayer.FAJR, PrayerStatus.MISSED, true, 42).let { dao.records.single().inJamaah })
    }

    private class FakePrayerDao : PrayerDao {
        val records = mutableListOf<PrayerRecord>()
        private val reasonRows = mutableListOf<PrayerReason>()

        override fun observeRecordsForDate(date: String): Flow<List<PrayerRecordDetails>> = flowOf(emptyList())
        override fun observeRecordsBetween(start: String, end: String): Flow<List<PrayerRecordDetails>> = flowOf(emptyList())
        override suspend fun record(date: String, prayer: Prayer) = records.singleOrNull { it.date == date && it.prayer == prayer }
        override suspend fun insertRecord(record: PrayerRecord): Long {
            check(records.none { it.date == record.date && it.prayer == record.prayer })
            val id = (records.maxOfOrNull { it.id } ?: 0) + 1
            records += record.copy(id = id)
            return id
        }
        override suspend fun updateRecord(record: PrayerRecord) {
            val index = records.indexOfFirst { it.id == record.id }
            check(index >= 0)
            records[index] = record
        }
        override suspend fun deleteRecord(date: String, prayer: Prayer) { records.removeAll { it.date == date && it.prayer == prayer } }
        override fun observeReasons(): Flow<List<PrayerReason>> = flowOf(reasonRows)
        override suspend fun reasons(): List<PrayerReason> = reasonRows
        override suspend fun insertReason(reason: PrayerReason): Long {
            val id = (reasonRows.maxOfOrNull { it.id } ?: 0) + 1
            reasonRows += reason.copy(id = id)
            return id
        }
        override fun observeTotalXp(): Flow<Int> = flowOf(records.sumOf { it.xpAwarded })
        override fun observeReasonCounts(status: PrayerStatus, limit: Int): Flow<List<ReasonCount>> = flowOf(emptyList())
    }

    companion object { const val DATE = "2026-10-03" }
}
