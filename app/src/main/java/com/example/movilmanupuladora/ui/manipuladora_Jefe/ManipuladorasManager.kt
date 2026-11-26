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
    private var ultimaSedeCargadaId: Int? = null
    private var ultimaSedeCargadaNombre: String? = null

    fun obtenerManipuladoras(): List<Manipuladora> = listaManipuladoras.toList()

    private fun normalizarTexto(texto: String?): String {
        if (texto.isNullOrBlank()) return ""
        val nfd = java.text.Normalizer.normalize(texto, java.text.Normalizer.Form.NFD)
        return nfd.replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "")
            .lowercase()
            .trim()
    }

    private fun limpiarNombreSede(nombre: String?): String {
        if (nombre.isNullOrBlank()) return ""
        return normalizarTexto(nombre)
            .replace("institucion educativa", " ")
            .replace("i.e.", " ")
            .replace("i.e", " ")
            .replace("ie", " ")
            .replace("sede", " ")
            .replace("·", " ")
            .replace("-", " ")
            .replace(",", " ")
            .replace("\\s+".toRegex(), " ")
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
            val tokens1 = s1.split(" ").filter { it.length >= 4 }
            val tokens2 = s2.split(" ").filter { it.length >= 4 }
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

        // Si cambió de sede en esta sesión, limpiar caché en memoria
        if ((idSedeFinal != null && idSedeFinal != ultimaSedeCargadaId) ||
            (!nombreSedeFinal.isNullOrBlank() && !nombreSedeFinal.equals(ultimaSedeCargadaNombre, ignoreCase = true))
        ) {
            listaManipuladoras.clear()
            ultimaSedeCargadaId = idSedeFinal
            ultimaSedeCargadaNombre = nombreSedeFinal
        }

        try {
            // Resolver id_sede desde /api/sedes/ si aún no se tiene numérico
            if ((idSedeFinal == null || idSedeFinal <= 0) && !nombreSedeFinal.isNullOrBlank()) {
                try {
                    val sedesRes = withContext(Dispatchers.IO) { RetrofitClient.apiService.obtenerSedes() }
                    if (sedesRes.isSuccessful && sedesRes.body() != null) {
                        val root = sedesRes.body()!!
                        val arr = when {
                            root.isJsonArray -> root.asJsonArray
                            root.isJsonObject && root.asJsonObject.has("results") -> root.asJsonObject.getAsJsonArray("results")
                            root.isJsonObject && root.asJsonObject.has("data") -> root.asJsonObject.getAsJsonArray("data")
                            else -> null
                        }
                        if (arr != null) {
                            val buscado = limpiarNombreSede(nombreSedeFinal)
                            for (el in arr) {
                                if (!el.isJsonObject) continue
                                val so = el.asJsonObject
                                val sNom = so.get("nombre_sede")?.takeIf { !it.isJsonNull }?.asString
                                    ?: so.get("nombre")?.takeIf { !it.isJsonNull }?.asString
                                if (limpiarNombreSede(sNom) == buscado) {
                                    val fid = so.get("id_sede")?.takeIf { !it.isJsonNull }?.asInt
                                        ?: so.get("id")?.takeIf { !it.isJsonNull }?.asInt
                                    if (fid != null && fid > 0) {
                                        idSedeFinal = fid
                                        break
                                    }
                                }
                            }
                        }
                    }
                } catch (_: Exception) { }
            }

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

                // Reemplazar completamente con la lista fresca y real del backend para esta sede
                listaManipuladoras.clear()
                ultimaSedeCargadaId = idSedeFinal
                ultimaSedeCargadaNombre = nombreSedeFinal

                // Actualizar o agregar los usuarios recibidos del backend
                for (u in usuariosSedeBackend) {
                    val c = u.correo.lowercase().trim()
                    val nombreCompletoReal = when {
                        u.nombreCompleto.isNotBlank() -> u.nombreCompleto.trim()
                        u.nombre.isNotBlank() && u.apellido.isNotBlank() -> "${u.nombre.trim()} ${u.apellido.trim()}"
                        u.nombre.isNotBlank() -> u.nombre.trim()
                        else -> u.correo.substringBefore("@")
                    }
                    val nuevoId = u.idUsuario ?: ((listaManipuladoras.maxOfOrNull { it.id } ?: 50) + 1)
                    val rolTxt = u.nombreRol?.takeIf { it.isNotBlank() } ?: "Manipuladora de alimentos"
                    listaManipuladoras.add(
                        Manipuladora(
                            id = nuevoId,
                            nombre = nombreCompletoReal,
                            funcion = rolTxt,
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
        } catch (e: Exception) {
            Log.e("ManipuladorasManager", "Error cargando manipuladoras del backend: ${e.message}")
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
