package com.knightspace.airswing.recognition

import com.knightspace.airswing.sensor.SensorFrame

data class VirtualImpactEvent(val timestampNs: Long, val strength: Float, val impactScore: Float)
class ImpactDetector(private val config: RecognitionConfig) {
    private var gyroBase = .1f; private var accBase = .1f; private var lastImpact = Long.MIN_VALUE
    fun reset() { gyroBase = .1f; accBase = .1f; lastImpact = Long.MIN_VALUE }
    fun process(frame: SensorFrame, swing: SwingUpdate): VirtualImpactEvent? {
        gyroBase = gyroBase * .97f + swing.gyroActivity * .03f; accBase = accBase * .97f + swing.accActivity * .03f
        if (!swing.candidate || (lastImpact != Long.MIN_VALUE && frame.timestampMs - lastImpact < config.cooldownMs)) return null
        val g = swing.gyroActivity / gyroBase.coerceAtLeast(.1f); val a = swing.accActivity / accBase.coerceAtLeast(.1f)
        val score = g * a
        if (score < config.minImpactScore) return null
        lastImpact = frame.timestampMs
        return VirtualImpactEvent((frame.timestampMs + config.impactOffsetMs) * 1_000_000, g, score)
    }
}
