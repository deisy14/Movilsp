package com.example.movilmanupuladora.ui.manipuladora_Jefe

import android.animation.ValueAnimator
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
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
import com.example.movilmanupuladora.utils.SessionManager
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
        val imagen: Int
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

        // Barra de estado amarilla (#F5B81F) con iconos oscuros (hora, wifi, batería)
        window.statusBarColor = Color.parseColor("#F5B81F")
        WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars = true

        binding = ActivityInicioJefaBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessionManager = SessionManager(this)

        configurarHeaderJefa()
        configurarSemanaActual()
        configurarEventos()
        configurarNavegacionInferior()

        actualizarResumen()
        cargarAsistenciaDelBackend()
    }

    override fun onResume() {
        super.onResume()
        actualizarResumen()
        cargarAsistenciaDelBackend()
        lifecycleScope.launch {
            try {
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
        val menuDia = MenuPaeCatalog.obtenerMenuDiaCompleto(diaSeleccionado)
        val fechaClave = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(diaSeleccionado.time)

        val nomDes = sessionManager.getPlatoDiaServicio(this, fechaClave, "Desayuno") ?: menuDia.desayuno.nombre
        val compDes = MenuPaeCatalog.obtenerComponentesPorNombre(nomDes).joinToString(", ")
        binding.imgDesayuno.setImageResource(obtenerImagenPlatoPorNombre(nomDes))
        binding.txtNombreDesayuno.text = nomDes
        binding.txtComponenteDesayuno.text = compDes

        val nomAlm = sessionManager.getPlatoDiaServicio(this, fechaClave, "Almuerzo") ?: menuDia.almuerzo.nombre
        val compAlm = MenuPaeCatalog.obtenerComponentesPorNombre(nomAlm).joinToString(", ")
        binding.imgAlmuerzo.setImageResource(obtenerImagenPlatoPorNombre(nomAlm))
        binding.txtNombreAlmuerzo.text = nomAlm
        binding.txtComponenteAlmuerzo.text = compAlm

        val nomMer = sessionManager.getPlatoDiaServicio(this, fechaClave, "Merienda") ?: menuDia.merienda.nombre
        val compMer = MenuPaeCatalog.obtenerComponentesPorNombre(nomMer).joinToString(", ")
        binding.imgMerienda.setImageResource(obtenerImagenPlatoPorNombre(nomMer))
        binding.txtNombreMerienda.text = nomMer
        binding.txtComponenteMerienda.text = compMer
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

        // Clic en tarjetas de comida -> Aplica el efecto de tamaño y abre el plato al confirmar selección
        binding.cardDesayuno.setOnClickListener {
            if (servicioComidaSeleccionado == 0) {
                val intent = Intent(this, MenuDelDiaMesActivity::class.java).apply {
                    putExtra("fecha_milisegundos", diaSeleccionado.timeInMillis)
                    putExtra("servicio", "Desayuno")
                }
                startActivity(intent)
            } else {
                aplicarEfectoSeleccionComida(0, animar = true, scroll = true)
            }
        }

        binding.cardAlmuerzo.setOnClickListener {
            if (servicioComidaSeleccionado == 1) {
                val intent = Intent(this, MenuDelDiaMesActivity::class.java).apply {
                    putExtra("fecha_milisegundos", diaSeleccionado.timeInMillis)
                    putExtra("servicio", "Almuerzo")
                }
                startActivity(intent)
            } else {
                aplicarEfectoSeleccionComida(1, animar = true, scroll = true)
            }
        }

        binding.cardMerienda.setOnClickListener {
            if (servicioComidaSeleccionado == 2) {
                val intent = Intent(this, MenuDelDiaMesActivity::class.java).apply {
                    putExtra("fecha_milisegundos", diaSeleccionado.timeInMillis)
                    putExtra("servicio", "Merienda")
                }
                startActivity(intent)
            } else {
                aplicarEfectoSeleccionComida(2, animar = true, scroll = true)
            }
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
        val maxScroll = (contenedor.width - scroll.width).coerceAtLeast(1)
        val density = resources.displayMetrics.density

        val progreso = (scroll.scrollX.toFloat() / maxScroll).coerceIn(0f, 1f)
        val idxFloat = progreso * 2f // 0 = Desayuno, 1 = Almuerzo, 2 = Merienda

        tarjetasComida.forEachIndexed { i, card ->
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
            val popupBinding = com.example.movilmanupuladora.databinding.PopupNotificacionesBinding.inflate(layoutInflater)
            popupBinding.txtCantidadNotificaciones.text = "3"

            val dialog = com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                .setView(popupBinding.root)
                .create()

            dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

            popupBinding.notificacionPreparacion.setOnClickListener {
                dialog.dismiss()
                startActivity(Intent(this, HistorialPreparacionesActivity::class.java))
            }

            popupBinding.notificacionInventario.setOnClickListener {
                dialog.dismiss()
                startActivity(Intent(this, InventarioJefaActivity::class.java))
            }

            popupBinding.notificacionAsistencia.setOnClickListener {
                dialog.dismiss()
                startActivity(Intent(this, AsistenciaJefaActivity::class.java))
            }

            popupBinding.btnVerTodasNotificaciones.setOnClickListener {
                dialog.dismiss()
                startActivity(Intent(this, com.example.movilmanupuladora.ui.manipuladora.AvisosActivity::class.java))
            }

            dialog.show()
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
        binding.txtMenusActivos.text = "3"

        val cantManipuladoras = ManipuladorasManager.obtenerManipuladoras().size
        binding.txtCantManipuladoras.text = if (cantManipuladoras > 0) cantManipuladoras.toString() else "8"
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
                        val regLegacy = asistenciasHoy.maxByOrNull { it.obtenerIdReal() ?: 0 }!!
                        val tot = regLegacy.ninosPresentes
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
