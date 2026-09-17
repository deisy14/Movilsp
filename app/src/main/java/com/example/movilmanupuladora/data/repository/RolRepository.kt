package com.example.movilmanipuladora.data.repository

import com.example.movilmanipuladora.data.api.ApiService
import com.example.movilmanupuladora.data.model.Rol
import retrofit2.Response

class RolRepository(private val apiService: ApiService) {

    suspend fun obtenerRoles(): Response<List<Rol>> {
        return apiService.obtenerRoles()
    }
}