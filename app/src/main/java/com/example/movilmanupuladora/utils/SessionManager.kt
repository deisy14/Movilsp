package com.example.movilmanupuladora.utils

import android.content.Context
import android.content.SharedPreferences

class SessionManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREF_NAME = "SIRAE_SESSION"
        private const val KEY_TOKEN = "auth_token"
        private const val KEY_USER_NAME = "user_name"
        private const val KEY_USER_ROLE = "user_role"
    }

    /**
     * Guarda el token JWT de acceso entregado por el backend
     */
    fun saveAuthToken(token: String) {
        prefs.edit().putString(KEY_TOKEN, token).apply()
    }

    /**
     * Recupera el token guardado en disco
     */
    fun fetchAuthToken(): String? {
        return prefs.getString(KEY_TOKEN, null)
    }

    /**
     * Guarda el nombre y el rol del usuario autenticado
     */
    fun saveUserData(name: String, role: String?) {
        val editor = prefs.edit()
        editor.putString(KEY_USER_NAME, name)
        editor.putString(KEY_USER_ROLE, role ?: "Sin Rol")
        editor.apply()
    }

    /**
     * Limpia completamente la sesión (ideal para el botón Cerrar Sesión)
     */
    fun clearSession() {
        prefs.edit().clear().apply()
    }

    /**
     * Verifica si hay un token activo guardado
     */
    fun isLoggedIn(): Boolean {
        return !fetchAuthToken().isNullOrEmpty()
    }

    fun getUserName(): String? = prefs.getString(KEY_USER_NAME, null)
    fun getUserRole(): String? = prefs.getString(KEY_USER_ROLE, null)
}