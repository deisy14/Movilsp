
package com.example.movilmanupuladora.data.model

import com.google.gson.annotations.SerializedName

// Modelo para enviar o recibir un usuario completo
data class Usuario(
    @SerializedName("id_usuario", alternate = ["id", "pk", "idUsuario"])
    val idUsuario: Int? = null,

    val nombre: String = "",
    val apellido: String = "",
    val correo: String = "",

    @SerializedName("tipo_documento")
    val tipoDocumento: String = "",

    @SerializedName("numero_documento")
    val numeroDocumento: String = "",

    // ID del rol seleccionado al registrar
    @SerializedName("rol", alternate = ["rol_id", "id_rol"])
    val rol: Int? = null,

    // Nombre del rol (viene en el listado de usuarios)
    @SerializedName("nombre_rol", alternate = ["rol_nombre", "nombreRol", "role"])
    val nombreRol: String? = null,

    val password: String? = null,

    @SerializedName("is_active", alternate = ["activo", "active", "isActive"])
    val isActive: Boolean? = true,

    @SerializedName("estado", alternate = ["estado_display", "status"])
    val estado: String? = null,

    @SerializedName("is_staff")
    val isStaff: Boolean? = false,

    @SerializedName("id_sede", alternate = ["sede_id", "idSede"])
    val idSede: Int? = null,

    @SerializedName("sede_nombre", alternate = ["nombre_sede", "sedeTexto", "nombreSede", "sede_display", "sede"])
    val sedeNombre: String? = null,

    @SerializedName("id_institucion", alternate = ["institucion_id", "idInstitucion"])
    val idInstitucion: Int? = null,

    @SerializedName("institucion_nombre", alternate = ["nombre_institucion", "institucionTexto", "nombreInstitucion", "institucion"])
    val institucionNombre: String? = null
) {
    /** Nombre completo del usuario */
    val nombreCompleto: String
        get() = if (apellido.isNotBlank()) "$nombre $apellido".trim() else nombre.trim()
}

/**
 * Deserializador robusto para Usuario que soporta tanto IDs planos como objetos anidados
 * para rol, sede e institución, así como múltiples esquemas de nombres y correos de Django REST.
 */
