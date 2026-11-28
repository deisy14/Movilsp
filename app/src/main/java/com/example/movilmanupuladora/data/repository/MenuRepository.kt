package com.example.movilmanupuladora.data.repository

import com.example.movilmanupuladora.data.api.ApiService
import com.example.movilmanupuladora.data.model.DetallePlato
import com.example.movilmanupuladora.data.model.PlatoResponse
import com.example.movilmanupuladora.data.model.SeccionMenu
import retrofit2.Response

class MenuRepository(private val apiService: ApiService) {

    // Secciones de Menú (ej: Desayuno, Almuerzo, Refrigerio)
    suspend fun obtenerSeccionesMenu(): Response<List<SeccionMenu>> {
        return apiService.obtenerSeccionesMenu()
    }

    suspend fun obtenerSeccionMenuPorId(id: Int): Response<SeccionMenu> {
        return apiService.obtenerSeccionMenuPorId(id)
    }

    // Platos (con su componente: Proteína, Principio, Acompañamiento, etc.)
    suspend fun obtenerPlatos(): Response<List<PlatoResponse>> {
        val res = apiService.obtenerPlatosRaw()
        if (res.isSuccessful && res.body() != null) {
            val root = res.body()!!
            val arr = when {
                root.isJsonArray -> root.asJsonArray
                root.isJsonObject && root.asJsonObject.has("results") -> root.asJsonObject.getAsJsonArray("results")
                root.isJsonObject && root.asJsonObject.has("data") -> root.asJsonObject.getAsJsonArray("data")
                else -> null
            }
            val type = object : com.google.gson.reflect.TypeToken<List<PlatoResponse>>() {}.type
            val lista: List<PlatoResponse> = if (arr != null) {
                com.google.gson.Gson().fromJson(arr, type) ?: emptyList()
            } else emptyList()
            return Response.success(lista, res.raw())
        }
        return Response.error(res.code(), res.errorBody() ?: okhttp3.ResponseBody.create(null, ""))
    }

    suspend fun obtenerPlatoPorId(id: Int): Response<PlatoResponse> {
        return apiService.obtenerPlatoPorId(id)
    }

    // Detalle de Platos (porciones, cantidades totales y estado)
    suspend fun obtenerDetallePlatos(): Response<List<DetallePlato>> {
        return apiService.obtenerDetallePlatos()
    }

    suspend fun obtenerDetallePlatoPorId(id: Int): Response<DetallePlato> {
        return apiService.obtenerDetallePlatoPorId(id)
    }

    suspend fun obtenerMenus(): Response<List<com.example.movilmanupuladora.data.model.menus>> {
        return apiService.obtenerMenus()
    }
}
