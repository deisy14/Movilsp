package com.example.movilmanupuladora.ui.manipuladora

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import com.example.movilmanupuladora.databinding.ActivityTurnoBinding

class TurnoActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTurnoBinding

    private val handler = Handler(Looper.getMainLooper())

    private val irSiguientePantalla = Runnable {

        val intent = Intent(
            this,
            MainActivity::class.java
        )

        startActivity(intent)
        finish()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        // =====================================================
        // VIEW BINDING
        // =====================================================

        binding = ActivityTurnoBinding.inflate(layoutInflater)

        setContentView(binding.root)

        // =====================================================
        // BARRAS DEL SISTEMA
        // =====================================================

        ViewCompat.setOnApplyWindowInsetsListener(
            binding.main
        ) { view, insets ->

            val systemBars =
                insets.getInsets(
                    WindowInsetsCompat.Type.systemBars()
                )

            view.setPadding(
                systemBars.left,
                systemBars.top,
                systemBars.right,
                systemBars.bottom
            )

            insets
        }

        // =====================================================
        // CARGAR DATOS DEL TURNO DESDE EL BACKEND
        // =====================================================

        cargarTurno()

        // Permitir tocar la tarjeta para continuar sin esperar
        binding.cardTurno.setOnClickListener {
            handler.removeCallbacks(irSiguientePantalla)
            irSiguientePantalla.run()
        }

        // =====================================================
        // CONTINUAR AUTOMÁTICAMENTE
        // =====================================================

        handler.postDelayed(
            irSiguientePantalla,
            8_000
        )
    }

    private fun cargarTurno() {
        lifecycleScope.launch {
            try {
                // Mantener el título original del diseño XML
                binding.txtTituloTurno.text = "Iniciando tu turno"
                binding.txtSubtituloTurno.text = "Tu plato asignado es:"

                // Cargar plato si está disponible
                try {
                    val resPlatos = com.example.movilmanupuladora.data.api.RetrofitClient.apiService.obtenerPlatos()
                    if (resPlatos.isSuccessful && !resPlatos.body().isNullOrEmpty()) {
                        binding.txtPlatoAsignado.text = resPlatos.body()!!.first().nombrePlato
                    } else {
                        binding.txtPlatoAsignado.text = "Pollo guisado"
                    }
                } catch (e: Exception) {
                    binding.txtPlatoAsignado.text = "Pollo guisado"
                }

                // Cargar horario de turno
                val res = com.example.movilmanupuladora.data.api.RetrofitClient.apiService.obtenerTurnos()
                if (res.isSuccessful && !res.body().isNullOrEmpty()) {
                    val turno = res.body()!!.first()
                    val nombreRaw = turno.nombreTurno ?: "Mañana"
                    // Evitar repetir la palabra "Turno" si el backend ya la trae (ej. "Turno Mañana MOD")
                    val nombreLimpio = if (nombreRaw.trim().startsWith("Turno", ignoreCase = true)) {
                        nombreRaw.trim()
                    } else {
                        "Turno ${nombreRaw.trim()}"
                    }
                    val hInicio = turno.horaInicio ?: "6:00 a. m."
                    val hFin = turno.horaFin ?: "2:00 p. m."
                    binding.txtMensajeTurno.text = "$nombreLimpio · $hInicio - $hFin · Ingresando..."
                } else {
                    binding.txtMensajeTurno.text = "Turno Mañana · 6:00 a. m. - 2:00 p. m. · Ingresando..."
                }
            } catch (e: Exception) {
                binding.txtTituloTurno.text = "Iniciando tu turno"
                binding.txtPlatoAsignado.text = "Pollo guisado"
                binding.txtMensajeTurno.text = "Turno Mañana · 6:00 a. m. - 2:00 p. m. · Ingresando..."
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        // Evita que la navegación se ejecute
        // si la Activity se destruye antes.
        handler.removeCallbacks(
            irSiguientePantalla
        )
    }
}