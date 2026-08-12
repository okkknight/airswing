package com.knightspace.airswing.sensor

import kotlin.math.sqrt

data class SensorFrame(val timestampNs: Long, val ax: Float, val ay: Float, val az: Float, val gx: Float, val gy: Float, val gz: Float) {
    val timestampMs: Long get() = timestampNs / 1_000_000
    val accMagnitude: Float get() = sqrt(ax * ax + ay * ay + az * az)
    val gyroMagnitude: Float get() = sqrt(gx * gx + gy * gy + gz * gz)
}
