package com.example.habittracker.data.local.dao

import androidx.room.*
import com.example.habittracker.data.local.entity.*
import kotlinx.coroutines.flow.Flow

@Dao
interface PrayerTimeDao {
    @Query("SELECT * FROM prayer_time_settings WHERE id = 1 LIMIT 1") fun observeSettings(): Flow<PrayerTimeSettings?>
    @Query("SELECT * FROM prayer_time_settings WHERE id = 1 LIMIT 1") suspend fun settings(): PrayerTimeSettings?
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun saveSettings(settings: PrayerTimeSettings)
    @Query("SELECT * FROM prayer_reminder_config ORDER BY prayer") fun observeReminders(): Flow<List<PrayerReminderConfig>>
    @Query("SELECT * FROM prayer_reminder_config ORDER BY prayer") suspend fun reminders(): List<PrayerReminderConfig>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun saveReminder(config: PrayerReminderConfig)
}
