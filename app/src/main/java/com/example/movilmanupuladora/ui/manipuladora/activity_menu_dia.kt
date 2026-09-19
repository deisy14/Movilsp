package com.example.movilmanupuladora.ui.manipuladora

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.movilmanupuladora.databinding.ActivityMenuDiaBinding

class activity_menu_dia : AppCompatActivity() {

    private lateinit var binding: ActivityMenuDiaBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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

        // Botón "Ver componentes" -> Navega a activity_componentes
        binding.btnVerComponentes.setOnClickListener {
            val intent = Intent(this, activity_componentes::class.java)
            startActivity(intent)
        }
    }
}