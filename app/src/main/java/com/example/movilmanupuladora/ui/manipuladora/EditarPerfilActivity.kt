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

    private val sessionManager by lazy { com.example.movilmanupuladora.utils.SessionManager(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        binding = ActivityEditarPerfilBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // ==========================================
        // INSETS
        // ==========================================

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // ==========================================
        // CARGAR DATOS INSTITUCIONALES (SOLO LECTURA)
        // ==========================================

        val nombreGuardado = sessionManager.getUserName() ?: "Manipuladora PAE"
        val correoGuardado = sessionManager.getUserEmail() ?: "manipuladora@sirae.gov.co"
        val telefonoGuardado = sessionManager.getUserPhone() ?: "310 450 8920"
        val cargoGuardado = sessionManager.getUserRole() ?: "Manipuladora de alimentos institucional"

        binding.edtNombre.setText(nombreGuardado)
        binding.edtCorreo.setText(correoGuardado)
        binding.edtTelefono.setText(telefonoGuardado)

        // Iniciales para el avatar
        val iniciales = nombreGuardado.split(" ")
            .filter { it.isNotEmpty() }
            .take(2)
            .joinToString("") { it.first().uppercase() }
            .ifEmpty { "MP" }
        binding.txtAvatar.text = iniciales

        // ==========================================
        // ACTUALIZAR CONTRASEÑA Y CONTACTO
        // ==========================================

        binding.btnGuardarCambios.setOnClickListener {
            val passNueva = binding.edtPasswordNueva.text.toString().trim()
            val passConfirmar = binding.edtPasswordConfirmar.text.toString().trim()
            val telefono = binding.edtTelefono.text.toString().trim()

            // Si ingresó contraseña nueva, validar coincidencia y longitud
            if (passNueva.isNotEmpty() || passConfirmar.isNotEmpty()) {
                if (passNueva.length < 6) {
                    binding.edtPasswordNueva.error = "La contraseña debe tener al menos 6 caracteres"
                    binding.edtPasswordNueva.requestFocus()
                    return@setOnClickListener
                }

                if (passNueva != passConfirmar) {
                    binding.edtPasswordConfirmar.error = "Las contraseñas no coinciden"
                    binding.edtPasswordConfirmar.requestFocus()
                    return@setOnClickListener
                }
            }

            if (telefono.isNotEmpty()) {
                sessionManager.saveUserPhone(telefono)
            }

            Toast.makeText(
                this,
                "✓ Contraseña y datos de contacto actualizados correctamente",
                Toast.LENGTH_LONG
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