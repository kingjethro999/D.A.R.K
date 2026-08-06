package com.dark.launcher.data.repo

import com.dark.launcher.data.db.FitnessDao
import com.dark.launcher.data.db.WorkoutLog
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

data class FitnessSummary(
    val workouts: Int = 0,
    val sprintAvg: String? = null,
    val stepsToday: Int = 0
)

@Singleton
class FitnessRepository @Inject constructor(
    private val dao: FitnessDao,
    private val steps: StepSensorRepository
) {
    private val weekMillis: Long = 7L * 24 * 60 * 60 * 1000
    private val refreshSignal = MutableStateFlow(0L)

    fun refresh() {
        refreshSignal.value = System.currentTimeMillis()
    }

    fun weeklySummary(): Flow<FitnessSummary> = combine(
        flow { while (true) { emit(Unit); kotlinx.coroutines.delay(60_000) } },
        refreshSignal,
        steps.todaySteps
    ) { _, _, _ -> Unit }.flatMapLatest { flow { emit(buildSummary()) } }

    suspend fun buildSummary(): FitnessSummary {
        val since = System.currentTimeMillis() - weekMillis
        val logs = dao.logsSince(since)
        val count = logs.size
        val sprints = logs.filter { it.type == "sprint" }
            .mapNotNull { parseSeconds(it.value2) }
        val avg = if (sprints.isNotEmpty()) {
            String.format("%.1fs", sprints.average())
        } else {
            null
        }
        return FitnessSummary(
            workouts = count,
            sprintAvg = avg,
            stepsToday = steps.todaySteps.value
        )
    }

    suspend fun log(type: String, value1: String, value2: String) {
        dao.insertLog(WorkoutLog(type = type, value1 = value1, value2 = value2))
    }

    private fun parseSeconds(raw: String): Double? =
        raw.trim().removeSuffix("s").toDoubleOrNull()
}
