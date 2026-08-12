package com.knightspace.airswing.domain

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.airSwingDataStore by preferencesDataStore(name = "airswing")

enum class Handedness { LEFT, RIGHT }

// Engineering estimate requested for the MVP: effective shoulder-to-phone radius for a 180 cm male.
private const val STANDARD_EFFECTIVE_ARM_RADIUS_METERS = .80f

fun estimatePeakLinearSpeedKmh(angularSpeedRadPerSecond: Float): Float =
    angularSpeedRadPerSecond.coerceAtLeast(0f) * STANDARD_EFFECTIVE_ARM_RADIUS_METERS * 3.6f

data class SessionSummary(
    val startedAtMs: Long,
    val endedAtMs: Long,
    val strokeCount: Int,
    val peakSwingSpeedRadPerSecond: Float = 0f,
)

data class AppPreferences(
    val hasSetup: Boolean = false,
    val handedness: Handedness = Handedness.RIGHT,
    val lastCount: Int = 0,
    val totalCount: Int = 0,
    val lastSession: SessionSummary? = null,
)

class SessionStore(private val dataStore: DataStore<Preferences>) {
    constructor(context: Context) : this(context.applicationContext.airSwingDataStore)

    val state: Flow<AppPreferences> = dataStore.data.map { preferences ->
        val lastStart = preferences[Keys.LAST_START]
        val lastEnd = preferences[Keys.LAST_END]
        val lastCount = preferences[Keys.LAST_COUNT] ?: 0
        AppPreferences(
            hasSetup = preferences[Keys.HAS_SETUP] ?: false,
            handedness = preferences[Keys.HAND]
                ?.let { runCatching { Handedness.valueOf(it) }.getOrNull() }
                ?: Handedness.RIGHT,
            lastCount = lastCount,
            totalCount = preferences[Keys.TOTAL_COUNT] ?: 0,
            lastSession = if (lastStart != null && lastEnd != null) {
                SessionSummary(
                    lastStart,
                    lastEnd,
                    lastCount,
                    preferences[Keys.LAST_PEAK_SWING_SPEED] ?: 0f,
                )
            } else {
                null
            },
        )
    }

    suspend fun saveSetup(handedness: Handedness) {
        dataStore.edit { preferences ->
            preferences[Keys.HAS_SETUP] = true
            preferences[Keys.HAND] = handedness.name
        }
    }

    suspend fun saveSession(session: SessionSummary) {
        dataStore.edit { preferences ->
            preferences[Keys.LAST_START] = session.startedAtMs
            preferences[Keys.LAST_END] = session.endedAtMs
            preferences[Keys.LAST_COUNT] = session.strokeCount
            preferences[Keys.LAST_PEAK_SWING_SPEED] = session.peakSwingSpeedRadPerSecond
            preferences[Keys.TOTAL_COUNT] = (preferences[Keys.TOTAL_COUNT] ?: 0) + session.strokeCount
        }
    }

    private object Keys {
        val HAS_SETUP = booleanPreferencesKey("hasSetup")
        val HAND = stringPreferencesKey("hand")
        val LAST_START = longPreferencesKey("lastStartedAtMs")
        val LAST_END = longPreferencesKey("lastEndedAtMs")
        val LAST_COUNT = intPreferencesKey("lastCount")
        val LAST_PEAK_SWING_SPEED = floatPreferencesKey("lastPeakSwingSpeedRadPerSecond")
        val TOTAL_COUNT = intPreferencesKey("totalCount")
    }
}
