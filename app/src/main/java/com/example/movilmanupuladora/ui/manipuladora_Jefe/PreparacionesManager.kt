package com.example.movilmanupuladora.ui.manipuladora_Jefe

object PreparacionesManager {

    data class Preparacion(
        val id: Int,
        val menu: String,
        val jornada: String,
        val ninos: Int,
        var estado: String, // "Pendiente", "En preparación", "Completada"
        var horaInicio: String = "",
        var horaFin: String = "",
        var observaciones: String = ""
    )

    private val preparaciones = mutableListOf(
        Preparacion(1, "Pollo guisado", "Mañana", 120, "Completada", "08:00 a. m.", "09:00 a. m.", "Sin novedad"),
        Preparacion(2, "Arroz con pollo", "Mañana", 120, "En preparación", "10:00 a. m.", "", "Fuego medio"),
        Preparacion(3, "Lentejas con arroz", "Tarde", 110, "Pendiente"),
        Preparacion(4, "Pasta con pollo", "Tarde", 110, "Pendiente")
    )

    fun obtenerPreparaciones(): List<Preparacion> = preparaciones.toList()

    fun actualizarEstado(id: Int, nuevoEstado: String, horaInicio: String = "", observaciones: String = "") {
        preparaciones.find { it.id == id }?.let {
            it.estado = nuevoEstado
            if (horaInicio.isNotEmpty()) it.horaInicio = horaInicio
            if (observaciones.isNotEmpty()) it.observaciones = observaciones
            if (nuevoEstado == "Completada") {
                it.horaFin = java.text.SimpleDateFormat("hh:mm a", java.util.Locale("es", "CO")).format(java.util.Date())
            }
        }
    }

    fun contarTotales(): Int = preparaciones.size
    fun contarCompletadas(): Int = preparaciones.count { it.estado == "Completada" }
    fun contarEnCurso(): Int = preparaciones.count { it.estado == "En preparación" }
    fun contarPendientes(): Int = preparaciones.count { it.estado == "Pendiente" }
}
