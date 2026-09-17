package com.example.movilmanipuladora.data.repository

import com.example.movilmanipuladora.data.api.ApiService
import com.example.movilmanipuladora.data.model.LoginRequest
import com.example.movilmanipuladora.data.model.LoginResponse
import com.example.movilmanipuladora.data.model.RegistroResponse
import com.example.movilmanipuladora.data.model.Usuario
import retrofit2.Response

class UsuarioRepository(private val apiService: ApiService) {

    suspend fun login(correo: String, password: String): Response<LoginResponse> {
        val request = LoginRequest(correo = correo, password = password)
        return apiService.login(request)
    }

    suspend fun registrarUsuario(usuario: Usuario): Response<RegistroResponse> {
        return apiService.registrarUsuario(usuario)
    }
}