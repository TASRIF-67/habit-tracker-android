package com.example.habittracker.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.habittracker.data.local.dao.HabitDao
import com.example.habittracker.data.local.dao.PrayerDao
import com.example.habittracker.data.local.dao.SleepDao
import com.example.habittracker.data.local.dao.PrayerTimeDao
import com.example.habittracker.data.local.entity.Habit
import com.example.habittracker.data.local.entity.HabitCompletion
import com.example.habittracker.data.local.entity.PrayerReason
import com.example.habittracker.data.local.entity.PrayerRecord
import com.example.habittracker.data.local.entity.RoutineProgress
import com.example.habittracker.data.local.entity.RoutineSchedule
import com.example.habittracker.data.local.entity.ActivitySession
import com.example.habittracker.data.local.entity.SleepPlan
import com.example.habittracker.data.local.entity.SleepSession
import com.example.habittracker.data.local.entity.PrayerTimeSettings
import com.example.habittracker.data.local.entity.PrayerReminderConfig

@Database(entities = [Habit::class, HabitCompletion::class, PrayerRecord::class, PrayerReason::class, RoutineProgress::class, RoutineSchedule::class, ActivitySession::class, SleepPlan::class, SleepSession::class, PrayerTimeSettings::class, PrayerReminderConfig::class], version = 10, exportSchema = true)
@TypeConverters(HabitConverters::class)
abstract class HabitDatabase : RoomDatabase() {
    abstract fun habitDao(): HabitDao
    abstract fun prayerDao(): PrayerDao
    abstract fun sleepDao(): SleepDao
    abstract fun prayerTimeDao(): PrayerTimeDao

