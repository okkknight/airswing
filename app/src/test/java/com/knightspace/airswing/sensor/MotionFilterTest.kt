package com.knightspace.airswing.sensor

import com.knightspace.airswing.recognition.RecognitionConfig
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MotionFilterTest {
    @Test
    fun `low pass filter smooths an abrupt gyro spike`() {
        val filter = MotionFilter(RecognitionConfig(lowPassCutoffHz = 4.58f))
        filter.process(frame(0, gyro = 0f, acc = 9.81f))

        val sample = filter.process(frame(10, gyro = 8f, acc = 9.81f))

        assertEquals(2f, sample.gyroActivity, absoluteTolerance = 0.02f)
        assertEquals(200f, sample.gyroRisePerSecond, absoluteTolerance = 2f)
    }

    @Test
    fun `timestamp based low pass response is stable across sample rates`() {
        val config = RecognitionConfig(lowPassCutoffHz = 8f)
        val at50Hz = MotionFilter(config)
        val at200Hz = MotionFilter(config)
        at50Hz.process(frame(0, gyro = 0f, acc = 9.81f))
        at200Hz.process(frame(0, gyro = 0f, acc = 9.81f))

        val sample50 = at50Hz.process(frame(20, gyro = 8f, acc = 9.81f))
        var sample200 = at200Hz.process(frame(5, gyro = 8f, acc = 9.81f))
        for (time in 10L..20L step 5) sample200 = at200Hz.process(frame(time, gyro = 8f, acc = 9.81f))

        assertEquals(sample50.gyroActivity, sample200.gyroActivity, absoluteTolerance = .05f)
    }

    @Test
    fun `invalid timestamp delta resets derivative instead of creating a spike`() {
        val filter = MotionFilter(RecognitionConfig(lowPassCutoffHz = Float.POSITIVE_INFINITY, maxFrameDeltaMs = 100))
        filter.process(frame(100, gyro = 1f, acc = 9.81f))

        val sample = filter.process(frame(250, gyro = 9f, acc = 20f))

        assertEquals(0f, sample.gyroRisePerSecond)
        assertEquals(0f, sample.accRisePerSecond)
        assertTrue(sample.deltaReset)

        val resumed = filter.process(frame(260, gyro = 10f, acc = 21f))
        assertEquals(100f, resumed.gyroRisePerSecond, absoluteTolerance = .01f)
        assertEquals(100f, resumed.accRisePerSecond, absoluteTolerance = .01f)
        assertTrue(!resumed.deltaReset)
    }

    @Test
    fun `screen normal rotation ratio retains filtered phone axis direction`() {
        val normalFilter = MotionFilter(RecognitionConfig(lowPassCutoffHz = Float.POSITIVE_INFINITY))
        normalFilter.process(frame3d(0, gx = 0f, gy = 0f, gz = 1f))
        val normal = normalFilter.process(frame3d(10, gx = 1f, gy = 1f, gz = 10f))

        val planarFilter = MotionFilter(RecognitionConfig(lowPassCutoffHz = Float.POSITIVE_INFINITY))
        planarFilter.process(frame3d(0, gx = 1f, gy = 0f, gz = 0f))
        val planar = planarFilter.process(frame3d(10, gx = 10f, gy = 1f, gz = 1f))

        assertTrue(normal.screenNormalRotationRatio > .9f)
        assertTrue(planar.screenNormalRotationRatio < .2f)
    }

    @Test
    fun `world linear acceleration is retained separately from gravity inclusive acceleration`() {
        val filter = MotionFilter(RecognitionConfig(lowPassCutoffHz = Float.POSITIVE_INFINITY))
        filter.process(frameWithWorldLinearAcceleration(0, 0f))

        val sample = filter.process(frameWithWorldLinearAcceleration(10, 24f))

        assertEquals(24f, sample.linearAccMagnitude, absoluteTolerance = .01f)
        assertEquals(0f, sample.accActivity, absoluteTolerance = .01f)
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

    private fun frameWithWorldLinearAcceleration(ms: Long, linearAx: Float) = SensorFrame(
        timestampNs = ms * 1_000_000,
        ax = 0f,
        ay = 0f,
        az = 9.81f,
        gx = 0f,
        gy = 0f,
        gz = 0f,
        worldLinearAx = linearAx,
        worldLinearAy = 0f,
        worldLinearAz = 0f,
    )
}
