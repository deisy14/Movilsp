package com.example.movilmanupuladora

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.movilmanupuladora.R
import com.example.movilmanupuladora.data.api.RetrofitClient
import com.example.movilmanupuladora.databinding.ActivityMainBinding
import com.example.movilmanupuladora.utils.SessionManager

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Inicializar sesión y restaurar token para Retrofit
        sessionManager = SessionManager(this)
        RetrofitClient.authToken = sessionManager.fetchAuthToken()

        // Inflar las vistas usando ViewBinding
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Ejemplos de acceso a elementos del layout usando el objeto binding:
        // binding.wheelContainer.setOnClickListener { }
        // binding.tvPlatoSeleccionado.text = "Seleccionado"
    }
}