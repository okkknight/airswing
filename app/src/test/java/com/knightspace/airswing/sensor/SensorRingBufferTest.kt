package com.knightspace.airswing.sensor

import org.junit.Test
import kotlin.test.assertEquals

class SensorRingBufferTest {
    @Test
    fun `keeps only frames inside configured time capacity`() {
        val buffer = SensorRingBuffer(capacityMs = 2_000)
        buffer.add(frameAt(0))
        buffer.add(frameAt(1_000))
        buffer.add(frameAt(2_100))

        assertEquals(listOf(1_000L, 2_100L), buffer.snapshot().map { it.timestampMs })
    }

    private fun frameAt(timestampMs: Long) = SensorFrame(
        timestampNs = timestampMs * 1_000_000,
        ax = 0f,
        ay = 0f,
        az = 9.81f,
        gx = 0f,
        gy = 0f,
        gz = 0f,
    )
}
