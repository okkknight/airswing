package com.knightspace.airswing.recognition

import com.knightspace.airswing.sensor.SensorFrame
import org.junit.Test
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RecognitionPipelineTest {
    @Test fun `a sustained rotational rise produces one swing candidate`() {
        val detector = SwingDetector(RecognitionConfig(minCandidateMs = 20, cooldownMs = 200))
        val updates = listOf(0L, 10L, 20L, 30L, 40L, 50L).map { time -> detector.process(frame(time, if (time < 20) .1f else 8f, 18f)) }
        assertTrue(updates.count { it.candidate } == 1)
    }
    @Test fun `candidate with acceleration prominence creates virtual impact`() {
        val detector = ImpactDetector(RecognitionConfig(minImpactScore = .1f))
        val event = detector.process(frame(100, 9f, 25f), SwingUpdate(SwingState.PEAK_CANDIDATE, true, 9f, 15f, .2f))
        assertNotNull(event)
        assertTrue(event.impactScore > 0f)
    }
    @Test fun `cooldown suppresses a second local peak`() {
        val detector = ImpactDetector(RecognitionConfig(minImpactScore = .1f, cooldownMs = 280))
        val swing = SwingUpdate(SwingState.PEAK_CANDIDATE, true, 9f, 15f, .2f)
        assertNotNull(detector.process(frame(100, 9f, 25f), swing))
        assertNull(detector.process(frame(180, 9f, 25f), swing))
    }
    @Test fun `impact offset is added to event timestamp`() {
        val detector = ImpactDetector(RecognitionConfig(minImpactScore = .1f, impactOffsetMs = 55))
        val event = detector.process(frame(100, 9f, 25f), SwingUpdate(SwingState.PEAK_CANDIDATE, true, 9f, 15f, .2f))
        assertNotNull(event)
        assertTrue(event.timestampNs == 155_000_000L)
    }
    private fun frame(ms: Long, gyro: Float, acc: Float) = SensorFrame(ms * 1_000_000, acc, 0f, 0f, gyro, 0f, 0f)
}
