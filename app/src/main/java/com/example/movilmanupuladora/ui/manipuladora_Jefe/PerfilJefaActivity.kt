package com.example.movilmanupuladora.ui.manipuladora_Jefe

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.movilmanupuladora.data.api.RetrofitClient
import com.example.movilmanupuladora.databinding.ActivityPerfilJefaBinding
import com.example.movilmanupuladora.databinding.DialogEditarPerfilBinding
import com.example.movilmanupuladora.ui.auth.LoginActivity
import com.example.movilmanupuladora.ui.manipuladora.EditarPerfilActivity
import com.example.movilmanupuladora.utils.NavigationHelperJefa
import com.example.movilmanupuladora.utils.SedeContratoManager
import com.example.movilmanupuladora.utils.SessionManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody

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
        val nombre = sessionManager.getUserName() ?: "Jefa de Cocina"
        val correo = sessionManager.getUserEmail() ?: ""
        val rol = sessionManager.getUserRole() ?: "Jefa de Manipuladoras"
        val ubicacion = sessionManager.getUbicacionCompleta()
        val telefono = sessionManager.getUserPhone() ?: "No registrado"

        binding.txtNombreJefa.text = nombre
        binding.txtNombre.text = nombre
        binding.txtCorreo.text = correo
        binding.txtCargo.text = "$rol · $ubicacion"
        binding.txtInstitucionSede.text = ubicacion
        binding.txtTelefono.text = telefono
        binding.txtContratoAsignado.text = SedeContratoManager.obtenerNumeroContrato(this) ?: "Sin contrato asignado"

        lifecycleScope.launch {
            try {
                SedeContratoManager.sincronizarSeccionesContrato(this@PerfilJefaActivity, sessionManager.getSedeId())
                binding.txtContratoAsignado.text =
                    SedeContratoManager.obtenerNumeroContrato(this@PerfilJefaActivity) ?: "Sin contrato asignado"
            } catch (_: Exception) { }
        }
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

        binding.btnBiometriaFacialCard.setOnClickListener {
            val intent = Intent(this, com.example.movilmanupuladora.ui.auth.ReconocimientoFacialActivity::class.java).apply {
                putExtra(
                    com.example.movilmanupuladora.ui.auth.ReconocimientoFacialActivity.EXTRA_MODO,
                    com.example.movilmanupuladora.ui.auth.ReconocimientoFacialActivity.MODO_REGISTRO
                )
                putExtra(
                    com.example.movilmanupuladora.ui.auth.ReconocimientoFacialActivity.EXTRA_CORREO,
                    sessionManager.getUserEmail()
                )
            }
            startActivity(intent)
        }

        binding.btnCerrarSesion.setOnClickListener {
            val dialogBinding = com.example.movilmanupuladora.databinding.DialogConfirmarAsignacionBinding.inflate(layoutInflater)
            dialogBinding.txtTituloConfirmacion.text = "Cerrar sesión"
            dialogBinding.txtMensajeConfirmacionAsignacion.text = "¿Estás segura de que deseas cerrar sesión?"
            dialogBinding.btnConfirmarGuardarAsignacion.text = "Sí, cerrar sesión"
            dialogBinding.btnSeguirEditandoAsignacion.text = "Cancelar"

            val dialog = MaterialAlertDialogBuilder(this)
                .setView(dialogBinding.root)
                .create()

            dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

            dialogBinding.btnConfirmarGuardarAsignacion.setOnClickListener {
                dialog.dismiss()
                sessionManager.clearSession()
                val intent = Intent(this, LoginActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                startActivity(intent)
                finish()
            }

            dialogBinding.btnSeguirEditandoAsignacion.setOnClickListener {
                dialog.dismiss()
            }

            dialog.show()
            dialog.window?.setLayout(
                (resources.displayMetrics.widthPixels * 0.90).toInt(),
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }
    }

    private fun mostrarVentanaEmergenteEditarPerfil() {
        try {
            val dialogBinding = DialogEditarPerfilBinding.inflate(layoutInflater)

            val nombreGuardado = sessionManager.getUserName() ?: "Jefa de Cocina"
            val correoGuardado = sessionManager.getUserEmail() ?: ""
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

    private fun mostrarDialogoEnviarNotificacion() {
        val builder = AlertDialog.Builder(this)
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 40, 50, 10)
        }

        val inputTitulo = EditText(this).apply { hint = "Título del mensaje" }
        val inputMensaje = EditText(this).apply { hint = "Escribe tu mensaje para el supervisor..." }

        layout.addView(inputTitulo)
        layout.addView(inputMensaje)

        builder.setView(layout)
            .setTitle("Enviar Notificación al Supervisor")
            .setPositiveButton("Enviar") { _, _ ->
                val tituloStr = inputTitulo.text.toString().trim()
                val mensajeStr = inputMensaje.text.toString().trim()

                if (tituloStr.isNotEmpty() && mensajeStr.isNotEmpty()) {
                    enviarMensajeAlServidor(tituloStr, mensajeStr)
                } else {
                    Toast.makeText(this, "Por favor completa todos los campos", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun enviarMensajeAlServidor(titulo: String, mensaje: String) {
        val tituloBody = titulo.toRequestBody("text/plain".toMediaTypeOrNull())
        val mensajeBody = mensaje.toRequestBody("text/plain".toMediaTypeOrNull())

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val response = RetrofitClient.apiService.enviarNotificacion(tituloBody, mensajeBody, null)

                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        Toast.makeText(this@PerfilJefaActivity, "¡Notificación enviada con éxito!", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this@PerfilJefaActivity, "Error al enviar la notificación", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@PerfilJefaActivity, "Error de red: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun configurarNavegacionInferior() {
        NavigationHelperJefa.setupBarraNavegacionJefa(
            this,
            binding.barraNavegacionJefa,
            NavigationHelperJefa.TabJefa.PERFIL
        )
    }
}