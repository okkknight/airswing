package com.knightspace.airswing.debug

import com.knightspace.airswing.sensor.SensorFrame
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SensorRecorderTest {
    @Test
    fun `recorder is opt in and exports stable csv columns`() {
        val recorder = SensorRecorder(maxFrames = 2)
        recorder.record(frame(1))
        assertEquals(0, recorder.frameCount)

        recorder.start()
        recorder.record(frame(2))
        recorder.record(frame(3))
        recorder.record(frame(4))

        val csv = recorder.toCsv()
        assertTrue(csv.startsWith("timestamp_ns,ax,ay,az,gx,gy,gz\n"))
        assertFalse(csv.contains("2000000,"))
        assertTrue(csv.contains("3000000,3.0,0.0,0.0,3.0,0.0,0.0"))
        assertTrue(csv.contains("4000000,4.0,0.0,0.0,4.0,0.0,0.0"))
    }

    private fun frame(ms: Long) = SensorFrame(
        timestampNs = ms * 1_000_000,
        ax = ms.toFloat(), ay = 0f, az = 0f,
        gx = ms.toFloat(), gy = 0f, gz = 0f,
    )
}
