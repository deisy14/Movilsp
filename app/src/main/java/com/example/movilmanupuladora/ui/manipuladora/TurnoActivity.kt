package com.example.movilmanupuladora.ui.manipuladora

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.movilmanupuladora.data.api.RetrofitClient
import com.example.movilmanupuladora.databinding.ActivityTurnoBinding
import com.example.movilmanupuladora.utils.PreparacionSyncManager
import com.example.movilmanupuladora.utils.SessionManager
import kotlinx.coroutines.launch

class TurnoActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTurnoBinding

    private val handler = Handler(Looper.getMainLooper())
    private var isNavigating = false

    private val irSiguientePantalla = Runnable {
        if (!isNavigating) {
            isNavigating = true
            val intent = Intent(
                this,
                MainActivity::class.java
            )

            startActivity(intent)
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        binding = ActivityTurnoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        cargarTurno()

        binding.cardTurno.setOnClickListener {
            handler.removeCallbacks(irSiguientePantalla)
            irSiguientePantalla.run()
        }

        handler.postDelayed(irSiguientePantalla, 5_000)
    }

    private fun cargarTurno() {
        val sessionMgr = SessionManager(this)
        var platoJefa = sessionMgr.getPlatoJefa(this)
        var componenteAsignado = sessionMgr.getComponenteAsignado(this)
        val nombreUser = sessionMgr.getUserName() ?: ""

        if (nombreUser.isNotBlank()) {
            binding.txtTituloTurno.text = "¡Hola, $nombreUser!"
        } else {
            binding.txtTituloTurno.text = "Iniciando tu turno"
        }

        if (platoJefa.isNotBlank()) {
            binding.txtPlatoAsignado.text = "$platoJefa  ➔"
        } else {
            binding.txtPlatoAsignado.text = "Sin plato asignado  ➔"
        }

        if (!componenteAsignado.isNullOrBlank()) {
            binding.txtSubtituloTurno.text = componenteAsignado
        } else {
            binding.txtSubtituloTurno.text = "Estación pendiente"
        }

        lifecycleScope.launch {
            try {
                // Sincronizar desde la nube para que aparezca la asignación más reciente hecha por la Jefa
                PreparacionSyncManager.sincronizarDesdeBackend(this@TurnoActivity)

                platoJefa = sessionMgr.getPlatoJefa(this@TurnoActivity)
                componenteAsignado = sessionMgr.getComponenteAsignado(this@TurnoActivity)

                if (platoJefa.isNotBlank()) {
                    binding.txtPlatoAsignado.text = "$platoJefa  ➔"
                } else {
                    binding.txtPlatoAsignado.text = "Sin plato asignado  ➔"
                }
                if (!componenteAsignado.isNullOrBlank()) {
                    binding.txtSubtituloTurno.text = componenteAsignado
                } else {
                    binding.txtSubtituloTurno.text = "Estación pendiente"
                }

                // Cargar horario de turno institucional
                val res = RetrofitClient.apiService.obtenerTurnos()
                if (res.isSuccessful && !res.body().isNullOrEmpty()) {
                    val turno = res.body()!!.firstOrNull { !it.nombreTurno.orEmpty().startsWith("PAE_") }
                        ?: res.body()!!.first()
                    val nombreRaw = turno.nombreTurno ?: "Mañana"
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
                Log.e("TurnoActivity", "Error al sincronizar turno: ${e.message}")
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(irSiguientePantalla)
    }
}
