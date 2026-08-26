package com.fitnessrpg.app.data.steps

import android.annotation.SuppressLint
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.time.TimeRangeFilter
import com.fitnessrpg.app.data.dto.ClaimDailyStepsParams
import com.fitnessrpg.app.data.dto.DailyStepDto
import com.fitnessrpg.app.data.dto.SyncDailyStepsParams
import com.fitnessrpg.app.data.remote.SupabaseProvider
import com.fitnessrpg.app.data.remote.toJsonObject
import com.fitnessrpg.app.domain.steps.DailyStepProgress
import com.fitnessrpg.app.domain.steps.completedStepStreak
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.ZoneId

enum class HealthStepAvailability { AVAILABLE, UPDATE_REQUIRED, UNAVAILABLE }

data class DailyMarchOverview(
    val today: DailyStepProgress,
    val history: List<DailyStepProgress>,
    val streak: Int,
)

class StepRepository(context: Context) {
    private val appContext = context.applicationContext
    private val localStore = StepLocalStore(appContext)
    private val sensorManager = appContext.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val db get() = SupabaseProvider.client

    val healthReadPermission: String = HealthPermission.getReadPermission(StepsRecord::class)

    fun healthAvailability(): HealthStepAvailability = when (HealthConnectClient.getSdkStatus(appContext)) {
        HealthConnectClient.SDK_AVAILABLE -> HealthStepAvailability.AVAILABLE
        HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED -> HealthStepAvailability.UPDATE_REQUIRED
        else -> HealthStepAvailability.UNAVAILABLE
    }

    suspend fun hasHealthPermission(): Boolean {
        if (healthAvailability() != HealthStepAvailability.AVAILABLE) return false
        return healthReadPermission in HealthConnectClient.getOrCreate(appContext)
            .permissionController.getGrantedPermissions()
    }

    suspend fun readTodayFromHealthConnect(
        today: LocalDate = LocalDate.now(),
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): Int {
        val client = HealthConnectClient.getOrCreate(appContext)
        val start = today.atStartOfDay(zoneId).toInstant()
        val end = java.time.Instant.now()
        if (!end.isAfter(start)) return 0
        val aggregate = client.aggregate(
            AggregateRequest(
                metrics = setOf(StepsRecord.COUNT_TOTAL),
                timeRangeFilter = TimeRangeFilter.between(start, end),
            ),
        )
        return (aggregate[StepsRecord.COUNT_TOTAL] ?: 0L).coerceIn(0L, 100_000L).toInt()
    }

    fun hasDeviceStepSensor(): Boolean = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER) != null

    fun observeStoredSensorSteps(today: LocalDate = LocalDate.now()): Flow<LocalStepSnapshot> = localStore.observe(today)

    suspend fun storedSensorSteps(today: LocalDate = LocalDate.now()): LocalStepSnapshot = localStore.read(today)

    suspend fun setGoal(goal: Int) = localStore.setGoal(goal)

    @SuppressLint("MissingPermission")
    fun observeLiveSensorSteps(todayProvider: () -> LocalDate = { LocalDate.now() }): Flow<Int> = callbackFlow {
        val sensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
        if (sensor == null) {
            close(IllegalStateException("Step counter sensor unavailable"))
            return@callbackFlow
        }
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                event.values.firstOrNull()?.toLong()?.let { trySend(it) }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }
        if (!sensorManager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_UI)) {
            close(IllegalStateException("Step counter sensor unavailable"))
            return@callbackFlow
        }
        awaitClose { sensorManager.unregisterListener(listener) }
    }.map { sample -> localStore.recordSensorSample(sample, todayProvider()).sensorSteps }
        .distinctUntilChanged()

    suspend fun syncAndLoad(
        date: LocalDate,
        steps: Int,
        goal: Int,
    ): DailyMarchOverview {
        val synced = db.postgrest.rpc(
            "sync_daily_steps",
            SyncDailyStepsParams(date.toString(), steps.coerceIn(0, 100_000), goal.coerceIn(1_000, 50_000)).toJsonObject(),
        ).decodeAs<DailyStepDto>().toDomain()
        val from = date.minusDays(29).toString()
        val history = db.from("daily_step_progress").select {
            filter { gte("step_date", from) }
            order("step_date", Order.DESCENDING)
        }.decodeList<DailyStepDto>().map { it.toDomain() }
        return DailyMarchOverview(synced, history, completedStepStreak(history, date))
    }

    suspend fun claimReward(date: LocalDate): DailyStepProgress = db.postgrest.rpc(
        "claim_daily_step_reward",
        ClaimDailyStepsParams(date.toString()).toJsonObject(),
    ).decodeAs<DailyStepDto>().toDomain()
}
