package com.example.movilmanupuladora.ui.manipuladora_Jefa

import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.movilmanupuladora.R
import com.example.movilmanupuladora.databinding.ActivityRedactarRecetaBinding

class RedactarRecetaActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRedactarRecetaBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityRedactarRecetaBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        configurarEventos()
    }

    private fun configurarEventos() {
        binding.btnVolver.setOnClickListener {
            finish()
        }

        binding.btnCancelar.setOnClickListener {
            finish()
        }

        binding.btnGuardarReceta.setOnClickListener {
            val nombre = binding.etNombrePlato.text.toString().trim()
            val ingredientes = binding.etIngredientes.text.toString().trim()
            val instrucciones = binding.etInstrucciones.text.toString().trim()

            if (nombre.isEmpty()) {
                binding.etNombrePlato.error = "Ingresa el nombre del plato"
                binding.etNombrePlato.requestFocus()
                return@setOnClickListener
            }

            if (ingredientes.isEmpty()) {
                binding.etIngredientes.error = "Ingresa los ingredientes requeridos"
                binding.etIngredientes.requestFocus()
                return@setOnClickListener
            }

            if (instrucciones.isEmpty()) {
                binding.etInstrucciones.error = "Ingresa las instrucciones de preparación"
                binding.etInstrucciones.requestFocus()
                return@setOnClickListener
            }

            val momento = when (binding.rgMomentoMenu.checkedRadioButtonId) {
                R.id.rbDesayuno -> "Desayuno"
                R.id.rbRefrigerio -> "Refrigerio"
                else -> "Almuerzo"
            }

            PreparacionesManager.agregarPreparacion(
                menu = nombre,
                jornada = if (momento == "Desayuno") "Mañana" else "Tarde",
                ninos = 120
            )

            Toast.makeText(
                this,
                "¡Receta '$nombre' ($momento) guardada y asociada al menú exitosamente!",
                Toast.LENGTH_LONG
            ).show()

            finish()
        }
    }
}
