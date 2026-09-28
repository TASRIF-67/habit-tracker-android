package com.example.habittracker.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.habittracker.data.local.dao.HabitDao
import com.example.habittracker.data.local.entity.Habit
import com.example.habittracker.data.local.entity.HabitCompletion

@Database(entities = [Habit::class, HabitCompletion::class], version = 1, exportSchema = false)
@TypeConverters(HabitConverters::class)
abstract class HabitDatabase : RoomDatabase() {
    abstract fun habitDao(): HabitDao

    companion object {
        @Volatile private var instance: HabitDatabase? = null
        fun getInstance(context: Context): HabitDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(context.applicationContext, HabitDatabase::class.java, "habits.db")
                .addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        db.execSQL("INSERT INTO habits (name, category, active, createdAt, sortOrder, isBuiltIn) VALUES ('Fajr','SALAT',1,0,10,1),('Dhuhr','SALAT',1,0,20,1),('Asr','SALAT',1,0,30,1),('Maghrib','SALAT',1,0,40,1),('Isha','SALAT',1,0,50,1),('Quran','GOOD_DEED',1,0,110,1),('Morning Adhkar','GOOD_DEED',1,0,120,1),('Evening Adhkar','GOOD_DEED',1,0,130,1),('Sadaqah','GOOD_DEED',1,0,140,1)")
                    }
                }).build().also { instance = it }
        }
    }
}
