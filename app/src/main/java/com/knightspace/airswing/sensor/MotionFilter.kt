package com.knightspace.airswing.sensor

import com.knightspace.airswing.recognition.RecognitionConfig
import kotlin.math.abs

data class MotionSample(
    val timestampNs: Long,
    val gyroActivity: Float,
    val accActivity: Float,
    val gyroRisePerSecond: Float,
    val accRisePerSecond: Float,
    val deltaReset: Boolean,
) {
    val timestampMs: Long get() = timestampNs / 1_000_000
}

/** Magnitude-based phone adaptation of the research low-pass preprocessing. */
class MotionFilter(private val config: RecognitionConfig) {
    private var initialized = false
    private var filteredGyro = 0f
    private var filteredAcc = 9.81f
    private var previousGyro = 0f
    private var previousDynamicAcc = 0f
    private var previousTimestampNs = 0L

    fun process(frame: SensorFrame): MotionSample {
        if (!initialized) {
            initialized = true
            filteredGyro = frame.gyroMagnitude
            filteredAcc = frame.accMagnitude
            previousGyro = filteredGyro
            previousDynamicAcc = abs(filteredAcc - 9.81f)
            previousTimestampNs = frame.timestampNs
            return sample(frame.timestampNs, previousGyro, previousDynamicAcc, 0f, 0f, false)
        }

        val alpha = config.lowPassAlpha.coerceIn(0f, 1f)
        filteredGyro += alpha * (frame.gyroMagnitude - filteredGyro)
        filteredAcc += alpha * (frame.accMagnitude - filteredAcc)
        val dynamicAcc = abs(filteredAcc - 9.81f)
        val deltaNs = frame.timestampNs - previousTimestampNs
        val validDelta = deltaNs > 0 && deltaNs <= config.maxFrameDeltaMs * 1_000_000
        val seconds = if (validDelta) deltaNs / 1_000_000_000f else 1f
        val gyroRise = if (validDelta) (filteredGyro - previousGyro) / seconds else 0f
        val accRise = if (validDelta) (dynamicAcc - previousDynamicAcc) / seconds else 0f

        previousGyro = filteredGyro
        previousDynamicAcc = dynamicAcc
        previousTimestampNs = frame.timestampNs
        return sample(frame.timestampNs, filteredGyro, dynamicAcc, gyroRise, accRise, !validDelta)
    }

    fun reset() {
        initialized = false
        filteredGyro = 0f
        filteredAcc = 9.81f
        previousGyro = 0f
        previousDynamicAcc = 0f
        previousTimestampNs = 0L
    }

    private fun sample(timestampNs: Long, gyro: Float, acc: Float, gyroRise: Float, accRise: Float, reset: Boolean) =
        MotionSample(timestampNs, gyro, acc, gyroRise, accRise, reset)
}