class UsuarioDeserializer : com.google.gson.JsonDeserializer<Usuario> {
    override fun deserialize(
        json: com.google.gson.JsonElement?,
        typeOfT: java.lang.reflect.Type?,
        context: com.google.gson.JsonDeserializationContext?
    ): Usuario {
        if (json == null || !json.isJsonObject) {
            return Usuario()
        }
        val obj = json.asJsonObject

        fun getStr(vararg keys: String): String {
            for (k in keys) {
                if (obj.has(k) && !obj.get(k).isJsonNull) {
                    val elem = obj.get(k)
                    if (elem.isJsonPrimitive) return elem.asString.trim()
                }
            }
            return ""
        }

        fun getIntOrNull(vararg keys: String): Int? {
            for (k in keys) {
                if (obj.has(k) && !obj.get(k).isJsonNull) {
                    val elem = obj.get(k)
                    if (elem.isJsonPrimitive) {
                        val num = elem.asString.toIntOrNull()
                        if (num != null) return num
                    } else if (elem.isJsonObject) {
                        val subObj = elem.asJsonObject
                        for (subKey in listOf("id_rol", "id_sede", "id_institucion", "id", "pk")) {
                            if (subObj.has(subKey) && !subObj.get(subKey).isJsonNull) {
                                val num = subObj.get(subKey).asString.toIntOrNull()
                                if (num != null) return num
                            }
                        }
                    }
                }
            }
            return null
        }

        fun getBoolOrNull(vararg keys: String): Boolean? {
            for (k in keys) {
                if (obj.has(k) && !obj.get(k).isJsonNull) {
                    val elem = obj.get(k)
                    if (elem.isJsonPrimitive) {
                        val s = elem.asString.lowercase()
                        if (s == "true" || s == "1") return true
                        if (s == "false" || s == "0") return false
                    }
                }
            }
            return null
        }

        val id = getIntOrNull("id_usuario", "id", "pk", "idUsuario")
        var nombre = getStr("nombre", "first_name")
        var apellido = getStr("apellido", "last_name")
        val nombreCompleto = getStr("nombre_completo", "nombreCompleto")

        if (nombre.isBlank() && nombreCompleto.isNotBlank()) {
            val partes = nombreCompleto.split("\\s+".toRegex())
            nombre = partes.firstOrNull() ?: ""
            apellido = if (partes.size > 1) partes.drop(1).joinToString(" ") else ""
        }

        val correo = getStr("correo", "email")
        val tipoDoc = getStr("tipo_documento", "tipo_doc")
        val numDoc = getStr("numero_documento", "documento_identidad", "num_documento")
        val password = getStr("password")

        val rolId = getIntOrNull("rol", "rol_id", "id_rol")
        var rolNombre = getStr("nombre_rol", "rol_nombre", "role")
        if (rolNombre.isBlank()) {
            if (obj.has("rol_detalle") && obj.get("rol_detalle").isJsonObject) {
                val rd = obj.getAsJsonObject("rol_detalle")
                rolNombre = rd.get("nombre_rol")?.takeIf { !it.isJsonNull }?.asString
                    ?: rd.get("nombre")?.takeIf { !it.isJsonNull }?.asString ?: ""
            } else if (obj.has("rol") && obj.get("rol").isJsonObject) {
                val ro = obj.getAsJsonObject("rol")
                rolNombre = ro.get("nombre_rol")?.takeIf { !it.isJsonNull }?.asString
                    ?: ro.get("nombre")?.takeIf { !it.isJsonNull }?.asString ?: ""
            }
        }

        val estado = getStr("estado", "estado_display", "status").takeIf { it.isNotBlank() }
        val isActive = getBoolOrNull("is_active", "activo", "active")
            ?: (estado == null || estado.equals("activo", ignoreCase = true) || estado.equals("activa", ignoreCase = true))

        val isStaff = getBoolOrNull("is_staff") ?: false

        val idSede = getIntOrNull("id_sede", "sede_id", "idSede", "sede")
        var sedeNombre = getStr("sede_nombre", "nombre_sede", "sedeTexto", "nombreSede", "sede_display", "sede")
        if (sedeNombre.isBlank()) {
            if (obj.has("id_sede") && obj.get("id_sede").isJsonObject) {
                val so = obj.getAsJsonObject("id_sede")
                sedeNombre = so.get("nombre_sede")?.takeIf { !it.isJsonNull }?.asString
                    ?: so.get("nombre")?.takeIf { !it.isJsonNull }?.asString ?: ""
            } else if (obj.has("sede") && obj.get("sede").isJsonObject) {
                val so = obj.getAsJsonObject("sede")
                sedeNombre = so.get("nombre_sede")?.takeIf { !it.isJsonNull }?.asString
                    ?: so.get("nombre")?.takeIf { !it.isJsonNull }?.asString ?: ""
            }
        }

        val idInst = getIntOrNull("id_institucion", "institucion_id", "idInstitucion", "institucion")
        var instNombre = getStr("institucion_nombre", "nombre_institucion", "institucionTexto", "nombreInstitucion", "institucion_display", "institucion")
        if (instNombre.isBlank()) {
            if (obj.has("id_institucion") && obj.get("id_institucion").isJsonObject) {
                val io = obj.getAsJsonObject("id_institucion")
                instNombre = io.get("nombre_institucion")?.takeIf { !it.isJsonNull }?.asString
                    ?: io.get("nombre")?.takeIf { !it.isJsonNull }?.asString ?: ""
            } else if (obj.has("institucion") && obj.get("institucion").isJsonObject) {
                val io = obj.getAsJsonObject("institucion")
                instNombre = io.get("nombre_institucion")?.takeIf { !it.isJsonNull }?.asString
                    ?: io.get("nombre")?.takeIf { !it.isJsonNull }?.asString ?: ""
            }
        }

        return Usuario(
            idUsuario = id,
            nombre = nombre,
            apellido = apellido,
            correo = correo,
            tipoDocumento = tipoDoc,
            numeroDocumento = numDoc,
            rol = rolId,
            nombreRol = rolNombre.takeIf { it.isNotBlank() },
            password = password.takeIf { it.isNotBlank() },
            isActive = isActive,
            estado = estado ?: if (isActive) "Activo" else "Inactivo",
            isStaff = isStaff,
            idSede = idSede,
            sedeNombre = sedeNombre.takeIf { it.isNotBlank() },
            idInstitucion = idInst,
            institucionNombre = instNombre.takeIf { it.isNotBlank() }
        )
    }
}

// Modelo para la petición de Login compatible con todos los esquemas del backend (Django/DRF)
data class LoginRequest(
    @SerializedName("correo")
    val correo: String,

    @SerializedName("username")
    val username: String = correo,

    @SerializedName("password")
    val password: String,

    @SerializedName("clave")
    val clave: String = password
)

