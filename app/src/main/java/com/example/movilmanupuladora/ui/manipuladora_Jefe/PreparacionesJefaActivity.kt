package com.example.movilmanupuladora.ui.manipuladora_Jefe

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.movilmanupuladora.R
import com.example.movilmanupuladora.databinding.ActivityPreparacionesJefaBinding
import com.example.movilmanupuladora.utils.SessionManager

class PreparacionesJefaActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPreparacionesJefaBinding
    private lateinit var sessionManager: SessionManager

    private var jornadaActual = Jornada.MANANA

    enum class Jornada {
        MANANA,
        TARDE
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityPreparacionesJefaBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessionManager = SessionManager(this)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
            insets
        }

        configurarNavegacionInferior()
        configurarEventos()
        actualizarDatosJornada()
    }

    override fun onResume() {
        super.onResume()
        actualizarDatosJornada()
    }

    private fun configurarEventos() {
        binding.btnManana.setOnClickListener {
            jornadaActual = Jornada.MANANA
            actualizarBotonesJornada()
            actualizarDatosJornada()
        }

        binding.btnTarde.setOnClickListener {
            jornadaActual = Jornada.TARDE
            actualizarBotonesJornada()
            actualizarDatosJornada()
        }

        binding.btnIniciarPreparacion.setOnClickListener {
            val plato = binding.txtPlatoPreparacion.text.toString()
            val jornadaNombre = if (jornadaActual == Jornada.MANANA) "Mañana" else "Tarde"

            val jefaPrefs = getSharedPreferences("ManipuladoraJefe", MODE_PRIVATE)
            val compJefa = jefaPrefs.getString("comp_jefa", null)

            if (!compJefa.isNullOrBlank()) {
                val intent = Intent(this, com.example.movilmanupuladora.ui.manipuladora.PreparacionActivity::class.java).apply {
                    putExtra("nombre_plato", plato)
                    putExtra("componente_seleccionado", compJefa)
                    putExtra("id_imagen", InicioJefaActivity.obtenerImagenPlatoPorNombre(plato))
                }
                startActivity(intent)
            } else {
                val intent = Intent(this, IniciarPreparacionActivity::class.java).apply {
                    putExtra("nombre_menu", plato)
                    putExtra("jornada", jornadaNombre)
                }
                startActivity(intent)
            }
        }

        binding.btnVerManipuladoras.setOnClickListener {
            val jornadaNombre = if (jornadaActual == Jornada.MANANA) "Mañana" else "Tarde"
            val intent = Intent(this, ManipuladorasAsignadasActivity::class.java).apply {
                putExtra("jornada", jornadaNombre)
            }
            startActivity(intent)
        }
    }

    private fun actualizarBotonesJornada() {
        val amarillo = ContextCompat.getColor(this, R.color.amarillo_principal)
        val gris = ContextCompat.getColor(this, R.color.gris_claro)
        val blanco = ContextCompat.getColor(this, R.color.blanco)
        val negro = ContextCompat.getColor(this, R.color.negro_principal)

        if (jornadaActual == Jornada.MANANA) {
            binding.btnManana.backgroundTintList = android.content.res.ColorStateList.valueOf(amarillo)
            binding.btnManana.setTextColor(blanco)
            binding.btnTarde.backgroundTintList = android.content.res.ColorStateList.valueOf(gris)
            binding.btnTarde.setTextColor(negro)
        } else {
            binding.btnTarde.backgroundTintList = android.content.res.ColorStateList.valueOf(amarillo)
            binding.btnTarde.setTextColor(blanco)
            binding.btnManana.backgroundTintList = android.content.res.ColorStateList.valueOf(gris)
            binding.btnManana.setTextColor(negro)
        }
    }

    private fun actualizarDatosJornada() {
        actualizarBotonesJornada()

        val guardados = if (jornadaActual == Jornada.MANANA) {
            AsistenciaManager.obtenerManana(this)
        } else {
            AsistenciaManager.obtenerTarde(this)
        }
        val ninos = if (guardados > 0) guardados else if (jornadaActual == Jornada.MANANA) 120 else 110

        if (jornadaActual == Jornada.MANANA) {
            binding.txtTituloJornada.text = "Preparación de la mañana"
            binding.txtNinosPreparacion.text = "$ninos niños"
        } else {
            binding.txtTituloJornada.text = "Preparación de la tarde"
            binding.txtNinosPreparacion.text = "$ninos niños"
        }

        val platoSeleccionado = getSharedPreferences("SIRAE", MODE_PRIVATE).getString("plato_seleccionado", "Pollo Guisado Criollo") ?: "Pollo Guisado Criollo"
        binding.txtPlatoPreparacion.text = platoSeleccionado

        val jefaPrefs = getSharedPreferences("ManipuladoraJefe", MODE_PRIVATE)
        val compJefa = jefaPrefs.getString("comp_jefa", null)
        val miNombre = sessionManager.getUserName() ?: "Isa Pérez"
        val miCorreo = sessionManager.getUserEmail() ?: ""

        val estadoJefa = if (!compJefa.isNullOrBlank()) {
            PreparacionEstadoManager.obtenerEstado(this, miNombre, compJefa, miCorreo)
        } else {
            null
        }

        if (!compJefa.isNullOrBlank()) {
            binding.txtDescripcionPlato.text = "Plato de ${if (jornadaActual == Jornada.MANANA) "la mañana" else "la tarde"}.\n👨‍🍳 Tu componente asignado: $compJefa ($estadoJefa)"
        } else {
            binding.txtDescripcionPlato.text = "Plato programado para la jornada de ${if (jornadaActual == Jornada.MANANA) "la mañana" else "la tarde"}."
        }

        val arrozKg = (ninos * 0.1).toInt().coerceAtLeast(10)
        val polloKg = (ninos * 0.15).toInt().coerceAtLeast(15)
        val lentejasKg = (ninos * 0.08).toInt().coerceAtLeast(8)
        val pastaKg = (ninos * 0.08).toInt().coerceAtLeast(8)

        binding.txtCantidadArroz.text = "$arrozKg kg"
        binding.txtCantidadPollo.text = "$polloKg kg"
        binding.txtCantidadLentejas.text = "$lentejasKg kg"
        binding.txtCantidadPasta.text = "$pastaKg kg"

        val estado = getSharedPreferences("SIRAE_JEFA", MODE_PRIVATE).getString("estado_plato", "PENDIENTE")
        if (estado == "EN_PREPARACION" || estadoJefa == PreparacionEstadoManager.ESTADO_EN_PROCESO) {
            binding.txtEstadoPreparacion.text = "En proceso"
            binding.txtEstadoPreparacion.setTextColor(Color.parseColor("#7A5500"))
            binding.btnIniciarPreparacion.text = if (!compJefa.isNullOrBlank()) "Preparar mi componente: $compJefa" else "Continuar preparación"
        } else if (estado == "FINALIZADO" || estadoJefa == PreparacionEstadoManager.ESTADO_FINALIZADA) {
            binding.txtEstadoPreparacion.text = "Completado"
            binding.txtEstadoPreparacion.setTextColor(Color.parseColor("#2A5226"))
            binding.btnIniciarPreparacion.text = if (!compJefa.isNullOrBlank()) "Preparación completada ($compJefa)" else "Preparación completada"
        } else {
            binding.txtEstadoPreparacion.text = "Pendiente"
            binding.txtEstadoPreparacion.setTextColor(Color.parseColor("#D92D20"))
            binding.btnIniciarPreparacion.text = if (!compJefa.isNullOrBlank()) "Iniciar mi componente: $compJefa" else "Iniciar preparación"
        }
    }

    private fun configurarNavegacionInferior() {
        com.example.movilmanupuladora.utils.NavigationHelperJefa.setupBarraNavegacionJefa(
            this,
            binding.barraNavegacionJefa,
            com.example.movilmanupuladora.utils.NavigationHelperJefa.TabJefa.PREPARACIONES
        )
    }
}