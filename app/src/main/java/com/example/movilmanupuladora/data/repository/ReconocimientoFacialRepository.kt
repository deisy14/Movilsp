package com.example.movilmanupuladora.data.repository

import com.example.movilmanupuladora.data.api.ApiService
import com.example.movilmanupuladora.data.model.EstadoBiometriaResponse
import com.example.movilmanupuladora.data.model.RegisterFaceRequest
import com.example.movilmanupuladora.data.model.RegisterFaceResponse
import com.example.movilmanupuladora.data.model.VerifyFaceRequest
import com.example.movilmanupuladora.data.model.VerifyFaceResponse
import com.google.gson.JsonObject
import retrofit2.Response

class ReconocimientoFacialRepository(private val apiService: ApiService) {

    suspend fun verificarRostro(base64Image: String): Response<VerifyFaceResponse> {
        val request = VerifyFaceRequest(image = base64Image)
        return apiService.verificarRostroFacial(request)
    }

    suspend fun registrarRostro(base64Image: String, correo: String? = null): Response<RegisterFaceResponse> {
        val request = RegisterFaceRequest(image = base64Image, correo = correo)
        return apiService.registrarRostroFacial(request)
    }

    suspend fun consultarEstadoBiometria(): Response<EstadoBiometriaResponse> {
        return apiService.consultarEstadoBiometria()
    }

    suspend fun eliminarBiometria(): Response<JsonObject> {
        return apiService.eliminarBiometriaFacial()
    }
}
