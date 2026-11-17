package com.example.movilmanupuladora.ui.manipuladora_Jefe

import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.movilmanupuladora.data.api.RetrofitClient
import com.example.movilmanupuladora.databinding.ActivityPerfilJefaBinding
import com.example.movilmanupuladora.ui.auth.LoginActivity
import com.example.movilmanupuladora.utils.NavigationHelperJefa
import com.example.movilmanupuladora.utils.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody

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
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
            insets
        }

        cargarDatosPerfilDinamicos()
        configurarNavegacionInferior()
        configurarEventos()
    }

    private fun cargarDatosPerfilDinamicos() {
        val nombre = sessionManager.getUserName() ?: ""
        val correo = sessionManager.getUserEmail() ?: ""
        val rol = sessionManager.getUserRole() ?: ""
        val telefono = sessionManager.getUserPhone() ?: ""

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
            mostrarDialogoEnviarNotificacion()
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