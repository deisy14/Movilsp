package com.example.movilmanipuladora.ui.auth
/*
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.movilmanipuladora.data.api.RetrofitClient
import com.example.movilmanipuladora.data.repository.UsuarioRepository
import com.example.movilmanipuladora.databinding.ActivityLoginBinding
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private val usuarioRepository = UsuarioRepository(RetrofitClient.apiService)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnLogin.setOnClickListener {
            iniciarSesion()
        }

        binding.btnIrARegistro?.setOnClickListener {
            startActivity(Intent(this, RegistroActivity::class.java))
        }
    }

    private fun iniciarSesion() {
        val correo = binding.etCorreo.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()

        if (correo.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Ingresa tu correo y contraseña", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            try {
                val response = usuarioRepository.login(correo, password)

                if (response.isSuccessful && response.body() != null) {
                    val loginResponse = response.body()!!
                    val usuario = loginResponse.usuario

                    Toast.makeText(
                        this@LoginActivity,
                        "¡Bienvenido ${usuario.nombre}! Rol: ${usuario.rol ?: "Sin rol"}",
                        Toast.LENGTH_LONG
                    ).show()

                    // NAVEGAR A LA PANTALLA PRINCIPAL
                    // val intent = Intent(this@LoginActivity, MainActivity::class.java)
                    // startActivity(intent)
                    // finish()

                } else {
                    Toast.makeText(
                        this@LoginActivity,
                        "Credenciales incorrectas",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                Toast.makeText(
                    this@LoginActivity,
                    "Error de conexión: ${e.localizedMessage}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
}
*/