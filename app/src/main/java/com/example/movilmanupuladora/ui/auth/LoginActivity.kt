package com.example.movilmanupuladora.ui.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.content.Context
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.movilmanupuladora.data.api.RetrofitClient
import com.example.movilmanupuladora.data.repository.UsuarioRepository
import com.example.movilmanupuladora.databinding.ActivityLoginBinding
import com.example.movilmanupuladora.ui.manipuladora.TurnoActivity
import com.example.movilmanupuladora.ui.manipuladora_Jefe.InicioJefaActivity
import com.example.movilmanupuladora.utils.SessionManager
import kotlinx.coroutines.launch
import org.json.JSONObject

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding

    private val usuarioRepository =
        UsuarioRepository(RetrofitClient.apiService)

    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessionManager = SessionManager(this)

        // =====================================================
        // RECUPERAR TOKEN GUARDADO
        // =====================================================

        sessionManager.fetchAuthToken()?.let { savedToken ->
            RetrofitClient.authToken = savedToken
        }

        // =====================================================
        // VISIBILIDAD DE CONTRASEÑA
        // =====================================================

        var passwordVisible = false
        binding.btnTogglePassword.setOnClickListener {
            passwordVisible = !passwordVisible
            if (passwordVisible) {
                binding.txtPassword.transformationMethod =
                    android.text.method.HideReturnsTransformationMethod.getInstance()
                binding.btnTogglePassword.setImageResource(com.example.movilmanupuladora.R.drawable.ic_visibility)
            } else {
                binding.txtPassword.transformationMethod =
                    android.text.method.PasswordTransformationMethod.getInstance()
                binding.btnTogglePassword.setImageResource(com.example.movilmanupuladora.R.drawable.ic_visibility_off)
            }
            binding.txtPassword.setSelection(binding.txtPassword.text.length)
        }

        // =====================================================
        // BOTÓN INGRESAR
        // =====================================================

        binding.btnIngresar.setOnClickListener {

            val correo = binding.txtCorreo.text
                .toString()
                .trim()

            val password = binding.txtPassword.text
                .toString()
                .trim()

            if (correo.isEmpty()) {
                binding.txtCorreo.requestFocus()
                binding.txtCorreo.error = "Ingresa tu correo"
                return@setOnClickListener
            }

            if (password.isEmpty()) {
                binding.txtPassword.requestFocus()
                binding.txtPassword.error = "Ingresa tu contraseña"
                return@setOnClickListener
            }

            ocultarTeclado()

            iniciarSesion(correo, password)
        }

        // =====================================================
        // OLVIDÉ CONTRASEÑA Y CONTACTAR ADMIN
        // =====================================================

        binding.txtOlvide.setOnClickListener {
            androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Recuperar contraseña")
                .setMessage("Para restablecer tu contraseña, por favor solicita la actualización a tu Administrador o comunícate con la coordinación del PAE.")
                .setPositiveButton("Aceptar", null)
                .show()
        }

        binding.txtAdministrador.setOnClickListener {
            androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Contactar Administrador")
                .setMessage("Si tienes inconvenientes para ingresar a la aplicación móvil, comunícate con el Administrador institucional del sistema SIRAE.")
                .setPositiveButton("Aceptar", null)
                .show()
        }
    }

    // =========================================================
    // LOGIN
    // =========================================================

    private fun iniciarSesion(
        correo: String,
        pass: String
    ) {

        mostrarCargando(true)

        lifecycleScope.launch {

            try {

                val response = usuarioRepository.login(correo, pass)

                if (response.isSuccessful && response.body() != null) {

                    val loginRes = response.body()!!

                    val token = loginRes.authToken

                    if (token != null) {

                        // =====================================
                        // GUARDAR TOKEN
                        // =====================================

                        RetrofitClient.authToken = token

                        sessionManager.saveAuthToken(token)

                        // =====================================
                        // GUARDAR DATOS DEL USUARIO
                        // =====================================

                        loginRes.usuario?.let { usuario ->

                            sessionManager.saveUserData(
                                name = usuario.nombre,
                                role = usuario.rol,
                                email = usuario.correo,
                                idUsuario = usuario.idUsuario
                            )
                        }

                        // =====================================
                        // MENSAJE
                        // =====================================

                        Toast.makeText(
                            this@LoginActivity,
                            "¡Bienvenido ${loginRes.usuario?.nombre ?: ""}!",
                            Toast.LENGTH_SHORT
                        ).show()

                        // =====================================
                        // DETERMINAR ROL Y REDIRIGIR
                        // =====================================

                        val rol = (loginRes.usuario?.rol ?: "").lowercase()
                        val email = (loginRes.usuario?.correo ?: correo).lowercase()

                        // 1. Si es Administrador, bloquear porque ese rol es exclusivo de la Web
                        if (rol.contains("admin")) {
                            mostrarCargando(false)
                            Toast.makeText(
                                this@LoginActivity,
                                "Correo no encontrado",
                                Toast.LENGTH_LONG
                            ).show()
                            return@launch
                        }

                        // 2. Si es Jefa de Manipuladoras, redirigir a InicioJefaActivity
                        val esJefa = rol.contains("jefa") || rol.contains("jefe") ||
                                email.contains("jefe") || email.contains("jefa") ||
                                email.contains("isaperez") || email.contains("carlos")

                        // 3. Determinar destino según el rol
                        val destino = when {
                            esJefa -> InicioJefaActivity::class.java
                            rol.contains("manipuladora") -> TurnoActivity::class.java
                            else -> TurnoActivity::class.java
                        }

                        startActivity(
                            Intent(
                                this@LoginActivity,
                                destino
                            )
                        )

                        finish()

                    } else {

                        mostrarCargando(false)

                        Toast.makeText(
                            this@LoginActivity,
                            "Error: Token no recibido",
                            Toast.LENGTH_LONG
                        ).show()
                    }

                } else {

                    // =========================================
                    // ERROR DEL BACKEND
                    // =========================================

                    mostrarCargando(false)

                    val errorBody = response.errorBody()?.string()

                    val msg = if (response.code() == 401) {
                        "Correo o contraseña incorrectos"
                    } else {
                        try {
                            val json = JSONObject(errorBody ?: "")
                            json.optString("detail", json.optString("error", "Error al iniciar sesión"))
                        } catch (e: Exception) {
                            "Error ${response.code()}: No se pudo iniciar sesión"
                        }
                    }

                    Toast.makeText(
                        this@LoginActivity,
                        msg,
                        Toast.LENGTH_LONG
                    ).show()
                }

            } catch (e: Exception) {
                mostrarCargando(false)

                Toast.makeText(
                    this@LoginActivity,
                    "Error de red: ${e.localizedMessage ?: "No se pudo conectar al servidor"}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    // =========================================================
    // ESTADO DEL BOTÓN
    // =========================================================

    private fun mostrarCargando(cargando: Boolean) {

        binding.btnIngresar.isEnabled = !cargando

        if (cargando) {

            binding.btnIngresar.text = "Ingresando..."

            binding.txtCorreo.isEnabled = false
            binding.txtPassword.isEnabled = false
            binding.txtOlvide.isEnabled = false
            binding.txtAdministrador.isEnabled = false

        } else {

            binding.btnIngresar.text = "Ingresar"

            binding.txtCorreo.isEnabled = true
            binding.txtPassword.isEnabled = true
            binding.txtOlvide.isEnabled = true
            binding.txtAdministrador.isEnabled = true
        }
    }

    // =========================================================
    // OCULTAR TECLADO
    // =========================================================

    private fun ocultarTeclado() {

        val imm =
            getSystemService(Context.INPUT_METHOD_SERVICE)
                    as InputMethodManager

        imm.hideSoftInputFromWindow(
            binding.root.windowToken,
            0
        )
    }
}