package com.knightspace.airswing.debug

import com.knightspace.airswing.sensor.SensorFrame
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SensorRecorderTest {
    @Test
    fun `recorder is opt in and exports sensor and event rows on one timeline`() {
        val recorder = SensorRecorder(
            maxRows = 4,
            metadata = mapOf("app_version" to "0.1.0", "recognition_config" to "test-config"),
        )
        recorder.record(frame(1))
        assertEquals(0, recorder.frameCount)

        recorder.start()
        recorder.recordSensor(frame(2), "ARMED", 4.5f, 1.2f)
        recorder.recordEvent(
            rowType = "candidate",
            timestampNs = 2_100_000,
            detectorState = "PEAK_CANDIDATE",
            swingScore = 8f,
            detail = "candidate accepted",
        )
        recorder.recordEvent(
            rowType = "impact",
            timestampNs = 2_200_000,
            detectorState = "FOLLOW_THROUGH",
            impactScore = 12f,
            eventTimestampNs = 2_050_000,
            detail = "strength=HARD",
        )

        val csv = recorder.toCsv()
        assertTrue(csv.startsWith("# airswing_csv_version=3\n"))
        assertTrue(csv.contains("# app_version=0.1.0\n"))
        assertTrue(csv.contains("# recognition_config=test-config\n"))
        assertTrue(csv.contains("row_type,timestamp_ns,ax,ay,az,gx,gy,gz,world_linear_ax,world_linear_ay,world_linear_az,detector_state,swing_score,impact_score,event_timestamp_ns,detail\n"))
        assertTrue(csv.contains("sensor,2000000,2.0,0.0,0.0,2.0,0.0,0.0,,,,ARMED,4.5,1.2,,"))
        assertTrue(csv.contains("candidate,2100000,,,,,,,,,,PEAK_CANDIDATE,8.0,,,candidate accepted"))
        assertTrue(csv.contains("impact,2200000,,,,,,,,,,FOLLOW_THROUGH,,12.0,2050000,strength=HARD"))
        assertEquals(1, recorder.frameCount)
        assertEquals(3, recorder.rowCount)
    }

    @Test
    fun `bounded timeline evicts oldest rows and quotes csv detail`() {
        val recorder = SensorRecorder(maxRows = 2)
        recorder.start()
        recorder.recordSensor(frame(1))
        recorder.recordEvent("candidate", 2_000_000, detail = "score=4, accepted")
        recorder.recordSensor(frame(3))

        val csv = recorder.toCsv()
        assertFalse(csv.contains("sensor,1000000,"))
        assertTrue(csv.contains("candidate,2000000,,,,,,,,,,,,,,\"score=4, accepted\""))
        assertTrue(csv.contains("sensor,3000000,"))
        assertEquals(1, recorder.frameCount)
        assertEquals(2, recorder.rowCount)
    }

    private fun frame(ms: Long) = SensorFrame(
        timestampNs = ms * 1_000_000,
        ax = ms.toFloat(), ay = 0f, az = 0f,
        gx = ms.toFloat(), gy = 0f, gz = 0f,
    )
}
