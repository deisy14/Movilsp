package com.example.manipuladorajefe

object ManipuladorasManager {

    data class Manipuladora(
        val id: Int,
        var nombre: String,
        var funcion: String,
        var jornada: String,
        var activa: Boolean,
        var estado: String = "Activa"
    )

    private val listaManipuladoras = mutableListOf(
        Manipuladora(1, "María González", "Preparación de alimentos", "Mañana", true, "Activa"),
        Manipuladora(2, "Ana Rodríguez", "Preparación y distribución", "Tarde", true, "Activa"),
        Manipuladora(3, "Laura Martínez", "Apoyo en cocina", "Mañana y tarde", true, "Activa")
    )

    fun obtenerManipuladoras(): List<Manipuladora> = listaManipuladoras.toList()

    fun agregarManipuladora(nombre: String, funcion: String, jornada: String, activa: Boolean = true) {
        val nuevoId = (listaManipuladoras.maxOfOrNull { it.id } ?: 0) + 1
        listaManipuladoras.add(Manipuladora(nuevoId, nombre, funcion, jornada, activa, if (activa) "Activa" else "Inactiva"))
    }

    fun actualizarEstado(id: Int, nuevaActiva: Boolean, nuevoEstado: String) {
        listaManipuladoras.find { it.id == id }?.let {
            it.activa = nuevaActiva
            it.estado = nuevoEstado
        }
    }

    fun eliminarManipuladora(id: Int) {
        listaManipuladoras.removeAll { it.id == id }
    }
}
