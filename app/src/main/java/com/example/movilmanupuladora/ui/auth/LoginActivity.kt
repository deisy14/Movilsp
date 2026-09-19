package com.example.movilmanupuladora.ui.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.movilmanupuladora.MainActivity
import com.example.movilmanupuladora.data.api.RetrofitClient
import com.example.movilmanupuladora.data.repository.UsuarioRepository
import com.example.movilmanupuladora.databinding.ActivityLoginBinding
import com.example.movilmanupuladora.utils.SessionManager
import kotlinx.coroutines.launch
import org.json.JSONObject

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private val usuarioRepository = UsuarioRepository(RetrofitClient.apiService)
    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessionManager = SessionManager(this)

        sessionManager.fetchAuthToken()?.let { savedToken ->
            RetrofitClient.authToken = savedToken
        }

        binding.btnIngresar.setOnClickListener {
            val correo = binding.txtCorreo.text.toString().trim()
            val password = binding.txtPassword.text.toString().trim()

            if (correo.isNotEmpty() && password.isNotEmpty()) {
                iniciarSesion(correo, password)
            } else {
                Toast.makeText(this, "Ingresa tus datos", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun iniciarSesion(correo: String, pass: String) {
        lifecycleScope.launch {
            try {
                val response = usuarioRepository.login(correo, pass)

                if (response.isSuccessful && response.body() != null) {
                    val loginRes = response.body()!!
                    val token = loginRes.authToken

                    if (token != null) {
                        RetrofitClient.authToken = token
                        sessionManager.saveAuthToken(token)

                        loginRes.usuario?.let {
                            sessionManager.saveUserData(it.nombre, it.rol)
                        }

                        Toast.makeText(this@LoginActivity, "¡Bienvenido ${loginRes.usuario?.nombre ?: ""}!", Toast.LENGTH_SHORT).show()
                        startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                        finish()
                    } else {
                        Toast.makeText(this@LoginActivity, "Error: Token no recibido", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    // Respaldo de prueba local
                    if (correo == "manipuladora@gmail.com" && pass == "123456789") {
                        Toast.makeText(this@LoginActivity, "Inicio de sesión (Modo prueba)", Toast.LENGTH_SHORT).show()
                        startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                        finish()
                        return@launch
                    }

                    val errorBody = response.errorBody()?.string()
                    val msg = try { JSONObject(errorBody ?: "").optString("detail", "Error de credenciales") }
                    catch (e: Exception) { "Error ${response.code()}" }
                    Toast.makeText(this@LoginActivity, msg, Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                if (correo == "manipuladora@gmail.com" && pass == "123456789") {
                    Toast.makeText(this@LoginActivity, "Inicio de sesión (Modo offline)", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                    finish()
                    return@launch
                }
                Toast.makeText(this@LoginActivity, "Error de red: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }
}