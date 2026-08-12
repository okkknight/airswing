package com.knightspace.airswing.recognition

import com.knightspace.airswing.sensor.MotionSample

enum class ImpactConfirmation { EARLY_HIGH_CONFIDENCE, FALL_CONFIRMED }

data class VirtualImpactEvent(
    val timestampNs: Long,
    val strength: Float,
    val impactScore: Float,
    val confirmation: ImpactConfirmation = ImpactConfirmation.FALL_CONFIRMED,
)

private data class PeakCandidate(
    val sample: MotionSample,
    val gyroProminence: Float,
    val accProminence: Float,
    val score: Float,
)

private data class ScoredMotion(
    val sample: MotionSample,
    val gyroProminence: Float,
    val accProminence: Float,
    val score: Float,
)

class ImpactDetector(private val config: RecognitionConfig) {
    private val recent = ArrayDeque<ScoredMotion>()
    private var gyroSum = 0f
    private var accSum = 0f
    private var lastImpactMs = Long.MIN_VALUE
    private var candidateUntilMs = Long.MIN_VALUE
    private var pendingPeak: PeakCandidate? = null
    var lastImpactScore: Float = 0f
        private set
    var lastDirectionRejected: Boolean = false
        private set

    fun process(sample: MotionSample, swing: SwingUpdate): VirtualImpactEvent? {
        lastDirectionRejected = false
        while (recent.isNotEmpty() && sample.timestampMs - recent.first().sample.timestampMs > config.impactWindowMs) {
            val expired = recent.removeFirst()
            gyroSum -= expired.sample.gyroActivity
            accSum -= expired.sample.accActivity
        }
        val comparisonCount = recent.size
        val gyroMean = if (comparisonCount == 0) config.minimumBaseline else
            (gyroSum / comparisonCount).coerceAtLeast(config.minimumBaseline)
        val accMean = if (comparisonCount == 0) config.minimumBaseline else
            (accSum / comparisonCount).coerceAtLeast(config.minimumBaseline)
        val gyroProminence = sample.gyroActivity / gyroMean
        val accProminence = sample.accActivity / accMean
        lastImpactScore = gyroProminence * accProminence

        if (swing.candidate) {
            // The swing candidate marks a sustained high-speed stroke, not necessarily the
            // impact itself. BadminSense searches a complete 2 s stroke window; within it,
            // the IPF-inspired dual-signal prominence finds the virtual impact peak.
            candidateUntilMs = sample.timestampMs + config.swingImpactWindowMs
            var best: ScoredMotion? = null
            recent.forEach { scored ->
                val previousBest = best
                if (isEligible(scored) &&
                    (previousBest == null || peakMagnitude(scored.sample) > peakMagnitude(previousBest.sample))) {
                    best = scored
                }
            }
            best?.let { scored ->
                pendingPeak = PeakCandidate(
                    sample = scored.sample,
                    gyroProminence = scored.gyroProminence,
                    accProminence = scored.accProminence,
                    score = scored.score,
                )
            }
        }
        val earlyPeak = pendingPeak?.takeIf(::isHighConfidence)
        if (earlyPeak != null) {
            val event = confirm(earlyPeak, ImpactConfirmation.EARLY_HIGH_CONFIDENCE)
            if (event != null) {
                candidateUntilMs = Long.MIN_VALUE
                pendingPeak = null
                recent.addLast(ScoredMotion(sample, gyroProminence, accProminence, lastImpactScore))
                gyroSum += sample.gyroActivity
                accSum += sample.accActivity
                return event
            }
        }
        val previousPeak = pendingPeak
        val hasFallenFromPrevious = previousPeak != null &&
            sample.gyroActivity < previousPeak.sample.gyroActivity &&
            sample.accActivity < previousPeak.sample.accActivity
        lastDirectionRejected = hasFallenFromPrevious && isScreenNormalRotationDominant()
        val event = if (hasFallenFromPrevious && !lastDirectionRejected) confirm(previousPeak) else null

        val currentPeak = pendingPeak
        if (event != null) {
            candidateUntilMs = Long.MIN_VALUE
            pendingPeak = null
        } else if (sample.timestampMs <= candidateUntilMs && comparisonCount > 0 &&
            isEligible(sample, gyroProminence, accProminence, lastImpactScore) &&
            (currentPeak == null || peakMagnitude(sample) > peakMagnitude(currentPeak.sample))
        ) {
            pendingPeak = PeakCandidate(sample, gyroProminence, accProminence, lastImpactScore)
        } else if (sample.timestampMs > candidateUntilMs || hasFallenFromPrevious) {
            pendingPeak = null
        }

        recent.addLast(ScoredMotion(sample, gyroProminence, accProminence, lastImpactScore))
        gyroSum += sample.gyroActivity
        accSum += sample.accActivity
        return event
    }

