package com.example.movilmanupuladora.ui.manipuladora

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
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
import com.example.movilmanupuladora.utils.SessionManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch
import kotlin.math.atan2

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val menuRepository =
        MenuRepository(RetrofitClient.apiService)

    // =========================================================
    // DATOS DEL BACKEND
    // =========================================================

    private var listaPlatos: List<PlatoResponse> = emptyList()

    private var listaSecciones: List<SeccionMenu> = emptyList()

    private var listaDetalles: List<DetallePlato> = emptyList()

    // Plato actualmente seleccionado
    private var platoActualSeleccionado: PlatoResponse? = null

    // =========================================================
    // SECCIONES DE LA RULETA
    // =========================================================

    private val seccionesRuleta = listOf(
        "Desayuno",
        "Almuerzo",
        "Merienda"
    )

    // Índice de la sección actualmente seleccionada
    private var seccionSeleccionada = 0

    // =========================================================
    // DATOS DE RESPALDO
    // =========================================================

    private val platosBaseRespaldo = listOf(

        PlatoResponse(
            idPlato = 4,
            idSeccion = 2,
            nombrePlato = "Bandeja Paisa",
            componente =
                "carne molida, chicharron, aguacate, arroz, frijol"
        ),

        PlatoResponse(
            idPlato = 1,
            idSeccion = 3,
            nombrePlato = "Arroz a la Valenciana",
            componente = "proteina"
        ),

        PlatoResponse(
            idPlato = 2,
            idSeccion = 3,
            nombrePlato = "Arroz con Pollo",
            componente = "pollo"
        ),

        PlatoResponse(
            idPlato = 3,
            idSeccion = 3,
            nombrePlato = "Arroz con Pollo Especial",
            componente = "Principal"
        ),

        PlatoResponse(
            idPlato = 5,
            idSeccion = 4,
            nombrePlato = "Café con Pan",
            componente = "no se"
        )
    )

    private val seccionesRespaldo = listOf(

        SeccionMenu(
            idSeccion = 1,
            idJornada = 1,
            nombreSeccion = "Desayuno"
        ),

        SeccionMenu(
            idSeccion = 2,
            idJornada = 1,
            nombreSeccion = "Almuerzo"
        ),

        SeccionMenu(
            idSeccion = 3,
            idJornada = 1,
            nombreSeccion = "Merienda"
        )
    )

    // =========================================================
    // ON CREATE
    // =========================================================

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding =
            ActivityMainBinding.inflate(layoutInflater)

        setContentView(binding.root)

        // Token de sesión
        SessionManager.getToken(this)

        // -----------------------------------------------------
        // Datos iniciales
        // -----------------------------------------------------

        listaPlatos = platosBaseRespaldo

        listaSecciones = seccionesRespaldo

        // Mostrar inicialmente DESAYUNO
        seccionSeleccionada = 0

        actualizarRuleta()

        actualizarInformacionSeccion()

        configurarMenusAnteriores()

        // -----------------------------------------------------
        // Backend
        // -----------------------------------------------------

        cargarDatosDesdeBackend()

        // -----------------------------------------------------
        // Ruleta
        // -----------------------------------------------------

        configurarGiroRuleta()

        // -----------------------------------------------------
        // Tarjeta del plato
        // -----------------------------------------------------

        binding.cardPlatoSeleccionado.setOnClickListener {

            platoActualSeleccionado?.let {

                mostrarDialogoPlato(it)
            }
        }

        // =====================================================
        // BARRA DE NAVEGACIÓN
        // =====================================================

        com.example.movilmanupuladora.utils.NavigationHelper.setupBarraNavegacion(
            this,
            binding.barraNavegacion,
            com.example.movilmanupuladora.utils.NavigationHelper.Tab.INICIO
        )
    }

    // =========================================================
    // CONSUMO DEL BACKEND
    // =========================================================

    private fun cargarDatosDesdeBackend() {

        lifecycleScope.launch {

            try {

                // -------------------------------------------------
                // A. PLATOS
                // -------------------------------------------------

                val resPlatos =
                    menuRepository.obtenerPlatos()

                if (
                    resPlatos.isSuccessful &&
                    !resPlatos.body().isNullOrEmpty()
                ) {

                    listaPlatos =
                        resPlatos.body()!!

                    Log.d(
                        "BACKEND_SIRAE",
                        "Platos recibidos: ${listaPlatos.size}"
                    )
                }

                // -------------------------------------------------
                // B. SECCIONES
                // -------------------------------------------------

                val resSecciones =
                    menuRepository.obtenerSeccionesMenu()

                if (
                    resSecciones.isSuccessful &&
                    !resSecciones.body().isNullOrEmpty()
                ) {

                    listaSecciones =
                        resSecciones.body()!!

                    Log.d(
                        "BACKEND_SIRAE",
                        "Secciones recibidas: ${listaSecciones.size}"
                    )
                }

                // -------------------------------------------------
                // C. DETALLES
                // -------------------------------------------------

                val resDetalles =
                    menuRepository.obtenerDetallePlatos()

                if (
                    resDetalles.isSuccessful &&
                    !resDetalles.body().isNullOrEmpty()
                ) {

                    listaDetalles =
                        resDetalles.body()!!

                    Log.d(
                        "BACKEND_SIRAE",
                        "Detalles recibidos: ${listaDetalles.size}"
                    )
                }

                // -------------------------------------------------
                // ACTUALIZAR INTERFAZ
                // -------------------------------------------------

                actualizarRuleta()

                actualizarInformacionSeccion()

                configurarMenusAnteriores()

            } catch (e: Exception) {

                Log.e(
                    "BACKEND_SIRAE",
                    "Error al conectar con Render: ${e.message}"
                )
            }
        }
    }

    // =========================================================
    // RULETA
    // =========================================================

    private fun configurarGiroRuleta() {

        var totalRotation = 0f

        var lastAngle = 0f

        binding.wheelContainer.setOnTouchListener { view, event ->

            val centerX =
                view.width / 2f

            val centerY =
                view.height / 2f

            val x = event.x

            val y = event.y

            when (event.action) {

                // -------------------------------------------------
                // INICIO DEL GIRO
                // -------------------------------------------------

                MotionEvent.ACTION_DOWN -> {

                    lastAngle =
                        Math.toDegrees(
                            atan2(
                                (y - centerY).toDouble(),
                                (x - centerX).toDouble()
                            )
                        ).toFloat()

                    true
                }

                // -------------------------------------------------
                // MOVIMIENTO
                // -------------------------------------------------

                MotionEvent.ACTION_MOVE -> {

                    val currentAngle =
                        Math.toDegrees(
                            atan2(
                                (y - centerY).toDouble(),
                                (x - centerX).toDouble()
                            )
                        ).toFloat()

                    var deltaAngle =
                        currentAngle - lastAngle

                    if (deltaAngle > 180f) {

                        deltaAngle -= 360f
                    }

                    if (deltaAngle < -180f) {

                        deltaAngle += 360f
                    }

                    totalRotation += deltaAngle

                    binding.wheelContainer.rotation =
                        totalRotation

                    lastAngle =
                        currentAngle

                    true
                }

                // -------------------------------------------------
                // FINAL DEL GIRO
                // -------------------------------------------------

                MotionEvent.ACTION_UP,
                MotionEvent.ACTION_CANCEL -> {

                    seleccionarSeccionPorRotacion(
                        totalRotation
                    )

                    true
                }

                else -> false
            }
        }
    }

    // =========================================================
    // DETERMINAR SECCIÓN
    // =========================================================

    private fun seleccionarSeccionPorRotacion(
        rotation: Float
    ) {

        val normalizedRotation =
            (rotation % 360f + 360f) % 360f

        /*
         * Tenemos 3 secciones.
         *
         * 360 / 3 = 120 grados
         */

        val sector =
            ((normalizedRotation + 60f) / 120f)
                .toInt() % 3

        seccionSeleccionada =
            sector

        actualizarInformacionSeccion()
    }

    // =========================================================
    // ACTUALIZAR RULETA
    // =========================================================

    private fun actualizarRuleta() {

        val desayuno =
            obtenerPlatoPorSeccion("Desayuno")

        val almuerzo =
            obtenerPlatoPorSeccion("Almuerzo")

        val merienda =
            obtenerPlatoPorSeccion("Merienda")

        // -----------------------------------------------------
        // DESAYUNO
        // -----------------------------------------------------

        desayuno?.let {

            binding.imgRuletaDesayuno.setImageResource(
                obtenerImagenPlato(it)
            )
        }

        // -----------------------------------------------------
        // ALMUERZO
        // -----------------------------------------------------

        almuerzo?.let {

            binding.imgRuletaAlmuerzo.setImageResource(
                obtenerImagenPlato(it)
            )
        }

        // -----------------------------------------------------
        // MERIENDA
        // -----------------------------------------------------

        merienda?.let {

            binding.imgRuletaMerienda.setImageResource(
                obtenerImagenPlato(it)
            )
        }
    }

    // =========================================================
    // MOSTRAR INFORMACIÓN DE LA SECCIÓN
    // =========================================================

    private fun actualizarInformacionSeccion() {

        val nombreSeccion =
            seccionesRuleta[seccionSeleccionada]

        binding.tvSeccionSeleccionada.text =
            nombreSeccion.uppercase()

        val plato =
            obtenerPlatoPorSeccion(
                nombreSeccion
            )

        if (plato != null) {

            platoActualSeleccionado =
                plato

            binding.tvPlatoSeleccionado.text =
                formatearNombrePlato(
                    plato.nombrePlato
                )

            binding.tvIngredientes.text =
                if (
                    !plato.componente.isNullOrEmpty()
                ) {

                    "• ${plato.componente} disponible"

                } else {

                    "• Toca para ver detalles"
                }

        } else {

            platoActualSeleccionado = null

            binding.tvPlatoSeleccionado.text =
                "Sin menú asignado"

            binding.tvIngredientes.text =
                "• No hay plato para esta sección"
        }
    }

    // =========================================================
    // BUSCAR PLATO POR SECCIÓN
    // =========================================================

    private fun obtenerPlatoPorSeccion(
        nombreSeccion: String
    ): PlatoResponse? {

        val seccion =
            listaSecciones.firstOrNull {

                it.nombreSeccion.equals(
                    nombreSeccion,
                    ignoreCase = true
                )
            }

        // -----------------------------------------------------
        // Primero usamos el ID real de la sección
        // -----------------------------------------------------

        if (seccion != null) {

            val platoPorId =
                listaPlatos.firstOrNull {

                    it.idSeccion ==
                            seccion.idSeccion
                }

            if (platoPorId != null) {

                return platoPorId
            }
        }

        // -----------------------------------------------------
        // Respaldo por nombre
        // -----------------------------------------------------

        return when {

            nombreSeccion.equals(
                "Desayuno",
                ignoreCase = true
            ) -> {

                listaPlatos.firstOrNull {

                    it.nombrePlato?.contains(
                        "café",
                        ignoreCase = true
                    ) == true ||
                            it.nombrePlato?.contains(
                                "chocolate",
                                ignoreCase = true
                            ) == true
                }
            }

            nombreSeccion.equals(
                "Almuerzo",
                ignoreCase = true
            ) -> {

                listaPlatos.firstOrNull {

                    it.nombrePlato?.contains(
                        "bandeja",
                        ignoreCase = true
                    ) == true
                }
            }

            nombreSeccion.equals(
                "Merienda",
                ignoreCase = true
            ) -> {

                listaPlatos.firstOrNull {

                    it.nombrePlato?.contains(
                        "pollo",
                        ignoreCase = true
                    ) == true ||
                            it.nombrePlato?.contains(
                                "valenciana",
                                ignoreCase = true
                            ) == true
                }
            }

            else -> null
        }
    }

    // =========================================================
    // IMAGEN DEL PLATO
    // =========================================================

    private fun obtenerImagenPlato(
        plato: PlatoResponse
    ): Int {

        val nombre =
            plato.nombrePlato
                ?.lowercase()
                ?: ""

        return when {

            nombre.contains("bandeja") ->
                R.drawable.bandeja_paisa

            nombre.contains("frijol") ->
                R.drawable.frijoles

            nombre.contains("chocolate") ->
                R.drawable.chocolate

            nombre.contains("huevo") ->
                R.drawable.huevo_perico

            nombre.contains("pollo") ->
                R.drawable.apanado

            nombre.contains("arroz") ->
                R.drawable.arroz_de_leche

            nombre.contains("café") ||
                    nombre.contains("cafe") ->
                R.drawable.apanado

            else ->
                R.drawable.frijoles
        }
    }

    // =========================================================
    // MENÚS ANTERIORES
    // =========================================================

    private fun configurarMenusAnteriores() {

        if (listaPlatos.isEmpty()) return

        val plato1 =
            listaPlatos.firstOrNull {

                it.nombrePlato?.contains(
                    "pollo",
                    ignoreCase = true
                ) == true

            } ?: listaPlatos.getOrNull(0)

        val plato2 =
            listaPlatos.firstOrNull {

                it.nombrePlato?.contains(
                    "bandeja",
                    ignoreCase = true
                ) == true

            } ?: listaPlatos.getOrNull(1)

        val plato3 =
            listaPlatos.firstOrNull {

                it.nombrePlato?.contains(
                    "valenciana",
                    ignoreCase = true
                ) == true ||

                        it.nombrePlato?.contains(
                            "lenteja",
                            ignoreCase = true
                        ) == true

            } ?: listaPlatos.getOrNull(2)
            ?: listaPlatos.lastOrNull()

        // -----------------------------------------------------
        // TARJETA 1
        // -----------------------------------------------------

        plato1?.let { p ->

            binding.tvPlatoAnterior1.text =
                formatearNombrePlato(
                    p.nombrePlato
                )

            binding.cardMenuAnterior1.setOnClickListener {

                mostrarDialogoPlato(p)
            }
        }

        // -----------------------------------------------------
        // TARJETA 2
        // -----------------------------------------------------

        plato2?.let { p ->

            binding.tvPlatoAnterior2.text =
                formatearNombrePlato(
                    p.nombrePlato
                )

            binding.imgMenuAnterior2.setImageResource(
                R.drawable.bandeja_paisa
            )

            binding.cardMenuAnterior2.setOnClickListener {

                mostrarDialogoPlato(p)
            }
        }

        // -----------------------------------------------------
        // TARJETA 3
        // -----------------------------------------------------

        plato3?.let { p ->

            binding.tvPlatoAnterior3.text =
                formatearNombrePlato(
                    p.nombrePlato
                )

            binding.cardMenuAnterior3.setOnClickListener {

                mostrarDialogoPlato(p)
            }
        }
    }

    // =========================================================
    // FORMATEAR NOMBRE
    // =========================================================

    private fun formatearNombrePlato(
        nombre: String?
    ): String {

        return nombre
            ?.split(" ")
            ?.joinToString(" ") { palabra ->

                palabra.replaceFirstChar {

                    if (it.isLowerCase())
                        it.titlecase()
                    else
                        it.toString()
                }
            }
            ?: "Plato del día"
    }

    // =========================================================
    // DIÁLOGO DEL PLATO
    // =========================================================

    private fun mostrarDialogoPlato(
        plato: PlatoResponse
    ) {

        platoActualSeleccionado =
            plato

        actualizarTarjetaPlato(
            plato
        )

        // -----------------------------------------------------
        // SECCIÓN
        // -----------------------------------------------------

        val seccion =
            listaSecciones.firstOrNull {

                it.idSeccion ==
                        plato.idSeccion
            }

        val nombreSeccion =
            seccion?.nombreSeccion
                ?: when (plato.idSeccion) {

                    1 -> "Desayuno"

                    2 -> "Almuerzo"

                    3 -> "Merienda"

                    else -> "Almuerzo"
                }

        // -----------------------------------------------------
        // DETALLE
        // -----------------------------------------------------

        val detalle =
            listaDetalles.firstOrNull {

                it.idPlato ==
                        plato.idPlato
            }

        val porcion =
            detalle?.porcionPorNino
                ?: "100g aprox."

        val totalPreparar =
            detalle?.totalAPreparar
                ?: "50"

        val unidad =
            detalle?.unidadTotal
                ?: "porciones"

        val estado =
            detalle?.estadoPreparacion
                ?: "Asignado"

        val componenteTipo =
            if (
                !plato.componente.isNullOrEmpty()
            ) {

                plato.componente

            } else {

                "Principio y Proteína"
            }

        // -----------------------------------------------------
        // VIEWBINDING MODAL
        // -----------------------------------------------------

        val dialogBinding =
            DialogDetallePlatoBinding.inflate(
                layoutInflater
            )

        val nombreFormateado =
            formatearNombrePlato(
                plato.nombrePlato
            )

        dialogBinding.tvTituloPlatoModal.text =
            nombreFormateado

        dialogBinding.tvSeccionModal.text =
            nombreSeccion

        dialogBinding.tvComponenteModal.text =
            componenteTipo

        dialogBinding.tvPorcionModal.text =
            porcion

        dialogBinding.tvTotalPrepararModal.text =
            "$totalPreparar $unidad"

        dialogBinding.tvBadgeEstadoModal.text =
            estado

        // -----------------------------------------------------
        // IMAGEN
        // -----------------------------------------------------

        dialogBinding.imgPlatoModal.setImageResource(
            obtenerImagenPlato(plato)
        )

        // -----------------------------------------------------
        // DIALOG
        // -----------------------------------------------------

        val dialog =
            MaterialAlertDialogBuilder(this)
                .setView(
                    dialogBinding.root
                )
                .create()

        dialog.window?.setBackgroundDrawableResource(
            android.R.color.transparent
        )

        // -----------------------------------------------------
        // CERRAR
        // -----------------------------------------------------

        dialogBinding.btnCerrarModal.setOnClickListener {

            dialog.dismiss()
        }

        // -----------------------------------------------------
        // COMPONENTES
        // -----------------------------------------------------

        dialogBinding.btnVerComponentesModal
            .setOnClickListener {

                dialog.dismiss()

                val intent =
                    Intent(
                        this,
                        AsignadasActivity::class.java
                    ).apply {

                        putExtra(
                            "id_plato",
                            plato.idPlato
                        )

                        putExtra(
                            "id_seccion",
                            plato.idSeccion ?: -1
                        )

                        putExtra(
                            "nombre_plato",
                            plato.nombrePlato
                        )
                    }

                startActivity(intent)
            }

        // -----------------------------------------------------
        // PREPARACIÓN
        // -----------------------------------------------------

        dialogBinding.btnOtroAleatorioModal
            .setOnClickListener {

                dialog.dismiss()

                val intent =
                    Intent(
                        this,
                        PreparacionActivity::class.java
                    ).apply {

                        putExtra(
                            "id_plato",
                            plato.idPlato
                        )

                        putExtra(
                            "nombre_plato",
                            plato.nombrePlato
                        )
                    }

                startActivity(intent)
            }

        dialog.show()
    }

    // =========================================================
    // ACTUALIZAR TARJETA
    // =========================================================

    private fun actualizarTarjetaPlato(
        plato: PlatoResponse
    ) {

        binding.tvPlatoSeleccionado.text =
            plato.nombrePlato
                ?: "Plato sin nombre"

        binding.tvIngredientes.text =
            if (
                !plato.componente.isNullOrEmpty()
            ) {

                "• ${plato.componente} disponible"

            } else {

                "• Toca para ver detalles"
            }
    }
}