package com.knightspace.airswing.recognition

import com.knightspace.airswing.sensor.MotionFilter
import com.knightspace.airswing.sensor.SensorFrame
import com.knightspace.airswing.sensor.SensorRingBuffer

data class RecognitionResult(
    val swing: SwingUpdate,
    val impact: VirtualImpactEvent?,
    val impactScore: Float,
    val directionRejected: Boolean = false,
)

class RecognitionPipeline(private val config: RecognitionConfig = RecognitionConfig()) {
    private val filter = MotionFilter(config)
    private val frames = SensorRingBuffer(config.ringBufferMs)
    private val swingDetector = SwingDetector(config)
    private val impactDetector = ImpactDetector(config)

    fun process(frame: SensorFrame): RecognitionResult {
        frames.add(frame)
        val sample = filter.process(frame)
        val swing = swingDetector.process(sample)
        val impact = impactDetector.process(sample, swing)
        if (impact != null) swingDetector.beginCooldown(frame.timestampMs)
        return RecognitionResult(
            swing,
            impact,
            impactDetector.lastImpactScore,
            impactDetector.lastDirectionRejected,
        )
    }

    fun bufferedFrames(): List<SensorFrame> = frames.snapshot()

    fun reset() {
        filter.reset()
        frames.clear()
        swingDetector.reset()
        impactDetector.reset()
    }
}