    private fun isEligible(scored: ScoredMotion): Boolean = isEligible(
        scored.sample,
        scored.gyroProminence,
        scored.accProminence,
        scored.score,
    )

    private fun isEligible(
        sample: MotionSample,
        gyroProminence: Float,
        accProminence: Float,
        score: Float,
    ): Boolean = sample.gyroActivity >= config.minImpactGyro &&
            sample.accActivity >= config.minImpactAcc &&
            gyroProminence >= config.minGyroProminence &&
            accProminence >= config.minAccProminence &&
            score >= config.minImpactScore

    private fun peakMagnitude(sample: MotionSample?): Float =
        sample?.let { it.gyroActivity * it.accActivity } ?: Float.NEGATIVE_INFINITY

    private fun isHighConfidence(peak: PeakCandidate): Boolean =
        peak.sample.gyroActivity >= config.earlyImpactGyro &&
            peak.sample.accActivity >= config.earlyImpactAcc

    /** Phone adaptation of IPF axis separation: uncertain or high-energy motion always remains eligible. */
    private fun isScreenNormalRotationDominant(): Boolean {
        var activeCount = 0
        var dominantCount = 0
        var firstDominantMs = Long.MAX_VALUE
        var lastDominantMs = Long.MIN_VALUE
        var maxAcc = 0f
        recent.forEach { scored ->
            val sample = scored.sample
            if (sample.accActivity > maxAcc) maxAcc = sample.accActivity
            if (sample.gyroActivity >= config.minImpactGyro) {
                activeCount++
                if (sample.screenNormalRotationRatio >= config.screenNormalRotationRatio) {
                    dominantCount++
                    if (sample.timestampMs < firstDominantMs) firstDominantMs = sample.timestampMs
                    if (sample.timestampMs > lastDominantMs) lastDominantMs = sample.timestampMs
                }
            }
        }
        val dominantFraction = if (activeCount > 0) dominantCount.toFloat() / activeCount else 0f
        val dominantDurationMs = if (dominantCount > 1) lastDominantMs - firstDominantMs else 0L
        return dominantFraction >= config.screenNormalRotationMinFraction &&
            dominantDurationMs >= config.screenNormalRotationMinDurationMs &&
            maxAcc < config.screenNormalRotationMaxAcc
    }

    private fun confirm(
        peak: PeakCandidate,
        confirmation: ImpactConfirmation = ImpactConfirmation.FALL_CONFIRMED,
    ): VirtualImpactEvent? {
        if (lastImpactMs != Long.MIN_VALUE && peak.sample.timestampMs - lastImpactMs < config.cooldownMs) return null
        lastImpactMs = peak.sample.timestampMs
        return VirtualImpactEvent(
            timestampNs = peak.sample.timestampNs + config.impactOffsetMs * 1_000_000,
            strength = peak.gyroProminence,
            impactScore = peak.score,
            confirmation = confirmation,
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
        lastDirectionRejected = false
    }
}
