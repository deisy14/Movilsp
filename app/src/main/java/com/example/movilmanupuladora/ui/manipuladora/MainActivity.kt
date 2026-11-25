package com.example.movilmanupuladora.ui.manipuladora

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.movilmanupuladora.R
import com.example.movilmanupuladora.data.api.RetrofitClient
import com.example.movilmanupuladora.data.model.DetallePlato
import com.example.movilmanupuladora.data.model.MenuPaeCatalog
import com.example.movilmanupuladora.data.model.PlatoResponse
import com.example.movilmanupuladora.data.model.SeccionMenu
import com.example.movilmanupuladora.data.repository.AsistenciaRepository
import com.example.movilmanupuladora.data.repository.MenuRepository
import com.example.movilmanupuladora.databinding.ActivityMainBinding
import com.example.movilmanupuladora.databinding.DialogDetallePlatoBinding
import com.example.movilmanupuladora.databinding.PopupNotificacionesBinding
import com.example.movilmanupuladora.ui.manipuladora_Jefe.AsistenciaManager
import com.example.movilmanupuladora.ui.manipuladora_Jefe.InicioJefaActivity
import com.example.movilmanupuladora.utils.NavigationHelper
import com.example.movilmanupuladora.utils.SessionManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val menuRepository = MenuRepository(RetrofitClient.apiService)
    private val asistenciaRepository = AsistenciaRepository(RetrofitClient.apiService)
    private var jobPollingAsistencia: Job? = null
    private var idPrimariaCache: Int = 16
    private var idSecundariaCache: Int = 19
    private var gradosCargados: Boolean = false

    private var listaPlatos: List<PlatoResponse> = emptyList()
    private var listaSecciones: List<SeccionMenu> = emptyList()
    private var listaDetalles: List<DetallePlato> = emptyList()

    private var platoActualSeleccionado: PlatoResponse? = null

    private val seccionesNombres = listOf("Desayuno", "Almuerzo", "Merienda")
    private var seccionSeleccionada = 0

    private var jornadaActualInicio = "MANANA"

    private val platosBaseRespaldo = listOf(
        PlatoResponse(
            idPlato = 1,
            idSeccion = 1,
            nombrePlato = "Huevos pericos con arroz blanco",
            componente = "Refresco de avena, huevos pericos, arroz blanco"
        ),
        PlatoResponse(
            idPlato = 11,
            idSeccion = 2,
            nombrePlato = "Pollo guisado con arroz blanco",
            componente = "Refresco de avena, pollo guisado, arroz blanco"
        ),
        PlatoResponse(
            idPlato = 9,
            idSeccion = 3,
            nombrePlato = "Leche con galleta cuca y mango",
            componente = "Leche, galleta cuca, maní salado, mango"
        )
    )

    private val seccionesRespaldo = listOf(
        SeccionMenu(idSeccion = 1, idJornada = 1, nombreSeccion = "Desayuno"),
        SeccionMenu(idSeccion = 2, idJornada = 1, nombreSeccion = "Almuerzo"),
        SeccionMenu(idSeccion = 3, idJornada = 1, nombreSeccion = "Merienda")
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
            insets
        }

        SessionManager.getToken(this)

        val sessionMgr = SessionManager(this)
        val nombreUser = sessionMgr.getUserName() ?: "Maria Alejandra Velazco"
        binding.tvSaludo.text = "Jornada de Servicio PAE"
        val sedeNombre = sessionMgr.getUbicacionCompleta()
        findViewById<android.widget.TextView>(R.id.tvInstitucion)?.text = sedeNombre

        val iniciales = nombreUser.split(" ")
            .filter { it.isNotEmpty() }
            .take(2)
            .joinToString("") { it.first().uppercase() }
            .ifEmpty { "MA" }
        findViewById<android.widget.TextView>(R.id.tvAvatarIniciales)?.text = iniciales

        listaPlatos = platosBaseRespaldo
        listaSecciones = seccionesRespaldo

        configurarBotonesJornadaInicio()
        configurarGiroRuleta()
        cargarDatosDesdeBackend()
        actualizarSumatoriaAsistencia()

        lifecycleScope.launch {
            try {
                com.example.movilmanupuladora.utils.PreparacionSyncManager.sincronizarDesdeBackend(this@MainActivity)
                actualizarInformacionSeccion()
                cargarAsistenciaLocal()
            } catch (_: Exception) { }
        }

        findViewById<View>(R.id.btnNotificaciones)?.setOnClickListener {
            mostrarPopupNotificaciones()
        }

        // Detectar plato y componente asignado por la Jefa
        binding.btnVerPreparacion.setOnClickListener {
            abrirPreparacionODetalles()
        }

        binding.cardPlatoMain.setOnClickListener {
            abrirPreparacionODetalles()
        }

        binding.cardAccesoAsignadas.setOnClickListener {
            abrirPreparacionODetalles()
        }

        configurarMenusAnteriores()

        // Configurar tarjeta de asistencia de estudiantes
        cargarAsistenciaLocal()
        configurarCardAsistencia()

        // Configurar barra de navegación inferior
        NavigationHelper.setupBarraNavegacion(
            this,
            binding.barraNavegacion,
            NavigationHelper.Tab.INICIO
        )
    }

    override fun onResume() {
        super.onResume()
        cargarAsistenciaLocal()
        iniciarPollingAsistenciaEnVivo()
        lifecycleScope.launch {
            try {
                com.example.movilmanupuladora.utils.PreparacionSyncManager.sincronizarDesdeBackend(this@MainActivity)
                actualizarInformacionSeccion()
                cargarAsistenciaLocal()
            } catch (_: Exception) { }
        }
    }

    override fun onPause() {
        super.onPause()
        jobPollingAsistencia?.cancel()
    }

    private fun iniciarPollingAsistenciaEnVivo() {
        jobPollingAsistencia?.cancel()
        jobPollingAsistencia = lifecycleScope.launch {
            while (isActive) {
                try {
                    // 1. Sincronizar en tiempo real el plato, componente asignado a la manipuladora y asistencia
                    com.example.movilmanupuladora.utils.PreparacionSyncManager.sincronizarDesdeBackend(this@MainActivity)

                    // 2. Sincronizar también con la tabla de asistencia diaria del backend
                    cargarAsistenciaBackend()

                    // 3. Actualizar interfaz de forma reactiva en el hilo principal
                    withContext(Dispatchers.Main) {
                        actualizarInformacionSeccion()
                        cargarAsistenciaLocal()
                    }
                } catch (e: Exception) {
                    Log.e("POLLING_MA", "Error en polling en vivo: ${e.message}")
                }
                delay(3000)
            }
        }
    }

    private fun actualizarSumatoriaAsistencia() {
        cargarAsistenciaLocal()
    }

    // =========================================================
    // BOTONES DE JORNADA (MAÑANA Y TARDE)
    // =========================================================

    private fun configurarBotonesJornadaInicio() {
        binding.btnJornadaManana.setOnClickListener {
            jornadaActualInicio = "MANANA"
            actualizarEstadoJornadaInicio()
            actualizarSumatoriaAsistencia()
        }

        binding.btnJornadaTarde.setOnClickListener {
            jornadaActualInicio = "TARDE"
            actualizarEstadoJornadaInicio()
            actualizarSumatoriaAsistencia()
        }

        actualizarEstadoJornadaInicio()
    }

    private fun actualizarEstadoJornadaInicio() {
        if (jornadaActualInicio == "MANANA") {
            binding.btnJornadaManana.setBackgroundResource(R.drawable.bg_pill_yellow)
            binding.btnJornadaManana.setTextColor(Color.parseColor("#7A5500"))

            binding.btnJornadaTarde.setBackgroundResource(R.drawable.bg_tag_plato)
            binding.btnJornadaTarde.setTextColor(Color.parseColor("#64748B"))

            // Mantener todas las secciones activas para que la manipuladora pueda revisar los 3 servicios
            binding.btnSeccionDesayuno.alpha = 1.0f
            binding.btnSeccionDesayuno.isEnabled = true

            binding.btnSeccionAlmuerzo.alpha = 1.0f
            binding.btnSeccionAlmuerzo.isEnabled = true

            binding.btnSeccionMerienda.alpha = 1.0f
            binding.btnSeccionMerienda.isEnabled = true

            seleccionarSeccion(0) // Selecciona Desayuno por defecto
        } else {
            binding.btnJornadaTarde.setBackgroundResource(R.drawable.bg_pill_yellow)
            binding.btnJornadaTarde.setTextColor(Color.parseColor("#7A5500"))

            binding.btnJornadaManana.setBackgroundResource(R.drawable.bg_tag_plato)
            binding.btnJornadaManana.setTextColor(Color.parseColor("#64748B"))

            // Mantener todas las secciones activas también en la tarde
            binding.btnSeccionDesayuno.alpha = 1.0f
            binding.btnSeccionDesayuno.isEnabled = true

            binding.btnSeccionAlmuerzo.alpha = 1.0f
            binding.btnSeccionAlmuerzo.isEnabled = true

            binding.btnSeccionMerienda.alpha = 1.0f
            binding.btnSeccionMerienda.isEnabled = true

            seleccionarSeccion(2) // Selecciona Merienda por defecto
        }
    }

    private fun mostrarPopupNotificaciones() {
        val popupBinding = PopupNotificacionesBinding.inflate(layoutInflater)
        val anchorView = findViewById<View>(R.id.btnNotificaciones) ?: binding.root

        // Ocultar el indicador de número al abrir las notificaciones
        findViewById<View>(R.id.badgeNotificacionesSticky)?.visibility = View.GONE

        val popupWindow = android.widget.PopupWindow(
            popupBinding.root,
            (320 * resources.displayMetrics.density).toInt(),
            android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
            true
        ).apply {
            elevation = 16f
            setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))
        }

        popupBinding.btnVerTodasNotificaciones.setOnClickListener {
            popupWindow.dismiss()
            val intent = Intent(this, AvisosActivity::class.java)
            startActivity(intent)
        }

        popupWindow.showAsDropDown(anchorView, (-240 * resources.displayMetrics.density).toInt(), (8 * resources.displayMetrics.density).toInt())

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.obtenerNotificaciones()
                if (response.isSuccessful && !response.body().isNullOrEmpty()) {
                    val notifs = response.body()!!
                    val noLeidas = notifs.count { it.leido == false }
                    popupBinding.txtCantidadNotificaciones.text = noLeidas.toString()

                    popupBinding.contenedorNotificaciones.removeAllViews()
                    for (notif in notifs.take(4)) {
                        val row = LinearLayout(this@MainActivity).apply {
                            orientation = LinearLayout.HORIZONTAL
                            gravity = android.view.Gravity.CENTER_VERTICAL
                            setPadding(
                                (12 * resources.displayMetrics.density).toInt(),
                                (10 * resources.displayMetrics.density).toInt(),
                                (12 * resources.displayMetrics.density).toInt(),
                                (10 * resources.displayMetrics.density).toInt()
                            )
                        }

                        val tvTitulo = TextView(this@MainActivity).apply {
                            text = notif.titulo ?: "Aviso PAE"
                            setTextColor(Color.parseColor("#1E293B"))
                            textSize = 13f
                            setTypeface(typeface, android.graphics.Typeface.BOLD)
                        }

                        val tvMensaje = TextView(this@MainActivity).apply {
                            text = notif.mensaje ?: ""
                            setTextColor(Color.parseColor("#64748B"))
                            textSize = 11.5f
                        }

                        val textLayout = LinearLayout(this@MainActivity).apply {
                            orientation = LinearLayout.VERTICAL
                            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).also {
                                it.marginStart = (10 * resources.displayMetrics.density).toInt()
                            }
                            addView(tvTitulo)
                            addView(tvMensaje)
                        }

                        row.addView(textLayout)
                        row.setOnClickListener {
                            popupWindow.dismiss()
                            val intent = Intent(this@MainActivity, AvisosActivity::class.java)
                            startActivity(intent)
                        }
                        popupBinding.contenedorNotificaciones.addView(row)
                    }
                } else {
                    popupBinding.txtCantidadNotificaciones.text = "0"
                }
            } catch (_: Exception) {}
        }
    }

    private fun configurarGiroRuleta() {
        binding.layoutMenuHoyHeader.setOnClickListener {
            val intent = Intent(this, MenuDiaActivity::class.java)
            startActivity(intent)
        }

        binding.btnSeccionDesayuno.setOnClickListener {
            if (jornadaActualInicio == "MANANA") {
                seleccionarSeccion(0)
            }
        }

        binding.btnSeccionAlmuerzo.setOnClickListener {
            if (jornadaActualInicio == "MANANA") {
                seleccionarSeccion(1)
            }
        }

        binding.btnSeccionMerienda.setOnClickListener {
            if (jornadaActualInicio == "TARDE") {
                seleccionarSeccion(2)
            }
        }
    }

    private fun seleccionarSeccion(index: Int) {
        seccionSeleccionada = index

        val colorActivo = Color.parseColor("#FEF9E7")
        val colorInactivo = Color.parseColor("#FFFFFF")
        val strokeActivo = Color.parseColor("#F4B41F")
        val strokeInactivo = Color.parseColor("#E2E8F0")

        val textActivo = Color.parseColor("#1B3317")
        val textInactivo = Color.parseColor("#1E293B")

        binding.btnSeccionDesayuno.setCardBackgroundColor(if (index == 0) colorActivo else colorInactivo)
        binding.btnSeccionDesayuno.strokeColor = if (index == 0) strokeActivo else strokeInactivo
        binding.btnSeccionDesayuno.strokeWidth = if (index == 0) (2 * resources.displayMetrics.density).toInt() else (1 * resources.displayMetrics.density).toInt()
        binding.tvLabelDesayuno.setTextColor(if (index == 0) textActivo else textInactivo)

        binding.btnSeccionAlmuerzo.setCardBackgroundColor(if (index == 1) colorActivo else colorInactivo)
        binding.btnSeccionAlmuerzo.strokeColor = if (index == 1) strokeActivo else strokeInactivo
        binding.btnSeccionAlmuerzo.strokeWidth = if (index == 1) (2 * resources.displayMetrics.density).toInt() else (1 * resources.displayMetrics.density).toInt()
        binding.tvLabelAlmuerzo.setTextColor(if (index == 1) textActivo else textInactivo)

        binding.btnSeccionMerienda.setCardBackgroundColor(if (index == 2) colorActivo else colorInactivo)
        binding.btnSeccionMerienda.strokeColor = if (index == 2) strokeActivo else strokeInactivo
        binding.btnSeccionMerienda.strokeWidth = if (index == 2) (2 * resources.displayMetrics.density).toInt() else (1 * resources.displayMetrics.density).toInt()
        binding.tvLabelMerienda.setTextColor(if (index == 2) textActivo else textInactivo)

        actualizarInformacionSeccion()
    }

    private fun abrirPreparacionODetalles() {
        val sessionMgr = SessionManager(this)
        val fechaHoy = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val nombreSeccion = seccionesNombres[seccionSeleccionada]

        val platoDia = sessionMgr.getPlatoDiaServicio(this, fechaHoy, nombreSeccion)
            ?: sessionMgr.getPlatoServicio(this, nombreSeccion)
            ?: sessionMgr.getPlatoJefa(this).takeIf { it.isNotBlank() }

        val comp = sessionMgr.getComponenteAsignado(this, nombreSeccion)

        val intent = Intent(this, PreparacionActivity::class.java).apply {
            putExtra("nombre_plato", platoDia)
            putExtra("componente_seleccionado", comp)
            if (!platoDia.isNullOrBlank()) {
                putExtra("id_imagen", InicioJefaActivity.obtenerImagenPlatoPorNombre(platoDia))
            }
        }
        startActivity(intent)
    }

    private fun actualizarInformacionSeccion() {
        val sessionMgr = SessionManager(this)
        val fechaHoy = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        // 1. Obtener los platos asignados por la Jefa para los 3 servicios del día
        val platoDes = sessionMgr.getPlatoDiaServicio(this, fechaHoy, "Desayuno")
            ?: sessionMgr.getPlatoServicio(this, "Desayuno")

        val platoAlm = sessionMgr.getPlatoDiaServicio(this, fechaHoy, "Almuerzo")
            ?: sessionMgr.getPlatoServicio(this, "Almuerzo")

        val platoMer = sessionMgr.getPlatoDiaServicio(this, fechaHoy, "Merienda")
            ?: sessionMgr.getPlatoServicio(this, "Merienda")

        // 2. Actualizar información de la sección actualmente seleccionada
        val nombreSeccion = seccionesNombres[seccionSeleccionada]
        binding.tvTagSeccionSeleccionada.text = "TU COMPONENTE · ${nombreSeccion.uppercase()}"

        val platoJefaGeneral = sessionMgr.getPlatoJefa(this)
        val componenteJefa = sessionMgr.getComponenteAsignado(this, nombreSeccion)
            ?: sessionMgr.getComponenteAsignado(this)

        val platoActivo = when (seccionSeleccionada) {
            0 -> platoDes
            1 -> platoAlm
            else -> platoMer
        }?.takeIf { it.isNotBlank() } ?: platoJefaGeneral.takeIf { it.isNotBlank() }

        if (!platoActivo.isNullOrBlank()) {
            binding.imgPlatoMain.setImageResource(
                InicioJefaActivity.obtenerImagenPlatoPorNombre(platoActivo)
            )
        }

        // 3. Mostrar componente asignado a esta manipuladora o estado correspondiente
        if (!componenteJefa.isNullOrBlank()) {
            val platoFinalDisplay = platoActivo.takeIf { !it.isNullOrBlank() } ?: "Menú PAE del día"
            binding.tvComponenteAsignado.text = componenteJefa
            binding.tvPlatoSeleccionado.text = "Plato: $platoFinalDisplay"
            binding.tvSubtituloCardAsignadas.text = "Tu tarea: $componenteJefa · $platoFinalDisplay"
        } else if (!platoActivo.isNullOrBlank()) {
            binding.tvComponenteAsignado.text = "Sin componente asignado"
            binding.tvPlatoSeleccionado.text = "Plato de hoy: $platoActivo"
            binding.tvSubtituloCardAsignadas.text = "Toca para ver detalles y preparación"
        } else {
            binding.tvComponenteAsignado.text = "La jefa aún no ha seleccionado el menú de hoy"
            binding.tvPlatoSeleccionado.text = "Servicio: $nombreSeccion · Menú pendiente"
            binding.tvSubtituloCardAsignadas.text = "Toca para ver el menú del día y asistencia"
        }
    }

    private fun cargarDatosDesdeBackend() {
        lifecycleScope.launch {
            try {
                val resPlatos = menuRepository.obtenerPlatos()
                if (resPlatos.isSuccessful && !resPlatos.body().isNullOrEmpty()) {
                    listaPlatos = resPlatos.body()!!
                    com.example.movilmanupuladora.data.model.MenuPaeCatalog.registrarPlatosDesdeBackend(listaPlatos)
                }

                val resSecciones = menuRepository.obtenerSeccionesMenu()
                if (resSecciones.isSuccessful && !resSecciones.body().isNullOrEmpty()) {
                    listaSecciones = resSecciones.body()!!
                }

                val resDetalles = menuRepository.obtenerDetallePlatos()
                if (resDetalles.isSuccessful && !resDetalles.body().isNullOrEmpty()) {
                    listaDetalles = resDetalles.body()!!
                }

                actualizarInformacionSeccion()
                configurarMenusAnteriores()
                cargarAsistenciaBackend()

            } catch (e: Exception) {
                Log.e("BACKEND_SIRAE", "Error al conectar con el servidor: ${e.message}")
            }
        }
    }

    private suspend fun cargarAsistenciaBackend() {
        withContext(Dispatchers.IO) {
            try {
                SessionManager.getToken(this@MainActivity)

                if (!gradosCargados) {
                    val gradosRes = asistenciaRepository.obtenerGrados()
                    if (gradosRes.isSuccessful && !gradosRes.body().isNullOrEmpty()) {
                        val listaGrados = gradosRes.body()!!
                        idPrimariaCache = listaGrados.find { it.obtenerNombreReal().contains("primaria", ignoreCase = true) }?.obtenerIdReal() ?: 16
                        idSecundariaCache = listaGrados.find { it.obtenerNombreReal().contains("secundaria", ignoreCase = true) }?.obtenerIdReal() ?: 19
                        gradosCargados = true
                    }
                }

                val asistenciasRes = asistenciaRepository.obtenerAsistencias()
                if (asistenciasRes.isSuccessful && !asistenciasRes.body().isNullOrEmpty()) {
                    val todas = asistenciasRes.body()!!
                    val fechaHoy = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

                    // 1. Filtrar registros con la fecha de hoy (soporta variaciones de formato y ceros)
                    val registrosFecha = todas.filter {
                        val f = it.fecha.trim()
                        f.startsWith(fechaHoy) || f.replace("-0", "-").startsWith(fechaHoy.replace("-0", "-"))
                    }

                    // 2. Tomar el registro de Primaria y Secundaria del día de hoy
                    val regPrimaria = registrosFecha
                        .filter { it.obtenerIdGradoReal() == idPrimariaCache || it.obtenerIdGradoReal() == 16 }
                        .maxByOrNull { it.obtenerIdReal() ?: 0 }

                    val regSecundaria = registrosFecha
                        .filter { it.obtenerIdGradoReal() == idSecundariaCache || it.obtenerIdGradoReal() == 19 }
                        .maxByOrNull { it.obtenerIdReal() ?: 0 }

                    var totalPrimaria = regPrimaria?.ninosPresentes ?: 0
                    var totalSecundaria = regSecundaria?.ninosPresentes ?: 0

                    if (regPrimaria == null && regSecundaria == null && registrosFecha.isNotEmpty()) {
                        val sumTot = registrosFecha.sumOf { it.ninosPresentes }
                        totalPrimaria = (sumTot * 0.6).toInt()
                        totalSecundaria = sumTot - totalPrimaria
                    }

                    var totalEstudiantes = totalPrimaria + totalSecundaria
                    if (totalEstudiantes == 0 && registrosFecha.isNotEmpty()) {
                        totalEstudiantes = registrosFecha.sumOf { it.ninosPresentes }
                        totalPrimaria = (totalEstudiantes * 0.6).toInt()
                        totalSecundaria = totalEstudiantes - totalPrimaria
                    }

                    if (totalEstudiantes == 0) {
                        val pLocal = AsistenciaManager.obtenerPrimaria(this@MainActivity)
                        val sLocal = AsistenciaManager.obtenerSecundaria(this@MainActivity)
                        val totLocal = (pLocal + sLocal).takeIf { it > 0 }
                            ?: AsistenciaManager.obtenerManana(this@MainActivity).takeIf { it > 0 }
                            ?: AsistenciaManager.obtenerTarde(this@MainActivity)
                        if (totLocal > 0) {
                            totalEstudiantes = totLocal
                            totalPrimaria = pLocal
                            totalSecundaria = sLocal
                        }
                    }

                    // Solo actualizar la caché si el servidor devolvió datos reales de hoy (> 0)
                    if (totalEstudiantes > 0) {
                        AsistenciaManager.guardarPrimaria(this@MainActivity, totalPrimaria)
                        AsistenciaManager.guardarSecundaria(this@MainActivity, totalSecundaria)
                        AsistenciaManager.guardarManana(this@MainActivity, totalEstudiantes)

                        withContext(Dispatchers.Main) {
                            actualizarVistaAsistencia(
                                primaria = totalPrimaria,
                                secundaria = totalSecundaria,
                                total = totalEstudiantes,
                                hayRegistro = true
                            )
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            cargarAsistenciaLocal()
                        }
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        cargarAsistenciaLocal()
                    }
                }
            } catch (e: Exception) {
                Log.e("ASISTENCIA_SYNC", "Error al sincronizar asistencia con el servidor: ${e.message}")
                withContext(Dispatchers.Main) {
                    cargarAsistenciaLocal()
                }
            }
        }
    }

    private fun cargarAsistenciaLocal() {
        val p = AsistenciaManager.obtenerPrimaria(this)
        val s = AsistenciaManager.obtenerSecundaria(this)
        var total = p + s
        if (total == 0) {
            total = AsistenciaManager.obtenerManana(this)
        }
        if (total == 0) {
            total = AsistenciaManager.obtenerTarde(this)
        }
        actualizarVistaAsistencia(p, s, total, hayRegistro = (total > 0))
    }

    private fun actualizarVistaAsistencia(primaria: Int, secundaria: Int, total: Int, hayRegistro: Boolean = true) {
        val p = if (primaria > 0) primaria else AsistenciaManager.obtenerPrimaria(this)
        val s = if (secundaria > 0) secundaria else AsistenciaManager.obtenerSecundaria(this)
        val tot = if (total > 0) total else (p + s).takeIf { it > 0 } ?: AsistenciaManager.obtenerManana(this)

        if (tot > 0) {
            binding.tvTotalAsistenciaSumatoria.text = "$tot Estudiantes registrados"
            binding.tvDesgloseAsistencia.text = "Primaria: $p  |  Secundaria: $s"
        } else {
            binding.tvTotalAsistenciaSumatoria.text = "Sin datos de asistencia"
            binding.tvDesgloseAsistencia.text = "Pendiente de registro por la jefa"
        }
    }

    private fun configurarCardAsistencia() {
        binding.cardAsistenciaSumatoria.setOnClickListener {
            lifecycleScope.launch {
                cargarAsistenciaBackend()
                mostrarDialogoAsistenciaPorGrados()
            }
        }
    }

    private fun mostrarDialogoAsistenciaPorGrados() {
        val vistaDialogo = android.view.LayoutInflater.from(this).inflate(
            R.layout.dialog_detalle_asistencia,
            null
        )

        val txtTotalCalc = vistaDialogo.findViewById<android.widget.TextView>(R.id.txtTotalEstudiantesCalculado)
        val txtGramajeCalc = vistaDialogo.findViewById<android.widget.TextView>(R.id.txtGramajeTotalEstimado)
        val btnEntendido = vistaDialogo.findViewById<android.widget.Button>(R.id.btnEntendidoAsistencia)

        val p = AsistenciaManager.obtenerPrimaria(this)
        val s = AsistenciaManager.obtenerSecundaria(this)
        val tot = (p + s).takeIf { it > 0 } ?: AsistenciaManager.obtenerManana(this)

        val finalP = if (p > 0) p else 99
        val finalS = if (s > 0) s else 66
        val finalTot = if (tot > 0) tot else (finalP + finalS)

        txtTotalCalc?.text = "Total Estudiantes: $finalTot (Primaria: $finalP | Secundaria: $finalS)"
        txtGramajeCalc?.text = String.format(Locale.US, "Gramaje total estimado: %.1f kg a cocinar", (finalP * 0.25) + (finalS * 0.38))

        val tvValJardin = vistaDialogo.findViewById<android.widget.TextView>(R.id.tvValJardin)
        val tvValPrimaria = vistaDialogo.findViewById<android.widget.TextView>(R.id.tvValPrimaria)
        val tvValSecundaria = vistaDialogo.findViewById<android.widget.TextView>(R.id.tvValSecundaria)

        tvValJardin?.text = "${finalP / 6}"
        tvValPrimaria?.text = "$finalP"
        tvValSecundaria?.text = "$finalS"

        // Configurar Chips de Filtro (Todos / Primaria / Secundaria)
        val chipTodos = vistaDialogo.findViewById<android.widget.TextView>(R.id.chipTodosAsistencia)
        val chipPrimaria = vistaDialogo.findViewById<android.widget.TextView>(R.id.chipPrimariaAsistencia)
        val chipSecundaria = vistaDialogo.findViewById<android.widget.TextView>(R.id.chipSecundariaAsistencia)
        val layoutPrimaria = vistaDialogo.findViewById<android.view.View>(R.id.layoutPrimaria)
        val layoutSecundaria = vistaDialogo.findViewById<android.view.View>(R.id.layoutSecundaria)

        chipPrimaria?.text = "Primaria ($finalP)"
        chipSecundaria?.text = "Secundaria ($finalS)"

        fun actualizarFiltroAsistencia(seleccionado: android.widget.TextView) {
            val chips = listOf(chipTodos, chipPrimaria, chipSecundaria)
            for (c in chips) {
                if (c == seleccionado) {
                    c?.setBackgroundResource(R.drawable.bg_pill_green)
                    c?.setTextColor(Color.WHITE)
                } else {
                    c?.background = null
                    c?.setTextColor(Color.parseColor("#64748B"))
                }
            }

            when (seleccionado) {
                chipTodos -> {
                    layoutPrimaria?.visibility = View.VISIBLE
                    layoutSecundaria?.visibility = View.VISIBLE
                }
                chipPrimaria -> {
                    layoutPrimaria?.visibility = View.VISIBLE
                    layoutSecundaria?.visibility = View.GONE
                }
                chipSecundaria -> {
                    layoutPrimaria?.visibility = View.GONE
                    layoutSecundaria?.visibility = View.VISIBLE
                }
            }
        }

        chipTodos?.setOnClickListener { actualizarFiltroAsistencia(chipTodos) }
        chipPrimaria?.setOnClickListener { actualizarFiltroAsistencia(chipPrimaria) }
        chipSecundaria?.setOnClickListener { actualizarFiltroAsistencia(chipSecundaria) }

        val dialog = MaterialAlertDialogBuilder(this)
            .setView(vistaDialogo)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        btnEntendido?.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun obtenerPlatoPorSeccion(nombreSeccion: String): PlatoResponse? {
        val seccion = listaSecciones.firstOrNull {
            it.nombreSeccion.equals(nombreSeccion, ignoreCase = true)
        }

        if (seccion != null) {
            val platoPorId = listaPlatos.firstOrNull { it.idSeccion == seccion.idSeccion }
            if (platoPorId != null) return platoPorId
        }

        return when {
            nombreSeccion.equals("Desayuno", ignoreCase = true) -> {
                listaPlatos.firstOrNull {
                    it.nombrePlato?.contains("café", true) == true ||
                            it.nombrePlato?.contains("huevo", true) == true ||
                            it.idSeccion == 1
                } ?: platosBaseRespaldo[0]
            }
            nombreSeccion.equals("Almuerzo", ignoreCase = true) -> {
                listaPlatos.firstOrNull {
                    it.nombrePlato?.contains("pollo", true) == true ||
                            it.nombrePlato?.contains("bandeja", true) == true ||
                            it.idSeccion == 2
                } ?: platosBaseRespaldo[1]
            }
            nombreSeccion.equals("Merienda", ignoreCase = true) -> {
                listaPlatos.firstOrNull {
                    it.nombrePlato?.contains("muffin", true) == true ||
                            it.nombrePlato?.contains("leche", true) == true ||
                            it.idSeccion == 3
                } ?: platosBaseRespaldo[2]
            }
            else -> null
        }
    }

    private fun obtenerImagenPlato(plato: PlatoResponse): Int {
        if (plato.idPlato in 1..20) {
            return MenuPaeCatalog.obtenerMenuPorId(plato.idPlato).imagenRes
        }
        return MenuPaeCatalog.obtenerImagenPorNombre(plato.nombrePlato ?: plato.componente)
    }

    private fun configurarMenusAnteriores() {
        val plato1 = listaPlatos.firstOrNull {
            it.nombrePlato?.contains("pollo", true) == true
        } ?: platosBaseRespaldo[1]

        val plato2 = listaPlatos.firstOrNull {
            it.nombrePlato?.contains("perico", true) == true || it.nombrePlato?.contains("huevo", true) == true
        } ?: platosBaseRespaldo[0]

        val plato3 = listaPlatos.firstOrNull {
            it.nombrePlato?.contains("cuca", true) == true || it.nombrePlato?.contains("leche", true) == true
        } ?: platosBaseRespaldo[2]

        val fmtFechaHistorial = java.text.SimpleDateFormat("dd MMM", java.util.Locale.forLanguageTag("es-CO"))
        val cal = java.util.Calendar.getInstance()

        cal.add(java.util.Calendar.DAY_OF_YEAR, -1)
        val fecha1 = fmtFechaHistorial.format(cal.time)

        cal.add(java.util.Calendar.DAY_OF_YEAR, -1)
        val fecha2 = fmtFechaHistorial.format(cal.time)

        cal.add(java.util.Calendar.DAY_OF_YEAR, -1)
        val fecha3 = fmtFechaHistorial.format(cal.time)

        binding.tvPlatoAnterior1.text = "Almuerzo"
        binding.imgMenuAnterior1.setImageResource(obtenerImagenPlato(plato1))
        binding.tvFechaAnterior1.text = fecha1
        binding.cardMenuAnterior1.setOnClickListener { mostrarDialogoPlato(plato1) }

        binding.tvPlatoAnterior2.text = "Desayuno"
        binding.imgMenuAnterior2.setImageResource(obtenerImagenPlato(plato2))
        binding.tvFechaAnterior2.text = fecha2
        binding.cardMenuAnterior2.setOnClickListener { mostrarDialogoPlato(plato2) }

        binding.tvPlatoAnterior3.text = "Merienda"
        binding.imgMenuAnterior3.setImageResource(obtenerImagenPlato(plato3))
        binding.tvFechaAnterior3.text = fecha3
        binding.cardMenuAnterior3.setOnClickListener { mostrarDialogoPlato(plato3) }
    }

    private fun formatearNombrePlato(nombre: String?): String {
        return nombre
            ?.split(" ")
            ?.joinToString(" ") { palabra ->
                palabra.replaceFirstChar {
                    if (it.isLowerCase()) it.titlecase() else it.toString()
                }
            }
            ?: "Arroz con pollo"
    }

    private fun mostrarDialogoPlato(plato: PlatoResponse) {
        val seccion = listaSecciones.firstOrNull { it.idSeccion == plato.idSeccion }
        val nombreSeccion = seccion?.nombreSeccion ?: when (plato.idSeccion) {
            1 -> "Desayuno"
            2 -> "Almuerzo"
            3 -> "Merienda"
            else -> "Almuerzo"
        }

        val detalle = listaDetalles.firstOrNull { it.idPlato == plato.idPlato }
        val porcion = detalle?.porcionPorNino ?: "100g aprox."
        val totalPreparar = detalle?.totalAPreparar ?: "50"
        val unidad = detalle?.unidadTotal ?: "porciones"
        val estado = detalle?.estadoPreparacion ?: "Asignado"
        val componenteTipo = if (!plato.componente.isNullOrEmpty()) plato.componente else "Sopas, Sazón y Proteína"

        val dialogBinding = DialogDetallePlatoBinding.inflate(layoutInflater)

        dialogBinding.tvTituloPlatoModal.text = formatearNombrePlato(plato.nombrePlato)
        dialogBinding.tvSeccionModal.text = nombreSeccion
        dialogBinding.tvComponenteModal.text = "Componente: $componenteTipo"
        dialogBinding.tvPorcionModal.text = porcion
        dialogBinding.tvTotalPrepararModal.text = "$totalPreparar $unidad"
        dialogBinding.tvBadgeEstadoModal.text = estado
        dialogBinding.imgPlatoModal.setImageResource(obtenerImagenPlato(plato))

        val dialog = MaterialAlertDialogBuilder(this)
            .setView(dialogBinding.root)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        dialogBinding.btnCerrarModal.setOnClickListener {
            dialog.dismiss()
        }

        dialogBinding.btnVerComponentesModal.setOnClickListener {
            dialog.dismiss()
            val intent = Intent(this, IngredientesActivity::class.java).apply {
                putExtra("id_plato", plato.idPlato)
                putExtra("id_seccion", plato.idSeccion ?: -1)
                putExtra("nombre_plato", plato.nombrePlato)
                putExtra("componente_seleccionado", componenteTipo)
            }
            startActivity(intent)
        }

        dialogBinding.btnOtroAleatorioModal.setOnClickListener {
            dialog.dismiss()
            val intent = Intent(this, PreparacionActivity::class.java).apply {
                putExtra("id_plato", plato.idPlato)
                putExtra("nombre_plato", plato.nombrePlato)
                putExtra("componente_seleccionado", componenteTipo)
            }
            startActivity(intent)
        }

        dialog.show()
    }
}