package com.knightspace.airswing.recognition

data class RecognitionConfig(
    // Research-reference defaults.
    val ringBufferMs: Long = 2_000, // research reference window
    val impactWindowMs: Long = 100,

    // Phone engineering initial values; tune here only after real-device evidence.
    val lowPassAlpha: Float = .22f, // phone engineering initial value
    val baselineAlpha: Float = .04f,
    val minimumBaseline: Float = .1f,
    val maxFrameDeltaMs: Long = 100, // phone engineering initial value
    val stationaryGyro: Float = .8f,
    val stationaryAcc: Float = .8f,
    val minSwingRatio: Float = 4f,
    val minGyroRisePerSecond: Float = 20f,
    val minSwingAcc: Float = .7f,
    val minCandidateMs: Long = 45,
    val minGyroProminence: Float = 1.5f,
    val minAccProminence: Float = 1.5f,
    val minImpactScore: Float = 2.25f,
    val impactOffsetMs: Long = 55, // phone engineering initial value
    val cooldownMs: Long = 280,
    val rearmGyro: Float = 1.5f,
    val rearmAccRisePerSecond: Float = 2f,
    val softStrength: Float = 1.8f, val hardStrength: Float = 3.5f,
) {
    fun mapStrength(strength: Float): ImpactStrength = when {
        strength < softStrength -> ImpactStrength.SOFT
        strength < hardStrength -> ImpactStrength.MEDIUM
        else -> ImpactStrength.HARD
    }
}

enum class ImpactStrength { SOFT, MEDIUM, HARD }
