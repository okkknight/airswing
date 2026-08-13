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
    private val rotationVector = manager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
    private var ax = 0f
    private var ay = 0f
    private var az = 9.81f
    private var running = false
    private val rotationMatrix = FloatArray(9)
    private var hasRotation = false
    private var hasWorldGravity = false
    private var worldGravityX = 0f
    private var worldGravityY = 0f
    private var worldGravityZ = 0f
    private var worldLinearAx = Float.NaN
    private var worldLinearAy = Float.NaN
    private var worldLinearAz = Float.NaN

    var frameCount = 0L
        private set
    var lastTimestampNs = 0L
        private set
    var sampleIntervalMs = 0f
        private set
    val supported: Boolean get() = accelerometer != null && gyroscope != null
    val isRunning: Boolean get() = running
    val rotationVectorAvailable: Boolean get() = rotationVector != null
    val usingRotationVector: Boolean get() = hasRotation

    fun start(): SensorStartResult {
        if (!supported) return SensorStartResult.UNSUPPORTED
        if (running) return SensorStartResult.ALREADY_RUNNING
        val accRegistered = manager.registerListener(this, accelerometer, 5_000)
        val gyroRegistered = manager.registerListener(this, gyroscope, 5_000)
        rotationVector?.let { manager.registerListener(this, it, 5_000) }
        if (!accRegistered || !gyroRegistered) {
            manager.unregisterListener(this)
            running = false
            return SensorStartResult.REGISTRATION_FAILED
        }
        lastTimestampNs = 0L
        sampleIntervalMs = 0f
        hasRotation = false
        hasWorldGravity = false
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
                updateWorldLinearAcceleration()
                onFrame(
                    SensorFrame(
                        timestampNs = event.timestamp,
                        ax = ax,
                        ay = ay,
                        az = az,
                        gx = event.values[0],
                        gy = event.values[1],
                        gz = event.values[2],
                        worldLinearAx = worldLinearAx,
                        worldLinearAy = worldLinearAy,
                        worldLinearAz = worldLinearAz,
                    ),
                )
            }
            Sensor.TYPE_ROTATION_VECTOR -> {
                SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                hasRotation = true
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    private fun updateWorldLinearAcceleration() {
        if (!hasRotation) {
            worldLinearAx = Float.NaN
            worldLinearAy = Float.NaN
            worldLinearAz = Float.NaN
            return
        }
        val worldX = rotationMatrix[0] * ax + rotationMatrix[1] * ay + rotationMatrix[2] * az
        val worldY = rotationMatrix[3] * ax + rotationMatrix[4] * ay + rotationMatrix[5] * az
        val worldZ = rotationMatrix[6] * ax + rotationMatrix[7] * ay + rotationMatrix[8] * az
        if (!hasWorldGravity) {
            hasWorldGravity = true
            worldGravityX = worldX
            worldGravityY = worldY
            worldGravityZ = worldZ
        } else {
            worldGravityX += .01f * (worldX - worldGravityX)
            worldGravityY += .01f * (worldY - worldGravityY)
            worldGravityZ += .01f * (worldZ - worldGravityZ)
        }
        worldLinearAx = worldX - worldGravityX
        worldLinearAy = worldY - worldGravityY
        worldLinearAz = worldZ - worldGravityZ
    }
}