// Modelo para la respuesta de Login
data class LoginResponse(
    @SerializedName("token")
    val token: String? = null,

    @SerializedName("access")
    val access: String? = null,

    @SerializedName("refresh")
    val refresh: String? = null,

    @SerializedName("usuario")
    val usuario: UsuarioInfo? = null
) {
    val authToken: String?
        get() = access ?: token
}

// Datos del usuario que entrega la respuesta del Login
data class UsuarioInfo(
    @SerializedName("id_usuario")
    val idUsuario: Int,

    val nombre: String = "",
    val apellido: String? = "",
    val correo: String = "",

    @SerializedName("rol", alternate = ["role", "tipo_usuario"])
    val rol: String? = null,

    @SerializedName("nombre_rol", alternate = ["rol_nombre", "nombreRol", "role_name"])
    val nombreRol: String? = null,

    @SerializedName("id_rol", alternate = ["rol_id"])
    val idRol: Int? = null,

    // Sede asignada al usuario autenticado
    @SerializedName("id_sede", alternate = ["sede_id", "idSede"])
    val idSede: Int? = null,

    @SerializedName("sede_nombre", alternate = ["nombre_sede", "sede_texto", "nombreSede", "sede_display", "sede"])
    val sedeNombre: String? = null,

    @SerializedName("id_institucion", alternate = ["institucion_id", "idInstitucion"])
    val idInstitucion: Int? = null,

    @SerializedName("institucion_nombre", alternate = ["nombre_institucion", "institucion_texto", "nombreInstitucion", "institucion"])
    val institucionNombre: String? = null,

    @SerializedName("is_superuser")
    val isSuperuser: Boolean? = false,

    @SerializedName("debe_cambiar_password")
    val debeCambiarPassword: Boolean? = false
) {
    /** Nombre completo del usuario */
    val nombreCompleto: String
        get() = listOf(nombre, apellido.orEmpty())
            .filter { it.isNotBlank() }
            .joinToString(" ")
            .trim()

    /** Rol unificado y normalizado */
    val rolEfectivo: String
        get() = (nombreRol ?: rol ?: "").trim()

    /** Sede o institución formateada para mostrar en cabecera */
    val sedeEfectiva: String?
        get() {
            val s = sedeNombre?.trim()
            val i = institucionNombre?.trim()
            return when {
                !s.isNullOrEmpty() && !i.isNullOrEmpty() && !s.equals(i, ignoreCase = true) -> "$i · $s"
                !s.isNullOrEmpty() -> s
                !i.isNullOrEmpty() -> i
                else -> null
            }
        }
}

/**
 * Deserializador robusto para UsuarioInfo en LoginResponse que evita NumberFormatException
 * cuando sede o institucion vienen como texto o como objetos.
 */
