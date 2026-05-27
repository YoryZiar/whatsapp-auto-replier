package com.example

import android.content.Context
import android.content.SharedPreferences

object SettingsManager {
    private const val PREFS_NAME = "autoreply_prefs"
    private const val KEY_IS_ACTIVE = "is_active"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun isServiceActive(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_IS_ACTIVE, true) // Secara default aktif
    }

    fun setServiceActive(context: Context, isActive: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_IS_ACTIVE, isActive).apply()
    }
}
