package com.knightspace.airswing.feedback

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import com.knightspace.airswing.R
import com.knightspace.airswing.recognition.ImpactStrength
import com.knightspace.airswing.recognition.RecognitionConfig
import com.knightspace.airswing.recognition.VirtualImpactEvent

enum class FeedbackStatus { LOADING, READY, ERROR }

fun feedbackDelayMs(eventTimestampNs: Long, nowNs: Long, maxDelayMs: Long = 100): Long =
    ((eventTimestampNs - nowNs) / 1_000_000).coerceIn(0, maxDelayMs)

class AudioLoadTracker(private val expectedSamples: Int) {
    private val successfulSamples = HashSet<Int>(expectedSamples)
    @Volatile
    var status: FeedbackStatus = FeedbackStatus.LOADING
        private set

    fun onLoadComplete(sampleId: Int, loadStatus: Int) {
        if (loadStatus != 0) {
            status = FeedbackStatus.ERROR
            return
        }
        if (status == FeedbackStatus.ERROR) return
        successfulSamples += sampleId
        if (successfulSamples.size == expectedSamples) status = FeedbackStatus.READY
    }

    fun fail() {
        status = FeedbackStatus.ERROR
    }
}

interface ImpactAudio {
    val status: FeedbackStatus
    fun play(event: VirtualImpactEvent): Boolean
}

interface ImpactHaptic {
    fun play(): Boolean
}

/** Keeps count eligibility on the same event path as audio and haptic feedback. */
class FeedbackCoordinator(
    private val audio: ImpactAudio,
    private val haptic: ImpactHaptic,
) {
    fun dispatch(event: VirtualImpactEvent): Boolean {
        if (audio.status != FeedbackStatus.READY) return false
        if (!audio.play(event)) return false
        haptic.play()
        return true
    }
}

class AudioEngine(
    context: Context,
    private val config: RecognitionConfig,
) : ImpactAudio {
    private val pool = SoundPool.Builder()
        .setMaxStreams(3)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build(),
        )
        .build()
    private val ids: IntArray
    private val loadTracker = AudioLoadTracker(expectedSamples = 3)
    override val status: FeedbackStatus get() = loadTracker.status

    init {
        pool.setOnLoadCompleteListener { _, sampleId, loadStatus ->
            loadTracker.onLoadComplete(sampleId, loadStatus)
        }
        ids = intArrayOf(
            pool.load(context, R.raw.hit_soft, 1),
            pool.load(context, R.raw.hit_medium, 1),
            pool.load(context, R.raw.hit_hard, 1),
        )
        if (ids.any { it == 0 }) loadTracker.fail()
    }

    override fun play(event: VirtualImpactEvent): Boolean {
        if (status != FeedbackStatus.READY) return false
        val index = when (config.mapStrength(event.strength)) {
            ImpactStrength.SOFT -> 0
            ImpactStrength.MEDIUM -> 1
            ImpactStrength.HARD -> 2
        }
        return pool.play(ids[index], 1f, 1f, 1, 0, 1f) != 0
    }

    fun release() = pool.release()
}

class HapticEngine(context: Context) : ImpactHaptic {
    private val vibrator = context.getSystemService(Vibrator::class.java)

    override fun play(): Boolean {
        val target = vibrator ?: return false
        if (!target.hasVibrator()) return false
        if (Build.VERSION.SDK_INT >= 29) {
            target.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
        } else {
            target.vibrate(VibrationEffect.createOneShot(18, VibrationEffect.DEFAULT_AMPLITUDE))
        }
        return true
    }
}
