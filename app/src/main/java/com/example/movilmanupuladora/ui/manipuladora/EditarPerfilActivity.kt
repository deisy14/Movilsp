package com.example.movilmanupuladora.ui.manipuladora

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.movilmanupuladora.databinding.ActivityEditarPerfilBinding

class EditarPerfilActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEditarPerfilBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        binding = ActivityEditarPerfilBinding.inflate(layoutInflater)
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
        // GUARDAR CAMBIOS
        // ==========================================

        binding.btnGuardarCambios.setOnClickListener {

            val nombre =
                binding.edtNombre.text.toString().trim()

            val telefono =
                binding.edtTelefono.text.toString().trim()

            val correo =
                binding.edtCorreo.text.toString().trim()

            if (nombre.isEmpty()) {

                binding.edtNombre.error =
                    "Ingresa tu nombre"

                binding.edtNombre.requestFocus()

                return@setOnClickListener
            }

            if (correo.isEmpty()) {

                binding.edtCorreo.error =
                    "Ingresa tu correo"

                binding.edtCorreo.requestFocus()

                return@setOnClickListener
            }

            Toast.makeText(
                this,
                "Perfil actualizado correctamente",
                Toast.LENGTH_SHORT
            ).show()

            finish()
        }

        // ==========================================
        // CANCELAR
        // ==========================================

        binding.btnCancelar.setOnClickListener {
            finish()
        }

        // ==========================================
        // BARRA DE NAVEGACIÓN
        // ==========================================

        binding.barraNavegacion.navInicio.setOnClickListener {

            startActivity(
                Intent(this, MainActivity::class.java)
            )

            finish()
        }

        binding.barraNavegacion.navAsignadas.setOnClickListener {

            startActivity(
                Intent(this, ComponentesActivity::class.java)
            )

            finish()
        }

        binding.barraNavegacion.navInventario.setOnClickListener {

            startActivity(
                Intent(this, InventarioActivity::class.java)
            )

            finish()
        }

        binding.barraNavegacion.navAvisos.setOnClickListener {

            startActivity(
                Intent(this, AvisosActivity::class.java)
            )

            finish()
        }

        binding.barraNavegacion.navPerfil.setOnClickListener {

            startActivity(
                Intent(this, PerfilActivity::class.java)
            )

            finish()
        }
    }
}