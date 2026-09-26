package com.example.movilmanupuladora.utils

import android.content.Context
import android.content.SharedPreferences
import com.example.movilmanupuladora.data.api.RetrofitClient

class SessionManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREF_NAME = "SIRAE_SESSION"
        private const val KEY_TOKEN = "auth_token"
        private const val KEY_USER_NAME = "user_name"
        private const val KEY_USER_ROLE = "user_role"

        private const val KEY_USER_EMAIL = "user_email"
        private const val KEY_USER_PHONE = "user_phone"

        var currentToken: String?
            get() = RetrofitClient.authToken
            set(value) {
                RetrofitClient.authToken = value
            }

        fun saveToken(context: Context, token: String?) {
            currentToken = token
            SessionManager(context).saveAuthToken(token ?: "")
        }

        fun getToken(context: Context): String? {
            val token = SessionManager(context).fetchAuthToken()
            currentToken = token
            return token
        }
    }

    /**
     * Guarda el token JWT de acceso entregado por el backend
     */
    fun saveAuthToken(token: String) {
        prefs.edit().putString(KEY_TOKEN, token).apply()
        RetrofitClient.authToken = token
    }

    /**
     * Recupera el token guardado en disco
     */
    fun fetchAuthToken(): String? {
        val token = prefs.getString(KEY_TOKEN, null)
        if (!token.isNullOrEmpty()) {
            RetrofitClient.authToken = token
        }
        return token
    }

    /**
     * Guarda el nombre, correo y rol del usuario autenticado
     */
    fun saveUserData(name: String, role: String?, email: String? = null) {
        val editor = prefs.edit()
        editor.putString(KEY_USER_NAME, name)
        editor.putString(KEY_USER_ROLE, role ?: "Manipuladora PAE")
        if (!email.isNullOrEmpty()) {
            editor.putString(KEY_USER_EMAIL, email)
        }
        editor.apply()
    }

    fun saveUserPhone(phone: String) {
        prefs.edit().putString(KEY_USER_PHONE, phone).apply()
    }

    /**
     * Limpia completamente la sesión
     */
    fun clearSession() {
        prefs.edit().clear().apply()
        RetrofitClient.authToken = null
    }

    /**
     * Verifica si hay un token activo guardado
     */
    fun isLoggedIn(): Boolean {
        return !fetchAuthToken().isNullOrEmpty()
    }

    fun getUserName(): String? = prefs.getString(KEY_USER_NAME, null)
    fun getUserRole(): String? = prefs.getString(KEY_USER_ROLE, null)
    fun getUserEmail(): String? = prefs.getString(KEY_USER_EMAIL, null)
    fun getUserPhone(): String? = prefs.getString(KEY_USER_PHONE, null)
}