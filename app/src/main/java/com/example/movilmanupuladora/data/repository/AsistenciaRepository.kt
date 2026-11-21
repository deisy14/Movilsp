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

    suspend fun guardarAsistenciaLimpia(
        idExistente: Int?,
        idGrado: Int,
        fecha: String,
        ninosPresentes: Int,
        idUsuario: Int
    ): Response<AsistenciaDiaria> {
        val payload = mapOf(
            "id_grado" to idGrado,
            "fecha" to fecha,
            "ninos_presentes" to ninosPresentes,
            "id_usuario_manipuladora" to idUsuario
        )
        return if (idExistente != null && idExistente > 0) {
            val resPatch = try { apiService.patchAsistenciaDiaria(idExistente, payload) } catch (_: Exception) { null }
            if (resPatch != null && resPatch.isSuccessful) {
                resPatch
            } else {
                apiService.actualizarAsistenciaDiaria(idExistente, AsistenciaDiaria(
                    idAsistencia = idExistente,
                    idGrado = idGrado,
                    fecha = fecha,
                    ninosPresentes = ninosPresentes,
                    idUsuarioManipuladora = idUsuario
                ))
            }
        } else {
            val resPost = try { apiService.registrarAsistenciaDiariaMap(payload) } catch (_: Exception) { null }
            if (resPost != null && resPost.isSuccessful) {
                resPost
            } else {
                apiService.registrarAsistenciaDiaria(AsistenciaDiaria(
                    idGrado = idGrado,
                    fecha = fecha,
                    ninosPresentes = ninosPresentes,
                    idUsuarioManipuladora = idUsuario
                ))
            }
        }
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
