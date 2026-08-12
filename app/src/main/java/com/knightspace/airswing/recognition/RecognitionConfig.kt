package com.knightspace.airswing.recognition

data class RecognitionConfig(
    val ringBufferMs: Long = 2_000, // research reference window
    val lowPassAlpha: Float = .22f, // phone engineering initial value
    val stationaryGyro: Float = .8f, val minSwingRatio: Float = 4f,
    val minGyroRise: Float = 2f, val minCandidateMs: Long = 45,
    val minImpactScore: Float = 1.2f, val impactOffsetMs: Long = 55, // phone engineering initial value
    val cooldownMs: Long = 280, val rearmGyro: Float = 1.5f,
    val softStrength: Float = 1.8f, val hardStrength: Float = 3.5f,
)
