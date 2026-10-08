package com.example.movilmanupuladora.ui.manipuladora

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.movilmanupuladora.R
import com.example.movilmanupuladora.data.api.RetrofitClient
import com.example.movilmanupuladora.data.model.DetallePlato
import com.example.movilmanupuladora.data.model.PlatoResponse
import com.example.movilmanupuladora.data.model.SeccionMenu
import com.example.movilmanupuladora.data.repository.MenuRepository
import com.example.movilmanupuladora.databinding.ActivityMainBinding
import com.example.movilmanupuladora.databinding.DialogDetallePlatoBinding
import com.example.movilmanupuladora.databinding.PopupNotificacionesBinding
import com.example.movilmanupuladora.utils.NavigationHelper
import com.example.movilmanupuladora.utils.SessionManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val menuRepository = MenuRepository(RetrofitClient.apiService)

    // =========================================================
    // DATOS DEL BACKEND / LOCAL
    // =========================================================

    private var listaPlatos: List<PlatoResponse> = emptyList()
    private var listaSecciones: List<SeccionMenu> = emptyList()
    private var listaDetalles: List<DetallePlato> = emptyList()

    // Plato actualmente seleccionado
    private var platoActualSeleccionado: PlatoResponse? = null

    // 0 = Desayuno, 1 = Almuerzo, 2 = Merienda
    private val seccionesNombres = listOf("Desayuno", "Almuerzo", "Merienda")
    private var seccionSeleccionada = 0

    // Jornada activa en el Inicio: "MANANA" o "TARDE"
    private var jornadaActualInicio = "MANANA"

    // Ángulo de rotación de la rueda circular
    private var anguloRotacionActual = 0f

    // =========================================================
    // DATOS DE RESPALDO (MOCK)
    // =========================================================

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

    // =========================================================
    // ON CREATE
    // =========================================================

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Token de sesión
        SessionManager.getToken(this)

        // Configurar usuario y fecha
        val sessionMgr = SessionManager(this)
        val nombreUser = sessionMgr.getUserName() ?: "María López"
        binding.tvSaludo.text = nombreUser

        val fechaFormatted = SimpleDateFormat(
            "EEEE / d / MMM / yyyy",
            Locale.forLanguageTag("es-CO")
        ).format(Date()).uppercase()
        binding.tvFecha.text = fechaFormatted

        // Inicializar con respaldo
        listaPlatos = platosBaseRespaldo
        listaSecciones = seccionesRespaldo

        // Configurar botones de Jornada Mañana / Tarde
        configurarBotonesJornadaInicio()

        // Configurar giro y deslizamiento en la rueda circular
        configurarGiroRuleta()

        // Cargar datos reales de la API
        cargarDatosDesdeBackend()

        // Botón Notificaciones -> Abre Pop-up emergente de Notificaciones
        binding.btnNotificaciones.setOnClickListener {
            mostrarPopupNotificaciones()
        }

        // Botón "Ver preparación" -> Abre la receta y pasos de cocción
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

        // Configurar tarjetas de menús anteriores
        configurarMenusAnteriores()

        // Configurar barra de navegación inferior
        NavigationHelper.setupBarraNavegacion(
            this,
            binding.barraNavegacion,
            NavigationHelper.Tab.INICIO,
        )
    }

    // =========================================================
    // BOTONES DE JORNADA (MAÑANA Y TARDE)
    // =========================================================

    private fun configurarBotonesJornadaInicio() {
        binding.btnJornadaManana.setOnClickListener {
            jornadaActualInicio = "MANANA"
            actualizarEstadoJornadaInicio()
        }

        binding.btnJornadaTarde.setOnClickListener {
            jornadaActualInicio = "TARDE"
            actualizarEstadoJornadaInicio()
        }

        actualizarEstadoJornadaInicio()
    }

    private fun actualizarEstadoJornadaInicio() {
        if (jornadaActualInicio == "MANANA") {
            // Estilos de botones de jornada
            binding.btnJornadaManana.setBackgroundResource(R.drawable.bg_pill_yellow)
            binding.btnJornadaManana.setTextColor(Color.parseColor("#7A5500"))

            binding.btnJornadaTarde.setBackgroundResource(R.drawable.bg_tag_plato)
            binding.btnJornadaTarde.setTextColor(Color.parseColor("#64748B"))

            // En Mañana: Solo DESAYUNO activo, Almuerzo y Merienda atenuados en gris
            binding.btnSeccionDesayuno.alpha = 1.0f
            binding.btnSeccionDesayuno.isEnabled = true

            binding.btnSeccionAlmuerzo.alpha = 0.35f
            binding.btnSeccionAlmuerzo.isEnabled = false

            binding.btnSeccionMerienda.alpha = 0.35f
            binding.btnSeccionMerienda.isEnabled = false

            seleccionarSeccion(0) // Selecciona Desayuno
        } else {
            // Estilos de botones de jornada
            binding.btnJornadaTarde.setBackgroundResource(R.drawable.bg_pill_yellow)
            binding.btnJornadaTarde.setTextColor(Color.parseColor("#7A5500"))

            binding.btnJornadaManana.setBackgroundResource(R.drawable.bg_tag_plato)
            binding.btnJornadaManana.setTextColor(Color.parseColor("#64748B"))

            // En Tarde: Desayuno atenuado en gris, Almuerzo y Merienda activos
            binding.btnSeccionDesayuno.alpha = 0.35f
            binding.btnSeccionDesayuno.isEnabled = false

            binding.btnSeccionAlmuerzo.alpha = 1.0f
            binding.btnSeccionAlmuerzo.isEnabled = true

            binding.btnSeccionMerienda.alpha = 1.0f
            binding.btnSeccionMerienda.isEnabled = true

            seleccionarSeccion(1) // Selecciona Almuerzo por defecto en la tarde
        }
    }

    // =========================================================
    // MOSTRAR POPUP EMERGENTE DE NOTIFICACIONES
    // =========================================================

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

    // =========================================================
    // GIRO Y DESLIZAMIENTO DE LA RULETA CIRCULAR
    // =========================================================

    private fun configurarGiroRuleta() {
        var startY = 0f
        var isSwiping = false

        val touchListener = View.OnTouchListener { view, event ->
            view.parent?.requestDisallowInterceptTouchEvent(true)

            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    startY = event.rawY
                    isSwiping = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val deltaY = event.rawY - startY
                    if (Math.abs(deltaY) > 15) {
                        isSwiping = true
                        val anguloTemp = anguloRotacionActual + (deltaY * 0.4f)
                        binding.wheelContainer.rotation = anguloTemp
                        contraRotarImagenes(-anguloTemp)
                    }
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    view.parent?.requestDisallowInterceptTouchEvent(false)
                    val deltaY = event.rawY - startY

                    if (isSwiping && Math.abs(deltaY) > 30) {
                        if (jornadaActualInicio == "MANANA") {
                            seleccionarSeccion(0)
                        } else {
                            if (seccionSeleccionada == 1) {
                                seleccionarSeccion(2)
                            } else {
                                seleccionarSeccion(1)
                            }
                        }
                    } else if (!isSwiping) { // Clic directo
                        when (view.id) {
                            R.id.btnSeccionDesayuno -> {
                                if (jornadaActualInicio == "MANANA") seleccionarSeccion(0)
                            }
                            R.id.btnSeccionAlmuerzo -> {
                                if (jornadaActualInicio == "TARDE") seleccionarSeccion(1)
                            }
                            R.id.btnSeccionMerienda -> {
                                if (jornadaActualInicio == "TARDE") seleccionarSeccion(2)
                            }
                        }
                    } else {
                        seleccionarSeccion(seccionSeleccionada)
                    }
                    isSwiping = false
                    true
                }
                else -> false
            }
        }

        binding.wheelAreaContainer.setOnTouchListener(touchListener)
        binding.wheelContainer.setOnTouchListener(touchListener)
        binding.btnSeccionDesayuno.setOnTouchListener(touchListener)
        binding.btnSeccionAlmuerzo.setOnTouchListener(touchListener)
        binding.btnSeccionMerienda.setOnTouchListener(touchListener)
    }

    private fun seleccionarSeccion(index: Int) {
        seccionSeleccionada = index

        val targetAngle = when (index) {
            0 -> 120f  // Desayuno
            1 -> 0f    // Almuerzo
            2 -> -120f // Merienda
            else -> 0f
        }

        anguloRotacionActual = targetAngle

        binding.wheelContainer.animate()
            .rotation(targetAngle)
            .setDuration(220)
            .start()

        contraRotarImagenes(-targetAngle)

        when (index) {
            0 -> { // DESAYUNO
                resaltarCard(binding.cardDesayuno, true)
                resaltarCard(binding.cardAlmuerzo, false)
                resaltarCard(binding.cardMerienda, false)
            }
            1 -> { // ALMUERZO
                resaltarCard(binding.cardDesayuno, false)
                resaltarCard(binding.cardAlmuerzo, true)
                resaltarCard(binding.cardMerienda, false)
            }
            2 -> { // MERIENDA
                resaltarCard(binding.cardDesayuno, false)
                resaltarCard(binding.cardAlmuerzo, false)
                resaltarCard(binding.cardMerienda, true)
            }
        }

        actualizarInformacionSeccion()
    }

    private fun contraRotarImagenes(anguloInverso: Float) {
        binding.cardDesayuno.rotation = anguloInverso
        binding.cardAlmuerzo.rotation = anguloInverso
        binding.cardMerienda.rotation = anguloInverso
    }

    private fun resaltarCard(card: com.google.android.material.card.MaterialCardView, activa: Boolean) {
        val params = card.layoutParams
        if (activa) {
            params.width = (66 * resources.displayMetrics.density).toInt()
            params.height = (66 * resources.displayMetrics.density).toInt()
            card.strokeWidth = (3.5 * resources.displayMetrics.density).toInt()
            card.cardElevation = 6 * resources.displayMetrics.density
        } else {
            params.width = (54 * resources.displayMetrics.density).toInt()
            params.height = (54 * resources.displayMetrics.density).toInt()
            card.strokeWidth = (2 * resources.displayMetrics.density).toInt()
            card.cardElevation = 2 * resources.displayMetrics.density
        }
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

    // =========================================================
    // CONSUMO DEL BACKEND (API SIRAE)
    // =========================================================

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

            } catch (e: Exception) {
                Log.e("BACKEND_SIRAE", "Error al conectar con el servidor: ${e.message}")
            }
        }
    }

    // =========================================================
    // BUSCAR PLATO POR SECCIÓN
    // =========================================================

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

    // =========================================================
    // IMAGEN DEL PLATO
    // =========================================================

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

    // =========================================================
    // MENÚS ANTERIORES
    // =========================================================

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

    // =========================================================
    // FORMATEAR NOMBRE
    // =========================================================

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

    // =========================================================
    // DIÁLOGO DETALLE DEL PLATO
    // =========================================================

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