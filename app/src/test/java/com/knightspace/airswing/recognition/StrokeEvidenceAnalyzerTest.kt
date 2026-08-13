package com.knightspace.airswing.recognition

import com.knightspace.airswing.sensor.MotionSample
import org.junit.Test
import kotlin.test.assertEquals

class StrokeEvidenceAnalyzerTest {
    private val config = RecognitionConfig(
        strokeEvidencePostMs = 80,
        swingImpactWindowMs = 80,
        minTranslationCoupling = 2f,
        maxPeakCooccurrenceMs = 60,
    )

    @Test
    fun `pure wrist rotation is rejected even when gyro is high`() {
        val analyzer = StrokeEvidenceAnalyzer(config)
        analyzer.process(sample(0, gyro = 1f, linearAcc = 0f), noCandidate())
        analyzer.process(sample(50, gyro = 18f, linearAcc = 8f), candidate(0))

        val decision = analyzer.process(sample(130, gyro = 6f, linearAcc = 3f), followThrough(0))

        assertEquals(StrokeDecision.REJECTED, decision)
    }

    @Test
    fun `weak complete swing is deferred confirmed within eighty milliseconds`() {
        val analyzer = StrokeEvidenceAnalyzer(config)
        analyzer.process(sample(0, gyro = 1f, linearAcc = 0f), noCandidate())
        analyzer.process(sample(50, gyro = 8f, linearAcc = 24f), candidate(0))

        val decision = analyzer.process(sample(80, gyro = 4f, linearAcc = 8f), followThrough(0))

        assertEquals(StrokeDecision.DEFERRED_CONFIRMED, decision)
    }

    @Test
    fun `strong coupled swing is fast confirmed without follow through wait`() {
        val analyzer = StrokeEvidenceAnalyzer(config)
        analyzer.process(sample(0, gyro = 1f, linearAcc = 0f), noCandidate())

        val decision = analyzer.process(sample(50, gyro = 30f, linearAcc = 160f), candidate(0))

        assertEquals(StrokeDecision.FAST_CONFIRMED, decision)
    }

    private fun sample(ms: Long, gyro: Float, linearAcc: Float) = MotionSample(
        timestampNs = ms * 1_000_000,
        gyroActivity = gyro,
        accActivity = linearAcc,
        gyroRisePerSecond = 0f,
        accRisePerSecond = 0f,
        deltaReset = false,
        linearAccMagnitude = linearAcc,
    )

    private fun candidate(startMs: Long) = SwingUpdate(
        SwingState.PEAK_CANDIDATE,
        candidate = true,
        gyroActivity = 8f,
        accActivity = 24f,
        baselineGyro = .2f,
        accelerationStartTimestampMs = startMs,
    )

    private fun followThrough(startMs: Long) = SwingUpdate(
        SwingState.FOLLOW_THROUGH,
        candidate = false,
        gyroActivity = 4f,
        accActivity = 8f,
        baselineGyro = .2f,
        accelerationStartTimestampMs = startMs,
    )

    private fun noCandidate() = SwingUpdate(SwingState.ARMED, false, 1f, 0f, .2f)
}
