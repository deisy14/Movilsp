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
        return apiService.obtenerPlatos()
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
}
