package com.knightspace.airswing.recognition

data class RecognitionConfig(
    // Research-reference defaults.
    val ringBufferMs: Long = 2_000, // research reference window
    val impactWindowMs: Long = 100,

    // Phone engineering initial values; tune here only after real-device evidence.
    val swingImpactWindowMs: Long = 2_000, // research-reference complete stroke window; retained for high-confidence recovery
    val fallCandidateWindowMs: Long = 500, // phone engineering initial value: causal low-confidence association
    val fallConfirmationWindowMs: Long = 150, // phone engineering initial value: only a prompt local fall confirms
    val minForwardGyroGrowth: Float = 1.5f, // phone engineering initial value: dimensionless phase consistency
    val minForwardAccGrowth: Float = 1.2f, // recall-first: retain weaker genuine phone swings
    val lowPassCutoffHz: Float = 8f, // research-inspired cutoff; timestamp-derived alpha preserves ~200 Hz tuning
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
    val minImpactGyro: Float = 3f, // real-phone tuning value: reject near-rest ratio spikes
    val minImpactAcc: Float = 3f, // real-phone tuning value: reject near-rest ratio spikes
    val earlyImpactGyro: Float = 28f, // real-phone tuning value: session9 lower bound with margin
    val earlyImpactAcc: Float = 145f, // real-phone tuning value: session9 lower bound with margin
    val screenNormalRotationRatio: Float = .85f, // real-phone tuning value: reject only clearly dominant Z rotation
    val screenNormalRotationMinFraction: Float = .5f,
    val screenNormalRotationMinDurationMs: Long = 40,
    val screenNormalRotationMaxAcc: Float = 120f, // real-phone tuning value: high-energy strokes always pass
    val impactOffsetMs: Long = 0, // real-phone tuning value: causal dual-signal peak needs no extra delay
    val cooldownMs: Long = 500, // research-reference IPF local-maximum neighborhood / real-time NMS
    val rearmGyro: Float = 3f, // real-phone tuning value: between-stroke filtered gyro floor
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
