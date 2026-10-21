package com.example.movilmanupuladora.ui.auth

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.movilmanupuladora.R
import com.example.movilmanupuladora.data.api.RetrofitClient
import com.example.movilmanupuladora.data.repository.UsuarioRepository
import com.example.movilmanupuladora.databinding.ActivityRecuperarPasswordBinding
import kotlinx.coroutines.launch
import org.json.JSONObject

class RecuperarPasswordActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRecuperarPasswordBinding
    private val usuarioRepository = UsuarioRepository(RetrofitClient.apiService)

    private var correoSolicitado: String = ""
    private var nuevaPasswordVisible = false
    private var confirmarPasswordVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRecuperarPasswordBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Pre-cargar correo si se pasó desde el Login
        intent.getStringExtra(EXTRA_CORREO)?.let { correo ->
            if (correo.isNotEmpty()) {
                binding.txtCorreoRecuperar.setText(correo)
            }
        }

        configurarBotones()
        configurarTogglesPassword()
    }

    private fun configurarBotones() {
        binding.btnRegresar.setOnClickListener {
            finish()
        }

        binding.btnEnviarCodigo.setOnClickListener {
            solicitarCodigo()
        }

        binding.btnReenviarCodigo.setOnClickListener {
            solicitarCodigo()
        }

        binding.btnRestablecer.setOnClickListener {
            confirmarCambioPassword()
        }
    }

    private fun configurarTogglesPassword() {
        binding.btnToggleNuevaPassword.setOnClickListener {
            nuevaPasswordVisible = !nuevaPasswordVisible
            if (nuevaPasswordVisible) {
                binding.txtNuevaPassword.transformationMethod =
                    HideReturnsTransformationMethod.getInstance()
                binding.btnToggleNuevaPassword.setImageResource(R.drawable.ic_visibility_white)
            } else {
                binding.txtNuevaPassword.transformationMethod =
                    PasswordTransformationMethod.getInstance()
                binding.btnToggleNuevaPassword.setImageResource(R.drawable.ic_visibility_off_white)
            }
            binding.txtNuevaPassword.setSelection(binding.txtNuevaPassword.text.length)
        }

        binding.btnToggleConfirmarPassword.setOnClickListener {
            confirmarPasswordVisible = !confirmarPasswordVisible
            if (confirmarPasswordVisible) {
                binding.txtConfirmarPassword.transformationMethod =
                    HideReturnsTransformationMethod.getInstance()
                binding.btnToggleConfirmarPassword.setImageResource(R.drawable.ic_visibility_white)
            } else {
                binding.txtConfirmarPassword.transformationMethod =
                    PasswordTransformationMethod.getInstance()
                binding.btnToggleConfirmarPassword.setImageResource(R.drawable.ic_visibility_off_white)
            }
            binding.txtConfirmarPassword.setSelection(binding.txtConfirmarPassword.text.length)
        }
    }

    // =========================================================
    // PASO 1: SOLICITAR CÓDIGO AL SERVIDOR
    // =========================================================
    private fun solicitarCodigo() {
        val correo = binding.txtCorreoRecuperar.text.toString().trim()

        if (correo.isEmpty()) {
            binding.txtCorreoRecuperar.error = "Ingresa tu correo electrónico"
            binding.txtCorreoRecuperar.requestFocus()
            return
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(correo).matches()) {
            binding.txtCorreoRecuperar.error = "Ingresa un correo electrónico válido"
            binding.txtCorreoRecuperar.requestFocus()
            return
        }

        mostrarCargandoEnvio(true)

        lifecycleScope.launch {
            try {
                val response = usuarioRepository.solicitarCodigoRecuperacion(correo)

                mostrarCargandoEnvio(false)

                if (response.isSuccessful) {
                    val cuerpo = response.body()
                    val mensaje = cuerpo?.mensaje
                        ?: "Si el correo está registrado, recibirás un código de verificación."

                    correoSolicitado = correo
                    binding.txtCorreoEnviado.text = correo
                    binding.layoutPaso2.visibility = View.VISIBLE

                    Toast.makeText(this@RecuperarPasswordActivity, mensaje, Toast.LENGTH_LONG).show()

                    // Scroll suave hacia abajo para mostrar el paso 2
                    binding.scrollRecuperar.post {
                        binding.scrollRecuperar.smoothScrollTo(0, binding.layoutPaso2.top)
                    }

                    binding.txtCodigoVerificacion.requestFocus()

                } else {
                    val errorBody = response.errorBody()?.string()
                    val mensajeError = extraerMensajeError(errorBody, response.code())

                    AlertDialog.Builder(this@RecuperarPasswordActivity)
                        .setTitle("No se pudo enviar el código")
                        .setMessage(mensajeError)
                        .setPositiveButton("Entendido", null)
                        .show()
                }

            } catch (e: Exception) {
                mostrarCargandoEnvio(false)
                Toast.makeText(
                    this@RecuperarPasswordActivity,
                    "Error de conexión: ${e.localizedMessage ?: "No se pudo conectar al servidor"}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    // =========================================================
    // PASO 2: VALIDAR CÓDIGO Y ESTABLECER NUEVA CONTRASEÑA
    // =========================================================
    private fun confirmarCambioPassword() {
        val correo = if (correoSolicitado.isNotEmpty()) correoSolicitado else binding.txtCorreoRecuperar.text.toString().trim()
        val codigo = binding.txtCodigoVerificacion.text.toString().trim()
        val nuevaPassword = binding.txtNuevaPassword.text.toString()
        val confirmarPassword = binding.txtConfirmarPassword.text.toString()

        if (correo.isEmpty()) {
            Toast.makeText(this, "El correo no puede estar vacío", Toast.LENGTH_SHORT).show()
            return
        }

        if (codigo.length != 6 || !codigo.all { it.isDigit() }) {
            binding.txtCodigoVerificacion.error = "El código debe tener exactamente 6 dígitos"
            binding.txtCodigoVerificacion.requestFocus()
            return
        }

        if (nuevaPassword.length < 6) {
            binding.txtNuevaPassword.error = "La nueva contraseña debe tener al menos 6 caracteres"
            binding.txtNuevaPassword.requestFocus()
            return
        }

        if (nuevaPassword != confirmarPassword) {
            binding.txtConfirmarPassword.error = "Las contraseñas no coinciden"
            binding.txtConfirmarPassword.requestFocus()
            return
        }

        mostrarCargandoRestablecer(true)

        lifecycleScope.launch {
            try {
                val response = usuarioRepository.confirmarCodigoRecuperacion(
                    correo = correo,
                    codigo = codigo,
                    nuevaPassword = nuevaPassword,
                    confirmarPassword = confirmarPassword
                )

                mostrarCargandoRestablecer(false)

                if (response.isSuccessful) {
                    val cuerpo = response.body()
                    val mensaje = cuerpo?.mensaje ?: "¡Tu contraseña ha sido restablecida exitosamente!"

                    AlertDialog.Builder(this@RecuperarPasswordActivity)
                        .setTitle("¡Éxito!")
                        .setMessage("$mensaje\n\nYa puedes iniciar sesión con tu nueva contraseña.")
                        .setCancelable(false)
                        .setPositiveButton("Iniciar sesión") { _, _ ->
                            val resultIntent = Intent().apply {
                                putExtra(EXTRA_CORREO, correo)
                            }
                            setResult(Activity.RESULT_OK, resultIntent)
                            finish()
                        }
                        .show()

                } else {
                    val errorBody = response.errorBody()?.string()
                    val mensajeError = extraerMensajeError(errorBody, response.code())

                    AlertDialog.Builder(this@RecuperarPasswordActivity)
                        .setTitle("Error al restablecer contraseña")
                        .setMessage(mensajeError)
                        .setPositiveButton("Corregir", null)
                        .show()
                }

            } catch (e: Exception) {
                mostrarCargandoRestablecer(false)
                Toast.makeText(
                    this@RecuperarPasswordActivity,
                    "Error de conexión: ${e.localizedMessage ?: "No se pudo conectar al servidor"}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun mostrarCargandoEnvio(cargando: Boolean) {
        binding.btnEnviarCodigo.isEnabled = !cargando
        binding.btnReenviarCodigo.isEnabled = !cargando
        binding.txtCorreoRecuperar.isEnabled = !cargando

        if (cargando) {
            binding.btnEnviarCodigo.text = "Enviando código..."
        } else {
            binding.btnEnviarCodigo.text = "Enviar código al correo"
        }
    }

    private fun mostrarCargandoRestablecer(cargando: Boolean) {
        binding.btnRestablecer.isEnabled = !cargando
        binding.txtCodigoVerificacion.isEnabled = !cargando
        binding.txtNuevaPassword.isEnabled = !cargando
        binding.txtConfirmarPassword.isEnabled = !cargando

        if (cargando) {
            binding.btnRestablecer.text = "Restableciendo contraseña..."
        } else {
            binding.btnRestablecer.text = "Restablecer contraseña"
        }
    }

    private fun extraerMensajeError(errorJson: String?, statusCode: Int): String {
        if (errorJson.isNullOrEmpty()) {
            return "Error $statusCode al procesar la solicitud."
        }
        return try {
            val json = JSONObject(errorJson)
            when {
                json.has("error") -> json.getString("error")
                json.has("detail") -> json.getString("detail")
                json.has("mensaje") -> json.getString("mensaje")
                else -> "Error $statusCode: $errorJson"
            }
        } catch (e: Exception) {
            if (statusCode == 503) {
                "El servicio de correo del servidor no está disponible en este momento. Por favor contacta al administrador del PAE."
            } else {
                "Error $statusCode: No se pudo procesar la solicitud."
            }
        }
    }

    companion object {
        const val EXTRA_CORREO = "EXTRA_CORREO"
    }
}
