package com.example.habittracker.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.habittracker.data.local.entity.SleepPlan
import com.example.habittracker.data.local.entity.SleepSession
import kotlinx.coroutines.flow.Flow

@Dao
interface SleepDao {
    @Query("SELECT * FROM sleep_plan WHERE id = 1 LIMIT 1") fun observePlan(): Flow<SleepPlan?>
    @Query("SELECT * FROM sleep_plan WHERE id = 1 LIMIT 1") suspend fun plan(): SleepPlan?
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun savePlan(plan: SleepPlan)

    @Query("SELECT * FROM sleep_sessions WHERE activeSlot = 1 LIMIT 1") fun observeActiveSession(): Flow<SleepSession?>
    @Query("SELECT * FROM sleep_sessions WHERE activeSlot = 1 LIMIT 1") suspend fun activeSession(): SleepSession?
    @Query("SELECT * FROM sleep_sessions WHERE id = :id LIMIT 1") suspend fun session(id: Long): SleepSession?
    @Insert suspend fun insertSession(session: SleepSession): Long
    @Update suspend fun updateSession(session: SleepSession)
    @Query("SELECT * FROM sleep_sessions WHERE sleepDate = :date AND status = 'COMPLETED' ORDER BY wentToBedAt DESC") fun observeForDate(date: String): Flow<List<SleepSession>>
    @Query("SELECT * FROM sleep_sessions WHERE sleepDate BETWEEN :start AND :end AND status = 'COMPLETED' ORDER BY wentToBedAt DESC") fun observeBetween(start: String, end: String): Flow<List<SleepSession>>
    @Query("SELECT * FROM sleep_sessions WHERE status = 'COMPLETED' ORDER BY wentToBedAt DESC LIMIT 1") fun observeLatestCompleted(): Flow<SleepSession?>
}
