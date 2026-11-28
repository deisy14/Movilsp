package com.example.movilmanupuladora.ui.manipuladora

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.movilmanupuladora.databinding.ActivityTurnoBinding
import com.example.movilmanupuladora.utils.PreparacionSyncManager
import com.example.movilmanupuladora.utils.SessionManager
import kotlinx.coroutines.launch

class TurnoActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTurnoBinding
    private var isNavigating = false

    private fun irSiguientePantalla() {
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

        binding.cardPlato.setOnClickListener {
            irSiguientePantalla()
        }
    }

    private fun limpiarNombreMenu(raw: String): String {
        return raw.trim()
            .replace(Regex("(?i)^men[uú]\\s*(?:n[°o.]?\\s*)?\\d*\\s*:\\s*"), "")
            .trim()
            .ifBlank { raw.trim() }
    }

    private fun cargarTurno() {
        val sessionMgr = SessionManager(this)
        val platoJefa = sessionMgr.getPlatoJefa(this)
        val componenteAsignado = sessionMgr.getComponenteAsignado(this)
        val nombreUser = sessionMgr.getUserName() ?: ""

        binding.txtTituloTurno.text = if (nombreUser.isNotBlank()) {
            "¡Hola, $nombreUser!"
        } else {
            "¡Hola, Manipuladora!"
        }

        binding.txtSubtituloTurno.text = if (platoJefa.isNotBlank()) {
            "Menú: ${limpiarNombreMenu(platoJefa)}"
        } else {
            "Menú: Sincronizando..."
        }

        binding.txtPlatoAsignado.text = if (!componenteAsignado.isNullOrBlank()) {
            "Preparar componente: $componenteAsignado  ➔"
        } else {
            "Preparar componente: Consultando...  ➔"
        }

        binding.txtSeccionTurno.text = "Sección de menú: Sincronizando..."
        binding.txtMensajeTurno.text = "Sincronizando turno y horario..."

        lifecycleScope.launch {
            try {
                // Sincronizar desde la nube para que aparezca la asignación más reciente hecha por la Jefa
                PreparacionSyncManager.sincronizarDesdeBackend(this@TurnoActivity)

                val todas = PreparacionSyncManager.obtenerMisAsignaciones(this@TurnoActivity)
                val pendientes = todas.filter {
                    !it.estado.contains("final", ignoreCase = true) &&
                        !it.estado.contains("complet", ignoreCase = true) &&
                        !it.estado.contains("listo", ignoreCase = true)
                }

                val listaActiva = pendientes.ifEmpty { todas }
                val seccionActiva = listaActiva.firstOrNull()?.servicio
                val itemsSeccion = if (!seccionActiva.isNullOrBlank()) {
                    listaActiva.filter { it.servicio.equals(seccionActiva, ignoreCase = true) }
                } else {
                    listaActiva
                }

                val platoMostrar = itemsSeccion.firstOrNull()?.plato?.takeIf { it.isNotBlank() } ?: platoJefa
                binding.txtSubtituloTurno.text = if (platoMostrar.isNotBlank()) {
                    "Menú: ${limpiarNombreMenu(platoMostrar)}"
                } else {
                    "Menú: Por asignar"
                }

                binding.txtPlatoAsignado.text = if (itemsSeccion.isNotEmpty()) {
                    val comps = itemsSeccion.map { it.componente }.distinct().joinToString(" · ")
                    "Preparar componente: $comps  ➔"
                } else if (!componenteAsignado.isNullOrBlank() && todas.isEmpty()) {
                    "Preparar componente: $componenteAsignado  ➔"
                } else {
                    "Preparar componente: Por asignar  ➔"
                }

                // Mostrar turno real asignado y sección de menú (Desayuno, Almuerzo, Merienda, etc.)
                val emailUser = sessionMgr.getUserEmail()?.trim().orEmpty()
                val manip = com.example.movilmanupuladora.ui.manipuladora_Jefe.ManipuladorasManager
                    .obtenerManipuladoras()
                    .firstOrNull {
                        (emailUser.isNotBlank() && it.correo.equals(emailUser, true)) ||
                            (nombreUser.isNotBlank() && it.nombre.equals(nombreUser, true))
                    }
                val turnoAsignado = manip?.jornada?.takeIf { it.isNotBlank() }
                    ?: getSharedPreferences("TURNO_PAE", MODE_PRIVATE).getString("plato_jornada", null)?.takeIf { it.isNotBlank() }

                val turnoTexto = if (!turnoAsignado.isNullOrBlank()) {
                    if (turnoAsignado.startsWith("Turno", true)) turnoAsignado else "Turno $turnoAsignado"
                } else {
                    "Turno Mañana"
                }
                val horarioTurno = when {
                    turnoTexto.contains("tarde", ignoreCase = true) -> "12:00 – 18:00"
                    turnoTexto.contains("noche", ignoreCase = true) -> "18:00 – 22:00"
                    else -> "06:00 – 14:00"
                }
                val seccionTexto = if (!seccionActiva.isNullOrBlank()) {
                    seccionActiva.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
                } else {
                    "Por asignar"
                }
                binding.txtSeccionTurno.text = "Sección de menú: $seccionTexto"
                binding.txtMensajeTurno.text = "$turnoTexto · $horarioTurno"
            } catch (e: Exception) {
                Log.e("TurnoActivity", "Error al sincronizar turno: ${e.message}")
                binding.txtSeccionTurno.text = "Sección de menú: Por asignar"
                binding.txtMensajeTurno.text = "Turno Mañana · 06:00 – 14:00"
            }
        }
    }
}