class UsuarioInfoDeserializer : com.google.gson.JsonDeserializer<UsuarioInfo> {
    override fun deserialize(
        json: com.google.gson.JsonElement?,
        typeOfT: java.lang.reflect.Type?,
        context: com.google.gson.JsonDeserializationContext?
    ): UsuarioInfo {
        if (json == null || !json.isJsonObject) {
            return UsuarioInfo(idUsuario = -1)
        }
        val obj = json.asJsonObject

        fun getStr(vararg keys: String): String {
            for (k in keys) {
                if (obj.has(k) && !obj.get(k).isJsonNull) {
                    val elem = obj.get(k)
                    if (elem.isJsonPrimitive) return elem.asString.trim()
                }
            }
            return ""
        }

        fun getIntOrNull(vararg keys: String): Int? {
            for (k in keys) {
                if (obj.has(k) && !obj.get(k).isJsonNull) {
                    val elem = obj.get(k)
                    if (elem.isJsonPrimitive) {
                        val num = elem.asString.toIntOrNull()
                        if (num != null) return num
                    } else if (elem.isJsonObject) {
                        val subObj = elem.asJsonObject
                        for (subKey in listOf("id_rol", "id_sede", "id_institucion", "id", "pk")) {
                            if (subObj.has(subKey) && !subObj.get(subKey).isJsonNull) {
                                val num = subObj.get(subKey).asString.toIntOrNull()
                                if (num != null) return num
                            }
                        }
                    }
                }
            }
            return null
        }

        fun getBoolOrNull(vararg keys: String): Boolean? {
            for (k in keys) {
                if (obj.has(k) && !obj.get(k).isJsonNull) {
                    val elem = obj.get(k)
                    if (elem.isJsonPrimitive) {
                        val s = elem.asString.lowercase()
                        if (s == "true" || s == "1") return true
                        if (s == "false" || s == "0") return false
                    }
                }
            }
            return null
        }

        val id = getIntOrNull("id_usuario", "id", "pk", "idUsuario") ?: -1
        var nombre = getStr("nombre", "first_name")
        var apellido = getStr("apellido", "last_name")
        val nombreCompleto = getStr("nombre_completo", "nombreCompleto")

        if (nombre.isBlank() && nombreCompleto.isNotBlank()) {
            val partes = nombreCompleto.split("\\s+".toRegex())
            nombre = partes.firstOrNull() ?: ""
            apellido = if (partes.size > 1) partes.drop(1).joinToString(" ") else ""
        }

        val correo = getStr("correo", "email")
        var rolStr = getStr("rol", "role", "tipo_usuario")
        val idRol = getIntOrNull("id_rol", "rol_id")
        var nombreRol = getStr("nombre_rol", "rol_nombre", "role_name")

        if (nombreRol.isBlank() && obj.has("rol") && obj.get("rol").isJsonObject) {
            val ro = obj.getAsJsonObject("rol")
            nombreRol = ro.get("nombre_rol")?.takeIf { !it.isJsonNull }?.asString
                ?: ro.get("nombre")?.takeIf { !it.isJsonNull }?.asString ?: ""
        }

        val idSede = getIntOrNull("id_sede", "sede_id", "idSede", "sede")
        var sedeNombre = getStr("sede_nombre", "nombre_sede", "sede_texto", "nombreSede", "sede_display", "sede")
        if (sedeNombre.isBlank()) {
            if (obj.has("id_sede") && obj.get("id_sede").isJsonObject) {
                val so = obj.getAsJsonObject("id_sede")
                sedeNombre = so.get("nombre_sede")?.takeIf { !it.isJsonNull }?.asString
                    ?: so.get("nombre")?.takeIf { !it.isJsonNull }?.asString ?: ""
            } else if (obj.has("sede") && obj.get("sede").isJsonObject) {
                val so = obj.getAsJsonObject("sede")
                sedeNombre = so.get("nombre_sede")?.takeIf { !it.isJsonNull }?.asString
                    ?: so.get("nombre")?.takeIf { !it.isJsonNull }?.asString ?: ""
            }
        }

        val idInst = getIntOrNull("id_institucion", "institucion_id", "idInstitucion", "institucion")
        var instNombre = getStr("institucion_nombre", "nombre_institucion", "institucion_texto", "nombreInstitucion", "institucion_display", "institucion")
        if (instNombre.isBlank()) {
            if (obj.has("id_institucion") && obj.get("id_institucion").isJsonObject) {
                val io = obj.getAsJsonObject("id_institucion")
                instNombre = io.get("nombre_institucion")?.takeIf { !it.isJsonNull }?.asString
                    ?: io.get("nombre")?.takeIf { !it.isJsonNull }?.asString ?: ""
            } else if (obj.has("institucion") && obj.get("institucion").isJsonObject) {
                val io = obj.getAsJsonObject("institucion")
                instNombre = io.get("nombre_institucion")?.takeIf { !it.isJsonNull }?.asString
                    ?: io.get("nombre")?.takeIf { !it.isJsonNull }?.asString ?: ""
            }
        }

        val isSuperuser = getBoolOrNull("is_superuser") ?: false
        val debeCambiarPassword = getBoolOrNull("debe_cambiar_password") ?: false

        return UsuarioInfo(
            idUsuario = id,
            nombre = nombre,
            apellido = apellido.takeIf { it.isNotBlank() },
            correo = correo,
            rol = rolStr.takeIf { it.isNotBlank() },
            nombreRol = nombreRol.takeIf { it.isNotBlank() },
            idRol = idRol,
            idSede = idSede,
            sedeNombre = sedeNombre.takeIf { it.isNotBlank() },
            idInstitucion = idInst,
            institucionNombre = instNombre.takeIf { it.isNotBlank() },
            isSuperuser = isSuperuser,
            debeCambiarPassword = debeCambiarPassword
        )
    }
}

// Modelo para la respuesta del Registro
data class RegistroResponse(
    val mensaje: String,
    val data: Usuario
)

// Modelos para recuperación de contraseña con código
data class SolicitarRecuperacionRequest(
    val correo: String
)

data class ConfirmarRecuperacionRequest(
    val correo: String,
    val codigo: String,
    val nueva_password: String,
    val confirmar_password: String
)

data class MensajeResponse(
    val mensaje: String? = null,
    val error: String? = null,
    val status: String? = null,

    @SerializedName("debug_code")
    val debugCode: String? = null,

    @SerializedName("debug_link")
    val debugLink: String? = null
)
