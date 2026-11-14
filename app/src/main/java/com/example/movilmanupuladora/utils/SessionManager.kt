
package com.example.movilmanupuladora.utils

import android.content.Context
import android.content.SharedPreferences
import com.example.movilmanupuladora.data.api.RetrofitClient
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SessionManager(context: Context) {

    private val appContext: Context = context.applicationContext
    private val prefs: SharedPreferences =
        appContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

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
        private const val KEY_INSTITUCION_ID = "institucion_id"
        private const val KEY_INSTITUCION_NOMBRE = "institucion_nombre"

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

    /**
     * Valida si existe un token de sesión y si la hora actual está dentro
     * de la ventana de sesión permitida (7:00 AM a 7:00 PM).
     */
    fun esSesionValidaHorario(): Boolean {
        val token = fetchAuthToken()
        if (token.isNullOrBlank()) return false

        val cal = java.util.Calendar.getInstance()
        val hora = cal.get(java.util.Calendar.HOUR_OF_DAY)
        val minuto = cal.get(java.util.Calendar.MINUTE)

        // De 7:00 AM a 7:00 PM (19:00 inclusive)
        return (hora in 7..18) || (hora == 19 && minuto == 0)
    }

    // Guarda los datos del usuario autenticado, su sede e institución.
    fun saveUserData(
        name: String,
        role: String?,
        email: String? = null,
        idUsuario: Int? = null,
        idSede: Int? = null,
        sedeNombre: String? = null,
        idInstitucion: Int? = null,
        institucionNombre: String? = null
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

        if (idInstitucion != null && idInstitucion > 0) {
            editor.putInt(KEY_INSTITUCION_ID, idInstitucion)
        } else {
            editor.remove(KEY_INSTITUCION_ID)
        }

        if (!institucionNombre.isNullOrBlank()) {
            editor.putString(KEY_INSTITUCION_NOMBRE, institucionNombre)
        } else {
            editor.remove(KEY_INSTITUCION_NOMBRE)
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

    fun getInstitucionId(): Int =
        prefs.getInt(KEY_INSTITUCION_ID, -1)

    fun getInstitucionNombre(): String? =
        prefs.getString(KEY_INSTITUCION_NOMBRE, null)

    /**
     * Retorna la institución y sede formateadas (ej: "I.E. Técnico San José · Sede Principal")
     */
    fun getUbicacionCompleta(): String {
        val inst = getInstitucionNombre()?.trim().orEmpty()
        val sede = getSedeNombre()?.trim().orEmpty()
        return when {
            inst.isNotBlank() && sede.isNotBlank() && !inst.equals(sede, ignoreCase = true) ->
                "$inst · $sede"
            sede.isNotBlank() -> sede
            inst.isNotBlank() -> inst
            else -> "Sede Principal"
        }
    }

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

    // Guarda el plato seleccionado en todas las preferencias de sincronización (SIRAE_SESSION, ManipuladoraJefe, TURNO_PAE)
    fun savePlatoSeleccionado(nombre: String, id: Int? = null) {
        val editor = prefs.edit()
            .putString("plato_seleccionado_nombre", nombre)

        if (id != null && id > 0) {
            editor.putInt("plato_seleccionado_id", id)
        } else {
            editor.remove("plato_seleccionado_id")
        }
        editor.apply()

        try {
            val jefaPrefs = appContext.getSharedPreferences("ManipuladoraJefe", Context.MODE_PRIVATE)
            val jefaEditor = jefaPrefs.edit().putString("plato_actual", nombre)
            if (id != null && id > 0) jefaEditor.putInt("plato_id", id)
            jefaEditor.apply()

            val turnoPrefs = appContext.getSharedPreferences("TURNO_PAE", Context.MODE_PRIVATE)
            val turnoEditor = turnoPrefs.edit().putString("plato_nombre", nombre)
            if (id != null && id > 0) turnoEditor.putInt("plato_id", id)
            turnoEditor.apply()
        } catch (_: Exception) { }
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
        val jefaPref = context.getSharedPreferences(
            "ManipuladoraJefe",
            Context.MODE_PRIVATE
        )

        return turnoPref.getString("plato_nombre", null)
            ?: jefaPref.getString("plato_actual", null)
            ?: prefs.getString("plato_seleccionado_nombre", null)
            ?: "Arroz con Pollo"
    }

    /**
     * Guarda el plato asignado a un servicio específico (Desayuno, Almuerzo, Merienda).
     */
    fun savePlatoServicio(context: Context, servicio: String, nombre: String, id: Int? = null) {
        val servKey = when (servicio.lowercase()) {
            "desayuno" -> "desayuno"
            "almuerzo" -> "almuerzo"
            "merienda" -> "merienda"
            else -> "desayuno"
        }
        val turnoPref = context.getSharedPreferences("TURNO_PAE", Context.MODE_PRIVATE).edit()
        val jefaPref = context.getSharedPreferences("ManipuladoraJefe", Context.MODE_PRIVATE).edit()

        turnoPref.putString("plato_$servKey", nombre)
        jefaPref.putString("plato_$servKey", nombre)

        if (id != null && id > 0) {
            turnoPref.putInt("plato_${servKey}_id", id)
            jefaPref.putInt("plato_${servKey}_id", id)
        } else {
            turnoPref.remove("plato_${servKey}_id")
            jefaPref.remove("plato_${servKey}_id")
        }
        turnoPref.apply()
        jefaPref.apply()
    }

    /**
     * Elimina la asignación de un servicio específico.
     */
    fun desasignarPlatoServicio(context: Context, servicio: String) {
        val servKey = when (servicio.lowercase()) {
            "desayuno" -> "desayuno"
            "almuerzo" -> "almuerzo"
            "merienda" -> "merienda"
            else -> "desayuno"
        }
        context.getSharedPreferences("TURNO_PAE", Context.MODE_PRIVATE).edit()
            .remove("plato_$servKey")
            .remove("plato_${servKey}_id")
            .apply()
        context.getSharedPreferences("ManipuladoraJefe", Context.MODE_PRIVATE).edit()
            .remove("plato_$servKey")
            .remove("plato_${servKey}_id")
            .apply()
    }

    /**
     * Obtiene el nombre del plato asignado a un servicio específico.
     */
    fun getPlatoServicio(context: Context, servicio: String): String? {
        val servKey = when (servicio.lowercase()) {
            "desayuno" -> "desayuno"
            "almuerzo" -> "almuerzo"
            "merienda" -> "merienda"
            else -> "desayuno"
        }
        val turnoVal = context.getSharedPreferences("TURNO_PAE", Context.MODE_PRIVATE)
            .getString("plato_$servKey", null)
        if (!turnoVal.isNullOrBlank()) return turnoVal

        return context.getSharedPreferences("ManipuladoraJefe", Context.MODE_PRIVATE)
            .getString("plato_$servKey", null)
    }

    /**
     * Obtiene el ID del plato asignado a un servicio específico.
     */
    fun getPlatoServicioId(context: Context, servicio: String): Int {
        val servKey = when (servicio.lowercase()) {
            "desayuno" -> "desayuno"
            "almuerzo" -> "almuerzo"
            "merienda" -> "merienda"
            else -> "desayuno"
        }
        val tId = context.getSharedPreferences("TURNO_PAE", Context.MODE_PRIVATE)
            .getInt("plato_${servKey}_id", -1)
        if (tId != -1) return tId

        return context.getSharedPreferences("ManipuladoraJefe", Context.MODE_PRIVATE)
            .getInt("plato_${servKey}_id", -1)
    }

    /**
     * Guarda el plato asignado a un día específico (fecha yyyy-MM-dd) y servicio (Desayuno, Almuerzo, Merienda).
     */
    fun savePlatoDiaServicio(context: Context, fecha: String, servicio: String, nombre: String, id: Int? = null) {
        val servKey = when (servicio.lowercase()) {
            "desayuno" -> "desayuno"
            "almuerzo" -> "almuerzo"
            "merienda" -> "merienda"
            else -> "desayuno"
        }
        val turnoPref = context.getSharedPreferences("TURNO_PAE", Context.MODE_PRIVATE).edit()
        val jefaPref = context.getSharedPreferences("ManipuladoraJefe", Context.MODE_PRIVATE).edit()

        turnoPref.putString("plan_${fecha}_${servKey}", nombre)
        jefaPref.putString("plan_${fecha}_${servKey}", nombre)

        if (id != null && id > 0) {
            turnoPref.putInt("plan_${fecha}_${servKey}_id", id)
            jefaPref.putInt("plan_${fecha}_${servKey}_id", id)
        } else {
            turnoPref.remove("plan_${fecha}_${servKey}_id")
            jefaPref.remove("plan_${fecha}_${servKey}_id")
        }
        turnoPref.apply()
        jefaPref.apply()

        // También actualiza la clave general del servicio para que la manipuladora lo vea inmediatamente
        savePlatoServicio(context, servicio, nombre, id)
    }

    /**
     * Desasigna el plato de un día y servicio específico.
     */
    fun desasignarPlatoDiaServicio(context: Context, fecha: String, servicio: String) {
        val servKey = when (servicio.lowercase()) {
            "desayuno" -> "desayuno"
            "almuerzo" -> "almuerzo"
            "merienda" -> "merienda"
            else -> "desayuno"
        }
        context.getSharedPreferences("TURNO_PAE", Context.MODE_PRIVATE).edit()
            .remove("plan_${fecha}_${servKey}")
            .remove("plan_${fecha}_${servKey}_id")
            .apply()
        context.getSharedPreferences("ManipuladoraJefe", Context.MODE_PRIVATE).edit()
            .remove("plan_${fecha}_${servKey}")
            .remove("plan_${fecha}_${servKey}_id")
            .apply()

        desasignarPlatoServicio(context, servicio)
    }

    /**
     * Obtiene el plato de un día y servicio específico.
     */
    fun getPlatoDiaServicio(context: Context, fecha: String, servicio: String): String? {
        val servKey = when (servicio.lowercase()) {
            "desayuno" -> "desayuno"
            "almuerzo" -> "almuerzo"
            "merienda" -> "merienda"
            else -> "desayuno"
        }
        val tVal = context.getSharedPreferences("TURNO_PAE", Context.MODE_PRIVATE)
            .getString("plan_${fecha}_${servKey}", null)
        if (!tVal.isNullOrBlank()) return tVal

        val jVal = context.getSharedPreferences("ManipuladoraJefe", Context.MODE_PRIVATE)
            .getString("plan_${fecha}_${servKey}", null)
        if (!jVal.isNullOrBlank()) return jVal

        return null
    }

    /**
     * Obtiene el ID del plato de un día y servicio específico.
     */
    fun getPlatoDiaServicioId(context: Context, fecha: String, servicio: String): Int {
        val servKey = when (servicio.lowercase()) {
            "desayuno" -> "desayuno"
            "almuerzo" -> "almuerzo"
            "merienda" -> "merienda"
            else -> "desayuno"
        }
        val tId = context.getSharedPreferences("TURNO_PAE", Context.MODE_PRIVATE)
            .getInt("plan_${fecha}_${servKey}_id", -1)
        if (tId != -1) return tId

        return context.getSharedPreferences("ManipuladoraJefe", Context.MODE_PRIVATE)
            .getInt("plan_${fecha}_${servKey}_id", -1)
    }

    /**
     * Obtiene el ID del plato que seleccionó la Jefa.
     */
    fun getPlatoJefaId(context: Context): Int {
        val turnoId = context.getSharedPreferences("TURNO_PAE", Context.MODE_PRIVATE).getInt("plato_id", -1)
        if (turnoId > 0) return turnoId

        val jefaId = context.getSharedPreferences("ManipuladoraJefe", Context.MODE_PRIVATE).getInt("plato_id", -1)
        if (jefaId > 0) return jefaId

        return prefs.getInt("plato_seleccionado_id", -1)
    }

    /**
     * Busca el componente asignado al usuario actualmente autenticado.
     *
     * Permite consultar por servicio específico (Desayuno, Almuerzo, Merienda)
     * o general del día.
     */
    fun getComponenteAsignado(context: Context, servicio: String? = null): String? {
        val jefaPrefs = context.getSharedPreferences(
            "ManipuladoraJefe",
            Context.MODE_PRIVATE
        )
        val asigPrefs = context.getSharedPreferences(
            "ASIGNACIONES_PAE",
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

        val myUid = getUserId()
        val keyCorreo = correoUser?.replace("@", "_")?.replace(".", "_") ?: ""
        val fechaHoy = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val serv = servicio ?: "Desayuno"

        // PRIORIDAD 1: Asignación oficial guardada por la Jefa para la fecha de hoy y el servicio actual
        val prefixServ = "asig_${fechaHoy}_${serv}_"
        val prefixCorreoServ = "asig_correo_${fechaHoy}_${serv}_"
        val prefixUidServ = "asig_uid_${fechaHoy}_${serv}_"

        for ((k, v) in asigPrefs.all) {
            if (k.startsWith(prefixCorreoServ) && v is String && correoUser != null) {
                if (v.trim().equals(correoUser, ignoreCase = true) || correoUser.contains(v.trim(), ignoreCase = true)) {
                    return k.removePrefix(prefixCorreoServ)
                }
            }
            if (k.startsWith(prefixUidServ) && v is Int && myUid > 0 && v == myUid) {
                return k.removePrefix(prefixUidServ)
            }
            if (k.startsWith(prefixServ) && v is String) {
                val cocineraNombre = v.trim().lowercase()
                if (nombreUser != null && (cocineraNombre.contains(nombreUser) || nombreUser.contains(cocineraNombre))) {
                    return k.removePrefix(prefixServ)
                }
                if (correoUser?.contains("pool") == true && cocineraNombre.contains("pool")) {
                    return k.removePrefix(prefixServ)
                }
                if (correoUser?.contains("deis") == true && cocineraNombre.contains("deis")) {
                    return k.removePrefix(prefixServ)
                }
            }
        }

        // PRIORIDAD 2: Asignación por servicio específico en claves comp_correo_ / comp_uid_
        if (correoUser?.contains("pool") == true || nombreUser?.contains("pool") == true || myUid == 21) {
            val pServ = asigPrefs.getString("asig_pool_${fechaHoy}_${serv}", null)
                ?: asigPrefs.getString("comp_pool_${serv}", null)
                ?: asigPrefs.getString("comp_uid_${serv}_21", null)
                ?: asigPrefs.getString("comp_pool", null)
                ?: asigPrefs.getString("comp_uid_21", null)
            if (!pServ.isNullOrBlank()) return pServ
        }
        if (correoUser?.contains("deis") == true || nombreUser?.contains("deis") == true || myUid == 24) {
            val dServ = asigPrefs.getString("asig_deis_${fechaHoy}_${serv}", null)
                ?: asigPrefs.getString("comp_deis_${serv}", null)
                ?: asigPrefs.getString("comp_uid_${serv}_24", null)
                ?: asigPrefs.getString("comp_deis", null)
                ?: asigPrefs.getString("comp_uid_24", null)
            if (!dServ.isNullOrBlank()) return dServ
        }

        if (keyCorreo.isNotBlank()) {
            val cServ = asigPrefs.getString("comp_correo_${serv}_$keyCorreo", null)
                ?: jefaPrefs.getString("comp_correo_${serv}_$keyCorreo", null)
            if (!cServ.isNullOrBlank()) return cServ
        }
        if (myUid > 0) {
            val cServUid = asigPrefs.getString("comp_uid_${serv}_$myUid", null)
                ?: jefaPrefs.getString("comp_uid_${serv}_$myUid", null)
            if (!cServUid.isNullOrBlank()) return cServUid
        }

        // PRIORIDAD 3: Asignación en cualquier servicio de la fecha de hoy
        val prefixHoy = "asig_${fechaHoy}_"
        val prefixCorreoHoy = "asig_correo_${fechaHoy}_"
        for ((k, v) in asigPrefs.all) {
            if (k.startsWith(prefixCorreoHoy) && v is String && correoUser != null) {
                if (v.trim().equals(correoUser, ignoreCase = true)) {
                    val resto = k.removePrefix(prefixCorreoHoy)
                    val partes = resto.split("_")
                    if (partes.size >= 2) return partes.subList(1, partes.size).joinToString("_")
                }
            }
            if (k.startsWith(prefixHoy) && !k.startsWith("asig_correo_") && !k.startsWith("asig_uid_") && v is String) {
                val cocineraNombre = v.trim().lowercase()
                if (nombreUser != null && (cocineraNombre.contains(nombreUser) || nombreUser.contains(cocineraNombre))) {
                    val resto = k.removePrefix(prefixHoy)
                    val partes = resto.split("_")
                    if (partes.size >= 2) return partes.subList(1, partes.size).joinToString("_")
                }
                if (correoUser?.contains("pool") == true && cocineraNombre.contains("pool")) {
                    val resto = k.removePrefix(prefixHoy)
                    val partes = resto.split("_")
                    if (partes.size >= 2) return partes.subList(1, partes.size).joinToString("_")
                }
                if (correoUser?.contains("deis") == true && cocineraNombre.contains("deis")) {
                    val resto = k.removePrefix(prefixHoy)
                    val partes = resto.split("_")
                    if (partes.size >= 2) return partes.subList(1, partes.size).joinToString("_")
                }
            }
        }

        // PRIORIDAD 4: Claves directas comp_correo_ / comp_uid_
        if (keyCorreo.isNotBlank()) {
            val cGen = asigPrefs.getString("comp_correo_$keyCorreo", null)
                ?: jefaPrefs.getString("comp_correo_$keyCorreo", null)
            if (!cGen.isNullOrBlank()) return cGen
        }
        if (myUid > 0) {
            val cGenUid = asigPrefs.getString("comp_uid_$myUid", null)
                ?: jefaPrefs.getString("comp_uid_$myUid", null)
            if (!cGenUid.isNullOrBlank()) return cGenUid
        }

        // PRIORIDAD 5: Mapeo por ID conocido o correo de las manipuladoras
        val uidKnown = when {
            correoUser?.contains("pool") == true || nombreUser?.contains("pool") == true -> 21
            correoUser?.contains("deis") == true || nombreUser?.contains("deis") == true -> 24
            correoUser?.contains("andrea") == true || nombreUser?.contains("andrea") == true -> 20
            correoUser?.contains("andres") == true || nombreUser?.contains("andres") == true -> 30
            else -> null
        }
        if (uidKnown != null) {
            val compK = asigPrefs.getString("comp_uid_${serv}_$uidKnown", null)
                ?: asigPrefs.getString("comp_uid_$uidKnown", null)
                ?: jefaPrefs.getString("comp_uid_$uidKnown", null)
            if (!compK.isNullOrBlank()) return compK
        }

        return null
    }
}
