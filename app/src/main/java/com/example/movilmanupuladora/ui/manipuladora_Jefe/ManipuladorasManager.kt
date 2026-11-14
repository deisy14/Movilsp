package com.example.movilmanupuladora.ui.manipuladora_Jefe

import android.content.Context
import android.util.Log
import com.example.movilmanupuladora.data.api.RetrofitClient
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
        var idUsuario: Int = -1
    )

    private val listaManipuladoras = mutableListOf(
        Manipuladora(20, "Andrea Sánchez", "Preparación de alimentos", "Mañana", true, "Activa", "andreasanchez@gmail.com", 20),
        Manipuladora(21, "Pool Gómez", "Preparación y distribución", "Mañana", true, "Activa", "poolgomez27@gmail.com", 21),
        Manipuladora(24, "Deisy", "Apoyo en cocina", "Mañana", true, "Activa", "deis@gmail.com", 24),
        Manipuladora(30, "Andreas Muñoz", "Preparación y control", "Mañana", true, "Activa", "andres@gmail.com", 30)
    )

    private var cargadoDesdeBackend = false

    fun obtenerManipuladoras(): List<Manipuladora> = listaManipuladoras.toList()

    /**
     * Carga usuarios reales desde el backend que tengan estrictamente el rol de Manipuladora.
     * Excluye sin excepción a Administradores, Jefas de Manipuladoras, Supervisores o roles de gestión.
     */
    suspend fun cargarDesdeBackend() {
        if (cargadoDesdeBackend) return
        try {
            val response = withContext(Dispatchers.IO) {
                RetrofitClient.apiService.obtenerUsuarios()
            }
            if (response.isSuccessful && !response.body().isNullOrEmpty()) {
                val todosUsuarios = response.body()!!
                // Filtrar ESTRICTAMENTE usuarios con rol Manipuladora (excluyendo administradores y jefas)
                val manipuladorasBackend = todosUsuarios.filter { usuario ->
                    val rolNombre = usuario.nombreRol?.lowercase()?.trim() ?: ""
                    val rolId = usuario.rol
                    val correo = usuario.correo.lowercase().trim()
                    val nombre = usuario.nombreCompleto.lowercase().trim()

                    // Exclusiones categóricas: Administradores, Jefes/Jefas, Supervisores
                    val esExcluido = rolNombre.contains("admin") ||
                            rolNombre.contains("jef") ||
                            rolNombre.contains("super") ||
                            rolNombre.contains("coordinad") ||
                            rolId == 1 || rolId == 2 || rolId == 4 ||
                            rolId == 10 || rolId == 26 || rolId == 27 || rolId == 28 || rolId == 29 ||
                            correo.contains("admin") || correo.contains("jefe") || correo.contains("jefa") ||
                            correo.contains("supervisor") ||
                            nombre.contains("admin") || nombre.contains("jefe") || nombre.contains("jefa") ||
                            nombre.contains("supervisor")

                    // Solo manipuladoras válidas por rol o función (sin correos quemados)
                    val esManipuladora = !esExcluido && (
                            rolId == 5 || rolId == 24 || rolId == 3 ||
                            rolNombre.contains("manipula") ||
                            rolNombre.contains("cocina") ||
                            rolNombre.contains("operaria")
                    )

                    esManipuladora
                }

                if (manipuladorasBackend.isNotEmpty()) {
                    // Actualizar IDs reales del backend sobre las manipuladoras base
                    for (u in manipuladorasBackend) {
                        val c = u.correo.lowercase().trim()
                        val n = u.nombreCompleto.lowercase().trim()
                        val existente = listaManipuladoras.firstOrNull {
                            it.correo.equals(c, ignoreCase = true) ||
                            (c.isNotBlank() && it.correo.contains(c, ignoreCase = true)) ||
                            (n.isNotBlank() && it.nombre.lowercase().contains(n))
                        }
                        if (existente != null) {
                            if (u.idUsuario != null && u.idUsuario > 0) {
                                existente.idUsuario = u.idUsuario
                            }
                            if (u.nombreCompleto.isNotBlank()) {
                                existente.nombre = u.nombreCompleto
                            }
                            if (u.isActive != null) {
                                existente.activa = u.isActive
                                existente.estado = if (u.isActive) "Activa" else "Inactiva"
                            }
                        } else {
                            val nuevoId = u.idUsuario ?: ((listaManipuladoras.maxOfOrNull { it.id } ?: 50) + 1)
                            listaManipuladoras.add(
                                Manipuladora(
                                    id = nuevoId,
                                    nombre = u.nombreCompleto,
                                    funcion = "Manipuladora de alimentos",
                                    jornada = "Mañana",
                                    activa = u.isActive ?: true,
                                    estado = if (u.isActive == true) "Activa" else "Inactiva",
                                    correo = u.correo,
                                    idUsuario = u.idUsuario ?: nuevoId
                                )
                            )
                        }
                    }
                    cargadoDesdeBackend = true
                    Log.d("ManipuladorasManager", "Cargadas ${listaManipuladoras.size} manipuladoras estrictas")
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
        listaManipuladoras.find { it.id == id }?.let {
            it.activa = nuevaActiva
            it.estado = nuevoEstado
        }
    }

    fun eliminarManipuladora(id: Int) {
        listaManipuladoras.removeAll { it.id == id }
    }

    /**
     * Busca una manipuladora por su correo electrónico (para mapear con el login)
     */
    fun buscarPorCorreo(correo: String): Manipuladora? {
        return listaManipuladoras.firstOrNull {
            it.correo.equals(correo, ignoreCase = true)
        }
    }

    /**
     * Busca una manipuladora por su nombre (parcial, ignore case)
     */
    fun buscarPorNombre(nombre: String): Manipuladora? {
        return listaManipuladoras.firstOrNull {
            it.nombre.equals(nombre, ignoreCase = true) ||
            it.nombre.contains(nombre, ignoreCase = true) ||
            nombre.contains(it.nombre, ignoreCase = true)
        }
    }
}
