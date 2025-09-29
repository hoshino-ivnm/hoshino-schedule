package com.misaka.kiraraschedule.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [CourseEntity::class, CourseTimeEntity::class, PeriodDefinitionEntity::class],
    version = 1,
    exportSchema = true
)
abstract class KiraraDatabase : RoomDatabase() {
    abstract fun courseDao(): CourseDao
    abstract fun periodDao(): PeriodDao

    companion object {
        @Volatile
        private var instance: KiraraDatabase? = null

        fun get(context: Context): KiraraDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                KiraraDatabase::class.java,
                "kirara.db"
            ).build().also { instance = it }
        }
    }
}
