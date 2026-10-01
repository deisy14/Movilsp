package com.example.movilmanupuladora.ui.manipuladora_Jefe

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.movilmanupuladora.databinding.ActivityPerfilJefaBinding

class PerfilJefaActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPerfilJefaBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityPerfilJefaBinding.inflate(layoutInflater)
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
        binding.btnEditarPerfil.setOnClickListener {
            Toast.makeText(this, "Editar perfil próximamente", Toast.LENGTH_SHORT).show()
        }

        binding.btnConfiguracionNotificaciones.setOnClickListener {
            startActivity(Intent(this, PopupNotificacionesActivity::class.java))
        }

        binding.btnCerrarSesion.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Cerrar sesión")
                .setMessage("¿Estás segura de que deseas cerrar sesión?")
                .setPositiveButton("Sí") { _, _ ->
                    val intent = Intent(this, InicioJefaActivity::class.java)
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                    startActivity(intent)
                    finish()
                }
                .setNegativeButton("Cancelar", null)
                .show()
        }
    }
}