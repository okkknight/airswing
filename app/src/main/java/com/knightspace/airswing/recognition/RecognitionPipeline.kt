package com.knightspace.airswing.recognition

import com.knightspace.airswing.sensor.MotionFilter
import com.knightspace.airswing.sensor.SensorFrame
import com.knightspace.airswing.sensor.SensorRingBuffer

data class RecognitionResult(
    val swing: SwingUpdate,
    val impact: VirtualImpactEvent?,
    val impactScore: Float,
    val directionRejected: Boolean = false,
    val strokeDecision: StrokeDecision = StrokeDecision.PENDING,
)

class RecognitionPipeline(private val config: RecognitionConfig = RecognitionConfig()) {
    private val filter = MotionFilter(config)
    private val frames = SensorRingBuffer(config.ringBufferMs)
    private val swingDetector = SwingDetector(config)
    private val strokeEvidenceAnalyzer = StrokeEvidenceAnalyzer(config)
    private val impactDetector = ImpactDetector(config)

    fun process(frame: SensorFrame): RecognitionResult {
        frames.add(frame)
        val sample = filter.process(frame)
        val swing = swingDetector.process(sample)
        val strokeDecision = strokeEvidenceAnalyzer.process(sample, swing)
        val impact = impactDetector.process(sample, swing, strokeDecision)
        if (impact != null) {
            swingDetector.beginCooldown(frame.timestampMs)
            strokeEvidenceAnalyzer.reset()
        }
        return RecognitionResult(
            swing,
            impact,
            impactDetector.lastImpactScore,
            impactDetector.lastDirectionRejected,
            strokeDecision,
        )
    }

    fun bufferedFrames(): List<SensorFrame> = frames.snapshot()

    fun reset() {
        filter.reset()
        frames.clear()
        swingDetector.reset()
        strokeEvidenceAnalyzer.reset()
        impactDetector.reset()
    }
}
