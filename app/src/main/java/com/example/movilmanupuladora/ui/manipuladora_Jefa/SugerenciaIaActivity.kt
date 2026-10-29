package com.example.movilmanupuladora.ui.manipuladora_Jefa

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.movilmanupuladora.databinding.ActivityAiRecetaBinding

class SugerenciaIaActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAiRecetaBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityAiRecetaBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        configurarEventos()
    }

    private fun configurarEventos() {
        binding.btnVolver.setOnClickListener {
            finish()
        }

        binding.cardSugerencia1.setOnClickListener {
            Toast.makeText(
                this,
                "🤖 IA: Sugerencia 'Pollo Guisado Criollo' seleccionada para el menú PAE.",
                Toast.LENGTH_LONG
            ).show()
            finish()
        }

        binding.cardSugerencia2.setOnClickListener {
            Toast.makeText(
                this,
                "🤖 IA: Sugerencia 'Huevos Pericos con Arepa' seleccionada para el menú PAE.",
                Toast.LENGTH_LONG
            ).show()
            finish()
        }

        binding.btnIrARedactar.setOnClickListener {
            val intent = Intent(this, RedactarRecetaActivity::class.java)
            startActivity(intent)
        }
    }
}
