package com.dark.launcher.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface FitnessDao {

    @Insert
    suspend fun insertLog(log: WorkoutLog)

    @Query("SELECT * FROM workout_log WHERE timestamp >= :since ORDER BY timestamp DESC")
    suspend fun logsSince(since: Long): List<WorkoutLog>

    @Query("SELECT COUNT(*) FROM workout_log WHERE timestamp >= :since")
    suspend fun countSince(since: Long): Int
}
