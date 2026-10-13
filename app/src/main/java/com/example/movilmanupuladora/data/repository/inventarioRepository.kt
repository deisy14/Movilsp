package com.example.movilmanupuladora.data.repository

import com.example.movilmanupuladora.data.api.ApiService
import com.example.movilmanupuladora.data.model.Ingrediente
import com.example.movilmanupuladora.data.model.inventario
import retrofit2.Response

import com.example.movilmanupuladora.data.model.CategoriaInventario
import com.example.movilmanupuladora.data.model.UnidadMedida

class InventarioRepository(private val apiService: ApiService) {
    suspend fun obtenerInventario(): Response<List<inventario>> {
        return apiService.obtenerInventario()
    }

    suspend fun crearInventario(inventarioData: inventario): Response<inventario> {
        return apiService.crearInventario(inventarioData)
    }

    suspend fun obtenerIngredientes(): Response<List<Ingrediente>> {
        return apiService.obtenerIngredientes()
    }

    suspend fun obtenerCategoriasInventario(): Response<List<CategoriaInventario>> {
        return apiService.obtenerCategoriasInventario()
    }

    suspend fun obtenerUnidadesMedida(): Response<List<UnidadMedida>> {
        return apiService.obtenerUnidadesMedida()
    }

    suspend fun actualizarInventario(id: Int, inventarioData: inventario): Response<inventario> {
        return apiService.actualizarInventario(id, inventarioData)
    }

    suspend fun registrarEntrada(payload: Map<String, Any>): Response<Map<String, Any>> {
        return apiService.registrarEntradaInventario(payload)
    }

    suspend fun registrarSalida(payload: Map<String, Any>): Response<Map<String, Any>> {
        return apiService.registrarSalidaInventario(payload)
    }
}