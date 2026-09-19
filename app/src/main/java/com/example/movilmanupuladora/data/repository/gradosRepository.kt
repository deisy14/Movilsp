package com.example.movilmanupuladora.data.repository

import com.example.movilmanupuladora.data.api.ApiService
import com.example.movilmanupuladora.data.model.grados

import com.example.movilmanupuladora.data.model.gramaje
import retrofit2.Response

class GradoRepository(private val apiService: ApiService) {
    suspend fun obtenerGrados(): Response<List<grados>> {
        return apiService.obtenerGrados()
    }
}