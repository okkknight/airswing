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
    fun `cyclic wrist rotation is rejected despite gyro and linear acceleration peaks`() {
        val analyzer = StrokeEvidenceAnalyzer(config)
        analyzer.process(sample(0, gyro = 1f, linearAcc = 0f, x = 0f, y = 0f), noCandidate())
        analyzer.process(sample(20, gyro = 8f, linearAcc = 24f, x = 24f, y = 0f), noCandidate())
        analyzer.process(sample(35, gyro = 12f, linearAcc = 24f, x = 17f, y = 17f), noCandidate())
        analyzer.process(sample(50, gyro = 18f, linearAcc = 24f, x = 0f, y = 24f), candidate(20))
        analyzer.process(sample(65, gyro = 12f, linearAcc = 24f, x = -17f, y = 17f), followThrough(20))

        val decision = analyzer.process(sample(80, gyro = 6f, linearAcc = 8f, x = -8f, y = 0f), followThrough(20))

        assertEquals(StrokeDecision.REJECTED, decision)
    }

    @Test
    fun `weak complete swing is deferred confirmed within eighty milliseconds`() {
        val analyzer = StrokeEvidenceAnalyzer(config)
        analyzer.process(sample(0, gyro = 1f, linearAcc = 0f, x = 0f, y = 0f), noCandidate())
        analyzer.process(sample(20, gyro = 4f, linearAcc = 12f, x = 12f, y = 0f), noCandidate())
        analyzer.process(sample(50, gyro = 8f, linearAcc = 24f, x = 24f, y = 0f), candidate(20))

        val decision = analyzer.process(sample(80, gyro = 4f, linearAcc = 8f, x = 8f, y = 0f), followThrough(20))

        assertEquals(StrokeDecision.DEFERRED_CONFIRMED, decision)
    }

    @Test
    fun `strong coupled swing is fast confirmed without follow through wait`() {
        val analyzer = StrokeEvidenceAnalyzer(config)
        analyzer.process(sample(0, gyro = 1f, linearAcc = 0f, x = 0f, y = 0f), noCandidate())
        analyzer.process(sample(20, gyro = 15f, linearAcc = 80f, x = 80f, y = 0f), noCandidate())

        val decision = analyzer.process(sample(50, gyro = 30f, linearAcc = 160f, x = 160f, y = 0f), candidate(20))

        assertEquals(StrokeDecision.FAST_CONFIRMED, decision)
    }

    @Test
    fun `strong cyclic rotation cannot use the fast path`() {
        val analyzer = StrokeEvidenceAnalyzer(config)
        analyzer.process(sample(0, gyro = 20f, linearAcc = 100f, x = 100f, y = 0f), noCandidate())
        analyzer.process(sample(15, gyro = 22f, linearAcc = 110f, x = 95f, y = 55f), noCandidate())
        analyzer.process(sample(30, gyro = 24f, linearAcc = 120f, x = 60f, y = 104f), noCandidate())
        analyzer.process(sample(45, gyro = 26f, linearAcc = 130f, x = 0f, y = 130f), noCandidate())
        analyzer.process(sample(60, gyro = 28f, linearAcc = 140f, x = -70f, y = 121f), noCandidate())
        analyzer.process(sample(75, gyro = 30f, linearAcc = 150f, x = -130f, y = 75f), noCandidate())

        val decision = analyzer.process(sample(90, gyro = 32f, linearAcc = 170f, x = -170f, y = 0f), candidate(0))

        assertEquals(StrokeDecision.PENDING, decision)
    }

    private fun sample(ms: Long, gyro: Float, linearAcc: Float, x: Float, y: Float) = MotionSample(
        timestampNs = ms * 1_000_000,
        gyroActivity = gyro,
        accActivity = linearAcc,
        gyroRisePerSecond = 0f,
        accRisePerSecond = 0f,
        deltaReset = false,
        linearAccMagnitude = linearAcc,
        linearAccX = x,
        linearAccY = y,
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
