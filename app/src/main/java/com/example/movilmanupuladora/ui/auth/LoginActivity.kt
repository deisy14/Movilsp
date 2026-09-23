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
                                usuario.nombre,
                                usuario.rol
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
                        // IR AL MAIN
                        // =====================================

                        startActivity(
                            Intent(
                                this@LoginActivity,
                                TurnoActivity::class.java
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
                    // MODO DE PRUEBA LOCAL
                    // =========================================

                    if (
                        correo == "manipuladora@gmail.com" &&
                        pass == "123456789"
                    ) {

                        Toast.makeText(
                            this@LoginActivity,
                            "Inicio de sesión (Modo prueba)",
                            Toast.LENGTH_SHORT
                        ).show()

                        startActivity(
                            Intent(
                                this@LoginActivity,
                                TurnoActivity::class.java
                            )
                        )

                        finish()

                        return@launch
                    }

                    // =========================================
                    // ERROR DEL BACKEND
                    // =========================================

                    mostrarCargando(false)

                    val errorBody =
                        response.errorBody()?.string()

                    val msg = try {

                        JSONObject(
                            errorBody ?: ""
                        ).optString(
                            "detail",
                            "Error de credenciales"
                        )

                    } catch (e: Exception) {

                        "Error ${response.code()}"
                    }

                    Toast.makeText(
                        this@LoginActivity,
                        msg,
                        Toast.LENGTH_LONG
                    ).show()
                }

            } catch (e: Exception) {

                // =============================================
                // MODO OFFLINE DE PRUEBA
                // =============================================

                if (
                    correo == "manipuladora@gmail.com" &&
                    pass == "123456789"
                ) {

                    Toast.makeText(
                        this@LoginActivity,
                        "Inicio de sesión (Modo offline)",
                        Toast.LENGTH_SHORT
                    ).show()

                    startActivity(
                        Intent(
                            this@LoginActivity,
                            TurnoActivity::class.java
                        )
                    )

                    finish()

                    return@launch
                }

                // =============================================
                // ERROR DE RED
                // =============================================

                mostrarCargando(false)

                Toast.makeText(
                    this@LoginActivity,
                    "Error de red: ${e.localizedMessage}",
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