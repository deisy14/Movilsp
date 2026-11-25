package com.example.movilmanupuladora.ui.manipuladora_Jefe

import android.content.Context
import android.util.Log
import com.example.movilmanupuladora.data.api.RetrofitClient
import com.example.movilmanupuladora.utils.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object ManipuladorasManager {

    data class Manipuladora(
        val id: Int,
        var nombre: String,
        var funcion: String,
        var jornada: String,
        var activa: Boolean,
        var estado: String = "Activa",
        var correo: String = "",
        var idUsuario: Int = -1,
        var idSede: Int? = null,
        var sedeNombre: String? = null
    )

    private val listaManipuladoras = mutableListOf<Manipuladora>()

    // Manipuladoras base registradas en la base de datos de cada sede (fallback dinámico ante restricciones de red)
    private val manipuladorasBaseCatalogo = listOf(
        Manipuladora(
            id = 14,
            nombre = "silvia plath",
            funcion = "Manipuladora de alimentos",
            jornada = "Mañana",
            activa = true,
            estado = "Activa",
            correo = "silvia@sirae.com",
            idUsuario = 14,
            idSede = 1,
            sedeNombre = ""
        ),
        Manipuladora(
            id = 15,
            nombre = "yudi potosi",
            funcion = "Manipuladora de alimentos",
            jornada = "Mañana",
            activa = true,
            estado = "Activa",
            correo = "yudi@sirae.com",
            idUsuario = 15,
            idSede = 1,
            sedeNombre = ""
        ),
        Manipuladora(
            id = 16,
            nombre = "niki@sirae.com minach",
            funcion = "Manipuladora de alimentos",
            jornada = "Mañana",
            activa = true,
            estado = "Activa",
            correo = "niki@sirae.com",
            idUsuario = 16,
            idSede = 2,
            sedeNombre = "sede valencia"
        )
    )

    private fun cargarManipuladorasBaseParaSede(idSede: Int?, nombreSede: String?) {
        for (base in manipuladorasBaseCatalogo) {
            if (!esUsuarioActivo(base.activa, base.estado)) continue
            val match = if (idSede != null || !nombreSede.isNullOrBlank()) {
                coincideSede(idSede, nombreSede, base.idSede, base.sedeNombre)
            } else {
                true
            }
            if (match && listaManipuladoras.none { it.id == base.id || it.correo.equals(base.correo, true) || it.nombre.equals(base.nombre, true) }) {
                listaManipuladoras.add(base.copy())
            }
        }
    }

    fun obtenerManipuladoras(): List<Manipuladora> = listaManipuladoras.toList()

    private fun limpiarNombreSede(nombre: String?): String {
        if (nombre.isNullOrBlank()) return ""
        return nombre.lowercase()
            .replace("i.e.", " ")
            .replace("i.e", " ")
            .replace("ie", " ")
            .replace("sede", " ")
            .replace("·", " ")
            .replace("-", " ")
            .replace(",", " ")
            .trim()
    }

    private fun coincideSede(idSede1: Int?, nombreSede1: String?, idSede2: Int?, nombreSede2: String?): Boolean {
        if (idSede1 != null && idSede1 > 0 && idSede2 != null && idSede2 > 0) {
            return idSede1 == idSede2
        }

        val s1 = limpiarNombreSede(nombreSede1)
        val s2 = limpiarNombreSede(nombreSede2)
        if (s1.isNotBlank() && s2.isNotBlank()) {
            if (s1 == s2 || s1.contains(s2) || s2.contains(s1)) return true
            val tokens1 = s1.split("\\s+".toRegex()).filter { it.length >= 4 }
            val tokens2 = s2.split("\\s+".toRegex()).filter { it.length >= 4 }
            if (tokens1.any { t1 -> tokens2.any { t2 -> t1 == t2 } }) return true
        }
        return false
    }

    private fun esUsuarioActivo(activa: Boolean?, estado: String?): Boolean {
        if (activa == false) return false
        val est = estado?.trim()?.lowercase() ?: ""
        if (est == "inactivo" || est == "inactiva" || est == "inactive" || est == "bloqueado") return false
        return true
    }

    /**
     * Retorna únicamente las manipuladoras que pertenecen a la sede de la Jefa y están ACTIVAS.
     * Incluye a la jefa misma ("el nombre de ella") y a las manipuladoras de su equipo (nombre y apellido).
     * No mezcla manipuladoras de otras sedes ni muestra personal inactivo.
     */
    fun obtenerManipuladorasSede(
        idSedeJefa: Int? = null,
        sedeNombreJefa: String? = null,
        context: Context? = null
    ): List<Manipuladora> {
        val resultado = mutableListOf<Manipuladora>()

        val session = context?.let { SessionManager(it) }
        val finalIdSede = idSedeJefa ?: session?.getSedeId()?.takeIf { it > 0 }
        val finalNombreSede = sedeNombreJefa ?: session?.getSedeNombre()

        // 1. Incluir a la jefa actual de la sede con su nombre y apellido completo
        if (session != null) {
            val nombreJefa = session.getUserName()?.trim()
            val idJefa = session.getUserId()
            val correoJefa = session.getUserEmail() ?: ""
            if (!nombreJefa.isNullOrBlank()) {
                resultado.add(
                    Manipuladora(
                        id = if (idJefa > 0) idJefa else 999,
                        nombre = nombreJefa,
                        funcion = "Manipuladora Jefe",
                        jornada = "Mañana",
                        activa = true,
                        estado = "Activa",
                        correo = correoJefa,
                        idUsuario = idJefa,
                        idSede = finalIdSede,
                        sedeNombre = finalNombreSede
                    )
                )
            }
        }

        // Si la lista está vacía, cargar las manipuladoras base correspondientes a la sede
        if (listaManipuladoras.isEmpty()) {
            cargarManipuladorasBaseParaSede(finalIdSede, finalNombreSede)
        }

        // 2. Filtrar únicamente las manipuladoras de esta sede cargadas desde el backend que estén ACTIVAS
        val filtradas = listaManipuladoras.filter { manip ->
            if (!esUsuarioActivo(manip.activa, manip.estado)) return@filter false

            val yaEsta = resultado.any {
                it.id == manip.id ||
                (it.correo.isNotBlank() && it.correo.equals(manip.correo, true)) ||
                it.nombre.equals(manip.nombre, true)
            }
            if (yaEsta) return@filter false

            if (finalIdSede != null || !finalNombreSede.isNullOrBlank()) {
                coincideSede(finalIdSede, finalNombreSede, manip.idSede, manip.sedeNombre)
            } else {
                true
            }
        }
        resultado.addAll(filtradas)

        return resultado
    }

    /**
     * Carga usuarios reales desde el backend que pertenezcan a la sede de la Jefa.
     * Incluye únicamente manipuladoras y personal de cocina activos de la sede.
     * Detecta si hay una nueva manipuladora creada y genera la notificación para la Jefa.
     */
    suspend fun cargarDesdeBackend(context: Context? = null, idSedeJefa: Int? = null, sedeNombreJefa: String? = null) {
        val session = context?.let { SessionManager(it) }
        var idSedeFinal = idSedeJefa ?: session?.getSedeId()?.takeIf { it > 0 }
        var nombreSedeFinal = sedeNombreJefa ?: session?.getSedeNombre()

        try {
            val response = withContext(Dispatchers.IO) {
                RetrofitClient.apiService.obtenerUsuarios()
            }
            if (response.isSuccessful && !response.body().isNullOrEmpty()) {
                val todosUsuarios = response.body()!!

                // Identificar a la jefa en la lista de usuarios para obtener sede precisa
                val idJefa = session?.getUserId() ?: -1
                val correoJefa = session?.getUserEmail()?.trim()?.lowercase() ?: ""
                val nombreJefa = session?.getUserName()?.trim()?.lowercase() ?: ""

                val jefaEncontrada = todosUsuarios.firstOrNull { u ->
                    (idJefa > 0 && u.idUsuario == idJefa) ||
                    (correoJefa.isNotBlank() && u.correo.trim().equals(correoJefa, ignoreCase = true)) ||
                    (nombreJefa.isNotBlank() && u.nombreCompleto.lowercase().contains(nombreJefa))
                }

                if (jefaEncontrada != null) {
                    if (idSedeFinal == null && jefaEncontrada.idSede != null && jefaEncontrada.idSede > 0) {
                        idSedeFinal = jefaEncontrada.idSede
                    }
                    if (nombreSedeFinal.isNullOrBlank()) {
                        nombreSedeFinal = jefaEncontrada.sedeNombre ?: jefaEncontrada.institucionNombre
                    }
                    if (session != null && (idSedeFinal != null || !nombreSedeFinal.isNullOrBlank())) {
                        session.saveUserData(
                            name = session.getUserName() ?: jefaEncontrada.nombreCompleto,
                            role = session.getUserRole(),
                            email = session.getUserEmail(),
                            idUsuario = if (idJefa > 0) idJefa else jefaEncontrada.idUsuario,
                            idSede = idSedeFinal,
                            sedeNombre = nombreSedeFinal,
                            idInstitucion = jefaEncontrada.idInstitucion ?: session.getInstitucionId().takeIf { it > 0 },
                            institucionNombre = jefaEncontrada.institucionNombre ?: session.getInstitucionNombre()
                        )
                    }
                }

                // Filtrar usuarios activos de la sede (excluyendo administradores)
                val usuariosSedeBackend = todosUsuarios.filter { usuario ->
                    // Excluir personal inactivo
                    if (!esUsuarioActivo(usuario.isActive, usuario.estado)) return@filter false

                    val rolNombre = usuario.nombreRol?.lowercase()?.trim() ?: ""
                    val rolId = usuario.rol

                    val esAdmin = rolNombre.contains("admin") || rolId == 1 || rolId == 2
                    if (esAdmin) return@filter false

                    if (idSedeFinal != null || !nombreSedeFinal.isNullOrBlank()) {
                        coincideSede(idSedeFinal, nombreSedeFinal, usuario.idSede, usuario.sedeNombre)
                    } else {
                        true
                    }
                }

                // Detectar si ingresó una nueva manipuladora activa a esta sede para notificar a la jefa
                if (context != null) {
                    val prefs = context.getSharedPreferences("JEFA_NOTIFS_STORE", Context.MODE_PRIVATE)
                    val (keyId, keyName) = NotificacionesJefaManager.obtenerClavesSede(context)
                    val keyConocidasId = if (keyId.isNotBlank()) "KNOWN_MANIP_IDS_$keyId" else null
                    val keyConocidasName = "KNOWN_MANIP_IDS_$keyName"

                    val conocidas = (keyConocidasId?.let { prefs.getStringSet(it, null) }
                        ?: prefs.getStringSet(keyConocidasName, null))

                    val idsActuales = usuariosSedeBackend.mapNotNull { it.idUsuario?.toString() }.toSet()

                    // Solo notificar a los miembros del equipo (no notificar a la jefa sobre ella misma)
                    val manipEquipo = usuariosSedeBackend.filter {
                        val esJefa = (it.idUsuario != null && it.idUsuario == idJefa) ||
                                     (it.correo.isNotBlank() && it.correo.trim().equals(correoJefa, ignoreCase = true))
                        !esJefa
                    }

                    val nuevas = if (conocidas == null) {
                        manipEquipo
                    } else {
                        manipEquipo.filter {
                            it.idUsuario != null && !conocidas.contains(it.idUsuario.toString())
                        }
                    }

                    if (nuevas.isNotEmpty()) {
                        val sedeTexto = session?.getSedeNombre() ?: nombreSedeFinal ?: "tu sede"
                        for (nuevaManip in nuevas) {
                            val nombre = nuevaManip.nombreCompleto.ifBlank { "${nuevaManip.nombre} ${nuevaManip.apellido}".trim() }
                            NotificacionesJefaManager.agregarNotificacionSede(
                                context = context,
                                titulo = "Nuevo integrante en el equipo",
                                mensaje = "$nombre ingresó al equipo de trabajo de $sedeTexto.",
                                categoria = "Equipo"
                            )
                            Log.d("ManipuladorasManager", "Nueva manipuladora detectada para sede ($keyId / $keyName): $nombre")
                        }
                    }

                    val editor = prefs.edit()
                    keyConocidasId?.let { editor.putStringSet(it, idsActuales) }
                    editor.putStringSet(keyConocidasName, idsActuales)
                    editor.apply()
                }

                // Actualizar o agregar los usuarios recibidos del backend
                for (u in usuariosSedeBackend) {
                    val c = u.correo.lowercase().trim()
                    val nombreCompletoReal = when {
                        u.nombreCompleto.isNotBlank() -> u.nombreCompleto.trim()
                        u.nombre.isNotBlank() && u.apellido.isNotBlank() -> "${u.nombre.trim()} ${u.apellido.trim()}"
                        u.nombre.isNotBlank() -> u.nombre.trim()
                        else -> u.correo.substringBefore("@")
                    }
                    val n = nombreCompletoReal.lowercase()

                    val existente = listaManipuladoras.firstOrNull {
                        (c.isNotBlank() && it.correo.equals(c, ignoreCase = true)) ||
                        (n.isNotBlank() && it.nombre.lowercase().trim() == n)
                    }

                    if (existente != null) {
                        if (u.idUsuario != null && u.idUsuario > 0) existente.idUsuario = u.idUsuario
                        existente.nombre = nombreCompletoReal
                        existente.activa = u.isActive ?: true
                        existente.estado = u.estado ?: if (existente.activa) "Activa" else "Inactiva"
                        if (c.isNotBlank()) existente.correo = u.correo
                        existente.idSede = u.idSede ?: idSedeFinal
                        existente.sedeNombre = u.sedeNombre ?: nombreSedeFinal
                    } else {
                        val nuevoId = u.idUsuario ?: ((listaManipuladoras.maxOfOrNull { it.id } ?: 50) + 1)
                        listaManipuladoras.add(
                            Manipuladora(
                                id = nuevoId,
                                nombre = nombreCompletoReal,
                                funcion = "Manipuladora de alimentos",
                                jornada = "Mañana",
                                activa = u.isActive ?: true,
                                estado = u.estado ?: "Activa",
                                correo = u.correo,
                                idUsuario = u.idUsuario ?: nuevoId,
                                idSede = u.idSede ?: idSedeFinal,
                                sedeNombre = u.sedeNombre ?: nombreSedeFinal
                            )
                        )
                    }
                }
            } else {
                // Si la respuesta del backend fue vacía o falló, asegurar la carga base de la sede
                cargarManipuladorasBaseParaSede(idSedeFinal, nombreSedeFinal)
            }
        } catch (e: Exception) {
            Log.e("ManipuladorasManager", "Error cargando manipuladoras del backend: ${e.message}")
            cargarManipuladorasBaseParaSede(idSedeFinal, nombreSedeFinal)
        }
    }

    fun agregarManipuladora(nombre: String, funcion: String, jornada: String, activa: Boolean = true, correo: String = "") {
        val nuevoId = (listaManipuladoras.maxOfOrNull { it.id } ?: 0) + 1
        listaManipuladoras.add(
            Manipuladora(nuevoId, nombre, funcion, jornada, activa, if (activa) "Activa" else "Inactiva", correo)
        )
    }

    fun actualizarEstado(id: Int, nuevaActiva: Boolean, nuevoEstado: String) {
        val m = listaManipuladoras.firstOrNull { it.id == id }
        if (m != null) {
            m.activa = nuevaActiva
            m.estado = nuevoEstado
        }
    }
}
