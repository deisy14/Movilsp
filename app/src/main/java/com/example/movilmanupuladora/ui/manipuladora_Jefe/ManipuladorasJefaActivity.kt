package com.example.movilmanupuladora.ui.manipuladora_Jefe

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.movilmanupuladora.R
import com.example.movilmanupuladora.data.api.RetrofitClient
import com.example.movilmanupuladora.data.model.Turno
import com.example.movilmanupuladora.databinding.ActivityManipuladorasJefaBinding
import com.example.movilmanupuladora.utils.NavigationHelperJefa
import com.example.movilmanupuladora.utils.SessionManager
import com.google.android.material.card.MaterialCardView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class ManipuladorasJefaActivity : AppCompatActivity() {

    private lateinit var binding: ActivityManipuladorasJefaBinding
    private lateinit var sessionManager: SessionManager

    // Lista exclusiva de manipuladoras de la sede de la jefa
    private var manipuladorasSede: List<ManipuladorasManager.Manipuladora> = emptyList()
    private var manipuladoraSeleccionada: ManipuladorasManager.Manipuladora? = null // null = Todas las manipuladoras

    // Control de edición: al asignar o tener turno ya guardado, se bloquea y aparece "Editar turno"
    private var esModoEdicion: Boolean = true

    // Estado del formulario
    private var jornadaSeleccionada: String = "Ambas"

    private var calendarInicio: Calendar? = null
    private var calendarFin: Calendar? = null

    // Días de la semana activos (1=Lun, 2=Mar, 3=Mié, 4=Jue, 5=Vie, 6=Sáb, 7=Dom)
    private val diasActivos = mutableSetOf(1, 2, 3, 4, 5)

    private var horaEntrada: Int? = null
    private var minutoEntrada: Int? = null
    private var horaSalida: Int? = null
    private var minutoSalida: Int? = null

    private val formatoFechaDisplay = SimpleDateFormat("dd MMM yyyy", Locale("es", "CO"))
    private val formatoFechaIso = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Edge-to-Edge: fondo se dibuja detrás de la barra de estado
        androidx.core.view.WindowCompat.setDecorFitsSystemWindows(window, false)

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            window.attributes.layoutInDisplayCutoutMode =
                android.view.WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }

        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.navigationBarColor = android.graphics.Color.TRANSPARENT
        androidx.core.view.WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars = true

        binding = ActivityManipuladorasJefaBinding.inflate(layoutInflater)
        setContentView(binding.root)

        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(binding.headerTurnos) { view, insets ->
            val statusBarHeight = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars()).top
            view.setPadding(
                view.paddingLeft,
                statusBarHeight + (10 * resources.displayMetrics.density).toInt(),
                view.paddingRight,
                view.paddingBottom
            )
            insets
        }

        sessionManager = SessionManager(this)

        configurarEncabezado()
        configurarSelectorManipuladora()
        configurarJornada()
        configurarPeriodo()
        configurarDias()
        configurarHorarios()
        configurarBotonesAccion()
        configurarNavegacionInferior()

        actualizarTextosFechas()
        actualizarTextoHoraEntrada()
        actualizarTextoHoraSalida()
        calcularHorasProgramadas()
        actualizarUIJornada()
        actualizarUIDias()

        cargarManipuladorasDeSede()
        sincronizarManipuladoras()
    }

    private fun configurarEncabezado() {
        // En la pestaña principal de Turnos no se muestra botón de volver atrás
    }

    private fun sincronizarManipuladoras() {
        val idSede = sessionManager.getSedeId().takeIf { it > 0 }
        val sedeNombre = sessionManager.getSedeNombre()

        lifecycleScope.launch {
            try {
                ManipuladorasManager.cargarDesdeBackend(this@ManipuladorasJefaActivity, idSede, sedeNombre)
            } catch (_: Exception) { }
            cargarManipuladorasDeSede()
            verificarEstadoTurnoDeSeleccionada()
        }
    }

    private fun cargarManipuladorasDeSede() {
        val idSede = sessionManager.getSedeId().takeIf { it > 0 }
        val sedeNombre = sessionManager.getSedeNombre()
        manipuladorasSede = ManipuladorasManager.obtenerManipuladorasSede(idSede, sedeNombre, this)
    }

    // =========================================================================
    // 1. SELECTOR DE MANIPULADORAS (MODAL CON DISEÑO EXACTO A LA IMAGEN)
    // =========================================================================

    private fun configurarSelectorManipuladora() {
        binding.cardSelectorManipuladora.setOnClickListener {
            mostrarDialogoSeleccionarManipuladora()
        }
    }

    private fun mostrarDialogoSeleccionarManipuladora() {
        cargarManipuladorasDeSede()

        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_seleccionar_manipuladora, null)
        val contenedorOpciones = dialogView.findViewById<LinearLayout>(R.id.contenedorOpciones)
        val btnAceptar = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnAceptarDialogo)
        val btnCancelar = dialogView.findViewById<TextView>(R.id.btnCancelarDialogo)

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        // Lista de opciones: 0 = "Todas las manipuladoras", 1..N = manipuladoras de la sede
        data class ItemOpcion(val id: Int, val nombre: String, val manipuladora: ManipuladorasManager.Manipuladora?)
        val items = mutableListOf<ItemOpcion>()
        items.add(ItemOpcion(-1, "Todas las manipuladoras", null))
        manipuladorasSede.forEach { m ->
            items.add(ItemOpcion(m.id, m.nombre, m))
        }

        var idSeleccionadoTemp = manipuladoraSeleccionada?.id ?: -1

        val vistasOpciones = mutableListOf<View>()

        fun refrescarVistas() {
            val density = resources.displayMetrics.density
            val strokeWidthPx = (1.5f * density).toInt()

            items.forEachIndexed { index, item ->
                val itemView = vistasOpciones.getOrNull(index) ?: return@forEachIndexed
                val card = itemView.findViewById<MaterialCardView>(R.id.cardOpcion)
                val text = itemView.findViewById<TextView>(R.id.txtNombreOpcion)
                val radio = itemView.findViewById<ImageView>(R.id.imgRadioOpcion)

                val esSeleccionado = (item.id == idSeleccionadoTemp)
                if (esSeleccionado) {
                    card.setCardBackgroundColor(Color.parseColor("#FFFDF0"))
                    card.strokeColor = Color.parseColor("#F4B41F")
                    card.strokeWidth = strokeWidthPx
                    text.setTextColor(Color.parseColor("#1E293B"))
                    text.typeface = Typeface.DEFAULT_BOLD
                    radio.setImageResource(R.drawable.ic_radio_selected)
                } else {
                    card.setCardBackgroundColor(Color.parseColor("#FFFFFF"))
                    card.strokeWidth = 0
                    text.setTextColor(Color.parseColor("#1E293B"))
                    text.typeface = Typeface.DEFAULT
                    radio.setImageResource(R.drawable.ic_radio_unselected)
                }
            }
        }

        items.forEach { item ->
            val itemView = LayoutInflater.from(this).inflate(R.layout.item_dialog_manipuladora, contenedorOpciones, false)
            val text = itemView.findViewById<TextView>(R.id.txtNombreOpcion)
            val card = itemView.findViewById<MaterialCardView>(R.id.cardOpcion)

            text.text = item.nombre

            card.setOnClickListener {
                idSeleccionadoTemp = item.id
                refrescarVistas()
            }

            vistasOpciones.add(itemView)
            contenedorOpciones.addView(itemView)
        }

        refrescarVistas()

        btnAceptar.setOnClickListener {
            val seleccionado = items.firstOrNull { it.id == idSeleccionadoTemp }
            if (seleccionado != null) {
                if (seleccionado.id == -1) {
                    manipuladoraSeleccionada = null
                    binding.txtSeleccionarManipuladora.text = "Todas las manipuladoras"
                } else {
                    manipuladoraSeleccionada = seleccionado.manipuladora
                    binding.txtSeleccionarManipuladora.text = seleccionado.nombre
                }
                binding.txtSeleccionarManipuladora.setTextColor(Color.parseColor("#1E293B"))
                verificarEstadoTurnoDeSeleccionada()
            }
            dialog.dismiss()
        }

        btnCancelar.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    // =========================================================================
    // CONTROL DE MODO LECTURA / EDICIÓN Y PERSISTENCIA DE TURNOS
    // =========================================================================

    private fun verificarEstadoTurnoDeSeleccionada() {
        val targetId = manipuladoraSeleccionada?.id ?: -1
        val prefs = getSharedPreferences("TURNO_PAE", MODE_PRIVATE)
        val tieneTurno = prefs.getBoolean("has_turno_$targetId", false)

        if (tieneTurno) {
            // Ya tiene turno asignado -> Cargar datos y bloquear edición
            jornadaSeleccionada = prefs.getString("turno_jornada_$targetId", "Ambas") ?: "Ambas"

            val fIni = prefs.getString("turno_fecha_ini_$targetId", null)
            val fFin = prefs.getString("turno_fecha_fin_$targetId", null)
            try {
                if (!fIni.isNullOrBlank()) {
                    formatoFechaIso.parse(fIni)?.let {
                        calendarInicio = Calendar.getInstance().apply { time = it }
                    }
                } else {
                    calendarInicio = null
                }
                if (!fFin.isNullOrBlank()) {
                    formatoFechaIso.parse(fFin)?.let {
                        calendarFin = Calendar.getInstance().apply { time = it }
                    }
                } else {
                    calendarFin = null
                }
            } catch (_: Exception) { }

            val diasStr = prefs.getString("turno_dias_$targetId", "1,2,3,4,5") ?: "1,2,3,4,5"
            diasActivos.clear()
            diasActivos.addAll(diasStr.split(",").mapNotNull { it.trim().toIntOrNull() }.ifEmpty { listOf(1, 2, 3, 4, 5) })

            val hEntradaStr = prefs.getString("turno_hora_ini_$targetId", null)
            if (!hEntradaStr.isNullOrBlank()) {
                val partesEntrada = hEntradaStr.split(":")
                horaEntrada = partesEntrada.getOrNull(0)?.toIntOrNull()
                minutoEntrada = partesEntrada.getOrNull(1)?.toIntOrNull()
            } else {
                horaEntrada = null
                minutoEntrada = null
            }

            val hSalidaStr = prefs.getString("turno_hora_fin_$targetId", null)
            if (!hSalidaStr.isNullOrBlank()) {
                val partesSalida = hSalidaStr.split(":")
                horaSalida = partesSalida.getOrNull(0)?.toIntOrNull()
                minutoSalida = partesSalida.getOrNull(1)?.toIntOrNull()
            } else {
                horaSalida = null
                minutoSalida = null
            }

            actualizarUIJornada()
            actualizarTextosFechas()
            actualizarUIDias()
            actualizarTextoHoraEntrada()
            actualizarTextoHoraSalida()
            calcularHorasProgramadas()

            establecerModoEdicion(editable = false)
        } else {
            // No tiene turno asignado -> Limpiar campos sin horas ni fechas quemadas, habilitados para asignar
            calendarInicio = null
            calendarFin = null
            horaEntrada = null
            minutoEntrada = null
            horaSalida = null
            minutoSalida = null
            jornadaSeleccionada = "Ambas"
            diasActivos.clear()
            diasActivos.addAll(listOf(1, 2, 3, 4, 5))

            actualizarUIJornada()
            actualizarTextosFechas()
            actualizarUIDias()
            actualizarTextoHoraEntrada()
            actualizarTextoHoraSalida()
            calcularHorasProgramadas()

            establecerModoEdicion(editable = true)
        }
    }

    private fun establecerModoEdicion(editable: Boolean) {
        esModoEdicion = editable

        if (!editable) {
            // BLOQUEADO: No se puede editar, botón "Editar turno" visible abajo
            binding.btnAsignarTurnos.visibility = View.GONE
            binding.btnEditarTurno.visibility = View.VISIBLE

            aplicarBloqueoVisualCampos(bloqueado = true)
        } else {
            // EDITABLE: Campos habilitados, botón "Asignar turnos" visible abajo
            binding.btnAsignarTurnos.visibility = View.VISIBLE
            binding.btnAsignarTurnos.text = "Asignar turnos"
            binding.btnEditarTurno.visibility = View.GONE

            aplicarBloqueoVisualCampos(bloqueado = false)
        }
    }

    private fun aplicarBloqueoVisualCampos(bloqueado: Boolean) {
        val alphaVal = if (bloqueado) 0.82f else 1.0f
        binding.cardJornadaManana.alpha = alphaVal
        binding.cardJornadaTarde.alpha = alphaVal
        binding.cardJornadaAmbas.alpha = alphaVal
        binding.cardFechaInicio.alpha = alphaVal
        binding.cardFechaFin.alpha = alphaVal
        binding.btnDiaLun.alpha = alphaVal
        binding.btnDiaMar.alpha = alphaVal
        binding.btnDiaMie.alpha = alphaVal
        binding.btnDiaJue.alpha = alphaVal
        binding.btnDiaVie.alpha = alphaVal
        binding.btnDiaSab.alpha = alphaVal
        binding.btnDiaDom.alpha = alphaVal
        binding.cardHoraEntrada.alpha = alphaVal
        binding.cardHoraSalida.alpha = alphaVal
    }

    private fun validarPermisoEdicion(): Boolean {
        if (!esModoEdicion) {
            Toast.makeText(this, "Toca 'Editar turno' para modificar los campos", Toast.LENGTH_SHORT).show()
            return false
        }
        return true
    }

    // =========================================================================
    // 2. ELEGIR JORNADA
    // =========================================================================

    private fun configurarJornada() {
        binding.cardJornadaManana.setOnClickListener {
            if (validarPermisoEdicion()) seleccionarJornada("Mañana")
        }
        binding.cardJornadaTarde.setOnClickListener {
            if (validarPermisoEdicion()) seleccionarJornada("Tarde")
        }
        binding.cardJornadaAmbas.setOnClickListener {
            if (validarPermisoEdicion()) seleccionarJornada("Ambas")
        }
    }

    private fun seleccionarJornada(jornada: String) {
        jornadaSeleccionada = jornada
        actualizarUIJornada()
    }

    private fun actualizarUIJornada() {
        val strokeWidthPx = (1 * resources.displayMetrics.density).toInt()

        fun aplicarEstilo(card: MaterialCardView, text: TextView, check: View, seleccionada: Boolean) {
            if (seleccionada) {
                card.setCardBackgroundColor(Color.parseColor("#F4B41F"))
                card.strokeWidth = 0
                check.visibility = View.VISIBLE
                text.setTextColor(Color.parseColor("#1E293B"))
            } else {
                card.setCardBackgroundColor(Color.parseColor("#FFFFFF"))
                card.strokeColor = Color.parseColor("#CBD5E1")
                card.strokeWidth = strokeWidthPx
                check.visibility = View.GONE
                text.setTextColor(Color.parseColor("#1E293B"))
            }
        }

        aplicarEstilo(binding.cardJornadaManana, binding.txtJornadaManana, binding.checkJornadaManana, jornadaSeleccionada == "Mañana")
        aplicarEstilo(binding.cardJornadaTarde, binding.txtJornadaTarde, binding.checkJornadaTarde, jornadaSeleccionada == "Tarde")
        aplicarEstilo(binding.cardJornadaAmbas, binding.txtJornadaAmbas, binding.checkJornadaAmbas, jornadaSeleccionada == "Ambas")
    }

    // =========================================================================
    // 3. PERÍODO DE TRABAJO Y DÍAS
    // =========================================================================

    private fun configurarPeriodo() {
        binding.cardFechaInicio.setOnClickListener {
            if (!validarPermisoEdicion()) return@setOnClickListener

            val baseCal = calendarInicio ?: Calendar.getInstance()

            DatePickerDialog(
                this,
                { _, year, month, dayOfMonth ->
                    calendarInicio = Calendar.getInstance().apply {
                        set(Calendar.YEAR, year)
                        set(Calendar.MONTH, month)
                        set(Calendar.DAY_OF_MONTH, dayOfMonth)
                    }
                    if (calendarFin != null && calendarFin!!.before(calendarInicio)) {
                        calendarFin = Calendar.getInstance().apply { time = calendarInicio!!.time }
                    }
                    actualizarTextosFechas()
                },
                baseCal.get(Calendar.YEAR),
                baseCal.get(Calendar.MONTH),
                baseCal.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        binding.cardFechaFin.setOnClickListener {
            if (!validarPermisoEdicion()) return@setOnClickListener

            val baseCal = calendarFin ?: calendarInicio ?: Calendar.getInstance()

            DatePickerDialog(
                this,
                { _, year, month, dayOfMonth ->
                    val temp = Calendar.getInstance().apply {
                        set(Calendar.YEAR, year)
                        set(Calendar.MONTH, month)
                        set(Calendar.DAY_OF_MONTH, dayOfMonth)
                    }
                    val inicio = calendarInicio
                    if (inicio != null && temp.before(inicio)) {
                        Toast.makeText(this, "La fecha de finalización no puede ser anterior a la de inicio", Toast.LENGTH_SHORT).show()
                    } else {
                        calendarFin = temp
                        actualizarTextosFechas()
                    }
                },
                baseCal.get(Calendar.YEAR),
                baseCal.get(Calendar.MONTH),
                baseCal.get(Calendar.DAY_OF_MONTH)
            ).show()
        }
    }

    private fun actualizarTextosFechas() {
        val fIni = calendarInicio
        if (fIni != null) {
            binding.txtFechaInicio.text = formatoFechaDisplay.format(fIni.time)
            binding.txtFechaInicio.setTextColor(Color.parseColor("#1E293B"))
            binding.txtFechaInicio.typeface = Typeface.DEFAULT_BOLD
        } else {
            binding.txtFechaInicio.text = "Seleccionar fecha"
            binding.txtFechaInicio.setTextColor(Color.parseColor("#64748B"))
            binding.txtFechaInicio.typeface = Typeface.DEFAULT
        }

        val fFin = calendarFin
        if (fFin != null) {
            binding.txtFechaFin.text = formatoFechaDisplay.format(fFin.time)
            binding.txtFechaFin.setTextColor(Color.parseColor("#1E293B"))
            binding.txtFechaFin.typeface = Typeface.DEFAULT_BOLD
        } else {
            binding.txtFechaFin.text = "Seleccionar fecha"
            binding.txtFechaFin.setTextColor(Color.parseColor("#64748B"))
            binding.txtFechaFin.typeface = Typeface.DEFAULT
        }
    }

    private fun configurarDias() {
        val mapaDias = mapOf(
            1 to binding.btnDiaLun,
            2 to binding.btnDiaMar,
            3 to binding.btnDiaMie,
            4 to binding.btnDiaJue,
            5 to binding.btnDiaVie,
            6 to binding.btnDiaSab,
            7 to binding.btnDiaDom
        )

        mapaDias.forEach { (dia, card) ->
            card.setOnClickListener {
                if (!validarPermisoEdicion()) return@setOnClickListener

                if (diasActivos.contains(dia)) {
                    if (diasActivos.size > 1) {
                        diasActivos.remove(dia)
                    } else {
                        Toast.makeText(this, "Debes seleccionar al menos un día", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    diasActivos.add(dia)
                }
                actualizarUIDias()
            }
        }
    }

    private fun actualizarUIDias() {
        val mapaDias = mapOf(
            1 to Pair(binding.btnDiaLun, binding.txtDiaLun),
            2 to Pair(binding.btnDiaMar, binding.txtDiaMar),
            3 to Pair(binding.btnDiaMie, binding.txtDiaMie),
            4 to Pair(binding.btnDiaJue, binding.txtDiaJue),
            5 to Pair(binding.btnDiaVie, binding.txtDiaVie),
            6 to Pair(binding.btnDiaSab, binding.txtDiaSab),
            7 to Pair(binding.btnDiaDom, binding.txtDiaDom)
        )

        val strokeWidthPx = (1 * resources.displayMetrics.density).toInt()

        mapaDias.forEach { (dia, par) ->
            val (card, text) = par
            val activo = diasActivos.contains(dia)
            if (activo) {
                card.setCardBackgroundColor(Color.parseColor("#F4B41F"))
                card.strokeWidth = 0
                text.setTextColor(Color.parseColor("#1E293B"))
                text.typeface = Typeface.DEFAULT_BOLD
            } else {
                card.setCardBackgroundColor(Color.parseColor("#FFFFFF"))
                card.strokeColor = Color.parseColor("#CBD5E1")
                card.strokeWidth = strokeWidthPx
                text.setTextColor(Color.parseColor("#64748B"))
                text.typeface = Typeface.DEFAULT
            }
        }
    }

    // =========================================================================
    // 4. HORARIO
    // =========================================================================

    private fun configurarHorarios() {
        binding.cardHoraEntrada.setOnClickListener {
            if (!validarPermisoEdicion()) return@setOnClickListener

            val h = horaEntrada ?: 6
            val m = minutoEntrada ?: 0

            TimePickerDialog(
                this,
                { _, hourOfDay, minute ->
                    horaEntrada = hourOfDay
                    minutoEntrada = minute
                    actualizarTextoHoraEntrada()
                    calcularHorasProgramadas()
                },
                h,
                m,
                false
            ).show()
        }

        binding.cardHoraSalida.setOnClickListener {
            if (!validarPermisoEdicion()) return@setOnClickListener

            val h = horaSalida ?: 12
            val m = minutoSalida ?: 0

            TimePickerDialog(
                this,
                { _, hourOfDay, minute ->
                    horaSalida = hourOfDay
                    minutoSalida = minute
                    actualizarTextoHoraSalida()
                    calcularHorasProgramadas()
                },
                h,
                m,
                false
            ).show()
        }
    }

    private fun actualizarTextoHoraEntrada() {
        val h = horaEntrada
        val m = minutoEntrada
        if (h != null && m != null) {
            binding.txtHoraEntrada.text = formatearHora(h, m)
            binding.txtHoraEntrada.setTextColor(Color.parseColor("#1E293B"))
            binding.txtHoraEntrada.typeface = Typeface.DEFAULT_BOLD
        } else {
            binding.txtHoraEntrada.text = "Seleccionar hora"
            binding.txtHoraEntrada.setTextColor(Color.parseColor("#64748B"))
            binding.txtHoraEntrada.typeface = Typeface.DEFAULT
        }
    }

    private fun actualizarTextoHoraSalida() {
        val h = horaSalida
        val m = minutoSalida
        if (h != null && m != null) {
            binding.txtHoraSalida.text = formatearHora(h, m)
            binding.txtHoraSalida.setTextColor(Color.parseColor("#1E293B"))
            binding.txtHoraSalida.typeface = Typeface.DEFAULT_BOLD
        } else {
            binding.txtHoraSalida.text = "Seleccionar hora"
            binding.txtHoraSalida.setTextColor(Color.parseColor("#64748B"))
            binding.txtHoraSalida.typeface = Typeface.DEFAULT
        }
    }

    private fun formatearHora(hora: Int, minuto: Int): String {
        val amPm = if (hora < 12) "a. m." else "p. m."
        val h = when {
            hora == 0 -> 12
            hora > 12 -> hora - 12
            else -> hora
        }
        val m = String.format(Locale.getDefault(), "%02d", minuto)
        return "$h:$m $amPm"
    }

    private fun calcularHorasProgramadas() {
        val hEnt = horaEntrada
        val mEnt = minutoEntrada
        val hSal = horaSalida
        val mSal = minutoSalida

        if (hEnt == null || mEnt == null || hSal == null || mSal == null) {
            binding.txtHorasCalculadas.text = "Selecciona la hora de entrada y salida"
            binding.txtHorasCalculadas.setTextColor(Color.parseColor("#64748B"))
            return
        }

        val minEntrada = hEnt * 60 + mEnt
        val minSalida = hSal * 60 + mSal
        val difMinutos = if (minSalida >= minEntrada) {
            minSalida - minEntrada
        } else {
            (24 * 60 - minEntrada) + minSalida
        }

        val difHoras = difMinutos / 60.0
        val textoHoras = if (difHoras % 1.0 == 0.0) {
            difHoras.toInt().toString()
        } else {
            String.format(Locale.US, "%.1f", difHoras)
        }

        binding.txtHorasCalculadas.text = "$textoHoras horas programadas por día"
        binding.txtHorasCalculadas.setTextColor(Color.parseColor("#334155"))
    }

    // =========================================================================
    // BOTONES DE ACCIÓN: ASIGNAR TURNOS / EDITAR TURNO
    // =========================================================================

    private fun configurarBotonesAccion() {
        // Al tocar "Editar turno", se desbloquean los campos y el botón vuelve a ser "Asignar turnos"
        binding.btnEditarTurno.setOnClickListener {
            establecerModoEdicion(editable = true)
            Toast.makeText(this, "Campos habilitados. Puedes modificar el horario.", Toast.LENGTH_SHORT).show()
        }

        // Al tocar "Asignar turnos", se guarda el turno y luego se bloquea la edición
        binding.btnAsignarTurnos.setOnClickListener {
            guardarYBloquearTurno()
        }
    }

    private fun guardarYBloquearTurno() {
        val calIni = calendarInicio
        val calFin = calendarFin
        if (calIni == null || calFin == null) {
            Toast.makeText(this, "Por favor selecciona el período de trabajo (fechas de inicio y fin)", Toast.LENGTH_SHORT).show()
            return
        }

        val hEnt = horaEntrada
        val mEnt = minutoEntrada
        val hSal = horaSalida
        val mSal = minutoSalida
        if (hEnt == null || mEnt == null || hSal == null || mSal == null) {
            Toast.makeText(this, "Por favor selecciona la hora de entrada y la hora de salida", Toast.LENGTH_SHORT).show()
            return
        }

        if (diasActivos.isEmpty()) {
            Toast.makeText(this, "Por favor selecciona al menos un día de trabajo", Toast.LENGTH_SHORT).show()
            return
        }

        val idSede = sessionManager.getSedeId().takeIf { it > 0 }
        val nombreDestinatario = manipuladoraSeleccionada?.nombre ?: "Todas las manipuladoras"
        val targetId = manipuladoraSeleccionada?.id ?: -1

        val diasNombres = listOf("Lun", "Mar", "Mié", "Jue", "Vie", "Sáb", "Dom")
        val diasSeleccionadosStr = diasActivos.sorted().map { diasNombres[it - 1] }.joinToString(", ")
        val diasIdsStr = diasActivos.sorted().joinToString(",")

        val horaIniStr = String.format(Locale.US, "%02d:%02d:00", hEnt, mEnt)
        val horaFinStr = String.format(Locale.US, "%02d:%02d:00", hSal, mSal)
        val fechaIniStr = formatoFechaIso.format(calIni.time)
        val fechaFinStr = formatoFechaIso.format(calFin.time)
        val horasTexto = binding.txtHorasCalculadas.text.toString()

        binding.btnAsignarTurnos.isEnabled = false
        binding.btnAsignarTurnos.text = "Guardando turno..."

        lifecycleScope.launch {
            try {
                // 1. Guardar turno en preferencias locales marcando que ya tiene turno asignado
                val prefs = getSharedPreferences("TURNO_PAE", MODE_PRIVATE)
                val editor = prefs.edit()

                editor.putBoolean("has_turno_$targetId", true)
                editor.putString("turno_jornada_$targetId", jornadaSeleccionada)
                editor.putString("turno_destinatario_$targetId", nombreDestinatario)
                editor.putString("turno_fecha_ini_$targetId", fechaIniStr)
                editor.putString("turno_fecha_fin_$targetId", fechaFinStr)
                editor.putString("turno_dias_$targetId", diasIdsStr)
                editor.putString("turno_dias_txt_$targetId", diasSeleccionadosStr)
                editor.putString("turno_hora_ini_$targetId", horaIniStr)
                editor.putString("turno_hora_fin_$targetId", horaFinStr)
                editor.putString("turno_horas_txt_$targetId", horasTexto)

                // Si se asignó a "Todas las manipuladoras", registrarlo también para cada manipuladora de la sede
                if (targetId == -1) {
                    manipuladorasSede.forEach { m ->
                        editor.putBoolean("has_turno_${m.id}", true)
                        editor.putString("turno_jornada_${m.id}", jornadaSeleccionada)
                        editor.putString("turno_destinatario_${m.id}", m.nombre)
                        editor.putString("turno_fecha_ini_${m.id}", fechaIniStr)
                        editor.putString("turno_fecha_fin_${m.id}", fechaFinStr)
                        editor.putString("turno_dias_${m.id}", diasIdsStr)
                        editor.putString("turno_dias_txt_${m.id}", diasSeleccionadosStr)
                        editor.putString("turno_hora_ini_${m.id}", horaIniStr)
                        editor.putString("turno_hora_fin_${m.id}", horaFinStr)
                        editor.putString("turno_horas_txt_${m.id}", horasTexto)
                    }
                }
                editor.apply()

                // 2. Registrar en la nube (backend API)
                val tagTurno = "PAE_TURNO: $jornadaSeleccionada · $nombreDestinatario".take(50)
                val obs = "UID:$targetId|SEDE:${idSede ?: -1}|JORNADA:$jornadaSeleccionada|FECHAS:$fechaIniStr/$fechaFinStr|DIAS:$diasIdsStr|DIASTXT:$diasSeleccionadosStr|HORAS:$horaIniStr/$horaFinStr|HORASTOT:$horasTexto|NOMBRE:$nombreDestinatario"

                withContext(Dispatchers.IO) {
                    try {
                        RetrofitClient.apiService.crearTurno(
                            Turno(
                                nombreTurno = tagTurno,
                                horaInicio = horaIniStr,
                                horaFin = horaFinStr,
                                observaciones = obs
                            )
                        )
                    } catch (_: Exception) { }
                }

                Toast.makeText(
                    this@ManipuladorasJefaActivity,
                    "¡Turno de $nombreDestinatario asignado exitosamente!",
                    Toast.LENGTH_LONG
                ).show()

                // 3. Ya no se pueden editar los campos y se muestra el botón "Editar turno"
                establecerModoEdicion(editable = false)
            } catch (e: Exception) {
                Toast.makeText(
                    this@ManipuladorasJefaActivity,
                    "Error al guardar: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
                establecerModoEdicion(editable = false)
            } finally {
                binding.btnAsignarTurnos.isEnabled = true
                binding.btnAsignarTurnos.text = "Asignar turnos"
            }
        }
    }

    private fun configurarNavegacionInferior() {
        NavigationHelperJefa.setupBarraNavegacionJefa(
            this,
            binding.barraNavegacionJefa,
            NavigationHelperJefa.TabJefa.MANIPULADORAS
        )
    }

    override fun onResume() {
        super.onResume()
        sincronizarManipuladoras()
    }
}
