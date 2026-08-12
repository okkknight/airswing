package com.knightspace.airswing.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager

enum class SensorStartResult { STARTED, ALREADY_RUNNING, UNSUPPORTED, REGISTRATION_FAILED }

class SensorEngine(
    context: Context,
    private val onFrame: (SensorFrame) -> Unit,
) : SensorEventListener {
    private val manager = context.getSystemService(SensorManager::class.java)
    private val accelerometer = manager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val gyroscope = manager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
    private var ax = 0f
    private var ay = 0f
    private var az = 9.81f
    private var running = false

    var frameCount = 0L
        private set
    var lastTimestampNs = 0L
        private set
    var sampleIntervalMs = 0f
        private set
    val supported: Boolean get() = accelerometer != null && gyroscope != null
    val isRunning: Boolean get() = running

    fun start(): SensorStartResult {
        if (!supported) return SensorStartResult.UNSUPPORTED
        if (running) return SensorStartResult.ALREADY_RUNNING
        val accRegistered = manager.registerListener(this, accelerometer, 5_000)
        val gyroRegistered = manager.registerListener(this, gyroscope, 5_000)
        if (!accRegistered || !gyroRegistered) {
            manager.unregisterListener(this)
            running = false
            return SensorStartResult.REGISTRATION_FAILED
        }
        lastTimestampNs = 0L
        sampleIntervalMs = 0f
        running = true
        return SensorStartResult.STARTED
    }

    fun stop() {
        if (!running) return
        manager.unregisterListener(this)
        running = false
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (!running) return
        when (event.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> {
                ax = event.values[0]
                ay = event.values[1]
                az = event.values[2]
            }
            Sensor.TYPE_GYROSCOPE -> {
                if (lastTimestampNs != 0L) {
                    sampleIntervalMs = (event.timestamp - lastTimestampNs) / 1_000_000f
                }
                lastTimestampNs = event.timestamp
                frameCount++
                onFrame(
                    SensorFrame(
                        timestampNs = event.timestamp,
                        ax = ax,
                        ay = ay,
                        az = az,
                        gx = event.values[0],
                        gy = event.values[1],
                        gz = event.values[2],
                    ),
                )
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
}
