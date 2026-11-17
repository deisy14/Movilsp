package com.example.movilmanupuladora.ui.manipuladora_Jefe
import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.toColorInt
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
import com.example.movilmanupuladora.data.model.SugerenciaRequest
import com.example.movilmanupuladora.data.model.SugerenciaRecetaResponse
import com.example.movilmanupuladora.data.repository.MenuRepository
import com.example.movilmanupuladora.databinding.ActivityPlanificacionSemanalJefaBinding
import com.example.movilmanupuladora.utils.PreparacionSyncManager
import com.example.movilmanupuladora.utils.SessionManager
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

// =========================================================
// MODELO GLOBAL DE PLATOS (Accesible desde cualquier Activity)
// =========================================================
data class PlatoItem(
    val id: Int,
    val nombre: String,
    val componente: String,
    val imagen: Int
)

class PlanificacionSemanalJefaActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPlanificacionSemanalJefaBinding
    private lateinit var sessionManager: SessionManager
    private val menuRepository = MenuRepository(RetrofitClient.apiService)

    private var diaSeleccionadoSemanaSiguiente: Calendar = Calendar.getInstance()
    private val diasSemanaSiguiente = mutableListOf<Calendar>()

    private var comidaSeleccionada = TipoComida.DESAYUNO
    private var posicionPlatoCarousel = 0

    enum class TipoComida {
        DESAYUNO,
        ALMUERZO,
        MERIENDA
    }

    private val platosDesayuno = MenuPaeCatalog.menus.map {
        PlatoItem(id = it.id, nombre = it.nombre, componente = it.componente, imagen = it.imagenRes)
    }.toMutableList()

    private val platosAlmuerzo = MenuPaeCatalog.menus.map {
        PlatoItem(id = it.id, nombre = it.nombre, componente = it.componente, imagen = it.imagenRes)
    }.toMutableList()

    private val platosMerienda = MenuPaeCatalog.menus.map {
        PlatoItem(id = it.id, nombre = it.nombre, componente = it.componente, imagen = it.imagenRes)
    }.toMutableList()

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

        configurarSemanaSiguiente()
        configurarEventos()
        configurarTabsComida()
        configurarViewPagerCarousel()
        configurarMenuDisponiblePAE()
        cargarPlatosDesdeBackend()

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                finish()
                @Suppress("DEPRECATION")
                overridePendingTransition(R.anim.stay, R.anim.slide_out_bottom)
            }
        })
    }

    private fun configurarSemanaSiguiente() {
        diasSemanaSiguiente.clear()

        val calProxSemana = Calendar.getInstance().apply {
            firstDayOfWeek = Calendar.MONDAY
            set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            add(Calendar.WEEK_OF_YEAR, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        repeat(7) {
            diasSemanaSiguiente.add(calProxSemana.clone() as Calendar)
            calProxSemana.add(Calendar.DAY_OF_MONTH, 1)
        }

        val mesNombre = SimpleDateFormat("MMMM yyyy", Locale.forLanguageTag("es-CO")).format(diasSemanaSiguiente.first().time)
        binding.txtMesAnio.text = mesNombre.replaceFirstChar {
            if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
        }

        diaSeleccionadoSemanaSiguiente = diasSemanaSiguiente.first()
        renderizarNumerosSemanaSiguiente()
    }

    private fun renderizarNumerosSemanaSiguiente() {
        binding.layoutNumerosSemanaSiguiente.removeAllViews()

        for (dia in diasSemanaSiguiente) {
            val cell = FrameLayout(this).apply {
                val p = LinearLayout.LayoutParams(0, (40 * resources.displayMetrics.density).toInt(), 1f)
                layoutParams = p
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
                    setTextColor("#1E293B".toColorInt())
                    setTypeface(null, Typeface.BOLD)
                } else {
                    setBackgroundColor(android.graphics.Color.TRANSPARENT)
                    setTextColor("#1E293B".toColorInt())
                }
            }

            cell.isClickable = true
            cell.isFocusable = true
            cell.setOnClickListener {
                diaSeleccionadoSemanaSiguiente = dia
                renderizarNumerosSemanaSiguiente()
                configurarViewPagerCarousel()

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

    private fun configurarTabsComida() {
        binding.tabDesayuno.setOnClickListener { seleccionarTipoComida(TipoComida.DESAYUNO) }
        binding.tabAlmuerzo.setOnClickListener { seleccionarTipoComida(TipoComida.ALMUERZO) }
        binding.tabMerienda.setOnClickListener { seleccionarTipoComida(TipoComida.MERIENDA) }

        seleccionarTipoComida(TipoComida.DESAYUNO)
    }

    private fun seleccionarTipoComida(tipo: TipoComida) {
        comidaSeleccionada = tipo

        fun estiloActivo(tab: LinearLayout, titulo: TextView, sub: TextView) {
            tab.setBackgroundResource(R.drawable.bg_dia_semana_activo)
            titulo.setTextColor("#1E293B".toColorInt())
            sub.setTextColor("#92400E".toColorInt())
        }

        fun estiloInactivo(tab: LinearLayout, titulo: TextView, sub: TextView) {
            tab.setBackgroundResource(R.drawable.bg_dia_semana_inactivo)
            titulo.setTextColor("#64748B".toColorInt())
            sub.setTextColor("#94A3B8".toColorInt())
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

        posicionPlatoCarousel = 0
        configurarViewPagerCarousel()
        configurarMenuDisponiblePAE()
    }

    private fun obtenerPlatosActuales(): List<PlatoItem> {
        val listaBase = when (comidaSeleccionada) {
            TipoComida.DESAYUNO -> platosDesayuno
            TipoComida.ALMUERZO -> platosAlmuerzo
            TipoComida.MERIENDA -> platosMerienda
        }

        if (listaBase.isNotEmpty()) {
            return listaBase
        }

        return mutableListOf(
            PlatoItem(
                id = 1,
                nombre = "Menú PAE Estándar",
                componente = "Alimento balanceado",
                imagen = R.drawable.ic_nav_preparations
            )
        )
    }

    private fun configurarViewPagerCarousel() {
        val platosActuales = obtenerPlatosActuales()
        val adapter = SugerenciaIaAdapter(platosActuales) { plato ->
            solicitarSugerenciaIA(1, plato.id)
        }
        binding.viewPagerSugerenciasIA.adapter = adapter

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

                val platoActual = platosActuales.getOrNull(position)
                if (platoActual != null) {
                    solicitarSugerenciaIA(1, platoActual.id)
                }
            }
        })

        val initialPos = if (platosActuales.size >= 2) 1 else 0
        binding.viewPagerSugerenciasIA.setCurrentItem(initialPos, false)
        posicionPlatoCarousel = initialPos
        actualizarIndicadorPuntos(initialPos, platosActuales.size)

        val platoInicial = platosActuales.getOrNull(initialPos)
        if (platoInicial != null) {
            solicitarSugerenciaIA(1, platoInicial.id)
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
        val adapter = MenuDisponibleAdapter(listaCompleta) { plato ->
            solicitarSugerenciaIA(1, plato.id)
        }
        binding.recyclerMenuDisponiblePAE.adapter = adapter
        binding.recyclerMenuDisponiblePAE.layoutManager = LinearLayoutManager(this, RecyclerView.HORIZONTAL, false)
    }

    private fun configurarEventos() {
        binding.btnVolver.setOnClickListener {
            finish()
            @Suppress("DEPRECATION")
            overridePendingTransition(R.anim.stay, R.anim.slide_out_bottom)
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
                val request = SugerenciaRequest(id_menu = 1, id_plato = plato.id)
                lifecycleScope.launch {
                    try {
                        val response = RetrofitClient.apiService.obtenerSugerenciaReceta(request)
                        if (response.isSuccessful && response.body() != null) {
                            procesarSeleccionPlatoExitoso(response.body()!!)
                        } else {
                            Toast.makeText(this@PlanificacionSemanalJefaActivity, "No se pudo programar el plato", Toast.LENGTH_SHORT).show()
                        }
                    } catch (_: Throwable) {
                        Toast.makeText(this@PlanificacionSemanalJefaActivity, "Error de red al programar", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        binding.btnInfoIA.setOnClickListener {
            Toast.makeText(
                this,
                "La IA sugiere los mejores menús según los inventarios y la programación semanal PAE.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun solicitarSugerenciaIA(idMenu: Int, idPlato: Int) {
        val request = SugerenciaRequest(id_menu = idMenu, id_plato = idPlato)
        binding.txtNinosPresentesInfo.text = "Calculando ingredientes con IA..."

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.obtenerSugerenciaReceta(request)
                if (response.isSuccessful && response.body() != null) {
                    response.body()?.let { sugerencia ->
                        pintarIngredientesEnPantalla(sugerencia)
                    }
                } else {
                    val errorBody = response.errorBody()?.string() ?: ""
                    if (errorBody.contains("<!DOCTYPE html>") || errorBody.contains("<html")) {
                        binding.txtNinosPresentesInfo.text = "Error 404: Ruta de IA no encontrada en Django."
                    } else {
                        binding.txtNinosPresentesInfo.text = "Error del servidor (${response.code()}): $errorBody"
                    }
                }
            } catch (t: Throwable) {
                binding.txtNinosPresentesInfo.text = "Fallo de red: ${t.localizedMessage}"
            }
        }
    }

    private fun pintarIngredientesEnPantalla(sugerencia: SugerenciaRecetaResponse) {
        binding.layoutContenedorIngredientes.removeAllViews()
        binding.txtNinosPresentesInfo.text = "👥 Niños presentes en la sede: ${sugerencia.ninos_presentes}"

        for (ingrediente in sugerencia.ingredientes) {
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
                text = ingrediente.nombre
                setTextColor("#1E293B".toColorInt())
                textSize = 14f
                setTypeface(null, Typeface.BOLD)
            }

            val tvDetalle = TextView(this).apply {
                text = "Requerido: ${ingrediente.cantidad_total} ${ingrediente.unidad}"
                setTextColor("#92400E".toColorInt())
                textSize = 12f
            }

            textLayout.addView(tvNombre)
            textLayout.addView(tvDetalle)
            itemLayout.addView(textLayout)

            val tvEstado = TextView(this).apply {
                text = if (ingrediente.alcanza) "✓ Disponible" else "✕ Insuficiente"
                setTextColor(if (ingrediente.alcanza) "#16A34A".toColorInt() else "#DC2626".toColorInt())
                textSize = 12f
                setTypeface(null, Typeface.BOLD)
            }
            itemLayout.addView(tvEstado)

            binding.layoutContenedorIngredientes.addView(itemLayout)

            val divider = View(this).apply {
                setBackgroundColor("#E2E8F0".toColorInt())
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 1).also {
                    it.topMargin = 4
                    it.bottomMargin = 4
                }
            }
            binding.layoutContenedorIngredientes.addView(divider)
        }
    }

    private fun procesarSeleccionPlatoExitoso(sugerencia: SugerenciaRecetaResponse) {
        val comidaNombre = when (comidaSeleccionada) {
            TipoComida.DESAYUNO -> "Desayuno"
            TipoComida.ALMUERZO -> "Almuerzo"
            TipoComida.MERIENDA -> "Merienda"
        }

        sessionManager.savePlatoSeleccionado(sugerencia.nombre_plato, sugerencia.id_plato)

        lifecycleScope.launch {
            try {
                PreparacionSyncManager.guardarPlatoSeleccionado(
                    this@PlanificacionSemanalJefaActivity,
                    sugerencia.nombre_plato,
                    sugerencia.id_plato,
                    comidaNombre
                )
            } catch (_: Exception) { }
        }

        Toast.makeText(this, "¡Plato ${sugerencia.nombre_plato} programado correctamente!", Toast.LENGTH_SHORT).show()
    }

    private fun cargarPlatosDesdeBackend() {
        lifecycleScope.launch {
            try {
                val platosRes = menuRepository.obtenerPlatos()
                if (platosRes.isSuccessful && !platosRes.body().isNullOrEmpty()) {
                    val lista = platosRes.body()!!
                    val temp = mutableListOf<PlatoItem>()

                    for (p in lista) {
                        val nombre = p.nombrePlato ?: "Plato del día"
                        val imagen = if (p.idPlato in 1..20) {
                            MenuPaeCatalog.obtenerMenuPorId(p.idPlato).imagenRes
                        } else {
                            MenuPaeCatalog.obtenerImagenPorNombre(p.nombrePlato ?: p.componente)
                        }
                        val plato = PlatoItem(id = p.idPlato, nombre = nombre, componente = p.componente ?: "", imagen = imagen)
                        temp.add(plato)
                    }

                    if (temp.isNotEmpty()) {
                        platosAlmuerzo.clear()
                        platosAlmuerzo.addAll(temp)
                        configurarViewPagerCarousel()
                        configurarMenuDisponiblePAE()
                    }
                }
            } catch (_: Exception) { }
        }
    }
}