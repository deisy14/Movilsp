
package com.example.movilmanupuladora.ui.manipuladora_Jefe

import android.animation.ValueAnimator
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.movilmanupuladora.R
import com.example.movilmanupuladora.data.api.RetrofitClient
import com.example.movilmanupuladora.data.model.MenuPaeCatalog
import com.example.movilmanupuladora.databinding.ActivityInicioJefaBinding
import com.example.movilmanupuladora.utils.NavigationHelperJefa
import com.example.movilmanupuladora.utils.PreparacionSyncManager
import com.example.movilmanupuladora.utils.SedeContratoManager
import com.example.movilmanupuladora.utils.SessionManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class InicioJefaActivity : AppCompatActivity() {

    private lateinit var binding: ActivityInicioJefaBinding
    private lateinit var sessionManager: SessionManager

    private var diaSeleccionado: Calendar = Calendar.getInstance()
    private val diasSemanaActual = mutableListOf<Calendar>()

    data class Plato(
        val id: Int,
        val nombre: String,
        val componente: String,
        val imagen: Int,
        val imagenUrl: String? = null
    )

    companion object {
        val imagenesPlatos = listOf(
            R.drawable.plato1,
            R.drawable.plato2,
            R.drawable.plato3,
            R.drawable.plato4,
            R.drawable.plato5,
            R.drawable.plato6,
            R.drawable.plato7,
            R.drawable.plato8,
            R.drawable.plato9,
            R.drawable.plato10,
            R.drawable.plato11,
            R.drawable.plato14,
            R.drawable.plato15
        )

        fun obtenerImagenPlato(id: Int? = null, index: Int = 0): Int {
            if (id != null && id in 1..20) {
                return MenuPaeCatalog.obtenerMenuPorId(id).imagenRes
            }
            val semilla = Math.abs(id ?: index)
            return imagenesPlatos[semilla % imagenesPlatos.size]
        }

        fun obtenerImagenPlatoPorNombre(nombre: String?): Int {
            return MenuPaeCatalog.obtenerImagenPorNombre(nombre)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Edge-to-Edge: el fondo amarillo se dibuja detrás de la barra de estado como en el login
        WindowCompat.setDecorFitsSystemWindows(window, false)

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            window.attributes.layoutInDisplayCutoutMode =
                android.view.WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }

        @Suppress("DEPRECATION")
        window.statusBarColor = Color.TRANSPARENT
        @Suppress("DEPRECATION")
        window.navigationBarColor = Color.TRANSPARENT
        WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars = true

        binding = ActivityInicioJefaBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.franjaSuperiorAmarilla) { view, insets ->
            val statusBarHeight = insets.getInsets(WindowInsetsCompat.Type.systemBars()).top
            view.setPadding(
                view.paddingLeft,
                statusBarHeight + (10 * resources.displayMetrics.density).toInt(),
                view.paddingRight,
                view.paddingBottom
            )
            insets
        }

        sessionManager = SessionManager(this)

        configurarHeaderJefa()
        configurarSemanaActual()
        configurarEventos()
        configurarNavegacionInferior()

        actualizarResumen()
        actualizarPlatosVisuales()
        actualizarBadgeNotificaciones()
        cargarAsistenciaDelBackend()

        lifecycleScope.launch {
            try {
                val idSede = sessionManager.getSedeId().takeIf { it > 0 }
                val sedeNombre = sessionManager.getSedeNombre()
                ManipuladorasManager.cargarDesdeBackend(this@InicioJefaActivity, idSede, sedeNombre)
                NotificacionesJefaManager.sincronizarNotificacionesDelBackend(this@InicioJefaActivity)
                SedeContratoManager.sincronizarSeccionesContrato(this@InicioJefaActivity, idSede ?: 1)
                actualizarBadgeNotificaciones()
                actualizarResumen()
                actualizarPlatosVisuales()
            } catch (_: Exception) { }
        }

        // Monitoreo periódico en segundo plano para reflejar nuevas notificaciones automáticamente
        lifecycleScope.launch {
            while (isActive) {
                delay(20000)
                try {
                    NotificacionesJefaManager.sincronizarNotificacionesDelBackend(this@InicioJefaActivity)
                    actualizarBadgeNotificaciones()
                } catch (_: Exception) { }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars = true
        actualizarResumen()
        actualizarPlatosVisuales()
        actualizarBadgeNotificaciones()
        cargarAsistenciaDelBackend()
        lifecycleScope.launch {
            try {
                val idSede = sessionManager.getSedeId().takeIf { it > 0 }
                val sedeNombre = sessionManager.getSedeNombre()
                ManipuladorasManager.cargarDesdeBackend(this@InicioJefaActivity, idSede, sedeNombre)
                NotificacionesJefaManager.sincronizarNotificacionesDelBackend(this@InicioJefaActivity)
                SedeContratoManager.sincronizarSeccionesContrato(this@InicioJefaActivity, idSede ?: 1)
                actualizarResumen()
                actualizarBadgeNotificaciones()

                val res = RetrofitClient.apiService.obtenerPlatos()
                if (res.isSuccessful && !res.body().isNullOrEmpty()) {
                    MenuPaeCatalog.registrarPlatosDesdeBackend(res.body()!!)
                }
                PreparacionSyncManager.sincronizarDesdeBackend(this@InicioJefaActivity)
                actualizarPlatosVisuales()
            } catch (_: Exception) { }
        }
    }

    // =========================================================
    // CONFIGURACIÓN DINÁMICA DE LA SEMANA (LUN - VIE)
    // =========================================================

    private fun configurarSemanaActual() {
        diasSemanaActual.clear()

        // Calcular el lunes de la semana actual
        val calLunes = Calendar.getInstance().apply {
            firstDayOfWeek = Calendar.MONDAY
            set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        for (i in 0 until 7) {
            diasSemanaActual.add(calLunes.clone() as Calendar)
            calLunes.add(Calendar.DAY_OF_MONTH, 1)
        }

        // Subtítulo con rango de semana: e.g. "Menús programados · 2 – 8 de noviembre"
        val primerDia = diasSemanaActual.first()
        val ultimoDia = diasSemanaActual.last()
        val mesNombre = SimpleDateFormat("MMMM", Locale.forLanguageTag("es-CO")).format(primerDia.time)
        val rangoTexto = "Menús programados · ${primerDia.get(Calendar.DAY_OF_MONTH)} – ${ultimoDia.get(Calendar.DAY_OF_MONTH)} de $mesNombre"
        binding.txtRangoSemana.text = rangoTexto

        // Asignar los números de cada día
        binding.txtNumDiaLun.text = diasSemanaActual[0].get(Calendar.DAY_OF_MONTH).toString()
        binding.txtNumDiaMar.text = diasSemanaActual[1].get(Calendar.DAY_OF_MONTH).toString()
        binding.txtNumDiaMie.text = diasSemanaActual[2].get(Calendar.DAY_OF_MONTH).toString()
        binding.txtNumDiaJue.text = diasSemanaActual[3].get(Calendar.DAY_OF_MONTH).toString()
        binding.txtNumDiaVie.text = diasSemanaActual[4].get(Calendar.DAY_OF_MONTH).toString()
        binding.txtNumDiaSab.text = diasSemanaActual[5].get(Calendar.DAY_OF_MONTH).toString()
        binding.txtNumDiaDom.text = diasSemanaActual[6].get(Calendar.DAY_OF_MONTH).toString()

        // Por defecto seleccionar el día de hoy si está dentro de la semana
        val hoy = Calendar.getInstance()
        val indiceHoy = diasSemanaActual.indexOfFirst { esMismoDia(it, hoy) }
        val diaInicial = if (indiceHoy != -1) diasSemanaActual[indiceHoy] else diasSemanaActual.first()

        seleccionarDia(diaInicial)
    }

    private fun seleccionarDia(cal: Calendar) {
        diaSeleccionado = cal.clone() as Calendar

        // Actualizar visual de los 7 botones
        actualizarBotonDia(binding.btnDiaLun, binding.txtNomDiaLun, binding.txtNumDiaLun, esMismoDia(diasSemanaActual[0], diaSeleccionado))
        actualizarBotonDia(binding.btnDiaMar, binding.txtNomDiaMar, binding.txtNumDiaMar, esMismoDia(diasSemanaActual[1], diaSeleccionado))
        actualizarBotonDia(binding.btnDiaMie, binding.txtNomDiaMie, binding.txtNumDiaMie, esMismoDia(diasSemanaActual[2], diaSeleccionado))
        actualizarBotonDia(binding.btnDiaJue, binding.txtNomDiaJue, binding.txtNumDiaJue, esMismoDia(diasSemanaActual[3], diaSeleccionado))
        actualizarBotonDia(binding.btnDiaVie, binding.txtNomDiaVie, binding.txtNumDiaVie, esMismoDia(diasSemanaActual[4], diaSeleccionado))
        actualizarBotonDia(binding.btnDiaSab, binding.txtNomDiaSab, binding.txtNumDiaSab, esMismoDia(diasSemanaActual[5], diaSeleccionado))
        actualizarBotonDia(binding.btnDiaDom, binding.txtNomDiaDom, binding.txtNumDiaDom, esMismoDia(diasSemanaActual[6], diaSeleccionado))

        // Formatear texto de fecha: "Lun, 26 de octubre"
        val formato = SimpleDateFormat("EEE, d 'de' MMMM", Locale.forLanguageTag("es-CO"))
        val textoFecha = formato.format(diaSeleccionado.time).replaceFirstChar {
            if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
        }

        binding.txtFechaSeleccionadaBanner.text = textoFecha
        binding.txtFechaResumenDia.text = textoFecha

        actualizarPlatosVisuales()
    }

    private fun actualizarBotonDia(
        container: LinearLayout,
        txtNombre: TextView,
        txtNumero: TextView,
        activo: Boolean
    ) {
        if (activo) {
            container.setBackgroundResource(R.drawable.bg_dia_semana_activo)
            container.elevation = 4f * resources.displayMetrics.density
            txtNombre.setTextColor(Color.parseColor("#1E293B"))
            txtNumero.setTextColor(Color.parseColor("#1E293B"))
        } else {
            container.setBackgroundResource(R.drawable.bg_dia_semana_inactivo)
            container.elevation = 0f
            txtNombre.setTextColor(Color.parseColor("#64748B"))
            txtNumero.setTextColor(Color.parseColor("#1E293B"))
        }
    }

    private fun esMismoDia(c1: Calendar, c2: Calendar): Boolean {
        return c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR) &&
                c1.get(Calendar.DAY_OF_YEAR) == c2.get(Calendar.DAY_OF_YEAR)
    }

    // =========================================================
    // ACTUALIZACIÓN DE PLATOS DEL DÍA SELECCIONADO
    // =========================================================

    private fun actualizarPlatosVisuales() {
        val fechaClave = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(diaSeleccionado.time)

        // 1. DESAYUNO
        val nomDes = sessionManager.getPlatoDiaServicio(this, fechaClave, "Desayuno")
        if (!nomDes.isNullOrBlank()) {
            binding.badgeEstadoDesayuno.text = "Seleccionado"
            binding.badgeEstadoDesayuno.setBackgroundResource(R.drawable.bg_chip_seleccionado)
            binding.badgeEstadoDesayuno.setTextColor(Color.parseColor("#15803D"))
            binding.imgDesayuno.visibility = View.VISIBLE
            binding.imgDesayuno.setImageResource(obtenerImagenPlatoPorNombre(nomDes))
            binding.txtNombreDesayuno.text = nomDes
            binding.lblComponenteDesayuno.visibility = View.VISIBLE
            val compDes = MenuPaeCatalog.obtenerComponentesPorNombre(nomDes).joinToString(", ")
            binding.txtComponenteDesayuno.text = compDes
        } else {
            binding.badgeEstadoDesayuno.text = "Pendiente"
            binding.badgeEstadoDesayuno.setBackgroundResource(R.drawable.bg_chip_pendiente)
            binding.badgeEstadoDesayuno.setTextColor(Color.parseColor("#92400E"))
            binding.imgDesayuno.visibility = View.GONE
            binding.txtNombreDesayuno.text = "Sin plato seleccionado"
            binding.lblComponenteDesayuno.visibility = View.GONE
            binding.txtComponenteDesayuno.text = "La manipuladora jefe aún no ha seleccionado el plato para esta jornada"
        }

        // 2. ALMUERZO
        val nomAlm = sessionManager.getPlatoDiaServicio(this, fechaClave, "Almuerzo")
        if (!nomAlm.isNullOrBlank()) {
            binding.badgeEstadoAlmuerzo.text = "Seleccionado"
            binding.badgeEstadoAlmuerzo.setBackgroundResource(R.drawable.bg_chip_seleccionado)
            binding.badgeEstadoAlmuerzo.setTextColor(Color.parseColor("#15803D"))
            binding.imgAlmuerzo.visibility = View.VISIBLE
            binding.imgAlmuerzo.setImageResource(obtenerImagenPlatoPorNombre(nomAlm))
            binding.txtNombreAlmuerzo.text = nomAlm
            binding.lblComponenteAlmuerzo.visibility = View.VISIBLE
            val compAlm = MenuPaeCatalog.obtenerComponentesPorNombre(nomAlm).joinToString(", ")
            binding.txtComponenteAlmuerzo.text = compAlm
        } else {
            binding.badgeEstadoAlmuerzo.text = "Pendiente"
            binding.badgeEstadoAlmuerzo.setBackgroundResource(R.drawable.bg_chip_pendiente)
            binding.badgeEstadoAlmuerzo.setTextColor(Color.parseColor("#92400E"))
            binding.imgAlmuerzo.visibility = View.GONE
            binding.txtNombreAlmuerzo.text = "Sin plato seleccionado"
            binding.lblComponenteAlmuerzo.visibility = View.GONE
            binding.txtComponenteAlmuerzo.text = "La manipuladora jefe aún no ha seleccionado el plato para esta jornada"
        }

        // 3. MERIENDA
        val nomMer = sessionManager.getPlatoDiaServicio(this, fechaClave, "Merienda")
        if (!nomMer.isNullOrBlank()) {
            binding.badgeEstadoMerienda.text = "Seleccionado"
            binding.badgeEstadoMerienda.setBackgroundResource(R.drawable.bg_chip_seleccionado)
            binding.badgeEstadoMerienda.setTextColor(Color.parseColor("#15803D"))
            binding.imgMerienda.visibility = View.VISIBLE
            binding.imgMerienda.setImageResource(obtenerImagenPlatoPorNombre(nomMer))
            binding.txtNombreMerienda.text = nomMer
            binding.lblComponenteMerienda.visibility = View.VISIBLE
            val compMer = MenuPaeCatalog.obtenerComponentesPorNombre(nomMer).joinToString(", ")
            binding.txtComponenteMerienda.text = compMer
        } else {
            binding.badgeEstadoMerienda.text = "Pendiente"
            binding.badgeEstadoMerienda.setBackgroundResource(R.drawable.bg_chip_pendiente)
            binding.badgeEstadoMerienda.setTextColor(Color.parseColor("#92400E"))
            binding.imgMerienda.visibility = View.GONE
            binding.txtNombreMerienda.text = "Sin plato seleccionado"
            binding.lblComponenteMerienda.visibility = View.GONE
            binding.txtComponenteMerienda.text = "La manipuladora jefe aún no ha seleccionado el plato para esta jornada"
        }

        val seccionesActivas = SedeContratoManager.obtenerSeccionesActivas(this).ifEmpty { setOf("Desayuno") }
        acomodarTarjetasSegunSecciones(seccionesActivas)
    }

    /**
     * Acomoda visualmente las tarjetas de menús seleccionados según el contrato de la sede:
     * - Si la sede solo maneja 1 sección (ej: solo Desayuno), la tarjeta se centra como Hero Card destacada (260dp de ancho).
     * - Si maneja 2 secciones (ej: Desayuno y Almuerzo), ambas se disponen lado a lado centradas en pantalla (165dp c/u).
     * - Si maneja 3 secciones (Desayuno, Almuerzo y Merienda), se activan las 3 con el carrusel de desplazamiento horizontal.
     */
    private fun acomodarTarjetasSegunSecciones(seccionesActivas: Set<String>) {
        val density = resources.displayMetrics.density

        val tieneDesayuno = seccionesActivas.contains("Desayuno")
        val tieneAlmuerzo = seccionesActivas.contains("Almuerzo")
        val tieneMerienda = seccionesActivas.contains("Merienda")

        binding.cardDesayuno.visibility = if (tieneDesayuno) View.VISIBLE else View.GONE
        binding.cardAlmuerzo.visibility = if (tieneAlmuerzo) View.VISIBLE else View.GONE
        binding.cardMerienda.visibility = if (tieneMerienda) View.VISIBLE else View.GONE

        val visibles = listOf(
            Pair(binding.cardDesayuno, tieneDesayuno),
            Pair(binding.cardAlmuerzo, tieneAlmuerzo),
            Pair(binding.cardMerienda, tieneMerienda)
        ).filter { it.second }.map { it.first }

        val count = visibles.size

        when (count) {
            1 -> {
                // Solo 1 sección (ej: solo Desayuno) -> Centrada en la pantalla
                binding.layoutContenedorCardsComida.gravity = Gravity.CENTER
                binding.layoutContenedorCardsComida.setPadding((16 * density).toInt(), 0, (16 * density).toInt(), 0)

                val cardUnica = visibles.first()
                val params = cardUnica.layoutParams as ViewGroup.MarginLayoutParams
                params.width = (260 * density).toInt()
                params.setMargins(0, 0, 0, 0)
                cardUnica.layoutParams = params

                cardUnica.scaleX = 1.0f
                cardUnica.scaleY = 1.0f
                cardUnica.alpha = 1.0f
                cardUnica.elevation = 4f * density
            }
            2 -> {
                // 2 secciones (ej: Desayuno y Almuerzo) -> Centradas y balanceadas
                binding.layoutContenedorCardsComida.gravity = Gravity.CENTER
                binding.layoutContenedorCardsComida.setPadding((12 * density).toInt(), 0, (12 * density).toInt(), 0)

                visibles.forEachIndexed { index, card ->
                    val params = card.layoutParams as ViewGroup.MarginLayoutParams
                    params.width = (165 * density).toInt()
                    val marginEnd = if (index == 0) (14 * density).toInt() else 0
                    params.setMargins(0, 0, marginEnd, 0)
                    card.layoutParams = params

                    card.scaleX = 1.0f
                    card.scaleY = 1.0f
                    card.alpha = 1.0f
                    card.elevation = 3f * density
                }
            }
            else -> {
                // 3 o más secciones -> Carrusel horizontal dinámico
                binding.layoutContenedorCardsComida.gravity = Gravity.CENTER_VERTICAL
                val relleno = (16 * density).toInt()
                binding.layoutContenedorCardsComida.setPadding(relleno, 0, relleno, 0)

                visibles.forEachIndexed { index, card ->
                    val params = card.layoutParams as ViewGroup.MarginLayoutParams
                    params.width = (180 * density).toInt()
                    val marginEnd = if (index < visibles.size - 1) (16 * density).toInt() else 0
                    params.setMargins(0, 0, marginEnd, 0)
                    card.layoutParams = params
                }
                actualizarEscalaCarrusel()
            }
        }
    }

    // =========================================================
    // EVENTOS Y ACCIONES
    // =========================================================

    private fun configurarEventos() {
        // Clic en los días de la semana (7 días)
        binding.btnDiaLun.setOnClickListener { if (diasSemanaActual.isNotEmpty()) seleccionarDia(diasSemanaActual[0]) }
        binding.btnDiaMar.setOnClickListener { if (diasSemanaActual.size > 1) seleccionarDia(diasSemanaActual[1]) }
        binding.btnDiaMie.setOnClickListener { if (diasSemanaActual.size > 2) seleccionarDia(diasSemanaActual[2]) }
        binding.btnDiaJue.setOnClickListener { if (diasSemanaActual.size > 3) seleccionarDia(diasSemanaActual[3]) }
        binding.btnDiaVie.setOnClickListener { if (diasSemanaActual.size > 4) seleccionarDia(diasSemanaActual[4]) }
        binding.btnDiaSab.setOnClickListener { if (diasSemanaActual.size > 5) seleccionarDia(diasSemanaActual[5]) }
        binding.btnDiaDom.setOnClickListener { if (diasSemanaActual.size > 6) seleccionarDia(diasSemanaActual[6]) }

        // Clic en la flecha amarilla del recuadro del calendario -> Abre Pantalla 2 (MenuDelDiaMesActivity)
        binding.bannerCalendarioMes.setOnClickListener {
            val intent = Intent(this, MenuDelDiaMesActivity::class.java).apply {
                putExtra("fecha_milisegundos", diaSeleccionado.timeInMillis)
            }
            startActivity(intent)
        }

        // Clic en "Registra la asistencia" -> Abre AsistenciaJefaActivity
        binding.bannerRegistraAsistencia.setOnClickListener {
            val intent = Intent(this, AsistenciaJefaActivity::class.java)
            startActivity(intent)
        }


        // Clic en "Resumen del día" -> Abre AsistenciaJefaActivity
        binding.cardResumenDia.setOnClickListener {
            val intent = Intent(this, AsistenciaJefaActivity::class.java)
            startActivity(intent)
        }

        // Clic en tarjetas de comida -> Abre AsignarComponentesActivity para el servicio correspondiente
        binding.cardDesayuno.setOnClickListener {
            val fechaClave = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(diaSeleccionado.time)
            val intent = Intent(this, AsignarComponentesActivity::class.java).apply {
                putExtra("fecha_seleccionada", fechaClave)
                putExtra("servicio", "Desayuno")
            }
            startActivity(intent)
        }

        binding.cardAlmuerzo.setOnClickListener {
            val fechaClave = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(diaSeleccionado.time)
            val intent = Intent(this, AsignarComponentesActivity::class.java).apply {
                putExtra("fecha_seleccionada", fechaClave)
                putExtra("servicio", "Almuerzo")
            }
            startActivity(intent)
        }

        binding.cardMerienda.setOnClickListener {
            val fechaClave = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(diaSeleccionado.time)
            val intent = Intent(this, AsignarComponentesActivity::class.java).apply {
                putExtra("fecha_seleccionada", fechaClave)
                putExtra("servicio", "Merienda")
            }
            startActivity(intent)
        }

        // Efecto carrusel: la tarjeta activa se ve más grande de forma suave y continua
        binding.scrollMenusAsignados.setOnScrollChangeListener { _, _, _, _, _ ->
            actualizarEscalaCarrusel()
            scrollCheckRunnable?.let { binding.scrollMenusAsignados.removeCallbacks(it) }
            scrollCheckRunnable = Runnable { ajustarTarjetaAlCentro() }
            binding.scrollMenusAsignados.postDelayed(scrollCheckRunnable, 140)
        }

        // Estado inicial: Desayuno seleccionado al inicio
        binding.scrollMenusAsignados.post {
            prepararCarrusel()
            binding.scrollMenusAsignados.post { centrarTarjeta(0, suave = false) }
        }
    }

    private var servicioComidaSeleccionado = 0
    private var scrollCheckRunnable: Runnable? = null

    private val tarjetasComida: List<View>
        get() = listOf(binding.cardDesayuno, binding.cardAlmuerzo, binding.cardMerienda)

    /** Relleno lateral moderado (16dp) para que las tarjetas no queden pegadas pero sin dejar espacio vacío excesivo */
    private fun prepararCarrusel() {
        val density = resources.displayMetrics.density
        val relleno = (16 * density).toInt()
        binding.layoutContenedorCardsComida.setPadding(relleno, 0, relleno, 0)
        tarjetasComida.forEach {
            it.pivotX = it.width / 2f
            it.pivotY = it.height / 2f
        }
    }

    /** Escala cada tarjeta de forma continua según el desplazamiento en el scroll */
    private fun actualizarEscalaCarrusel() {
        val scroll = binding.scrollMenusAsignados
        val contenedor = binding.layoutContenedorCardsComida
        val density = resources.displayMetrics.density

        val visibles = tarjetasComida.filter { it.visibility == View.VISIBLE }
        if (visibles.size <= 2) {
            visibles.forEach { card ->
                card.scaleX = 1f
                card.scaleY = 1f
                card.alpha = 1f
                card.elevation = 3f * density
            }
            return
        }

        val maxScroll = (contenedor.width - scroll.width).coerceAtLeast(1)
        val progreso = (scroll.scrollX.toFloat() / maxScroll).coerceIn(0f, 1f)
        val idxFloat = progreso * 2f // 0 = Desayuno, 1 = Almuerzo, 2 = Merienda

        tarjetasComida.forEachIndexed { i, card ->
            if (card.visibility != View.VISIBLE) return@forEachIndexed
            val dist = kotlin.math.abs(i - idxFloat)
            val cercania = (1f - dist).coerceIn(0f, 1f)

            val escala = 0.88f + 0.12f * cercania
            card.pivotX = card.width / 2f
            card.pivotY = card.height / 2f
            card.scaleX = escala
            card.scaleY = escala
            card.alpha = 0.78f + 0.22f * cercania
            card.elevation = (1f + 5f * cercania) * density
        }

        servicioComidaSeleccionado = kotlin.math.round(idxFloat).toInt().coerceIn(0, 2)
    }

    /** Al soltar el dedo, acomoda suavemente la tarjeta más cercana en su posición natural */
    private fun ajustarTarjetaAlCentro() {
        val visibles = tarjetasComida.filter { it.visibility == View.VISIBLE }
        if (visibles.size <= 2) return

        val scroll = binding.scrollMenusAsignados
        val contenedor = binding.layoutContenedorCardsComida
        val maxScroll = (contenedor.width - scroll.width).coerceAtLeast(1)
        val destinoX = (servicioComidaSeleccionado * maxScroll / 2.0).toInt()
        if (kotlin.math.abs(destinoX - scroll.scrollX) > 4) {
            scroll.smoothScrollTo(destinoX, 0)
        }
    }

    private fun centrarTarjeta(index: Int, suave: Boolean = true) {
        val scroll = binding.scrollMenusAsignados
        val contenedor = binding.layoutContenedorCardsComida
        val visibles = tarjetasComida.filter { it.visibility == View.VISIBLE }
        if (visibles.size <= 2) {
            actualizarEscalaCarrusel()
            return
        }
        scroll.post {
            val maxScroll = (contenedor.width - scroll.width).coerceAtLeast(1)
            val destinoX = (index * maxScroll / 2.0).toInt()
            if (suave) scroll.smoothScrollTo(destinoX, 0) else scroll.scrollTo(destinoX, 0)
            servicioComidaSeleccionado = index
            actualizarEscalaCarrusel()
        }
    }

    @Suppress("UNUSED_PARAMETER")
    private fun aplicarEfectoSeleccionComida(index: Int, animar: Boolean = true, scroll: Boolean = true) {
        centrarTarjeta(index, suave = animar)
    }

    private fun configurarHeaderJefa() {
        val nombreUser = sessionManager.getUserName() ?: "Jefa de Cocina"
        binding.txtSaludoJefa.text = "Hola, $nombreUser"
        val sedeNombre = sessionManager.getUbicacionCompleta()
        binding.txtInstitucionHeaderJefa.text = sedeNombre

        val iniciales = nombreUser.split(" ")
            .filter { it.isNotEmpty() }
            .take(2)
            .joinToString("") { it.first().uppercase() }
            .ifEmpty { "JF" }
        binding.txtInicialesJefa.text = iniciales

        binding.btnPerfilHeader.setOnClickListener {
            val intent = Intent(this, PerfilJefaActivity::class.java)
            startActivity(intent)
        }

        binding.btnNotificacionesJefa.setOnClickListener {
            val intent = Intent(this, HistorialNotificacionesJefaActivity::class.java)
            startActivity(intent)
        }
    }

    private fun actualizarResumen() {
        val p = AsistenciaManager.obtenerPrimaria(this)
        val s = AsistenciaManager.obtenerSecundaria(this)
        val suma = p + s
        val guardados = if (suma > 0) {
            suma
        } else {
            AsistenciaManager.obtenerManana(this)
        }

        binding.txtRacionesTotales.text = guardados.toString()
        binding.txtMenusActivos.text = SedeContratoManager.obtenerSeccionesActivas(this).ifEmpty { setOf("Desayuno") }.size.toString()

        val idSede = sessionManager.getSedeId().takeIf { it > 0 }
        val cantManipuladoras = ManipuladorasManager.obtenerManipuladorasSede(idSede, sessionManager.getSedeNombre(), this).size
        binding.txtCantManipuladoras.text = cantManipuladoras.toString()
        actualizarBadgeNotificaciones()
    }

    private fun actualizarBadgeNotificaciones() {
        try {
            val conteo = NotificacionesJefaManager.obtenerCantidadNoLeidas(this)
            if (conteo > 0) {
                binding.badgeNotificacionesJefa.visibility = View.VISIBLE
                binding.badgeNotificacionesJefa.bringToFront()
                binding.badgeNotificacionesJefa.text = if (conteo > 99) "99+" else conteo.toString()
            } else {
                binding.badgeNotificacionesJefa.visibility = View.GONE
            }
        } catch (_: Exception) {
            try {
                binding.badgeNotificacionesJefa.visibility = View.GONE
            } catch (_: Exception) { }
        }
    }

    private fun cargarAsistenciaDelBackend() {
        lifecycleScope.launch {
            try {
                sessionManager.fetchAuthToken()
                val fechaHoy = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

                // 1. Sincronizar en tiempo real desde Turnos
                com.example.movilmanupuladora.utils.PreparacionSyncManager.sincronizarDesdeBackend(this@InicioJefaActivity)

                // 2. Consultar asistencia diaria del backend exclusivamente para hoy
                val asistenciasRes = RetrofitClient.apiService.obtenerAsistenciaDiaria()
                if (asistenciasRes.isSuccessful && !asistenciasRes.body().isNullOrEmpty()) {
                    val lista = asistenciasRes.body()!!
                    val asistenciasHoy = lista.filter { it.fecha.trim().startsWith(fechaHoy) }

                    val regPrimaria = asistenciasHoy.filter { it.obtenerIdGradoReal() == 16 }.maxByOrNull { it.obtenerIdReal() ?: 0 }
                    val regSecundaria = asistenciasHoy.filter { it.obtenerIdGradoReal() == 19 }.maxByOrNull { it.obtenerIdReal() ?: 0 }

                    var p = regPrimaria?.ninosPresentes ?: AsistenciaManager.obtenerPrimaria(this@InicioJefaActivity)
                    var s = regSecundaria?.ninosPresentes ?: AsistenciaManager.obtenerSecundaria(this@InicioJefaActivity)

                    if (regPrimaria == null && regSecundaria == null && asistenciasHoy.isNotEmpty()) {
                        val tot = asistenciasHoy.sumOf { it.ninosPresentes }
                        p = (tot * 0.6).toInt()
                        s = tot - p
                    }

                    val total = p + s
                    if (total > 0) {
                        AsistenciaManager.guardarPrimaria(this@InicioJefaActivity, p)
                        AsistenciaManager.guardarSecundaria(this@InicioJefaActivity, s)
                        AsistenciaManager.guardarManana(this@InicioJefaActivity, total)
                    }

                    actualizarResumen()
                }
            } catch (_: Exception) { }
        }
    }

    private fun configurarNavegacionInferior() {
        NavigationHelperJefa.setupBarraNavegacionJefa(
            this,
            binding.barraNavegacionJefa,
            NavigationHelperJefa.TabJefa.INICIO
        )
    }
}
