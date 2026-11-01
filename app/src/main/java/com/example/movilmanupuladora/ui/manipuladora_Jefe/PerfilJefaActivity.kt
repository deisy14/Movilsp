package com.example.movilmanupuladora.ui.manipuladora_Jefe

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.movilmanupuladora.databinding.ActivityPerfilJefaBinding
import com.example.movilmanupuladora.databinding.DialogEditarPerfilBinding
import com.example.movilmanupuladora.ui.auth.LoginActivity
import com.example.movilmanupuladora.ui.manipuladora.EditarPerfilActivity
import com.example.movilmanupuladora.utils.SessionManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class PerfilJefaActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPerfilJefaBinding
    private lateinit var sessionManager: SessionManager

    private val selectorFotoPerfil =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            if (uri != null) {
                binding.imgFotoPerfil.setImageURI(uri)
                binding.imgFotoPerfil.setPadding(0, 0, 0, 0)
                Toast.makeText(this, "Foto de perfil actualizada correctamente", Toast.LENGTH_SHORT).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityPerfilJefaBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessionManager = SessionManager(this)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
            insets
        }

        cargarDatosPerfil()
        configurarNavegacionInferior()
        configurarEventos()
    }

    private fun cargarDatosPerfil() {
        val nombre = sessionManager.getUserName() ?: "Isa Pérez"
        val correo = sessionManager.getUserEmail() ?: "Isaperez@gmail.com"
        val rol = sessionManager.getUserRole() ?: "Jefa de Manipuladoras"
        val telefono = sessionManager.getUserPhone() ?: "No registrado"

        binding.txtNombreJefa.text = nombre
        binding.txtNombre.text = nombre
        binding.txtCorreo.text = correo
        binding.txtCargo.text = rol
        binding.txtTelefono.text = telefono
    }

    private fun configurarEventos() {
        binding.btnCambiarFotoPerfil.setOnClickListener {
            selectorFotoPerfil.launch("image/*")
        }

        binding.btnEditarPerfil.setOnClickListener {
            mostrarVentanaEmergenteEditarPerfil()
        }

        binding.btnConfiguracionNotificaciones.setOnClickListener {
            startActivity(Intent(this, HistorialNotificacionesJefaActivity::class.java))
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

    private fun mostrarVentanaEmergenteEditarPerfil() {
        try {
            val dialogBinding = DialogEditarPerfilBinding.inflate(layoutInflater)

            val nombreGuardado = sessionManager.getUserName() ?: "Isa Pérez"
            val correoGuardado = sessionManager.getUserEmail() ?: "Isaperez@gmail.com"
            val telefonoGuardado = sessionManager.getUserPhone() ?: "No registrado"

            dialogBinding.edtNombre.setText(nombreGuardado)
            dialogBinding.edtCorreo.setText(correoGuardado)
            dialogBinding.edtTelefono.setText(if (telefonoGuardado != "No registrado") telefonoGuardado else "")

            val dialog = MaterialAlertDialogBuilder(this)
                .setView(dialogBinding.root)
                .create()

            dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

            dialogBinding.btnGuardarCambios.setOnClickListener {
                val passNueva = dialogBinding.edtPasswordNueva.text.toString().trim()
                val passConfirmar = dialogBinding.edtPasswordConfirmar.text.toString().trim()
                val telefono = dialogBinding.edtTelefono.text.toString().trim()

                if (passNueva.isNotEmpty() || passConfirmar.isNotEmpty()) {
                    if (passNueva.length < 6) {
                        dialogBinding.edtPasswordNueva.error = "La contraseña debe tener al menos 6 caracteres"
                        dialogBinding.edtPasswordNueva.requestFocus()
                        return@setOnClickListener
                    }

                    if (passNueva != passConfirmar) {
                        dialogBinding.edtPasswordConfirmar.error = "Las contraseñas no coinciden"
                        dialogBinding.edtPasswordConfirmar.requestFocus()
                        return@setOnClickListener
                    }

                    val correoUsuario = sessionManager.getUserEmail() ?: correoGuardado
                    if (correoUsuario.isNotBlank()) {
                        val localAuth = getSharedPreferences("SIRAE_LOCAL_AUTH", MODE_PRIVATE)
                        localAuth.edit().putString("pwd_${correoUsuario.lowercase().trim()}", passNueva).apply()
                    }
                }

                if (telefono.isNotEmpty()) {
                    sessionManager.saveUserPhone(telefono)
                }

                Toast.makeText(
                    this,
                    "Contraseña y datos de perfil actualizados correctamente",
                    Toast.LENGTH_LONG
                ).show()

                cargarDatosPerfil()
                dialog.dismiss()
            }

            dialogBinding.btnCancelar.setOnClickListener {
                dialog.dismiss()
            }

            dialog.show()

            dialog.window?.setLayout(
                (resources.displayMetrics.widthPixels * 0.90).toInt(),
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT
            )
        } catch (e: Exception) {
            e.printStackTrace()
            startActivity(Intent(this, EditarPerfilActivity::class.java))
        }
    }

    private fun configurarNavegacionInferior() {
        com.example.movilmanupuladora.utils.NavigationHelperJefa.setupBarraNavegacionJefa(
            this,
            binding.barraNavegacionJefa,
            com.example.movilmanupuladora.utils.NavigationHelperJefa.TabJefa.PERFIL
        )
    }
}