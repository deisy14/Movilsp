package com.example.psirae

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.psirae.databinding.ActivityAvisosBinding

class avisos_Activity : AppCompatActivity() {

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

        // ------------------------------------------
        // INICIO
        // ------------------------------------------

        binding.barraNavegacion.navInicio.setOnClickListener {

            startActivity(
                Intent(this, MainActivity::class.java)
            )

            finish()
        }


        // ------------------------------------------
        // ASIGNADAS
        // ------------------------------------------

        binding.barraNavegacion.navAsignadas.setOnClickListener {

            startActivity(
                Intent(this, activity_componentes::class.java)
            )

            finish()
        }


        // ------------------------------------------
        // INVENTARIO
        // ------------------------------------------

        binding.barraNavegacion.navInventario.setOnClickListener {

            startActivity(
                Intent(this, inventario_activity::class.java)
            )

            finish()
        }


        // ------------------------------------------
        // AVISOS
        // ------------------------------------------

        binding.barraNavegacion.navAvisos.setOnClickListener {

            // Ya estamos en Avisos.
            // No hacemos nada para evitar abrir
            // nuevamente la misma pantalla.
        }


        // ------------------------------------------
        // PERFIL
        // ------------------------------------------

        binding.barraNavegacion.navPerfil.setOnClickListener {

            startActivity(
                Intent(this, activity_perfil::class.java)
            )

            finish()
        }
    }
}
