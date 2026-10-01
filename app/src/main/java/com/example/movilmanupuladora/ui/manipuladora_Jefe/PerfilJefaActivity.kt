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
import com.example.movilmanupuladora.ui.auth.LoginActivity
import com.example.movilmanupuladora.utils.SessionManager

class PerfilJefaActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPerfilJefaBinding
    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityPerfilJefaBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessionManager = SessionManager(this)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        cargarDatosPerfil()
        configurarNavegacionInferior()
        configurarEventos()
    }

    private fun cargarDatosPerfil() {
        val nombre = sessionManager.getUserName() ?: "Jefa de Cocina"
        val correo = sessionManager.getUserEmail() ?: "jefa@sirae.com"
        val rol = sessionManager.getUserRole() ?: "Jefa de manipuladora"
        val telefono = sessionManager.getUserPhone() ?: "No registrado"

        binding.txtNombreJefa.text = nombre
        binding.txtNombre.text = nombre
        binding.txtCorreo.text = correo
        binding.txtCargo.text = rol
        binding.txtTelefono.text = telefono
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
                    sessionManager.clearSession()
                    val intent = Intent(this, LoginActivity::class.java)
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    startActivity(intent)
                    finish()
                }
                .setNegativeButton("Cancelar", null)
                .show()
        }
    }
}