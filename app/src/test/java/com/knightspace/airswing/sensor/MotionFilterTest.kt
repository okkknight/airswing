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

    @Test
    fun `screen normal rotation ratio retains filtered phone axis direction`() {
        val normalFilter = MotionFilter(RecognitionConfig(lowPassAlpha = 1f))
        normalFilter.process(frame3d(0, gx = 0f, gy = 0f, gz = 1f))
        val normal = normalFilter.process(frame3d(10, gx = 1f, gy = 1f, gz = 10f))

        val planarFilter = MotionFilter(RecognitionConfig(lowPassAlpha = 1f))
        planarFilter.process(frame3d(0, gx = 1f, gy = 0f, gz = 0f))
        val planar = planarFilter.process(frame3d(10, gx = 10f, gy = 1f, gz = 1f))

        assertTrue(normal.screenNormalRotationRatio > .9f)
        assertTrue(planar.screenNormalRotationRatio < .2f)
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

    private fun frame3d(ms: Long, gx: Float, gy: Float, gz: Float) = SensorFrame(
        timestampNs = ms * 1_000_000,
        ax = 0f,
        ay = 0f,
        az = 9.81f,
        gx = gx,
        gy = gy,
        gz = gz,
    )
}
