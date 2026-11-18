package com.example.movilmanupuladora.ui.manipuladora_Jefe

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.movilmanupuladora.R
import com.example.movilmanupuladora.data.api.RetrofitClient
import com.example.movilmanupuladora.data.model.MenuPaeCatalog
import com.example.movilmanupuladora.data.model.PlatoResponse
import com.example.movilmanupuladora.data.repository.MenuRepository
import com.example.movilmanupuladora.databinding.ActivityMenuDelDiaMesBinding
import com.example.movilmanupuladora.databinding.DialogDetallePlatoBinding
import com.example.movilmanupuladora.databinding.ItemPlatoCatalogoBinding
import com.example.movilmanupuladora.utils.PreparacionSyncManager
import com.example.movilmanupuladora.utils.SessionManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
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

    data class PlatoOpcion(
        val id: Int,
        val nombre: String,
        val componente: String,
        val imagenRes: Int
    )

    private var platoActualDesayuno: DetallePlatoDia? = null
    private var platoActualAlmuerzo: DetallePlatoDia? = null
    private var platoActualMerienda: DetallePlatoDia? = null
    private val sessionManager by lazy { SessionManager(this) }
    private val menuRepository = MenuRepository(RetrofitClient.apiService)
    private val platosCatalogoPae = mutableListOf<PlatoResponse>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Edge-to-Edge: fondo se dibuja detrás de la barra de estado
        androidx.core.view.WindowCompat.setDecorFitsSystemWindows(window, false)

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            window.attributes.layoutInDisplayCutoutMode =
                android.view.WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }

        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT
        androidx.core.view.WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars = true

        binding = ActivityMenuDelDiaMesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.headerMenuDelDia) { view, insets ->
            val statusBarHeight = insets.getInsets(WindowInsetsCompat.Type.systemBars()).top
            view.setPadding(
                view.paddingLeft,
                statusBarHeight + (8 * resources.displayMetrics.density).toInt(),
                view.paddingRight,
                view.paddingBottom
            )
            insets
        }

        // Obtener fecha inicial pasada por Intent si existe
        val milis = intent.getLongExtra("fecha_milisegundos", System.currentTimeMillis())
        diaSeleccionado.timeInMillis = milis
        mesActual.timeInMillis = milis

        configurarEventos()
        generarGridMes()
        actualizarDetallePlatos()
        cargarPlatosBackend()
    }

    override fun onResume() {
        super.onResume()
        actualizarDetallePlatos()
        generarGridMes()
    }

    private fun cargarPlatosBackend() {
        lifecycleScope.launch {
            try {
                val res = menuRepository.obtenerPlatos()
                if (res.isSuccessful && !res.body().isNullOrEmpty()) {
                    platosCatalogoPae.clear()
                    platosCatalogoPae.addAll(res.body()!!)
                }
            } catch (_: Exception) { }
        }
    }

    private fun obtenerPlatosPaeDisponibles(): List<PlatoOpcion> {
        val lista = mutableListOf<PlatoOpcion>()
        val mapa = mutableSetOf<String>()

        // 1. Platos oficiales registrados y supervisados en el backend PAE
        for (p in platosCatalogoPae) {
            val nom = p.nombrePlato?.trim()
            if (!nom.isNullOrBlank() && mapa.add(nom.lowercase())) {
                val img = if (p.idPlato in 1..20) {
                    MenuPaeCatalog.obtenerMenuPorId(p.idPlato).imagenRes
                } else {
                    MenuPaeCatalog.obtenerImagenPorNombre(nom)
                }
                lista.add(
                    PlatoOpcion(
                        id = p.idPlato,
                        nombre = nom,
                        componente = p.componente ?: "Sopas, Sazón y Proteína estipulada PAE",
                        imagenRes = img
                    )
                )
            }
        }

        // 2. Platos oficiales de los 20 menús del ciclo PAE
        for (m in MenuPaeCatalog.menus) {
            if (mapa.add(m.nombre.lowercase())) {
                lista.add(
                    PlatoOpcion(
                        id = m.id,
                        nombre = m.nombre,
                        componente = m.componente,
                        imagenRes = m.imagenRes
                    )
                )
            }
        }

        return lista
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

        // Programar o cambiar plato para la jornada seleccionada
        binding.btnProgramarDesayuno.setOnClickListener { mostrarDialogoSeleccionarPlato("Desayuno") }
        binding.cardDetalleDesayuno.setOnClickListener { mostrarDialogoSeleccionarPlato("Desayuno") }
        binding.btnVerDetalleDesayuno.setOnClickListener { platoActualDesayuno?.let { mostrarModalDetalle(it) } }

        binding.btnProgramarAlmuerzo.setOnClickListener { mostrarDialogoSeleccionarPlato("Almuerzo") }
        binding.cardDetalleAlmuerzo.setOnClickListener { mostrarDialogoSeleccionarPlato("Almuerzo") }
        binding.btnVerDetalleAlmuerzo.setOnClickListener { platoActualAlmuerzo?.let { mostrarModalDetalle(it) } }

        binding.btnProgramarMerienda.setOnClickListener { mostrarDialogoSeleccionarPlato("Merienda") }
        binding.cardDetalleMerienda.setOnClickListener { mostrarDialogoSeleccionarPlato("Merienda") }
        binding.btnVerDetalleMerienda.setOnClickListener { platoActualMerienda?.let { mostrarModalDetalle(it) } }

        // Botón para acceder directamente a la planificación semanal completa
        binding.btnPlanificarSemanaSiguiente.setOnClickListener {
            val intent = Intent(this, PlanificacionSemanalJefaActivity::class.java).apply {
                putExtra("semana_offset", 1) // Siguiente semana
            }
            startActivity(intent)
        }

        // Botón para enviar programación de menús directamente al supervisor
        binding.btnEnviarProgramacionSupervisor.setOnClickListener {
            confirmarYEnviarAlSupervisor()
        }
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
        val primerDiaSemana = calGrid.get(Calendar.DAY_OF_WEEK)
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
                        calDiaActual.set(Calendar.HOUR_OF_DAY, 0)
                        calDiaActual.set(Calendar.MINUTE, 0)
                        calDiaActual.set(Calendar.SECOND, 0)
                        calDiaActual.set(Calendar.MILLISECOND, 0)

                        val hoyCero = Calendar.getInstance().apply {
                            set(Calendar.HOUR_OF_DAY, 0)
                            set(Calendar.MINUTE, 0)
                            set(Calendar.SECOND, 0)
                            set(Calendar.MILLISECOND, 0)
                        }

                        val isToday = esMismoDia(calDiaActual, Calendar.getInstance())
                        val isPast = calDiaActual.before(hoyCero)
                        val isSelected = esMismoDia(calDiaActual, diaSeleccionado)
                        val esCompleto = esDiaCompletoProgramado(calDiaActual)

                        when {
                            isToday -> {
                                txtDia.setBackgroundResource(R.drawable.circulo_amarillo_badge)
                                txtDia.setTextColor(Color.parseColor("#1E293B"))
                                txtDia.typeface = android.graphics.Typeface.DEFAULT_BOLD
                            }
                            isPast -> {
                                // Días anteriores al 14 (hoy): siempre círculo punteado
                                txtDia.setBackgroundResource(R.drawable.bg_date_selected)
                                txtDia.setTextColor(Color.parseColor("#7A5500"))
                                txtDia.typeface = android.graphics.Typeface.DEFAULT_BOLD
                            }
                            isSelected -> {
                                if (esCompleto) {
                                    txtDia.setBackgroundResource(R.drawable.bg_pill_green)
                                    txtDia.setTextColor(Color.parseColor("#2A5226"))
                                } else {
                                    txtDia.setBackgroundResource(R.drawable.circulo_amarillo_badge)
                                    txtDia.setTextColor(Color.parseColor("#1E293B"))
                                }
                                txtDia.typeface = android.graphics.Typeface.DEFAULT_BOLD
                            }
                            esCompleto -> {
                                // Día futuro con todos sus momentos programados: VERDE
                                txtDia.setBackgroundResource(R.drawable.bg_pill_green)
                                txtDia.setTextColor(Color.parseColor("#2A5226"))
                                txtDia.typeface = android.graphics.Typeface.DEFAULT_BOLD
                            }
                            else -> {
                                txtDia.setBackgroundColor(Color.TRANSPARENT)
                                txtDia.setTextColor(Color.parseColor("#1E293B"))
                                txtDia.typeface = android.graphics.Typeface.DEFAULT
                            }
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

    private fun esDiaCompletoProgramado(cal: Calendar): Boolean {
        val fStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cal.time)
        val des = sessionManager.getPlatoDiaServicio(this, fStr, "Desayuno")
        val sedeTexto = sessionManager.getSedeNombre().orEmpty().lowercase()
        val esSoloDesayuno = sedeTexto.contains("piagua")
        if (esSoloDesayuno) {
            return !des.isNullOrBlank()
        }
        val alm = sessionManager.getPlatoDiaServicio(this, fStr, "Almuerzo")
        val mer = sessionManager.getPlatoDiaServicio(this, fStr, "Merienda")
        return !des.isNullOrBlank() && !alm.isNullOrBlank() && !mer.isNullOrBlank()
    }

    // =========================================================
    // ACTUALIZAR DETALLE DE PLATOS (DESAYUNO, ALMUERZO, MERIENDA)
    // =========================================================

    private fun actualizarDetallePlatos() {
        val fechaClave = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(diaSeleccionado.time)
        val customDes = sessionManager.getPlatoDiaServicio(this, fechaClave, "Desayuno")
        val customAlm = sessionManager.getPlatoDiaServicio(this, fechaClave, "Almuerzo")
        val customMer = sessionManager.getPlatoDiaServicio(this, fechaClave, "Merienda")

        val hoyCero = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
        val calDiaSelCero = (diaSeleccionado.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
        val esPasado = calDiaSelCero.before(hoyCero)
        val menuDia = MenuPaeCatalog.obtenerMenuDiaCompleto(diaSeleccionado)

        // 0. ASISTENCIA DEL DÍA POR GRADO
        val detalleAsist = AsistenciaManager.obtenerDetalleGradosFecha(this, fechaClave)
        val totalAsist = AsistenciaManager.obtenerTotalFecha(this, fechaClave)
        val nombreSedeActual = SessionManager(this).getSedeNombre() ?: "Sede"
        binding.txtTituloAsistencia.text = "👥 Asistencia del Día ($nombreSedeActual)"

        if (totalAsist > 0) {
            binding.cardAsistenciaDia.visibility = View.VISIBLE
            binding.txtTotalAsistenciaDia.text = "$totalAsist niños presentes"
            binding.txtTotalAsistenciaDia.setBackgroundResource(R.drawable.bg_pill_green)
            binding.txtTotalAsistenciaDia.setTextColor(Color.parseColor("#2A5226"))

            val partes = mutableListOf<String>()
            detalleAsist["p1"]?.takeIf { it.isNotBlank() }?.let { partes.add("1°: $it") }
            detalleAsist["p2"]?.takeIf { it.isNotBlank() }?.let { partes.add("2°: $it") }
            detalleAsist["p4"]?.takeIf { it.isNotBlank() }?.let { partes.add("4°: $it") }
            detalleAsist["p5"]?.takeIf { it.isNotBlank() }?.let { partes.add("5°: $it") }
            detalleAsist["s6"]?.takeIf { it.isNotBlank() }?.let { partes.add("6°: $it") }
            val textoGrados = if (partes.isNotEmpty()) partes.joinToString(" | ") else "Asistencia registrada por manipuladora"
            binding.txtDetalleGradosAsistenciaDia.text = textoGrados
            binding.txtDetalleGradosAsistenciaDia.setTextColor(Color.parseColor("#334155"))
        } else if (esPasado) {
            binding.cardAsistenciaDia.visibility = View.VISIBLE
            binding.txtTotalAsistenciaDia.text = "Sin registro"
            binding.txtTotalAsistenciaDia.setBackgroundResource(R.drawable.bg_date_selected)
            binding.txtTotalAsistenciaDia.setTextColor(Color.parseColor("#854D0E"))
            binding.txtDetalleGradosAsistenciaDia.text = "No se reportó asistencia de estudiantes en esta fecha"
            binding.txtDetalleGradosAsistenciaDia.setTextColor(Color.parseColor("#64748B"))
        } else {
            binding.cardAsistenciaDia.visibility = View.VISIBLE
            binding.txtTotalAsistenciaDia.text = "Pendiente registro"
            binding.txtTotalAsistenciaDia.setBackgroundResource(R.drawable.bg_date_selected)
            binding.txtTotalAsistenciaDia.setTextColor(Color.parseColor("#854D0E"))
            binding.txtDetalleGradosAsistenciaDia.text = "La manipuladora reportará la asistencia durante la jornada"
            binding.txtDetalleGradosAsistenciaDia.setTextColor(Color.parseColor("#64748B"))
        }

        // 1. DESAYUNO
        if (!customDes.isNullOrBlank()) {
            val imgDes = MenuPaeCatalog.obtenerImagenPorNombre(customDes)
            platoActualDesayuno = DetallePlatoDia(
                nombre = customDes,
                seccion = "Desayuno",
                componente = menuDia.desayuno.componente,
                porciones = "42",
                imagen = imgDes
            )
            binding.imgDetalleDesayuno.setImageResource(imgDes)
            binding.txtDetalleNombreDesayuno.text = customDes
            binding.txtDetalleNombreDesayuno.setTextColor(Color.parseColor("#1E293B"))
            binding.badgeEstadoDesayuno.text = "✓ Programado"
            binding.badgeEstadoDesayuno.setBackgroundResource(R.drawable.bg_pill_green)
            binding.badgeEstadoDesayuno.setTextColor(Color.parseColor("#2A5226"))
        } else if (esPasado) {
            val nomDes = menuDia.desayuno.nombre
            val imgDes = MenuPaeCatalog.obtenerImagenPorNombre(nomDes)
            platoActualDesayuno = DetallePlatoDia(
                nombre = nomDes,
                seccion = "Desayuno",
                componente = menuDia.desayuno.componente,
                porciones = "42",
                imagen = imgDes
            )
            binding.imgDetalleDesayuno.setImageResource(imgDes)
            binding.txtDetalleNombreDesayuno.text = nomDes
            binding.txtDetalleNombreDesayuno.setTextColor(Color.parseColor("#1E293B"))
            binding.badgeEstadoDesayuno.text = "Sugerido PAE"
            binding.badgeEstadoDesayuno.setBackgroundResource(R.drawable.bg_date_selected)
            binding.badgeEstadoDesayuno.setTextColor(Color.parseColor("#854D0E"))
        } else {
            platoActualDesayuno = null
            binding.imgDetalleDesayuno.setImageResource(R.drawable.plato1)
            binding.txtDetalleNombreDesayuno.text = "⚠️ Pendiente por programar"
            binding.txtDetalleNombreDesayuno.setTextColor(Color.parseColor("#64748B"))
            binding.badgeEstadoDesayuno.text = "Por programar"
            binding.badgeEstadoDesayuno.setBackgroundResource(R.drawable.bg_date_selected)
            binding.badgeEstadoDesayuno.setTextColor(Color.parseColor("#64748B"))
        }

        // 2. ALMUERZO
        if (!customAlm.isNullOrBlank()) {
            val imgAlm = MenuPaeCatalog.obtenerImagenPorNombre(customAlm)
            platoActualAlmuerzo = DetallePlatoDia(
                nombre = customAlm,
                seccion = "Almuerzo",
                componente = menuDia.almuerzo.componente,
                porciones = "60",
                imagen = imgAlm
            )
            binding.imgDetalleAlmuerzo.setImageResource(imgAlm)
            binding.txtDetalleNombreAlmuerzo.text = customAlm
            binding.txtDetalleNombreAlmuerzo.setTextColor(Color.parseColor("#1E293B"))
            binding.badgeEstadoAlmuerzo.text = "✓ Programado"
            binding.badgeEstadoAlmuerzo.setBackgroundResource(R.drawable.bg_pill_green)
            binding.badgeEstadoAlmuerzo.setTextColor(Color.parseColor("#2A5226"))
        } else if (esPasado) {
            val nomAlm = menuDia.almuerzo.nombre
            val imgAlm = MenuPaeCatalog.obtenerImagenPorNombre(nomAlm)
            platoActualAlmuerzo = DetallePlatoDia(
                nombre = nomAlm,
                seccion = "Almuerzo",
                componente = menuDia.almuerzo.componente,
                porciones = "60",
                imagen = imgAlm
            )
            binding.imgDetalleAlmuerzo.setImageResource(imgAlm)
            binding.txtDetalleNombreAlmuerzo.text = nomAlm
            binding.txtDetalleNombreAlmuerzo.setTextColor(Color.parseColor("#1E293B"))
            binding.badgeEstadoAlmuerzo.text = "Sugerido PAE"
            binding.badgeEstadoAlmuerzo.setBackgroundResource(R.drawable.bg_date_selected)
            binding.badgeEstadoAlmuerzo.setTextColor(Color.parseColor("#854D0E"))
        } else {
            platoActualAlmuerzo = null
            binding.imgDetalleAlmuerzo.setImageResource(R.drawable.plato2)
            binding.txtDetalleNombreAlmuerzo.text = "⚠️ Pendiente por programar"
            binding.txtDetalleNombreAlmuerzo.setTextColor(Color.parseColor("#64748B"))
            binding.badgeEstadoAlmuerzo.text = "Por programar"
            binding.badgeEstadoAlmuerzo.setBackgroundResource(R.drawable.bg_date_selected)
            binding.badgeEstadoAlmuerzo.setTextColor(Color.parseColor("#64748B"))
        }

        // 3. MERIENDA
        if (!customMer.isNullOrBlank()) {
            val imgMer = MenuPaeCatalog.obtenerImagenPorNombre(customMer)
            platoActualMerienda = DetallePlatoDia(
                nombre = customMer,
                seccion = "Merienda",
                componente = menuDia.merienda.componente,
                porciones = "30",
                imagen = imgMer
            )
            binding.imgDetalleMerienda.setImageResource(imgMer)
            binding.txtDetalleNombreMerienda.text = customMer
            binding.txtDetalleNombreMerienda.setTextColor(Color.parseColor("#1E293B"))
            binding.badgeEstadoMerienda.text = "✓ Programado"
            binding.badgeEstadoMerienda.setBackgroundResource(R.drawable.bg_pill_green)
            binding.badgeEstadoMerienda.setTextColor(Color.parseColor("#2A5226"))
        } else if (esPasado) {
            val nomMer = menuDia.merienda.nombre
            val imgMer = MenuPaeCatalog.obtenerImagenPorNombre(nomMer)
            platoActualMerienda = DetallePlatoDia(
                nombre = nomMer,
                seccion = "Merienda",
                componente = menuDia.merienda.componente,
                porciones = "30",
                imagen = imgMer
            )
            binding.imgDetalleMerienda.setImageResource(imgMer)
            binding.txtDetalleNombreMerienda.text = nomMer
            binding.txtDetalleNombreMerienda.setTextColor(Color.parseColor("#1E293B"))
            binding.badgeEstadoMerienda.text = "Sugerido PAE"
            binding.badgeEstadoMerienda.setBackgroundResource(R.drawable.bg_date_selected)
            binding.badgeEstadoMerienda.setTextColor(Color.parseColor("#854D0E"))
        } else {
            platoActualMerienda = null
            binding.imgDetalleMerienda.setImageResource(R.drawable.plato3)
            binding.txtDetalleNombreMerienda.text = "⚠️ Pendiente por programar"
            binding.txtDetalleNombreMerienda.setTextColor(Color.parseColor("#64748B"))
            binding.badgeEstadoMerienda.text = "Por programar"
            binding.badgeEstadoMerienda.setBackgroundResource(R.drawable.bg_date_selected)
            binding.badgeEstadoMerienda.setTextColor(Color.parseColor("#64748B"))
        }

        val sedeTexto = sessionManager.getSedeNombre().orEmpty().lowercase()
        val esSoloDesayuno = sedeTexto.contains("piagua")
        if (esSoloDesayuno) {
            binding.cardDetalleAlmuerzo.visibility = View.GONE
            binding.cardDetalleMerienda.visibility = View.GONE
        } else {
            binding.cardDetalleAlmuerzo.visibility = View.VISIBLE
            binding.cardDetalleMerienda.visibility = View.VISIBLE
        }

        actualizarBotonSecuenciaSemana()
    }

    // =========================================================
    // FLUJO SECUENCIAL DE PLANIFICACIÓN SEMANAL (LUNES A VIERNES)
    // =========================================================

    private fun actualizarBotonSecuenciaSemana() {
        val calLunes = (diaSeleccionado.clone() as Calendar).apply {
            firstDayOfWeek = Calendar.MONDAY
            set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }

        val diasSemanaHabiles = (0..4).map { offset ->
            (calLunes.clone() as Calendar).apply { add(Calendar.DAY_OF_MONTH, offset) }
        }

        val todosCompletos = diasSemanaHabiles.all { esDiaCompletoProgramado(it) }
        val diaActualCompleto = esDiaCompletoProgramado(diaSeleccionado)
        val dayOfWeek = diaSeleccionado.get(Calendar.DAY_OF_WEEK)

        // Quitar completamente cualquier icono del botón de planificar
        binding.btnPlanificarSemanaSiguiente.icon = null

        if (todosCompletos) {
            // Toda la semana completa de Lunes a Viernes planificada
            binding.btnPlanificarSemanaSiguiente.text = "Semana planificada (Lunes a Viernes)"
            binding.btnPlanificarSemanaSiguiente.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#E2E8F0"))
            binding.btnPlanificarSemanaSiguiente.setTextColor(Color.parseColor("#334155"))
            binding.btnPlanificarSemanaSiguiente.setOnClickListener {
                Toast.makeText(this, "Esta semana ya tiene todos sus días planificados. Presiona 'Enviar programación al supervisor' abajo para oficializarla.", Toast.LENGTH_LONG).show()
            }
        } else if (diaActualCompleto && dayOfWeek in Calendar.MONDAY..Calendar.THURSDAY) {
            // El día actual ya tiene todos sus platos requeridos: avanzar al siguiente día hábil
            val calSiguiente = (diaSeleccionado.clone() as Calendar).apply { add(Calendar.DAY_OF_MONTH, 1) }
            val numDiaSig = calSiguiente.get(Calendar.DAY_OF_MONTH)
            val nombreDiaSig = when (calSiguiente.get(Calendar.DAY_OF_WEEK)) {
                Calendar.TUESDAY -> "Martes"
                Calendar.WEDNESDAY -> "Miércoles"
                Calendar.THURSDAY -> "Jueves"
                Calendar.FRIDAY -> "Viernes"
                else -> "siguiente"
            }

            binding.btnPlanificarSemanaSiguiente.text = "Planificar día $numDiaSig ($nombreDiaSig) ➡️"
            binding.btnPlanificarSemanaSiguiente.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#FAC94B"))
            binding.btnPlanificarSemanaSiguiente.setTextColor(Color.parseColor("#1E293B"))
            binding.btnPlanificarSemanaSiguiente.setOnClickListener {
                diaSeleccionado = calSiguiente
                generarGridMes()
                actualizarDetallePlatos()
            }
        } else {
            val nombreDia = when (dayOfWeek) {
                Calendar.MONDAY -> "Lunes"
                Calendar.TUESDAY -> "Martes"
                Calendar.WEDNESDAY -> "Miércoles"
                Calendar.THURSDAY -> "Jueves"
                Calendar.FRIDAY -> "Viernes"
                Calendar.SATURDAY -> "Sábado"
                Calendar.SUNDAY -> "Domingo"
                else -> "Día"
            }
            val numDia = diaSeleccionado.get(Calendar.DAY_OF_MONTH)

            if (!diaActualCompleto && dayOfWeek in Calendar.MONDAY..Calendar.FRIDAY) {
                val sedeTexto = sessionManager.getSedeNombre().orEmpty().lowercase()
                val faltantes = if (sedeTexto.contains("piagua")) "el Desayuno" else "Desayuno, Almuerzo y Merienda"
                binding.btnPlanificarSemanaSiguiente.text = "Programar $faltantes de $nombreDia $numDia"
            } else {
                binding.btnPlanificarSemanaSiguiente.text = "Planificar toda la semana siguiente"
            }

            binding.btnPlanificarSemanaSiguiente.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#FAC94B"))
            binding.btnPlanificarSemanaSiguiente.setTextColor(Color.parseColor("#1E293B"))
            binding.btnPlanificarSemanaSiguiente.setOnClickListener {
                if (dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY) {
                    val proxLunes = (calLunes.clone() as Calendar).apply { add(Calendar.DAY_OF_MONTH, 7) }
                    diaSeleccionado = proxLunes
                    generarGridMes()
                    actualizarDetallePlatos()
                } else {
                    Toast.makeText(this, "Completa los platos requeridos de $nombreDia $numDia para avanzar", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun confirmarYEnviarAlSupervisor() {
        val sdfClave = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val calLunes = (diaSeleccionado.clone() as Calendar).apply {
            firstDayOfWeek = Calendar.MONDAY
            set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
        val diasSemanaHabiles = (0..4).map { offset ->
            (calLunes.clone() as Calendar).apply { add(Calendar.DAY_OF_MONTH, offset) }
        }

        // Buscar qué días tienen al menos un plato registrado
        val diasConMenu = diasSemanaHabiles.filter { calDia ->
            val f = sdfClave.format(calDia.time)
            !sessionManager.getPlatoDiaServicio(this, f, "Desayuno").isNullOrBlank() ||
            !sessionManager.getPlatoDiaServicio(this, f, "Almuerzo").isNullOrBlank() ||
            !sessionManager.getPlatoDiaServicio(this, f, "Merienda").isNullOrBlank()
        }.toMutableList()

        val fSel = sdfClave.format(diaSeleccionado.time)
        val selTiene = !sessionManager.getPlatoDiaServicio(this, fSel, "Desayuno").isNullOrBlank() ||
                       !sessionManager.getPlatoDiaServicio(this, fSel, "Almuerzo").isNullOrBlank() ||
                       !sessionManager.getPlatoDiaServicio(this, fSel, "Merienda").isNullOrBlank()

        if (selTiene && diasConMenu.none { esMismoDia(it, diaSeleccionado) }) {
            diasConMenu.add(diaSeleccionado)
        }

        if (diasConMenu.isEmpty()) {
            Toast.makeText(this, "Primero programa al menos un menú antes de enviar al supervisor", Toast.LENGTH_SHORT).show()
            return
        }

        val sedeNombre = sessionManager.getSedeNombre() ?: "asignada"
        val cantDias = diasConMenu.size
        val resumenDias = diasConMenu.joinToString(", ") { SimpleDateFormat("d 'de' MMMM", Locale.forLanguageTag("es-CO")).format(it.time) }

        MaterialAlertDialogBuilder(this)
            .setTitle("¿Enviar Programación al Supervisor?")
            .setMessage("Has seleccionado menús para $cantDias día(s) ($resumenDias) de la sede $sedeNombre.\n\nAl enviar, estos menús se registrarán en la plataforma para que le aparezcan al Supervisor PAE en su minuta oficial y pueda dar el visto bueno.\n\n¿Deseas enviar la programación ahora?")
            .setPositiveButton("Sí, Enviar") { _, _ ->
                enviarProgramacionAlSupervisor(diasConMenu)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun enviarProgramacionAlSupervisor(dias: List<Calendar>) {
        val sdfClave = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val sedeNombre = sessionManager.getSedeNombre() ?: "asignada"
        val jefaNombre = sessionManager.getUserName() ?: "Jefa de Cocina"
        val idSede = sessionManager.getSedeId().takeIf { it > 0 } ?: 1

        lifecycleScope.launch {
            try {
                var totalPlatosEnviados = 0
                for (d in dias) {
                    val fClave = sdfClave.format(d.time)
                    val semanaDelAnio = d.get(Calendar.WEEK_OF_YEAR)
                    val indiceSemana = Math.abs(semanaDelAnio - 1) % 4
                    val diaSemana = d.get(Calendar.DAY_OF_WEEK)
                    val diaIndice = when (diaSemana) {
                        Calendar.MONDAY -> 0
                        Calendar.TUESDAY -> 1
                        Calendar.WEDNESDAY -> 2
                        Calendar.THURSDAY -> 3
                        Calendar.FRIDAY -> 4
                        else -> 0
                    }
                    val numMenu = (indiceSemana * 5) + diaIndice + 1

                    // 1. Desayuno (id_seccion = 1)
                    val des = sessionManager.getPlatoDiaServicio(this@MenuDelDiaMesActivity, fClave, "Desayuno")
                    val idDes = sessionManager.getPlatoDiaServicioId(this@MenuDelDiaMesActivity, fClave, "Desayuno")
                    if (!des.isNullOrBlank()) {
                        PreparacionSyncManager.guardarPlatoServicio(this@MenuDelDiaMesActivity, "Desayuno", des, idDes, fClave)
                        try {
                            val body = mapOf(
                                "id_jornada" to 1,
                                "fecha" to fClave,
                                "ninos_presentes" to 120,
                                "id_contrato" to 1,
                                "id_seccion" to 1,
                                "id_sede" to idSede,
                                "id_plato" to (idDes.takeIf { it > 0 }),
                                "estado" to "Programado",
                                "informacion_nutricional" to "[SEDE:$idSede] [MENU_CICLO:$numMenu] [Elegido Jefa] $des"
                            )
                            RetrofitClient.apiService.registrarMenu(body)
                            totalPlatosEnviados++
                        } catch (ePost: Exception) {
                            Log.w("MenuDelDia", "Error post menu Desayuno: ${ePost.message}")
                        }
                    }

                    // 2. Almuerzo (id_seccion = 2)
                    val alm = sessionManager.getPlatoDiaServicio(this@MenuDelDiaMesActivity, fClave, "Almuerzo")
                    val idAlm = sessionManager.getPlatoDiaServicioId(this@MenuDelDiaMesActivity, fClave, "Almuerzo")
                    if (!alm.isNullOrBlank()) {
                        PreparacionSyncManager.guardarPlatoServicio(this@MenuDelDiaMesActivity, "Almuerzo", alm, idAlm, fClave)
                        try {
                            val body = mapOf(
                                "id_jornada" to 1,
                                "fecha" to fClave,
                                "ninos_presentes" to 120,
                                "id_contrato" to 1,
                                "id_seccion" to 2,
                                "id_sede" to idSede,
                                "id_plato" to (idAlm.takeIf { it > 0 }),
                                "estado" to "Programado",
                                "informacion_nutricional" to "[SEDE:$idSede] [MENU_CICLO:$numMenu] [Elegido Jefa] $alm"
                            )
                            RetrofitClient.apiService.registrarMenu(body)
                            totalPlatosEnviados++
                        } catch (ePost: Exception) {
                            Log.w("MenuDelDia", "Error post menu Almuerzo: ${ePost.message}")
                        }
                    }

                    // 3. Merienda (id_seccion = 3)
                    val mer = sessionManager.getPlatoDiaServicio(this@MenuDelDiaMesActivity, fClave, "Merienda")
                    val idMer = sessionManager.getPlatoDiaServicioId(this@MenuDelDiaMesActivity, fClave, "Merienda")
                    if (!mer.isNullOrBlank()) {
                        PreparacionSyncManager.guardarPlatoServicio(this@MenuDelDiaMesActivity, "Merienda", mer, idMer, fClave)
                        try {
                            val body = mapOf(
                                "id_jornada" to 1,
                                "fecha" to fClave,
                                "ninos_presentes" to 120,
                                "id_contrato" to 1,
                                "id_seccion" to 3,
                                "id_sede" to idSede,
                                "id_plato" to (idMer.takeIf { it > 0 }),
                                "estado" to "Programado",
                                "informacion_nutricional" to "[SEDE:$idSede] [MENU_CICLO:$numMenu] [Elegido Jefa] $mer"
                            )
                            RetrofitClient.apiService.registrarMenu(body)
                            totalPlatosEnviados++
                        } catch (ePost: Exception) {
                            Log.w("MenuDelDia", "Error post menu Merienda: ${ePost.message}")
                        }
                    }
                }

                // Notificación al Supervisor
                try {
                    val titBody = "Programación de Menús Enviada por Jefa".toRequestBody("text/plain".toMediaTypeOrNull())
                    val msgBody = "La Jefa de Cocina ($jefaNombre) ha enviado la programación de menús para la sede $sedeNombre. Ya están registrados en el sistema y disponibles en la minuta oficial para su aprobación."
                        .toRequestBody("text/plain".toMediaTypeOrNull())
                    RetrofitClient.apiService.enviarNotificacion(titBody, msgBody, null)
                } catch (eNotif: Exception) {
                    Log.w("MenuDelDia", "Aviso push al supervisor: ${eNotif.message}")
                }

                MaterialAlertDialogBuilder(this@MenuDelDiaMesActivity)
                    .setTitle("¡Programación Enviada con Éxito!")
                    .setMessage("Los menús programados ($totalPlatosEnviados platos) han sido enviados al Supervisor PAE. Ya se reflejan en la minuta oficial de la sede para dar el visto bueno.")
                    .setPositiveButton("Aceptar") { _, _ ->
                        generarGridMes()
                        actualizarDetallePlatos()
                    }
                    .show()

            } catch (e: Exception) {
                Toast.makeText(this@MenuDelDiaMesActivity, "Error al enviar programación: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    // =========================================================
    // MODAL DE SELECCIÓN DE PLATOS DEL PAE PARA LA JORNADA
    // =========================================================

    private fun mostrarDialogoSeleccionarPlato(servicio: String) {
        val sedeTexto = sessionManager.getSedeNombre().orEmpty().lowercase()
        val esSoloDesayuno = sedeTexto.contains("piagua")
        if (esSoloDesayuno && !servicio.equals("Desayuno", ignoreCase = true)) {
            MaterialAlertDialogBuilder(this)
                .setTitle("Servicio no autorizado")
                .setMessage("La Sede Piagua opera exclusivamente para el servicio de Desayuno.")
                .setPositiveButton("Entendido", null)
                .show()
            return
        }

        val fechaClave = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(diaSeleccionado.time)
        val fechaFormato = SimpleDateFormat("EEEE, d 'de' MMMM", Locale.forLanguageTag("es-CO")).format(diaSeleccionado.time)
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }

        // Calcular platos servidos el día inmediatamente anterior (Regla de Rotación PAE)
        val calAyer = (diaSeleccionado.clone() as Calendar).apply { add(Calendar.DAY_OF_MONTH, -1) }
        val fechaAyer = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calAyer.time)
        val platosAyer = listOfNotNull(
            sessionManager.getPlatoDiaServicio(this, fechaAyer, "Desayuno"),
            sessionManager.getPlatoDiaServicio(this, fechaAyer, "Almuerzo"),
            sessionManager.getPlatoDiaServicio(this, fechaAyer, "Merienda")
        ).toSet()

        val platoAsignadoActual = sessionManager.getPlatoDiaServicio(this, fechaClave, servicio)
        val listaPlatosOpciones = obtenerPlatosPaeDisponibles()

        // Contenedor principal con ScrollView
        val scroll = androidx.core.widget.NestedScrollView(this).apply {
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        }
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(
                (20 * resources.displayMetrics.density).toInt(),
                (14 * resources.displayMetrics.density).toInt(),
                (20 * resources.displayMetrics.density).toInt(),
                (20 * resources.displayMetrics.density).toInt()
            )
        }
        scroll.addView(layout)

        // Subtítulo explicativo
        val tvSub = TextView(this).apply {
            text = "📅 $fechaFormato\nSelecciona el plato estipulado del PAE para la jornada:"
            textSize = 13f
            setTextColor(Color.parseColor("#475569"))
            setPadding(0, 0, 0, (14 * resources.displayMetrics.density).toInt())
        }
        layout.addView(tvSub)

        var dialogRef: androidx.appcompat.app.AlertDialog? = null

        for (plato in listaPlatosOpciones) {
            val esDeAyer = platosAyer.contains(plato.nombre)
            val esActual = platoAsignadoActual != null && platoAsignadoActual.equals(plato.nombre, ignoreCase = true)

            val itemBinding = ItemPlatoCatalogoBinding.inflate(layoutInflater, layout, false)
            itemBinding.tvNombrePlatoCatalogo.text = plato.nombre
            itemBinding.tvComponenteCatalogo.text = plato.componente
            itemBinding.imgPlatoCatalogo.setImageResource(plato.imagenRes)

            when {
                esActual -> {
                    itemBinding.root.strokeColor = Color.parseColor("#15803D")
                    itemBinding.root.strokeWidth = (2 * resources.displayMetrics.density).toInt()
                    itemBinding.tvComponenteCatalogo.text = "✓ Actualmente seleccionado para $servicio"
                    itemBinding.tvComponenteCatalogo.setTextColor(Color.parseColor("#15803D"))
                }
                esDeAyer -> {
                    itemBinding.root.alpha = 0.5f
                    itemBinding.tvComponenteCatalogo.text = "⛔ No disponible: servido el día anterior (rotación PAE)"
                    itemBinding.tvComponenteCatalogo.setTextColor(Color.parseColor("#BE123C"))
                }
                else -> {
                    itemBinding.root.strokeColor = Color.parseColor("#E2E8F0")
                    itemBinding.root.strokeWidth = (1 * resources.displayMetrics.density).toInt()
                }
            }

            itemBinding.root.setOnClickListener {
                if (esDeAyer) {
                    val diaAyerTexto = SimpleDateFormat("EEEE d", Locale.forLanguageTag("es-CO")).format(calAyer.time)
                    MaterialAlertDialogBuilder(this)
                        .setTitle("Plato no disponible por rotación")
                        .setMessage("El plato \"${plato.nombre}\" ya fue seleccionado para el día anterior ($diaAyerTexto).\n\nPor lineamiento PAE de variedad nutricional, no se puede repetir en días consecutivos.")
                        .setPositiveButton("Entendido", null)
                        .show()
                    return@setOnClickListener
                }

                // Guardar plato en sesión local y sincronizar con backend
                sessionManager.savePlatoDiaServicio(this@MenuDelDiaMesActivity, fechaClave, servicio, plato.nombre, plato.id)
                lifecycleScope.launch {
                    PreparacionSyncManager.guardarPlatoServicio(
                        this@MenuDelDiaMesActivity,
                        servicio,
                        plato.nombre,
                        plato.id,
                        fechaClave
                    )
                }

                Toast.makeText(this@MenuDelDiaMesActivity, "✓ $servicio programado: ${plato.nombre}", Toast.LENGTH_SHORT).show()
                actualizarDetallePlatos()
                generarGridMes()
                dialogRef?.dismiss()
            }

            layout.addView(itemBinding.root)
        }

        dialogRef = MaterialAlertDialogBuilder(this)
            .setTitle("Programar $servicio")
            .setView(scroll)
            .setNegativeButton("Cerrar", null)
            .create()

        dialogRef.show()
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

        dialogBinding.btnOtroAleatorioModal.text = "Cambiar plato"
        dialogBinding.btnOtroAleatorioModal.setOnClickListener {
            dialog.dismiss()
            mostrarDialogoSeleccionarPlato(detalle.seccion)
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
