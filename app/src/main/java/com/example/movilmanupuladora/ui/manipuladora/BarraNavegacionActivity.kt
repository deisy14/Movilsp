package com.example.movilmanupuladora.ui.manipuladora

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.movilmanupuladora.databinding.ActivityBarraNavegacionBinding
import com.example.movilmanupuladora.MainActivity

class BarraNavegacionActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBarraNavegacionBinding


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Inicializar Binding
        binding = ActivityBarraNavegacionBinding.inflate(layoutInflater)

        // Mostrar el XML
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

            val intent = Intent(
                this,
                MainActivity::class.java
            )

            startActivity(intent)
        }


        // ==========================================
        // ASIGNADAS
        // ==========================================

        binding.navAsignadas.setOnClickListener {

            val intent = Intent(
                this,
                BarraNavegacionActivity::class.java
            )

            startActivity(intent)
        }


        // ==========================================
        // INVENTARIO
        // ==========================================

        binding.navInventario.setOnClickListener {

            val intent = Intent(
                this,
                InventarioActivity::class.java
            )

            startActivity(intent)
        }


        // ==========================================
        // AVISOS
        // ==========================================

        binding.navAvisos.setOnClickListener {

            val intent = Intent(
                this,
                AvisosActivity::class.java
            )

            startActivity(intent)
        }


        // ==========================================
        // PERFIL
        // ==========================================

        binding.navPerfil.setOnClickListener {

            val intent = Intent(
                this,
                PerfilActivity::class.java
            )

            startActivity(intent)
        }
    }
}