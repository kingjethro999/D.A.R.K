package com.dark.launcher.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [WorkoutLog::class], version = 1, exportSchema = false)
abstract class DarkDatabase : RoomDatabase() {
    abstract fun fitnessDao(): FitnessDao
}
