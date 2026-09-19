package com.example.movilmanupuladora.utils

import android.content.Context
import android.content.SharedPreferences

object SessionManager {
    private const val PREF_NAME = "SIRAE_SESSION_PREF"
    private const val KEY_JWT = "KEY_JWT_TOKEN"

    var currentToken: String? = null

    fun saveToken(context: Context, token: String?) {
        currentToken = token
        val prefs: SharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_JWT, token).apply()
    }

    fun getToken(context: Context): String? {
        if (currentToken == null) {
            val prefs: SharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            currentToken = prefs.getString(KEY_JWT, null)
        }
        return currentToken
    }

    fun clear(context: Context) {
        currentToken = null
        val prefs: SharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
    }
}