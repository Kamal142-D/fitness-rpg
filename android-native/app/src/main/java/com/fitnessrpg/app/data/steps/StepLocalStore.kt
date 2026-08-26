package com.fitnessrpg.app.data.steps

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.fitnessrpg.app.domain.steps.DEFAULT_DAILY_STEP_GOAL
import com.fitnessrpg.app.domain.steps.SensorAccumulator
import com.fitnessrpg.app.domain.steps.accumulateSensorSample
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.LocalDate

private val Context.dailyMarchDataStore by preferencesDataStore(name = "daily_march")

data class LocalStepSnapshot(
    val date: LocalDate,
    val sensorSteps: Int,
    val goal: Int,
    val lastBootStepCount: Long?,
)

class StepLocalStore(context: Context) {
    private val store = context.applicationContext.dailyMarchDataStore

    private object Keys {
        val date = stringPreferencesKey("date")
        val sensorSteps = intPreferencesKey("sensor_steps")
        val goal = intPreferencesKey("goal")
        val lastBootStepCount = longPreferencesKey("last_boot_step_count")
    }

    fun observe(today: LocalDate = LocalDate.now()): Flow<LocalStepSnapshot> = store.data.map { preferences ->
        preferences.toSnapshot(today)
    }

    suspend fun read(today: LocalDate = LocalDate.now()): LocalStepSnapshot = store.data.first().toSnapshot(today)

    suspend fun recordSensorSample(bootStepCount: Long, today: LocalDate = LocalDate.now()): LocalStepSnapshot {
        var result: LocalStepSnapshot? = null
        store.edit { preferences ->
            val current = preferences.toSnapshot(today)
            val next = accumulateSensorSample(
                SensorAccumulator(current.date, current.lastBootStepCount, current.sensorSteps),
                bootStepCount,
                today,
            )
            preferences[Keys.date] = next.date.toString()
            preferences[Keys.sensorSteps] = next.accumulatedSteps
            preferences[Keys.lastBootStepCount] = next.lastBootStepCount ?: bootStepCount
            result = LocalStepSnapshot(next.date, next.accumulatedSteps, current.goal, next.lastBootStepCount)
        }
        return checkNotNull(result)
    }

    suspend fun setGoal(goal: Int) {
        store.edit { it[Keys.goal] = goal.coerceIn(1_000, 50_000) }
    }

    private fun Preferences.toSnapshot(today: LocalDate): LocalStepSnapshot {
        val storedDate = this[Keys.date]?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
        val date = storedDate ?: today
        val sameDay = date == today
        return LocalStepSnapshot(
            date = if (sameDay) date else today,
            sensorSteps = if (sameDay) this[Keys.sensorSteps] ?: 0 else 0,
            goal = (this[Keys.goal] ?: DEFAULT_DAILY_STEP_GOAL).coerceIn(1_000, 50_000),
            lastBootStepCount = if (sameDay) this[Keys.lastBootStepCount] else null,
        )
    }
}
