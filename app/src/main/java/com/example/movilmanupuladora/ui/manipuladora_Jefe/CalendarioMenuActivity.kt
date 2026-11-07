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
import com.google.android.material.dialog.MaterialAlertDialogBuilder
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
            mostrarPantallaFlotanteHistorial()
        }
    }

    // =========================================================
    // GENERAR TIRA HORIZONTAL DE TODOS LOS DÍAS DEL MES
    // =========================================================

    private fun generarTiraDiasSemana() {
        listaDiasSemana.clear()
        binding.contenedorDiasStrip.removeAllViews()

        val calBase = diaSeleccionado.clone() as Calendar
        calBase.set(Calendar.DAY_OF_MONTH, 1)

        val totalDiasMes = calBase.getActualMaximum(Calendar.DAY_OF_MONTH)
        val formatoNombreMes = SimpleDateFormat("MMMM yyyy", Locale.forLanguageTag("es-CO"))
        val nombreMesAnio = formatoNombreMes.format(calBase.time).uppercase()
        binding.txtSubtitulo.text = "Mes: $nombreMesAnio · Días pasados destacados · Toca cualquier día"

        val hoyCero = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        for (i in 1..totalDiasMes) {
            val fechaDia = calBase.clone() as Calendar
            fechaDia.set(Calendar.HOUR_OF_DAY, 0)
            fechaDia.set(Calendar.MINUTE, 0)
            fechaDia.set(Calendar.SECOND, 0)
            fechaDia.set(Calendar.MILLISECOND, 0)

            listaDiasSemana.add(fechaDia)

            val isToday = esMismoDia(fechaDia, Calendar.getInstance())
            val isPast = fechaDia.before(hoyCero)
            val isSelected = esMismoDia(fechaDia, diaSeleccionado)

            val cardDia = com.google.android.material.card.MaterialCardView(this).apply {
                radius = (14 * resources.displayMetrics.density)
                elevation = if (isSelected) (4 * resources.displayMetrics.density) else (1 * resources.displayMetrics.density)

                when {
                    isToday -> {
                        // HOY EN AMARILLO RELLENO
                        strokeWidth = (2 * resources.displayMetrics.density).toInt()
                        setStrokeColor(Color.parseColor("#F4B41F"))
                        setCardBackgroundColor(Color.parseColor("#F4B41F"))
                    }
                    isPast || isSelected -> {
                        // ANTERIORES Y SELECCIONADO EN PUNTICOS / BORDE AMARILLO
                        strokeWidth = (1.5 * resources.displayMetrics.density).toInt()
                        setStrokeColor(Color.parseColor("#F4B41F"))
                        setCardBackgroundColor(Color.parseColor("#FEF3C7"))
                    }
                    else -> {
                        // MAÑANA Y LOS RESTANTES EN BLANCO
                        strokeWidth = (1 * resources.displayMetrics.density).toInt()
                        setStrokeColor(Color.parseColor("#E2E8F0"))
                        setCardBackgroundColor(Color.parseColor("#FFFFFF"))
                    }
                }

                val sizePx = (58 * resources.displayMetrics.density).toInt()
                val params = LinearLayout.LayoutParams(sizePx, sizePx).apply {
                    setMargins((5 * resources.displayMetrics.density).toInt(), 0, (5 * resources.displayMetrics.density).toInt(), 0)
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
                setTextColor(
                    when {
                        isToday -> Color.parseColor("#FFFFFF")
                        isSelected || isPast -> Color.parseColor("#7A5500")
                        else -> Color.parseColor("#94A3B8") // BLANCO / GRIS SUAVE
                    }
                )
                textSize = 9.5f
                setTypeface(typeface, android.graphics.Typeface.BOLD)
            }

            val tvNumDia = TextView(this).apply {
                text = SimpleDateFormat("dd", Locale.getDefault()).format(fechaDia.time)
                setTextColor(
                    when {
                        isToday -> Color.parseColor("#FFFFFF")
                        isSelected || isPast -> Color.parseColor("#1B3317")
                        else -> Color.parseColor("#64748B") // BLANCO / GRIS SUAVE
                    }
                )
                textSize = 15f
                setTypeface(typeface, android.graphics.Typeface.BOLD)
            }

            layoutInterno.addView(tvNombreDia)
            layoutInterno.addView(tvNumDia)

            // Puntico amarillo de efecto en días anteriores o seleccionado
            if (isPast || isSelected) {
                val dotView = android.view.View(this).apply {
                    val dotParams = LinearLayout.LayoutParams(
                        (5 * resources.displayMetrics.density).toInt(),
                        (5 * resources.displayMetrics.density).toInt()
                    ).apply {
                        topMargin = (2 * resources.displayMetrics.density).toInt()
                    }
                    layoutParams = dotParams
                    setBackgroundResource(R.drawable.ic_dot_yellow)
                }
                layoutInterno.addView(dotView)
            }

            cardDia.addView(layoutInterno)

            cardDia.setOnClickListener {
                diaSeleccionado = fechaDia
                generarTiraDiasSemana()
                mostrarInformacionDia()
            }

            binding.contenedorDiasStrip.addView(cardDia)
            calBase.add(Calendar.DAY_OF_MONTH, 1)
        }

        // Auto-scroll al día seleccionado
        binding.scrollDiasSemana.post {
            val selectedDay = diaSeleccionado.get(Calendar.DAY_OF_MONTH) - 1
            val cardWidth = (68 * resources.displayMetrics.density).toInt()
            binding.scrollDiasSemana.smoothScrollTo(selectedDay * cardWidth, 0)
        }
    }

    private fun esMismoDia(cal1: Calendar, cal2: Calendar): Boolean {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }

    // =========================================================
    // INFORMACIÓN Y PANTALLA FLOTANTE DEL DÍA Y OPERARIAS
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
            val infoNutricional = menuDia.informacion_nutricional ?: "Estándar PAE"
            val ninos = menuDia.ninos_presentes ?: 120
            val estado = menuDia.estado ?: "Planificado"

            binding.txtPlatoManana.text = "Arroz con Pollo Criollo"
            binding.badgeEstadoManana.text = estado

            binding.txtPlatoTarde.text = "Bandeja Paisa Tradicional"
            binding.badgeEstadoTarde.text = "Confirmado"

            binding.txtHistorialTitulo.text = "$ninos raciones asignadas ($infoNutricional)"
            binding.txtHistorialSubtitulo.text = "Operarias a cargo: Ana López & María Rodríguez"
        } else {
            binding.txtPlatoManana.text = "Sopa de Frijoles y Proteína"
            binding.badgeEstadoManana.text = "Programado"

            binding.txtPlatoTarde.text = "Arroz con Pollo Especial"
            binding.badgeEstadoTarde.text = "Confirmado"

            binding.txtHistorialTitulo.text = "Menú del día registrado en servidor"
            binding.txtHistorialSubtitulo.text = "Operarias a cargo: Ana López & María Rodríguez"
        }
    }

    private fun mostrarPantallaFlotanteHistorial() {
        val formatoLargo = SimpleDateFormat("EEEE, d 'de' MMMM", Locale.forLanguageTag("es-CO"))
        val textoFecha = formatoLargo.format(diaSeleccionado.time).replaceFirstChar { it.uppercase() }

        val mensajeHistorial = """
            Fecha: $textoFecha
            Plato Mañana: ${binding.txtPlatoManana.text}
            Plato Tarde: ${binding.txtPlatoTarde.text}
            
            Manipuladoras a cargo del plato:
            • Ana López (Encargada de cocción)
            • María Rodríguez (Porcionado e inocuidad)
            
            Estado del turno: Registrado e insumos al día.
        """.trimIndent()

        val dialogBinding = com.example.movilmanupuladora.databinding.DialogConfirmarAsignacionBinding.inflate(layoutInflater)
        dialogBinding.txtTituloConfirmacion.text = "Historial y Responsables del Día"
        dialogBinding.txtMensajeConfirmacionAsignacion.text = mensajeHistorial
        dialogBinding.btnConfirmarGuardarAsignacion.text = "Ver Receta Completa"
        dialogBinding.btnSeguirEditandoAsignacion.text = "Cerrar"

        val dialog = MaterialAlertDialogBuilder(this)
            .setView(dialogBinding.root)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        dialogBinding.btnConfirmarGuardarAsignacion.setOnClickListener {
            dialog.dismiss()
            val intent = Intent(this, DetallePreparacionActivity::class.java).apply {
                putExtra("fecha", textoFecha)
                putExtra("plato", binding.txtPlatoManana.text.toString())
                putExtra("jornada", "Mañana y Tarde")
                putExtra("ninos", "120 niños")
            }
            startActivity(intent)
        }

        dialogBinding.btnSeguirEditandoAsignacion.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.90).toInt(),
            android.view.ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

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
                // Conserva datos locales si no hay red
            }
        }
    }
}