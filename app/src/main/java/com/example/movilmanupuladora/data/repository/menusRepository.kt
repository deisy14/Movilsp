package com.example.movilmanupuladora.data.repository

import com.example.movilmanupuladora.data.api.ApiService
import com.example.movilmanupuladora.data.model.menus
import retrofit2.Response

data class menusRepository(private val apiService: ApiService){

    suspend fun obtenerMenus(): Response<List<menus>>{
        return apiService.obtenerMenus()
    }

}
