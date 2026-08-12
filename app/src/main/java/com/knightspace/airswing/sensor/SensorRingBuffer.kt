package com.knightspace.airswing.sensor

class SensorRingBuffer(private val capacityMs: Long) {
    private val frames = ArrayDeque<SensorFrame>()
    fun add(frame: SensorFrame) { frames.addLast(frame); while (frames.isNotEmpty() && frame.timestampMs - frames.first().timestampMs > capacityMs) frames.removeFirst() }
    fun snapshot(): List<SensorFrame> = frames.toList()
    fun around(timestampMs: Long, beforeMs: Long, afterMs: Long): List<SensorFrame> = frames.filter { it.timestampMs in timestampMs - beforeMs..timestampMs + afterMs }
    fun clear() = frames.clear()
}
