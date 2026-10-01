package com.example.movilmanupuladora.ui.manipuladora_Jefe

import com.example.movilmanupuladora.R

import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.movilmanupuladora.databinding.ActivityCalendarioMenuBinding
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class CalendarioMenuActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCalendarioMenuBinding

    // =========================================================
    // CALENDARIO ACTUAL
    // =========================================================

    private val calendario = Calendar.getInstance()

    // =========================================================
    // DÍA SELECCIONADO
    // =========================================================

    private var diaSeleccionado: Calendar? = null


    // =========================================================
    // ON CREATE
    // =========================================================

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        binding = ActivityCalendarioMenuBinding.inflate(layoutInflater)

        setContentView(binding.root)

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

        configurarBotones()

        configurarCalendario()

        configurarNavegacionInferior()

        mostrarMesActual()
    }


    // =========================================================
    // BOTONES
    // =========================================================

    private fun configurarBotones() {

        // -----------------------------------------------------
        // CERRAR
        // -----------------------------------------------------

        binding.btnCerrar.setOnClickListener {

            finish()
        }


        // -----------------------------------------------------
        // HISTORIAL
        // -----------------------------------------------------

        binding.btnHistorial.setOnClickListener {

            seleccionarHistorial()
        }


        // -----------------------------------------------------
        // PROGRAMAR
        // -----------------------------------------------------

        binding.btnProgramar.setOnClickListener {

            seleccionarProgramar()
        }


        // -----------------------------------------------------
        // MES ANTERIOR
        // -----------------------------------------------------

        binding.btnMesAnterior.setOnClickListener {

            calendario.add(
                Calendar.MONTH,
                -1
            )

            mostrarMesActual()
        }


        // -----------------------------------------------------
        // MES SIGUIENTE
        // -----------------------------------------------------

        binding.btnMesSiguiente.setOnClickListener {

            calendario.add(
                Calendar.MONTH,
                1
            )

            mostrarMesActual()
        }


        // -----------------------------------------------------
        // PROGRAMAR SEMANA
        // -----------------------------------------------------

        binding.btnProgramarSemana.setOnClickListener {

            abrirProgramacionSemana()
        }


        // -----------------------------------------------------
        // DETALLE DEL DÍA
        // -----------------------------------------------------

        binding.btnVerDetalleDia.setOnClickListener {

            mostrarDetalleDia()
        }
    }


    // =========================================================
    // CONFIGURAR CALENDARIO
    // =========================================================

    private fun configurarCalendario() {

        val dias = listOf(
            binding.dia1,
            binding.dia2,
            binding.dia3,
            binding.dia4,
            binding.dia5,
            binding.dia6,
            binding.dia7,
            binding.dia8,
            binding.dia9,
            binding.dia10,
            binding.dia11,
            binding.dia12,
            binding.dia13,
            binding.dia14,
            binding.dia15,
            binding.dia16,
            binding.dia17,
            binding.dia18,
            binding.dia19,
            binding.dia20,
            binding.dia21,
            binding.dia22,
            binding.dia23,
            binding.dia24,
            binding.dia25,
            binding.dia26,
            binding.dia27,
            binding.dia28,
            binding.dia29,
            binding.dia30,
            binding.dia31,
            binding.dia32,
            binding.dia33,
            binding.dia34,
            binding.dia35,
            binding.dia36,
            binding.dia37,
            binding.dia38,
            binding.dia39,
            binding.dia40,
            binding.dia41,
            binding.dia42
        )

        dias.forEach { dia ->

            dia.setOnClickListener {

                if (dia.tag is Calendar) {

                    val fecha =
                        dia.tag as Calendar

                    seleccionarDia(fecha)
                }
            }
        }
    }


    // =========================================================
    // MOSTRAR MES ACTUAL
    // =========================================================

    private fun mostrarMesActual() {

        val formatoMes =
            SimpleDateFormat(
                "MMMM yyyy",
                Locale("es", "ES")
            )

        binding.txtMesActual.text =
            formatoMes.format(calendario.time)
                .replaceFirstChar {
                    it.uppercase()
                }

        limpiarDias()

        cargarDias()
    }


    // =========================================================
    // LIMPIAR DÍAS
    // =========================================================

    private fun limpiarDias() {

        val dias = obtenerTextViewsDias()

        dias.forEach { dia ->

            dia.text = ""

            dia.tag = null

            dia.backgroundTintList = null

            dia.setTextColor(
                ContextCompat.getColor(
                    this,
                    R.color.gris_texto
                )
            )

            dia.isClickable = false
        }
    }


    // =========================================================
    // CARGAR DÍAS DEL MES
    // =========================================================

    private fun cargarDias() {

        val dias = obtenerTextViewsDias()

        val primerDia =
            calendario.clone() as Calendar

        primerDia.set(
            Calendar.DAY_OF_MONTH,
            1
        )

        // Calendar: domingo=1, lunes=2...
        // Convertimos para que lunes sea la primera columna.

        val diaSemana =
            (primerDia.get(Calendar.DAY_OF_WEEK) + 5) % 7

        val cantidadDias =
            calendario.getActualMaximum(
                Calendar.DAY_OF_MONTH
            )

        for (diaNumero in 1..cantidadDias) {

            val posicion =
                diaSemana + diaNumero - 1

            if (posicion >= dias.size) {
                break
            }

            val vista =
                dias[posicion]

            val fecha =
                calendario.clone() as Calendar

            fecha.set(
                Calendar.DAY_OF_MONTH,
                diaNumero
            )

            vista.text =
                diaNumero.toString()

            vista.tag = fecha

            vista.isClickable = true

            configurarEstadoDia(
                vista,
                fecha
            )
        }
    }


    // =========================================================
    // ESTADO DEL DÍA
    // =========================================================

    private fun configurarEstadoDia(
        vista: TextView,
        fecha: Calendar
    ) {
        val hoy = Calendar.getInstance()
        val esHoy =
            fecha.get(Calendar.YEAR) == hoy.get(Calendar.YEAR) &&
                    fecha.get(Calendar.MONTH) == hoy.get(Calendar.MONTH) &&
                    fecha.get(Calendar.DAY_OF_MONTH) == hoy.get(Calendar.DAY_OF_MONTH)

        val esSeleccionado = diaSeleccionado != null &&
                fecha.get(Calendar.YEAR) == diaSeleccionado!!.get(Calendar.YEAR) &&
                fecha.get(Calendar.MONTH) == diaSeleccionado!!.get(Calendar.MONTH) &&
                fecha.get(Calendar.DAY_OF_MONTH) == diaSeleccionado!!.get(Calendar.DAY_OF_MONTH)

        if (esSeleccionado || esHoy) {
            vista.setBackgroundResource(R.drawable.bg_estado_activo)
            vista.setTextColor(ContextCompat.getColor(this, R.color.blanco))
        } else {
            vista.background = null
            vista.setTextColor(ContextCompat.getColor(this, R.color.negro_principal))
        }
    }


    // =========================================================
    // SELECCIONAR DÍA
    // =========================================================

    private fun seleccionarDia(
        fecha: Calendar
    ) {

        diaSeleccionado =
            fecha.clone() as Calendar

        actualizarSeleccionVisual()

        mostrarInformacionDia()
    }


    // =========================================================
    // ACTUALIZAR SELECCIÓN VISUAL
    // =========================================================

    private fun actualizarSeleccionVisual() {

        val dias =
            obtenerTextViewsDias()

        dias.forEach { vista ->

            val fecha =
                vista.tag as? Calendar
                    ?: return@forEach

            configurarEstadoDia(vista, fecha)
        }
    }


    // =========================================================
    // INFORMACIÓN DEL DÍA
    // =========================================================

    private fun mostrarInformacionDia() {

        val fecha =
            diaSeleccionado ?: return

        val formato =
            SimpleDateFormat(
                "EEEE, dd 'de' MMMM 'de' yyyy",
                Locale("es", "ES")
            )

        val textoFecha =
            formato.format(fecha.time)
                .replaceFirstChar {
                    it.uppercase()
                }

        binding.txtTituloDia.text =
            textoFecha

        binding.txtFechaSeleccionada.text =
            textoFecha

        // -----------------------------------------------------
        // POR AHORA SE MUESTRA EL ESTADO GUARDADO LOCALMENTE
        // -----------------------------------------------------

        binding.txtPlatoManana.text =
            "Mañana: Sin plato programado"

        binding.txtPlatoTarde.text =
            "Tarde: Sin plato programado"
    }


    // =========================================================
    // SELECCIONAR HISTORIAL
    // =========================================================

    private fun seleccionarHistorial() {

        binding.btnHistorial.backgroundTintList =
            ColorStateList.valueOf(
                ContextCompat.getColor(
                    this,
                    R.color.amarillo_principal
                )
            )

        binding.btnHistorial.setTextColor(
            ContextCompat.getColor(
                this,
                R.color.blanco
            )
        )


        binding.btnProgramar.backgroundTintList =
            ColorStateList.valueOf(
                ContextCompat.getColor(
                    this,
                    R.color.gris_claro
                )
            )

        binding.btnProgramar.setTextColor(
            ContextCompat.getColor(
                this,
                R.color.negro_principal
            )
        )
    }


    // =========================================================
    // SELECCIONAR PROGRAMAR
    // =========================================================

    private fun seleccionarProgramar() {

        binding.btnProgramar.backgroundTintList =
            ColorStateList.valueOf(
                ContextCompat.getColor(
                    this,
                    R.color.amarillo_principal
                )
            )

        binding.btnProgramar.setTextColor(
            ContextCompat.getColor(
                this,
                R.color.blanco
            )
        )


        binding.btnHistorial.backgroundTintList =
            ColorStateList.valueOf(
                ContextCompat.getColor(
                    this,
                    R.color.gris_claro
                )
            )

        binding.btnHistorial.setTextColor(
            ContextCompat.getColor(
                this,
                R.color.negro_principal
            )
        )
    }


    // =========================================================
    // PROGRAMAR SEMANA
    // =========================================================

    private fun abrirProgramacionSemana() {

        seleccionarProgramar()

        val intent = Intent(this, ProgramacionSemanalActivity::class.java)
        startActivity(intent)
    }


    // =========================================================
    // VER DETALLE
    // =========================================================

    private fun mostrarDetalleDia() {

        if (diaSeleccionado == null) {
            diaSeleccionado = Calendar.getInstance()
            actualizarSeleccionVisual()
        }

        val fecha = diaSeleccionado!!
        val formato = SimpleDateFormat("EEEE, dd 'de' MMMM 'de' yyyy", Locale("es", "ES"))
        val textoFecha = formato.format(fecha.time).replaceFirstChar { it.uppercase() }

        val intent = Intent(this, DetallePreparacionActivity::class.java).apply {
            putExtra("fecha", textoFecha)
            putExtra("plato", binding.txtPlatoManana.text.toString())
            putExtra("jornada", "Mañana y Tarde")
            putExtra("ninos", "${AsistenciaManager.obtenerManana(this@CalendarioMenuActivity)} niños")
        }
        startActivity(intent)
    }


    // =========================================================
    // OBTENER TEXTVIEWS DEL CALENDARIO
    // =========================================================

    private fun obtenerTextViewsDias(): List<TextView> {

        return listOf(
            binding.dia1,
            binding.dia2,
            binding.dia3,
            binding.dia4,
            binding.dia5,
            binding.dia6,
            binding.dia7,
            binding.dia8,
            binding.dia9,
            binding.dia10,
            binding.dia11,
            binding.dia12,
            binding.dia13,
            binding.dia14,
            binding.dia15,
            binding.dia16,
            binding.dia17,
            binding.dia18,
            binding.dia19,
            binding.dia20,
            binding.dia21,
            binding.dia22,
            binding.dia23,
            binding.dia24,
            binding.dia25,
            binding.dia26,
            binding.dia27,
            binding.dia28,
            binding.dia29,
            binding.dia30,
            binding.dia31,
            binding.dia32,
            binding.dia33,
            binding.dia34,
            binding.dia35,
            binding.dia36,
            binding.dia37,
            binding.dia38,
            binding.dia39,
            binding.dia40,
            binding.dia41,
            binding.dia42
        )
    }
}