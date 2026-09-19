package com.example.movilmanupuladora.data.repository

import com.example.movilmanupuladora.data.api.ApiService
import com.example.movilmanupuladora.data.model.Ingrediente
import com.example.movilmanupuladora.data.model.inventario
import retrofit2.Response

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
}