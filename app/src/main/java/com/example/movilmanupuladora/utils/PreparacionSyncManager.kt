package com.example.movilmanupuladora.utils

import android.content.Context
import android.util.Log
import com.example.movilmanupuladora.data.api.RetrofitClient
import com.example.movilmanupuladora.data.model.MenuPaeCatalog
import com.example.movilmanupuladora.data.model.Turno
import com.example.movilmanupuladora.ui.auth.LoginActivity
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

            // 1. Platos seleccionados por servicio (Desayuno, Almuerzo, Merienda) y plato más reciente en la nube
            val turnosPlatos = turnos.filter { it.obtenerNombreReal()?.startsWith(PREFIX_PLATO) == true }
            for (t in turnosPlatos) {
                val data = t.obtenerNombreReal()!!.removePrefix(PREFIX_PLATO).split("#")
                if (data.isNotEmpty()) {
                    var platoNombre = data[0].trim()
                    val jornada = if (data.size > 1 && data[1].isNotBlank()) data[1].trim() else "Mañana"
                    val idPlato = if (data.size > 2) data[2].toIntOrNull() ?: -1 else -1

                    if (idPlato in 1..20) {
                        val itemPae = MenuPaeCatalog.menus.find { it.numeroMenu == idPlato || it.id == idPlato }
                        if (itemPae != null) {
                            platoNombre = itemPae.nombre
                        }
                    }

                    if (platoNombre.isNotBlank()) {
                        val jNorm = jornada.lowercase()
                        val servKey = when {
                            jNorm.contains("desayuno") -> "desayuno"
                            jNorm.contains("almuerzo") -> "almuerzo"
                            jNorm.contains("merienda") -> "merienda"
                            else -> null
                        }
                        if (servKey != null) {
                            jefaEditor.putString("plato_$servKey", platoNombre)
                            jefaEditor.putInt("plato_${servKey}_id", idPlato)
                            turnoEditor.putString("plato_$servKey", platoNombre)
                            turnoEditor.putInt("plato_${servKey}_id", idPlato)
                        }
                    }
                }
            }

            val turnoPlatoMasReciente = turnosPlatos.maxByOrNull { it.obtenerIdReal() ?: 0 }

            if (turnoPlatoMasReciente != null) {
                val data = turnoPlatoMasReciente.obtenerNombreReal()!!.removePrefix(PREFIX_PLATO).split("#")
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

            // 2. Componentes asignados: agrupar por UID y Servicio para que Desayuno, Almuerzo y Merienda mantengan sus propios componentes
            val turnosComp = turnos.filter { it.obtenerNombreReal()?.startsWith(PREFIX_COMP) == true }
            val turnosPorUsuarioYServicio = mutableMapOf<String, Turno>()

            for (t in turnosComp) {
                val raw = t.obtenerNombreReal()!!.removePrefix(PREFIX_COMP)
                val parts = raw.split(":")
                if (parts.size >= 3) {
                    val uid = parts[0].trim().toIntOrNull() ?: continue
                    val servTag = when {
                        parts.size >= 4 && parts[1].contains("almuerzo", ignoreCase = true) -> "Almuerzo"
                        parts.size >= 4 && parts[1].contains("merienda", ignoreCase = true) -> "Merienda"
                        parts.size >= 4 && parts[1].contains("desayuno", ignoreCase = true) -> "Desayuno"
                        t.observaciones?.contains("almuerzo", ignoreCase = true) == true -> "Almuerzo"
                        t.observaciones?.contains("merienda", ignoreCase = true) == true -> "Merienda"
                        else -> "Desayuno"
                    }
                    val key = "$uid:$servTag"
                    val actual = turnosPorUsuarioYServicio[key]
                    if (actual == null || (t.obtenerIdReal() ?: 0) > (actual.obtenerIdReal() ?: 0)) {
                        turnosPorUsuarioYServicio[key] = t
                    }
                }
            }

            for ((keyUS, t) in turnosPorUsuarioYServicio) {
                val partsKey = keyUS.split(":")
                val uid = partsKey[0].toIntOrNull() ?: continue
                val servKeyDef = if (partsKey.size > 1) partsKey[1] else "Desayuno"

                val raw = t.obtenerNombreReal()!!.removePrefix(PREFIX_COMP)
                val parts = raw.split(":")

                var estado: String
                var comp: String
                var servTurno = servKeyDef

                if (parts.size >= 4) {
                    // Formato nuevo: PAE_COMP:uid:servicio:estadoCode:componente
                    servTurno = when {
                        parts[1].contains("almuerzo", ignoreCase = true) -> "Almuerzo"
                        parts[1].contains("merienda", ignoreCase = true) -> "Merienda"
                        else -> "Desayuno"
                    }
                    val estadoCode = parts[2].trim()
                    estado = when (estadoCode) {
                        "1" -> PreparacionEstadoManager.ESTADO_EN_PROCESO
                        "2" -> PreparacionEstadoManager.ESTADO_FINALIZADA
                        else -> PreparacionEstadoManager.ESTADO_POR_HACER
                    }
                    comp = parts.subList(3, parts.size).joinToString(":").trim()
                } else if (parts.size >= 3) {
                    // Formato legacy: PAE_COMP:uid:estadoCode:componente
                    val p1 = parts[1].trim()
                    val p2 = parts.subList(2, parts.size).joinToString(":").trim()
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
                } else {
                    continue
                }

                if (uid > 0 && comp.isNotBlank()) {
                    val obs = t.observaciones ?: ""
                    val obsParts = obs.split("#")
                    val obsCorreo = if (obsParts.isNotEmpty() && obsParts[0].contains("@")) obsParts[0].trim() else ""
                    val obsNombre = if (obsParts.size > 1 && obsParts[1].isNotBlank()) obsParts[1].trim() else ""

                    val rolSession = sessionMgr.getUserRole()?.lowercase() ?: ""
                    val emailSession = sessionMgr.getUserEmail()?.lowercase() ?: ""
                    val uidSession = sessionMgr.getUserId()

                    val esJefaTurno = LoginActivity.esUsuarioJefa(null, obsCorreo) ||
                            obsNombre.contains("jefa", true) ||
                            obsNombre.contains("jefe", true) ||
                            (rolSession.contains("jef") && (uid == uidSession || (obsCorreo.isNotBlank() && obsCorreo.equals(emailSession, true))))

                    if (esJefaTurno) {
                        val nombreJ = if (obsNombre.isNotBlank()) obsNombre else (sessionMgr.getUserName() ?: "Jefa de Cocina")
                        val correoJ = if (obsCorreo.isNotBlank()) obsCorreo else emailSession
                        jefaEditor.putString("comp_jefa", comp)
                        jefaEditor.putString("comp_uid_$uid", comp)
                        PreparacionEstadoManager.guardarEstado(
                            context = context,
                            persona = nombreJ,
                            componente = comp,
                            estado = estado,
                            correo = correoJ
                        )
                        Log.d(TAG, "Componente Jefa sincronizado: $comp ($estado) para $nombreJ")
                    } else {
                        val obsJornada = if (obsParts.size > 5 && obsParts[5].isNotBlank()) obsParts[5].trim() else ""
                        val obsFecha = if (obsParts.size > 6 && obsParts[6].isNotBlank()) obsParts[6].trim() else ""

                        val manip = listaManip.firstOrNull { it.idUsuario == uid || it.id == uid }
                        val manipNombre = if (obsNombre.isNotBlank()) obsNombre else (manip?.nombre ?: jefaPrefs.getString("manip_nombre_uid_$uid", "Manipuladora") ?: "Manipuladora")
                        val manipCorreo = if (obsCorreo.isNotBlank()) obsCorreo else (manip?.correo ?: jefaPrefs.getString("manip_correo_uid_$uid", "") ?: "")

                        val normNombre = normalizar(manipNombre)
                        jefaEditor.putString("comp_$normNombre", comp)
                        jefaEditor.putString("comp_${servTurno}_$normNombre", comp)

                        val primerNombre = manipNombre.trim().split(" ").firstOrNull() ?: ""
                        if (primerNombre.isNotBlank()) {
                            jefaEditor.putString("comp_" + normalizar(primerNombre), comp)
                            jefaEditor.putString("comp_${servTurno}_" + normalizar(primerNombre), comp)
                        }

                        if (manipCorreo.isNotBlank()) {
                            val keyCorreo = "comp_correo_" + normalizar(manipCorreo)
                            jefaEditor.putString(keyCorreo, comp)
                            jefaEditor.putString("comp_correo_${servTurno}_" + normalizar(manipCorreo), comp)
                        }

                        jefaEditor.putString("comp_uid_$uid", comp)
                        jefaEditor.putString("comp_uid_${servTurno}_$uid", comp)
                        jefaEditor.putString("manip_nombre_uid_$uid", manipNombre)
                        jefaEditor.putString("manip_correo_uid_$uid", manipCorreo)

                        // Guardar también en SharedPreferences ASIGNACIONES_PAE para que cualquier vista lo encuentre
                        try {
                            val asigPrefs = context.getSharedPreferences("ASIGNACIONES_PAE", Context.MODE_PRIVATE)
                            val asigEd = asigPrefs.edit()
                            val fechaHoySync = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                            val fKey = if (obsFecha.isNotBlank()) obsFecha else fechaHoySync
                            val sKey = if (servTurno.isNotBlank()) servTurno else when {
                                obsJornada.contains("almuerzo", ignoreCase = true) -> "Almuerzo"
                                obsJornada.contains("merienda", ignoreCase = true) -> "Merienda"
                                else -> "Desayuno"
                            }

                            if (manipCorreo.isNotBlank()) {
                                val normC = normalizar(manipCorreo)
                                asigEd.putString("comp_correo_$normC", comp)
                                asigEd.putString("comp_correo_${sKey}_$normC", comp)
                                asigEd.putString("asig_correo_${fKey}_${sKey}_$comp", manipCorreo)
                            }
                            asigEd.putString("comp_uid_$uid", comp)
                            asigEd.putString("comp_uid_${sKey}_$uid", comp)
                            asigEd.putString("comp_nombre_$normNombre", comp)
                            asigEd.putString("comp_nombre_${sKey}_$normNombre", comp)
                            asigEd.putString("asig_${fKey}_${sKey}_$comp", manipNombre)
                            asigEd.putInt("asig_uid_${fKey}_${sKey}_$comp", uid)

                            // Mapeo directo explícito para Pool Gómez y Deisy
                            if (manipCorreo.contains("pool", true) || manipNombre.contains("pool", true) || uid == 21) {
                                asigEd.putString("comp_pool", comp)
                                asigEd.putString("comp_pool_$sKey", comp)
                                asigEd.putString("comp_uid_21", comp)
                                asigEd.putString("comp_uid_${sKey}_21", comp)
                                asigEd.putString("asig_pool_${fKey}_$sKey", comp)
                            }
                            if (manipCorreo.contains("deis", true) || manipNombre.contains("deis", true) || uid == 24) {
                                asigEd.putString("comp_deis", comp)
                                asigEd.putString("comp_deis_$sKey", comp)
                                asigEd.putString("comp_uid_24", comp)
                                asigEd.putString("comp_uid_${sKey}_24", comp)
                                asigEd.putString("asig_deis_${fKey}_$sKey", comp)
                            }

                            asigEd.apply()
                        } catch (_: Exception) { }

                        PreparacionEstadoManager.guardarEstado(
                            context = context,
                            persona = manipNombre,
                            componente = comp,
                            estado = estado,
                            correo = manipCorreo
                        )

                        Log.d(TAG, "Componente de $manipNombre (uid=$uid, correo=$manipCorreo, serv=$servTurno) sincronizado: $comp ($estado)")
                    }
                }
            }

            // 3. Asistencia de estudiantes compartida en tiempo real (priorizar hoy y más reciente)
            val fechaHoy = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            val turnosAsist = turnos.filter { it.obtenerNombreReal()?.startsWith(PREFIX_ASIST) == true }

            val turnoAsist = turnosAsist.firstOrNull { t ->
                val p = t.obtenerNombreReal()!!.removePrefix(PREFIX_ASIST).split(":")
                val f = if (p.size >= 4) p[3].trim() else ""
                f == fechaHoy || f.replace("-0", "-") == fechaHoy.replace("-0", "-")
            } ?: turnosAsist.maxByOrNull { it.obtenerIdReal() ?: 0 }

            if (turnoAsist != null) {
                val parts = turnoAsist.obtenerNombreReal()!!.removePrefix(PREFIX_ASIST).split(":")
                if (parts.size >= 3) {
                    val prim = parts[0].trim().toIntOrNull() ?: 0
                    val sec = parts[1].trim().toIntOrNull() ?: 0
                    val tot = parts[2].trim().toIntOrNull() ?: (prim + sec)
                    val jornada = if (parts.size >= 5) parts[4].trim() else "MANANA"
                    if (tot > 0) {
                        AsistenciaManager.guardarPrimaria(context, prim)
                        AsistenciaManager.guardarSecundaria(context, sec)
                        if (jornada.contains("tarde", ignoreCase = true)) {
                            AsistenciaManager.guardarTarde(context, tot)
                        } else {
                            AsistenciaManager.guardarManana(context, tot)
                        }

                        // Restaurar el desglose detallado de grados desde observaciones
                        val obsAsist = turnoAsist.observaciones.orEmpty()
                        if (obsAsist.contains("=") && obsAsist.contains(";")) {
                            val mapaDetalle = mutableMapOf<String, String>()
                            obsAsist.split(";").forEach { item ->
                                val kv = item.split("=")
                                if (kv.size == 2) {
                                    mapaDetalle[kv[0].trim()] = kv[1].trim()
                                }
                            }
                            if (mapaDetalle.isNotEmpty()) {
                                AsistenciaManager.guardarDetalleGradosHoy(context, mapaDetalle)
                            }
                        }

                        Log.d(TAG, "Asistencia sincronizada desde Turnos: Primaria=$prim, Secundaria=$sec, Total=$tot ($jornada)")
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
                val rolSession = sessionMgr.getUserRole()?.lowercase() ?: ""
                val emailSession = sessionMgr.getUserEmail()?.lowercase() ?: ""
                val uidSession = sessionMgr.getUserId()
                val esJefaAsignada = LoginActivity.esUsuarioJefa(null, a.correo) ||
                        a.nombre.contains("jefa", true) ||
                        a.nombre.contains("jefe", true) ||
                        (rolSession.contains("jef") && (a.idUsuario == uidSession || (a.correo.isNotBlank() && a.correo.equals(emailSession, true))))

                if (esJefaAsignada) {
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
                val servLimpio = when {
                    jornada.contains("almuerzo", ignoreCase = true) -> "Almuerzo"
                    jornada.contains("merienda", ignoreCase = true) -> "Merienda"
                    else -> "Desayuno"
                }
                val tagPlato = "$PREFIX_PLATO${nombreMenu.trim().take(30)}#$servLimpio#${idPlato ?: -1}"
                val turnosPlatos = todosTurnos.filter { it.obtenerNombreReal()?.startsWith(PREFIX_PLATO) == true }
                val existentePlato = turnosPlatos.firstOrNull {
                    val raw = it.obtenerNombreReal()!!.removePrefix(PREFIX_PLATO).split("#")
                    raw.size > 1 && raw[1].trim().equals(servLimpio, ignoreCase = true)
                } ?: turnosPlatos.firstOrNull()

                val idP = existentePlato?.obtenerIdReal()
                if (idP != null) {
                    try { RetrofitClient.apiService.patchTurno(idP, mapOf("nombre_turno" to tagPlato)) } catch (_: Exception) { }
                } else {
                    try { RetrofitClient.apiService.crearTurno(Turno(nombreTurno = tagPlato, horaInicio = "06:00:00", horaFin = "14:00:00")) } catch (_: Exception) { }
                }

                // 2. Guardar o actualizar asignación de cada miembro con discriminación por servicio
                for (a in asignaciones) {
                    val uidFinal = if (a.idUsuario > 0) a.idUsuario else {
                        val m = ManipuladorasManager.obtenerManipuladoras().firstOrNull {
                            it.correo.equals(a.correo, true) || it.nombre.equals(a.nombre, true)
                        }
                        when {
                            m != null && m.idUsuario > 0 -> m.idUsuario
                            a.correo.contains("pool", true) || a.nombre.contains("pool", true) -> 21
                            a.correo.contains("deis", true) || a.nombre.contains("deis", true) -> 24
                            a.correo.contains("andrea", true) || a.nombre.contains("andrea", true) -> 20
                            a.correo.contains("andres", true) || a.nombre.contains("andres", true) -> 30
                            else -> 20 + asignaciones.indexOf(a)
                        }
                    }

                    val prefixExact = "$PREFIX_COMP$uidFinal:$servLimpio:"
                    val prefixLegacy = "$PREFIX_COMP$uidFinal:"
                    val existentesComp = todosTurnos.filter {
                        val nom = it.obtenerNombreReal().orEmpty()
                        nom.startsWith(prefixExact) || (nom.startsWith(prefixLegacy) && !nom.contains("Desayuno") && !nom.contains("Almuerzo") && !nom.contains("Merienda"))
                    }

                    if (a.componente.isNullOrBlank()) {
                        for (ex in existentesComp) {
                            val idC = ex.obtenerIdReal() ?: continue
                            try { RetrofitClient.apiService.eliminarTurno(idC) } catch (_: Exception) { }
                        }
                    } else {
                        val estadoCode = when {
                            a.estado.contains("proceso", ignoreCase = true) -> "1"
                            a.estado.contains("final", ignoreCase = true) -> "2"
                            else -> "0"
                        }
                        val compLimpio = a.componente.trim().take(34)
                        val tagComp = "$PREFIX_COMP$uidFinal:$servLimpio:$estadoCode:$compLimpio"
                        val fechaHoyObs = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                        val obs = "${a.correo.trim()}#${a.nombre.trim()}#${a.componente.trim()}#$uidFinal#$estadoCode#$servLimpio#$fechaHoyObs"

                        if (existentesComp.isNotEmpty()) {
                            for (ex in existentesComp) {
                                val idC = ex.obtenerIdReal() ?: continue
                                var patchRes = try {
                                    RetrofitClient.apiService.patchTurno(idC, mapOf("nombre_turno" to tagComp, "observaciones" to obs))
                                } catch (_: Exception) { null }

                                if (patchRes == null || !patchRes.isSuccessful) {
                                    try {
                                        RetrofitClient.apiService.patchTurno(idC, mapOf("nombre_turno" to tagComp))
                                    } catch (_: Exception) { }
                                }
                            }
                        } else {
                            var postRes = try {
                                RetrofitClient.apiService.crearTurno(Turno(nombreTurno = tagComp, horaInicio = "06:00:00", horaFin = "14:00:00", observaciones = obs))
                            } catch (_: Exception) { null }

                            if (postRes == null || !postRes.isSuccessful) {
                                try {
                                    RetrofitClient.apiService.crearTurno(Turno(nombreTurno = tagComp, horaInicio = "06:00:00", horaFin = "14:00:00"))
                                } catch (_: Exception) { }
                            }
                        }
                    }
                }
                Log.d(TAG, "Asignaciones ($servLimpio) y plato guardados exitosamente en nube")
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
        context: Context,
        primaria: Int,
        secundaria: Int,
        total: Int,
        jornada: String = "MANANA"
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            SessionManager.getToken(context)
            val fechaHoy = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            val tag = "$PREFIX_ASIST$primaria:$secundaria:$total:$fechaHoy:$jornada"

            val detalle = AsistenciaManager.obtenerDetalleGradosHoy(context)
            val obsDetalle = if (detalle.isNotEmpty()) {
                detalle.entries.joinToString(";") { "${it.key}=${it.value}" }
            } else ""

            val res = RetrofitClient.apiService.obtenerTurnos()
            if (res.isSuccessful && res.body() != null) {
                val turnosAsist = res.body()!!.filter { it.obtenerNombreReal()?.startsWith(PREFIX_ASIST) == true }

                val turnoHoy = turnosAsist.firstOrNull { t ->
                    val p = t.obtenerNombreReal()!!.removePrefix(PREFIX_ASIST).split(":")
                    p.size >= 4 && (p[3].trim() == fechaHoy || p[3].trim().replace("-0", "-") == fechaHoy.replace("-0", "-"))
                } ?: turnosAsist.maxByOrNull { it.obtenerIdReal() ?: 0 }

                val maxId = turnoHoy?.obtenerIdReal()
                if (maxId != null) {
                    var patchRes = try {
                        RetrofitClient.apiService.patchTurno(maxId, mapOf("nombre_turno" to tag, "observaciones" to obsDetalle))
                    } catch (_: Exception) { null }

                    if (patchRes == null || !patchRes.isSuccessful) {
                        try {
                            RetrofitClient.apiService.patchTurno(maxId, mapOf("nombre_turno" to tag))
                        } catch (_: Exception) { }
                    }
                } else {
                    var postRes = try {
                        RetrofitClient.apiService.crearTurno(Turno(nombreTurno = tag, horaInicio = "06:00:00", horaFin = "14:00:00", observaciones = obsDetalle))
                    } catch (_: Exception) { null }

                    if (postRes == null || !postRes.isSuccessful) {
                        try {
                            RetrofitClient.apiService.crearTurno(Turno(nombreTurno = tag, horaInicio = "06:00:00", horaFin = "14:00:00"))
                        } catch (_: Exception) { }
                    }
                }
                // Sincronizar también cualquier otro turno duplicado para consistencia total
                for (otro in turnosAsist) {
                    val otroId = otro.obtenerIdReal()
                    if (otroId != null && otroId != maxId) {
                        try {
                            RetrofitClient.apiService.patchTurno(otroId, mapOf("nombre_turno" to tag))
                        } catch (_: Exception) { }
                    }
                }
                Log.d(TAG, "Asistencia guardada en Turnos en la nube: $tag")
                return@withContext true
            } else {
                Log.e(TAG, "Error al obtener turnos en guardarAsistenciaEnNube: HTTP ${res.code()}")
                return@withContext false
            }
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
                val existente = res.body()!!.firstOrNull { it.obtenerNombreReal()?.startsWith(PREFIX_PLATO) == true }
                val idT = existente?.obtenerIdReal()
                if (idT != null) {
                    RetrofitClient.apiService.patchTurno(idT, mapOf("nombre_turno" to tag))
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
     * Guarda el plato para un servicio específico (Desayuno, Almuerzo, Merienda).
     * Si este mismo plato estaba seleccionado en otro servicio, lo desasigna de dicho servicio.
     * Si otro plato estaba asignado a este servicio, lo reemplaza.
     */
    suspend fun guardarPlatoServicio(
        context: Context,
        servicio: String,
        nombrePlato: String,
        idPlato: Int?,
        fecha: String? = null
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val sessionMgr = SessionManager(context)
            val servCapitalizado = when (servicio.lowercase()) {
                "desayuno" -> "Desayuno"
                "almuerzo" -> "Almuerzo"
                "merienda" -> "Merienda"
                else -> servicio.replaceFirstChar { it.uppercase() }
            }
            val servKey = servCapitalizado.lowercase()

            // 1. Si este plato estaba asignado en otro servicio para esta fecha, desasignarlo de allí
            val otrosServicios = listOf("Desayuno", "Almuerzo", "Merienda").filter { !it.equals(servCapitalizado, ignoreCase = true) }
            for (otro in otrosServicios) {
                val platoOtro = if (!fecha.isNullOrBlank()) {
                    sessionMgr.getPlatoDiaServicio(context, fecha, otro)
                } else {
                    sessionMgr.getPlatoServicio(context, otro)
                }
                if (platoOtro != null && (platoOtro.equals(nombrePlato, ignoreCase = true) || platoOtro.contains(nombrePlato, ignoreCase = true) || nombrePlato.contains(platoOtro, ignoreCase = true))) {
                    if (!fecha.isNullOrBlank()) {
                        sessionMgr.desasignarPlatoDiaServicio(context, fecha, otro)
                    } else {
                        sessionMgr.desasignarPlatoServicio(context, otro)
                    }
                }
            }

            // 2. Guardar en SharedPreferences y SessionManager
            if (!fecha.isNullOrBlank()) {
                sessionMgr.savePlatoDiaServicio(context, fecha, servCapitalizado, nombrePlato, idPlato)
            } else {
                sessionMgr.savePlatoServicio(context, servCapitalizado, nombrePlato, idPlato)
            }
            sessionMgr.savePlatoSeleccionado(nombrePlato, idPlato)

            val jefaPrefs = context.getSharedPreferences("ManipuladoraJefe", Context.MODE_PRIVATE)
            val turnoPrefs = context.getSharedPreferences("TURNO_PAE", Context.MODE_PRIVATE)

            jefaPrefs.edit()
                .putString("plato_actual", nombrePlato)
                .putString("jornada", servCapitalizado)
                .putString("plato_$servKey", nombrePlato)
                .putInt("plato_${servKey}_id", idPlato ?: -1)
                .apply()

            turnoPrefs.edit()
                .putString("plato_nombre", nombrePlato)
                .putString("plato_jornada", servCapitalizado)
                .putInt("plato_id", idPlato ?: -1)
                .putString("plato_$servKey", nombrePlato)
                .putInt("plato_${servKey}_id", idPlato ?: -1)
                .apply()

            // 3. Persistir en la nube (backend DRF turnos)
            val tag = "$PREFIX_PLATO${nombrePlato.trim().take(30)}#${servCapitalizado}#${idPlato ?: -1}"
            SessionManager.getToken(context)
            val res = RetrofitClient.apiService.obtenerTurnos()
            if (res.isSuccessful && res.body() != null) {
                val turnosPlatos = res.body()!!.filter { it.obtenerNombreReal()?.startsWith(PREFIX_PLATO) == true }

                // Actualizar o limpiar cualquier turno del mismo plato en otro servicio
                for (t in turnosPlatos) {
                    val raw = t.obtenerNombreReal()!!.removePrefix(PREFIX_PLATO).split("#")
                    if (raw.size > 1) {
                        val j = raw[1].trim()
                        val pNom = raw[0].trim()
                        val idT = t.obtenerIdReal() ?: continue

                        // Si es el turno de este servicio, reemplazarlo con el nuevo plato
                        if (j.equals(servCapitalizado, ignoreCase = true)) {
                            RetrofitClient.apiService.patchTurno(idT, mapOf("nombre_turno" to tag))
                            Log.d(TAG, "Plato de $servCapitalizado actualizado en nube: $tag")
                            return@withContext true
                        }
                    }
                }

                // Si no existía turno previo para este servicio, crearlo
                RetrofitClient.apiService.crearTurno(Turno(nombreTurno = tag))
                Log.d(TAG, "Plato de $servCapitalizado creado en nube: $tag")
                return@withContext true
            }
            return@withContext false
        } catch (e: Exception) {
            Log.e(TAG, "Error guardando plato de servicio en backend: ${e.message}")
            return@withContext false
        }
    }

    /**
     * Desasigna un plato de un servicio y fecha.
     */
    suspend fun desasignarPlatoServicio(
        context: Context,
        servicio: String,
        fecha: String? = null
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val sessionMgr = SessionManager(context)
            if (!fecha.isNullOrBlank()) {
                sessionMgr.desasignarPlatoDiaServicio(context, fecha, servicio)
            } else {
                sessionMgr.desasignarPlatoServicio(context, servicio)
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error desasignando plato de servicio: ${e.message}")
            false
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
                val existente = res.body()!!.firstOrNull { it.obtenerNombreReal()?.startsWith(prefixUser) == true }
                val idT = existente?.obtenerIdReal()

                if (componente.isNullOrBlank()) {
                    if (idT != null) {
                        RetrofitClient.apiService.eliminarTurno(idT)
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

                if (idT != null) {
                    RetrofitClient.apiService.patchTurno(idT, mapOf("nombre_turno" to tag))
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

    /**
     * Descuenta del inventario los ingredientes requeridos cuando un componente
     * o plato es marcado como FINALIZADO.
     * Si no se finaliza (ej. no hubo clases ese día), no se descuenta nada del inventario.
     */
    suspend fun descontarInventarioPorFinalizacion(
        context: Context,
        nombrePlato: String,
        componente: String? = null
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val fechaHoy = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            val deduccionKey = "deduccion_inv_${fechaHoy}_${normalizar(componente ?: nombrePlato)}"
            val prefs = context.getSharedPreferences("INVENTARIO_DEDUCCIONES", Context.MODE_PRIVATE)

            // Evitar doble descuento para el mismo componente/plato en el mismo día
            if (prefs.getBoolean(deduccionKey, false)) {
                Log.d(TAG, "Inventario ya descontado previamente hoy para: $componente ($nombrePlato)")
                return@withContext true
            }

            // 1. Obtener número de niños presentes
            val p = AsistenciaManager.obtenerPrimaria(context)
            val s = AsistenciaManager.obtenerSecundaria(context)
            val m = AsistenciaManager.obtenerManana(context)
            val ninos = (p + s).takeIf { it > 0 } ?: (if (m > 0) m else 50)

            // 2. Determinar ingredientes a descontar según el componente o receta
            val compLimpio = (componente ?: nombrePlato).lowercase().trim()
            val ingredientesConsumo = mutableMapOf<String, Double>()

            when {
                compLimpio.contains("arroz") -> {
                    ingredientesConsumo["arroz"] = (ninos * 0.08).coerceAtLeast(3.0)
                    ingredientesConsumo["aceite"] = (ninos * 0.015).coerceAtLeast(0.5)
                }
                compLimpio.contains("pollo") -> {
                    ingredientesConsumo["pollo"] = (ninos * 0.12).coerceAtLeast(5.0)
                    ingredientesConsumo["aceite"] = (ninos * 0.015).coerceAtLeast(0.5)
                }
                compLimpio.contains("carne") -> {
                    ingredientesConsumo["carne"] = (ninos * 0.12).coerceAtLeast(5.0)
                }
                compLimpio.contains("huevo") -> {
                    ingredientesConsumo["huevos"] = ninos.toDouble().coerceAtLeast(10.0)
                    ingredientesConsumo["aceite"] = (ninos * 0.01).coerceAtLeast(0.3)
                }
                compLimpio.contains("frijol") || compLimpio.contains("lenteja") -> {
                    ingredientesConsumo["frijol"] = (ninos * 0.07).coerceAtLeast(3.0)
                }
                compLimpio.contains("leche") || compLimpio.contains("avena") || compLimpio.contains("chocolate") -> {
                    ingredientesConsumo["leche"] = (ninos * 0.20).coerceAtLeast(8.0)
                }
                compLimpio.contains("fruta") || compLimpio.contains("papaya") || compLimpio.contains("mango") || compLimpio.contains("banano") -> {
                    ingredientesConsumo["fruta"] = ninos.toDouble().coerceAtLeast(15.0)
                }
                else -> {
                    // Descuento estándar proporcional según el componente
                    ingredientesConsumo[compLimpio] = (ninos * 0.06).coerceAtLeast(2.0)
                }
            }

            // 3. Consultar inventario del backend y actualizar stock
            SessionManager.getToken(context)
            val invRes = RetrofitClient.apiService.obtenerInventario()
            if (invRes.isSuccessful && !invRes.body().isNullOrEmpty()) {
                val inventarios = invRes.body()!!

                for ((ingNom, cantConsumida) in ingredientesConsumo) {
                    val itemInv = inventarios.firstOrNull { inv ->
                        val n = inv.idIngrediente?.toString()?.lowercase() ?: ""
                        n.contains(ingNom) || ingNom.contains(n)
                    } ?: inventarios.firstOrNull()

                    if (itemInv != null && itemInv.idInventario != null) {
                        val cantActual = itemInv.cantidadActual?.toDoubleOrNull() ?: 15.0
                        val nuevaCant = (cantActual - cantConsumida).coerceAtLeast(0.0)
                        val payload = itemInv.copy(
                            cantidadActual = String.format(Locale.US, "%.2f", nuevaCant)
                        )
                        try {
                            RetrofitClient.apiService.actualizarInventario(itemInv.idInventario, payload)
                            Log.d(TAG, "Deducción de inventario exitosa: $ingNom -$cantConsumida -> Quedan $nuevaCant")
                        } catch (e: Exception) {
                            Log.w(TAG, "No se pudo actualizar inventario en backend para $ingNom: ${e.message}")
                        }
                    }
                }
            }

            prefs.edit().putBoolean(deduccionKey, true).apply()
            Log.d(TAG, "Inventario descontado exitosamente para $componente ($nombrePlato)")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error al descontar inventario: ${e.message}")
            false
        }
    }
}
