package com.knightspace.airswing.recognition

import com.knightspace.airswing.sensor.MotionSample

enum class SwingState { IDLE, ARMED, ACCELERATING, PEAK_CANDIDATE, FOLLOW_THROUGH, COOLDOWN }

data class SwingUpdate(
    val state: SwingState,
    val candidate: Boolean,
    val gyroActivity: Float,
    val accActivity: Float,
    val baselineGyro: Float,
    val swingScore: Float = 0f,
)

class SwingDetector(private val config: RecognitionConfig) {
    private var state = SwingState.IDLE
    private var baselineGyro = .15f
    private var acceleratingAtMs = 0L
    private var cooldownAtMs = Long.MIN_VALUE
    private var cooldownActivityFell = false

    fun process(sample: MotionSample): SwingUpdate {
        if (sample.deltaReset) {
            state = SwingState.IDLE
            acceleratingAtMs = 0L
        }
        val stable = sample.gyroActivity < config.stationaryGyro && sample.accActivity < config.stationaryAcc
        if (stable) {
            baselineGyro += config.baselineAlpha * (sample.gyroActivity - baselineGyro)
        }
        val score = sample.gyroActivity / baselineGyro.coerceAtLeast(config.minimumBaseline)

        if (state == SwingState.COOLDOWN) {
            val timeReady = sample.timestampMs - cooldownAtMs >= config.cooldownMs
            if (sample.gyroActivity < config.rearmGyro) cooldownActivityFell = true
            val newRise = sample.accRisePerSecond >= config.rearmAccRisePerSecond
            if (timeReady && cooldownActivityFell && newRise) state = SwingState.ARMED
            return update(sample, score, false)
        }

        when (state) {
            SwingState.IDLE -> if (stable) state = SwingState.ARMED
            SwingState.ARMED -> if (
                score >= config.minSwingRatio &&
                sample.gyroRisePerSecond >= config.minGyroRisePerSecond &&
                sample.accActivity >= config.minSwingAcc
            ) {
                state = SwingState.ACCELERATING
                acceleratingAtMs = sample.timestampMs
            }
            SwingState.ACCELERATING -> {
                if (sample.gyroActivity < config.stationaryGyro) {
                    state = SwingState.ARMED
                } else if (sample.timestampMs - acceleratingAtMs >= config.minCandidateMs) {
                    state = SwingState.PEAK_CANDIDATE
                    return update(sample, score, true)
                }
            }
            SwingState.PEAK_CANDIDATE -> state = SwingState.FOLLOW_THROUGH
            SwingState.FOLLOW_THROUGH -> if (sample.gyroActivity < config.rearmGyro) state = SwingState.ARMED
            else -> Unit
        }
        return update(sample, score, false)
    }

    fun beginCooldown(timestampMs: Long) {
        state = SwingState.COOLDOWN
        cooldownAtMs = timestampMs
        cooldownActivityFell = false
    }

    fun reset() {
        state = SwingState.IDLE
        baselineGyro = .15f
        acceleratingAtMs = 0L
        cooldownAtMs = Long.MIN_VALUE
        cooldownActivityFell = false
    }

    private fun update(sample: MotionSample, score: Float, candidate: Boolean) = SwingUpdate(
        state = state,
        candidate = candidate,
        gyroActivity = sample.gyroActivity,
        accActivity = sample.accActivity,
        baselineGyro = baselineGyro,
        swingScore = score,
    )
}
