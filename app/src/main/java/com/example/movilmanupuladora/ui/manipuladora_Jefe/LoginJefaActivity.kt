package com.example.movilmanupuladora.ui.manipuladora_Jefe

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.movilmanupuladora.databinding.ActivityLoginJefaBinding

class LoginJefaActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginJefaBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityLoginJefaBinding.inflate(layoutInflater)
        setContentView(binding.root)

        configurarLogin()
    }

    private fun configurarLogin() {

        // =====================================================
        // BOTÓN INGRESAR
        // =====================================================

        binding.btnIngresar.setOnClickListener {

            val correo = binding.txtCorreo.text.toString().trim()
            val password = binding.txtPassword.text.toString()

            // ================================================
            // VALIDACIÓN PARA PRUEBAS
            // ================================================

            if (correo.isEmpty()) {
                binding.txtCorreo.error = "Ingresa un correo"
                binding.txtCorreo.requestFocus()
                return@setOnClickListener
            }

            if (password.isEmpty()) {
                binding.txtPassword.error = "Ingresa una contraseña"
                binding.txtPassword.requestFocus()
                return@setOnClickListener
            }

            // =================================================
            // CUALQUIER CORREO Y CONTRASEÑA SON VÁLIDOS
            // =================================================

            Toast.makeText(
                this,
                "Ingreso correcto",
                Toast.LENGTH_SHORT
            ).show()

            // =================================================
            // IR A INICIO DE LA JEFA
            // =================================================

            val intent = Intent(
                this,
                InicioJefaActivity::class.java
            )

            startActivity(intent)

            // No regresar al login
            finish()
        }


        // =====================================================
        // OLVIDÉ MI CONTRASEÑA
        // =====================================================

        binding.txtOlvide.setOnClickListener {

            Toast.makeText(
                this,
                "Recuperación de contraseña pendiente",
                Toast.LENGTH_SHORT
            ).show()
        }


        // =====================================================
        // CONTACTAR ADMINISTRADOR
        // =====================================================

        binding.txtAdministrador.setOnClickListener {

            Toast.makeText(
                this,
                "Contacta al administrador",
                Toast.LENGTH_SHORT
            ).show()
        }
    }
}