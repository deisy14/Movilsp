package com.example.movilmanupuladora.ui.manipuladora

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.movilmanupuladora.databinding.ActivityAvisosBinding

class AvisosActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAvisosBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // ==========================================
        // VIEW BINDING
        // ==========================================

        binding = ActivityAvisosBinding.inflate(layoutInflater)
        setContentView(binding.root)


        // ==========================================
        // INSETS
        // ==========================================

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->

            val systemBars =
                insets.getInsets(WindowInsetsCompat.Type.systemBars())

            v.setPadding(
                systemBars.left,
                systemBars.top,
                systemBars.right,
                systemBars.bottom
            )

            insets
        }


        // ==========================================
        // BARRA DE NAVEGACIÓN
        // ==========================================

        // INICIO
        binding.barraNavegacion.navInicio.setOnClickListener {

            startActivity(
                Intent(this, MainActivity::class.java)
            )

            finish()
        }


        // ASIGNADAS
        binding.barraNavegacion.navAsignadas.setOnClickListener {

            startActivity(
                Intent(this, AsignadasActivity::class.java)
            )

            finish()
        }


        // INVENTARIO
        binding.barraNavegacion.navInventario.setOnClickListener {

            startActivity(
                Intent(this, InventarioActivity::class.java)
            )

            finish()
        }


        // AVISOS
        binding.barraNavegacion.navAvisos.setOnClickListener {
            // Ya estamos en Avisos.
        }


        // PERFIL
        binding.barraNavegacion.navPerfil.setOnClickListener {

            startActivity(
                Intent(this, PerfilActivity::class.java)
            )

            finish()
        }
    }
}

