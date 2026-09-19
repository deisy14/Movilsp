package com.example.movilmanupuladora.ui.auth

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.movilmanupuladora.data.api.RetrofitClient
import com.example.movilmanupuladora.data.model.Rol
import com.example.movilmanupuladora.data.repository.RolRepository
import com.example.movilmanupuladora.data.repository.UsuarioRepository

class RegistroActivity : AppCompatActivity() {

    private val rolRepository = RolRepository(RetrofitClient.apiService)
    private val usuarioRepository = UsuarioRepository(RetrofitClient.apiService)

    private var listaRoles: List<Rol> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Toast.makeText(this, "RegistroActivity", Toast.LENGTH_SHORT).show()
    }
}
