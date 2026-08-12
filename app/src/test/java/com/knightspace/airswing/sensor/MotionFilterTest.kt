package com.knightspace.airswing.sensor

import com.knightspace.airswing.recognition.RecognitionConfig
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MotionFilterTest {
    @Test
    fun `low pass filter smooths an abrupt gyro spike`() {
        val filter = MotionFilter(RecognitionConfig(lowPassAlpha = 0.25f))
        filter.process(frame(0, gyro = 0f, acc = 9.81f))

        val sample = filter.process(frame(10, gyro = 8f, acc = 9.81f))

        assertEquals(2f, sample.gyroActivity, absoluteTolerance = 0.001f)
        assertEquals(200f, sample.gyroRisePerSecond, absoluteTolerance = 0.01f)
    }

    @Test
    fun `invalid timestamp delta resets derivative instead of creating a spike`() {
        val filter = MotionFilter(RecognitionConfig(lowPassAlpha = 1f, maxFrameDeltaMs = 100))
        filter.process(frame(100, gyro = 1f, acc = 9.81f))

        val sample = filter.process(frame(250, gyro = 9f, acc = 20f))

        assertEquals(0f, sample.gyroRisePerSecond)
        assertEquals(0f, sample.accRisePerSecond)
        assertTrue(sample.deltaReset)
    }

    private fun frame(ms: Long, gyro: Float, acc: Float) = SensorFrame(
        timestampNs = ms * 1_000_000,
        ax = acc,
        ay = 0f,
        az = 0f,
        gx = gyro,
        gy = 0f,
        gz = 0f,
    )
}
