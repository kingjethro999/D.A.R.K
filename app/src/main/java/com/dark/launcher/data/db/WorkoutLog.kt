package com.dark.launcher.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "workout_log")
data class WorkoutLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String,
    val value1: String,
    val value2: String,
    val note: String = "",
    val timestamp: Long = System.currentTimeMillis()
)
