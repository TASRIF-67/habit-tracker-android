package com.example.habittracker

import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.platform.app.InstrumentationRegistry
import com.example.habittracker.data.local.HabitDatabase
import com.example.habittracker.data.local.entity.Prayer
import com.example.habittracker.data.local.entity.PrayerStatus
import com.example.habittracker.data.local.entity.RoutineType
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PrayerMigrationTest {
    @Test
    fun migration9To10PreservesExistingSingleTimeSchedule() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val name = "routine-time-block-migration-test.db"
        context.deleteDatabase(name)
        FrameworkSQLiteOpenHelperFactory().create(SupportSQLiteOpenHelper.Configuration.builder(context).name(name).callback(object : SupportSQLiteOpenHelper.Callback(9) {
            override fun onCreate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE routine_schedules (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, habitId INTEGER NOT NULL, enabled INTEGER NOT NULL, timeMinutes INTEGER NOT NULL, daysMask INTEGER NOT NULL, reminderEnabled INTEGER NOT NULL, reminderOffsetMinutes INTEGER NOT NULL, reminderStyle TEXT NOT NULL)")
                db.execSQL("INSERT INTO routine_schedules (id,habitId,enabled,timeMinutes,daysMask,reminderEnabled,reminderOffsetMinutes,reminderStyle) VALUES (1,42,1,1140,127,1,10,'PROMINENT')")
            }
            override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
        }).build()).writableDatabase.close()
        val helper = FrameworkSQLiteOpenHelperFactory().create(SupportSQLiteOpenHelper.Configuration.builder(context).name(name).callback(object : SupportSQLiteOpenHelper.Callback(10) {
            override fun onCreate(db: SupportSQLiteDatabase) = Unit
            override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = HabitDatabase.MIGRATION_9_10.migrate(db)
        }).build())
        helper.writableDatabase.query("SELECT timeMinutes,endTimeMinutes FROM routine_schedules WHERE id=1").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(1140, cursor.getInt(0))
            assertTrue(cursor.isNull(1))
        }
        helper.close()
        context.deleteDatabase(name)
    }

    @Test
    fun freshVersion5DatabaseSeedsCheckHabits() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val database = Room.inMemoryDatabaseBuilder(context, HabitDatabase::class.java)
            .addCallback(object : androidx.room.RoomDatabase.Callback() {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    db.execSQL("INSERT INTO habits (name, category, active, createdAt, sortOrder, isBuiltIn, routineType) VALUES ('Fajr','SALAT',1,0,10,1,'CHECK')")
                }
            }).build()
        try { assertEquals(RoutineType.CHECK, database.habitDao().observeAllHabits().first().single().routineType) } finally { database.close() }
    }

    @Test
    fun migrationPreservesHabitsAndImportsOnlyCompletedPrayerRows() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val name = "prayer-migration-test.db"
        context.deleteDatabase(name)
        val helper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(name)
                .callback(object : SupportSQLiteOpenHelper.Callback(1) {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        db.execSQL("CREATE TABLE habits (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, name TEXT NOT NULL, category TEXT NOT NULL, active INTEGER NOT NULL, createdAt INTEGER NOT NULL, sortOrder INTEGER NOT NULL, isBuiltIn INTEGER NOT NULL)")
                        db.execSQL("CREATE TABLE habit_completions (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, habitId INTEGER NOT NULL, date TEXT NOT NULL, completed INTEGER NOT NULL, FOREIGN KEY(habitId) REFERENCES habits(id) ON UPDATE NO ACTION ON DELETE CASCADE)")
                        db.execSQL("CREATE INDEX index_habit_completions_habitId ON habit_completions(habitId)")
                        db.execSQL("CREATE UNIQUE INDEX index_habit_completions_habitId_date ON habit_completions(habitId, date)")
                        db.execSQL("INSERT INTO habits (id,name,category,active,createdAt,sortOrder,isBuiltIn) VALUES (1,'Fajr','SALAT',1,0,10,1),(2,'Dhuhr','SALAT',1,0,20,1),(3,'Quran','GOOD_DEED',1,0,110,1)")
                        db.execSQL("INSERT INTO habit_completions (habitId,date,completed) VALUES (1,'2026-10-03',1),(2,'2026-10-03',0),(3,'2026-10-03',1)")
                    }

                    override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
                })
                .build(),
        )
        helper.writableDatabase.close()

        val database = Room.databaseBuilder(context, HabitDatabase::class.java, name)
            .addMigrations(HabitDatabase.MIGRATION_1_2, HabitDatabase.MIGRATION_2_3, HabitDatabase.MIGRATION_3_4, HabitDatabase.MIGRATION_4_5, HabitDatabase.MIGRATION_5_6, HabitDatabase.MIGRATION_6_7, HabitDatabase.MIGRATION_7_8, HabitDatabase.MIGRATION_8_9, HabitDatabase.MIGRATION_9_10)
            .build()
        try {
            val fajr = database.prayerDao().record("2026-10-03", Prayer.FAJR)
            assertNotNull(fajr)
            assertEquals(PrayerStatus.COMPLETED, fajr?.status)
            assertEquals(0, fajr?.xpAwarded)
            assertNull(database.prayerDao().record("2026-10-03", Prayer.DHUHR))
            database.openHelper.readableDatabase.query("SELECT COUNT(*) FROM habits").use { cursor ->
                cursor.moveToFirst()
                assertEquals(3, cursor.getInt(0))
            }
        } finally {
            database.close()
            context.deleteDatabase(name)
        }
    }

    @Test
    fun migration2To3PreservesCheckHabitsCompletionsAndPrayerTables() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val name = "routine-migration-test.db"
        context.deleteDatabase(name)
        val helper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context).name(name)
                .callback(object : SupportSQLiteOpenHelper.Callback(2) {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        db.execSQL("CREATE TABLE habits (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, name TEXT NOT NULL, category TEXT NOT NULL, active INTEGER NOT NULL, createdAt INTEGER NOT NULL, sortOrder INTEGER NOT NULL, isBuiltIn INTEGER NOT NULL)")
                        db.execSQL("CREATE TABLE habit_completions (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, habitId INTEGER NOT NULL, date TEXT NOT NULL, completed INTEGER NOT NULL, FOREIGN KEY(habitId) REFERENCES habits(id) ON UPDATE NO ACTION ON DELETE CASCADE)")
                        db.execSQL("CREATE INDEX index_habit_completions_habitId ON habit_completions(habitId)")
                        db.execSQL("CREATE UNIQUE INDEX index_habit_completions_habitId_date ON habit_completions(habitId, date)")
                        db.execSQL("CREATE TABLE prayer_reasons (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, name TEXT NOT NULL, isBuiltIn INTEGER NOT NULL, createdAt INTEGER NOT NULL)")
                        db.execSQL("CREATE UNIQUE INDEX index_prayer_reasons_name ON prayer_reasons(name)")
                        db.execSQL("CREATE TABLE prayer_records (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, prayer TEXT NOT NULL, date TEXT NOT NULL, status TEXT NOT NULL, inJamaah INTEGER NOT NULL, reasonId INTEGER, updatedAt INTEGER NOT NULL, xpAwarded INTEGER NOT NULL, FOREIGN KEY(reasonId) REFERENCES prayer_reasons(id) ON UPDATE NO ACTION ON DELETE SET NULL)")
                        db.execSQL("CREATE UNIQUE INDEX index_prayer_records_date_prayer ON prayer_records(date, prayer)")
                        db.execSQL("CREATE INDEX index_prayer_records_reasonId ON prayer_records(reasonId)")
                        db.execSQL("INSERT INTO habits (id,name,category,active,createdAt,sortOrder,isBuiltIn) VALUES (10,'Make bed','PERSONAL',0,0,1000,0)")
                        db.execSQL("INSERT INTO habit_completions (habitId,date,completed) VALUES (10,'2026-10-02',1)")
                        db.execSQL("INSERT INTO prayer_records (prayer,date,status,inJamaah,reasonId,updatedAt,xpAwarded) VALUES ('FAJR','2026-10-02','COMPLETED',1,NULL,0,15)")
                    }
                    override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
                }).build(),
        )
        helper.writableDatabase.close()
        val database = Room.databaseBuilder(context, HabitDatabase::class.java, name).addMigrations(HabitDatabase.MIGRATION_2_3, HabitDatabase.MIGRATION_3_4, HabitDatabase.MIGRATION_4_5, HabitDatabase.MIGRATION_5_6, HabitDatabase.MIGRATION_6_7, HabitDatabase.MIGRATION_7_8, HabitDatabase.MIGRATION_8_9, HabitDatabase.MIGRATION_9_10).build()
        try {
            val habit = database.habitDao().observeAllHabits().first().single()
            assertEquals(RoutineType.CHECK, habit.routineType)
            assertNotNull(database.habitDao().completion(10, "2026-10-02"))
            assertEquals(15, database.prayerDao().record("2026-10-02", Prayer.FAJR)?.xpAwarded)
            assertNull(database.habitDao().progress(10, "2026-10-02"))
        } finally { database.close(); context.deleteDatabase(name) }
    }

    @Test
    fun migration3To4AddsIdentityAndMeasurementDefaultsWithoutLosingData() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val name = "routine-identity-migration-test.db"
        context.deleteDatabase(name)
        val helper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context).name(name)
                .callback(object : SupportSQLiteOpenHelper.Callback(3) {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        db.execSQL("CREATE TABLE habits (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, name TEXT NOT NULL, category TEXT NOT NULL, active INTEGER NOT NULL, createdAt INTEGER NOT NULL, sortOrder INTEGER NOT NULL, isBuiltIn INTEGER NOT NULL, routineType TEXT NOT NULL, target INTEGER, unit TEXT)")
                        db.execSQL("CREATE TABLE habit_completions (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, habitId INTEGER NOT NULL, date TEXT NOT NULL, completed INTEGER NOT NULL, FOREIGN KEY(habitId) REFERENCES habits(id) ON UPDATE NO ACTION ON DELETE CASCADE)")
                        db.execSQL("CREATE INDEX index_habit_completions_habitId ON habit_completions(habitId)")
                        db.execSQL("CREATE UNIQUE INDEX index_habit_completions_habitId_date ON habit_completions(habitId,date)")
                        db.execSQL("CREATE TABLE prayer_reasons (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, name TEXT NOT NULL, isBuiltIn INTEGER NOT NULL, createdAt INTEGER NOT NULL)")
                        db.execSQL("CREATE UNIQUE INDEX index_prayer_reasons_name ON prayer_reasons(name)")
                        db.execSQL("CREATE TABLE prayer_records (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, prayer TEXT NOT NULL, date TEXT NOT NULL, status TEXT NOT NULL, inJamaah INTEGER NOT NULL, reasonId INTEGER, updatedAt INTEGER NOT NULL, xpAwarded INTEGER NOT NULL, FOREIGN KEY(reasonId) REFERENCES prayer_reasons(id) ON UPDATE NO ACTION ON DELETE SET NULL)")
                        db.execSQL("CREATE UNIQUE INDEX index_prayer_records_date_prayer ON prayer_records(date,prayer)")
                        db.execSQL("CREATE INDEX index_prayer_records_reasonId ON prayer_records(reasonId)")
                        db.execSQL("CREATE TABLE routine_progress (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, habitId INTEGER NOT NULL, date TEXT NOT NULL, value INTEGER NOT NULL, updatedAt INTEGER NOT NULL, FOREIGN KEY(habitId) REFERENCES habits(id) ON UPDATE NO ACTION ON DELETE CASCADE)")
                        db.execSQL("CREATE INDEX index_routine_progress_habitId ON routine_progress(habitId)")
                        db.execSQL("CREATE UNIQUE INDEX index_routine_progress_habitId_date ON routine_progress(habitId,date)")
                        db.execSQL("INSERT INTO habits (id,name,category,active,createdAt,sortOrder,isBuiltIn,routineType,target,unit) VALUES (20,'Existing count','PERSONAL',1,0,1000,0,'COUNT',4,'bottles')")
                        db.execSQL("INSERT INTO routine_progress (id,habitId,date,value,updatedAt) VALUES (1,20,'2026-10-04',3,0)")
                    }
                    override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
                }).build(),
        )
        helper.writableDatabase.close()
        val database = Room.databaseBuilder(context, HabitDatabase::class.java, name).addMigrations(HabitDatabase.MIGRATION_3_4, HabitDatabase.MIGRATION_4_5, HabitDatabase.MIGRATION_5_6, HabitDatabase.MIGRATION_6_7, HabitDatabase.MIGRATION_7_8, HabitDatabase.MIGRATION_8_9, HabitDatabase.MIGRATION_9_10).build()
        try {
            val habit = database.habitDao().observeAllHabits().first().single()
            assertEquals("CHECK", habit.iconKey)
            assertEquals("FOREST", habit.themeKey)
            assertNull(habit.quantityPerCount)
            assertNull(habit.measurementUnit)
            assertEquals(3, database.habitDao().progress(20, "2026-10-04")?.value)
        } finally { database.close(); context.deleteDatabase(name) }
    }

    @Test
    fun migration4To5PreservesExistingDataAndCreatesNoSchedules() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val name = "routine-schedule-migration-test.db"
        context.deleteDatabase(name)
        val helper = FrameworkSQLiteOpenHelperFactory().create(SupportSQLiteOpenHelper.Configuration.builder(context).name(name).callback(object : SupportSQLiteOpenHelper.Callback(4) {
            override fun onCreate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE habits (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, name TEXT NOT NULL, category TEXT NOT NULL, active INTEGER NOT NULL, createdAt INTEGER NOT NULL, sortOrder INTEGER NOT NULL, isBuiltIn INTEGER NOT NULL, routineType TEXT NOT NULL, target INTEGER, unit TEXT, iconKey TEXT NOT NULL DEFAULT 'CHECK', themeKey TEXT NOT NULL DEFAULT 'FOREST', quantityPerCount REAL, measurementUnit TEXT)")
                db.execSQL("CREATE TABLE habit_completions (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, habitId INTEGER NOT NULL, date TEXT NOT NULL, completed INTEGER NOT NULL, FOREIGN KEY(habitId) REFERENCES habits(id) ON UPDATE NO ACTION ON DELETE CASCADE)")
                db.execSQL("CREATE INDEX index_habit_completions_habitId ON habit_completions(habitId)"); db.execSQL("CREATE UNIQUE INDEX index_habit_completions_habitId_date ON habit_completions(habitId,date)")
                db.execSQL("CREATE TABLE prayer_reasons (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, name TEXT NOT NULL, isBuiltIn INTEGER NOT NULL, createdAt INTEGER NOT NULL)"); db.execSQL("CREATE UNIQUE INDEX index_prayer_reasons_name ON prayer_reasons(name)")
                db.execSQL("CREATE TABLE prayer_records (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, prayer TEXT NOT NULL, date TEXT NOT NULL, status TEXT NOT NULL, inJamaah INTEGER NOT NULL, reasonId INTEGER, updatedAt INTEGER NOT NULL, xpAwarded INTEGER NOT NULL, FOREIGN KEY(reasonId) REFERENCES prayer_reasons(id) ON UPDATE NO ACTION ON DELETE SET NULL)"); db.execSQL("CREATE UNIQUE INDEX index_prayer_records_date_prayer ON prayer_records(date,prayer)"); db.execSQL("CREATE INDEX index_prayer_records_reasonId ON prayer_records(reasonId)")
                db.execSQL("CREATE TABLE routine_progress (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, habitId INTEGER NOT NULL, date TEXT NOT NULL, value INTEGER NOT NULL, updatedAt INTEGER NOT NULL, FOREIGN KEY(habitId) REFERENCES habits(id) ON UPDATE NO ACTION ON DELETE CASCADE)"); db.execSQL("CREATE INDEX index_routine_progress_habitId ON routine_progress(habitId)"); db.execSQL("CREATE UNIQUE INDEX index_routine_progress_habitId_date ON routine_progress(habitId,date)")
                db.execSQL("INSERT INTO habits (id,name,category,active,createdAt,sortOrder,isBuiltIn,routineType,target,unit,iconKey,themeKey,quantityPerCount,measurementUnit) VALUES (30,'Water','PERSONAL',1,0,1000,0,'COUNT',4,'bottles','WATER','OCEAN',500.0,'mL')")
                db.execSQL("INSERT INTO routine_progress (id,habitId,date,value,updatedAt) VALUES (2,30,'2026-10-04',2,0)")
            }
            override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
        }).build())
        helper.writableDatabase.close()
        val database = Room.databaseBuilder(context, HabitDatabase::class.java, name).addMigrations(HabitDatabase.MIGRATION_4_5, HabitDatabase.MIGRATION_5_6, HabitDatabase.MIGRATION_6_7, HabitDatabase.MIGRATION_7_8, HabitDatabase.MIGRATION_8_9, HabitDatabase.MIGRATION_9_10).build()
        try {
            assertEquals("WATER", database.habitDao().observeAllHabits().first().single().iconKey)
            assertEquals(2, database.habitDao().progress(30, "2026-10-04")?.value)
            assertTrue(database.habitDao().schedules().isEmpty())
            assertNull(database.sleepDao().plan())
            assertNull(database.sleepDao().activeSession())
        } finally { database.close(); context.deleteDatabase(name) }
    }
}
