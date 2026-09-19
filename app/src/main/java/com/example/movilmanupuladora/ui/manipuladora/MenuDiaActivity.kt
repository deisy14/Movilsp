package com.example.movilmanupuladora.ui.manipuladora

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.movilmanupuladora.data.api.RetrofitClient
import com.example.movilmanupuladora.databinding.ActivityMenuDiaBinding
import com.example.movilmanupuladora.utils.SessionManager

class MenuDiaActivity : AppCompatActivity() {

    private lateinit var sessionManager: SessionManager
    private lateinit var binding: ActivityMenuDiaBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Restaurar token
        sessionManager = SessionManager(this)
        RetrofitClient.authToken = sessionManager.fetchAuthToken()

        enableEdgeToEdge()

        // Inicialización con ViewBinding
        binding = ActivityMenuDiaBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Ajustar la pantalla a las barras del sistema
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(
                systemBars.left,
                systemBars.top,
                systemBars.right,
                systemBars.bottom
            )
            insets
        }

        // Botón "Ver componentes" -> Navega a ComponentesActivity
        binding.btnVerComponentes.setOnClickListener {
            val intent = Intent(this, ComponentesActivity::class.java)
            startActivity(intent)
        }
    }
}