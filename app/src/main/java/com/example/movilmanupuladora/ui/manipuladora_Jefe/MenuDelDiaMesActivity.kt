package com.example.movilmanupuladora.ui.manipuladora_Jefe

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.movilmanupuladora.R
import com.example.movilmanupuladora.data.model.MenuPaeCatalog
import com.example.movilmanupuladora.databinding.ActivityMenuDelDiaMesBinding
import com.example.movilmanupuladora.databinding.DialogDetallePlatoBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class MenuDelDiaMesActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMenuDelDiaMesBinding

    private var mesActual: Calendar = Calendar.getInstance()
    private var diaSeleccionado: Calendar = Calendar.getInstance()

    data class DetallePlatoDia(
        val nombre: String,
        val seccion: String,
        val componente: String,
        val porciones: String,
        val imagen: Int
    )

    private var platoActualDesayuno: DetallePlatoDia? = null
    private var platoActualAlmuerzo: DetallePlatoDia? = null
    private var platoActualMerienda: DetallePlatoDia? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityMenuDelDiaMesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Obtener fecha inicial pasada por Intent si existe
        val milis = intent.getLongExtra("fecha_milisegundos", System.currentTimeMillis())
        diaSeleccionado.timeInMillis = milis
        mesActual.timeInMillis = milis

        configurarEventos()
        generarGridMes()
        actualizarDetallePlatos()
    }

    private fun configurarEventos() {
        binding.btnVolver.setOnClickListener {
            finish()
        }

        binding.btnMesAnterior.setOnClickListener {
            mesActual.add(Calendar.MONTH, -1)
            generarGridMes()
        }

        binding.btnMesSiguiente.setOnClickListener {
            mesActual.add(Calendar.MONTH, 1)
            generarGridMes()
        }

        // Clics en el icono de ojo o en la tarjeta para ver detalle del plato
        binding.btnVerDetalleDesayuno.setOnClickListener { platoActualDesayuno?.let { mostrarModalDetalle(it) } }
        binding.cardDetalleDesayuno.setOnClickListener { platoActualDesayuno?.let { mostrarModalDetalle(it) } }

        binding.btnVerDetalleAlmuerzo.setOnClickListener { platoActualAlmuerzo?.let { mostrarModalDetalle(it) } }
        binding.cardDetalleAlmuerzo.setOnClickListener { platoActualAlmuerzo?.let { mostrarModalDetalle(it) } }

        binding.btnVerDetalleMerienda.setOnClickListener { platoActualMerienda?.let { mostrarModalDetalle(it) } }
        binding.cardDetalleMerienda.setOnClickListener { platoActualMerienda?.let { mostrarModalDetalle(it) } }
    }

    // =========================================================
    // GENERAR CUADRÍCULA DE DÍAS DEL MES (7 COLUMNAS)
    // =========================================================

    private fun generarGridMes() {
        binding.contenedorGridCalendario.removeAllViews()

        // Actualizar título del mes (ej. "Octubre 2026")
        val formatoMes = SimpleDateFormat("MMMM yyyy", Locale.forLanguageTag("es-CO"))
        val mesAnioTexto = formatoMes.format(mesActual.time).replaceFirstChar {
            if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
        }
        binding.txtMesAnio.text = mesAnioTexto

        val calGrid = mesActual.clone() as Calendar
        calGrid.set(Calendar.DAY_OF_MONTH, 1)

        // Encontrar el día de inicio respecto al Lunes (1 = Lun, 2 = Mar, ..., 7 = Dom)
        var primerDiaSemana = calGrid.get(Calendar.DAY_OF_WEEK) // Domingo es 1, Lunes es 2 en Java Calendar
        val offsetLunes = when (primerDiaSemana) {
            Calendar.MONDAY -> 0
            Calendar.TUESDAY -> 1
            Calendar.WEDNESDAY -> 2
            Calendar.THURSDAY -> 3
            Calendar.FRIDAY -> 4
            Calendar.SATURDAY -> 5
            Calendar.SUNDAY -> 6
            else -> 0
        }

        val diasEnMes = calGrid.getActualMaximum(Calendar.DAY_OF_MONTH)

        // Días del mes anterior para rellenar
        val calMesAnterior = calGrid.clone() as Calendar
        calMesAnterior.add(Calendar.MONTH, -1)
        val maxDiasMesAnterior = calMesAnterior.getActualMaximum(Calendar.DAY_OF_MONTH)

        var diaMesActual = 1
        var diaMesSiguiente = 1
        val totalCeldas = 35 // 5 semanas x 7 días (o 42 si requiere 6 semanas)
        val totalSemanas = if (offsetLunes + diasEnMes > 35) 6 else 5

        for (semana in 0 until totalSemanas) {
            val fila = LinearLayout(this).apply {
                layoutWidth(LinearLayout.LayoutParams.MATCH_PARENT)
                layoutHeight(LinearLayout.LayoutParams.WRAP_CONTENT)
                orientation = LinearLayout.HORIZONTAL
                weightSum = 7f
                setPadding(0, 4, 0, 4)
            }

            for (columna in 0 until 7) {
                val indiceTotal = semana * 7 + columna

                val cellContainer = FrameLayout(this).apply {
                    val p = LinearLayout.LayoutParams(0, (40 * resources.displayMetrics.density).toInt(), 1f)
                    layoutParams = p
                }

                val txtDia = TextView(this).apply {
                    val p = FrameLayout.LayoutParams(
                        (34 * resources.displayMetrics.density).toInt(),
                        (34 * resources.displayMetrics.density).toInt()
                    ).apply { gravity = Gravity.CENTER }
                    layoutParams = p
                    gravity = Gravity.CENTER
                    textSize = 13f
                }

                when {
                    indiceTotal < offsetLunes -> {
                        // Días del mes anterior
                        val numDia = maxDiasMesAnterior - offsetLunes + indiceTotal + 1
                        txtDia.text = numDia.toString()
                        txtDia.setTextColor(Color.parseColor("#CBD5E1"))
                    }
                    diaMesActual <= diasEnMes -> {
                        // Días del mes actual
                        val diaLoop = diaMesActual
                        txtDia.text = diaLoop.toString()

                        val calDiaActual = mesActual.clone() as Calendar
                        calDiaActual.set(Calendar.DAY_OF_MONTH, diaLoop)

                        val isSelected = esMismoDia(calDiaActual, diaSeleccionado)

                        if (isSelected) {
                            txtDia.setBackgroundResource(R.drawable.circulo_amarillo_badge)
                            txtDia.setTextColor(Color.parseColor("#1E293B"))
                            txtDia.typeface = android.graphics.Typeface.DEFAULT_BOLD
                        } else {
                            txtDia.setBackgroundColor(Color.TRANSPARENT)
                            txtDia.setTextColor(Color.parseColor("#1E293B"))
                        }

                        cellContainer.isClickable = true
                        cellContainer.isFocusable = true
                        cellContainer.setOnClickListener {
                            diaSeleccionado = calDiaActual
                            generarGridMes()
                            actualizarDetallePlatos()
                        }

                        diaMesActual++
                    }
                    else -> {
                        // Días del mes siguiente
                        txtDia.text = diaMesSiguiente.toString()
                        txtDia.setTextColor(Color.parseColor("#CBD5E1"))
                        diaMesSiguiente++
                    }
                }

                cellContainer.addView(txtDia)
                fila.addView(cellContainer)
            }

            binding.contenedorGridCalendario.addView(fila)
        }
    }

    private fun esMismoDia(c1: Calendar, c2: Calendar): Boolean {
        return c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR) &&
                c1.get(Calendar.DAY_OF_YEAR) == c2.get(Calendar.DAY_OF_YEAR)
    }

    // =========================================================
    // ACTUALIZAR DETALLE DE PLATOS (DESAYUNO, ALMUERZO, MERIENDA)
    // =========================================================

    private fun actualizarDetallePlatos() {
        val menuDia = MenuPaeCatalog.obtenerMenuDiaCompleto(diaSeleccionado)

        platoActualDesayuno = DetallePlatoDia(
            nombre = menuDia.desayuno.nombre,
            seccion = "Desayuno",
            componente = menuDia.desayuno.componente,
            porciones = "42",
            imagen = menuDia.desayuno.imagenRes
        )
        platoActualAlmuerzo = DetallePlatoDia(
            nombre = menuDia.almuerzo.nombre,
            seccion = "Almuerzo",
            componente = menuDia.almuerzo.componente,
            porciones = "60",
            imagen = menuDia.almuerzo.imagenRes
        )
        platoActualMerienda = DetallePlatoDia(
            nombre = menuDia.merienda.nombre,
            seccion = "Merienda",
            componente = menuDia.merienda.componente,
            porciones = "30",
            imagen = menuDia.merienda.imagenRes
        )

        binding.imgDetalleDesayuno.setImageResource(menuDia.desayuno.imagenRes)
        binding.txtDetalleNombreDesayuno.text = menuDia.desayuno.nombre

        binding.imgDetalleAlmuerzo.setImageResource(menuDia.almuerzo.imagenRes)
        binding.txtDetalleNombreAlmuerzo.text = menuDia.almuerzo.nombre

        binding.imgDetalleMerienda.setImageResource(menuDia.merienda.imagenRes)
        binding.txtDetalleNombreMerienda.text = menuDia.merienda.nombre
    }

    private fun mostrarModalDetalle(detalle: DetallePlatoDia) {
        val dialogBinding = DialogDetallePlatoBinding.inflate(layoutInflater)
        dialogBinding.tvTituloPlatoModal.text = detalle.nombre
        dialogBinding.tvSeccionModal.text = detalle.seccion
        dialogBinding.tvComponenteModal.text = "Componente: ${detalle.componente}"
        dialogBinding.tvPorcionModal.text = "100g aprox. por ración"
        dialogBinding.tvTotalPrepararModal.text = "${detalle.porciones} porciones"
        dialogBinding.tvBadgeEstadoModal.text = "Programado"
        dialogBinding.imgPlatoModal.setImageResource(detalle.imagen)

        val dialog = MaterialAlertDialogBuilder(this)
            .setView(dialogBinding.root)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        dialogBinding.btnCerrarModal.setOnClickListener {
            dialog.dismiss()
        }

        dialogBinding.btnVerComponentesModal.setOnClickListener {
            dialog.dismiss()
            val intent = Intent(this, IniciarPreparacionActivity::class.java).apply {
                putExtra("nombre_menu", detalle.nombre)
                putExtra("jornada", detalle.seccion)
            }
            startActivity(intent)
        }

        dialogBinding.btnOtroAleatorioModal.text = "Cerrar"
        dialogBinding.btnOtroAleatorioModal.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun LinearLayout.layoutWidth(width: Int) {
        val p = layoutParams ?: LinearLayout.LayoutParams(width, LinearLayout.LayoutParams.WRAP_CONTENT)
        p.width = width
        layoutParams = p
    }

    private fun LinearLayout.layoutHeight(height: Int) {
        val p = layoutParams ?: LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, height)
        p.height = height
        layoutParams = p
    }
}
