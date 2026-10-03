package com.example.habittracker

import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.platform.app.InstrumentationRegistry
import com.example.habittracker.data.local.HabitDatabase
import com.example.habittracker.data.local.entity.Prayer
import com.example.habittracker.data.local.entity.PrayerStatus
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class PrayerMigrationTest {
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
            .addMigrations(HabitDatabase.MIGRATION_1_2)
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
}
