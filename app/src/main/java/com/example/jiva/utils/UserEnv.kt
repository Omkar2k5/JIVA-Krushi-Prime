package com.example.jiva.utils

import android.content.Context

/**
 * Lightweight environment-like store for app-wide values.
 * Uses SharedPreferences under the hood.
 */
object UserEnv {
    private const val PREF_NAME = "jiva_env_prefs"
    private const val KEY_USER_ID = "user_id"

    fun setUserId(context: Context, userId: String) {
        context.applicationContext
            .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_USER_ID, userId)
            .apply()
    }

    fun getUserId(context: Context): String? {
        return context.applicationContext
            .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getString(KEY_USER_ID, null)
    }

    fun clearUserId(context: Context) {
        context.applicationContext
            .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit()
            .remove(KEY_USER_ID)
            .apply()
    }
}