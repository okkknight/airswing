package com.knightspace.airswing.recognition

import com.knightspace.airswing.sensor.SensorFrame
import com.knightspace.airswing.sensor.MotionSample
import org.junit.Test
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RecognitionPipelineTest {
    @Test fun `a sustained rotational rise produces one swing candidate`() {
        val detector = SwingDetector(RecognitionConfig(minCandidateMs = 20, cooldownMs = 200))
        val updates = listOf(0L, 10L, 20L, 30L, 40L, 50L).map { time ->
            detector.process(sample(
                ms = time,
                gyro = if (time < 20) .1f else 8f,
                acc = if (time < 20) .1f else 2f,
                gyroRise = if (time == 20L) 790f else 0f,
            ))
        }
        assertTrue(updates.count { it.candidate } == 1)
    }
    @Test fun `candidate with acceleration prominence creates virtual impact`() {
        val detector = ImpactDetector(RecognitionConfig(minImpactScore = .1f))
        prime(detector)
        detector.process(sample(100, 9f, 15f), SwingUpdate(SwingState.PEAK_CANDIDATE, true, 9f, 15f, .2f))
        val event = detector.process(sample(110, 5f, 8f), SwingUpdate(SwingState.FOLLOW_THROUGH, false, 5f, 8f, .2f))
        assertNotNull(event)
        assertTrue(event.impactScore > 0f)
    }
    @Test fun `cooldown suppresses a second local peak`() {
        val detector = ImpactDetector(RecognitionConfig(minImpactScore = .1f, cooldownMs = 280))
        val swing = SwingUpdate(SwingState.PEAK_CANDIDATE, true, 9f, 15f, .2f)
        prime(detector)
        detector.process(sample(100, 9f, 15f), swing)
        assertNotNull(detector.process(sample(110, 5f, 8f), SwingUpdate(SwingState.FOLLOW_THROUGH, false, 5f, 8f, .2f)))
        detector.process(sample(180, 9f, 15f), swing)
        assertNull(detector.process(sample(190, 5f, 8f), SwingUpdate(SwingState.FOLLOW_THROUGH, false, 5f, 8f, .2f)))
    }
    @Test fun `impact offset is added to event timestamp`() {
        val detector = ImpactDetector(RecognitionConfig(minImpactScore = .1f, impactOffsetMs = 55))
        prime(detector)
        detector.process(sample(100, 9f, 15f), SwingUpdate(SwingState.PEAK_CANDIDATE, true, 9f, 15f, .2f))
        val event = detector.process(sample(110, 5f, 8f), SwingUpdate(SwingState.FOLLOW_THROUGH, false, 5f, 8f, .2f))
        assertNotNull(event)
        assertTrue(event.timestampNs == 155_000_000L)
    }
    @Test fun `strength mapping uses configured soft and hard boundaries`() {
        val config = RecognitionConfig(softStrength = 2f, hardStrength = 4f)

        assertEquals(ImpactStrength.SOFT, config.mapStrength(1.99f))
        assertEquals(ImpactStrength.MEDIUM, config.mapStrength(2f))
        assertEquals(ImpactStrength.MEDIUM, config.mapStrength(3.99f))
        assertEquals(ImpactStrength.HARD, config.mapStrength(4f))
    }
    @Test fun `impact requires acceleration and gyro prominence in the same candidate window`() {
        val detector = ImpactDetector(RecognitionConfig(
            minImpactScore = 1.2f,
            minGyroProminence = 1.5f,
            minAccProminence = 1.5f,
        ))
        val noCandidate = SwingUpdate(SwingState.ACCELERATING, false, 1f, 1f, .2f)
        repeat(5) { index -> detector.process(sample(index * 10L, gyro = 1f, acc = 1f), noCandidate) }

        val onlyGyro = detector.process(
            sample(60, gyro = 8f, acc = 1f),
            SwingUpdate(SwingState.PEAK_CANDIDATE, true, 8f, 1f, .2f),
        )

        assertNull(onlyGyro)
    }

    @Test fun `impact waits for both signals to fall from a local peak`() {
        val detector = ImpactDetector(RecognitionConfig(
            minGyroProminence = 1.2f,
            minAccProminence = 1.2f,
            minImpactScore = 1.5f,
            impactOffsetMs = 0,
        ))
        prime(detector)
        val candidate = SwingUpdate(SwingState.PEAK_CANDIDATE, true, 9f, 15f, .2f)

        assertNull(detector.process(sample(100, 9f, 15f), candidate))
        val event = detector.process(
            sample(110, 5f, 8f),
            SwingUpdate(SwingState.FOLLOW_THROUGH, false, 5f, 8f, .2f),
        )

        assertNotNull(event)
        assertEquals(100L, event.timestampNs / 1_000_000)
    }

    @Test fun `pipeline keeps raw frames in its configured two second window`() {
        val pipeline = RecognitionPipeline(RecognitionConfig(ringBufferMs = 2_000))
        pipeline.process(frame(0, .1f, 9.81f))
        pipeline.process(frame(1_000, .1f, 9.81f))
        pipeline.process(frame(2_100, .1f, 9.81f))

        assertEquals(listOf(1_000L, 2_100L), pipeline.bufferedFrames().map { it.timestampMs })
    }

    @Test fun `one complete synthetic swing produces exactly one virtual impact`() {
        val pipeline = RecognitionPipeline(RecognitionConfig(
            lowPassAlpha = 1f,
            minCandidateMs = 20,
            minGyroProminence = 1.2f,
            minAccProminence = 1.2f,
            minImpactScore = 1.5f,
            impactOffsetMs = 0,
        ))
        val sequence = listOf(
            frame(0, .1f, 9.81f),
            frame(10, .1f, 9.81f),
            frame(20, 4f, 11f),
            frame(30, 6f, 12f),
            frame(40, 8f, 14f),
            frame(50, 12f, 20f),
            frame(60, 7f, 13f),
            frame(70, 2f, 10f),
        )

        val impacts = sequence.mapNotNull { pipeline.process(it).impact }

        assertEquals(1, impacts.size)
        assertEquals(50L, impacts.single().timestampNs / 1_000_000)
    }

    @Test fun `slow phone movement never becomes a swing candidate`() {
        val pipeline = RecognitionPipeline(RecognitionConfig(lowPassAlpha = 1f, minCandidateMs = 20))
        val results = (0L..400L step 20).map { time ->
            val gyro = .1f + time / 400f
            pipeline.process(frame(time, gyro, 9.81f + time / 1_000f))
        }

        assertTrue(results.none { it.swing.candidate })
        assertTrue(results.none { it.impact != null })
    }

    @Test fun `activity fall and a new rise rearm a second swing after cooldown`() {
        val detector = SwingDetector(RecognitionConfig(
            minCandidateMs = 20,
            cooldownMs = 100,
            rearmGyro = 1.5f,
            rearmAccRisePerSecond = 2f,
        ))
        detector.process(sample(0, .1f, .1f))
        detector.process(sample(10, 8f, 2f, gyroRise = 790f))
        assertTrue(detector.process(sample(30, 8f, 2f)).candidate)
        detector.beginCooldown(30)

        assertEquals(SwingState.COOLDOWN, detector.process(sample(160, .5f, .2f, accRise = 0f)).state)
        assertEquals(SwingState.ARMED, detector.process(sample(170, .5f, .4f, accRise = 20f)).state)
        assertEquals(SwingState.ACCELERATING, detector.process(sample(180, 8f, 2f, gyroRise = 750f)).state)
    }

    private fun prime(detector: ImpactDetector) {
        val noCandidate = SwingUpdate(SwingState.ARMED, false, 1f, 1f, .2f)
        repeat(5) { detector.process(sample(it * 10L, 1f, 1f), noCandidate) }
    }

    private fun sample(
        ms: Long,
        gyro: Float,
        acc: Float,
        gyroRise: Float = 0f,
        accRise: Float = 0f,
    ) = MotionSample(
        timestampNs = ms * 1_000_000,
        gyroActivity = gyro,
        accActivity = acc,
        gyroRisePerSecond = gyroRise,
        accRisePerSecond = accRise,
        deltaReset = false,
    )
    private fun frame(ms: Long, gyro: Float, acc: Float) = SensorFrame(ms * 1_000_000, acc, 0f, 0f, gyro, 0f, 0f)
}
