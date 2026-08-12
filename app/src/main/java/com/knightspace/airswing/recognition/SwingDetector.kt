package com.knightspace.airswing.recognition

import com.knightspace.airswing.sensor.SensorFrame
import kotlin.math.abs

enum class SwingState { IDLE, ARMED, ACCELERATING, PEAK_CANDIDATE, FOLLOW_THROUGH, COOLDOWN }
data class SwingUpdate(val state: SwingState, val candidate: Boolean, val gyroActivity: Float, val accActivity: Float, val baselineGyro: Float)

class SwingDetector(private val config: RecognitionConfig) {
    private var state = SwingState.IDLE; private var baseline = .15f; private var lastGyro = 0f; private var candidateStarted = 0L; private var cooldownAt = Long.MIN_VALUE
    fun reset() { state = SwingState.IDLE; baseline = .15f; lastGyro = 0f; candidateStarted = 0; cooldownAt = Long.MIN_VALUE }
    fun process(frame: SensorFrame): SwingUpdate {
        val gyro = frame.gyroMagnitude; val dynamicAcc = abs(frame.accMagnitude - 9.81f); val rise = gyro - lastGyro; lastGyro = gyro
        if (gyro < config.stationaryGyro) baseline = baseline * .96f + gyro * .04f
        val score = gyro / baseline.coerceAtLeast(.1f)
        if (state == SwingState.COOLDOWN) { if (frame.timestampMs - cooldownAt >= config.cooldownMs && gyro < config.rearmGyro) state = SwingState.ARMED; return update(false, gyro, dynamicAcc) }
        when (state) { SwingState.IDLE -> if (gyro < config.stationaryGyro) state = SwingState.ARMED
            SwingState.ARMED -> if (score >= config.minSwingRatio && rise >= config.minGyroRise) { state = SwingState.ACCELERATING; candidateStarted = frame.timestampMs }
            SwingState.ACCELERATING -> if (frame.timestampMs - candidateStarted >= config.minCandidateMs) state = SwingState.PEAK_CANDIDATE
            SwingState.PEAK_CANDIDATE -> { state = SwingState.FOLLOW_THROUGH; return update(true, gyro, dynamicAcc) }
            SwingState.FOLLOW_THROUGH -> if (gyro < config.rearmGyro) { state = SwingState.COOLDOWN; cooldownAt = frame.timestampMs }
            else -> Unit }
        return update(false, gyro, dynamicAcc)
    }
    fun beginCooldown(timestampMs: Long) { state = SwingState.COOLDOWN; cooldownAt = timestampMs }
    private fun update(candidate: Boolean, gyro: Float, acc: Float) = SwingUpdate(state, candidate, gyro, acc, baseline)
}
