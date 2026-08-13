package com.knightspace.airswing.recognition

import com.knightspace.airswing.sensor.MotionSample
import kotlin.math.max
import kotlin.math.sqrt

enum class StrokeDecision { PENDING, FAST_CONFIRMED, DEFERRED_CONFIRMED, REJECTED }

/**
 * Causal phone adaptation of the complete-stroke/IPF route.  The fixed short
 * history preserves pre-swing evidence without allocating collections on the
 * sensor path.  It deliberately leaves the existing strong IPF fast path to
 * ImpactDetector: hand feel must not wait for follow-through.
 */
class StrokeEvidenceAnalyzer(private val config: RecognitionConfig) {
    private val history = arrayOfNulls<MotionSample>(64) // > 240 ms at 200 Hz
    private var next = 0
    private var size = 0
    private var candidateAtMs = Long.MIN_VALUE
    private var forwardStartedAtMs = Long.MIN_VALUE
    private var coupledPeakAtMs = Long.MIN_VALUE
    private var coupledPeakScore = 0f
    private var previousGyro = 0f
    private var previousLinear = 0f
    private var decision = StrokeDecision.PENDING

    /** Debug-only scalar: resultant / total linear impulse in the forward window. */
    var lastAxisCoherence: Float = 0f
        private set

    fun process(sample: MotionSample, swing: SwingUpdate): StrokeDecision {
        if (sample.deltaReset) reset()
        append(sample)
        if (swing.candidate) begin(sample, swing)
        if (candidateAtMs == Long.MIN_VALUE || decision != StrokeDecision.PENDING) return decision

        val coupledLinear = nearbyLinearPeak(sample.timestampMs)
        val coupled = sample.gyroActivity >= config.minImpactGyro &&
            coupledLinear / sample.gyroActivity >= config.minTranslationCoupling
        val score = sample.gyroActivity * coupledLinear
        if (coupled && score > coupledPeakScore) {
            coupledPeakScore = score
            coupledPeakAtMs = sample.timestampMs
        }
        lastAxisCoherence = translationImpulseCoherence(sample.timestampMs)

        if (coupled && lastAxisCoherence >= config.minTranslationImpulseCoherence &&
            sample.gyroActivity >= config.earlyImpactGyro &&
            coupledLinear >= config.earlyImpactAcc
        ) {
            decision = StrokeDecision.FAST_CONFIRMED
            previousGyro = sample.gyroActivity
            previousLinear = sample.linearAccMagnitude
            return decision
        }

        // Medium confidence is allowed to wait only for an immediate fall.
        val hasFallen = sample.gyroActivity < previousGyro && sample.linearAccMagnitude < previousLinear
        val sincePeak = sample.timestampMs - coupledPeakAtMs
        if (coupledPeakAtMs != Long.MIN_VALUE && hasFallen && sincePeak in 0..config.strokeEvidencePostMs) {
            decision = if (lastAxisCoherence >= config.minTranslationImpulseCoherence) {
                StrokeDecision.DEFERRED_CONFIRMED
            } else {
                StrokeDecision.REJECTED
            }
        } else if (sample.timestampMs - candidateAtMs >= config.deferredCandidateWindowMs) {
            decision = StrokeDecision.REJECTED
        }
        previousGyro = sample.gyroActivity
        previousLinear = sample.linearAccMagnitude
        return decision
    }

    fun reset() {
        next = 0
        size = 0
        candidateAtMs = Long.MIN_VALUE
        forwardStartedAtMs = Long.MIN_VALUE
        coupledPeakAtMs = Long.MIN_VALUE
        coupledPeakScore = 0f
        previousGyro = 0f
        previousLinear = 0f
        decision = StrokeDecision.PENDING
        lastAxisCoherence = 0f
    }

    private fun begin(sample: MotionSample, swing: SwingUpdate) {
        candidateAtMs = sample.timestampMs
        forwardStartedAtMs = swing.accelerationStartTimestampMs.takeIf { it in 0..sample.timestampMs }
            ?: (candidateAtMs - config.strokeEvidencePreMs)
        coupledPeakAtMs = Long.MIN_VALUE
        coupledPeakScore = 0f
        previousGyro = sample.gyroActivity
        previousLinear = sample.linearAccMagnitude
        decision = StrokeDecision.PENDING
    }

    private fun append(sample: MotionSample) {
        history[next] = sample
        next = (next + 1) % history.size
        size = minOf(size + 1, history.size)
    }

    private fun nearbyLinearPeak(timestampMs: Long): Float {
        var peak = 0f
        for (i in 0 until size) {
            val item = history[(next - 1 - i + history.size) % history.size] ?: continue
            val age = timestampMs - item.timestampMs
            if (age > config.maxPeakCooccurrenceMs) break
            peak = max(peak, item.linearAccMagnitude)
        }
        return peak
    }

    private fun translationImpulseCoherence(timestampMs: Long): Float {
        val startMs = maxOf(forwardStartedAtMs, timestampMs - config.strokeEvidencePreMs)
        var sumX = 0f
        var sumY = 0f
        var sumZ = 0f
        var total = 0f
        for (i in 0 until size) {
            val item = history[(next - 1 - i + history.size) % history.size] ?: continue
            if (item.timestampMs < startMs) break
            if (item.linearAccMagnitude >= config.minImpactAcc) {
                sumX += item.linearAccX
                sumY += item.linearAccY
                sumZ += item.linearAccZ
                total += item.linearAccMagnitude
            }
        }
        return if (total == 0f) 0f else sqrt(sumX * sumX + sumY * sumY + sumZ * sumZ) / total
    }
}
