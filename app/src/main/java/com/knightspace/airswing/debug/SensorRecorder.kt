package com.knightspace.airswing.debug

import com.knightspace.airswing.sensor.SensorFrame

/** Debug-only consumer: callers must guard all use with BuildConfig.DEBUG. */
class SensorRecorder(private val maxFrames: Int = 120_000) {
    private val frames = ArrayDeque<SensorFrame>()
    var isRecording: Boolean = false
        private set
    val frameCount: Int get() = frames.size

    fun start() {
        frames.clear()
        isRecording = true
    }

    fun stop() {
        isRecording = false
    }

    fun record(frame: SensorFrame) {
        if (!isRecording) return
        frames.addLast(frame)
        if (frames.size > maxFrames) frames.removeFirst()
    }

    fun toCsv(): String = buildString {
        appendLine("timestamp_ns,ax,ay,az,gx,gy,gz")
        frames.forEach { frame ->
            append(frame.timestampNs).append(',')
            append(frame.ax).append(',').append(frame.ay).append(',').append(frame.az).append(',')
            append(frame.gx).append(',').append(frame.gy).append(',').append(frame.gz).append('\n')
        }
    }
}
