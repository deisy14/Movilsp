package com.example.movilmanupuladora.data.repository

import com.example.movilmanupuladora.data.api.ApiService
import com.example.movilmanupuladora.data.model.gramaje
import retrofit2.Response

class gramajeRepository(private val apiService: ApiService) {
    suspend fun obtenerGramajes(): Response<List<gramaje>> {
        return apiService.obtenerGramajes()
    }

    suspend fun crearGramaje(gramaje: gramaje): Response<gramaje> {
        return apiService.crearGramaje(gramaje)
    }
}