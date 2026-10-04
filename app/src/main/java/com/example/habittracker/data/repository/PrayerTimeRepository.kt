package com.example.habittracker.data.repository

import com.example.habittracker.data.PrayerTimeRules
import com.example.habittracker.data.local.dao.PrayerTimeDao
import com.example.habittracker.data.local.entity.*
import com.example.habittracker.reminders.PrayerReminderScheduler
import kotlinx.coroutines.flow.Flow

class PrayerTimeRepository(private val dao: PrayerTimeDao, private val scheduler: PrayerReminderScheduler? = null) {
    fun settings(): Flow<PrayerTimeSettings?> = dao.observeSettings()
    fun reminders(): Flow<List<PrayerReminderConfig>> = dao.observeReminders()
    suspend fun settingsSnapshot() = dao.settings()
    suspend fun reminderSnapshot() = dao.reminders()
    suspend fun saveSettings(settings: PrayerTimeSettings): Result<Unit> = runCatching {
        require(settings.locationLabel.isNotBlank()) { "Location name is required" }
        require(settings.latitude in -90.0..90.0) { "Latitude must be between -90 and 90" }
        require(settings.longitude in -180.0..180.0) { "Longitude must be between -180 and 180" }
        dao.saveSettings(settings.copy(id = 1)); rescheduleReminders()
    }
    suspend fun saveReminder(config: PrayerReminderConfig): Result<Unit> = runCatching {
        require(config.offsetMinutes in PrayerTimeRules.reminderOffsets)
        dao.saveReminder(config); rescheduleReminders()
    }
    suspend fun rescheduleReminders() { scheduler?.sync(dao.settings(), dao.reminders()) }
}
