package com.knightspace.airswing.domain

enum class StartDestination { SETUP, PLAY }

fun startDestination(hasSetup: Boolean): StartDestination =
    if (hasSetup) StartDestination.PLAY else StartDestination.SETUP