    companion object {
        @Volatile private var instance: HabitDatabase? = null
        fun getInstance(context: Context): HabitDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(context.applicationContext, HabitDatabase::class.java, "habits.db")
                .addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        db.execSQL("INSERT INTO habits (name, category, active, createdAt, sortOrder, isBuiltIn, routineType) VALUES ('Fajr','SALAT',1,0,10,1,'CHECK'),('Dhuhr','SALAT',1,0,20,1,'CHECK'),('Asr','SALAT',1,0,30,1,'CHECK'),('Maghrib','SALAT',1,0,40,1,'CHECK'),('Isha','SALAT',1,0,50,1,'CHECK'),('Quran','GOOD_DEED',1,0,110,1,'CHECK'),('Morning Adhkar','GOOD_DEED',1,0,120,1,'CHECK'),('Evening Adhkar','GOOD_DEED',1,0,130,1,'CHECK'),('Sadaqah','GOOD_DEED',1,0,140,1,'CHECK')")
                        seedReasons(db)
                    }
                }).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10).build().also { instance = it }
        }

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""CREATE TABLE IF NOT EXISTS `prayer_reasons` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `isBuiltIn` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL)""")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_prayer_reasons_name` ON `prayer_reasons` (`name`)")
                db.execSQL("""CREATE TABLE IF NOT EXISTS `prayer_records` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `prayer` TEXT NOT NULL, `date` TEXT NOT NULL, `status` TEXT NOT NULL, `inJamaah` INTEGER NOT NULL, `reasonId` INTEGER, `updatedAt` INTEGER NOT NULL, `xpAwarded` INTEGER NOT NULL, FOREIGN KEY(`reasonId`) REFERENCES `prayer_reasons`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL)""")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_prayer_records_date_prayer` ON `prayer_records` (`date`, `prayer`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_prayer_records_reasonId` ON `prayer_records` (`reasonId`)")
                seedReasons(db)
                db.execSQL("""
                    INSERT OR IGNORE INTO prayer_records (prayer, date, status, inJamaah, reasonId, updatedAt, xpAwarded)
                    SELECT CASE h.name
                        WHEN 'Fajr' THEN 'FAJR' WHEN 'Dhuhr' THEN 'DHUHR' WHEN 'Asr' THEN 'ASR'
                        WHEN 'Maghrib' THEN 'MAGHRIB' WHEN 'Isha' THEN 'ISHA' END,
                        c.date, 'COMPLETED', 0, NULL, 0, 0
                    FROM habit_completions c
                    INNER JOIN habits h ON h.id = c.habitId
                    WHERE h.category = 'SALAT' AND c.completed = 1
                      AND h.name IN ('Fajr', 'Dhuhr', 'Asr', 'Maghrib', 'Isha')
                """.trimIndent())
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE habits ADD COLUMN routineType TEXT NOT NULL DEFAULT 'CHECK'")
                db.execSQL("ALTER TABLE habits ADD COLUMN target INTEGER")
                db.execSQL("ALTER TABLE habits ADD COLUMN unit TEXT")
                db.execSQL("""CREATE TABLE IF NOT EXISTS `routine_progress` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `habitId` INTEGER NOT NULL, `date` TEXT NOT NULL, `value` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, FOREIGN KEY(`habitId`) REFERENCES `habits`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)""")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_routine_progress_habitId` ON `routine_progress` (`habitId`)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_routine_progress_habitId_date` ON `routine_progress` (`habitId`, `date`)")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE habits ADD COLUMN iconKey TEXT NOT NULL DEFAULT 'CHECK'")
                db.execSQL("ALTER TABLE habits ADD COLUMN themeKey TEXT NOT NULL DEFAULT 'FOREST'")
                db.execSQL("ALTER TABLE habits ADD COLUMN quantityPerCount REAL")
                db.execSQL("ALTER TABLE habits ADD COLUMN measurementUnit TEXT")
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS routine_schedules (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, habitId INTEGER NOT NULL, enabled INTEGER NOT NULL, timeMinutes INTEGER NOT NULL, daysMask INTEGER NOT NULL, reminderEnabled INTEGER NOT NULL, reminderOffsetMinutes INTEGER NOT NULL, FOREIGN KEY(habitId) REFERENCES habits(id) ON UPDATE NO ACTION ON DELETE CASCADE)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_routine_schedules_habitId ON routine_schedules(habitId)")
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS activity_sessions (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, habitId INTEGER NOT NULL, businessDate TEXT NOT NULL, startedAt INTEGER NOT NULL, accumulatedActiveMillis INTEGER NOT NULL, resumedAt INTEGER, status TEXT NOT NULL, finishedAt INTEGER, activeSlot INTEGER, FOREIGN KEY(habitId) REFERENCES habits(id) ON UPDATE NO ACTION ON DELETE CASCADE)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_activity_sessions_habitId ON activity_sessions(habitId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_activity_sessions_status ON activity_sessions(status)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_activity_sessions_activeSlot ON activity_sessions(activeSlot)")
            }
        }

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS sleep_plan (id INTEGER NOT NULL, enabled INTEGER NOT NULL, bedtimeMinutes INTEGER NOT NULL, wakeTimeMinutes INTEGER NOT NULL, daysMask INTEGER NOT NULL, windDownEnabled INTEGER NOT NULL, windDownOffsetMinutes INTEGER NOT NULL, bedtimeReminderEnabled INTEGER NOT NULL, PRIMARY KEY(id))")
                db.execSQL("CREATE TABLE IF NOT EXISTS sleep_sessions (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, sleepDate TEXT NOT NULL, wentToBedAt INTEGER NOT NULL, wokeUpAt INTEGER, status TEXT NOT NULL, activeSlot INTEGER, plannedBedtimeMinutes INTEGER NOT NULL, plannedWakeTimeMinutes INTEGER NOT NULL)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_sleep_sessions_sleepDate ON sleep_sessions(sleepDate)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_sleep_sessions_status ON sleep_sessions(status)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_sleep_sessions_activeSlot ON sleep_sessions(activeSlot)")
            }
        }

        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE routine_schedules ADD COLUMN reminderStyle TEXT NOT NULL DEFAULT 'PROMINENT'")
            }
        }

        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS prayer_time_settings (id INTEGER NOT NULL, locationLabel TEXT NOT NULL, latitude REAL NOT NULL, longitude REAL NOT NULL, calculationMethod TEXT NOT NULL, asrMethod TEXT NOT NULL, PRIMARY KEY(id))")
                db.execSQL("CREATE TABLE IF NOT EXISTS prayer_reminder_config (prayer TEXT NOT NULL, enabled INTEGER NOT NULL, offsetMinutes INTEGER NOT NULL, PRIMARY KEY(prayer))")
            }
        }

        val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE routine_schedules ADD COLUMN endTimeMinutes INTEGER")
            }
        }

        private fun seedReasons(db: SupportSQLiteDatabase) {
            db.execSQL("""INSERT OR IGNORE INTO prayer_reasons (name, isBuiltIn, createdAt) VALUES
                ('Overslept',1,0),('Forgot',1,0),('Work',1,0),('Study / exam',1,0),('Travel',1,0),
                ('Traffic / commute',1,0),('Illness',1,0),('No suitable place',1,0),('Laziness / procrastination',1,0),('Other',1,0)
            """.trimIndent())
        }
    }
}
