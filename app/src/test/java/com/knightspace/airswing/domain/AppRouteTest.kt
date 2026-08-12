package com.knightspace.airswing.domain

import org.junit.Test
import kotlin.test.assertEquals

class AppRouteTest {
    @Test
    fun `first launch routes start action to setup`() {
        assertEquals(StartDestination.SETUP, startDestination(hasSetup = false))
    }

    @Test
    fun `returning user routes start action directly to play`() {
        assertEquals(StartDestination.PLAY, startDestination(hasSetup = true))
    }
}
