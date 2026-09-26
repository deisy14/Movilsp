package com.example.movilmanupuladora.ui.manipuladora

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.movilmanupuladora.databinding.ActivityBarraNavegacionBinding

class BarraNavegacionActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBarraNavegacionBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityBarraNavegacionBinding.inflate(layoutInflater)
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
        // INICIO
        // ==========================================

        binding.navInicio.setOnClickListener {
            startActivity(
                Intent(this, MainActivity::class.java)
            )
            finish()
        }

        // ==========================================
        // ASIGNADAS
        // ==========================================

        binding.navAsignadas.setOnClickListener {
            startActivity(
                Intent(this, AsignadasActivity::class.java)
            )
            finish()
        }

        // ==========================================
        // INVENTARIO
        // ==========================================

        binding.navInventario.setOnClickListener {
            startActivity(
                Intent(this, InventarioActivity::class.java)
            )
            finish()
        }

        // ==========================================
        // AVISOS
        // ==========================================

        binding.navAvisos.setOnClickListener {
            startActivity(
                Intent(this, AvisosActivity::class.java)
            )
            finish()
        }

        // ==========================================
        // PERFIL
        // ==========================================

        binding.navPerfil.setOnClickListener {
            startActivity(
                Intent(this, PerfilActivity::class.java)
            )
            finish()
        }
    }
}

