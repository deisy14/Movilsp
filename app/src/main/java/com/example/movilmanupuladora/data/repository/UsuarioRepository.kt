package com.example.movilmanupuladora.data.repository

import com.example.movilmanupuladora.data.api.ApiService
import com.example.movilmanupuladora.data.model.LoginRequest
import com.example.movilmanupuladora.data.model.LoginResponse
import com.example.movilmanupuladora.data.model.RegistroResponse
import com.example.movilmanupuladora.data.model.Usuario
import retrofit2.Response

class UsuarioRepository(private val apiService: ApiService) {

    suspend fun login(correo: String, password: String): Response<LoginResponse> {
        // Usamos el campo 'correo' de nuevo
        val request = LoginRequest(correo = correo, password = password)
        return apiService.login(request)
    }

    suspend fun registrarUsuario(usuario: Usuario): Response<RegistroResponse> {
        return apiService.registrarUsuario(usuario)
    }

    suspend fun solicitarCodigoRecuperacion(correo: String): Response<com.example.movilmanupuladora.data.model.MensajeResponse> {
        val request = com.example.movilmanupuladora.data.model.SolicitarRecuperacionRequest(correo = correo)
        return apiService.solicitarCodigoRecuperacion(request)
    }

    suspend fun confirmarCodigoRecuperacion(
        correo: String,
        codigo: String,
        nuevaPassword: String,
        confirmarPassword: String
    ): Response<com.example.movilmanupuladora.data.model.MensajeResponse> {
        val request = com.example.movilmanupuladora.data.model.ConfirmarRecuperacionRequest(
            correo = correo,
            codigo = codigo,
            nueva_password = nuevaPassword,
            confirmar_password = confirmarPassword
        )
        return apiService.confirmarCodigoRecuperacion(request)
    }
}
