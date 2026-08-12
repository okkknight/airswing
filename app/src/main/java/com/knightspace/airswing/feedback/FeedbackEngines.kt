package com.knightspace.airswing.feedback

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.Build
import com.knightspace.airswing.R
import com.knightspace.airswing.recognition.VirtualImpactEvent

class AudioEngine(context: Context) {
    private val pool = SoundPool.Builder().setMaxStreams(3).setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build()).build()
    private var ready = false; private var ids = intArrayOf()
    init { var loaded = 0; pool.setOnLoadCompleteListener { _, _, status -> if (status == 0 && ++loaded == 3) ready = true }; ids = intArrayOf(pool.load(context, R.raw.hit_soft, 1), pool.load(context, R.raw.hit_medium, 1), pool.load(context, R.raw.hit_hard, 1)) }
    fun isReady() = ready
    fun play(event: VirtualImpactEvent) { if (ready) { val index = if (event.strength < 1.8f) 0 else if (event.strength < 3.5f) 1 else 2; pool.play(ids[index], 1f, 1f, 1, 0, 1f) } }
    fun release() = pool.release()
}
class HapticEngine(context: Context) { private val vibrator = context.getSystemService(Vibrator::class.java); fun play() { if (Build.VERSION.SDK_INT >= 29) vibrator?.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK)) else vibrator?.vibrate(VibrationEffect.createOneShot(18, VibrationEffect.DEFAULT_AMPLITUDE)) } }
