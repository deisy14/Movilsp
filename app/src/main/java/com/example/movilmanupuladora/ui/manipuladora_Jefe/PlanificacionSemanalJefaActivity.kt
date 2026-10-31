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
    }

    // =========================================================
    // CÁLCULO DE LA SEMANA SIGUIENTE (LUNES A DOMINGO)
    // =========================================================

    private fun configurarSemanaSiguiente() {
        diasSemanaSiguiente.clear()

        // Calcular el lunes de la próxima semana
        val calProxSemana = Calendar.getInstance().apply {
            firstDayOfWeek = Calendar.MONDAY
            set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            add(Calendar.WEEK_OF_YEAR, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        for (i in 0 until 7) {
            diasSemanaSiguiente.add(calProxSemana.clone() as Calendar)
            calProxSemana.add(Calendar.DAY_OF_MONTH, 1)
        }

        // Título del mes de la siguiente semana (ej. "Noviembre 2026")
        val mesNombre = SimpleDateFormat("MMMM yyyy", Locale.forLanguageTag("es-CO")).format(diasSemanaSiguiente.first().time)
        binding.txtMesAnio.text = mesNombre.replaceFirstChar {
            if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
        }

        // Por defecto seleccionar el lunes de la siguiente semana
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
                    setTextColor(Color.parseColor("#1E293B"))
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

    // =========================================================
    // TABS DE COMIDA (DESAYUNO, ALMUERZO, MERIENDA)
    // =========================================================

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
        val adapter = SugerenciaIaAdapter(platosActuales) { plato ->
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
            }
        })

        val initialPos = if (platosActuales.size >= 2) 1 else 0
        binding.viewPagerSugerenciasIA.setCurrentItem(initialPos, false)
        posicionPlatoCarousel = initialPos
        actualizarIndicadorPuntos(initialPos, platosActuales.size)
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
            startActivity(Intent(this, TodosLosPlatosActivity::class.java))
        }

        binding.btnInfoIA.setOnClickListener {
            Toast.makeText(
                this,
                "La IA sugiere los mejores menús según los inventarios y la programación semanal PAE.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun seleccionarPlatoParaSiguienteSemana(plato: InicioJefaActivity.Plato) {

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
        val mesAbrev = when (cal.get(Calendar.MONTH)) {
            Calendar.JANUARY -> "ene."
            Calendar.FEBRUARY -> "feb."
            Calendar.MARCH -> "mar."
            Calendar.APRIL -> "abr."
            Calendar.MAY -> "may."
            Calendar.JUNE -> "jun."
            Calendar.JULY -> "jul."
            Calendar.AUGUST -> "ago."
            Calendar.SEPTEMBER -> "sep."
            Calendar.OCTOBER -> "oct."
            Calendar.NOVEMBER -> "nov."
            Calendar.DECEMBER -> "dic."
            else -> ""
        }
        val badgeTexto = "$comidaNombre · $diaSemanaAbrev $diaMes $mesAbrev"

            val formatoDia = try {
                SimpleDateFormat("EEEE d 'de' MMMM", Locale("es", "CO"))
            } catch (_: Exception) {
                SimpleDateFormat("EEEE d 'de' MMMM", Locale.getDefault())
            }

            val fechaTexto = try {
                formatoDia.format(diaSeleccionadoSemanaSiguiente.time)
            } catch (_: Exception) {
                "la fecha seleccionada"
            }

            // Guardar plato en sesión y en la nube
            try {
                sessionManager.savePlatoSeleccionado(nombrePlato, if (idPlato > 0) idPlato else null)
            } catch (e: Exception) {
                android.util.Log.e("PLANIFICACION", "Error en sessionManager: ${e.message}")
            }

        // Mostrar modal personalizado idéntico al diseño
        val viewDialog = layoutInflater.inflate(R.layout.dialog_plato_asignado_exito, null)
        val imgPlato = viewDialog.findViewById<ImageView>(R.id.imgPlatoAsignadoModal)
        val tvNombre = viewDialog.findViewById<TextView>(R.id.tvNombrePlatoAsignadoModal)
        val tvBadge = viewDialog.findViewById<TextView>(R.id.tvBadgePlatoAsignadoModal)
        val tvSubtitulo = viewDialog.findViewById<TextView>(R.id.tvSubtituloPlatoAsignadoModal)
        val btnContinuar = viewDialog.findViewById<Button>(R.id.btnContinuarPlanificandoModal)
        val btnConfigurar = viewDialog.findViewById<Button>(R.id.btnConfigurarEquipoModal)

        val imgRes = if (plato.imagen != 0) plato.imagen else InicioJefaActivity.obtenerImagenPlatoPorNombre(plato.nombre)
        imgPlato.setImageResource(imgRes)
        tvNombre.text = plato.nombre
        tvBadge.text = badgeTexto
        tvSubtitulo.text = "Tu plato quedó programado para\nla siguiente semana."

        val dialog = androidx.appcompat.app.AlertDialog.Builder(this)
            .setView(viewDialog)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        btnContinuar.setOnClickListener {
            dialog.dismiss()
        }

        btnConfigurar.setOnClickListener {
            dialog.dismiss()
            val intent = Intent(this, IniciarPreparacionActivity::class.java).apply {
                putExtra("nombre_menu", plato.nombre)
                putExtra("id_plato", plato.id ?: -1)
                putExtra("jornada", comidaNombre)
            }
            startActivity(intent)
        }

        dialog.show()

        val width = (resources.displayMetrics.widthPixels * 0.90).toInt()
        dialog.window?.setLayout(width, android.view.ViewGroup.LayoutParams.WRAP_CONTENT)
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
