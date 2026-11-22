package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [GripSegmentEntity::class, GripPatternSampleEntity::class],
    version = 2,
    exportSchema = false
)
abstract class GripDatabase : RoomDatabase() {
    abstract fun gripDao(): GripDao

    companion object {
        @Volatile
        private var INSTANCE: GripDatabase? = null

        fun getInstance(context: Context): GripDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    GripDatabase::class.java,
                    "grippulse_telemetry.db"
                )
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
