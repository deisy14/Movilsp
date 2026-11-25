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
            platosDesayuno = MenuPaeCatalog.menus.map {
                InicioJefaActivity.Plato(id = it.id, nombre = it.nombre, componente = it.componente, imagen = it.imagenRes)
            }.toMutableList()

            platosAlmuerzo = MenuPaeCatalog.menus.map {
                InicioJefaActivity.Plato(id = it.id, nombre = it.nombre, componente = it.componente, imagen = it.imagenRes)
            }.toMutableList()

            platosMerienda = MenuPaeCatalog.menus.map {
                InicioJefaActivity.Plato(id = it.id, nombre = it.nombre, componente = it.componente, imagen = it.imagenRes)
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
                val idSede = sessionManager.getSedeId().takeIf { it > 0 } ?: 1
                SedeContratoManager.sincronizarSeccionesContrato(this@PlanificacionSemanalJefaActivity, idSede)
                configurarTabsComida()
                configurarBotonesSeccionesIA()
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
            configurarTabsComida()
            configurarBotonesSeccionesIA()
        }
    }

    private var semanaOffset = 0

    // =========================================================
    // CÁLCULO DE LA SEMANA (LUNES A DOMINGO)
    // =========================================================

    private fun configurarSemanaSiguiente() {
        diasSemanaSiguiente.clear()

        // Calcular el lunes de la semana según el offset (0 = semana actual, 1 = semana siguiente)
        val calSemana = Calendar.getInstance().apply {
            firstDayOfWeek = Calendar.MONDAY
            set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
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

        // Por defecto seleccionar el día de hoy si está dentro de la semana
        val hoy = Calendar.getInstance()
        val indexHoy = diasSemanaSiguiente.indexOfFirst { esMismoDia(it, hoy) }
        diaSeleccionadoSemanaSiguiente = if (indexHoy != -1) diasSemanaSiguiente[indexHoy] else diasSemanaSiguiente.first()

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

            val txtNum = TextView(this).apply {
                val p = FrameLayout.LayoutParams(
                    (34 * resources.displayMetrics.density).toInt(),
                    (34 * resources.displayMetrics.density).toInt()
                ).apply { gravity = Gravity.CENTER }
                layoutParams = p
                gravity = Gravity.CENTER
                textSize = 14f
                text = dia.get(Calendar.DAY_OF_MONTH).toString()

                val isSelected = esMismoDia(dia, diaSeleccionadoSemanaSiguiente)
                if (isSelected) {
                    setBackgroundResource(R.drawable.circulo_amarillo_badge)
                    setTextColor(Color.parseColor("#1E293B"))
                    typeface = android.graphics.Typeface.DEFAULT_BOLD
                } else if (diaCompleto) {
                    setBackgroundResource(R.drawable.bg_pill_green)
                    setTextColor(Color.parseColor("#166534"))
                    typeface = android.graphics.Typeface.DEFAULT_BOLD
                } else {
                    setBackgroundColor(Color.TRANSPARENT)
                    setTextColor(Color.parseColor("#1E293B"))
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

                val formatoFecha = SimpleDateFormat("EEEE, d 'de' MMMM", Locale.forLanguageTag("es-CO"))
                val fechaStr = formatoFecha.format(dia.time)
                Toast.makeText(this, "Planificando para: $fechaStr", Toast.LENGTH_SHORT).show()
            }

            cell.addView(txtNum)
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
    }

    private fun configurarBotonesSeccionesIA() {
        val seccionesActivas = SedeContratoManager.obtenerSeccionesActivas(this)

        // Si la sede trabaja solo con 1 sección (ej: solo Desayuno),
        // no son necesarios los botones de desayuno, almuerzo, merienda como indicó el usuario
        if (seccionesActivas.size <= 1) {
            binding.layoutSelectorSeccionesIA.visibility = View.GONE
            val seccionUnica = seccionesActivas.firstOrNull() ?: "Desayuno"
            val idSec = when (seccionUnica) {
                "Almuerzo" -> 2
                "Merienda" -> 3
                else -> 1
            }
            val tipo = when (seccionUnica) {
                "Almuerzo" -> TipoComida.ALMUERZO
                "Merienda" -> TipoComida.MERIENDA
                else -> TipoComida.DESAYUNO
            }
            if (comidaSeleccionada != tipo) {
                seleccionarTipoComida(tipo)
            } else {
                cargarSugerenciasSegunInventario(idSec)
            }
            return
        }

        binding.layoutSelectorSeccionesIA.visibility = View.VISIBLE
        binding.btnSeccionDesayuno.visibility = if (seccionesActivas.contains("Desayuno")) View.VISIBLE else View.GONE
        binding.btnSeccionAlmuerzo.visibility = if (seccionesActivas.contains("Almuerzo")) View.VISIBLE else View.GONE
        binding.btnSeccionMerienda.visibility = if (seccionesActivas.contains("Merienda")) View.VISIBLE else View.GONE

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
        val platosActuales = obtenerPlatosActuales()
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
        }
        val adapter = MenuDisponibleAdapter(
            listaPlatos = listaCompleta,
            servicioActual = obtenerNombreComidaActual(),
            platoSeleccionadoActual = obtenerPlatoSeleccionadoActual(),
            otrosAsignados = obtenerOtrosAsignados(),
            platosDiaAnterior = obtenerPlatosDiaAnterior()
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
            val fechaStr = obtenerClaveFecha(diaSeleccionadoSemanaSiguiente)
            lifecycleScope.launch {
                val des = platoSeleccionadoDesayuno
                val alm = platoSeleccionadoAlmuerzo
                val mer = platoSeleccionadoMerienda

                if (des != null) {
                    PreparacionSyncManager.guardarPlatoServicio(this@PlanificacionSemanalJefaActivity, "Desayuno", des.nombre, des.id, fechaStr)
                }
                if (alm != null) {
                    PreparacionSyncManager.guardarPlatoServicio(this@PlanificacionSemanalJefaActivity, "Almuerzo", alm.nombre, alm.id, fechaStr)
                }
                if (mer != null) {
                    PreparacionSyncManager.guardarPlatoServicio(this@PlanificacionSemanalJefaActivity, "Merienda", mer.nombre, mer.id, fechaStr)
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

        val fechaStr = SimpleDateFormat("EEEE, d 'de' MMMM", Locale.forLanguageTag("es-CO")).format(diaSeleccionadoSemanaSiguiente.time)
        val servicio = obtenerNombreComidaActual()
        val ninosTexto = if ((itemIA?.ninosPresentes ?: 0) > 0) " · Para ${itemIA?.ninosPresentes} niños" else ""

        val rootLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding((18 * density).toInt(), (18 * density).toInt(), (18 * density).toInt(), (18 * density).toInt())
            setBackgroundColor(Color.WHITE)
        }

        val scroll = androidx.core.widget.NestedScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, (460 * density).toInt())
            isFillViewport = true
        }

        val contentLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        // FOTO DEL PLATO
        val imgCard = com.google.android.material.card.MaterialCardView(this).apply {
            radius = 18 * density
            cardElevation = 2 * density
            strokeWidth = 0
            val cardParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, (180 * density).toInt())
            layoutParams = cardParams
        }

        val imgView = ImageView(this).apply {
            scaleType = ImageView.ScaleType.CENTER_CROP
            layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
            val urlAi = plato.imagenUrl?.takeIf { it.isNotBlank() } ?: itemIA?.obtenerUrlImagenReal()
            ImageLoaderUtil.cargarImagen(this, urlAi, R.drawable.bg_placeholder_plato_ia)
        }
        imgCard.addView(imgView)
        contentLayout.addView(imgCard)

        // BADGE FECHA Y MOMENTO
        val tvBadge = TextView(this).apply {
            text = "📅 $fechaStr · $servicio$ninosTexto"
            setTextColor(Color.parseColor("#92400E"))
            textSize = 12f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setBackgroundResource(R.drawable.bg_pill_yellow)
            setPadding((10 * density).toInt(), (4 * density).toInt(), (10 * density).toInt(), (4 * density).toInt())
            val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).also {
                it.topMargin = (12 * density).toInt()
            }
            layoutParams = lp
        }
        contentLayout.addView(tvBadge)

        // NOMBRE DEL PLATO
        val tvNombre = TextView(this).apply {
            text = plato.nombre
            setTextColor(Color.parseColor("#1E293B"))
            textSize = 18f
            setTypeface(null, android.graphics.Typeface.BOLD)
            val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).also {
                it.topMargin = (8 * density).toInt()
            }
            layoutParams = lp
        }
        contentLayout.addView(tvNombre)

        // COMPONENTES
        val tvComponentes = TextView(this).apply {
            val compTexto = plato.componente.ifBlank {
                itemIA?.componente?.ifBlank {
                    MenuPaeCatalog.obtenerComponentesPorNombre(plato.nombre).joinToString(", ")
                } ?: "Aporte calórico balanceado según normas PAE"
            }
            text = "🍱 Componentes del plato:\n$compTexto"
            setTextColor(Color.parseColor("#475569"))
            textSize = 13f
            val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).also {
                it.topMargin = (8 * density).toInt()
            }
            layoutParams = lp
        }
        contentLayout.addView(tvComponentes)

        // RECETA (PASOS)
        val tvRecetaTitulo = TextView(this).apply {
            text = "📖 Receta / Preparación paso a paso:"
            setTextColor(Color.parseColor("#1E293B"))
            textSize = 14f
            setTypeface(null, android.graphics.Typeface.BOLD)
            val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).also {
                it.topMargin = (14 * density).toInt()
            }
            layoutParams = lp
        }
        contentLayout.addView(tvRecetaTitulo)

        val pasos = itemIA?.pasos.orEmpty().filter { it.isNotBlank() }
        if (pasos.isNotEmpty()) {
            pasos.forEachIndexed { i, paso ->
                val tvPaso = TextView(this).apply {
                    text = "${i + 1}. $paso"
                    setTextColor(Color.parseColor("#334155"))
                    textSize = 12f
                    setPadding(0, (2 * density).toInt(), 0, (2 * density).toInt())
                }
                contentLayout.addView(tvPaso)
            }
        } else {
            val tvRecetaDefault = TextView(this).apply {
                text = "1. Alistar y desinfectar ingredientes según protocolo BPM.\n2. Cocinar a fuego medio manteniendo temperaturas y tiempos de cocción PAE.\n3. Servir en porciones estipuladas según grupo de edad."
                setTextColor(Color.parseColor("#64748B"))
                textSize = 12f
            }
            contentLayout.addView(tvRecetaDefault)
        }

        // INGREDIENTES E INVENTARIO
        val tvIngredientesTitulo = TextView(this).apply {
            text = "📦 Ingredientes requeridos y Stock en Bodega:"
            setTextColor(Color.parseColor("#1E293B"))
            textSize = 14f
            setTypeface(null, android.graphics.Typeface.BOLD)
            val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).also {
                it.topMargin = (14 * density).toInt()
            }
            layoutParams = lp
        }
        contentLayout.addView(tvIngredientesTitulo)

        val ingredientes = itemIA?.ingredientes.orEmpty()
        if (ingredientes.isNotEmpty()) {
            ingredientes.forEach { ing ->
                val tvIng = TextView(this).apply {
                    val estado = if (ing.alcanza) "✓ Stock suficiente" else "⚠️ Stock bajo"
                    text = "• ${ing.nombreIngrediente}: ${ing.cantidadTotal} (En bodega: ${ing.stockActual} ${ing.unidad}) · $estado"
                    setTextColor(Color.parseColor(if (ing.alcanza) "#15803D" else "#B91C1C"))
                    textSize = 12f
                    setPadding(0, (2 * density).toInt(), 0, (2 * density).toInt())
                }
                contentLayout.addView(tvIng)
            }
        } else {
            val tvIngDefault = TextView(this).apply {
                text = "• Ingredientes balanceados según normas PAE e inventario activo."
                setTextColor(Color.parseColor("#64748B"))
                textSize = 12f
            }
            contentLayout.addView(tvIngDefault)
        }

        scroll.addView(contentLayout)
        rootLayout.addView(scroll)

        MaterialAlertDialogBuilder(this)
            .setView(rootLayout)
            .setPositiveButton("Seleccionar para este día") { _, _ ->
                seleccionarPlatoParaSiguienteSemana(plato)
            }
            .setNegativeButton("Cerrar", null)
            .show()
    }

    private fun seleccionarPlatoParaSiguienteSemana(plato: InicioJefaActivity.Plato) {
        if (esSemanaAprobadaFijada) {
            MaterialAlertDialogBuilder(this)
                .setTitle("Semana Aprobada por el Supervisor")
                .setMessage("La programación de esta semana ya ha sido formalmente aprobada por el supervisor PAE. No se pueden modificar los menús programados.")
                .setPositiveButton("Entendido", null)
                .show()
            return
        }

        val comidaNombre = obtenerNombreComidaActual()
        val platosAyer = obtenerPlatosDiaAnterior()

        // 1. Validar si el plato ya fue servido en el día inmediatamente anterior
        if (platosAyer.contains(plato.nombre)) {
            val calAyer = (diaSeleccionadoSemanaSiguiente.clone() as Calendar).apply {
                add(Calendar.DAY_OF_MONTH, -1)
            }
            val nombreAyer = SimpleDateFormat("EEEE d", Locale.forLanguageTag("es-CO")).format(calAyer.time)
            MaterialAlertDialogBuilder(this)
                .setTitle("Plato no permitido para este día")
                .setMessage("El plato \"${plato.nombre}\" ya fue seleccionado para el día anterior ($nombreAyer).\n\nPor normatividad PAE de rotación de menús, no se puede repetir en días consecutivos.")
                .setPositiveButton("Entendido", null)
                .show()
            return
        }

        // 2. Si el usuario toca el plato que YA está seleccionado para este servicio, confirmar quitarlo
        val actual = obtenerPlatoSeleccionadoActual()
        if (actual != null && actual.nombre.equals(plato.nombre, ignoreCase = true)) {
            mostrarDialogoConfirmarDesmarcar(plato, comidaNombre)
            return
        }

        // 3. Si este servicio YA tiene otro plato asignado, pedir confirmación para reemplazarlo
        if (actual != null && !actual.nombre.equals(plato.nombre, ignoreCase = true)) {
            MaterialAlertDialogBuilder(this)
                .setTitle("Servicio ya seleccionado")
                .setMessage("El servicio de $comidaNombre ya tiene asignado \"${actual.nombre}\". ¿Deseas cambiar esta selección por \"${plato.nombre}\"?")
                .setPositiveButton("Sí, cambiar") { _, _ ->
                    confirmarYAsignarPlato(plato)
                }
                .setNegativeButton("Cancelar", null)
                .show()
            return
        }

        // 4. Asignación directa si el servicio estaba libre
        confirmarYAsignarPlato(plato)
    }

    private fun mostrarDialogoConfirmarDesmarcar(plato: InicioJefaActivity.Plato, servicio: String) {
        val dialogBinding = com.example.movilmanupuladora.databinding.DialogQuitarPlatoBinding.inflate(layoutInflater)
        dialogBinding.txtMensajeQuitarPlato.text = "¿Estás segura de que deseas quitar \"${plato.nombre}\" del servicio de $servicio?"

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
        val comidaNombre = obtenerNombreComidaActual()
        val fechaStr = obtenerClaveFecha(diaSeleccionadoSemanaSiguiente)

        // Si este plato estaba asignado a otro servicio en este mismo día, desasignarlo de allí
        if (comidaSeleccionada != TipoComida.DESAYUNO && platoSeleccionadoDesayuno?.nombre.equals(plato.nombre, ignoreCase = true)) {
            platoSeleccionadoDesayuno = null
            sessionManager.desasignarPlatoDiaServicio(this, fechaStr, "Desayuno")
        }
        if (comidaSeleccionada != TipoComida.ALMUERZO && platoSeleccionadoAlmuerzo?.nombre.equals(plato.nombre, ignoreCase = true)) {
            platoSeleccionadoAlmuerzo = null
            sessionManager.desasignarPlatoDiaServicio(this, fechaStr, "Almuerzo")
        }
        if (comidaSeleccionada != TipoComida.MERIENDA && platoSeleccionadoMerienda?.nombre.equals(plato.nombre, ignoreCase = true)) {
            platoSeleccionadoMerienda = null
            sessionManager.desasignarPlatoDiaServicio(this, fechaStr, "Merienda")
        }

        // Asignar al servicio actual
        when (comidaSeleccionada) {
            TipoComida.DESAYUNO -> platoSeleccionadoDesayuno = plato
            TipoComida.ALMUERZO -> platoSeleccionadoAlmuerzo = plato
            TipoComida.MERIENDA -> platoSeleccionadoMerienda = plato
        }

        actualizarSubtitulosTabs()
        renderizarNumerosSemanaSiguiente()
        configurarViewPagerCarousel()
        configurarMenuDisponiblePAE()

        val cal = diaSeleccionadoSemanaSiguiente
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
                    fecha = fechaStr
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
        tvSubtitulo.text = "Tu plato quedó programado para\nla siguiente semana."
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
            if (currentIndex in 0 until diasSemanaSiguiente.size - 1) {
                val siguienteDia = diasSemanaSiguiente[currentIndex + 1]
                diaSeleccionadoSemanaSiguiente = siguienteDia
                renderizarNumerosSemanaSiguiente()
                cargarPlatosSeleccionados()
                configurarViewPagerCarousel()
                configurarMenuDisponiblePAE()

                val formatoFecha = SimpleDateFormat("EEEE, d 'de' MMMM", Locale.forLanguageTag("es-CO"))
                val fechaStr = formatoFecha.format(siguienteDia.time)
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

        for (dia in diasSemanaSiguiente.take(5)) { // Lunes a Viernes
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

        MaterialAlertDialogBuilder(this)
            .setTitle("Enviar programación al supervisor")
            .setMessage("¿Deseas enviar la programación de la semana al Supervisor PAE?\n\nAl enviarla, se actualizará la minuta semanal para su correspondiente revisión y visto bueno.")
            .setPositiveButton("Sí, enviar") { _, _ ->
                ejecutarEnvioSupervisor()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun ejecutarEnvioSupervisor() {
        lifecycleScope.launch {
            try {
                val sedeNombre = sessionManager.getSedeNombre() ?: "Sede"
                val jefaNombre = sessionManager.getUserName() ?: "Jefa de Cocina"
                val primerDia = diasSemanaSiguiente.first()
                val ultimoDia = diasSemanaSiguiente[4] // Viernes
                val rango = "${SimpleDateFormat("d MMM", Locale.forLanguageTag("es-CO")).format(primerDia.time)} – ${SimpleDateFormat("d MMM", Locale.forLanguageTag("es-CO")).format(ultimoDia.time)}"

                // 1. Guardar todos los platos de la semana en el backend
                for (dia in diasSemanaSiguiente.take(5)) {
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
                            PreparacionSyncManager.guardarPlatoServicio(this@PlanificacionSemanalJefaActivity, serv, plato, id, fechaStr)
                        }
                    }
                }

                // 2. Enviar notificación al Supervisor
                val tituloBody = "Programación Semanal Enviada - $sedeNombre".toRequestBody("text/plain".toMediaTypeOrNull())
                val mensajeBody = "La Jefa de Cocina ($jefaNombre) ha enviado la programación de menús de la semana ($rango). La minuta se encuentra actualizada y lista para su visto bueno.".toRequestBody("text/plain".toMediaTypeOrNull())

                try {
                    RetrofitClient.apiService.enviarNotificacion(tituloBody, mensajeBody, null)
                } catch (_: Exception) { }

                val semanaClave = SimpleDateFormat("yyyy_MM_W", Locale.getDefault()).format(diasSemanaSiguiente.first().time)
                getSharedPreferences("MINUTA_APROBACION", MODE_PRIVATE)
                    .edit()
                    .putBoolean("enviada_supervisor_$semanaClave", true)
                    .apply()

                MaterialAlertDialogBuilder(this@PlanificacionSemanalJefaActivity)
                    .setTitle("✓ Programación Enviada al Supervisor")
                    .setMessage("La programación de la semana ($rango) fue remitida exitosamente al supervisor PAE.\n\nSe ha actualizado la minuta para su revisión y aprobación.")
                    .setPositiveButton("Aceptar") { _, _ ->
                        finish()
                    }
                    .show()
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
                    // Solo reemplazar si el backend contiene el nuevo catálogo completo (15 o más platos)
                    if (lista.size >= 15) {
                        val temp = mutableListOf<InicioJefaActivity.Plato>()

                        for (p in lista) {
                            val nombre = p.nombrePlato ?: "Plato del día"
                            val imagen = if (p.idPlato in 1..20) {
                                MenuPaeCatalog.obtenerMenuPorId(p.idPlato).imagenRes
                            } else {
                                MenuPaeCatalog.obtenerImagenPorNombre(p.nombrePlato ?: p.componente)
                            }
                            val plato = InicioJefaActivity.Plato(id = p.idPlato, nombre = nombre, componente = p.componente ?: "", imagen = imagen)
                            temp.add(plato)
                        }

                        if (temp.isNotEmpty()) {
                            platosAlmuerzo.clear()
                            platosAlmuerzo.addAll(temp)
                            configurarViewPagerCarousel()
                            configurarMenuDisponiblePAE()
                        }
                    }
                }
            } catch (_: Exception) { }
        }
    }
}
