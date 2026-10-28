
package com.example.movilmanupuladora.utils

import android.content.Context
import android.content.SharedPreferences
import com.example.movilmanupuladora.data.api.RetrofitClient

class SessionManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREF_NAME = "SIRAE_SESSION"
        private const val KEY_TOKEN = "auth_token"
        private const val KEY_USER_NAME = "user_name"
        private const val KEY_USER_ROLE = "user_role"
        private const val KEY_USER_EMAIL = "user_email"
        private const val KEY_USER_PHONE = "user_phone"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_SEDE_ID = "sede_id"
        private const val KEY_SEDE_NOMBRE = "sede_nombre"

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

    // Guarda el token JWT de acceso.
    fun saveAuthToken(token: String) {
        prefs.edit().putString(KEY_TOKEN, token).apply()
        RetrofitClient.authToken = token
    }

    // Recupera el token guardado.
    fun fetchAuthToken(): String? {
        val token = prefs.getString(KEY_TOKEN, null)

        if (!token.isNullOrEmpty()) {
            RetrofitClient.authToken = token
        }

        return token
    }

    // Guarda los datos del usuario autenticado y su sede.
    fun saveUserData(
        name: String,
        role: String?,
        email: String? = null,
        idUsuario: Int? = null,
        idSede: Int? = null,
        sedeNombre: String? = null
    ) {
        val editor = prefs.edit()

        editor.putString(KEY_USER_NAME, name)
        editor.putString(KEY_USER_ROLE, role ?: "Manipuladora PAE")

        if (!email.isNullOrEmpty()) {
            editor.putString(KEY_USER_EMAIL, email)
        }

        if (idUsuario != null && idUsuario > 0) {
            editor.putInt(KEY_USER_ID, idUsuario)
        }

        if (idSede != null && idSede > 0) {
            editor.putInt(KEY_SEDE_ID, idSede)
        } else {
            editor.remove(KEY_SEDE_ID)
        }

        if (!sedeNombre.isNullOrBlank()) {
            editor.putString(KEY_SEDE_NOMBRE, sedeNombre)
        } else {
            editor.remove(KEY_SEDE_NOMBRE)
        }

        editor.apply()
    }

    fun getUserId(): Int =
        prefs.getInt(KEY_USER_ID, -1)

    // Devuelve el ID de la sede asociada al usuario.
    fun getSedeId(): Int =
        prefs.getInt(KEY_SEDE_ID, -1)

    fun getSedeNombre(): String? =
        prefs.getString(KEY_SEDE_NOMBRE, null)

    fun saveUserPhone(phone: String) {
        prefs.edit().putString(KEY_USER_PHONE, phone).apply()
    }

    // Limpia completamente la sesión.
    fun clearSession() {
        prefs.edit().clear().apply()
        RetrofitClient.authToken = null
    }

    // Comprueba si hay una sesión activa.
    fun isLoggedIn(): Boolean {
        return !fetchAuthToken().isNullOrEmpty()
    }

    fun getUserName(): String? =
        prefs.getString(KEY_USER_NAME, null)

    fun getUserRole(): String? =
        prefs.getString(KEY_USER_ROLE, null)

    fun getUserEmail(): String? =
        prefs.getString(KEY_USER_EMAIL, null)

    fun getUserPhone(): String? =
        prefs.getString(KEY_USER_PHONE, null)

    // Guarda el plato seleccionado.
    fun savePlatoSeleccionado(nombre: String, id: Int? = null) {
        val editor = prefs.edit()
            .putString("plato_seleccionado_nombre", nombre)

        if (id != null) {
            editor.putInt("plato_seleccionado_id", id)
        } else {
            editor.remove("plato_seleccionado_id")
        }

        editor.apply()
    }

    fun getPlatoSeleccionadoNombre(): String =
        prefs.getString(
            "plato_seleccionado_nombre",
            "Arroz con pollo"
        ) ?: "Arroz con pollo"

    fun getPlatoSeleccionadoId(): Int =
        prefs.getInt("plato_seleccionado_id", -1)

    /**
     * Obtiene el plato seleccionado por la Jefa para el turno actual.
     */
    fun getPlatoJefa(context: Context): String {
        val turnoPref = context.getSharedPreferences(
            "TURNO_PAE",
            Context.MODE_PRIVATE
        )

        return turnoPref.getString("plato_nombre", null)
            ?: context.getSharedPreferences(
                "ManipuladoraJefe",
                Context.MODE_PRIVATE
            ).getString("plato_actual", "Arroz con Pollo")
            ?: "Arroz con Pollo"
    }

    /**
     * Obtiene el ID del plato que seleccionó la Jefa.
     */
    fun getPlatoJefaId(context: Context): Int {
        return context.getSharedPreferences(
            "TURNO_PAE",
            Context.MODE_PRIVATE
        ).getInt("plato_id", -1)
    }

    /**
     * Busca el componente asignado al usuario actualmente autenticado.
     *
     * Las asignaciones de la Jefa se consultan en SharedPreferences
     * "ManipuladoraJefe".
     */
    fun getComponenteAsignado(context: Context): String? {
        val jefaPrefs = context.getSharedPreferences(
            "ManipuladoraJefe",
            Context.MODE_PRIVATE
        )

        val rol = getUserRole()?.lowercase() ?: ""
        val compJefa = jefaPrefs.getString("comp_jefa", null)
        val correoUser = getUserEmail()?.trim()?.lowercase()
        val nombreUser = getUserName()?.trim()?.lowercase()

        // Si el usuario es la Jefa o tiene rol de coordinación.
        if (!compJefa.isNullOrBlank() &&
            (
                rol.contains("jef") ||
                rol.contains("coord") ||
                nombreUser?.contains("isa") == true ||
                correoUser?.contains("isa") == true
            )
        ) {
            return compJefa
        }

        // 1. Buscar por ID del usuario.
        val myUid = getUserId()

        if (myUid > 0) {
            val compUid = jefaPrefs.getString("comp_uid_$myUid", null)

            if (!compUid.isNullOrBlank()) {
                return compUid
            }
        }

        // 2. Buscar por correo electrónico.
        if (!correoUser.isNullOrBlank()) {
            val keyCorreo = "comp_correo_" +
                correoUser.replace("@", "_").replace(".", "_")

            val comp = jefaPrefs.getString(keyCorreo, null)

            if (!comp.isNullOrBlank()) {
                return comp
            }
        }

        // 3. Buscar por nombre.
        if (!nombreUser.isNullOrBlank()) {
            val keyNombre = "comp_" + nombreUser.replace(" ", "_")
            val comp = jefaPrefs.getString(keyNombre, null)

            if (!comp.isNullOrBlank()) {
                return comp
            }

            // Buscar también el nombre sin tildes.
            val nombreSinTilde = java.text.Normalizer
                .normalize(
                    nombreUser,
                    java.text.Normalizer.Form.NFD
                )
                .replace(
                    "\\p{InCombiningDiacriticalMarks}+".toRegex(),
                    ""
                )
                .replace(" ", "_")

            val compSinTilde = jefaPrefs.getString(
                "comp_$nombreSinTilde",
                null
            )

            if (!compSinTilde.isNullOrBlank()) {
                return compSinTilde
            }

            // Buscar por el primer nombre.
            val primerNombre =
                nombreUser.split(" ").firstOrNull()?.trim() ?: ""

            if (primerNombre.isNotBlank()) {
                val compPrimerNombre = jefaPrefs.getString(
                    "comp_$primerNombre",
                    null
                )

                if (!compPrimerNombre.isNullOrBlank()) {
                    return compPrimerNombre
                }
            }

            // Buscar coincidencias parciales entre las claves.
            val all = jefaPrefs.all

            for ((key, value) in all) {
                if (
                    key.startsWith("comp_") &&
                    !key.startsWith("comp_correo_") &&
                    value is String &&
                    value.isNotBlank()
                ) {
                    val nombreEnClave = key.removePrefix("comp_")

                    if (
                        nombreUser.contains(nombreEnClave) ||
                        nombreEnClave.contains(nombreUser) ||
                        nombreSinTilde.contains(nombreEnClave) ||
                        nombreEnClave.contains(nombreSinTilde)
                    ) {
                        return value
                    }
                }
            }
        }

        // 4. Compatibilidad con asignaciones de Andrea Sánchez.
        if (
            correoUser?.contains("andrea") == true ||
            nombreUser?.contains("andrea") == true
        ) {
            val all = jefaPrefs.all

            for ((key, value) in all) {
                if (
                    (key.contains("andrea") || key.contains("sanchez")) &&
                    value is String &&
                    value.isNotBlank()
                ) {
                    return value
                }
            }
        }

        return null
    }
}
