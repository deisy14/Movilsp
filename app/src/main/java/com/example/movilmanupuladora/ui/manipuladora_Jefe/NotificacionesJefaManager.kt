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
        var leida: Boolean = false
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

        return try {
            val type = object : TypeToken<List<NotificacionItem>>() {}.type
            val lista: List<NotificacionItem> = gson.fromJson(json, type) ?: emptyList()
            lista.sortedByDescending { it.timestamp }
        } catch (_: Exception) {
            emptyList()
        }
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
                    val cat = when {
                        nb.tipo.equals("inventario", ignoreCase = true) || tit.contains("inventario", ignoreCase = true) -> "Inventario"
                        nb.tipo.equals("preparacion", ignoreCase = true) || tit.contains("prep", ignoreCase = true) -> "Preparaciones"
                        nb.tipo.equals("asistencia", ignoreCase = true) || tit.contains("asist", ignoreCase = true) -> "Asistencia"
                        else -> "Equipo"
                    }

                    val yaExiste = listaActual.any { it.titulo == tit && it.mensaje == msg }
                    if (!yaExiste) {
                        listaActual.add(
                            NotificacionItem(
                                id = "backend_${nb.idNotificacion ?: System.currentTimeMillis()}",
                                titulo = tit,
                                mensaje = msg,
                                fecha = fechaFormateada,
                                categoria = cat,
                                timestamp = System.currentTimeMillis() - 86400000L // Notificaciones anteriores
                            )
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
                        val tit = "📦 Nueva entrega despachada: $rem"
                        val nomSup = items.firstOrNull()?.observaciones?.let { obs ->
                            val match = Regex("Responsable:\\s*([^|;\\n]+)", RegexOption.IGNORE_CASE).find(obs)
                            match?.groupValues?.get(1)?.trim()
                        } ?: "Supervisor PAE"
                        val msg = "El $nomSup ha despachado la remisión $rem con ${items.size} alimentos para tu sede. Toca aquí para revisar cantidades, reportar faltantes y firmar el acta."

                        val yaExiste = listaActual.any { it.titulo == tit }
                        if (!yaExiste) {
                            listaActual.add(
                                0,
                                NotificacionItem(
                                    id = "entrega_$rem",
                                    titulo = tit,
                                    mensaje = msg,
                                    fecha = "En tránsito",
                                    categoria = "Inventario",
                                    timestamp = System.currentTimeMillis()
                                )
                            )
                        }
                    }
                }
            } catch (_: Exception) {}

            guardarLista(context, listaActual)
        } catch (_: Exception) { }
    }

    private fun formatearFechaBackend(fechaRaw: String?): String {
        if (fechaRaw.isNullOrBlank()) return "Reciente"
        if (fechaRaw.contains(",") || fechaRaw.contains("·")) return fechaRaw
        return try {
            val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
            val fecha = parser.parse(fechaRaw)
            if (fecha != null) {
                SimpleDateFormat("dd MMM, hh:mm a", Locale("es", "CO")).format(fecha)
            } else {
                fechaRaw
            }
        } catch (_: Exception) {
            try {
                val parserSimple = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val fecha = parserSimple.parse(fechaRaw)
                if (fecha != null) {
                    SimpleDateFormat("dd MMM yyyy", Locale("es", "CO")).format(fecha)
                } else {
                    fechaRaw
                }
            } catch (_: Exception) {
                fechaRaw
            }
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
