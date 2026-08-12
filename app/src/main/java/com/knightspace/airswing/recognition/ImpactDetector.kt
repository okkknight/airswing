package com.knightspace.airswing.recognition

import com.knightspace.airswing.sensor.MotionSample

data class VirtualImpactEvent(val timestampNs: Long, val strength: Float, val impactScore: Float)

private data class PeakCandidate(
    val sample: MotionSample,
    val gyroProminence: Float,
    val accProminence: Float,
    val score: Float,
)

class ImpactDetector(private val config: RecognitionConfig) {
    private val recent = ArrayDeque<MotionSample>()
    private var gyroSum = 0f
    private var accSum = 0f
    private var lastImpactMs = Long.MIN_VALUE
    private var candidateUntilMs = Long.MIN_VALUE
    private var pendingPeak: PeakCandidate? = null
    var lastImpactScore: Float = 0f
        private set

    fun process(sample: MotionSample, swing: SwingUpdate): VirtualImpactEvent? {
        while (recent.isNotEmpty() && sample.timestampMs - recent.first().timestampMs > config.impactWindowMs) {
            val expired = recent.removeFirst()
            gyroSum -= expired.gyroActivity
            accSum -= expired.accActivity
        }
        val comparisonCount = recent.size
        val gyroMean = if (comparisonCount == 0) config.minimumBaseline else
            (gyroSum / comparisonCount).coerceAtLeast(config.minimumBaseline)
        val accMean = if (comparisonCount == 0) config.minimumBaseline else
            (accSum / comparisonCount).coerceAtLeast(config.minimumBaseline)
        val gyroProminence = sample.gyroActivity / gyroMean
        val accProminence = sample.accActivity / accMean
        lastImpactScore = gyroProminence * accProminence

        if (swing.candidate) candidateUntilMs = sample.timestampMs + config.impactWindowMs
        val previousPeak = pendingPeak
        val hasFallenFromPrevious = previousPeak != null &&
            sample.gyroActivity < previousPeak.sample.gyroActivity &&
            sample.accActivity < previousPeak.sample.accActivity
        val event = if (hasFallenFromPrevious) confirm(previousPeak) else null

        val currentPeak = pendingPeak
        if (sample.timestampMs <= candidateUntilMs && comparisonCount > 0 &&
            gyroProminence >= config.minGyroProminence &&
            accProminence >= config.minAccProminence &&
            lastImpactScore >= config.minImpactScore &&
            (currentPeak == null || lastImpactScore > currentPeak.score)
        ) {
            pendingPeak = PeakCandidate(sample, gyroProminence, accProminence, lastImpactScore)
        } else if (sample.timestampMs > candidateUntilMs || hasFallenFromPrevious) {
            pendingPeak = null
        }

        recent.addLast(sample)
        gyroSum += sample.gyroActivity
        accSum += sample.accActivity
        return event
    }

    private fun confirm(peak: PeakCandidate): VirtualImpactEvent? {
        if (lastImpactMs != Long.MIN_VALUE && peak.sample.timestampMs - lastImpactMs < config.cooldownMs) return null
        lastImpactMs = peak.sample.timestampMs
        return VirtualImpactEvent(
            timestampNs = peak.sample.timestampNs + config.impactOffsetMs * 1_000_000,
            strength = peak.gyroProminence,
            impactScore = peak.score,
        )
    }

    fun reset() {
        recent.clear()
        gyroSum = 0f
        accSum = 0f
        lastImpactMs = Long.MIN_VALUE
        candidateUntilMs = Long.MIN_VALUE
        pendingPeak = null
        lastImpactScore = 0f
    }
}
