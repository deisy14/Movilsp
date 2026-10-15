package com.example.movilmanupuladora.ui.manipuladora

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.movilmanupuladora.R
import com.example.movilmanupuladora.data.api.RetrofitClient
import com.example.movilmanupuladora.data.model.DetallePlato
import com.example.movilmanupuladora.data.model.PlatoResponse
import com.example.movilmanupuladora.data.model.SeccionMenu
import com.example.movilmanupuladora.data.repository.AsistenciaRepository
import com.example.movilmanupuladora.data.repository.MenuRepository
import com.example.movilmanupuladora.databinding.ActivityMainBinding
import com.example.movilmanupuladora.databinding.DialogDetallePlatoBinding
import com.example.movilmanupuladora.databinding.PopupNotificacionesBinding
import com.example.movilmanupuladora.ui.manipuladora_Jefe.AsistenciaManager
import com.example.movilmanupuladora.utils.NavigationHelper
import com.example.movilmanupuladora.utils.SessionManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val menuRepository = MenuRepository(RetrofitClient.apiService)
    private val asistenciaRepository = AsistenciaRepository(RetrofitClient.apiService)
    private var jobPollingAsistencia: Job? = null

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

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

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

        binding.btnVerPreparacion.setOnClickListener {
            val intent = Intent(this, PreparacionActivity::class.java).apply {
                platoActualSeleccionado?.let { plato ->
                    putExtra("id_plato", plato.idPlato)
                    putExtra("nombre_plato", plato.nombrePlato)
                    putExtra("componente_seleccionado", plato.componente)
                }
            }
            startActivity(intent)
        }

        binding.cardPlatoMain.setOnClickListener {
            platoActualSeleccionado?.let { plato ->
                mostrarDialogoPlato(plato)
            }
        }

        binding.cardAccesoAsignadas.setOnClickListener {
            val intent = Intent(this, PreparacionActivity::class.java).apply {
                platoActualSeleccionado?.let { plato ->
                    putExtra("id_plato", plato.idPlato)
                    putExtra("nombre_plato", plato.nombrePlato)
                    putExtra("componente_seleccionado", plato.componente)
                }
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
        actualizarSumatoriaAsistencia()
        cargarAsistenciaDelBackendParaManipuladora()
        iniciarPollingAsistenciaEnVivo()
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
        val totalManana = AsistenciaManager.obtenerManana(this)
        val totalTarde = AsistenciaManager.obtenerTarde(this)
        val totalAsistencia = if (jornadaActualInicio == "MANANA") totalManana else totalTarde

        if (totalAsistencia > 0) {
            val primaria = (totalAsistencia * 0.6).toInt()
            val bachillerato = totalAsistencia - primaria
            binding.tvTotalAsistenciaSumatoria.text = "$totalAsistencia Estudiantes registrados"
            binding.tvDesgloseAsistencia.text = "Primaria: $primaria  |  Bachillerato: $bachillerato"
        } else {
            binding.tvTotalAsistenciaSumatoria.text = "Sin datos de asistencia"
            binding.tvDesgloseAsistencia.text = "Dato suministrado por la jefa"
        }
    }

    private fun cargarAsistenciaDelBackendParaManipuladora() {
        lifecycleScope.launch {
            try {
                val res = RetrofitClient.apiService.obtenerAsistenciaDiaria()
                if (res.isSuccessful && !res.body().isNullOrEmpty()) {
                    val fechaHoy = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                    val registroHoy = res.body()!!.find { it.fecha == fechaHoy }
                    if (registroHoy != null) {
                        val total = registroHoy.ninosPresentes
                        if (jornadaActualInicio == "MANANA") {
                            AsistenciaManager.guardarManana(this@MainActivity, total)
                        } else {
                            AsistenciaManager.guardarTarde(this@MainActivity, total)
                        }
                        actualizarSumatoriaAsistencia()
                    }
                }
            } catch (e: Exception) {
                // Silencioso en modo offline
            }
        }
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

            binding.btnSeccionDesayuno.alpha = 1.0f
            binding.btnSeccionDesayuno.isEnabled = true

            binding.btnSeccionAlmuerzo.alpha = 0.35f
            binding.btnSeccionAlmuerzo.isEnabled = false

            binding.btnSeccionMerienda.alpha = 0.35f
            binding.btnSeccionMerienda.isEnabled = false

            seleccionarSeccion(0)
        } else {
            binding.btnJornadaTarde.setBackgroundResource(R.drawable.bg_pill_yellow)
            binding.btnJornadaTarde.setTextColor(Color.parseColor("#7A5500"))

            binding.btnJornadaManana.setBackgroundResource(R.drawable.bg_tag_plato)
            binding.btnJornadaManana.setTextColor(Color.parseColor("#64748B"))

            binding.btnSeccionDesayuno.alpha = 0.35f
            binding.btnSeccionDesayuno.isEnabled = false

            binding.btnSeccionAlmuerzo.alpha = 1.0f
            binding.btnSeccionAlmuerzo.isEnabled = true

            binding.btnSeccionMerienda.alpha = 1.0f
            binding.btnSeccionMerienda.isEnabled = true

            seleccionarSeccion(1)
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
            if (jornadaActualInicio == "TARDE") {
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
        val sizePx = if (activa) (78 * density).toInt() else (58 * density).toInt()
        params.width = sizePx
        params.height = sizePx
        card.radius = sizePx / 2f
        card.shapeAppearanceModel = card.shapeAppearanceModel
            .toBuilder()
            .setAllCornerSizes(sizePx / 2f)
            .build()
        card.strokeWidth = if (activa) (3.5 * density).toInt() else (2.5 * density).toInt()
        card.cardElevation = if (activa) 8 * density else 3 * density
        card.layoutParams = params
    }

    private fun actualizarInformacionSeccion() {
        val nombreSeccion = seccionesNombres[seccionSeleccionada]
        binding.tvSeccionSeleccionada.text = nombreSeccion.uppercase()

        val plato = obtenerPlatoPorSeccion(nombreSeccion)

        if (plato != null) {
            platoActualSeleccionado = plato
            binding.tvPlatoSeleccionado.text = formatearNombrePlato(plato.nombrePlato)
            binding.imgPlatoMain.setImageResource(obtenerImagenPlato(plato))
        } else {
            platoActualSeleccionado = null
            binding.tvPlatoSeleccionado.text = "Sin menú asignado"
            binding.imgPlatoMain.setImageResource(R.drawable.arroz_pollo)
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
        try {
            SessionManager.getToken(this@MainActivity)
            val gradosRes = asistenciaRepository.obtenerGrados()
            val listaGrados = gradosRes.body().orEmpty()
            val idPrimaria = listaGrados.find { it.nombreGrado.contains("primaria", ignoreCase = true) }?.idGrado ?: 16
            val idSecundaria = listaGrados.find { it.nombreGrado.contains("secundaria", ignoreCase = true) }?.idGrado ?: 19

            val asistenciasRes = asistenciaRepository.obtenerAsistencias()
            if (asistenciasRes.isSuccessful && !asistenciasRes.body().isNullOrEmpty()) {
                val fechaHoy = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                val asistenciasHoy = asistenciasRes.body()!!.filter { it.fecha.trim().startsWith(fechaHoy) }

                val regPrimaria = asistenciasHoy.filter { it.idGrado == idPrimaria || it.idGrado == 16 }.maxByOrNull { it.idAsistencia ?: 0 }
                val regSecundaria = asistenciasHoy.filter { it.idGrado == idSecundaria || it.idGrado == 19 }.maxByOrNull { it.idAsistencia ?: 0 }

                var totalPrimaria = regPrimaria?.ninosPresentes ?: 0
                var totalSecundaria = regSecundaria?.ninosPresentes ?: 0

                if (regPrimaria == null && regSecundaria == null && asistenciasHoy.isNotEmpty()) {
                    val regLegacy = asistenciasHoy.maxByOrNull { it.idAsistencia ?: 0 }!!
                    val tot = regLegacy.ninosPresentes
                    totalPrimaria = (tot * 0.6).toInt()
                    totalSecundaria = tot - totalPrimaria
                }

                val totalEstudiantes = totalPrimaria + totalSecundaria

                AsistenciaManager.guardarPrimaria(this@MainActivity, totalPrimaria)
                AsistenciaManager.guardarSecundaria(this@MainActivity, totalSecundaria)
                AsistenciaManager.guardarManana(this@MainActivity, totalEstudiantes)

                actualizarVistaAsistencia(
                    primaria = totalPrimaria,
                    secundaria = totalSecundaria,
                    total = totalEstudiantes,
                    hayRegistro = (regPrimaria != null || regSecundaria != null || asistenciasHoy.isNotEmpty())
                )
            } else {
                cargarAsistenciaLocal()
            }
        } catch (e: Exception) {
            cargarAsistenciaLocal()
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
            if (total > 0) {
                binding.tvTotalAsistenciaSumatoria.text = "$total Estudiantes registrados"
                binding.tvDesgloseAsistencia.text = "Primaria: $primaria  |  Secundaria: $secundaria"
            } else {
                binding.tvTotalAsistenciaSumatoria.text = "Sin datos de asistencia"
                binding.tvDesgloseAsistencia.text = "Dato suministrado por la jefa"
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
            }

            val p = AsistenciaManager.obtenerPrimaria(this)
            val s = AsistenciaManager.obtenerSecundaria(this)
            val total = p + s

            if (total > 0) {
                val kgPrimaria = p * 0.25
                val kgSecundaria = s * 0.38
                val kgTotal = kgPrimaria + kgSecundaria

                MaterialAlertDialogBuilder(this)
                    .setTitle("Sumatoria Asistencia PAE")
                    .setMessage(
                        "📊 Asistencia de estudiantes registrada para hoy:\n\n" +
                        "• Primaria: $p niños (~${String.format(Locale.US, "%.1f", kgPrimaria)} kg)\n" +
                        "• Secundaria: $s estudiantes (~${String.format(Locale.US, "%.1f", kgSecundaria)} kg)\n" +
                        "───────────────────────\n" +
                        "• Total raciones a servir: $total raciones\n" +
                        "• Gramaje total PAE: ${String.format(Locale.US, "%.1f", kgTotal)} kg"
                    )
                    .setPositiveButton("Entendido", null)
                    .show()
            } else {
                MaterialAlertDialogBuilder(this)
                    .setTitle("Sumatoria Asistencia PAE")
                    .setMessage("⚠️ Aún no hay datos de asistencia registrados por la Jefa para la jornada actual.")
                    .setPositiveButton("Entendido", null)
                    .show()
            }
        }
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
        val nombre = plato.nombrePlato?.lowercase() ?: ""

        return when {
            nombre.contains("bandeja") -> R.drawable.bandeja_paisa
            nombre.contains("frijol") -> R.drawable.frijoles
            nombre.contains("chocolate") -> R.drawable.chocolate
            nombre.contains("huevo") || nombre.contains("café") || nombre.contains("pan") -> R.drawable.comida_desayuno
            nombre.contains("pasta") || nombre.contains("carne") -> R.drawable.pasta_carne
            nombre.contains("lenteja") -> R.drawable.lentejas_arroz
            nombre.contains("pollo") -> R.drawable.arroz_pollo
            nombre.contains("muffin") || nombre.contains("leche") -> R.drawable.comida_merienda
            else -> R.drawable.arroz_pollo
        }
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