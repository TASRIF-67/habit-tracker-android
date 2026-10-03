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
import com.example.habittracker.data.local.entity.Habit
import com.example.habittracker.data.local.entity.HabitCompletion
import com.example.habittracker.data.local.entity.PrayerReason
import com.example.habittracker.data.local.entity.PrayerRecord

@Database(entities = [Habit::class, HabitCompletion::class, PrayerRecord::class, PrayerReason::class], version = 2, exportSchema = true)
@TypeConverters(HabitConverters::class)
abstract class HabitDatabase : RoomDatabase() {
    abstract fun habitDao(): HabitDao
    abstract fun prayerDao(): PrayerDao

    companion object {
        @Volatile private var instance: HabitDatabase? = null
        fun getInstance(context: Context): HabitDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(context.applicationContext, HabitDatabase::class.java, "habits.db")
                .addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        db.execSQL("INSERT INTO habits (name, category, active, createdAt, sortOrder, isBuiltIn) VALUES ('Fajr','SALAT',1,0,10,1),('Dhuhr','SALAT',1,0,20,1),('Asr','SALAT',1,0,30,1),('Maghrib','SALAT',1,0,40,1),('Isha','SALAT',1,0,50,1),('Quran','GOOD_DEED',1,0,110,1),('Morning Adhkar','GOOD_DEED',1,0,120,1),('Evening Adhkar','GOOD_DEED',1,0,130,1),('Sadaqah','GOOD_DEED',1,0,140,1)")
                        seedReasons(db)
                    }
                }).addMigrations(MIGRATION_1_2).build().also { instance = it }
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

        private fun seedReasons(db: SupportSQLiteDatabase) {
            db.execSQL("""INSERT OR IGNORE INTO prayer_reasons (name, isBuiltIn, createdAt) VALUES
                ('Overslept',1,0),('Forgot',1,0),('Work',1,0),('Study / exam',1,0),('Travel',1,0),
                ('Traffic / commute',1,0),('Illness',1,0),('No suitable place',1,0),('Laziness / procrastination',1,0),('Other',1,0)
            """.trimIndent())
        }
    }
}
