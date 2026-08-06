package com.dark.launcher.data.repo

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private val Context.stepDataStore by preferencesDataStore(name = "dark_steps")

@Singleton
class StepSensorRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    private val SENSOR_VALUE = longPreferencesKey("sensor_value")
    private val BASELINE = longPreferencesKey("baseline")
    private val DAY = stringPreferencesKey("day")

    private val _todaySteps = MutableStateFlow(0)
    val todaySteps: StateFlow<Int> = _todaySteps.asStateFlow()

    val sensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)

    var running: Boolean = false
        private set

    private val listener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            val value = event.values[0].toLong()
            scope.launch {
                val today = LocalDate.now().toString()
                val prefs = context.stepDataStore.data.first()
                val sensorValue = prefs[SENSOR_VALUE] ?: 0L
                val baseline = prefs[BASELINE] ?: 0L
                val newBaseline = when {
                    prefs[DAY] != today -> value
                    value < sensorValue -> value
                    else -> baseline
                }
                context.stepDataStore.edit { p ->
                    p[SENSOR_VALUE] = value
                    p[BASELINE] = newBaseline
                    p[DAY] = today
                }
                _todaySteps.value = (value - newBaseline).coerceAtLeast(0).toInt()
            }
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
    }

    init {
        scope.launch {
            val prefs = context.stepDataStore.data.first()
            if (prefs[DAY] == LocalDate.now().toString()) {
                val value = prefs[SENSOR_VALUE] ?: 0L
                val baseline = prefs[BASELINE] ?: 0L
                _todaySteps.value = (value - baseline).coerceAtLeast(0).toInt()
            }
        }
    }

    fun start() {
        val s = sensor ?: run {
            _todaySteps.value = 0
            return
        }
        val ok = runCatching {
            sensorManager.registerListener(listener, s, SensorManager.SENSOR_DELAY_NORMAL)
        }.getOrDefault(false)
        running = ok
    }

    fun stop() {
        if (!running) return
        running = false
        runCatching { sensorManager.unregisterListener(listener) }
    }
}
