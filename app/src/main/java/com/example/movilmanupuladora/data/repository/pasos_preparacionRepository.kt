package com.example.movilmanupuladora.data.repository

import com.example.movilmanupuladora.data.api.ApiService
import com.example.movilmanupuladora.data.api.RetrofitClient.apiService
import com.example.movilmanupuladora.data.model.pasos_preparacion
import retrofit2.Response

data class pasos_preparacionRepository( private val apiService: ApiService)

suspend fun obtenerPasosPreparacion(): Response<List<pasos_preparacion>>{
    return apiService.obtenerPasosPreparacion()
}



