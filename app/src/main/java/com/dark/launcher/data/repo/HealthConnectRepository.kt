package com.dark.launcher.data.repo

import android.content.Context
import androidx.activity.result.contract.ActivityResultContract
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Instant
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Singleton
class HealthConnectRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val client: HealthConnectClient? by lazy {
        val result = runCatching { HealthConnectClient.getOrCreate(context) }
        if (result.isFailure) {
            android.util.Log.e("DARK-HC", "getOrCreate failed: ${result.exceptionOrNull()}")
        }
        result.getOrNull()
    }

    fun available(): Boolean = client != null &&
        HealthConnectClient.getSdkStatus(context) == HealthConnectClient.SDK_AVAILABLE

    fun stepsPermission(): String = HealthPermission.getReadPermission(StepsRecord::class)

    fun permissionContract(): ActivityResultContract<Set<String>, Set<String>> =
        androidx.health.connect.client.PermissionController.createRequestPermissionResultContract()

    fun healthDataAppIntent(): android.content.Intent? = runCatching {
        android.content.Intent("android.health.connect.action.MANAGE_HEALTH_DATA")
            .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
    }.getOrNull()

    suspend fun hasStepsPermission(): Boolean {
        val c = client ?: return false
        return runCatching {
            c.permissionController.getGrantedPermissions().contains(stepsPermission())
        }.getOrDefault(false)
    }

    suspend fun readTodaySteps(): Int = withContext(Dispatchers.IO) {
        val c = client ?: return@withContext 0
        if (!hasStepsPermission()) return@withContext 0
        runCatching {
            val zone = ZoneId.systemDefault()
            val now = Instant.now()
            val startOfDay = now.atZone(zone).toLocalDate().atStartOfDay(zone).toInstant()
            val response = c.readRecords(
                ReadRecordsRequest(
                    recordType = StepsRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(startOfDay, now)
                )
            )
            response.records.sumOf { it.count }.toInt()
        }.getOrDefault(0)
    }
}
