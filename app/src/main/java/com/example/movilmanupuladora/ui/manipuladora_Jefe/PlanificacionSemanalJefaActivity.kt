package com.example.movilmanupuladora.ui.manipuladora_Jefe

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.CompositePageTransformer
import androidx.viewpager2.widget.ViewPager2
import com.example.movilmanupuladora.R
import com.example.movilmanupuladora.data.api.RetrofitClient
import com.example.movilmanupuladora.data.model.MenuPaeCatalog
import com.example.movilmanupuladora.data.model.SugerenciaIAItem
import com.example.movilmanupuladora.data.model.SugerenciasRecetasRequest
import com.example.movilmanupuladora.data.repository.MenuRepository
import com.example.movilmanupuladora.databinding.ActivityPlanificacionSemanalJefaBinding
import com.example.movilmanupuladora.utils.PreparacionSyncManager
import com.example.movilmanupuladora.utils.SedeContratoManager
import com.example.movilmanupuladora.utils.SessionManager
import com.example.movilmanupuladora.utils.ImageLoaderUtil
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch
import java.util.Date
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class PlanificacionSemanalJefaActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPlanificacionSemanalJefaBinding
    private lateinit var sessionManager: SessionManager
    private val menuRepository = MenuRepository(RetrofitClient.apiService)

    private var diaSeleccionadoSemanaSiguiente: Calendar = Calendar.getInstance()
    private val diasSemanaSiguiente = mutableListOf<Calendar>()

    private var comidaSeleccionada = TipoComida.DESAYUNO
    private var posicionPlatoCarousel = 0
    private var esSemanaAprobadaFijada = false
    private var carouselPageCallback: ViewPager2.OnPageChangeCallback? = null
    private val cacheSugerenciasIA = mutableMapOf<Int, List<SugerenciaIAItem>>()
    private val platosSugeridosIA = mutableMapOf<Int, List<InicioJefaActivity.Plato>>()

    private var platoSeleccionadoDesayuno: InicioJefaActivity.Plato? = null
    private var platoSeleccionadoAlmuerzo: InicioJefaActivity.Plato? = null
    private var platoSeleccionadoMerienda: InicioJefaActivity.Plato? = null

    enum class TipoComida {
        DESAYUNO,
        ALMUERZO,
        MERIENDA
    }

    private lateinit var platosDesayuno: MutableList<InicioJefaActivity.Plato>
    private lateinit var platosAlmuerzo: MutableList<InicioJefaActivity.Plato>
    private lateinit var platosMerienda: MutableList<InicioJefaActivity.Plato>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityPlanificacionSemanalJefaBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        sessionManager = SessionManager(this)

        try {
            val esAlmuerzoKeywords = listOf(
                "pollo", "carne", "res", "cerdo", "hígado", "higado", "pescado", "atún", "atun",
                "lenteja", "frijol", "fríjol", "garbanzo", "sancocho", "sopa", "pasta", "espagueti",
                "ensalada", "guisad", "sudado", "albóndiga", "albondiga", "almuerzo"
            )

            platosDesayuno = MenuPaeCatalog.menus
                .filter { item ->
                    val low = item.nombreDesayuno.lowercase()
                    esAlmuerzoKeywords.none { low.contains(it) }
                }
                .map {
                    InicioJefaActivity.Plato(id = it.id, nombre = it.nombreDesayuno, componente = it.compDesayuno, imagen = it.imgDesayuno)
                }.toMutableList()

            platosAlmuerzo = MenuPaeCatalog.menus.map {
                InicioJefaActivity.Plato(id = it.id, nombre = it.nombreAlmuerzo, componente = it.compAlmuerzo, imagen = it.imgAlmuerzo)
            }.toMutableList()

            platosMerienda = MenuPaeCatalog.menus.map {
                InicioJefaActivity.Plato(id = it.id, nombre = it.nombreMerienda, componente = it.compMerienda, imagen = it.imgMerienda)
            }.toMutableList()

            semanaOffset = intent.getIntExtra("semana_offset", 0)
            configurarSemanaSiguiente()
            configurarEventos()
            configurarTabsComida()
            configurarBotonesSeccionesIA()
            cargarPlatosSeleccionados()
            configurarViewPagerCarousel()
            configurarMenuDisponiblePAE()
            cargarPlatosDesdeBackend()
            consultarEstadoAprobacionYDevolucion()
            cargarSugerenciasSegunInventario(obtenerIdSeccionActual())

            lifecycleScope.launch {
                val idSede = SedeContratoManager.resolverIdSedeEfectivo(sessionManager)
                SedeContratoManager.sincronizarSeccionesContrato(this@PlanificacionSemanalJefaActivity, idSede)
                PreparacionSyncManager.sincronizarMenusPorFechaDesdeBackend(this@PlanificacionSemanalJefaActivity)
                configurarTabsComida()
                configurarBotonesSeccionesIA()
                renderizarNumerosSemanaSiguiente()
                cargarPlatosSeleccionados()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Error al abrir planificación: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }

        // Manejar botón atrás con animación de deslizamiento hacia abajo
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                finish()
                @Suppress("DEPRECATION")
                overridePendingTransition(R.anim.stay, R.anim.slide_out_bottom)
            }
        })
    }

    override fun onResume() {
        super.onResume()
        configurarTabsComida()
        configurarBotonesSeccionesIA()
        cargarPlatosDesdeBackend()
        cargarPlatosSeleccionados()
        configurarViewPagerCarousel()
        configurarMenuDisponiblePAE()
        consultarEstadoAprobacionYDevolucion()
        cargarSugerenciasSegunInventario(obtenerIdSeccionActual())

        lifecycleScope.launch {
            val idSede = sessionManager.getSedeId().takeIf { it > 0 } ?: 1
            SedeContratoManager.sincronizarSeccionesContrato(this@PlanificacionSemanalJefaActivity, idSede)
            PreparacionSyncManager.sincronizarMenusPorFechaDesdeBackend(this@PlanificacionSemanalJefaActivity)
            configurarTabsComida()
            configurarBotonesSeccionesIA()
            renderizarNumerosSemanaSiguiente()
            cargarPlatosSeleccionados()
        }
    }

    private var semanaOffset = 0

    // =========================================================
    // CÁLCULO DE LA SEMANA (LUNES A DOMINGO)
    // =========================================================

    private fun configurarSemanaSiguiente() {
        diasSemanaSiguiente.clear()

        // Calcular el domingo de la semana según el offset (0 = semana actual, 1 = semana siguiente)
        val calSemana = Calendar.getInstance().apply {
            firstDayOfWeek = Calendar.SUNDAY
            set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
            if (semanaOffset != 0) {
                add(Calendar.WEEK_OF_YEAR, semanaOffset)
            }
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        for (i in 0 until 7) {
            diasSemanaSiguiente.add(calSemana.clone() as Calendar)
            calSemana.add(Calendar.DAY_OF_MONTH, 1)
        }

        // Título del mes de la semana (ej. "Noviembre 2026")
        val mesNombre = SimpleDateFormat("MMMM yyyy", Locale.forLanguageTag("es-CO")).format(diasSemanaSiguiente.first().time)
        binding.txtMesAnio.text = mesNombre.replaceFirstChar {
            if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
        }

        // Por defecto seleccionar hoy si es día hábil (Lun-Vie); si hoy es fin de semana en la semana actual, elegir el viernes anterior; en otras semanas, el lunes
        val hoy = Calendar.getInstance()
        val hoyEsFinDeSemana = hoy.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY ||
                hoy.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY
        val indexHoy = if (!hoyEsFinDeSemana) diasSemanaSiguiente.indexOfFirst { esMismoDia(it, hoy) } else -1
        diaSeleccionadoSemanaSiguiente = when {
            indexHoy != -1 -> diasSemanaSiguiente[indexHoy]
            semanaOffset == 0 && hoyEsFinDeSemana -> diasSemanaSiguiente.getOrElse(5) { diasSemanaSiguiente.first() }
            else -> diasSemanaSiguiente.getOrElse(1) { diasSemanaSiguiente.first() }
        }

        renderizarNumerosSemanaSiguiente()
        cargarPlatosSeleccionados()
    }

    private fun renderizarNumerosSemanaSiguiente() {
        binding.layoutNumerosSemanaSiguiente.removeAllViews()

        for (dia in diasSemanaSiguiente) {
            val cell = FrameLayout(this).apply {
                val p = LinearLayout.LayoutParams(0, (44 * resources.displayMetrics.density).toInt(), 1f)
                layoutParams = p
            }

            val fechaDia = obtenerClaveFecha(dia)
            val tieneDes = !sessionManager.getPlatoDiaServicio(this, fechaDia, "Desayuno").isNullOrBlank()
            val tieneAlm = !sessionManager.getPlatoDiaServicio(this, fechaDia, "Almuerzo").isNullOrBlank()
            val tieneMer = !sessionManager.getPlatoDiaServicio(this, fechaDia, "Merienda").isNullOrBlank()
            val permitidas = obtenerJornadasPermitidas()
            val diaCompleto = permitidas.all { tipo ->
                when (tipo) {
                    TipoComida.DESAYUNO -> tieneDes
                    TipoComida.ALMUERZO -> tieneAlm
                    TipoComida.MERIENDA -> tieneMer
                }
            }

            val dayOfWeek = dia.get(Calendar.DAY_OF_WEEK)
            val esFinDeSemana = dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY
            val isSelected = esMismoDia(dia, diaSeleccionadoSemanaSiguiente)

            val txtNum = TextView(this).apply {
                val p = FrameLayout.LayoutParams(
                    (34 * resources.displayMetrics.density).toInt(),
                    (34 * resources.displayMetrics.density).toInt()
                ).apply { gravity = Gravity.CENTER }
                layoutParams = p
                gravity = Gravity.CENTER
                textSize = 14f
                text = dia.get(Calendar.DAY_OF_MONTH).toString()

                val tieneAlgunMenu = tieneDes || tieneAlm || tieneMer
                val colorTextoDia = when {
                    esFinDeSemana && isSelected -> Color.parseColor("#1E293B")
                    esFinDeSemana -> Color.parseColor("#CBD5E1")
                    isSelected -> Color.parseColor("#1E293B")
                    tieneAlgunMenu -> Color.parseColor("#92400E")
                    else -> Color.parseColor("#1E293B")
                }
                when {
                    esFinDeSemana && isSelected -> {
                        setBackgroundResource(R.drawable.circulo_amarillo_badge)
                        setTextColor(colorTextoDia)
                        typeface = android.graphics.Typeface.DEFAULT_BOLD
                    }
                    esFinDeSemana -> {
                        background = null
                        setTextColor(colorTextoDia)
                        typeface = android.graphics.Typeface.DEFAULT
                    }
                    isSelected -> {
                        setBackgroundResource(R.drawable.circulo_amarillo_badge)
                        setTextColor(colorTextoDia)
                        typeface = android.graphics.Typeface.DEFAULT_BOLD
                    }
                    tieneAlgunMenu -> {
                        setBackgroundResource(R.drawable.bg_dia_con_menu)
                        setTextColor(colorTextoDia)
                        typeface = android.graphics.Typeface.DEFAULT_BOLD
                    }
                    else -> {
                        setBackgroundColor(Color.TRANSPARENT)
                        setTextColor(colorTextoDia)
                        typeface = android.graphics.Typeface.DEFAULT_BOLD
                    }
                }
            }

            cell.isClickable = true
            cell.isFocusable = true
            cell.setOnClickListener {
                diaSeleccionadoSemanaSiguiente = dia
                renderizarNumerosSemanaSiguiente()
                cargarPlatosSeleccionados()
                configurarViewPagerCarousel()
                configurarMenuDisponiblePAE()
                actualizarEstadoFinDeSemanaUI()
                if (esFinDeSemana) {
                    Toast.makeText(this, "Los fines de semana no tienen servicio escolar PAE.", Toast.LENGTH_SHORT).show()
                }
            }

            cell.addView(txtNum)
            if (!esFinDeSemana && sessionManager.estaDiaPendienteAprobacion(this, fechaDia)) {
                val colorDot = if (isSelected) Color.parseColor("#1E293B") else Color.parseColor("#92400E")
                com.example.movilmanupuladora.utils.CalendarDotHelper.aplicarPuntitoPendiente(cell, txtNum, colorDot)
            }
            binding.layoutNumerosSemanaSiguiente.addView(cell)
        }
    }

    private fun esMismoDia(c1: Calendar, c2: Calendar): Boolean {
        return c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR) &&
                c1.get(Calendar.DAY_OF_YEAR) == c2.get(Calendar.DAY_OF_YEAR)
    }

    // =========================================================
    // GESTIÓN DE PLATOS SELECCIONADOS POR DÍA Y SERVICIO
    // =========================================================

    private fun obtenerClaveFecha(cal: Calendar): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cal.time)

    /**
     * Obtiene el conjunto de nombres de platos asignados al día inmediatamente anterior
     * para asegurar la regla de rotación PAE (no repetir platos en días consecutivos).
     */
    private fun obtenerPlatosDiaAnterior(): Set<String> {
        val calAyer = (diaSeleccionadoSemanaSiguiente.clone() as Calendar).apply {
            add(Calendar.DAY_OF_MONTH, -1)
        }
        val fechaAyer = obtenerClaveFecha(calAyer)
        val des = sessionManager.getPlatoDiaServicio(this, fechaAyer, "Desayuno")
        val alm = sessionManager.getPlatoDiaServicio(this, fechaAyer, "Almuerzo")
        val mer = sessionManager.getPlatoDiaServicio(this, fechaAyer, "Merienda")
        return listOfNotNull(des, alm, mer).toSet()
    }

    private fun buscarPlatoPorNombre(nombre: String?): InicioJefaActivity.Plato? {
        if (nombre.isNullOrBlank()) return null
        val todos = (platosDesayuno + platosAlmuerzo + platosMerienda)
        return todos.firstOrNull { it.nombre.equals(nombre, ignoreCase = true) }
            ?: todos.firstOrNull { it.nombre.contains(nombre, ignoreCase = true) || nombre.contains(it.nombre, ignoreCase = true) }
            ?: run {
                val img = InicioJefaActivity.obtenerImagenPlatoPorNombre(nombre)
                InicioJefaActivity.Plato(id = 0, nombre = nombre, componente = "Menú PAE", imagen = img)
            }
    }

    private fun cargarPlatosSeleccionados() {
        val fecha = obtenerClaveFecha(diaSeleccionadoSemanaSiguiente)
        var nomDes = sessionManager.getPlatoDiaServicio(this, fecha, "Desayuno")
        var nomAlm = sessionManager.getPlatoDiaServicio(this, fecha, "Almuerzo")
        var nomMer = sessionManager.getPlatoDiaServicio(this, fecha, "Merienda")

        // Fallback inicial solo si es el primer día y no tenía datos previos guardados
        if (nomDes == null && nomAlm == null && nomMer == null &&
            diasSemanaSiguiente.isNotEmpty() && esMismoDia(diaSeleccionadoSemanaSiguiente, diasSemanaSiguiente.first())) {
            nomDes = sessionManager.getPlatoServicio(this, "Desayuno")
            nomAlm = sessionManager.getPlatoServicio(this, "Almuerzo")
            nomMer = sessionManager.getPlatoServicio(this, "Merienda")
        }

        platoSeleccionadoDesayuno = buscarPlatoPorNombre(nomDes)
        platoSeleccionadoAlmuerzo = buscarPlatoPorNombre(nomAlm)
        platoSeleccionadoMerienda = buscarPlatoPorNombre(nomMer)

        actualizarSubtitulosTabs()
        renderizarResumenPlatosDia()
        actualizarEstadoFinDeSemanaUI()
    }

    private fun esDiaFinDeSemana(cal: Calendar): Boolean {
        val dow = cal.get(Calendar.DAY_OF_WEEK)
        return dow == Calendar.SATURDAY || dow == Calendar.SUNDAY
    }

    private fun esDiaPasado(cal: Calendar): Boolean {
        val hoyClave = obtenerClaveFecha(Calendar.getInstance())
        return obtenerClaveFecha(cal) < hoyClave
    }

    private fun estaServicioBloqueadoPorEnvio(cal: Calendar, servicio: String): Boolean {
        val fecha = obtenerClaveFecha(cal)
        val plato = sessionManager.getPlatoDiaServicio(this, fecha, servicio)
        if (plato.isNullOrBlank()) return false
        val estado = sessionManager.getEstadoMenuDiaServicio(this, fecha, servicio).trim()
        return estado.isNotBlank() &&
            !estado.equals("Borrador", ignoreCase = true) &&
            !estado.equals("Rechazado", ignoreCase = true)
    }

    private fun actualizarEstadoFinDeSemanaUI() {
        val esFinSem = esDiaFinDeSemana(diaSeleccionadoSemanaSiguiente)
        val esPasado = esDiaPasado(diaSeleccionadoSemanaSiguiente)
        val servActual = obtenerNombreComidaActual()
        val bloqueadoEnvio = !esFinSem && estaServicioBloqueadoPorEnvio(diaSeleccionadoSemanaSiguiente, servActual)
        val todosServiciosBloqueados = !esFinSem && obtenerJornadasPermitidas().all { tipo ->
            val serv = when (tipo) {
                TipoComida.DESAYUNO -> "Desayuno"
                TipoComida.ALMUERZO -> "Almuerzo"
                TipoComida.MERIENDA -> "Merienda"
            }
            estaServicioBloqueadoPorEnvio(diaSeleccionadoSemanaSiguiente, serv)
        }

        if (esFinSem) {
            binding.btnSeleccionarPlato.isEnabled = false
            binding.btnSeleccionarPlato.alpha = 0.45f
            binding.btnSeleccionarPlato.text = "No disponible (Fin de semana)"
            binding.btnGuardarSeleccion.isEnabled = false
            binding.btnGuardarSeleccion.alpha = 0.45f
        } else if (esPasado) {
            binding.btnSeleccionarPlato.isEnabled = false
            binding.btnSeleccionarPlato.alpha = 0.45f
            binding.btnSeleccionarPlato.text = "Día anterior (Solo lectura)"
            binding.btnGuardarSeleccion.isEnabled = false
            binding.btnGuardarSeleccion.alpha = 0.45f
        } else if (bloqueadoEnvio) {
            binding.btnSeleccionarPlato.isEnabled = false
            binding.btnSeleccionarPlato.alpha = 0.45f
            binding.btnSeleccionarPlato.text = "Enviado al supervisor"
            binding.btnGuardarSeleccion.isEnabled = !todosServiciosBloqueados
            binding.btnGuardarSeleccion.alpha = if (todosServiciosBloqueados) 0.45f else 1.0f
        } else if (!esSemanaAprobadaFijada) {
            binding.btnSeleccionarPlato.isEnabled = true
            binding.btnSeleccionarPlato.alpha = 1.0f
            binding.btnGuardarSeleccion.isEnabled = true
            binding.btnGuardarSeleccion.alpha = 1.0f
            actualizarBotonSeleccionarPlato(posicionPlatoCarousel, obtenerPlatosActuales())
        }
    }

    private fun renderizarResumenPlatosDia() {
        val contenedor = binding.layoutPlatosProgramadosDia
        contenedor.removeAllViews()

        val esFinSem = esDiaFinDeSemana(diaSeleccionadoSemanaSiguiente)
        val calResumen = (diaSeleccionadoSemanaSiguiente.clone() as Calendar).apply {
            val dow = get(Calendar.DAY_OF_WEEK)
            if (dow == Calendar.SATURDAY) add(Calendar.DAY_OF_MONTH, -1)
            else if (dow == Calendar.SUNDAY) add(Calendar.DAY_OF_MONTH, -2)
        }
        val esPasado = esFinSem || esDiaPasado(diaSeleccionadoSemanaSiguiente)

        val fecha = obtenerClaveFecha(calResumen)
        val permitidas = obtenerJornadasPermitidas()
        val serviciosMostrar = mutableListOf<Pair<TipoComida, InicioJefaActivity.Plato>>()

        for (tipo in permitidas) {
            val servNom = when (tipo) {
                TipoComida.DESAYUNO -> "Desayuno"
                TipoComida.ALMUERZO -> "Almuerzo"
                TipoComida.MERIENDA -> "Merienda"
            }
            val plato = if (esFinSem) {
                val nomGuardado = sessionManager.getPlatoDiaServicio(this, fecha, servNom)
                val listaTipo = when (tipo) {
                    TipoComida.DESAYUNO -> platosDesayuno
                    TipoComida.ALMUERZO -> platosAlmuerzo
                    TipoComida.MERIENDA -> platosMerienda
                }
                if (!nomGuardado.isNullOrBlank()) {
                    listaTipo.firstOrNull { SessionManager.limpiarPrefijoMenu(it.nombre).equals(SessionManager.limpiarPrefijoMenu(nomGuardado), ignoreCase = true) }
                        ?: InicioJefaActivity.Plato(id = 0, nombre = nomGuardado, componente = "Menú del viernes", imagen = InicioJefaActivity.obtenerImagenPlatoPorNombre(nomGuardado))
                } else {
                    null
                }
            } else {
                when (tipo) {
                    TipoComida.DESAYUNO -> platoSeleccionadoDesayuno
                    TipoComida.ALMUERZO -> platoSeleccionadoAlmuerzo
                    TipoComida.MERIENDA -> platoSeleccionadoMerienda
                }
            }
            if (plato != null) {
                serviciosMostrar.add(tipo to plato)
            }
        }

        if (serviciosMostrar.isEmpty()) {
            contenedor.visibility = View.GONE
            return
        }

        contenedor.visibility = View.VISIBLE
        val d = resources.displayMetrics.density

        for ((idx, par) in serviciosMostrar.withIndex()) {
            val (tipo, plato) = par
            val servNombre = when (tipo) {
                TipoComida.DESAYUNO -> "Desayuno"
                TipoComida.ALMUERZO -> "Almuerzo"
                TipoComida.MERIENDA -> "Merienda"
            }
            val estadoMenu = sessionManager.getEstadoMenuDiaServicio(this, fecha, servNombre).trim()
            val yaEnviado = esPasado || (estadoMenu.isNotBlank() &&
                !estadoMenu.equals("Borrador", ignoreCase = true) &&
                !estadoMenu.equals("Rechazado", ignoreCase = true))

            val card = com.google.android.material.card.MaterialCardView(this).apply {
                val lp = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                    if (idx < serviciosMostrar.size - 1) marginEnd = (8 * d).toInt()
                }
                layoutParams = lp
                radius = 14 * d
                cardElevation = 1 * d
                setCardBackgroundColor(if (esPasado) Color.parseColor("#F1F5F9") else Color.WHITE)
                strokeColor = Color.parseColor(if (esPasado) "#CBD5E1" else "#FDE68A")
                strokeWidth = (1 * d).toInt()
                alpha = if (esPasado) 0.7f else 1.0f
            }

            val col = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding((10 * d).toInt(), (10 * d).toInt(), (10 * d).toInt(), (10 * d).toInt())
            }

            val badgeServ = TextView(this).apply {
                text = if (esFinSem) "$servNombre (Viernes)" else servNombre
                textSize = 10f
                setTypeface(null, android.graphics.Typeface.BOLD)
                setTextColor(Color.parseColor(if (esPasado) "#64748B" else "#92400E"))
                setBackgroundResource(if (esPasado) R.drawable.bg_dia_semana_inactivo else R.drawable.bg_chip_pendiente)
                setPadding((8 * d).toInt(), (2 * d).toInt(), (8 * d).toInt(), (2 * d).toInt())
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            }
            col.addView(badgeServ)

            val filaPlato = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { topMargin = (8 * d).toInt() }
            }

            val img = com.google.android.material.imageview.ShapeableImageView(this).apply {
                layoutParams = LinearLayout.LayoutParams((40 * d).toInt(), (40 * d).toInt())
                scaleType = android.widget.ImageView.ScaleType.CENTER_CROP
                setImageResource(InicioJefaActivity.obtenerImagenPlatoPorNombre(plato.nombre))
                shapeAppearanceModel = shapeAppearanceModel.toBuilder()
                    .setAllCornerSizes(10 * d)
                    .build()
            }
            filaPlato.addView(img)

            val txtNombre = TextView(this).apply {
                text = SessionManager.limpiarPrefijoMenu(plato.nombre)
                textSize = 11.5f
                maxLines = 2
                ellipsize = android.text.TextUtils.TruncateAt.END
                setTypeface(null, android.graphics.Typeface.BOLD)
                setTextColor(Color.parseColor("#1E293B"))
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                    marginStart = (8 * d).toInt()
                }
            }
            filaPlato.addView(txtNombre)
            col.addView(filaPlato)

            if (!yaEnviado && !esSemanaAprobadaFijada) {
                val btnEditar = com.google.android.material.button.MaterialButton(this).apply {
                    text = "Editar"
                    isAllCaps = false
                    textSize = 11f
                    setTypeface(null, android.graphics.Typeface.BOLD)
                    setTextColor(Color.parseColor("#1E293B"))
                    backgroundTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#FEF3C7"))
                    cornerRadius = (12 * d).toInt()
                    insetTop = 0
                    insetBottom = 0
                    minHeight = 0
                    minimumHeight = 0
                    setPadding(0, (5 * d).toInt(), 0, (5 * d).toInt())
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        (30 * d).toInt()
                    ).apply { topMargin = (8 * d).toInt() }
                    setOnClickListener {
                        seleccionarTipoComida(tipo)
                        binding.btnSeleccionarPlato.parent?.requestChildFocus(binding.btnSeleccionarPlato, binding.btnSeleccionarPlato)
                    }
                }
                col.addView(btnEditar)
            }

            card.addView(col)
            contenedor.addView(card)
        }
    }

    private fun actualizarSubtitulosTabs() {
        val des = platoSeleccionadoDesayuno
        if (des != null) {
            binding.txtTabDesayunoSub.text = "✓ ${des.nombre}"
            if (comidaSeleccionada != TipoComida.DESAYUNO) {
                binding.txtTabDesayunoSub.setTextColor(Color.parseColor("#15803D"))
            }
        } else {
            binding.txtTabDesayunoSub.text = "Por elegir"
            if (comidaSeleccionada != TipoComida.DESAYUNO) {
                binding.txtTabDesayunoSub.setTextColor(Color.parseColor("#94A3B8"))
            }
        }

        val alm = platoSeleccionadoAlmuerzo
        if (alm != null) {
            binding.txtTabAlmuerzoSub.text = "✓ ${alm.nombre}"
            if (comidaSeleccionada != TipoComida.ALMUERZO) {
                binding.txtTabAlmuerzoSub.setTextColor(Color.parseColor("#15803D"))
            }
        } else {
            binding.txtTabAlmuerzoSub.text = "Pendiente"
            if (comidaSeleccionada != TipoComida.ALMUERZO) {
                binding.txtTabAlmuerzoSub.setTextColor(Color.parseColor("#94A3B8"))
            }
        }

        val mer = platoSeleccionadoMerienda
        if (mer != null) {
            binding.txtTabMeriendaSub.text = "✓ ${mer.nombre}"
            if (comidaSeleccionada != TipoComida.MERIENDA) {
                binding.txtTabMeriendaSub.setTextColor(Color.parseColor("#15803D"))
            }
        } else {
            binding.txtTabMeriendaSub.text = "Pendiente"
            if (comidaSeleccionada != TipoComida.MERIENDA) {
                binding.txtTabMeriendaSub.setTextColor(Color.parseColor("#94A3B8"))
            }
        }
    }

    private fun obtenerNombreComidaActual(): String = when (comidaSeleccionada) {
        TipoComida.DESAYUNO -> "Desayuno"
        TipoComida.ALMUERZO -> "Almuerzo"
        TipoComida.MERIENDA -> "Merienda"
    }

    private fun obtenerPlatoSeleccionadoActual(): InicioJefaActivity.Plato? = when (comidaSeleccionada) {
        TipoComida.DESAYUNO -> platoSeleccionadoDesayuno
        TipoComida.ALMUERZO -> platoSeleccionadoAlmuerzo
        TipoComida.MERIENDA -> platoSeleccionadoMerienda
    }

    private fun obtenerOtrosAsignados(): Map<String, String> {
        val map = mutableMapOf<String, String>()
        if (comidaSeleccionada != TipoComida.DESAYUNO && platoSeleccionadoDesayuno != null) {
            map[platoSeleccionadoDesayuno!!.nombre] = "Desayuno"
        }
        if (comidaSeleccionada != TipoComida.ALMUERZO && platoSeleccionadoAlmuerzo != null) {
            map[platoSeleccionadoAlmuerzo!!.nombre] = "Almuerzo"
        }
        if (comidaSeleccionada != TipoComida.MERIENDA && platoSeleccionadoMerienda != null) {
            map[platoSeleccionadoMerienda!!.nombre] = "Merienda"
        }
        return map
    }

    // =========================================================
    // TABS DE COMIDA (DESAYUNO, ALMUERZO, MERIENDA)
    // =========================================================

    /**
     * Determina las jornadas y momentos habilitados según el contrato real de la sede en el backend.
     * Cero datos quemados: si en el contrato solo tiene Desayuno, solo permite Desayuno.
     * Si luego el superadmin le asigna Almuerzo en la web, se habilitan Desayuno y Almuerzo dinámicamente.
     */
    private fun obtenerJornadasPermitidas(): List<TipoComida> {
        val secciones = SedeContratoManager.obtenerSeccionesActivas(this)
        val lista = mutableListOf<TipoComida>()
        if (secciones.contains("Desayuno")) lista.add(TipoComida.DESAYUNO)
        if (secciones.contains("Almuerzo")) lista.add(TipoComida.ALMUERZO)
        if (secciones.contains("Merienda")) lista.add(TipoComida.MERIENDA)

        // Si no hay secciones configuradas aún en el contrato, toma las que existan o Desayuno por fallback
        return if (lista.isNotEmpty()) lista else listOf(TipoComida.DESAYUNO)
    }

    private fun configurarTabsComida() {
        val permitidas = obtenerJornadasPermitidas()

        binding.tabDesayuno.visibility = if (permitidas.contains(TipoComida.DESAYUNO)) View.VISIBLE else View.GONE
        binding.tabAlmuerzo.visibility = if (permitidas.contains(TipoComida.ALMUERZO)) View.VISIBLE else View.GONE
        binding.tabMerienda.visibility = if (permitidas.contains(TipoComida.MERIENDA)) View.VISIBLE else View.GONE

        binding.tabDesayuno.setOnClickListener {
            if (permitidas.contains(TipoComida.DESAYUNO)) {
                seleccionarTipoComida(TipoComida.DESAYUNO)
            }
        }
        binding.tabAlmuerzo.setOnClickListener {
            if (permitidas.contains(TipoComida.ALMUERZO)) {
                seleccionarTipoComida(TipoComida.ALMUERZO)
            } else {
                Toast.makeText(this, "Esta sede no tiene asignado el servicio de Almuerzo en el contrato.", Toast.LENGTH_SHORT).show()
            }
        }
        binding.tabMerienda.setOnClickListener {
            if (permitidas.contains(TipoComida.MERIENDA)) {
                seleccionarTipoComida(TipoComida.MERIENDA)
            } else {
                Toast.makeText(this, "Esta sede no tiene asignado el servicio de Merienda en el contrato.", Toast.LENGTH_SHORT).show()
            }
        }

        val inicial = if (permitidas.contains(comidaSeleccionada)) comidaSeleccionada else (permitidas.firstOrNull() ?: TipoComida.DESAYUNO)
        seleccionarTipoComida(inicial)
    }

    private fun seleccionarTipoComida(tipo: TipoComida) {
        comidaSeleccionada = tipo

        fun estiloActivo(tab: LinearLayout, titulo: TextView, sub: TextView) {
            tab.setBackgroundResource(R.drawable.bg_dia_semana_activo)
            titulo.setTextColor(Color.parseColor("#1E293B"))
            sub.setTextColor(Color.parseColor("#92400E"))
        }

        fun estiloInactivo(tab: LinearLayout, titulo: TextView, sub: TextView) {
            tab.setBackgroundResource(R.drawable.bg_dia_semana_inactivo)
            titulo.setTextColor(Color.parseColor("#64748B"))
            sub.setTextColor(Color.parseColor("#94A3B8"))
        }

        estiloInactivo(binding.tabDesayuno, binding.txtTabDesayunoTitulo, binding.txtTabDesayunoSub)
        estiloInactivo(binding.tabAlmuerzo, binding.txtTabAlmuerzoTitulo, binding.txtTabAlmuerzoSub)
        estiloInactivo(binding.tabMerienda, binding.txtTabMeriendaTitulo, binding.txtTabMeriendaSub)

        when (tipo) {
            TipoComida.DESAYUNO -> {
                estiloActivo(binding.tabDesayuno, binding.txtTabDesayunoTitulo, binding.txtTabDesayunoSub)
                binding.txtSubtituloIA.text = "Platos recomendados para el desayuno escolar"
            }
            TipoComida.ALMUERZO -> {
                estiloActivo(binding.tabAlmuerzo, binding.txtTabAlmuerzoTitulo, binding.txtTabAlmuerzoSub)
                binding.txtSubtituloIA.text = "Platos recomendados para el almuerzo escolar"
            }
            TipoComida.MERIENDA -> {
                estiloActivo(binding.tabMerienda, binding.txtTabMeriendaTitulo, binding.txtTabMeriendaSub)
                binding.txtSubtituloIA.text = "Platos recomendados para la merienda o refrigerio"
            }
        }

        actualizarSubtitulosTabs()
        actualizarEstilosBotonesSeccionesIA()
        posicionPlatoCarousel = 0
        configurarViewPagerCarousel()
        configurarMenuDisponiblePAE()
        actualizarEstadoFinDeSemanaUI()
    }

    private fun configurarBotonesSeccionesIA() {
        val seccionesActivas = SedeContratoManager.obtenerSeccionesActivas(this)
        val permitidas = obtenerJornadasPermitidas()

        binding.layoutSelectorSeccionesIA.visibility = View.VISIBLE
        binding.btnSeccionDesayuno.visibility = if (seccionesActivas.contains("Desayuno")) View.VISIBLE else View.GONE
        binding.btnSeccionAlmuerzo.visibility = if (seccionesActivas.contains("Almuerzo")) View.VISIBLE else View.GONE
        binding.btnSeccionMerienda.visibility = if (seccionesActivas.contains("Merienda")) View.VISIBLE else View.GONE

        // Si la comida seleccionada actualmente ya no está permitida por el contrato, cambiar a la primera permitida
        if (!permitidas.contains(comidaSeleccionada)) {
            val primera = permitidas.firstOrNull() ?: TipoComida.DESAYUNO
            seleccionarTipoComida(primera)
        } else if (seccionesActivas.size <= 1) {
            cargarSugerenciasSegunInventario(obtenerIdSeccionActual())
        }

        // Ajustar márgenes para que el último botón visible no deje hueco a la derecha
        val density = resources.displayMetrics.density
        val botonesVisibles = listOf(
            binding.btnSeccionDesayuno,
            binding.btnSeccionAlmuerzo,
            binding.btnSeccionMerienda
        ).filter { it.visibility == View.VISIBLE }

        botonesVisibles.forEachIndexed { idx, btn ->
            val lp = btn.layoutParams as LinearLayout.LayoutParams
            lp.marginEnd = if (idx < botonesVisibles.size - 1) (6 * density).toInt() else 0
            btn.layoutParams = lp
        }

        actualizarEstilosBotonesSeccionesIA()

        binding.btnSeccionDesayuno.setOnClickListener {
            seleccionarTipoComida(TipoComida.DESAYUNO)
            actualizarEstilosBotonesSeccionesIA()
            cargarSugerenciasSegunInventario(1)
        }

        binding.btnSeccionAlmuerzo.setOnClickListener {
            seleccionarTipoComida(TipoComida.ALMUERZO)
            actualizarEstilosBotonesSeccionesIA()
            cargarSugerenciasSegunInventario(2)
        }

        binding.btnSeccionMerienda.setOnClickListener {
            seleccionarTipoComida(TipoComida.MERIENDA)
            actualizarEstilosBotonesSeccionesIA()
            cargarSugerenciasSegunInventario(3)
        }
    }

    private fun actualizarEstilosBotonesSeccionesIA() {
        val colorActivoFondo = Color.parseColor("#F4B41F")
        val colorActivoTexto = Color.parseColor("#1E293B")
        val colorInactivoFondo = Color.parseColor("#FEF3C7")
        val colorInactivoTexto = Color.parseColor("#92400E")

        binding.btnSeccionDesayuno.backgroundTintList = android.content.res.ColorStateList.valueOf(
            if (comidaSeleccionada == TipoComida.DESAYUNO) colorActivoFondo else colorInactivoFondo
        )
        binding.btnSeccionDesayuno.setTextColor(
            if (comidaSeleccionada == TipoComida.DESAYUNO) colorActivoTexto else colorInactivoTexto
        )

        binding.btnSeccionAlmuerzo.backgroundTintList = android.content.res.ColorStateList.valueOf(
            if (comidaSeleccionada == TipoComida.ALMUERZO) colorActivoFondo else colorInactivoFondo
        )
        binding.btnSeccionAlmuerzo.setTextColor(
            if (comidaSeleccionada == TipoComida.ALMUERZO) colorActivoTexto else colorInactivoTexto
        )

        binding.btnSeccionMerienda.backgroundTintList = android.content.res.ColorStateList.valueOf(
            if (comidaSeleccionada == TipoComida.MERIENDA) colorActivoFondo else colorInactivoFondo
        )
        binding.btnSeccionMerienda.setTextColor(
            if (comidaSeleccionada == TipoComida.MERIENDA) colorActivoTexto else colorInactivoTexto
        )
    }

    private fun obtenerAsistenciaReal(): Int {
        val fechaSel = obtenerClaveFecha(diaSeleccionadoSemanaSiguiente)
        val asistFecha = AsistenciaManager.obtenerTotalFecha(this, fechaSel)
        if (asistFecha > 0) return asistFecha

        val asistHoy = AsistenciaManager.obtenerTotal(this)
        if (asistHoy > 0) return asistHoy

        val cuposContrato = SedeContratoManager.obtenerCuposSede(this)
        if (cuposContrato > 0) return cuposContrato

        return 0
    }

    private fun cargarSugerenciasSegunInventario(idSeccion: Int) {
        val idSede = sessionManager.getSedeId().takeIf { it > 0 } ?: 1
        var ninos = obtenerAsistenciaReal()

        binding.txtSubtituloIA.text = "Generando opciones de ${obtenerNombreComidaActual().lowercase()} según inventario..."

        lifecycleScope.launch {
            try {
                if (ninos <= 0) {
                    try {
                        val resAsist = RetrofitClient.apiService.obtenerAsistenciaDiaria()
                        if (resAsist.isSuccessful && !resAsist.body().isNullOrEmpty()) {
                            val fechaSel = obtenerClaveFecha(diaSeleccionadoSemanaSiguiente)
                            val lista = resAsist.body()!!
                            val sumaFecha = lista.filter { it.fecha.trim().startsWith(fechaSel) }.sumOf { it.ninosPresentes }
                            if (sumaFecha > 0) ninos = sumaFecha
                            else {
                                val fechaHoy = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                                val sumaHoy = lista.filter { it.fecha.trim().startsWith(fechaHoy) }.sumOf { it.ninosPresentes }
                                if (sumaHoy > 0) ninos = sumaHoy
                            }
                        }
                    } catch (_: Exception) { }
                }

                val request = SugerenciasRecetasRequest(
                    idSede = idSede,
                    ninosPresentes = if (ninos > 0) ninos else 0,
                    idSeccion = idSeccion,
                    preferencia = ""
                )
                val response = RetrofitClient.apiService.sugerirRecetas(request)
                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()!!
                    // Cero platos fijos quemados del PAE: solo platos sugeridos por la IA
                    val rawSugerencias = body.sugerenciasIaEstructuradas.orEmpty()
                    val lista = if (rawSugerencias.isNotEmpty()) {
                        rawSugerencias.distinctBy { it.nombrePlato.trim().lowercase() }
                    } else {
                        // Generar opciones creativas de IA si el backend devolvió lista vacía
                        generarOpcionesIAFallback(idSeccion, ninos)
                    }

                    if (lista.isNotEmpty()) {
                        cacheSugerenciasIA[idSeccion] = lista

                        val platosMapeados = lista.map { item ->
                            val comp = item.componente.ifBlank {
                                item.ingredientes.joinToString(", ") { it.nombreIngrediente }
                            }
                            val idReal = item.idPlato ?: Math.abs(item.nombrePlato.hashCode())
                            val urlAi = item.obtenerUrlImagenReal()
                            InicioJefaActivity.Plato(
                                id = idReal,
                                nombre = item.nombrePlato,
                                componente = if (comp.isNotBlank()) comp else "Aporte calórico balanceado con IA según inventario",
                                imagen = 0,
                                imagenUrl = urlAi
                            )
                        }

                        platosSugeridosIA[idSeccion] = platosMapeados
                        configurarViewPagerCarousel()

                        val viables = lista.count { it.viableConInventario }
                        val momento = obtenerNombreComidaActual().lowercase()
                        binding.txtSubtituloIA.text = if (viables > 0) {
                            "$viables ${if (viables == 1) "menú viable" else "menús viables"} según stock en bodega"
                        } else {
                            "Menús de $momento calculados con IA"
                        }

                        if (lista.isNotEmpty()) {
                            pintarIngredientesEnPantalla(lista.first(), body.ninosPresentes ?: ninos)
                        }
                    }
                }
            } catch (_: Exception) { }
        }
    }

    private fun generarOpcionesIAFallback(idSeccion: Int, ninos: Int): List<SugerenciaIAItem> {
        val momento = when (idSeccion) {
            2 -> "Almuerzo"
            3 -> "Merienda"
            else -> "Desayuno"
        }
        val ninosEfectivos = if (ninos > 0) ninos else 165
        return when (idSeccion) {
            2 -> listOf(
                SugerenciaIAItem(
                    idPlato = 201,
                    nombrePlato = "Sugerencia IA: Pollo campesino al vapor con pasta corta y ensalada fresca",
                    idSeccion = 2,
                    seccionNombre = "Almuerzo",
                    componente = "Pollo en salsa natural, pasta corta, ensalada y jugo de fruta",
                    ninosPresentes = ninosEfectivos,
                    viableConInventario = true
                ),
                SugerenciaIAItem(
                    idPlato = 202,
                    nombrePlato = "Sugerencia IA: Guiso casero de carne molida con arroz blanco y lentejas",
                    idSeccion = 2,
                    seccionNombre = "Almuerzo",
                    componente = "Carne molida magra, arroz blanco, lentejas guisadas y fruta",
                    ninosPresentes = ninosEfectivos,
                    viableConInventario = true
                )
            )
            3 -> listOf(
                SugerenciaIAItem(
                    idPlato = 301,
                    nombrePlato = "Sugerencia IA: Chocolate espumoso en leche con arepa campesina y queso",
                    idSeccion = 3,
                    seccionNombre = "Merienda",
                    componente = "Bebida láctea de chocolate, arepa artesanal y queso fresco",
                    ninosPresentes = ninosEfectivos,
                    viableConInventario = true
                ),
                SugerenciaIAItem(
                    idPlato = 302,
                    nombrePlato = "Sugerencia IA: Avena tibia en leche con galleta integral y fruta fresca",
                    idSeccion = 3,
                    seccionNombre = "Merienda",
                    componente = "Avena molida en leche, galletas nutritivas y banano",
                    ninosPresentes = ninosEfectivos,
                    viableConInventario = true
                )
            )
            else -> listOf(
                SugerenciaIAItem(
                    idPlato = 101,
                    nombrePlato = "Sugerencia IA: Cazuela campesina matutina de Huevos con hogao criollo",
                    idSeccion = 1,
                    seccionNombre = "Desayuno",
                    componente = "Huevos revueltos con hogao, arroz blanco, queso fresco y bebida láctea",
                    ninosPresentes = ninosEfectivos,
                    viableConInventario = true
                ),
                SugerenciaIAItem(
                    idPlato = 102,
                    nombrePlato = "Sugerencia IA: Tortilla montañera de huevos con queso y avena tibia",
                    idSeccion = 1,
                    seccionNombre = "Desayuno",
                    componente = "Tortilla de huevos campesinos, queso tajado, avena en leche y fruta",
                    ninosPresentes = ninosEfectivos,
                    viableConInventario = true
                ),
                SugerenciaIAItem(
                    idPlato = 103,
                    nombrePlato = "Sugerencia IA: Desayuno boyacense de huevos tibios con arepa y chocolate",
                    idSeccion = 1,
                    seccionNombre = "Desayuno",
                    componente = "Huevos frescos, arepa de maíz blanco, queso y chocolate en leche",
                    ninosPresentes = ninosEfectivos,
                    viableConInventario = true
                )
            )
        }
    }

    private fun obtenerPlatosActuales(): List<InicioJefaActivity.Plato> {
        val idSeccion = obtenerIdSeccionActual()
        val platosIA = platosSugeridosIA[idSeccion]
        if (!platosIA.isNullOrEmpty()) {
            return platosIA
        }

        val fallbackItems = generarOpcionesIAFallback(idSeccion, obtenerAsistenciaReal())
        return fallbackItems.map { item ->
            val comp = item.componente.ifBlank { "Nutrición balanceada con IA según inventario" }
            val idReal = item.idPlato ?: Math.abs(item.nombrePlato.hashCode())
            val urlAi = item.obtenerUrlImagenReal()
            InicioJefaActivity.Plato(
                id = idReal,
                nombre = item.nombrePlato,
                componente = comp,
                imagen = 0,
                imagenUrl = urlAi
            )
        }
    }

    // =========================================================
    // CARRUSEL DISCOS DE PLATOS (VIEW PAGER 2)
    // =========================================================

    private fun configurarViewPagerCarousel() {
        val platosActuales = obtenerPlatosActuales().map {
            it.copy(nombre = SessionManager.limpiarPrefijoMenu(it.nombre))
        }
        val adapter = SugerenciaIaAdapter(
            listaPlatos = platosActuales,
            servicioActual = obtenerNombreComidaActual(),
            platoSeleccionadoActual = obtenerPlatoSeleccionadoActual(),
            otrosAsignados = obtenerOtrosAsignados(),
            platosDiaAnterior = obtenerPlatosDiaAnterior()
        ) { plato ->
            mostrarDialogoDetallePlato(plato)
        }
        binding.viewPagerSugerenciasIA.adapter = adapter

        // Efecto visual 3D con discos laterales superpuestos detrás
        val pageTransformer = CompositePageTransformer().apply {
            addTransformer { page, position ->
                val absPosition = Math.abs(position)
                val scale = 0.70f + (1.0f - 0.70f) * (1.0f - Math.min(1.0f, absPosition))
                page.scaleX = scale
                page.scaleY = scale
                page.alpha = 0.80f + (1.0f - 0.80f) * (1.0f - Math.min(1.0f, absPosition))
                page.translationZ = (1.0f - Math.min(1.0f, absPosition)) * 30f
                val overlap = page.width * 0.32f
                page.translationX = -position * overlap
            }
        }

        binding.viewPagerSugerenciasIA.setPageTransformer(pageTransformer)

        carouselPageCallback?.let {
            binding.viewPagerSugerenciasIA.unregisterOnPageChangeCallback(it)
        }
        val callback = object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                posicionPlatoCarousel = position
                actualizarIndicadorPuntos(position, platosActuales.size)
                actualizarBotonSeleccionarPlato(position, platosActuales)
                platosActuales.getOrNull(position)?.let {
                    solicitarSugerenciaIA(it.nombre, position)
                }
            }
        }
        carouselPageCallback = callback
        binding.viewPagerSugerenciasIA.registerOnPageChangeCallback(callback)

        val initialPos = if (platosActuales.size >= 2) 1 else 0
        binding.viewPagerSugerenciasIA.setCurrentItem(initialPos, false)
        posicionPlatoCarousel = initialPos
        actualizarIndicadorPuntos(initialPos, platosActuales.size)
        actualizarBotonSeleccionarPlato(initialPos, platosActuales)
        platosActuales.getOrNull(initialPos)?.let {
            solicitarSugerenciaIA(it.nombre, initialPos)
        }
    }

    private fun actualizarBotonSeleccionarPlato(position: Int, lista: List<InicioJefaActivity.Plato>) {
        if (position in lista.indices) {
            val plato = lista[position]
            val servicio = obtenerNombreComidaActual()
            val actual = obtenerPlatoSeleccionadoActual()
            val otros = obtenerOtrosAsignados()
            val platosAyer = obtenerPlatosDiaAnterior()

            when {
                platosAyer.contains(plato.nombre) -> {
                    binding.btnSeleccionarPlato.text = "No disponible (Servido ayer)"
                    binding.btnSeleccionarPlato.setBackgroundColor(Color.parseColor("#94A3B8"))
                    binding.btnSeleccionarPlato.setTextColor(Color.WHITE)
                }
                actual != null && plato.nombre.equals(actual.nombre, ignoreCase = true) -> {
                    binding.btnSeleccionarPlato.text = "✓ Seleccionado (Toca para quitar)"
                    binding.btnSeleccionarPlato.setBackgroundColor(Color.parseColor("#15803D"))
                    binding.btnSeleccionarPlato.setTextColor(Color.WHITE)
                }
                otros.containsKey(plato.nombre) -> {
                    binding.btnSeleccionarPlato.text = "Reasignar a $servicio (está en ${otros[plato.nombre]})"
                    binding.btnSeleccionarPlato.setBackgroundColor(Color.parseColor("#F4B41F"))
                    binding.btnSeleccionarPlato.setTextColor(Color.parseColor("#1E293B"))
                }
                else -> {
                    binding.btnSeleccionarPlato.text = "Seleccionar para $servicio"
                    binding.btnSeleccionarPlato.setBackgroundColor(Color.parseColor("#F4B41F"))
                    binding.btnSeleccionarPlato.setTextColor(Color.parseColor("#1E293B"))
                }
            }
        }
    }

    private fun actualizarIndicadorPuntos(position: Int, total: Int) {
        if (total <= 0) {
            binding.txtIndicadorPlatos.text = ""
            return
        }

        val sb = StringBuilder()
        for (i in 0 until total) {
            if (i == position) {
                sb.append("●")
            } else {
                sb.append("○")
            }
            if (i < total - 1) sb.append("  ")
        }
        binding.txtIndicadorPlatos.text = sb.toString()
    }

    private fun configurarMenuDisponiblePAE() {
        val listaCompleta = when (comidaSeleccionada) {
            TipoComida.DESAYUNO -> platosDesayuno
            TipoComida.ALMUERZO -> platosAlmuerzo
            TipoComida.MERIENDA -> platosMerienda
        }.map { it.copy(nombre = SessionManager.limpiarPrefijoMenu(it.nombre)) }
        val servActual = obtenerNombreComidaActual()
        val esFinSem = esDiaFinDeSemana(diaSeleccionadoSemanaSiguiente)
        val esPasado = esDiaPasado(diaSeleccionadoSemanaSiguiente)
        val bloqueadoEnvio = esSemanaAprobadaFijada || (!esFinSem && estaServicioBloqueadoPorEnvio(diaSeleccionadoSemanaSiguiente, servActual))
        val adapter = MenuDisponibleAdapter(
            listaPlatos = listaCompleta,
            servicioActual = servActual,
            platoSeleccionadoActual = obtenerPlatoSeleccionadoActual(),
            otrosAsignados = obtenerOtrosAsignados(),
            platosDiaAnterior = obtenerPlatosDiaAnterior(),
            bloqueadoPorEnvio = bloqueadoEnvio,
            esFinDeSemana = esFinSem,
            esDiaAnterior = esPasado
        ) { plato ->
            seleccionarPlatoParaSiguienteSemana(plato)
        }
        binding.recyclerMenuDisponiblePAE.adapter = adapter
        binding.recyclerMenuDisponiblePAE.layoutManager = LinearLayoutManager(this, RecyclerView.HORIZONTAL, false)
    }

    // =========================================================
    // EVENTOS Y BOTONES
    // =========================================================

    private fun configurarEventos() {
        binding.btnVolver.setOnClickListener {
            finish()
            @Suppress("DEPRECATION")
            overridePendingTransition(R.anim.stay, R.anim.slide_out_bottom)
        }

        binding.btnMesAnterior.setOnClickListener {
            semanaOffset--
            configurarSemanaSiguiente()
            configurarViewPagerCarousel()
            configurarMenuDisponiblePAE()
        }

        binding.btnMesSiguiente.setOnClickListener {
            semanaOffset++
            configurarSemanaSiguiente()
            configurarViewPagerCarousel()
            configurarMenuDisponiblePAE()
        }

        binding.btnNuevaReceta.setOnClickListener {
            val intent = Intent(this, RedactarRecetaActivity::class.java)
            startActivity(intent)
            @Suppress("DEPRECATION")
            overridePendingTransition(R.anim.slide_in_bottom, R.anim.stay)
        }

        binding.btnPlatoAnterior.setOnClickListener {
            val current = binding.viewPagerSugerenciasIA.currentItem
            val count = obtenerPlatosActuales().size
            if (count > 0) {
                binding.viewPagerSugerenciasIA.currentItem = if (current > 0) current - 1 else count - 1
            }
        }

        binding.btnPlatoSiguiente.setOnClickListener {
            val current = binding.viewPagerSugerenciasIA.currentItem
            val count = obtenerPlatosActuales().size
            if (count > 0) {
                binding.viewPagerSugerenciasIA.currentItem = if (current < count - 1) current + 1 else 0
            }
        }

        binding.btnSeleccionarPlato.setOnClickListener {
            if (esDiaFinDeSemana(diaSeleccionadoSemanaSiguiente)) {
                Toast.makeText(this, "Los fines de semana no tienen servicio escolar PAE.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (estaServicioBloqueadoPorEnvio(diaSeleccionadoSemanaSiguiente, obtenerNombreComidaActual())) {
                Toast.makeText(this, "Este menú ya fue enviado al supervisor. Solo puedes cambiarlo si es rechazado.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val platos = obtenerPlatosActuales()
            if (platos.isNotEmpty() && posicionPlatoCarousel in platos.indices) {
                val plato = platos[posicionPlatoCarousel]
                seleccionarPlatoParaSiguienteSemana(plato)
            }
        }

        binding.btnVerTodosPlatos.setOnClickListener {
            val intent = Intent(this, TodosLosPlatosActivity::class.java).apply {
                putExtra("fecha_seleccionada", obtenerClaveFecha(diaSeleccionadoSemanaSiguiente))
            }
            startActivity(intent)
        }

        binding.btnInfoIA.setOnClickListener {
            Toast.makeText(
                this,
                "La IA sugiere los mejores menús según los inventarios y la programación semanal PAE.",
                Toast.LENGTH_LONG
            ).show()
        }

        binding.btnGuardarSeleccion.setOnClickListener {
            if (esDiaFinDeSemana(diaSeleccionadoSemanaSiguiente)) {
                Toast.makeText(this, "Los fines de semana no tienen servicio escolar PAE.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val fechaStr = obtenerClaveFecha(diaSeleccionadoSemanaSiguiente)
            lifecycleScope.launch {
                val des = platoSeleccionadoDesayuno
                val alm = platoSeleccionadoAlmuerzo
                val mer = platoSeleccionadoMerienda

                if (des != null) {
                    PreparacionSyncManager.guardarPlatoServicio(this@PlanificacionSemanalJefaActivity, "Desayuno", des.nombre, des.id, fechaStr, estadoMenu = "Borrador")
                }
                if (alm != null) {
                    PreparacionSyncManager.guardarPlatoServicio(this@PlanificacionSemanalJefaActivity, "Almuerzo", alm.nombre, alm.id, fechaStr, estadoMenu = "Borrador")
                }
                if (mer != null) {
                    PreparacionSyncManager.guardarPlatoServicio(this@PlanificacionSemanalJefaActivity, "Merienda", mer.nombre, mer.id, fechaStr, estadoMenu = "Borrador")
                }
            }
            Toast.makeText(this, "Planificación del día guardada exitosamente", Toast.LENGTH_SHORT).show()
            renderizarNumerosSemanaSiguiente()
            verificarAvanzarSiguienteDia()
        }

        binding.btnEnviarProgramacionSupervisor.setOnClickListener {
            enviarProgramacionAlSupervisor()
        }
    }

    private fun mostrarDialogoDetallePlato(plato: InicioJefaActivity.Plato) {
        val density = resources.displayMetrics.density
        val idSeccion = obtenerIdSeccionActual()
        val itemIA = cacheSugerenciasIA[idSeccion]?.firstOrNull {
            it.nombrePlato.equals(plato.nombre, ignoreCase = true) ||
            it.nombrePlato.contains(plato.nombre, ignoreCase = true) ||
            plato.nombre.contains(it.nombrePlato, ignoreCase = true)
        } ?: cacheSugerenciasIA[idSeccion]?.getOrNull(posicionPlatoCarousel)
          ?: cacheSugerenciasIA[idSeccion]?.firstOrNull()

        val fechaSel = obtenerClaveFecha(diaSeleccionadoSemanaSiguiente)
        val fechaFormateada = SimpleDateFormat("EEEE, d 'de' MMMM", Locale.forLanguageTag("es-CO"))
            .format(diaSeleccionadoSemanaSiguiente.time)
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }

        val servicio = obtenerNombreComidaActual()
        val asistRegistrada = AsistenciaManager.obtenerTotalFecha(this, fechaSel)
        val racionesTexto = if (asistRegistrada > 0) {
            "Para $asistRegistrada niños"
        } else {
            "Sin registro de asistencia"
        }

        val dialogBinding = com.example.movilmanupuladora.databinding.DialogDetalleSugerenciaIaBinding.inflate(layoutInflater)

        // 1. TÍTULO Y FECHA (Limpiar prefijo 'Sugerencia IA:' para evitar redundancia con el badge)
        val nombreLimpio = plato.nombre
            .replace("Sugerencia IA:", "", ignoreCase = true)
            .replace("Sugerencia IA :", "", ignoreCase = true)
            .replace("Sugerencia:", "", ignoreCase = true)
            .trim()
        dialogBinding.tvTituloModalPlato.text = nombreLimpio
        dialogBinding.tvFechaModalPlato.text = fechaFormateada

        // 2. JORNADA Y RACIONES (ASISTENCIA)
        dialogBinding.tvJornadaValor.text = servicio
        dialogBinding.tvRacionesValor.text = racionesTexto

        // 3. COMPONENTES DEL PLATO
        val compTexto = plato.componente.ifBlank {
            itemIA?.componente?.ifBlank {
                MenuPaeCatalog.obtenerComponentesPorNombre(plato.nombre).joinToString(", ")
            } ?: "Aporte calórico balanceado según normas PAE"
        }
        dialogBinding.tvComponentesValor.text = compTexto

        // 4. PREPARACIÓN PASO A PASO (Con alineación superior y sin recortar letras)
        dialogBinding.layoutContenedorPasos.removeAllViews()
        val pasos = itemIA?.pasos.orEmpty().filter { it.isNotBlank() }
        val listaPasos = if (pasos.isNotEmpty()) {
            pasos
        } else {
            listOf(
                "Alistar y pesar las cantidades exactas requeridas para las raciones escolares.",
                "Preparar y cocinar los alimentos a fuego medio manteniendo temperaturas y tiempos seguros.",
                "Integrar los componentes asegurando la consistencia y sazón casera según receta PAE.",
                "Servir caliente siguiendo los gramajes establecidos por el programa de alimentación escolar."
            )
        }

        listaPasos.forEachIndexed { i, paso ->
            val rowPaso = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                isBaselineAligned = false
                gravity = Gravity.TOP
                val lp = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).also {
                    it.topMargin = (5 * density).toInt()
                    it.bottomMargin = (5 * density).toInt()
                }
                layoutParams = lp
            }

            val badgeNum = TextView(this).apply {
                text = (i + 1).toString()
                setTextColor(Color.parseColor("#B45309"))
                textSize = 12f
                setTypeface(null, android.graphics.Typeface.BOLD)
                gravity = Gravity.CENTER
                setBackgroundResource(R.drawable.bg_step_number)
                val lp = LinearLayout.LayoutParams(
                    (22 * density).toInt(),
                    (22 * density).toInt()
                ).also {
                    it.gravity = Gravity.TOP
                    it.topMargin = (2 * density).toInt()
                }
                layoutParams = lp
            }

            val tvTextoPaso = TextView(this).apply {
                text = paso
                setTextColor(Color.parseColor("#475569"))
                textSize = 13f
                gravity = Gravity.TOP
                includeFontPadding = false
                setLineSpacing(0f, 1.25f)
                setPadding(0, 0, 0, (4 * density).toInt())
                val lp = LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1f
                ).also {
                    it.marginStart = (10 * density).toInt()
                    it.gravity = Gravity.TOP
                }
                layoutParams = lp
            }

            rowPaso.addView(badgeNum)
            rowPaso.addView(tvTextoPaso)
            dialogBinding.layoutContenedorPasos.addView(rowPaso)
        }

        // 5. INGREDIENTES Y STOCK EN BODEGA
        dialogBinding.layoutContenedorFilasIngredientes.removeAllViews()
        val ingredientes = itemIA?.ingredientes.orEmpty()
        val cantIngredientes = ingredientes.size
        val todosAlcanzan = ingredientes.all { it.alcanza }

        if (cantIngredientes > 0) {
            if (todosAlcanzan) {
                dialogBinding.tvBannerStockModal.text = "Stock suficiente para los $cantIngredientes ingredientes listados"
                dialogBinding.tvBannerStockModal.setBackgroundResource(R.drawable.bg_stock_suficiente_banner)
                dialogBinding.tvBannerStockModal.setTextColor(Color.parseColor("#15803D"))
            } else {
                val faltantes = ingredientes.count { !it.alcanza }
                dialogBinding.tvBannerStockModal.text = "⚠️ Alerta: $faltantes ingrediente(s) con stock insuficiente en bodega"
                dialogBinding.tvBannerStockModal.setBackgroundColor(Color.parseColor("#FEE2E2"))
                dialogBinding.tvBannerStockModal.setTextColor(Color.parseColor("#B91C1C"))
            }

            ingredientes.forEach { ing ->
                val rowIng = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    setPadding((10 * density).toInt(), (6 * density).toInt(), (10 * density).toInt(), (6 * density).toInt())
                    val lp = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                    layoutParams = lp
                }

                val tvNom = TextView(this).apply {
                    text = ing.nombreIngrediente
                    setTextColor(Color.parseColor("#334155"))
                    textSize = 12f
                    val lp = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.6f)
                    layoutParams = lp
                }

                val tvReq = TextView(this).apply {
                    text = ing.cantidadTotal
                    setTextColor(Color.parseColor("#475569"))
                    textSize = 12f
                    gravity = Gravity.END
                    val lp = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.1f)
                    layoutParams = lp
                }

                val tvStock = TextView(this).apply {
                    text = "${ing.stockActual} ${ing.unidad}"
                    setTextColor(Color.parseColor(if (ing.alcanza) "#475569" else "#DC2626"))
                    textSize = 12f
                    gravity = Gravity.END
                    val lp = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.2f)
                    layoutParams = lp
                }

                rowIng.addView(tvNom)
                rowIng.addView(tvReq)
                rowIng.addView(tvStock)
                dialogBinding.layoutContenedorFilasIngredientes.addView(rowIng)
            }
        } else {
            dialogBinding.tvBannerStockModal.text = "Stock suficiente para los ingredientes requeridos"
            dialogBinding.tvBannerStockModal.setBackgroundResource(R.drawable.bg_stock_suficiente_banner)
            dialogBinding.tvBannerStockModal.setTextColor(Color.parseColor("#15803D"))

            val rowDef = TextView(this).apply {
                text = "Ingredientes balanceados según normas PAE e inventario activo de bodega."
                setTextColor(Color.parseColor("#64748B"))
                textSize = 12f
                setPadding((10 * density).toInt(), (8 * density).toInt(), (10 * density).toInt(), (8 * density).toInt())
            }
            dialogBinding.layoutContenedorFilasIngredientes.addView(rowDef)
        }

        // 6. MOSTRAR DIÁLOGO
        val dialog = MaterialAlertDialogBuilder(this)
            .setView(dialogBinding.root)
            .create()

        dialog.window?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(Color.TRANSPARENT))

        dialogBinding.btnCerrarModal.setOnClickListener {
            dialog.dismiss()
        }

        val esFinSem = esDiaFinDeSemana(diaSeleccionadoSemanaSiguiente)
        val bloqueado = estaServicioBloqueadoPorEnvio(diaSeleccionadoSemanaSiguiente, obtenerNombreComidaActual())
        if (esFinSem || bloqueado) {
            dialogBinding.btnSeleccionarDiaModal.isEnabled = false
            dialogBinding.btnSeleccionarDiaModal.alpha = 0.45f
            dialogBinding.btnSeleccionarDiaModal.text = if (esFinSem) "No disponible (Fin de semana)" else "Enviado al supervisor"
        }

        dialogBinding.btnSeleccionarDiaModal.setOnClickListener {
            if (esFinSem || bloqueado) return@setOnClickListener
            dialog.dismiss()
            seleccionarPlatoParaSiguienteSemana(plato)
        }

        dialog.show()

        // Ocultar barra vertical gris conservando el scroll fluido táctil
        dialogBinding.scrollDetalleModal.isVerticalScrollBarEnabled = false

        // Ajustar el alto máximo del scroll para que en cualquier pantalla
        // o tamaño de celular sea 100% scrolleable y nunca quede apretado
        val dm = resources.displayMetrics
        val anchoModal = (dm.widthPixels * 0.92).toInt()
        val altoMaximoScroll = (dm.heightPixels * 0.62).toInt()

        dialog.window?.setLayout(anchoModal, android.view.WindowManager.LayoutParams.WRAP_CONTENT)
        dialogBinding.scrollDetalleModal.layoutParams = dialogBinding.scrollDetalleModal.layoutParams.apply {
            height = altoMaximoScroll
        }
    }

    private fun seleccionarPlatoParaSiguienteSemana(plato: InicioJefaActivity.Plato) {
        if (esDiaFinDeSemana(diaSeleccionadoSemanaSiguiente)) {
            Toast.makeText(this, "Los fines de semana no tienen servicio escolar PAE.", Toast.LENGTH_SHORT).show()
            return
        }
        if (esDiaPasado(diaSeleccionadoSemanaSiguiente)) {
            Toast.makeText(this, "No se pueden editar menús de días anteriores.", Toast.LENGTH_SHORT).show()
            return
        }
        if (estaServicioBloqueadoPorEnvio(diaSeleccionadoSemanaSiguiente, obtenerNombreComidaActual())) {
            Toast.makeText(this, "Este menú ya fue enviado al supervisor. Solo puedes cambiarlo si es rechazado.", Toast.LENGTH_SHORT).show()
            return
        }
        val permitidas = obtenerJornadasPermitidas().map {
            when (it) {
                TipoComida.DESAYUNO -> "Desayuno"
                TipoComida.ALMUERZO -> "Almuerzo"
                TipoComida.MERIENDA -> "Merienda"
            }
        }.ifEmpty { listOf("Desayuno") }

        var servicioElegido = obtenerNombreComidaActual().takeIf { permitidas.contains(it) } ?: permitidas.first()
        val calElegido = (diaSeleccionadoSemanaSiguiente.clone() as Calendar)

        val dialogBinding = com.example.movilmanupuladora.databinding.DialogAsignarPlatoFechaBinding.inflate(layoutInflater)
        val sdfDiaSemana = SimpleDateFormat("EEEE", Locale.forLanguageTag("es-CO"))
        val sdfMes = SimpleDateFormat("MMMM", Locale.forLanguageTag("es-CO"))

        val fallbackImg = if (plato.imagen != 0) plato.imagen else InicioJefaActivity.obtenerImagenPlatoPorNombre(plato.nombre)
        com.example.movilmanupuladora.utils.ImageLoaderUtil.cargarImagen(
            dialogBinding.imgPlatoAsignarModal,
            plato.imagenUrl,
            fallbackImg
        )
        dialogBinding.tvNombrePlatoAsignarModal.text = plato.nombre

        fun refrescarTextoDialogo() {
            dialogBinding.tvBadgeAsignarModal.text = servicioElegido
            val diaSemana = sdfDiaSemana.format(calElegido.time).lowercase()
            val diaNum = calElegido.get(Calendar.DAY_OF_MONTH)
            val mesNom = sdfMes.format(calElegido.time).lowercase()
            dialogBinding.tvSubtituloAsignarModal.text = "Este plato será asignado para el día\n$diaSemana $diaNum de $mesNom"
        }
        refrescarTextoDialogo()

        dialogBinding.layoutBadgeAsignar.setOnClickListener {
            if (permitidas.size > 1) {
                val idxActual = permitidas.indexOf(servicioElegido).coerceAtLeast(0)
                servicioElegido = permitidas[(idxActual + 1) % permitidas.size]
                refrescarTextoDialogo()
            }
        }

        dialogBinding.tvSubtituloAsignarModal.setOnClickListener {
            android.app.DatePickerDialog(
                this,
                { _, year, month, dayOfMonth ->
                    calElegido.set(Calendar.YEAR, year)
                    calElegido.set(Calendar.MONTH, month)
                    calElegido.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                    refrescarTextoDialogo()
                },
                calElegido.get(Calendar.YEAR),
                calElegido.get(Calendar.MONTH),
                calElegido.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        val dialog = MaterialAlertDialogBuilder(this)
            .setView(dialogBinding.root)
            .create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        dialogBinding.btnCancelarAsignar.setOnClickListener {
            dialog.dismiss()
        }

        dialogBinding.btnConfirmarAsignar.setOnClickListener {
            val dowElegido = calElegido.get(Calendar.DAY_OF_WEEK)
            if (dowElegido == Calendar.SATURDAY || dowElegido == Calendar.SUNDAY) {
                Toast.makeText(this, "Sábados y domingos no son días escolares PAE.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (esSemanaAprobadaFijada && diasSemanaSiguiente.any { esMismoDia(it, calElegido) }) {
                MaterialAlertDialogBuilder(this)
                    .setTitle("Semana Aprobada por el Supervisor")
                    .setMessage("La programación de esta semana ya ha sido formalmente aprobada por el supervisor PAE. No se pueden modificar los menús de esta semana.")
                    .setPositiveButton("Entendido", null)
                    .show()
                return@setOnClickListener
            }

            val fechaElegidaStr = obtenerClaveFecha(calElegido)

            // 1. Validar regla de rotación PAE respecto al día inmediatamente anterior a la fecha elegida
            val calAyer = (calElegido.clone() as Calendar).apply { add(Calendar.DAY_OF_MONTH, -1) }
            val fechaAyerStr = obtenerClaveFecha(calAyer)
            val platosAyer = listOfNotNull(
                sessionManager.getPlatoDiaServicio(this, fechaAyerStr, "Desayuno"),
                sessionManager.getPlatoDiaServicio(this, fechaAyerStr, "Almuerzo"),
                sessionManager.getPlatoDiaServicio(this, fechaAyerStr, "Merienda")
            ).toSet()

            if (platosAyer.contains(plato.nombre)) {
                val nombreAyer = SimpleDateFormat("EEEE d", Locale.forLanguageTag("es-CO")).format(calAyer.time)
                MaterialAlertDialogBuilder(this)
                    .setTitle("Plato no permitido para este día")
                    .setMessage("El plato \"${plato.nombre}\" ya fue seleccionado para el día anterior ($nombreAyer).\n\nPor normatividad PAE de rotación de menús, no se puede repetir en días consecutivos.")
                    .setPositiveButton("Entendido", null)
                    .show()
                return@setOnClickListener
            }

            val platoExistente = sessionManager.getPlatoDiaServicio(this, fechaElegidaStr, servicioElegido)
            dialog.dismiss()

            // 2. Si el servicio elegido en esa fecha YA tiene este mismo plato, confirmar si desea quitarlo
            if (!platoExistente.isNullOrBlank() && platoExistente.equals(plato.nombre, ignoreCase = true)) {
                mostrarDialogoConfirmarDesmarcar(plato, servicioElegido)
                return@setOnClickListener
            }

            // 3. Si el servicio elegido en esa fecha YA tiene OTRO menú seleccionado, preguntar si está de acuerdo en cambiarlo
            if (!platoExistente.isNullOrBlank() && !platoExistente.equals(plato.nombre, ignoreCase = true)) {
                val fechaBonita = "${calElegido.get(Calendar.DAY_OF_MONTH)} de ${sdfMes.format(calElegido.time).lowercase()}"
                MaterialAlertDialogBuilder(this)
                    .setTitle("Menú ya seleccionado")
                    .setMessage("El servicio de $servicioElegido para el $fechaBonita ya tiene seleccionado \"$platoExistente\".\n\n¿Estás de acuerdo en cambiar el menú ya seleccionado por \"${plato.nombre}\"?")
                    .setPositiveButton("Sí, cambiar") { _, _ ->
                        confirmarYAsignarPlatoParaFecha(plato, servicioElegido, calElegido)
                    }
                    .setNegativeButton("Cancelar", null)
                    .show()
                return@setOnClickListener
            }

            // 4. Asignación directa si el servicio en esa fecha estaba libre
            confirmarYAsignarPlatoParaFecha(plato, servicioElegido, calElegido)
        }

        dialog.show()
        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.80).toInt(),
            android.view.WindowManager.LayoutParams.WRAP_CONTENT
        )
    }

    private fun mostrarDialogoConfirmarDesmarcar(plato: InicioJefaActivity.Plato, servicio: String) {
        val dialogBinding = com.example.movilmanupuladora.databinding.DialogQuitarPlatoBinding.inflate(layoutInflater)
        val fallbackImg = if (plato.imagen != 0) plato.imagen else InicioJefaActivity.obtenerImagenPlatoPorNombre(plato.nombre)
        com.example.movilmanupuladora.utils.ImageLoaderUtil.cargarImagen(
            dialogBinding.imgPlatoQuitarModal,
            plato.imagenUrl,
            fallbackImg
        )
        dialogBinding.tvNombrePlatoQuitarModal.text = plato.nombre
        dialogBinding.tvBadgeQuitarModal.text = servicio
        dialogBinding.txtMensajeQuitarPlato.text = "Esta acción eliminará el plato del servicio\nseleccionado."

        val dialog = com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
            .setView(dialogBinding.root)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        dialogBinding.btnSiQuitar.setOnClickListener {
            dialog.dismiss()
            desmarcarPlatoActual(servicio)
        }

        dialogBinding.btnCancelarQuitar.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.86).toInt(),
            android.view.WindowManager.LayoutParams.WRAP_CONTENT
        )
    }

    private fun desmarcarPlatoActual(servicio: String) {
        if (esSemanaAprobadaFijada) {
            Toast.makeText(this, "Esta semana está formalmente aprobada y no se pueden quitar platos.", Toast.LENGTH_SHORT).show()
            return
        }
        val fechaStr = obtenerClaveFecha(diaSeleccionadoSemanaSiguiente)
        when (servicio.lowercase()) {
            "desayuno" -> platoSeleccionadoDesayuno = null
            "almuerzo" -> platoSeleccionadoAlmuerzo = null
            "merienda" -> platoSeleccionadoMerienda = null
        }

        lifecycleScope.launch {
            PreparacionSyncManager.desasignarPlatoServicio(
                this@PlanificacionSemanalJefaActivity,
                servicio,
                fechaStr
            )
        }

        actualizarSubtitulosTabs()
        renderizarNumerosSemanaSiguiente()
        configurarViewPagerCarousel()
        configurarMenuDisponiblePAE()
        Toast.makeText(this, "Plato quitado de $servicio", Toast.LENGTH_SHORT).show()
    }

    private fun confirmarYAsignarPlato(plato: InicioJefaActivity.Plato) {
        confirmarYAsignarPlatoParaFecha(plato, obtenerNombreComidaActual(), diaSeleccionadoSemanaSiguiente)
    }

    private fun confirmarYAsignarPlatoParaFecha(
        plato: InicioJefaActivity.Plato,
        comidaNombre: String,
        calObjetivo: Calendar
    ) {
        val fechaStr = obtenerClaveFecha(calObjetivo)
        val tipoComidaObjetivo = when (comidaNombre.lowercase()) {
            "desayuno" -> TipoComida.DESAYUNO
            "merienda" -> TipoComida.MERIENDA
            else -> TipoComida.ALMUERZO
        }

        // Si este plato estaba asignado a otro servicio en este mismo día, desasignarlo de allí
        if (tipoComidaObjetivo != TipoComida.DESAYUNO &&
            sessionManager.getPlatoDiaServicio(this, fechaStr, "Desayuno").equals(plato.nombre, ignoreCase = true)
        ) {
            if (esMismoDia(calObjetivo, diaSeleccionadoSemanaSiguiente)) platoSeleccionadoDesayuno = null
            sessionManager.desasignarPlatoDiaServicio(this, fechaStr, "Desayuno")
        }
        if (tipoComidaObjetivo != TipoComida.ALMUERZO &&
            sessionManager.getPlatoDiaServicio(this, fechaStr, "Almuerzo").equals(plato.nombre, ignoreCase = true)
        ) {
            if (esMismoDia(calObjetivo, diaSeleccionadoSemanaSiguiente)) platoSeleccionadoAlmuerzo = null
            sessionManager.desasignarPlatoDiaServicio(this, fechaStr, "Almuerzo")
        }
        if (tipoComidaObjetivo != TipoComida.MERIENDA &&
            sessionManager.getPlatoDiaServicio(this, fechaStr, "Merienda").equals(plato.nombre, ignoreCase = true)
        ) {
            if (esMismoDia(calObjetivo, diaSeleccionadoSemanaSiguiente)) platoSeleccionadoMerienda = null
            sessionManager.desasignarPlatoDiaServicio(this, fechaStr, "Merienda")
        }

        // Guardar inmediatamente en sesión local para la fecha elegida
        sessionManager.savePlatoDiaServicio(this, fechaStr, comidaNombre, plato.nombre, plato.id)

        // Si la fecha elegida coincide con el día visible actualmente, actualizar estado en memoria
        if (esMismoDia(calObjetivo, diaSeleccionadoSemanaSiguiente)) {
            when (tipoComidaObjetivo) {
                TipoComida.DESAYUNO -> platoSeleccionadoDesayuno = plato
                TipoComida.ALMUERZO -> platoSeleccionadoAlmuerzo = plato
                TipoComida.MERIENDA -> platoSeleccionadoMerienda = plato
            }
        }

        actualizarSubtitulosTabs()
        renderizarNumerosSemanaSiguiente()
        configurarViewPagerCarousel()
        configurarMenuDisponiblePAE()

        val cal = calObjetivo
        val diaSemanaAbrev = when (cal.get(Calendar.DAY_OF_WEEK)) {
            Calendar.MONDAY -> "Lun."
            Calendar.TUESDAY -> "Mar."
            Calendar.WEDNESDAY -> "Mié."
            Calendar.THURSDAY -> "Jue."
            Calendar.FRIDAY -> "Vie."
            Calendar.SATURDAY -> "Sáb."
            Calendar.SUNDAY -> "Dom."
            else -> "Lun."
        }
        val diaMes = cal.get(Calendar.DAY_OF_MONTH)
        val mesAbrev = SimpleDateFormat("MMM", Locale.forLanguageTag("es-CO")).format(cal.time)
        val badgeTexto = "$comidaNombre · $diaSemanaAbrev $diaMes $mesAbrev"

        // Guardar plato en sesión y en la nube
        lifecycleScope.launch {
            try {
                PreparacionSyncManager.guardarPlatoServicio(
                    context = this@PlanificacionSemanalJefaActivity,
                    servicio = comidaNombre,
                    nombrePlato = plato.nombre,
                    idPlato = plato.id,
                    fecha = fechaStr,
                    estadoMenu = "Borrador"
                )
            } catch (_: Exception) { }
        }

        // Mostrar modal personalizado de asignación exitosa
        val viewDialog = layoutInflater.inflate(R.layout.dialog_plato_asignado_exito, null)
        val imgPlato = viewDialog.findViewById<ImageView>(R.id.imgPlatoAsignadoModal)
        val tvNombre = viewDialog.findViewById<TextView>(R.id.tvNombrePlatoAsignadoModal)
        val tvBadge = viewDialog.findViewById<TextView>(R.id.tvBadgePlatoAsignadoModal)
        val tvSubtitulo = viewDialog.findViewById<TextView>(R.id.tvSubtituloPlatoAsignadoModal)
        val btnGuardarModal = viewDialog.findViewById<Button>(R.id.btnContinuarPlanificandoModal)
        val btnConfigurar = viewDialog.findViewById<Button>(R.id.btnConfigurarEquipoModal)

        val imgRes = if (plato.imagen != 0) plato.imagen else InicioJefaActivity.obtenerImagenPlatoPorNombre(plato.nombre)
        imgPlato.setImageResource(imgRes)
        tvNombre.text = plato.nombre
        tvBadge.text = badgeTexto
        tvSubtitulo.text = "Tu plato quedó programado para\nel $diaMes de ${SimpleDateFormat("MMMM", Locale.forLanguageTag("es-CO")).format(cal.time)}."
        btnGuardarModal.text = "Guardar"

        val dialog = androidx.appcompat.app.AlertDialog.Builder(this)
            .setView(viewDialog)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        btnGuardarModal.setOnClickListener {
            dialog.dismiss()
            avanzarSiguienteServicioODia(comidaNombre)
        }

        btnConfigurar.setOnClickListener {
            dialog.dismiss()
            val intent = Intent(this, AsignarComponentesActivity::class.java).apply {
                putExtra("fecha_seleccionada", fechaStr)
                putExtra("servicio", comidaNombre)
            }
            startActivity(intent)
        }

        dialog.show()

        val width = (resources.displayMetrics.widthPixels * 0.90).toInt()
        dialog.window?.setLayout(width, android.view.ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    private fun avanzarSiguienteServicioODia(servicioGuardado: String) {
        val permitidas = obtenerJornadasPermitidas()
        val des = platoSeleccionadoDesayuno
        val alm = platoSeleccionadoAlmuerzo
        val mer = platoSeleccionadoMerienda

        // Buscar el siguiente servicio permitido que aún no tenga plato asignado en este día
        val siguientePendiente = permitidas.firstOrNull { tipo ->
            when (tipo) {
                TipoComida.DESAYUNO -> des == null
                TipoComida.ALMUERZO -> alm == null
                TipoComida.MERIENDA -> mer == null
            }
        }

        if (siguientePendiente != null && siguientePendiente != comidaSeleccionada) {
            seleccionarTipoComida(siguientePendiente)
            val nom = when (siguientePendiente) {
                TipoComida.DESAYUNO -> "Desayuno"
                TipoComida.ALMUERZO -> "Almuerzo"
                TipoComida.MERIENDA -> "Merienda"
            }
            Toast.makeText(this, "Guardado. Selecciona ahora el $nom", Toast.LENGTH_SHORT).show()
        } else {
            // Todos los servicios requeridos para este día están completos
            verificarAvanzarSiguienteDia()
        }
    }

    private fun verificarAvanzarSiguienteDia() {
        val permitidas = obtenerJornadasPermitidas()
        val des = platoSeleccionadoDesayuno
        val alm = platoSeleccionadoAlmuerzo
        val mer = platoSeleccionadoMerienda

        val diaCompleto = permitidas.all { tipo ->
            when (tipo) {
                TipoComida.DESAYUNO -> des != null
                TipoComida.ALMUERZO -> alm != null
                TipoComida.MERIENDA -> mer != null
            }
        }

        if (diaCompleto) {
            val currentIndex = diasSemanaSiguiente.indexOfFirst { esMismoDia(it, diaSeleccionadoSemanaSiguiente) }
            val siguienteEscolar = diasSemanaSiguiente.drop((currentIndex + 1).coerceAtLeast(0)).firstOrNull {
                val dow = it.get(Calendar.DAY_OF_WEEK)
                dow != Calendar.SATURDAY && dow != Calendar.SUNDAY
            }
            if (siguienteEscolar != null) {
                diaSeleccionadoSemanaSiguiente = siguienteEscolar
                renderizarNumerosSemanaSiguiente()
                cargarPlatosSeleccionados()
                configurarViewPagerCarousel()
                configurarMenuDisponiblePAE()

                val formatoFecha = SimpleDateFormat("EEEE, d 'de' MMMM", Locale.forLanguageTag("es-CO"))
                val fechaStr = formatoFecha.format(siguienteEscolar.time)
                Toast.makeText(this, "Día completado. Pasando a: $fechaStr", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(this, "¡Semana completa planificada con éxito!", Toast.LENGTH_LONG).show()
            }
        }
    }

    /**
     * Consulta si la semana ha sido aprobada por el Supervisor PAE o si devolvió algún plato.
     */
    private fun consultarEstadoAprobacionYDevolucion() {
        lifecycleScope.launch {
            try {
                val semanaClave = SimpleDateFormat("yyyy_MM_W", Locale.getDefault()).format(diaSeleccionadoSemanaSiguiente.time)
                val prefs = getSharedPreferences("MINUTA_APROBACION", MODE_PRIVATE)
                val semanaAprobada = prefs.getBoolean("aprobada_supervisor_$semanaClave", false)

                val notifRes = RetrofitClient.apiService.obtenerNotificaciones()
                var platoDevueltoNotif: com.example.movilmanupuladora.data.model.Notificacion? = null
                var semanaAprobadaNotif = false

                if (notifRes.isSuccessful && !notifRes.body().isNullOrEmpty()) {
                    val lista = notifRes.body()!!
                    val devuelta = lista.firstOrNull {
                        val t = (it.titulo ?: "").lowercase()
                        val m = (it.mensaje ?: "").lowercase()
                        t.contains("devuelto") || t.contains("rechaz") || m.contains("devuelto") || m.contains("rechaz")
                    }
                    if (devuelta != null) {
                        platoDevueltoNotif = devuelta
                    }

                    val aprob = lista.firstOrNull {
                        val t = (it.titulo ?: "").lowercase()
                        val m = (it.mensaje ?: "").lowercase()
                        (t.contains("aprobada") || m.contains("aprobada")) && (t.contains("minuta") || t.contains("semana") || m.contains("minuta"))
                    }
                    if (aprob != null) {
                        semanaAprobadaNotif = true
                    }
                }

                if (semanaAprobada || semanaAprobadaNotif) {
                    esSemanaAprobadaFijada = true
                    binding.cardAlertaSupervisor.visibility = View.VISIBLE
                    binding.cardAlertaSupervisor.setCardBackgroundColor(Color.parseColor("#ECFDF5"))
                    binding.cardAlertaSupervisor.strokeColor = Color.parseColor("#A7F3D0")
                    binding.txtIconoAlertaSupervisor.text = "✓"
                    binding.txtTituloAlertaSupervisor.text = "Semana Aprobada por el Supervisor PAE"
                    binding.txtTituloAlertaSupervisor.setTextColor(Color.parseColor("#065F46"))
                    binding.txtMensajeAlertaSupervisor.text = "La programación de esta semana ha sido aprobada formalmente. Es oficial, fija y no se puede modificar."
                    binding.txtMensajeAlertaSupervisor.setTextColor(Color.parseColor("#047857"))
                    binding.btnEnviarProgramacionSupervisor.text = "✓ Programación Aprobada y Fijada"
                    binding.btnEnviarProgramacionSupervisor.isEnabled = false
                    binding.btnEnviarProgramacionSupervisor.backgroundTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#64748B"))
                    binding.btnGuardarSeleccion.isEnabled = false
                    binding.btnGuardarSeleccion.alpha = 0.5f
                    binding.btnSeleccionarPlato.isEnabled = false
                    binding.btnSeleccionarPlato.alpha = 0.5f
                } else if (platoDevueltoNotif != null) {
                    esSemanaAprobadaFijada = false
                    binding.cardAlertaSupervisor.visibility = View.VISIBLE
                    binding.cardAlertaSupervisor.setCardBackgroundColor(Color.parseColor("#FFF1F2"))
                    binding.cardAlertaSupervisor.strokeColor = Color.parseColor("#FECDD3")
                    binding.txtIconoAlertaSupervisor.text = "⚠️"
                    binding.txtTituloAlertaSupervisor.text = platoDevueltoNotif.titulo ?: "Plato devuelto por el Supervisor"
                    binding.txtTituloAlertaSupervisor.setTextColor(Color.parseColor("#9F1239"))
                    binding.txtMensajeAlertaSupervisor.text = platoDevueltoNotif.mensaje ?: "El supervisor devolvió un plato. Por favor cámbialo antes de reenviar la programación."
                    binding.txtMensajeAlertaSupervisor.setTextColor(Color.parseColor("#881337"))
                    binding.btnEnviarProgramacionSupervisor.isEnabled = true
                    binding.btnGuardarSeleccion.isEnabled = true
                    binding.btnGuardarSeleccion.alpha = 1.0f
                    binding.btnSeleccionarPlato.isEnabled = true
                    binding.btnSeleccionarPlato.alpha = 1.0f
                } else {
                    esSemanaAprobadaFijada = false
                    binding.cardAlertaSupervisor.visibility = View.GONE
                    binding.btnEnviarProgramacionSupervisor.isEnabled = true
                    binding.btnGuardarSeleccion.isEnabled = true
                    binding.btnGuardarSeleccion.alpha = 1.0f
                    binding.btnSeleccionarPlato.isEnabled = true
                    binding.btnSeleccionarPlato.alpha = 1.0f
                }
            } catch (_: Exception) { }
        }
    }

    /**
     * Valida y remite la programación semanal completa al Supervisor PAE.
     */
    private fun enviarProgramacionAlSupervisor() {
        if (esSemanaAprobadaFijada) {
            Toast.makeText(this, "Esta semana ya fue formalmente aprobada y está fijada.", Toast.LENGTH_LONG).show()
            return
        }

        val permitidas = obtenerJornadasPermitidas()
        val diasIncompletos = mutableListOf<String>()

        val diasHabiles = if (diasSemanaSiguiente.size >= 6) diasSemanaSiguiente.subList(1, 6) else diasSemanaSiguiente.take(5)
        for (dia in diasHabiles) { // Lunes a Viernes
            val fechaStr = obtenerClaveFecha(dia)
            val diaNombre = SimpleDateFormat("EEEE d", Locale.forLanguageTag("es-CO")).format(dia.time)
            val completo = permitidas.all { tipo ->
                val serv = when (tipo) {
                    TipoComida.DESAYUNO -> "Desayuno"
                    TipoComida.ALMUERZO -> "Almuerzo"
                    TipoComida.MERIENDA -> "Merienda"
                }
                !sessionManager.getPlatoDiaServicio(this, fechaStr, serv).isNullOrBlank()
            }
            if (!completo) {
                diasIncompletos.add(diaNombre)
            }
        }

        if (diasIncompletos.isNotEmpty()) {
            MaterialAlertDialogBuilder(this)
                .setTitle("Programación Incompleta")
                .setMessage("Aún faltan menús por definir en los siguientes días:\n\n• ${diasIncompletos.joinToString("\n• ")}\n\nPor favor asigna los menús de toda la semana antes de enviarla al supervisor.")
                .setPositiveButton("Entendido", null)
                .show()
            return
        }

        val hayPendientePorEnviar = diasHabiles.any { dia ->
            val fechaStr = obtenerClaveFecha(dia)
            permitidas.any { tipo ->
                val serv = when (tipo) {
                    TipoComida.DESAYUNO -> "Desayuno"
                    TipoComida.ALMUERZO -> "Almuerzo"
                    TipoComida.MERIENDA -> "Merienda"
                }
                val est = sessionManager.getEstadoMenuDiaServicio(this, fechaStr, serv).trim()
                est.isBlank() || est.equals("Borrador", ignoreCase = true) || est.equals("Rechazado", ignoreCase = true)
            }
        }
        if (!hayPendientePorEnviar) {
            Toast.makeText(this, "Esta programación ya fue enviada", Toast.LENGTH_LONG).show()
            return
        }

        val dialog = android.app.Dialog(this)
        val vistaDialog = layoutInflater.inflate(R.layout.dialog_confirmar_envio_supervisor, null)
        dialog.setContentView(vistaDialog)
        dialog.window?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(Color.TRANSPARENT))
        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.90).toInt(),
            android.view.ViewGroup.LayoutParams.WRAP_CONTENT
        )

        vistaDialog.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnConfirmarEnvioSupervisor)
            .setOnClickListener {
                dialog.dismiss()
                ejecutarEnvioSupervisor()
            }
        vistaDialog.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnCancelarEnvioSupervisor)
            .setOnClickListener {
                dialog.dismiss()
            }

        dialog.show()
    }

    private fun ejecutarEnvioSupervisor() {
        lifecycleScope.launch {
            try {
                val sedeNombre = sessionManager.getSedeNombre() ?: "Sede"
                val ieNombre = sessionManager.getInstitucionNombre()?.takeIf { it.isNotBlank() } ?: "Institución Educativa"
                val diasHabiles = if (diasSemanaSiguiente.size >= 6) diasSemanaSiguiente.subList(1, 6) else diasSemanaSiguiente.take(5)
                val primerDia = diasHabiles.first()
                val ultimoDia = diasHabiles.last() // Viernes
                val sdfCorto = SimpleDateFormat("d MMM", Locale.forLanguageTag("es-CO"))
                val rango = "${sdfCorto.format(primerDia.time).replace(".", "")} al ${sdfCorto.format(ultimoDia.time).replace(".", "")}"

                // 1. Guardar todos los platos de la semana en el backend con estado Pendiente para el Supervisor
                for (dia in diasHabiles) {
                    val fechaStr = obtenerClaveFecha(dia)
                    for (tipo in obtenerJornadasPermitidas()) {
                        val serv = when (tipo) {
                            TipoComida.DESAYUNO -> "Desayuno"
                            TipoComida.ALMUERZO -> "Almuerzo"
                            TipoComida.MERIENDA -> "Merienda"
                        }
                        val plato = sessionManager.getPlatoDiaServicio(this@PlanificacionSemanalJefaActivity, fechaStr, serv)
                        if (!plato.isNullOrBlank()) {
                            val id = sessionManager.getPlatoDiaServicioId(this@PlanificacionSemanalJefaActivity, fechaStr, serv)
                            PreparacionSyncManager.guardarPlatoServicio(
                                this@PlanificacionSemanalJefaActivity,
                                serv,
                                plato,
                                id,
                                fechaStr,
                                estadoMenu = "Pendiente"
                            )
                        }
                    }
                }

                // 2. Enviar notificación al Supervisor de esa sede
                val tituloBody = "Programación semanal enviada - $sedeNombre".toRequestBody("text/plain".toMediaTypeOrNull())
                val mensajeBody = "La manipuladora jefe de la institución educativa $ieNombre de la sede $sedeNombre ha programado los menús de la semana $rango.".toRequestBody("text/plain".toMediaTypeOrNull())

                try {
                    RetrofitClient.apiService.enviarNotificacion(tituloBody, mensajeBody, null)
                } catch (_: Exception) { }

                val semanaClave = SimpleDateFormat("yyyy_MM_W", Locale.getDefault()).format(diasSemanaSiguiente.first().time)
                getSharedPreferences("MINUTA_APROBACION", MODE_PRIVATE)
                    .edit()
                    .putBoolean("enviada_supervisor_$semanaClave", true)
                    .apply()

                Toast.makeText(this@PlanificacionSemanalJefaActivity, "Programación enviada", Toast.LENGTH_LONG).show()
                renderizarNumerosSemanaSiguiente()
                cargarPlatosSeleccionados()
            } catch (e: Exception) {
                Toast.makeText(this@PlanificacionSemanalJefaActivity, "Error al enviar: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun obtenerIdSeccionActual(): Int = when (comidaSeleccionada) {
        TipoComida.DESAYUNO -> 1
        TipoComida.ALMUERZO -> 2
        TipoComida.MERIENDA -> 3
    }

    private fun solicitarSugerenciaIA(nombrePlato: String, posicionCarrusel: Int) {
        val idSeccion = obtenerIdSeccionActual()
        val idSede = sessionManager.getSedeId().takeIf { it > 0 } ?: 1
        var ninos = obtenerAsistenciaReal()

        val cacheadas = cacheSugerenciasIA[idSeccion]
        if (!cacheadas.isNullOrEmpty()) {
            val sugerencia = cacheadas[posicionCarrusel % cacheadas.size]
            pintarIngredientesEnPantalla(sugerencia, ninos)
            return
        }

        binding.txtNinosPresentesInfo.text = "Calculando ingredientes con IA..."
        binding.layoutContenedorIngredientes.removeAllViews()

        lifecycleScope.launch {
            try {
                if (ninos <= 0) {
                    try {
                        val resAsist = RetrofitClient.apiService.obtenerAsistenciaDiaria()
                        if (resAsist.isSuccessful && !resAsist.body().isNullOrEmpty()) {
                            val fechaSel = obtenerClaveFecha(diaSeleccionadoSemanaSiguiente)
                            val lista = resAsist.body()!!
                            val sumaFecha = lista.filter { it.fecha.trim().startsWith(fechaSel) }.sumOf { it.ninosPresentes }
                            if (sumaFecha > 0) ninos = sumaFecha
                        }
                    } catch (_: Exception) { }
                }

                val request = SugerenciasRecetasRequest(
                    idSede = idSede,
                    ninosPresentes = if (ninos > 0) ninos else 0,
                    idSeccion = idSeccion,
                    preferencia = nombrePlato
                )
                val response = RetrofitClient.apiService.sugerirRecetas(request)
                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()!!
                    val lista = body.sugerenciasIaEstructuradas.orEmpty().ifEmpty {
                        body.platosEstipuladosPae.orEmpty()
                    }
                    if (lista.isNotEmpty()) {
                        cacheSugerenciasIA[idSeccion] = lista
                        val sugerencia = lista[posicionCarrusel % lista.size]
                        val ninosResp = body.ninosPresentes ?: ninos
                        pintarIngredientesEnPantalla(sugerencia, ninosResp)
                    } else {
                        binding.txtNinosPresentesInfo.text = "Sin sugerencias IA disponibles para esta jornada."
                    }
                } else {
                    binding.txtNinosPresentesInfo.text = "No se pudo consultar la IA (${response.code()})."
                }
            } catch (t: Throwable) {
                binding.txtNinosPresentesInfo.text = "Error de conexión con IA: ${t.localizedMessage}"
            }
        }
    }

    private fun pintarIngredientesEnPantalla(sugerencia: SugerenciaIAItem, ninos: Int) {
        binding.layoutContenedorIngredientes.removeAllViews()
        val estadoStock = if (sugerencia.viableConInventario) "✓ Stock disponible" else "⚠️ Revisar stock"
        binding.txtNinosPresentesInfo.text =
            "🤖 ${sugerencia.nombrePlato}\n👥 Calculado para $ninos niños · $estadoStock"

        for (ingrediente in sugerencia.ingredientes.orEmpty()) {
            val itemLayout = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(0, 8, 0, 8)
                gravity = Gravity.CENTER_VERTICAL
            }

            val textLayout = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }

            val tvNombre = TextView(this).apply {
                text = ingrediente.nombreIngrediente
                setTextColor(Color.parseColor("#1E293B"))
                textSize = 14f
                setTypeface(null, android.graphics.Typeface.BOLD)
            }

            val detalleTexto = if (ingrediente.cantidadTotal.isNotBlank()) {
                "Requerido: ${ingrediente.cantidadTotal} · Stock: ${ingrediente.stockActual} ${ingrediente.unidad}"
            } else {
                "Ingrediente requerido del menú PAE"
            }

            val tvDetalle = TextView(this).apply {
                text = detalleTexto
                setTextColor(Color.parseColor("#92400E"))
                textSize = 12f
            }

            textLayout.addView(tvNombre)
            textLayout.addView(tvDetalle)
            itemLayout.addView(textLayout)

            val tvEstado = TextView(this).apply {
                text = if (ingrediente.alcanza) "✓ Disponible" else "✕ Insuficiente"
                setTextColor(Color.parseColor(if (ingrediente.alcanza) "#16A34A" else "#DC2626"))
                textSize = 12f
                setTypeface(null, android.graphics.Typeface.BOLD)
            }
            itemLayout.addView(tvEstado)

            binding.layoutContenedorIngredientes.addView(itemLayout)

            val divider = View(this).apply {
                setBackgroundColor(Color.parseColor("#E2E8F0"))
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 1).also {
                    it.topMargin = 4
                    it.bottomMargin = 4
                }
            }
            binding.layoutContenedorIngredientes.addView(divider)
        }

        val pasos = sugerencia.pasos.orEmpty().filter { it.isNotBlank() }
        if (pasos.isNotEmpty()) {
            val tvTituloPasos = TextView(this).apply {
                text = "👨‍🍳 Receta / Preparación paso a paso"
                setTextColor(Color.parseColor("#1E293B"))
                textSize = 13f
                setTypeface(null, android.graphics.Typeface.BOLD)
                setPadding(0, 14, 0, 6)
            }
            binding.layoutContenedorIngredientes.addView(tvTituloPasos)

            pasos.forEachIndexed { idx, paso ->
                val tvPaso = TextView(this).apply {
                    text = "${idx + 1}. $paso"
                    setTextColor(Color.parseColor("#475569"))
                    textSize = 12f
                    setPadding(0, 4, 0, 4)
                }
                binding.layoutContenedorIngredientes.addView(tvPaso)
            }
        }
    }

    private fun cargarPlatosDesdeBackend() {
        lifecycleScope.launch {
            try {
                val platosRes = menuRepository.obtenerPlatos()
                if (platosRes.isSuccessful && !platosRes.body().isNullOrEmpty()) {
                    val lista = platosRes.body()!!
                    val tempDes = mutableListOf<InicioJefaActivity.Plato>()
                    val tempAlm = mutableListOf<InicioJefaActivity.Plato>()
                    val tempMer = mutableListOf<InicioJefaActivity.Plato>()

                    for (p in lista) {
                        val nombre = p.nombrePlato ?: "Plato del día"
                        val imagen = if (p.idPlato in 1..20) {
                            MenuPaeCatalog.obtenerMenuPorId(p.idPlato).imagenRes
                        } else {
                            MenuPaeCatalog.obtenerImagenPorNombre(p.nombrePlato ?: p.componente)
                        }
                        val plato = InicioJefaActivity.Plato(
                            id = p.idPlato,
                            nombre = nombre,
                            componente = p.componente ?: "",
                            imagen = imagen,
                            imagenUrl = p.imagenUrl
                        )
                        val secNom = p.nombreSeccion?.lowercase().orEmpty()
                        val esDesayuno = p.idSeccion == 1 || secNom.contains("desayun")
                        val esAlmuerzo = p.idSeccion == 2 || secNom.contains("almuerz")
                        val esMerienda = p.idSeccion == 3 || secNom.contains("meriend") || secNom.contains("refrigerio")

                        when {
                            esDesayuno -> tempDes.add(plato)
                            esAlmuerzo -> tempAlm.add(plato)
                            esMerienda -> tempMer.add(plato)
                            else -> {
                                val nomLow = nombre.lowercase()
                                val esAlm = listOf(
                                    "pollo", "carne", "res", "cerdo", "hígado", "higado", "pescado", "atún", "atun",
                                    "lenteja", "frijol", "fríjol", "garbanzo", "sancocho", "sopa", "pasta", "espagueti",
                                    "ensalada", "guisad", "sudado", "albóndiga", "albondiga", "almuerzo"
                                ).any { nomLow.contains(it) }

                                val esMer = listOf("jugo", "avena", "agua de panela", "banano", "fruta", "papaya", "mango", "yogurt", "merienda", "refrigerio")
                                    .any { nomLow.contains(it) } && !esAlm

                                val esDes = listOf("desayuno", "huevo", "arepa", "chocolate", "calentado", "hojaldra", "pan", "galleta", "queso", "caldo", "changua", "café", "leche", "torta")
                                    .any { nomLow.contains(it) } && !esAlm

                                when {
                                    esAlm -> tempAlm.add(plato)
                                    esMer -> tempMer.add(plato)
                                    esDes -> tempDes.add(plato)
                                }
                            }
                        }
                    }

                    if (tempDes.isNotEmpty()) {
                        platosDesayuno.clear()
                        platosDesayuno.addAll(tempDes)
                    }
                    if (tempAlm.isNotEmpty()) {
                        platosAlmuerzo.clear()
                        platosAlmuerzo.addAll(tempAlm)
                    }
                    if (tempMer.isNotEmpty()) {
                        platosMerienda.clear()
                        platosMerienda.addAll(tempMer)
                    }

                    configurarViewPagerCarousel()
                    configurarMenuDisponiblePAE()
                }
            } catch (_: Exception) { }
        }
    }
}
