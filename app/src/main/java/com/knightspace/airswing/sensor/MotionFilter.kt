package com.knightspace.airswing.sensor

import com.knightspace.airswing.recognition.RecognitionConfig
import kotlin.math.abs
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sqrt

data class MotionSample(
    val timestampNs: Long,
    val gyroActivity: Float,
    val accActivity: Float,
    val gyroRisePerSecond: Float,
    val accRisePerSecond: Float,
    val screenNormalRotationRatio: Float = 0f,
    val deltaReset: Boolean,
) {
    val timestampMs: Long get() = timestampNs / 1_000_000
}

/** Research low-pass preprocessing with phone-axis direction retained only for conservative rejection. */
class MotionFilter(private val config: RecognitionConfig) {
    private var initialized = false
    private var filteredGyro = 0f
    private var filteredGx = 0f
    private var filteredGy = 0f
    private var filteredGz = 0f
    private var filteredAcc = 9.81f
    private var previousGyro = 0f
    private var previousDynamicAcc = 0f
    private var previousTimestampNs = 0L

    fun process(frame: SensorFrame): MotionSample {
        if (!initialized) {
            initialized = true
            filteredGyro = frame.gyroMagnitude
            filteredGx = frame.gx
            filteredGy = frame.gy
            filteredGz = frame.gz
            filteredAcc = frame.accMagnitude
            previousGyro = filteredGyro
            previousDynamicAcc = abs(filteredAcc - 9.81f)
            previousTimestampNs = frame.timestampNs
            return sample(frame.timestampNs, previousGyro, previousDynamicAcc, 0f, 0f, false)
        }

        val deltaNs = frame.timestampNs - previousTimestampNs
        val validDelta = deltaNs > 0 && deltaNs <= config.maxFrameDeltaMs * 1_000_000
        val seconds = if (validDelta) deltaNs / 1_000_000_000f else 1f
        if (!validDelta) {
            filteredGyro = frame.gyroMagnitude
            filteredGx = frame.gx
            filteredGy = frame.gy
            filteredGz = frame.gz
            filteredAcc = frame.accMagnitude
            previousGyro = filteredGyro
            previousDynamicAcc = abs(filteredAcc - 9.81f)
            previousTimestampNs = frame.timestampNs
            return sample(
                frame.timestampNs,
                previousGyro,
                previousDynamicAcc,
                0f,
                0f,
                true,
                screenNormalRotationRatio(),
            )
        }
        val alpha = when {
            config.lowPassCutoffHz.isInfinite() -> 1f
            config.lowPassCutoffHz <= 0f -> 0f
            else -> (1.0 - exp(-2.0 * PI * config.lowPassCutoffHz * seconds)).toFloat()
        }
        filteredGyro += alpha * (frame.gyroMagnitude - filteredGyro)
        filteredGx += alpha * (frame.gx - filteredGx)
        filteredGy += alpha * (frame.gy - filteredGy)
        filteredGz += alpha * (frame.gz - filteredGz)
        filteredAcc += alpha * (frame.accMagnitude - filteredAcc)
        val dynamicAcc = abs(filteredAcc - 9.81f)
        val gyroRise = (filteredGyro - previousGyro) / seconds
        val accRise = (dynamicAcc - previousDynamicAcc) / seconds

        previousGyro = filteredGyro
        previousDynamicAcc = dynamicAcc
        previousTimestampNs = frame.timestampNs
        return sample(
            frame.timestampNs,
            filteredGyro,
            dynamicAcc,
            gyroRise,
            accRise,
            !validDelta,
            screenNormalRotationRatio(),
        )
    }

    fun reset() {
        initialized = false
        filteredGyro = 0f
        filteredGx = 0f
        filteredGy = 0f
        filteredGz = 0f
        filteredAcc = 9.81f
        previousGyro = 0f
        previousDynamicAcc = 0f
        previousTimestampNs = 0L
    }

    private fun sample(
        timestampNs: Long,
        gyro: Float,
        acc: Float,
        gyroRise: Float,
        accRise: Float,
        reset: Boolean,
        screenNormalRatio: Float = 0f,
    ) =
        MotionSample(
            timestampNs = timestampNs,
            gyroActivity = gyro,
            accActivity = acc,
            gyroRisePerSecond = gyroRise,
            accRisePerSecond = accRise,
            screenNormalRotationRatio = screenNormalRatio,
            deltaReset = reset,
        )

    private fun screenNormalRotationRatio(): Float {
        val magnitude = sqrt(
            filteredGx * filteredGx + filteredGy * filteredGy + filteredGz * filteredGz,
        )
        return if (magnitude > 0f) abs(filteredGz) / magnitude else 0f
    }
}
