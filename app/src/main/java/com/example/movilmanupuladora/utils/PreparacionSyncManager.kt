package com.example.movilmanupuladora.utils

import android.content.Context
import android.util.Log
import com.example.movilmanupuladora.data.api.RetrofitClient
import com.example.movilmanupuladora.data.model.MenuPaeCatalog
import com.example.movilmanupuladora.data.model.Turno
import com.example.movilmanupuladora.ui.manipuladora_Jefe.AsistenciaManager
import com.example.movilmanupuladora.ui.manipuladora_Jefe.ManipuladorasManager
import com.example.movilmanupuladora.ui.manipuladora_Jefe.PreparacionEstadoManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.Normalizer
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PreparacionSyncManager {

    private const val TAG = "PreparacionSync"
    private const val PREFIX_PLATO = "PAE_PLATO:"
    private const val PREFIX_COMP = "PAE_COMP:"
    private const val PREFIX_ASIST = "PAE_ASIST:"

    data class AsignacionItem(
        val idUsuario: Int,
        val nombre: String,
        val correo: String,
        val componente: String?,
        val estado: String
    )

    private fun normalizar(texto: String?): String {
        if (texto.isNullOrBlank()) return ""
        val sinTildes = Normalizer.normalize(texto, Normalizer.Form.NFD)
            .replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "")
        return sinTildes.lowercase().trim()
            .replace("@", "_")
            .replace(".", "_")
            .replace(" ", "_")
    }

    /**
     * Sincroniza desde el backend el plato actual seleccionado, los componentes asignados con sus estados
     * y la asistencia de estudiantes del día.
     * Almacena todo en SharedPreferences locales de forma transparente para que cualquier móvil lo refleje.
     */
    suspend fun sincronizarDesdeBackend(context: Context): Boolean = withContext(Dispatchers.IO) {
        try {
            SessionManager.getToken(context)
            val resp = RetrofitClient.apiService.obtenerTurnos()
            if (!resp.isSuccessful || resp.body().isNullOrEmpty()) {
                Log.d(TAG, "No se obtuvieron turnos o respuesta vacía: ${resp.code()}")
                return@withContext false
            }

            val turnos = resp.body()!!
            val sessionMgr = SessionManager(context)
            val jefaPrefs = context.getSharedPreferences("ManipuladoraJefe", Context.MODE_PRIVATE)
            val turnoPrefs = context.getSharedPreferences("TURNO_PAE", Context.MODE_PRIVATE)
            val jefaEditor = jefaPrefs.edit()
            val turnoEditor = turnoPrefs.edit()

            // Asegurar que las manipuladoras del backend estén cargadas en memoria
            try {
                ManipuladorasManager.cargarDesdeBackend()
            } catch (_: Exception) { }
            val listaManip = ManipuladorasManager.obtenerManipuladoras()

            var platoEncontrado = false

            // 1. Plato seleccionado más reciente en la nube
            val turnoPlatoMasReciente = turnos
                .filter { it.nombreTurno?.startsWith(PREFIX_PLATO) == true }
                .maxByOrNull { it.idTurno ?: 0 }

            if (turnoPlatoMasReciente != null) {
                val data = turnoPlatoMasReciente.nombreTurno!!.removePrefix(PREFIX_PLATO).split("#")
                if (data.isNotEmpty()) {
                    var platoNombre = data[0].trim()
                    val jornada = if (data.size > 1 && data[1].isNotBlank()) data[1].trim() else "Mañana"
                    val idPlato = if (data.size > 2) data[2].toIntOrNull() ?: -1 else -1

                    // Restaurar nombre completo oficial si está en el catálogo PAE
                    if (idPlato in 1..20) {
                        val itemPae = MenuPaeCatalog.menus.find { it.numeroMenu == idPlato || it.id == idPlato }
                        if (itemPae != null) {
                            platoNombre = itemPae.nombre
                        }
                    }

                    if (platoNombre.isNotBlank()) {
                        sessionMgr.savePlatoSeleccionado(platoNombre, if (idPlato > 0) idPlato else null)

                        jefaEditor.putString("plato_actual", platoNombre)
                        jefaEditor.putString("jornada", jornada)

                        turnoEditor.putString("plato_nombre", platoNombre)
                        turnoEditor.putString("plato_jornada", jornada)
                        turnoEditor.putInt("plato_id", idPlato)
                        platoEncontrado = true
                        Log.d(TAG, "Plato sincronizado desde nube: $platoNombre ($jornada)")
                    }
                }
            }

            // 2. Componentes asignados: agrupar por UID y tomar el registro MÁS RECIENTE de cada manipuladora
            val turnosComp = turnos.filter { it.nombreTurno?.startsWith(PREFIX_COMP) == true }
            val turnosPorUsuario = mutableMapOf<Int, Turno>()

            for (t in turnosComp) {
                val parts = t.nombreTurno!!.removePrefix(PREFIX_COMP).split(":")
                if (parts.size >= 3) {
                    val uid = parts[0].trim().toIntOrNull() ?: continue
                    val actual = turnosPorUsuario[uid]
                    if (actual == null || (t.idTurno ?: 0) > (actual.idTurno ?: 0)) {
                        turnosPorUsuario[uid] = t
                    }
                }
            }

            for ((uid, t) in turnosPorUsuario) {
                val parts = t.nombreTurno!!.removePrefix(PREFIX_COMP).split(":")
                if (parts.size >= 3) {
                    val p1 = parts[1].trim()
                    val p2 = parts.subList(2, parts.size).joinToString(":").trim()

                    val estado: String
                    val comp: String
                    if (p1 == "0" || p1 == "1" || p1 == "2") {
                        estado = when (p1) {
                            "1" -> PreparacionEstadoManager.ESTADO_EN_PROCESO
                            "2" -> PreparacionEstadoManager.ESTADO_FINALIZADA
                            else -> PreparacionEstadoManager.ESTADO_POR_HACER
                        }
                        comp = p2
                    } else {
                        comp = p1
                        estado = when {
                            p2.contains("proceso", ignoreCase = true) -> PreparacionEstadoManager.ESTADO_EN_PROCESO
                            p2.contains("final", ignoreCase = true) -> PreparacionEstadoManager.ESTADO_FINALIZADA
                            else -> PreparacionEstadoManager.ESTADO_POR_HACER
                        }
                    }

                    if (uid > 0 && comp.isNotBlank()) {
                        if (uid == 19) {
                            jefaEditor.putString("comp_jefa", comp)
                            jefaEditor.putString("comp_uid_19", comp)
                            PreparacionEstadoManager.guardarEstado(
                                context = context,
                                persona = "Isa Pérez",
                                componente = comp,
                                estado = estado,
                                correo = "isaperez@gmail.com"
                            )
                            Log.d(TAG, "Componente Jefa sincronizado: $comp ($estado)")
                        } else {
                            val manip = listaManip.firstOrNull { it.idUsuario == uid || it.id == uid }
                            val manipNombre = manip?.nombre ?: jefaPrefs.getString("manip_nombre_uid_$uid", "Manipuladora") ?: "Manipuladora"
                            val manipCorreo = manip?.correo ?: jefaPrefs.getString("manip_correo_uid_$uid", "") ?: ""

                            val normNombre = normalizar(manipNombre)
                            jefaEditor.putString("comp_$normNombre", comp)

                            val primerNombre = manipNombre.trim().split(" ").firstOrNull() ?: ""
                            if (primerNombre.isNotBlank()) {
                                jefaEditor.putString("comp_" + normalizar(primerNombre), comp)
                            }

                            if (manipCorreo.isNotBlank()) {
                                val keyCorreo = "comp_correo_" + normalizar(manipCorreo)
                                jefaEditor.putString(keyCorreo, comp)
                            }

                            jefaEditor.putString("comp_uid_$uid", comp)
                            jefaEditor.putString("manip_nombre_uid_$uid", manipNombre)
                            jefaEditor.putString("manip_correo_uid_$uid", manipCorreo)

                            PreparacionEstadoManager.guardarEstado(
                                context = context,
                                persona = manipNombre,
                                componente = comp,
                                estado = estado,
                                correo = manipCorreo
                            )

                            Log.d(TAG, "Componente de $manipNombre (uid=$uid) sincronizado: $comp ($estado)")
                        }
                    }
                }
            }

            // 3. Asistencia de estudiantes compartida en tiempo real
            val turnoAsist = turnos
                .filter { it.nombreTurno?.startsWith(PREFIX_ASIST) == true }
                .maxByOrNull { it.idTurno ?: 0 }

            if (turnoAsist != null) {
                val parts = turnoAsist.nombreTurno!!.removePrefix(PREFIX_ASIST).split(":")
                if (parts.size >= 3) {
                    val prim = parts[0].trim().toIntOrNull() ?: 0
                    val sec = parts[1].trim().toIntOrNull() ?: 0
                    val tot = parts[2].trim().toIntOrNull() ?: (prim + sec)
                    if (tot > 0) {
                        AsistenciaManager.guardarPrimaria(context, prim)
                        AsistenciaManager.guardarSecundaria(context, sec)
                        AsistenciaManager.guardarManana(context, tot)
                        Log.d(TAG, "Asistencia sincronizada desde Turnos: Primaria=$prim, Secundaria=$sec, Total=$tot")
                    }
                }
            }

            jefaEditor.putBoolean("componentes_asignados_guardados", true)
            jefaEditor.apply()
            turnoEditor.apply()

            return@withContext platoEncontrado
        } catch (e: Exception) {
            Log.e(TAG, "Error sincronizando preparaciones: ${e.message}")
            return@withContext false
        }
    }

    /**
     * Guarda en un solo lote atómico el plato seleccionado y las asignaciones de componentes de todo el equipo
     * realizando una sola lectura previa de turnos en el backend.
     */
    suspend fun guardarPlatoYAsignacionesCompleto(
        context: Context,
        nombreMenu: String,
        idPlato: Int?,
        jornada: String,
        asignaciones: List<AsignacionItem>
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val sessionMgr = SessionManager(context)
            sessionMgr.savePlatoSeleccionado(nombreMenu, idPlato)

            val jefaPrefs = context.getSharedPreferences("ManipuladoraJefe", Context.MODE_PRIVATE)
            val turnoPrefs = context.getSharedPreferences("TURNO_PAE", Context.MODE_PRIVATE)
            val jefaEditor = jefaPrefs.edit()
            val turnoEditor = turnoPrefs.edit()

            jefaEditor.putString("plato_actual", nombreMenu)
            jefaEditor.putString("jornada", jornada)
            turnoEditor.putString("plato_nombre", nombreMenu)
            turnoEditor.putString("plato_jornada", jornada)
            turnoEditor.putInt("plato_id", idPlato ?: -1)

            for (a in asignaciones) {
                val comp = a.componente ?: ""
                val normNombre = normalizar(a.nombre)
                jefaEditor.putString("comp_$normNombre", comp)

                val primerNombre = a.nombre.trim().split(" ").firstOrNull() ?: ""
                if (primerNombre.isNotBlank()) {
                    jefaEditor.putString("comp_" + normalizar(primerNombre), comp)
                }
                if (a.correo.isNotBlank()) {
                    val keyCorreo = "comp_correo_" + normalizar(a.correo)
                    jefaEditor.putString(keyCorreo, comp)
                }
                if (a.idUsuario > 0) {
                    jefaEditor.putString("comp_uid_${a.idUsuario}", comp)
                    jefaEditor.putString("manip_nombre_uid_${a.idUsuario}", a.nombre)
                    jefaEditor.putString("manip_correo_uid_${a.idUsuario}", a.correo)
                }
                if (a.idUsuario == 19 || a.nombre.contains("isa", true)) {
                    jefaEditor.putString("comp_jefa", comp)
                }
                PreparacionEstadoManager.guardarEstado(
                    context = context,
                    persona = a.nombre,
                    componente = comp,
                    estado = a.estado,
                    correo = a.correo
                )
            }
            jefaEditor.putBoolean("componentes_asignados_guardados", true)
            jefaEditor.apply()
            turnoEditor.apply()

            // Sincronizar en la nube con UNA sola llamada GET inicial
            SessionManager.getToken(context)
            val res = RetrofitClient.apiService.obtenerTurnos()
            if (res.isSuccessful && res.body() != null) {
                val todosTurnos = res.body()!!

                // 1. Guardar o actualizar plato
                val tagPlato = "$PREFIX_PLATO${nombreMenu.trim().take(30)}#${jornada.trim()}#${idPlato ?: -1}"
                val existentePlato = todosTurnos.firstOrNull { it.nombreTurno?.startsWith(PREFIX_PLATO) == true }
                if (existentePlato?.idTurno != null) {
                    RetrofitClient.apiService.patchTurno(existentePlato.idTurno, mapOf("nombre_turno" to tagPlato))
                } else {
                    RetrofitClient.apiService.crearTurno(Turno(nombreTurno = tagPlato))
                }

                // 2. Guardar o actualizar asignación de cada miembro
                for (a in asignaciones) {
                    if (a.idUsuario <= 0) continue
                    val prefixUser = "$PREFIX_COMP${a.idUsuario}:"
                    val existenteComp = todosTurnos.firstOrNull { it.nombreTurno?.startsWith(prefixUser) == true }

                    if (a.componente.isNullOrBlank()) {
                        if (existenteComp?.idTurno != null) {
                            RetrofitClient.apiService.eliminarTurno(existenteComp.idTurno)
                        }
                    } else {
                        val estadoCode = when {
                            a.estado.contains("proceso", ignoreCase = true) -> "1"
                            a.estado.contains("final", ignoreCase = true) -> "2"
                            else -> "0"
                        }
                        val compLimpio = a.componente.trim().take(34)
                        val tagComp = "$PREFIX_COMP${a.idUsuario}:$estadoCode:$compLimpio"

                        if (existenteComp?.idTurno != null) {
                            RetrofitClient.apiService.patchTurno(existenteComp.idTurno, mapOf("nombre_turno" to tagComp))
                        } else {
                            RetrofitClient.apiService.crearTurno(Turno(nombreTurno = tagComp))
                        }
                    }
                }
                Log.d(TAG, "Asignaciones y plato guardados exitosamente en nube")
                return@withContext true
            }
            return@withContext false
        } catch (e: Exception) {
            Log.e(TAG, "Error en guardarPlatoYAsignacionesCompleto: ${e.message}")
            return@withContext false
        }
    }

    /**
     * Guarda la asistencia del día en la nube a través del canal común de Turnos
     * para asegurar disponibilidad inmediata en todos los móviles.
     */
    suspend fun guardarAsistenciaEnNube(
        primaria: Int,
        secundaria: Int,
        total: Int
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val fechaHoy = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            val tag = "$PREFIX_ASIST$primaria:$secundaria:$total:$fechaHoy"
            val res = RetrofitClient.apiService.obtenerTurnos()
            if (res.isSuccessful && res.body() != null) {
                val turnosAsist = res.body()!!.filter { it.nombreTurno?.startsWith(PREFIX_ASIST) == true }
                val maxTurno = turnosAsist.maxByOrNull { it.idTurno ?: 0 }
                if (maxTurno?.idTurno != null) {
                    RetrofitClient.apiService.patchTurno(maxTurno.idTurno, mapOf("nombre_turno" to tag))
                } else {
                    RetrofitClient.apiService.crearTurno(Turno(nombreTurno = tag))
                }
                // Sincronizar también cualquier otro turno duplicado para consistencia total
                for (otro in turnosAsist) {
                    if (otro.idTurno != null && otro.idTurno != maxTurno?.idTurno) {
                        try {
                            RetrofitClient.apiService.patchTurno(otro.idTurno, mapOf("nombre_turno" to tag))
                        } catch (_: Exception) { }
                    }
                }
                Log.d(TAG, "Asistencia guardada en Turnos en la nube: $tag")
                return@withContext true
            }
            return@withContext false
        } catch (e: Exception) {
            Log.e(TAG, "Error guardando asistencia en turnos: ${e.message}")
            return@withContext false
        }
    }

    /**
     * Guarda el plato seleccionado en el backend para que todos los dispositivos lo reciban.
     */
    suspend fun guardarPlatoSeleccionado(
        context: Context,
        nombrePlato: String,
        idPlato: Int?,
        jornada: String
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val sessionMgr = SessionManager(context)
            sessionMgr.savePlatoSeleccionado(nombrePlato, idPlato)
            context.getSharedPreferences("ManipuladoraJefe", Context.MODE_PRIVATE).edit()
                .putString("plato_actual", nombrePlato)
                .putString("jornada", jornada)
                .apply()
            context.getSharedPreferences("TURNO_PAE", Context.MODE_PRIVATE).edit()
                .putString("plato_nombre", nombrePlato)
                .putString("plato_jornada", jornada)
                .putInt("plato_id", idPlato ?: -1)
                .apply()

            val tag = "$PREFIX_PLATO${nombrePlato.trim().take(30)}#${jornada.trim()}#${idPlato ?: -1}"
            val res = RetrofitClient.apiService.obtenerTurnos()
            if (res.isSuccessful && res.body() != null) {
                val existente = res.body()!!.firstOrNull { it.nombreTurno?.startsWith(PREFIX_PLATO) == true }
                if (existente?.idTurno != null) {
                    RetrofitClient.apiService.patchTurno(existente.idTurno, mapOf("nombre_turno" to tag))
                } else {
                    RetrofitClient.apiService.crearTurno(Turno(nombreTurno = tag))
                }
                Log.d(TAG, "Plato guardado exitosamente en nube: $tag")
                return@withContext true
            }
            return@withContext false
        } catch (e: Exception) {
            Log.e(TAG, "Error guardando plato en backend: ${e.message}")
            return@withContext false
        }
    }

    /**
     * Guarda o elimina en la nube las asignaciones de componentes de la Jefa y de las manipuladoras.
     */
    suspend fun guardarAsignacionEnBackend(
        idUsuario: Int,
        componente: String?,
        estado: String
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            if (idUsuario <= 0) return@withContext false

            val res = RetrofitClient.apiService.obtenerTurnos()
            if (res.isSuccessful && res.body() != null) {
                val prefixUser = "$PREFIX_COMP$idUsuario:"
                val existente = res.body()!!.firstOrNull { it.nombreTurno?.startsWith(prefixUser) == true }

                if (componente.isNullOrBlank()) {
                    if (existente?.idTurno != null) {
                        RetrofitClient.apiService.eliminarTurno(existente.idTurno)
                        Log.d(TAG, "Asignación eliminada de la nube para uid=$idUsuario")
                    }
                    return@withContext true
                }

                val estadoCode = when {
                    estado.contains("proceso", ignoreCase = true) -> "1"
                    estado.contains("final", ignoreCase = true) -> "2"
                    else -> "0"
                }

                val compLimpio = componente.trim().take(34)
                val tag = "$PREFIX_COMP$idUsuario:$estadoCode:$compLimpio"

                if (existente?.idTurno != null) {
                    RetrofitClient.apiService.patchTurno(existente.idTurno, mapOf("nombre_turno" to tag))
                } else {
                    RetrofitClient.apiService.crearTurno(Turno(nombreTurno = tag))
                }
                Log.d(TAG, "Asignación guardada en nube para uid=$idUsuario: $tag")
                return@withContext true
            }
            return@withContext false
        } catch (e: Exception) {
            Log.e(TAG, "Error guardando asignación uid=$idUsuario en backend: ${e.message}")
            return@withContext false
        }
    }

    /**
     * Actualiza el estado de preparación de un componente en el backend en tiempo real.
     */
    suspend fun actualizarEstadoEnBackend(
        context: Context,
        nuevoEstado: String,
        componente: String? = null
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val sessionMgr = SessionManager(context)
            var uid = sessionMgr.getUserId()
            val correo = sessionMgr.getUserEmail()?.lowercase()?.trim() ?: ""
            val rol = sessionMgr.getUserRole()?.lowercase() ?: ""

            if (uid <= 0) {
                val enLista = ManipuladorasManager.obtenerManipuladoras().firstOrNull {
                    it.correo.equals(correo, ignoreCase = true)
                }
                uid = when {
                    enLista != null && enLista.idUsuario > 0 -> enLista.idUsuario
                    rol.contains("jef") || correo.contains("isa") -> 19
                    correo.contains("deis") -> 24
                    correo.contains("pool") -> 21
                    correo.contains("andres") -> 30
                    correo.contains("andrea") -> 20
                    else -> 24
                }
            }

            val comp = componente ?: sessionMgr.getComponenteAsignado(context) ?: "General"
            return@withContext guardarAsignacionEnBackend(uid, comp, nuevoEstado)
        } catch (e: Exception) {
            Log.e(TAG, "Error actualizando estado en backend: ${e.message}")
            return@withContext false
        }
    }
}
