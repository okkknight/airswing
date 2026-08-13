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
    private var coupledPeakAtMs = Long.MIN_VALUE
    private var coupledPeakScore = 0f
    private var previousGyro = 0f
    private var previousLinear = 0f
    private var decision = StrokeDecision.PENDING

    /** Debug-only scalar: 1 means the recent linear motion kept a direction. */
    var lastAxisCoherence: Float = 0f
        private set

    fun process(sample: MotionSample, swing: SwingUpdate): StrokeDecision {
        if (sample.deltaReset) reset()
        append(sample)
        if (swing.candidate) begin(sample)
        if (candidateAtMs == Long.MIN_VALUE || decision != StrokeDecision.PENDING) return decision

        val coupledLinear = nearbyLinearPeak(sample.timestampMs)
        val coupled = sample.gyroActivity >= config.minImpactGyro &&
            coupledLinear / sample.gyroActivity >= config.minTranslationCoupling
        val score = sample.gyroActivity * coupledLinear
        if (coupled && score > coupledPeakScore) {
            coupledPeakScore = score
            coupledPeakAtMs = sample.timestampMs
        }
        lastAxisCoherence = axisCoherence(sample.timestampMs)

        if (coupled && sample.gyroActivity >= config.earlyImpactGyro &&
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
            decision = StrokeDecision.DEFERRED_CONFIRMED
        } else if (sample.timestampMs - candidateAtMs >= config.swingImpactWindowMs) {
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
        coupledPeakAtMs = Long.MIN_VALUE
        coupledPeakScore = 0f
        previousGyro = 0f
        previousLinear = 0f
        decision = StrokeDecision.PENDING
        lastAxisCoherence = 0f
    }

    private fun begin(sample: MotionSample) {
        candidateAtMs = sample.timestampMs
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

    private fun axisCoherence(timestampMs: Long): Float {
        var previous: MotionSample? = null
        var sum = 0f
        var pairs = 0
        for (i in 0 until size) {
            val item = history[(next - 1 - i + history.size) % history.size] ?: continue
            if (timestampMs - item.timestampMs > config.strokeEvidencePreMs) break
            val prior = previous
            if (prior != null) {
                val a = item.linearAccMagnitude
                val b = prior.linearAccMagnitude
                if (a > config.minImpactAcc && b > config.minImpactAcc) {
                    val dot = item.linearAccX * prior.linearAccX + item.linearAccY * prior.linearAccY + item.linearAccZ * prior.linearAccZ
                    sum += (dot / (a * b)).coerceIn(-1f, 1f)
                    pairs++
                }
            }
            previous = item
        }
        return if (pairs == 0) 0f else sum / pairs
    }
}
