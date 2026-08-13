package com.knightspace.airswing.sensor

import kotlin.math.sqrt

data class SensorFrame(
    val timestampNs: Long,
    val ax: Float,
    val ay: Float,
    val az: Float,
    val gx: Float,
    val gy: Float,
    val gz: Float,
    /** Gravity-compensated acceleration in world coordinates when Rotation Vector is available. */
    val worldLinearAx: Float = Float.NaN,
    val worldLinearAy: Float = Float.NaN,
    val worldLinearAz: Float = Float.NaN,
) {
    val timestampMs: Long get() = timestampNs / 1_000_000
    val accMagnitude: Float get() = sqrt(ax * ax + ay * ay + az * az)
    val gyroMagnitude: Float get() = sqrt(gx * gx + gy * gy + gz * gz)
}
