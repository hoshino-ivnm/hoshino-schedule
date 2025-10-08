package com.misaka.hoshinoschedule.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [CourseEntity::class, CourseTimeEntity::class, PeriodDefinitionEntity::class],
    version = 2,
    exportSchema = true
)
abstract class KiraraDatabase : RoomDatabase() {
    abstract fun courseDao(): CourseDao
    abstract fun periodDao(): PeriodDao

    companion object {
        @Volatile
        private var instance: KiraraDatabase? = null
        private val MIGRATION_1_2 = object : androidx.room.migration.Migration(1, 2) {
            override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE course_times ADD COLUMN weeks TEXT")
            }
        }

        fun get(context: Context): KiraraDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                KiraraDatabase::class.java,
                "kirara.db"
            ).addMigrations(MIGRATION_1_2).build().also { instance = it }
        }
    }
}
