package com.example.movilmanupuladora.ui.manipuladora_Jefe

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
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
import com.example.movilmanupuladora.data.repository.MenuRepository
import com.example.movilmanupuladora.databinding.ActivityPlanificacionSemanalJefaBinding
import com.example.movilmanupuladora.utils.PreparacionSyncManager
import com.example.movilmanupuladora.utils.SessionManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch
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

            configurarSemanaSiguiente()
            configurarEventos()
            configurarTabsComida()
            cargarPlatosSeleccionados()
            configurarViewPagerCarousel()
            configurarMenuDisponiblePAE()
            cargarPlatosDesdeBackend()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Error al abrir planificación: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }

        // Manejar botón atrás con animación de deslizamiento hacia abajo
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                finish()
                overridePendingTransition(R.anim.stay, R.anim.slide_out_bottom)
            }
        })
    }

    override fun onResume() {
        super.onResume()
        cargarPlatosDesdeBackend()
        cargarPlatosSeleccionados()
        configurarViewPagerCarousel()
        configurarMenuDisponiblePAE()
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
            val diaCompleto = tieneDes && tieneAlm && tieneMer

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

    private fun configurarTabsComida() {
        binding.tabDesayuno.setOnClickListener {
            seleccionarTipoComida(TipoComida.DESAYUNO)
        }
        binding.tabAlmuerzo.setOnClickListener {
            seleccionarTipoComida(TipoComida.ALMUERZO)
        }
        binding.tabMerienda.setOnClickListener {
            seleccionarTipoComida(TipoComida.MERIENDA)
        }

        seleccionarTipoComida(TipoComida.DESAYUNO)
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
        posicionPlatoCarousel = 0
        configurarViewPagerCarousel()
        configurarMenuDisponiblePAE()
    }

    private fun obtenerPlatosActuales(): List<InicioJefaActivity.Plato> {
        return try {
            val menusSemana = MenuPaeCatalog.obtenerMenusDeSemana(diaSeleccionadoSemanaSiguiente)
            menusSemana.map {
                InicioJefaActivity.Plato(id = it.id, nombre = it.nombre, componente = it.componente, imagen = it.imagenRes)
            }
        } catch (_: Exception) {
            if (::platosAlmuerzo.isInitialized) platosAlmuerzo.take(5) else emptyList()
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
            seleccionarPlatoParaSiguienteSemana(plato)
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

        binding.viewPagerSugerenciasIA.registerOnPageChangeCallback(object :
            ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                posicionPlatoCarousel = position
                actualizarIndicadorPuntos(position, platosActuales.size)
                actualizarBotonSeleccionarPlato(position, platosActuales)
            }
        })

        val initialPos = if (platosActuales.size >= 2) 1 else 0
        binding.viewPagerSugerenciasIA.setCurrentItem(initialPos, false)
        posicionPlatoCarousel = initialPos
        actualizarIndicadorPuntos(initialPos, platosActuales.size)
        actualizarBotonSeleccionarPlato(initialPos, platosActuales)
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
            Toast.makeText(this, "✓ Planificación del día guardada exitosamente", Toast.LENGTH_SHORT).show()
            renderizarNumerosSemanaSiguiente()
            verificarAvanzarSiguienteDia()
        }
    }

    private fun seleccionarPlatoParaSiguienteSemana(plato: InicioJefaActivity.Plato) {
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
        MaterialAlertDialogBuilder(this)
            .setTitle("Quitar plato del servicio")
            .setMessage("¿Estás segura de que deseas quitar \"${plato.nombre}\" del servicio de $servicio?")
            .setPositiveButton("Sí, quitar") { _, _ ->
                desmarcarPlatoActual(servicio)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun desmarcarPlatoActual(servicio: String) {
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
        Toast.makeText(this, "✓ Plato quitado de $servicio", Toast.LENGTH_SHORT).show()
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
        val des = platoSeleccionadoDesayuno
        val alm = platoSeleccionadoAlmuerzo
        val mer = platoSeleccionadoMerienda

        when {
            // Si acabamos de seleccionar desayuno y falta almuerzo, pasar a almuerzo en este mismo día
            comidaSeleccionada == TipoComida.DESAYUNO && alm == null -> {
                seleccionarTipoComida(TipoComida.ALMUERZO)
                Toast.makeText(this, "✓ Desayuno guardado. Selecciona ahora el Almuerzo", Toast.LENGTH_SHORT).show()
            }
            // Si falta merienda, pasar a merienda en este mismo día
            (comidaSeleccionada == TipoComida.DESAYUNO || comidaSeleccionada == TipoComida.ALMUERZO) && mer == null -> {
                seleccionarTipoComida(TipoComida.MERIENDA)
                Toast.makeText(this, "✓ Guardado. Selecciona ahora la Merienda", Toast.LENGTH_SHORT).show()
            }
            // Si los 3 servicios ya están completos en este día, avanzar al siguiente día
            des != null && alm != null && mer != null -> {
                verificarAvanzarSiguienteDia()
            }
            else -> {
                Toast.makeText(this, "✓ Plato guardado para $servicioGuardado", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun verificarAvanzarSiguienteDia() {
        val des = platoSeleccionadoDesayuno
        val alm = platoSeleccionadoAlmuerzo
        val mer = platoSeleccionadoMerienda

        if (des != null && alm != null && mer != null) {
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
                Toast.makeText(this, "✓ Día completado. Pasando a: $fechaStr", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(this, "✓ ¡Semana completa planificada con éxito!", Toast.LENGTH_LONG).show()
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
