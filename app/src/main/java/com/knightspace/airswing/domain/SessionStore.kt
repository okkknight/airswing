package com.knightspace.airswing.domain

import android.content.Context

class SessionStore(context: Context) {
    private val prefs = context.getSharedPreferences("airswing", Context.MODE_PRIVATE)
    val hasSetup get() = prefs.getBoolean("hasSetup", false)
    val lastCount get() = prefs.getInt("lastCount", 0)
    val totalCount get() = prefs.getInt("totalCount", 0)
    fun saveSetup(hand: String) = prefs.edit().putBoolean("hasSetup", true).putString("hand", hand).apply()
    fun saveSession(count: Int) = prefs.edit().putInt("lastCount", count).putInt("totalCount", totalCount + count).apply()
}
