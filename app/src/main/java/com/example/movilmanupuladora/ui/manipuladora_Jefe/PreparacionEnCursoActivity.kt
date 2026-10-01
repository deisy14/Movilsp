package com.example.movilmanupuladora.ui.manipuladora_Jefe

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.movilmanupuladora.databinding.ActivityPreparacionEnCursoBinding


class PreparacionEnCursoActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPreparacionEnCursoBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityPreparacionEnCursoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        configurarEventos()
    }

    private fun configurarEventos() {
        binding.btnCerrar.setOnClickListener {
            finish()
        }

        binding.btnFinalizarPreparacion.setOnClickListener {
            Toast.makeText(this, "Preparación finalizada exitosamente", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, PreparacionCompletadaActivity::class.java))
            finish()
        }

        binding.btnPausarPreparacion.setOnClickListener {
            Toast.makeText(this, "Preparación pausada", Toast.LENGTH_SHORT).show()
        }
    }
}