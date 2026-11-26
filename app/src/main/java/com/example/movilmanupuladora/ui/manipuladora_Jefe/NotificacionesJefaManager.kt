package com.example.movilmanupuladora.ui.manipuladora_Jefe

import android.content.Context
import com.example.movilmanupuladora.data.api.RetrofitClient
import com.example.movilmanupuladora.utils.SessionManager
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object NotificacionesJefaManager {

    data class NotificacionItem(
        val id: String,
        val titulo: String,
        val mensaje: String,
        val fecha: String,
        val categoria: String, // "Equipo", "Inventario", "Preparaciones", "Asistencia"
        val timestamp: Long = System.currentTimeMillis(),
        var leida: Boolean = false,
        val numeroRemision: String? = null
    )

    private val gson = Gson()

    /**
     * Obtiene el par de claves únicas de la sede de la Jefa actual:
     * (clave por ID numérico si existe, clave por nombre normalizado).
     * Esto previene que una notificación se pierda si la sede fue guardada primero
     * con nombre y luego sincronizada con ID, o viceversa.
     */
    fun obtenerClavesSede(context: Context): Pair<String, String> {
        val session = SessionManager(context)
        val idSede = session.getSedeId().takeIf { it > 0 }
        val idKey = if (idSede != null) "sede_$idSede" else ""

        val nombre = session.getSedeNombre()?.trim()?.lowercase().orEmpty()
        val limpia = nombre
            .replace("i.e.", " ")
            .replace("i.e", " ")
            .replace("ie", " ")
            .replace("sede", " ")
            .replace("·", " ")
            .replace("-", " ")
            .trim()
            .replace("\\s+".toRegex(), "_")
        val nameKey = "sede_${limpia.ifBlank { "general" }}"
        return Pair(idKey, nameKey)
    }

    fun obtenerClaveSede(context: Context): String {
        val (idKey, nameKey) = obtenerClavesSede(context)
        return if (idKey.isNotBlank()) idKey else nameKey
    }

    /**
     * Retorna todas las notificaciones históricas de la sede actual,
     * ordenadas cronológicamente con las más recientes primero.
     * Busca tanto por ID de sede como por nombre de sede para compatibilidad total.
     */
    fun obtenerNotificacionesSede(context: Context): List<NotificacionItem> {
        val (idKey, nameKey) = obtenerClavesSede(context)
        val prefs = context.getSharedPreferences("JEFA_NOTIFS_STORE", Context.MODE_PRIVATE)

        var json: String? = null
        if (idKey.isNotBlank()) {
            json = prefs.getString("NOTIFS_$idKey", null)
        }
        if (json.isNullOrBlank()) {
            json = prefs.getString("NOTIFS_$nameKey", null)
        }
        if (json.isNullOrBlank()) return emptyList()

        val leidas = obtenerConjuntoLeidas(context)

        return try {
            val type = object : TypeToken<List<NotificacionItem>>() {}.type
            val lista: List<NotificacionItem> = gson.fromJson(json, type) ?: emptyList()
            lista.map {
                it.copy(leida = it.leida || leidas.contains(it.id))
            }.sortedByDescending { it.timestamp }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun obtenerConjuntoLeidas(context: Context): MutableSet<String> {
        val prefs = context.getSharedPreferences("JEFA_NOTIFS_STORE", Context.MODE_PRIVATE)
        val csv = prefs.getString("NOTIFS_LEIDAS_CSV", "") ?: ""
        val set = csv.split("|").map { it.trim() }.filter { it.isNotEmpty() }.toMutableSet()
        // Migrar si existían en el StringSet anterior
        val legacy = prefs.getStringSet("NOTIFS_LEIDAS", emptySet())
        if (!legacy.isNullOrEmpty()) {
            set.addAll(legacy)
        }
        return set
    }

    fun marcarLeida(context: Context, id: String) {
        val prefs = context.getSharedPreferences("JEFA_NOTIFS_STORE", Context.MODE_PRIVATE)
        val leidas = obtenerConjuntoLeidas(context)
        if (leidas.add(id)) {
            prefs.edit()
                .putString("NOTIFS_LEIDAS_CSV", leidas.joinToString("|"))
                .apply()
        }
    }

    fun esLeida(context: Context, id: String): Boolean {
        return obtenerConjuntoLeidas(context).contains(id)
    }

    fun obtenerConteoNoLeidas(context: Context): Int {
        val notifs = obtenerNotificacionesSede(context)
        val leidas = obtenerConjuntoLeidas(context)
        return notifs.count { !it.leida && !leidas.contains(it.id) }
    }

    /**
     * Registra una nueva notificación para la sede de la jefa actual
     * estampando la fecha y hora exacta del suceso.
     */
    fun agregarNotificacionSede(
        context: Context,
        titulo: String,
        mensaje: String,
        categoria: String
    ) {
        val listaActual = obtenerNotificacionesSede(context).toMutableList()

        val id = "${System.currentTimeMillis()}_${(100..999).random()}"
        val fechaFormateada = try {
            SimpleDateFormat("dd MMM, hh:mm a", Locale("es", "CO")).format(Date())
        } catch (_: Exception) {
            "Reciente"
        }

        val nueva = NotificacionItem(
            id = id,
            titulo = titulo,
            mensaje = mensaje,
            fecha = fechaFormateada,
            categoria = categoria,
            timestamp = System.currentTimeMillis()
        )

        // Evitar duplicar exactamente la misma notificación en la misma sede
        if (listaActual.none { it.titulo == titulo && it.mensaje == mensaje }) {
            listaActual.add(0, nueva)
            guardarLista(context, listaActual)
        }
    }

    /**
     * Sincroniza avisos y notificaciones históricas que provengan del backend (Django)
     * respetando sus fechas originales emitidas por el administrador o el sistema.
     */
    suspend fun sincronizarNotificacionesDelBackend(context: Context) {
        try {
            val res = withContext(Dispatchers.IO) {
                RetrofitClient.apiService.obtenerNotificaciones()
            }
            val listaActual = obtenerNotificacionesSede(context).toMutableList()

            if (res.isSuccessful && !res.body().isNullOrEmpty()) {
                val notifsBackend = res.body()!!

                for (nb in notifsBackend) {
                    val tit = nb.titulo?.trim().orEmpty()
                    val msg = nb.mensaje?.trim().orEmpty()
                    if (tit.isBlank() && msg.isBlank()) continue

                    val fechaFormateada = formatearFechaBackend(nb.fecha)
                    val tsBackend = parsearTimestampBackend(nb.fecha) ?: (System.currentTimeMillis() - 86400000L)
                    val textoCompleto = "$tit $msg"
                    val esEntrega = textoCompleto.contains("entrega", ignoreCase = true) ||
                            textoCompleto.contains("remisi", ignoreCase = true) ||
                            textoCompleto.contains("despach", ignoreCase = true) ||
                            textoCompleto.contains("factura", ignoreCase = true) ||
                            textoCompleto.contains("FAC", ignoreCase = true) ||
                            textoCompleto.contains("#REM", ignoreCase = true)

                    val cat = when {
                        esEntrega -> "Inventario"
                        nb.tipo.equals("inventario", ignoreCase = true) || tit.contains("inventario", ignoreCase = true) -> "Inventario"
                        nb.tipo.equals("preparacion", ignoreCase = true) || tit.contains("prep", ignoreCase = true) -> "Preparaciones"
                        nb.tipo.equals("asistencia", ignoreCase = true) || tit.contains("asist", ignoreCase = true) -> "Asistencia"
                        else -> "Equipo"
                    }

                    val remMatch = Regex("(?:#REM-|FAC-?)[0-9A-Za-z-]+", RegexOption.IGNORE_CASE).find(textoCompleto)?.value

                    val notifId = "backend_${nb.idNotificacion ?: System.currentTimeMillis()}"
                    if (nb.leido == true) {
                        marcarLeida(context, notifId)
                    }
                    val idxExistente = listaActual.indexOfFirst {
                        it.id == notifId || (it.titulo == tit && it.mensaje == msg)
                    }
                    if (idxExistente < 0) {
                        listaActual.add(
                            NotificacionItem(
                                id = notifId,
                                titulo = tit,
                                mensaje = msg,
                                fecha = fechaFormateada,
                                categoria = cat,
                                timestamp = tsBackend,
                                leida = nb.leido == true,
                                numeroRemision = remMatch
                            )
                        )
                    } else {
                        // Actualizar fecha real, timestamp y estado leído si antes estaba en "Reciente" o sin leer
                        val previo = listaActual[idxExistente]
                        listaActual[idxExistente] = previo.copy(
                            id = notifId,
                            fecha = if (fechaFormateada != "Reciente") fechaFormateada else previo.fecha,
                            timestamp = if (tsBackend > 0) tsBackend else previo.timestamp,
                            leida = previo.leida || (nb.leido == true)
                        )
                    }
                }
            }

            // Sincronizar entregas en tránsito despachadas por el supervisor para esta sede
            try {
                val session = SessionManager(context)
                val idSede = session.getSedeId().takeIf { it > 0 }
                val resEntregas = withContext(Dispatchers.IO) {
                    RetrofitClient.apiService.obtenerEntregas(
                        idSede = idSede,
                        estado = "EN_TRANSITO"
                    )
                }
                if (resEntregas.isSuccessful && !resEntregas.body().isNullOrEmpty()) {
                    val entregas = resEntregas.body()!!
                    val agrupadas = entregas.groupBy { it.documentoEntrega ?: "#REM-${it.idEntrega ?: 0}" }

                    for ((rem, items) in agrupadas) {
                        val tit = "Nueva entrega despachada: $rem"
                        val nomSup = items.firstOrNull()?.observaciones?.let { obs ->
                            val match = Regex("Responsable:\\s*([^|;\\n]+)", RegexOption.IGNORE_CASE).find(obs)
                            match?.groupValues?.get(1)?.trim()
                        } ?: "Supervisor PAE"
                        val msg = "El $nomSup ha despachado la remisión $rem con ${items.size} alimentos para tu sede. Toca aquí para revisar cantidades y firmar el acta."

                        val entregaId = "entrega_$rem"
                        val yaExiste = listaActual.any { it.id == entregaId || it.numeroRemision == rem || it.titulo.endsWith(rem) }
                        if (!yaExiste) {
                            listaActual.add(
                                0,
                                NotificacionItem(
                                    id = entregaId,
                                    titulo = tit,
                                    mensaje = msg,
                                    fecha = SimpleDateFormat("dd MMM, hh:mm a", Locale("es", "CO")).format(Date()),
                                    categoria = "Inventario",
                                    timestamp = System.currentTimeMillis(),
                                    numeroRemision = rem
                                )
                            )
                        }
                    }
                }
            } catch (_: Exception) {}

            guardarLista(context, listaActual)
        } catch (_: Exception) { }
    }

    private fun parsearTimestampBackend(fechaRaw: String?): Long? {
        if (fechaRaw.isNullOrBlank()) return null
        val formatos = listOf(
            "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX",
            "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
            "yyyy-MM-dd'T'HH:mm:ssXXX",
            "yyyy-MM-dd'T'HH:mm:ss.SSSSSS",
            "yyyy-MM-dd'T'HH:mm:ss.SSS",
            "yyyy-MM-dd'T'HH:mm:ss",
            "yyyy-MM-dd HH:mm:ss",
            "yyyy-MM-dd"
        )
        for (fmt in formatos) {
            try {
                val parser = SimpleDateFormat(fmt, Locale.getDefault())
                val fecha = parser.parse(fechaRaw)
                if (fecha != null) return fecha.time
            } catch (_: Exception) { }
        }
        return null
    }

    private fun formatearFechaBackend(fechaRaw: String?): String {
        if (fechaRaw.isNullOrBlank()) return "Reciente"
        if (fechaRaw.contains(",") || fechaRaw.contains("·")) return fechaRaw
        val ts = parsearTimestampBackend(fechaRaw) ?: return fechaRaw
        val diffMs = System.currentTimeMillis() - ts
        if (diffMs in 0..59_999L) return "Hace un momento"
        val mins = diffMs / 60_000L
        if (mins in 1..59) return "Hace $mins min"
        val horas = diffMs / 3_600_000L
        if (horas in 1..23) return "Hace $horas h"
        val dias = diffMs / 86_400_000L
        if (dias == 1L) return "Ayer"
        return try {
            SimpleDateFormat("dd MMM, hh:mm a", Locale("es", "CO")).format(Date(ts))
        } catch (_: Exception) {
            fechaRaw
        }
    }

    private fun guardarLista(context: Context, lista: List<NotificacionItem>) {
        val (idKey, nameKey) = obtenerClavesSede(context)
        val prefs = context.getSharedPreferences("JEFA_NOTIFS_STORE", Context.MODE_PRIVATE)
        val json = gson.toJson(lista)
        val editor = prefs.edit()
        if (idKey.isNotBlank()) {
            editor.putString("NOTIFS_$idKey", json)
        }
        editor.putString("NOTIFS_$nameKey", json)
        editor.apply()
    }

    fun obtenerCantidadNotificaciones(context: Context): Int {
        return obtenerNotificacionesSede(context).size
    }

    fun obtenerCantidadNoLeidas(context: Context): Int {
        val lista = obtenerNotificacionesSede(context)
        return lista.count { !it.leida }
    }

    fun marcarTodasComoLeidas(context: Context) {
        val lista = obtenerNotificacionesSede(context)
        if (lista.any { !it.leida }) {
            val actualizadas = lista.map { it.copy(leida = true) }
            guardarLista(context, actualizadas)
        }
    }
}
