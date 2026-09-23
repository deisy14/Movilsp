package com.example.movilmanupuladora.ui.manipuladora

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.movilmanupuladora.R
import com.example.movilmanupuladora.databinding.ActivityRegistrarEntradaBinding

class RegistrarEntradaActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegistrarEntradaBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        binding = ActivityRegistrarEntradaBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // ==========================================
        // INSETS
        // ==========================================

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->

            val systemBars =
                insets.getInsets(WindowInsetsCompat.Type.systemBars())

            v.setPadding(
                systemBars.left,
                systemBars.top,
                systemBars.right,
                systemBars.bottom
            )

            insets
        }

        // ==========================================
        // VOLVER
        // ==========================================

        binding.btnVolver.setOnClickListener {
            finish()
        }

        // ==========================================
        // REGISTRAR ENTRADA
        // ==========================================

        binding.btnGuardarEntrada.setOnClickListener {

            val ingrediente =
                binding.edtIngrediente.text.toString().trim()

            val cantidad =
                binding.edtCantidad.text.toString().trim()

            val unidad =
                binding.edtUnidad.text.toString().trim()

            // Validaciones
            if (ingrediente.isEmpty()) {

                binding.edtIngrediente.error =
                    "Ingresa el ingrediente"

                binding.edtIngrediente.requestFocus()

                return@setOnClickListener
            }

            if (cantidad.isEmpty()) {

                binding.edtCantidad.error =
                    "Ingresa la cantidad"

                binding.edtCantidad.requestFocus()

                return@setOnClickListener
            }

            if (unidad.isEmpty()) {

                binding.edtUnidad.error =
                    "Ingresa la unidad"

                binding.edtUnidad.requestFocus()

                return@setOnClickListener
            }

            // ==========================================
            // POR AHORA
            // ==========================================

            Toast.makeText(
                this,
                "Entrada registrada correctamente",
                Toast.LENGTH_SHORT
            ).show()

            // Regresar al inventario
            val intent = Intent(
                this,
                InventarioActivity::class.java
            )

            intent.flags =
                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP

            startActivity(intent)

            finish()
        }

        // ==========================================
        // BARRA DE NAVEGACIÓN
        // ==========================================

        configurarBarraNavegacion()
    }

    private fun configurarBarraNavegacion() {

        // INICIO
        binding.barraNavegacion.navInicio.setOnClickListener {

            startActivity(
                Intent(this, MainActivity::class.java)
            )

            finish()
        }

        // ASIGNADAS
        binding.barraNavegacion.navAsignadas.setOnClickListener {

            startActivity(
                Intent(this, ComponentesActivity::class.java)
            )

            finish()
        }

        // INVENTARIO
        binding.barraNavegacion.navInventario.setOnClickListener {

            startActivity(
                Intent(this, InventarioActivity::class.java)
            )

            finish()
        }

        // AVISOS
        binding.barraNavegacion.navAvisos.setOnClickListener {

            startActivity(
                Intent(this, AvisosActivity::class.java)
            )

            finish()
        }

        // PERFIL
        binding.barraNavegacion.navPerfil.setOnClickListener {

            startActivity(
                Intent(this, PerfilActivity::class.java)
            )

            finish()
        }
    }
}