package com.example.movilmanupuladora.ui.manipuladora_Jefe

import com.example.movilmanupuladora.R

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.movilmanupuladora.databinding.ActivityPreparacionesJefaBinding

class PreparacionesJefaActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPreparacionesJefaBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityPreparacionesJefaBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        configurarNavegacionInferior()
        configurarEventos()
    }

    private fun configurarEventos() {
        // Example: card click to open detail or start preparation
        try {
            binding.root.findViewById<android.view.View>(R.id.btnVer1)?.setOnClickListener {
                startActivity(Intent(this, DetallePreparacionActivity::class.java))
            }
        } catch (e: Exception) {
            // fallback
        }
    }
}