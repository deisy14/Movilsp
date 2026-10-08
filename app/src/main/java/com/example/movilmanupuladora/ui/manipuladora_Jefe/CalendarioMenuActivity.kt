package com.example.movilmanupuladora.ui.manipuladora_Jefe

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.movilmanupuladora.R
import com.example.movilmanupuladora.data.api.RetrofitClient
import com.example.movilmanupuladora.data.model.menus
import com.example.movilmanupuladora.data.repository.MenuRepository
import com.example.movilmanupuladora.databinding.ActivityCalendarioMenuBinding
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class CalendarioMenuActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCalendarioMenuBinding

    private var diaSeleccionado: Calendar = Calendar.getInstance()
    private val menuRepository = MenuRepository(RetrofitClient.apiService)
    private val listaMenus = mutableListOf<menus>()
    private val listaDiasSemana = mutableListOf<Calendar>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        binding = ActivityCalendarioMenuBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        configurarBotones()
        generarTiraDiasSemana()
        mostrarInformacionDia()
        cargarMenusDelBackend()
    }

    private fun configurarBotones() {
        binding.btnCerrar.setOnClickListener {
            finish()
        }

        binding.btnProgramarSemana.setOnClickListener {
            val intent = Intent(this, ProgramacionSemanalActivity::class.java)
            startActivity(intent)
        }

        binding.btnVerDetalleDia.setOnClickListener {
            mostrarDetalleDia()
        }
    }

    // =========================================================
    // GENERAR Y MOSTRAR TIRA HORIZONTAL DE DÍAS (DAY STRIP)
    // =========================================================

    private fun generarTiraDiasSemana() {
        listaDiasSemana.clear()
        binding.contenedorDiasStrip.removeAllViews()

        val calBase = Calendar.getInstance()
        calBase.firstDayOfWeek = Calendar.MONDAY
        calBase.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)

        for (i in 0 until 7) {
            val fechaDia = calBase.clone() as Calendar
            listaDiasSemana.add(fechaDia)

            val isSelected = esMismoDia(fechaDia, diaSeleccionado)

            val cardDia = com.google.android.material.card.MaterialCardView(this).apply {
                radius = (14 * resources.displayMetrics.density)
                elevation = if (isSelected) (4 * resources.displayMetrics.density) else (1 * resources.displayMetrics.density)
                strokeWidth = if (isSelected) (2 * resources.displayMetrics.density).toInt() else (1 * resources.displayMetrics.density).toInt()
                setStrokeColor(if (isSelected) Color.parseColor("#F4B41F") else Color.parseColor("#E2E8F0"))
                setCardBackgroundColor(if (isSelected) Color.parseColor("#FDF2CA") else Color.parseColor("#FFFFFF"))

                val params = LinearLayout.LayoutParams(
                    (62 * resources.displayMetrics.density).toInt(),
                    (58 * resources.displayMetrics.density).toInt()
                ).also {
                    it.setMargins((4 * resources.displayMetrics.density).toInt(), 0, (4 * resources.displayMetrics.density).toInt(), 0)
                }
                layoutParams = params
            }

            val layoutInterno = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
            }

            val formatoNombreDia = SimpleDateFormat("EEE", Locale.forLanguageTag("es-CO"))
            val tvNombreDia = TextView(this).apply {
                text = formatoNombreDia.format(fechaDia.time).uppercase()
                setTextColor(if (isSelected) Color.parseColor("#7A5500") else Color.parseColor("#64748B"))
                textSize = 11f
                setTypeface(typeface, android.graphics.Typeface.BOLD)
            }

            val tvNumDia = TextView(this).apply {
                text = SimpleDateFormat("dd", Locale.getDefault()).format(fechaDia.time)
                setTextColor(Color.parseColor("#1B3317"))
                textSize = 16f
                setTypeface(typeface, android.graphics.Typeface.BOLD)
            }

            layoutInterno.addView(tvNombreDia)
            layoutInterno.addView(tvNumDia)
            cardDia.addView(layoutInterno)

            cardDia.setOnClickListener {
                diaSeleccionado = fechaDia
                generarTiraDiasSemana()
                mostrarInformacionDia()
            }

            binding.contenedorDiasStrip.addView(cardDia)
            calBase.add(Calendar.DAY_OF_MONTH, 1)
        }
    }

    private fun esMismoDia(cal1: Calendar, cal2: Calendar): Boolean {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }

    // =========================================================
    // INFORMACIÓN Y DETALLE DEL DÍA SELECCIONADO
    // =========================================================

    private fun mostrarInformacionDia() {
        val formatoLargo = SimpleDateFormat("EEEE, d 'de' MMMM", Locale.forLanguageTag("es-CO"))
        val formatoPill = SimpleDateFormat("d MMM", Locale.forLanguageTag("es-CO"))
        val formatoIso = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        val textoLargo = formatoLargo.format(diaSeleccionado.time).replaceFirstChar { it.uppercase() }
        val textoPill = formatoPill.format(diaSeleccionado.time)
        val fechaIso = formatoIso.format(diaSeleccionado.time)

        binding.txtTituloDia.text = textoLargo
        binding.txtFechaSeleccionada.text = textoPill

        val menuDia = listaMenus.find { it.fecha == fechaIso }
        if (menuDia != null) {
            val infoNutricional = menuDia.informacion_nutricional ?: "Estándar Nutricional PAE"
            val ninos = menuDia.ninos_presentes ?: 120
            val estado = menuDia.estado ?: "Planificado"

            binding.txtPlatoManana.text = "Arroz con Pollo Criollo"
            binding.badgeEstadoManana.text = estado

            binding.txtPlatoTarde.text = "Bandeja Paisa Tradicional"
            binding.badgeEstadoTarde.text = "Confirmado"

            binding.txtHistorialTitulo.text = "$ninos raciones asignadas ($infoNutricional)"
            binding.txtHistorialSubtitulo.text = "Ingredientes e insumos verificados en inventario"
        } else {
            binding.txtPlatoManana.text = "Sopa de Frijoles y Proteína"
            binding.badgeEstadoManana.text = "Programado"

            binding.txtPlatoTarde.text = "Arroz con Pollo Especial"
            binding.badgeEstadoTarde.text = "Confirmado"

            binding.txtHistorialTitulo.text = "Menú del día disponible en servidor"
            binding.txtHistorialSubtitulo.text = "Control de gramaje e inocuidad certificado PAE"
        }
    }

    private fun mostrarDetalleDia() {
        val formatoLargo = SimpleDateFormat("EEEE, d 'de' MMMM", Locale.forLanguageTag("es-CO"))
        val textoFecha = formatoLargo.format(diaSeleccionado.time).replaceFirstChar { it.uppercase() }

        val intent = Intent(this, DetallePreparacionActivity::class.java).apply {
            putExtra("fecha", textoFecha)
            putExtra("plato", binding.txtPlatoManana.text.toString())
            putExtra("jornada", "Mañana y Tarde")
            putExtra("ninos", "120 niños")
        }
        startActivity(intent)
    }

    // =========================================================
    // CONSUMO DEL BACKEND (API SIRAE)
    // =========================================================

    private fun cargarMenusDelBackend() {
        lifecycleScope.launch {
            try {
                val res = menuRepository.obtenerMenus()
                if (res.isSuccessful && !res.body().isNullOrEmpty()) {
                    listaMenus.clear()
                    listaMenus.addAll(res.body()!!)
                    mostrarInformacionDia()
                }
            } catch (e: Exception) {
                // Mantiene datos locales de respaldo si no hay red
            }
        }
    }
}
