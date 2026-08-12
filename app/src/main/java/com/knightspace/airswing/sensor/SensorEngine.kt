package com.knightspace.airswing.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager

class SensorEngine(context: Context, private val onFrame: (SensorFrame) -> Unit) : SensorEventListener {
    private val manager = context.getSystemService(SensorManager::class.java); private val acc = manager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER); private val gyro = manager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
    private var ax = 0f; private var ay = 0f; private var az = 9.81f; private var running = false; var frameCount = 0L; private set; var lastTimestampNs = 0L; private set; var sampleIntervalMs = 0f; private set
    val supported get() = acc != null && gyro != null
    fun start() { if (supported && !running) { manager.registerListener(this, acc, 5_000); manager.registerListener(this, gyro, 5_000); running = true } }
    fun stop() { if (running) { manager.unregisterListener(this); running = false } }
    override fun onSensorChanged(event: SensorEvent) { if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) { ax = event.values[0]; ay = event.values[1]; az = event.values[2] } else if (event.sensor.type == Sensor.TYPE_GYROSCOPE) { if (lastTimestampNs != 0L) sampleIntervalMs = (event.timestamp - lastTimestampNs) / 1_000_000f; lastTimestampNs = event.timestamp; frameCount++; onFrame(SensorFrame(event.timestamp, ax, ay, az, event.values[0], event.values[1], event.values[2])) } }
    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
}
