package com.example.movilmanupuladora.data.repository

import com.example.movilmanupuladora.data.api.ApiService
import com.example.movilmanupuladora.data.model.AsistenciaDiaria
import com.example.movilmanupuladora.data.model.grados
import retrofit2.Response

class AsistenciaRepository(private val apiService: ApiService) {

    suspend fun obtenerAsistencias(): Response<List<AsistenciaDiaria>> {
        return apiService.obtenerAsistenciaDiaria()
    }

    suspend fun registrarAsistencia(asistencia: AsistenciaDiaria): Response<AsistenciaDiaria> {
        return apiService.registrarAsistenciaDiaria(asistencia)
    }

    suspend fun actualizarAsistencia(id: Int, asistencia: AsistenciaDiaria): Response<AsistenciaDiaria> {
        return apiService.actualizarAsistenciaDiaria(id, asistencia)
    }

    suspend fun eliminarAsistencia(id: Int): Response<Unit> {
        return apiService.eliminarAsistenciaDiaria(id)
    }

    suspend fun obtenerGrados(): Response<List<grados>> {
        return apiService.obtenerGrados()
    }
}
