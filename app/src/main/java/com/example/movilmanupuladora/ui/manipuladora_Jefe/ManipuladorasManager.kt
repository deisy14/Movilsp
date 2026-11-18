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

    fun obtenerManipuladoras(): List<Manipuladora> = listaManipuladoras.toList()

    /**
     * Retorna únicamente las manipuladoras que pertenecen a la sede de la Jefa.
     * Incluye a la jefa misma ("el nombre de ella") y a las manipuladoras de su equipo (nombre y apellido).
     * No mezcla manipuladoras de otras sedes.
     */
    fun obtenerManipuladorasSede(
        idSedeJefa: Int? = null,
        sedeNombreJefa: String? = null,
        context: Context? = null
    ): List<Manipuladora> {
        val sedeNorm = sedeNombreJefa?.trim()?.lowercase()
        val resultado = mutableListOf<Manipuladora>()

        // 1. Incluir a la jefa actual de la sede con su nombre y apellido completo
        if (context != null) {
            val session = SessionManager(context)
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
                        idSede = idSedeJefa,
                        sedeNombre = sedeNombreJefa
                    )
                )
            }
        }

        // 2. Filtrar únicamente las manipuladoras de esta sede cargadas desde el backend
        val filtradas = listaManipuladoras.filter { manip ->
            val yaEsta = resultado.any {
                it.id == manip.id ||
                (it.correo.isNotBlank() && it.correo.equals(manip.correo, true)) ||
                it.nombre.equals(manip.nombre, true)
            }
            if (yaEsta) return@filter false

            val matchId = idSedeJefa != null && idSedeJefa > 0 && manip.idSede == idSedeJefa
            val matchNombre = !sedeNorm.isNullOrBlank() && manip.sedeNombre?.trim()?.lowercase()?.contains(sedeNorm) == true
            matchId || matchNombre
        }
        resultado.addAll(filtradas)

        return resultado
    }

    /**
     * Carga usuarios reales desde el backend que pertenezcan a la sede de la Jefa.
     * Incluye manipuladoras y personal de cocina de la sede.
     * Detecta si hay una nueva manipuladora creada y genera la notificación para la Jefa.
     */
    suspend fun cargarDesdeBackend(context: Context? = null, idSedeJefa: Int? = null, sedeNombreJefa: String? = null) {
        try {
            val response = withContext(Dispatchers.IO) {
                RetrofitClient.apiService.obtenerUsuarios()
            }
            if (response.isSuccessful && !response.body().isNullOrEmpty()) {
                val todosUsuarios = response.body()!!
                val sedeNorm = sedeNombreJefa?.trim()?.lowercase()

                // Filtrar usuarios de la sede (excluyendo solo administradores del sistema)
                val usuariosSedeBackend = todosUsuarios.filter { usuario ->
                    val rolNombre = usuario.nombreRol?.lowercase()?.trim() ?: ""
                    val rolId = usuario.rol

                    val esAdmin = rolNombre.contains("admin") || rolId == 1 || rolId == 2
                    if (esAdmin) return@filter false

                    val perteneceSede = when {
                        idSedeJefa != null && idSedeJefa > 0 -> {
                            usuario.idSede == idSedeJefa || (!sedeNorm.isNullOrBlank() && usuario.sedeNombre?.trim()?.lowercase()?.contains(sedeNorm) == true)
                        }
                        !sedeNorm.isNullOrBlank() -> {
                            usuario.sedeNombre?.trim()?.lowercase()?.contains(sedeNorm) == true
                        }
                        else -> true
                    }

                    perteneceSede
                }

                // Detectar si ingresó una nueva manipuladora a esta sede para notificar a la jefa
                if (context != null && idSedeJefa != null && idSedeJefa > 0) {
                    val prefs = context.getSharedPreferences("JEFA_NOTIFS", Context.MODE_PRIVATE)
                    val keyConocidas = "KNOWN_MANIP_IDS_$idSedeJefa"
                    val conocidas = prefs.getStringSet(keyConocidas, null)

                    val idsActuales = usuariosSedeBackend.mapNotNull { it.idUsuario?.toString() }.toSet()

                    if (conocidas != null && conocidas.isNotEmpty()) {
                        val nuevas = usuariosSedeBackend.filter {
                            it.idUsuario != null && !conocidas.contains(it.idUsuario.toString())
                        }
                        if (nuevas.isNotEmpty()) {
                            val nombresNuevas = nuevas.joinToString(", ") {
                                it.nombreCompleto.ifBlank { "${it.nombre} ${it.apellido}".trim() }
                            }
                            prefs.edit()
                                .putBoolean("HAY_NUEVA_MANIP", true)
                                .putString("TITULO_NUEVA_MANIP", "Nuevo integrante en el equipo")
                                .putString("MSG_NUEVA_MANIP", "$nombresNuevas ingresó al equipo de trabajo de tu sede.")
                                .apply()
                            Log.d("ManipuladorasManager", "Nueva manipuladora detectada para sede $idSedeJefa: $nombresNuevas")
                        }
                    }

                    prefs.edit().putStringSet(keyConocidas, idsActuales).apply()
                }

                if (idSedeJefa != null && idSedeJefa > 0) {
                    listaManipuladoras.removeAll { it.idSede != null && it.idSede != idSedeJefa }
                }

                if (usuariosSedeBackend.isNotEmpty()) {
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
                            if (u.isActive != null) {
                                existente.activa = u.isActive
                                existente.estado = if (u.isActive) "Activa" else "Inactiva"
                            }
                            if (c.isNotBlank()) existente.correo = u.correo
                            existente.idSede = u.idSede ?: idSedeJefa
                            existente.sedeNombre = u.sedeNombre ?: sedeNombreJefa
                        } else {
                            val nuevoId = u.idUsuario ?: ((listaManipuladoras.maxOfOrNull { it.id } ?: 50) + 1)
                            listaManipuladoras.add(
                                Manipuladora(
                                    id = nuevoId,
                                    nombre = nombreCompletoReal,
                                    funcion = "Manipuladora de alimentos",
                                    jornada = "Mañana",
                                    activa = u.isActive ?: true,
                                    estado = if (u.isActive == true) "Activa" else "Inactiva",
                                    correo = u.correo,
                                    idUsuario = u.idUsuario ?: nuevoId,
                                    idSede = u.idSede ?: idSedeJefa,
                                    sedeNombre = u.sedeNombre ?: sedeNombreJefa
                                )
                            )
                        }
                    }
                    Log.d("ManipuladorasManager", "Cargadas ${listaManipuladoras.size} manipuladoras de sede $idSedeJefa")
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
