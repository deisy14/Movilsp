package com.example.movilmanupuladora.ui.manipuladora

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
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
        val nombreUser = sessionMgr.getUserName() ?: "Manipuladora"
        binding.includeStickyBar.tvNombreUsuarioSticky.text = nombreUser
        binding.tvSaludo.text = "Jornada de Servicio PAE"
        val sedeNombre = sessionMgr.getUbicacionCompleta()
        binding.includeStickyBar.tvInstitucion.text = if (sedeNombre.isNotBlank()) sedeNombre else "Sede Principal"

        val fechaFormatted = SimpleDateFormat(
            "EEEE / d / MMM / yyyy",
            Locale.forLanguageTag("es-CO")
        ).format(Date()).uppercase()
        binding.tvFecha.text = "BIENVENIDA · $fechaFormatted"

        listaPlatos = platosBaseRespaldo
        listaSecciones = seccionesRespaldo

        configurarBotonesJornadaInicio()
        configurarGiroRuleta()
        actualizarBadgeNotificaciones()
        cargarDatosDesdeBackend()
        actualizarSumatoriaAsistencia()

        lifecycleScope.launch {
            try {
                com.example.movilmanupuladora.utils.PreparacionSyncManager.sincronizarDesdeBackend(this@MainActivity)
                actualizarInformacionSeccion()
                cargarAsistenciaLocal()
            } catch (_: Exception) { }
        }

        binding.includeStickyBar.btnNotificaciones.setOnClickListener {
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
        actualizarBadgeNotificaciones()
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
            binding.btnJornadaManana.setBackgroundResource(R.drawable.bg_pill_green)
            binding.btnJornadaManana.setTextColor(Color.WHITE)

            binding.btnJornadaTarde.setBackgroundResource(0)
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
            binding.btnJornadaTarde.setBackgroundResource(R.drawable.bg_pill_green)
            binding.btnJornadaTarde.setTextColor(Color.WHITE)

            binding.btnJornadaManana.setBackgroundResource(0)
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

    private fun actualizarBadgeNotificaciones() {
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.obtenerNotificaciones()
                if (response.isSuccessful && !response.body().isNullOrEmpty()) {
                    val noLeidas = response.body()!!.count { it.leido == false }
                    if (noLeidas > 0) {
                        binding.includeStickyBar.badgeNotificacionesSticky.visibility = View.VISIBLE
                    } else {
                        binding.includeStickyBar.badgeNotificacionesSticky.visibility = View.GONE
                    }
                } else {
                    binding.includeStickyBar.badgeNotificacionesSticky.visibility = View.GONE
                }
            } catch (_: Exception) {
                binding.includeStickyBar.badgeNotificacionesSticky.visibility = View.GONE
            }
        }
    }

    private fun mostrarPopupNotificaciones() {
        val popupBinding = PopupNotificacionesBinding.inflate(layoutInflater)
        popupBinding.txtCantidadNotificaciones.text = "0"
        popupBinding.contenedorSinNotificaciones.visibility = View.GONE
        popupBinding.contenedorNotificaciones.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.obtenerNotificaciones()
                if (response.isSuccessful && !response.body().isNullOrEmpty()) {
                    val notifs = response.body()!!
                    val noLeidas = notifs.count { it.leido == false }
                    popupBinding.txtCantidadNotificaciones.text = noLeidas.toString()
                    if (notifs.isEmpty()) {
                        popupBinding.contenedorNotificaciones.visibility = View.GONE
                        popupBinding.contenedorSinNotificaciones.visibility = View.VISIBLE
                    } else {
                        popupBinding.contenedorNotificaciones.visibility = View.VISIBLE
                        popupBinding.contenedorSinNotificaciones.visibility = View.GONE
                    }
                } else {
                    popupBinding.txtCantidadNotificaciones.text = "0"
                    popupBinding.contenedorNotificaciones.visibility = View.GONE
                    popupBinding.contenedorSinNotificaciones.visibility = View.VISIBLE
                }
            } catch (_: Exception) {
                popupBinding.txtCantidadNotificaciones.text = "0"
                popupBinding.contenedorNotificaciones.visibility = View.GONE
                popupBinding.contenedorSinNotificaciones.visibility = View.VISIBLE
            }
        }

        val dialog = MaterialAlertDialogBuilder(this)
            .setView(popupBinding.root)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        popupBinding.notificacionPreparacion.setOnClickListener {
            dialog.dismiss()
            val intent = Intent(this, PreparacionActivity::class.java).apply {
                platoActualSeleccionado?.let { plato ->
                    putExtra("id_plato", plato.idPlato)
                    putExtra("nombre_plato", plato.nombrePlato)
                }
            }
            startActivity(intent)
        }

        popupBinding.notificacionInventario.setOnClickListener {
            dialog.dismiss()
            val intent = Intent(this, InventarioActivity::class.java)
            startActivity(intent)
        }

        popupBinding.notificacionAsistencia.setOnClickListener {
            dialog.dismiss()
            val intent = Intent(this, AsignadasActivity::class.java)
            startActivity(intent)
        }

        popupBinding.btnVerTodasNotificaciones.setOnClickListener {
            dialog.dismiss()
            val intent = Intent(this, AvisosActivity::class.java)
            startActivity(intent)
        }

        dialog.show()
    }

    private fun configurarGiroRuleta() {
        binding.layoutMenuHoyHeader.setOnClickListener {
            val intent = Intent(this, MenuDiaActivity::class.java)
            startActivity(intent)
        }
        binding.btnVerMenuCompleto.setOnClickListener {
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

        val colorActivo = Color.parseColor("#1D3E1A")
        val colorInactivo = Color.parseColor("#FFFFFF")
        val strokeActivo = Color.parseColor("#1D3E1A")
        val strokeInactivo = Color.parseColor("#E2E8F0")

        binding.btnSeccionDesayuno.setCardBackgroundColor(if (index == 0) colorActivo else colorInactivo)
        binding.btnSeccionDesayuno.strokeColor = if (index == 0) strokeActivo else strokeInactivo
        binding.tvLabelDesayuno.setTextColor(if (index == 0) Color.WHITE else Color.parseColor("#1E293B"))

        binding.btnSeccionAlmuerzo.setCardBackgroundColor(if (index == 1) colorActivo else colorInactivo)
        binding.btnSeccionAlmuerzo.strokeColor = if (index == 1) strokeActivo else strokeInactivo
        binding.tvLabelAlmuerzo.setTextColor(if (index == 1) Color.WHITE else Color.parseColor("#1E293B"))

        binding.btnSeccionMerienda.setCardBackgroundColor(if (index == 2) colorActivo else colorInactivo)
        binding.btnSeccionMerienda.strokeColor = if (index == 2) strokeActivo else strokeInactivo
        binding.tvLabelMerienda.setTextColor(if (index == 2) Color.WHITE else Color.parseColor("#1E293B"))

        val horario = when (index) {
            0 -> "07:00 â€“ 08:15"
            1 -> "11:30 â€“ 13:00"
            else -> "09:30 â€“ 10:15"
        }
        binding.tvHorarioServicio.text = horario

        actualizarInformacionSeccion()
    }

    private fun resaltarCard(card: com.google.android.material.card.MaterialCardView, activa: Boolean) {
        val density = resources.displayMetrics.density
        val params = card.layoutParams
        val sizePx = if (activa) (92 * density).toInt() else (52 * density).toInt()
        params.width = sizePx
        params.height = sizePx
        card.radius = sizePx / 2f
        card.shapeAppearanceModel = card.shapeAppearanceModel
            .toBuilder()
            .setAllCornerSizes(sizePx / 2f)
            .build()
        card.strokeWidth = if (activa) (4 * density).toInt() else (2 * density).toInt()
        card.cardElevation = if (activa) 12 * density else 2 * density
        card.layoutParams = params
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

        // 2. Asignar imágenes circulares correspondientes a cada sector de la ruleta si existen
        if (!platoDes.isNullOrBlank()) {
            binding.imgRuletaDesayuno.setImageResource(
                InicioJefaActivity.obtenerImagenPlatoPorNombre(platoDes)
            )
        }
        if (!platoAlm.isNullOrBlank()) {
            binding.imgRuletaAlmuerzo.setImageResource(
                InicioJefaActivity.obtenerImagenPlatoPorNombre(platoAlm)
            )
        }
        if (!platoMer.isNullOrBlank()) {
            binding.imgRuletaMerienda.setImageResource(
                InicioJefaActivity.obtenerImagenPlatoPorNombre(platoMer)
            )
        }

        // 3. Actualizar información de la sección actualmente seleccionada
        val nombreSeccion = seccionesNombres[seccionSeleccionada]
        binding.tvTagSeccionSeleccionada.text = "TU COMPONENTE · ${nombreSeccion.uppercase()}"
        binding.tvSeccionSeleccionada.text = nombreSeccion.uppercase()

        val platoActivo = when (seccionSeleccionada) {
            0 -> platoDes
            1 -> platoAlm
            else -> platoMer
        }?.takeIf { it.isNotBlank() } ?: sessionMgr.getPlatoJefa(this).takeIf { it.isNotBlank() }

        if (!platoActivo.isNullOrBlank()) {
            binding.imgPlatoMain.setImageResource(
                InicioJefaActivity.obtenerImagenPlatoPorNombre(platoActivo)
            )
        }

        // 4. Mostrar componente asignado a esta manipuladora o estado correspondiente
        val componenteJefa = sessionMgr.getComponenteAsignado(this, nombreSeccion)

        if (platoActivo.isNullOrBlank()) {
            // Caso 1: La jefa aún no ha seleccionado nada del menú
            binding.tvComponenteAsignado.text = "La jefa aún no ha seleccionado el menú de hoy"
            binding.tvPlatoSeleccionado.text = "Servicio: $nombreSeccion · Menú pendiente"
            binding.tvSubtituloCardAsignadas.text = "Toca para ver el menú del día y asistencia"
        } else if (componenteJefa.isNullOrBlank()) {
            // Caso 2: La jefa ya eligió menú pero no le asignó componente a ella
            binding.tvComponenteAsignado.text = "Sin componente asignado"
            binding.tvPlatoSeleccionado.text = "Plato de hoy: $platoActivo"
            binding.tvSubtituloCardAsignadas.text = "Toca para ver detalles y preparación"
        } else {
            // Caso 3: La jefa eligió menú y le asignó componente
            binding.tvComponenteAsignado.text = componenteJefa
            binding.tvPlatoSeleccionado.text = "Plato: $platoActivo"
            binding.tvSubtituloCardAsignadas.text = "Tu tarea: $componenteJefa · $platoActivo"
        }

        val componentes = com.example.movilmanupuladora.data.model.MenuPaeCatalog.obtenerComponentesPorNombre(platoActivo ?: "")
        val bebidaOComp = componentes.firstOrNull { it.contains("bebida", true) || it.contains("leche", true) || it.contains("avena", true) || it.contains("jugo", true) || it.contains("refresco", true) }
            ?: componentes.firstOrNull { it != componenteJefa }
            ?: "Preparación de alimentos PAE"
        binding.tvSubtituloBebida.text = bebidaOComp

        val miNombre = sessionMgr.getUserName() ?: "Manipuladora"
        val miCorreo = sessionMgr.getUserEmail() ?: ""
        val comp = componenteJefa ?: "Preparación general"
        val estadoActual = com.example.movilmanupuladora.ui.manipuladora_Jefe.PreparacionEstadoManager.obtenerEstado(this, miNombre, comp, miCorreo)
        val textoEtapas = when (estadoActual.lowercase()) {
            "listo", "terminado" -> "3/3 etapas (Listo)"
            "en proceso", "cocinando" -> "2/3 etapas (En proceso)"
            else -> "1/3 etapas (Por iniciar)"
        }
        binding.tvEtapasJornada.text = textoEtapas
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
            binding.tvTotalAsistenciaSumatoria.text = "$tot est."
            binding.tvDesgloseAsistencia.text = "Prim: $p Â· Sec: $s"
            binding.tvRacionesBadge.text = tot.toString()
            binding.tvSubtituloRacionesCalculadas.text = "$tot raciones calculadas Â· Primaria $p Â· Sec. $s"
        } else {
            binding.tvTotalAsistenciaSumatoria.text = "â€”"
            binding.tvDesgloseAsistencia.text = "Sin registro aÃºn"
            binding.tvRacionesBadge.text = "â€”"
            binding.tvSubtituloRacionesCalculadas.text = "Raciones pendientes de registro por la jefa"
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
        val dialogBinding = com.example.movilmanupuladora.databinding.DialogDetalleAsistenciaBinding.inflate(layoutInflater)

        // Cargar valores guardados en AsistenciaManager (registrados por la Jefa)
        val detalle = AsistenciaManager.obtenerDetalleGradosHoy(this)

        val jar = detalle["jar"]?.toIntOrNull() ?: 15
        val p1 = detalle["p1"]?.toIntOrNull() ?: 20
        val p2 = detalle["p2"]?.toIntOrNull() ?: 20
        val p3 = detalle["p3"]?.toIntOrNull() ?: 15
        val p4 = detalle["p4"]?.toIntOrNull() ?: 15
        val p5 = detalle["p5"]?.toIntOrNull() ?: 14

        val s6 = detalle["s6"]?.toIntOrNull() ?: 12
        val s7 = detalle["s7"]?.toIntOrNull() ?: 12
        val s8 = detalle["s8"]?.toIntOrNull() ?: 11
        val s9 = detalle["s9"]?.toIntOrNull() ?: 11
        val s10 = detalle["s10"]?.toIntOrNull() ?: 10
        val s11 = detalle["s11"]?.toIntOrNull() ?: 10

        val pSaved = AsistenciaManager.obtenerPrimaria(this)
        val sSaved = AsistenciaManager.obtenerSecundaria(this)

        val finalP = if (detalle.isEmpty() || detalle.values.all { it.isBlank() }) {
            if (pSaved > 0) pSaved else 99
        } else {
            jar + p1 + p2 + p3 + p4 + p5
        }

        val finalS = if (detalle.isEmpty() || detalle.values.all { it.isBlank() }) {
            if (sSaved > 0) sSaved else 66
        } else {
            s6 + s7 + s8 + s9 + s10 + s11
        }

        val total = finalP + finalS

        // Asignar textos a la vista de detalle de asistencia de la manipuladora
        dialogBinding.tvValJardin.text = jar.toString()
        dialogBinding.tvValPrimaria.text = finalP.toString()
        dialogBinding.tvValSecundaria.text = finalS.toString()

        dialogBinding.txtTotalEstudiantesCalculado.text = "Total Estudiantes: $total (Primaria: $finalP | Secundaria: $finalS)"
        val gramajeKg = (finalP * 0.25) + (finalS * 0.38)
        dialogBinding.txtGramajeTotalEstimado.text = String.format(Locale.US, "Gramaje total estimado: %.1f kg a cocinar", gramajeKg)

        // Chips de filtro
        dialogBinding.chipTodosAsistencia.setOnClickListener {
            dialogBinding.chipTodosAsistencia.setBackgroundResource(R.drawable.bg_pill_green)
            dialogBinding.chipTodosAsistencia.setTextColor(Color.WHITE)
            dialogBinding.chipPrimariaAsistencia.setBackgroundResource(0)
            dialogBinding.chipPrimariaAsistencia.setTextColor(Color.parseColor("#64748B"))
            dialogBinding.chipSecundariaAsistencia.setBackgroundResource(0)
            dialogBinding.chipSecundariaAsistencia.setTextColor(Color.parseColor("#64748B"))
            dialogBinding.layoutPrimaria.visibility = View.VISIBLE
            dialogBinding.layoutSecundaria.visibility = View.VISIBLE
        }

        dialogBinding.chipPrimariaAsistencia.setOnClickListener {
            dialogBinding.chipPrimariaAsistencia.setBackgroundResource(R.drawable.bg_pill_green)
            dialogBinding.chipPrimariaAsistencia.setTextColor(Color.WHITE)
            dialogBinding.chipTodosAsistencia.setBackgroundResource(0)
            dialogBinding.chipTodosAsistencia.setTextColor(Color.parseColor("#64748B"))
            dialogBinding.chipSecundariaAsistencia.setBackgroundResource(0)
            dialogBinding.chipSecundariaAsistencia.setTextColor(Color.parseColor("#64748B"))
            dialogBinding.layoutPrimaria.visibility = View.VISIBLE
            dialogBinding.layoutSecundaria.visibility = View.GONE
        }

        dialogBinding.chipSecundariaAsistencia.setOnClickListener {
            dialogBinding.chipSecundariaAsistencia.setBackgroundResource(R.drawable.bg_pill_green)
            dialogBinding.chipSecundariaAsistencia.setTextColor(Color.WHITE)
            dialogBinding.chipTodosAsistencia.setBackgroundResource(0)
            dialogBinding.chipTodosAsistencia.setTextColor(Color.parseColor("#64748B"))
            dialogBinding.chipPrimariaAsistencia.setBackgroundResource(0)
            dialogBinding.chipPrimariaAsistencia.setTextColor(Color.parseColor("#64748B"))
            dialogBinding.layoutPrimaria.visibility = View.GONE
            dialogBinding.layoutSecundaria.visibility = View.VISIBLE
        }

        val dialog = MaterialAlertDialogBuilder(this)
            .setView(dialogBinding.root)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        dialogBinding.btnEntendidoAsistencia.setOnClickListener {
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
        binding.tvSubtituloAnterior1.text = formatearNombrePlato(plato1.nombrePlato)
        binding.imgMenuAnterior1.setImageResource(obtenerImagenPlato(plato1))
        binding.tvFechaAnterior1.text = fecha1
        binding.cardMenuAnterior1.setOnClickListener { mostrarDialogoPlato(plato1) }

        binding.tvPlatoAnterior2.text = "Desayuno"
        binding.tvSubtituloAnterior2.text = formatearNombrePlato(plato2.nombrePlato)
        binding.imgMenuAnterior2.setImageResource(obtenerImagenPlato(plato2))
        binding.tvFechaAnterior2.text = fecha2
        binding.cardMenuAnterior2.setOnClickListener { mostrarDialogoPlato(plato2) }

        binding.tvPlatoAnterior3.text = "Merienda"
        binding.tvSubtituloAnterior3.text = formatearNombrePlato(plato3.nombrePlato)
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