package com.knightspace.airswing.domain

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SessionStoreTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun `first launch has no setup and saved setup is restored`() = runBlocking {
        val store = SessionStore(dataStore())
        assertFalse(store.state.first().hasSetup)

        store.saveSetup(Handedness.LEFT)

        val restored = store.state.first()
        assertTrue(restored.hasSetup)
        assertEquals(Handedness.LEFT, restored.handedness)
    }

    @Test
    fun `sessions persist timestamps last count and cumulative count`() = runBlocking {
        val store = SessionStore(dataStore())

        store.saveSession(SessionSummary(startedAtMs = 100, endedAtMs = 300, strokeCount = 4))
        store.saveSession(SessionSummary(startedAtMs = 400, endedAtMs = 900, strokeCount = 3))

        val state = store.state.first()
        assertEquals(3, state.lastCount)
        assertEquals(7, state.totalCount)
        assertEquals(400, state.lastSession?.startedAtMs)
        assertEquals(900, state.lastSession?.endedAtMs)
    }

    private fun dataStore(): DataStore<Preferences> = PreferenceDataStoreFactory.create {
        temporaryFolder.newFile("airswing-${System.nanoTime()}.preferences_pb")
    }
}
