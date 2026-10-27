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
            nombrePlato = "Café con Pan y Huevo",
            componente = "Bebida caliente y proteína"
        ),
        PlatoResponse(
            idPlato = 2,
            idSeccion = 2,
            nombrePlato = "Arroz con Pollo",
            componente = "Sopas, Sazón y Proteína"
        ),
        PlatoResponse(
            idPlato = 3,
            idSeccion = 3,
            nombrePlato = "Muffin y Leche",
            componente = "Cereal y lácteo"
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
        val nombreUser = sessionMgr.getUserName() ?: "María López"
        binding.tvSaludo.text = nombreUser

        val fechaFormatted = SimpleDateFormat(
            "EEEE / d / MMM / yyyy",
            Locale.forLanguageTag("es-CO")
        ).format(Date()).uppercase()
        binding.tvFecha.text = fechaFormatted

        listaPlatos = platosBaseRespaldo
        listaSecciones = seccionesRespaldo

        configurarBotonesJornadaInicio()
        configurarGiroRuleta()
        cargarDatosDesdeBackend()
        actualizarSumatoriaAsistencia()

        binding.btnNotificaciones.setOnClickListener {
            mostrarPopupNotificaciones()
        }

        // Detectar plato y componente asignado por la Jefa
        binding.btnVerPreparacion.setOnClickListener {
            val intent = Intent(this, PreparacionActivity::class.java).apply {
                val platoJefa = sessionMgr.getPlatoJefa(this@MainActivity)
                val compJefa = sessionMgr.getComponenteAsignado(this@MainActivity)
                putExtra("nombre_plato", platoJefa)
                putExtra("componente_seleccionado", compJefa)
                putExtra("id_imagen", com.example.movilmanupuladora.ui.manipuladora_Jefe.InicioJefaActivity.obtenerImagenPlatoPorNombre(platoJefa))
            }
            startActivity(intent)
        }

        binding.cardPlatoMain.setOnClickListener {
            val intent = Intent(this, PreparacionActivity::class.java).apply {
                val platoJefa = sessionMgr.getPlatoJefa(this@MainActivity)
                val compJefa = sessionMgr.getComponenteAsignado(this@MainActivity)
                putExtra("nombre_plato", platoJefa)
                putExtra("componente_seleccionado", compJefa)
                putExtra("id_imagen", com.example.movilmanupuladora.ui.manipuladora_Jefe.InicioJefaActivity.obtenerImagenPlatoPorNombre(platoJefa))
            }
            startActivity(intent)
        }

        binding.cardAccesoAsignadas.setOnClickListener {
            val intent = Intent(this, PreparacionActivity::class.java).apply {
                val platoJefa = sessionMgr.getPlatoJefa(this@MainActivity)
                val compJefa = sessionMgr.getComponenteAsignado(this@MainActivity)
                putExtra("nombre_plato", platoJefa)
                putExtra("componente_seleccionado", compJefa)
                putExtra("id_imagen", com.example.movilmanupuladora.ui.manipuladora_Jefe.InicioJefaActivity.obtenerImagenPlatoPorNombre(platoJefa))
            }
            startActivity(intent)
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
                cargarAsistenciaBackend()
                delay(4000)
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

            // Jornada Mañana: Desayuno y Almuerzo activos
            binding.btnSeccionDesayuno.alpha = 1.0f
            binding.btnSeccionDesayuno.isEnabled = true

            binding.btnSeccionAlmuerzo.alpha = 1.0f
            binding.btnSeccionAlmuerzo.isEnabled = true

            binding.btnSeccionMerienda.alpha = 0.35f
            binding.btnSeccionMerienda.isEnabled = false

            seleccionarSeccion(0) // Selecciona Desayuno por defecto
        } else {
            binding.btnJornadaTarde.setBackgroundResource(R.drawable.bg_pill_yellow)
            binding.btnJornadaTarde.setTextColor(Color.parseColor("#7A5500"))

            binding.btnJornadaManana.setBackgroundResource(R.drawable.bg_tag_plato)
            binding.btnJornadaManana.setTextColor(Color.parseColor("#64748B"))

            // Jornada Tarde: Solo Merienda activa
            binding.btnSeccionDesayuno.alpha = 0.35f
            binding.btnSeccionDesayuno.isEnabled = false

            binding.btnSeccionAlmuerzo.alpha = 0.35f
            binding.btnSeccionAlmuerzo.isEnabled = false

            binding.btnSeccionMerienda.alpha = 1.0f
            binding.btnSeccionMerienda.isEnabled = true

            seleccionarSeccion(2) // Selecciona Merienda por defecto
        }
    }

    private fun mostrarPopupNotificaciones() {
        val popupBinding = PopupNotificacionesBinding.inflate(layoutInflater)
        popupBinding.txtCantidadNotificaciones.text = "3"

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

        when (index) {
            0 -> {
                resaltarCard(binding.cardDesayuno, true)
                resaltarCard(binding.cardAlmuerzo, false)
                resaltarCard(binding.cardMerienda, false)
            }
            1 -> {
                resaltarCard(binding.cardDesayuno, false)
                resaltarCard(binding.cardAlmuerzo, true)
                resaltarCard(binding.cardMerienda, false)
            }
            2 -> {
                resaltarCard(binding.cardDesayuno, false)
                resaltarCard(binding.cardAlmuerzo, false)
                resaltarCard(binding.cardMerienda, true)
            }
        }

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

    private fun actualizarInformacionSeccion() {
        val sessionMgr = SessionManager(this)
        val platoJefa = sessionMgr.getPlatoJefa(this)
        val componenteJefa = sessionMgr.getComponenteAsignado(this)

        val nombreSeccion = seccionesNombres[seccionSeleccionada]
        binding.tvSeccionSeleccionada.text = nombreSeccion.uppercase()

        // Mostrar prioritariamente el plato seleccionado por la Jefa
        if (platoJefa.isNotBlank()) {
            if (!componenteJefa.isNullOrBlank()) {
                binding.tvPlatoSeleccionado.text = "$platoJefa\n(Mi componente: $componenteJefa)"
            } else {
                binding.tvPlatoSeleccionado.text = platoJefa
            }
            binding.imgPlatoMain.setImageResource(
                com.example.movilmanupuladora.ui.manipuladora_Jefe.InicioJefaActivity.obtenerImagenPlatoPorNombre(platoJefa)
            )
        } else {
            val plato = obtenerPlatoPorSeccion(nombreSeccion)
            if (plato != null) {
                platoActualSeleccionado = plato
                binding.tvPlatoSeleccionado.text = formatearNombrePlato(plato.nombrePlato)
                binding.imgPlatoMain.setImageResource(obtenerImagenPlato(plato))
            } else {
                platoActualSeleccionado = null
                binding.tvPlatoSeleccionado.text = "Sin menú asignado"
                binding.imgPlatoMain.setImageResource(R.drawable.plato1)
            }
        }
    }

    private fun cargarDatosDesdeBackend() {
        lifecycleScope.launch {
            try {
                val resPlatos = menuRepository.obtenerPlatos()
                if (resPlatos.isSuccessful && !resPlatos.body().isNullOrEmpty()) {
                    listaPlatos = resPlatos.body()!!
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
                        idPrimariaCache = listaGrados.find { it.nombreGrado.contains("primaria", ignoreCase = true) }?.idGrado ?: 16
                        idSecundariaCache = listaGrados.find { it.nombreGrado.contains("secundaria", ignoreCase = true) }?.idGrado ?: 19
                        gradosCargados = true
                    }
                }

                val asistenciasRes = asistenciaRepository.obtenerAsistencias()
                if (asistenciasRes.isSuccessful && !asistenciasRes.body().isNullOrEmpty()) {
                    val todas = asistenciasRes.body()!!
                    val fechaHoy = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

                    // 1. Filtrar registros con la fecha exacta de hoy
                    var registrosFecha = todas.filter { it.fecha.trim().startsWith(fechaHoy) }

                    // 2. Si no hay registros de hoy (por posible desfase de reloj o zona horaria UTC entre teléfonos),
                    // tomar automáticamente los de la fecha más reciente registrada en el servidor
                    if (registrosFecha.isEmpty()) {
                        val fechaMasReciente = todas.maxByOrNull { it.idAsistencia ?: 0 }?.fecha?.trim()?.take(10)
                        if (!fechaMasReciente.isNullOrEmpty()) {
                            registrosFecha = todas.filter { it.fecha.trim().startsWith(fechaMasReciente) }
                        }
                    }

                    // 3. Tomar siempre el registro más reciente para Primaria y Secundaria
                    val regPrimaria = registrosFecha
                        .filter { it.idGrado == idPrimariaCache || it.idGrado == 16 }
                        .maxByOrNull { it.idAsistencia ?: 0 }

                    val regSecundaria = registrosFecha
                        .filter { it.idGrado == idSecundariaCache || it.idGrado == 19 }
                        .maxByOrNull { it.idAsistencia ?: 0 }

                    var totalPrimaria = regPrimaria?.ninosPresentes ?: 0
                    var totalSecundaria = regSecundaria?.ninosPresentes ?: 0

                    if (regPrimaria == null && regSecundaria == null && registrosFecha.isNotEmpty()) {
                        val regLegacy = registrosFecha.maxByOrNull { it.idAsistencia ?: 0 }!!
                        val tot = regLegacy.ninosPresentes
                        totalPrimaria = (tot * 0.6).toInt()
                        totalSecundaria = tot - totalPrimaria
                    }

                    val totalEstudiantes = totalPrimaria + totalSecundaria
                    val existeRegistro = (regPrimaria != null || regSecundaria != null || registrosFecha.isNotEmpty())

                    // Guardar en la caché local del dispositivo de la manipuladora
                    AsistenciaManager.guardarPrimaria(this@MainActivity, totalPrimaria)
                    AsistenciaManager.guardarSecundaria(this@MainActivity, totalSecundaria)
                    AsistenciaManager.guardarManana(this@MainActivity, totalEstudiantes)

                    withContext(Dispatchers.Main) {
                        actualizarVistaAsistencia(
                            primaria = totalPrimaria,
                            secundaria = totalSecundaria,
                            total = totalEstudiantes,
                            hayRegistro = existeRegistro
                        )
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
        val total = p + s
        actualizarVistaAsistencia(p, s, total, hayRegistro = (total > 0))
    }

    private fun actualizarVistaAsistencia(primaria: Int, secundaria: Int, total: Int, hayRegistro: Boolean = true) {
        if (hayRegistro) {
            binding.tvTotalAsistenciaSumatoria.text = "$total Estudiantes registrados"
            if (total == 0) {
                binding.tvDesgloseAsistencia.text = "Primaria: $primaria  |  Secundaria: $secundaria (Sin asistencia hoy)"
            } else {
                binding.tvDesgloseAsistencia.text = "Primaria: $primaria  |  Secundaria: $secundaria"
            }
        } else {
            binding.tvTotalAsistenciaSumatoria.text = "Sin datos de asistencia"
            binding.tvDesgloseAsistencia.text = "Dato suministrado por la jefa"
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
            R.layout.dialog_registrar_asistencia,
            null
        )

        val txtTitulo = vistaDialogo.findViewById<android.widget.TextView>(R.id.txtTituloDialog)
        txtTitulo.text = "Asistencia de Estudiantes por Grado"

        val edtJardin = vistaDialogo.findViewById<android.widget.EditText>(R.id.edtGradoJardin)
        val edtP1 = vistaDialogo.findViewById<android.widget.EditText>(R.id.edtGradoPrimero)
        val edtP2 = vistaDialogo.findViewById<android.widget.EditText>(R.id.edtGradoSegundo)
        val edtP3 = vistaDialogo.findViewById<android.widget.EditText>(R.id.edtGradoTercero)
        val edtP4 = vistaDialogo.findViewById<android.widget.EditText>(R.id.edtGradoCuarto)
        val edtP5 = vistaDialogo.findViewById<android.widget.EditText>(R.id.edtGradoQuinto)

        val edtS6 = vistaDialogo.findViewById<android.widget.EditText>(R.id.edtGradoSexto)
        val edtS7 = vistaDialogo.findViewById<android.widget.EditText>(R.id.edtGradoSeptimo)
        val edtS8 = vistaDialogo.findViewById<android.widget.EditText>(R.id.edtGradoOctavo)
        val edtS9 = vistaDialogo.findViewById<android.widget.EditText>(R.id.edtGradoNoveno)
        val edtS10 = vistaDialogo.findViewById<android.widget.EditText>(R.id.edtGradoDecimo)
        val edtS11 = vistaDialogo.findViewById<android.widget.EditText>(R.id.edtCantidadSecundaria)

        val txtTotalCalc = vistaDialogo.findViewById<android.widget.TextView>(R.id.txtTotalEstudiantesCalculado)
        val txtGramajeCalc = vistaDialogo.findViewById<android.widget.TextView>(R.id.txtGramajeTotalEstimado)

        val btnCancelar = vistaDialogo.findViewById<android.widget.Button>(R.id.btnCancelarAsistencia)
        val btnGuardar = vistaDialogo.findViewById<android.widget.Button>(R.id.btnGuardarAsistencia)

        btnGuardar.text = "Entendido"
        btnCancelar.visibility = View.GONE

        // Cargar valores guardados en AsistenciaManager
        val detalle = AsistenciaManager.obtenerDetalleGradosHoy(this)
        edtJardin.setText(detalle["jar"].orEmpty())
        edtP1.setText(detalle["p1"].orEmpty())
        edtP2.setText(detalle["p2"].orEmpty())
        edtP3.setText(detalle["p3"].orEmpty())
        edtP4.setText(detalle["p4"].orEmpty())
        edtP5.setText(detalle["p5"].orEmpty())

        edtS6.setText(detalle["s6"].orEmpty())
        edtS7.setText(detalle["s7"].orEmpty())
        edtS8.setText(detalle["s8"].orEmpty())
        edtS9.setText(detalle["s9"].orEmpty())
        edtS10.setText(detalle["s10"].orEmpty())
        edtS11.setText(detalle["s11"].orEmpty())

        // Deshabilitar edición para la manipuladora (modo consulta)
        listOf(edtJardin, edtP1, edtP2, edtP3, edtP4, edtP5, edtS6, edtS7, edtS8, edtS9, edtS10, edtS11).forEach {
            it.isEnabled = false
            it.setTextColor(Color.parseColor("#1B3317"))
        }

        val pTotal = (edtJardin.text.toString().trim().toIntOrNull() ?: 0) +
                     (edtP1.text.toString().trim().toIntOrNull() ?: 0) +
                     (edtP2.text.toString().trim().toIntOrNull() ?: 0) +
                     (edtP3.text.toString().trim().toIntOrNull() ?: 0) +
                     (edtP4.text.toString().trim().toIntOrNull() ?: 0) +
                     (edtP5.text.toString().trim().toIntOrNull() ?: 0)

        val sTotal = (edtS6.text.toString().trim().toIntOrNull() ?: 0) +
                     (edtS7.text.toString().trim().toIntOrNull() ?: 0) +
                     (edtS8.text.toString().trim().toIntOrNull() ?: 0) +
                     (edtS9.text.toString().trim().toIntOrNull() ?: 0) +
                     (edtS10.text.toString().trim().toIntOrNull() ?: 0) +
                     (edtS11.text.toString().trim().toIntOrNull() ?: 0)

        val finalP = if (pTotal > 0) pTotal else AsistenciaManager.obtenerPrimaria(this)
        val finalS = if (sTotal > 0) sTotal else AsistenciaManager.obtenerSecundaria(this)
        val total = finalP + finalS

        val gramajeKg = (finalP * 0.25) + (finalS * 0.38)
        txtTotalCalc.text = "Total Estudiantes: $total (Primaria: $finalP | Secundaria: $finalS)"
        txtGramajeCalc.text = String.format(Locale.US, "Gramaje total estimado: %.1f kg a cocinar", gramajeKg)

        val dialog = MaterialAlertDialogBuilder(this)
            .setView(vistaDialogo)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        btnGuardar.setOnClickListener {
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
            it.nombrePlato?.contains("café", true) == true || it.nombrePlato?.contains("huevo", true) == true
        } ?: platosBaseRespaldo[0]

        val plato3 = listaPlatos.firstOrNull {
            it.nombrePlato?.contains("muffin", true) == true || it.nombrePlato?.contains("leche", true) == true
        } ?: platosBaseRespaldo[2]

        binding.tvPlatoAnterior1.text = "Almuerzo"
        binding.imgMenuAnterior1.setImageResource(obtenerImagenPlato(plato1))
        binding.tvFechaAnterior1.text = "01 Oct"
        binding.cardMenuAnterior1.setOnClickListener { mostrarDialogoPlato(plato1) }

        binding.tvPlatoAnterior2.text = "Desayuno"
        binding.imgMenuAnterior2.setImageResource(obtenerImagenPlato(plato2))
        binding.tvFechaAnterior2.text = "30 Sep"
        binding.cardMenuAnterior2.setOnClickListener { mostrarDialogoPlato(plato2) }

        binding.tvPlatoAnterior3.text = "Merienda"
        binding.imgMenuAnterior3.setImageResource(obtenerImagenPlato(plato3))
        binding.tvFechaAnterior3.text = "29 Sep"
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