package com.example.movilmanupuladora.ui.manipuladora

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.movilmanupuladora.R
import com.example.movilmanupuladora.MainActivity
import androidx.lifecycle.lifecycleScope
import com.example.movilmanupuladora.data.api.RetrofitClient
import com.example.movilmanupuladora.data.model.LoginRequest
import kotlinx.coroutines.launch

class activity_login : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setContentView(R.layout.activity_login)

        // ==========================================
        // REFERENCIAS
        // ==========================================

        val correo = findViewById<EditText>(R.id.txtCorreo)
        val password = findViewById<EditText>(R.id.txtPassword)

        val btnIngresar = findViewById<TextView>(R.id.btnIngresar)
        val txtOlvide = findViewById<TextView>(R.id.txtOlvide)
        val txtAdministrador =
            findViewById<TextView>(R.id.txtAdministrador)


        // ==========================================
        // BARRAS DEL SISTEMA
        // ==========================================

        ViewCompat.setOnApplyWindowInsetsListener(
            findViewById(R.id.cardLogin)
        ) { v, insets ->

            val systemBars =
                insets.getInsets(
                    WindowInsetsCompat.Type.systemBars()
                )

            v.setPadding(
                25,
                25 + systemBars.top,
                25,
                25 + systemBars.bottom
            )

            insets
        }


        // ==========================================
        // DATOS DE PRUEBA
        // ==========================================

        val correoCorrecto = "maria@sirae.com"
        val passwordCorrecta = "123456"


        // ==========================================
        // BOTÓN INGRESAR
        // ==========================================

        btnIngresar.setOnClickListener {

            val correoIngresado =
                correo.text.toString().trim()

            val passwordIngresada =
                password.text.toString().trim()


            // ------------------------------------------
            // VALIDAR CORREO
            // ------------------------------------------

            if (correoIngresado.isEmpty()) {

                correo.error = "Ingresa tu correo"
                correo.requestFocus()

                return@setOnClickListener
            }


            // ------------------------------------------
            // VALIDAR FORMATO
            // ------------------------------------------

            if (!Patterns.EMAIL_ADDRESS
                    .matcher(correoIngresado)
                    .matches()
            ) {

                correo.error = "Correo no válido"
                correo.requestFocus()

                return@setOnClickListener
            }


            // ------------------------------------------
            // VALIDAR CONTRASEÑA
            // ------------------------------------------

            if (passwordIngresada.isEmpty()) {

                password.error = "Ingresa tu contraseña"
                password.requestFocus()

                return@setOnClickListener
            }


            // ==========================================
            // COMPROBAR CREDENCIALES CON LA API
            // ==========================================

            lifecycleScope.launch {
                try {
                    val response = RetrofitClient.apiService.login(
                        LoginRequest(correoIngresado, passwordIngresada)
                    )

                    if (response.isSuccessful && response.body() != null) {
                        val loginResponse = response.body()!!
                        
                        // Guardar token JWT para que todas las peticiones a la API tengan permiso
                        val token = loginResponse.authToken
                        com.example.movilmanupuladora.utils.SessionManager.saveToken(this@activity_login, token)

                        val nombreUsuario = loginResponse.usuario?.nombre ?: "Manipuladora"
                        Toast.makeText(
                            this@activity_login,
                            "¡Bienvenido $nombreUsuario!",
                            Toast.LENGTH_SHORT
                        ).show()

                        val intent = Intent(this@activity_login, MainActivity::class.java)
                        startActivity(intent)
                        finish()
                    } else {
                        // Respaldo de prueba local por si acaso
                        if (correoIngresado == correoCorrecto && passwordIngresada == passwordCorrecta) {
                            Toast.makeText(this@activity_login, "Inicio de sesión exitoso (Prueba)", Toast.LENGTH_SHORT).show()
                            val intent = Intent(this@activity_login, MainActivity::class.java)
                            startActivity(intent)
                            finish()
                        } else {
                            Toast.makeText(
                                this@activity_login,
                                "Correo o contraseña incorrectos",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                } catch (e: Exception) {
                    // Si falla la red, también permitimos el usuario de prueba local
                    if (correoIngresado == correoCorrecto && passwordIngresada == passwordCorrecta) {
                        Toast.makeText(this@activity_login, "Inicio de sesión exitoso (Modo offline)", Toast.LENGTH_SHORT).show()
                        val intent = Intent(this@activity_login, MainActivity::class.java)
                        startActivity(intent)
                        finish()
                    } else {
                        Toast.makeText(
                            this@activity_login,
                            "Error de conexión: ${e.localizedMessage}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }


        // ==========================================
        // OLVIDÉ MI CONTRASEÑA
        // ==========================================

        txtOlvide.setOnClickListener {

            Toast.makeText(
                this,
                "Contacta al administrador para recuperar tu contraseña",
                Toast.LENGTH_LONG
            ).show()
        }


        // ==========================================
        // ADMINISTRADOR
        // ==========================================

        txtAdministrador.setOnClickListener {

            Toast.makeText(
                this,
                "Comunícate con el administrador de SIRAE",
                Toast.LENGTH_LONG
            ).show()
        }
    }
}