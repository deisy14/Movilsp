package com.example.movilmanupuladora

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.movilmanupuladora.data.api.RetrofitClient
import com.example.movilmanupuladora.data.model.DetallePlato
import com.example.movilmanupuladora.data.model.PlatoResponse
import com.example.movilmanupuladora.data.model.SeccionMenu
import com.example.movilmanupuladora.data.repository.MenuRepository
import com.example.movilmanupuladora.databinding.ActivityMainBinding
import com.example.movilmanupuladora.databinding.DialogDetallePlatoBinding
import com.example.movilmanupuladora.ui.manipuladora.AvisosActivity
import com.example.movilmanupuladora.ui.manipuladora.ComponentesActivity
import com.example.movilmanupuladora.ui.manipuladora.InventarioActivity
import com.example.movilmanupuladora.ui.manipuladora.PerfilActivity
import com.example.movilmanupuladora.utils.SessionManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch
import kotlin.math.atan2

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val menuRepository = MenuRepository(RetrofitClient.apiService)

    // Datos traídos desde el Backend
    private var listaPlatos: List<PlatoResponse> = emptyList()
    private var listaSecciones: List<SeccionMenu> = emptyList()
    private var listaDetalles: List<DetallePlato> = emptyList()

    // Plato actualmente seleccionado
    private var platoActualSeleccionado: PlatoResponse? = null

    // Platos base de respaldo con los registros reales de la BD SIRAE
    private val platosBaseRespaldo = listOf(
        PlatoResponse(idPlato = 4, idSeccion = 2, nombrePlato = "Bandeja Paisa", componente = "carne molida, chicharron, aguacate, arroz, frijol"),
        PlatoResponse(idPlato = 1, idSeccion = 3, nombrePlato = "Arroz a la Valenciana", componente = "proteina"),
        PlatoResponse(idPlato = 2, idSeccion = 3, nombrePlato = "Arroz con Pollo", componente = "pollo"),
        PlatoResponse(idPlato = 3, idSeccion = 3, nombrePlato = "Arroz con Pollo Especial", componente = "Principal"),
        PlatoResponse(idPlato = 5, idSeccion = 4, nombrePlato = "Café con Pan", componente = "no se")
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

        // 1. Cargar el token de sesión guardado al hacer login
        SessionManager.getToken(this)

        // Inicializar datos base inmediatos
        listaPlatos = platosBaseRespaldo
        listaSecciones = seccionesRespaldo
        platoActualSeleccionado = listaPlatos.first()
        actualizarTarjetaPlato(platoActualSeleccionado!!)
        configurarMenusAnteriores()

        // 2. Cargar datos reales de la base de datos de Django en Render
        cargarDatosDesdeBackend()

        // 3. Control de giro manual de la ruleta
        configurarGiroRuleta()

        // 4. Botón del plato central seleccionado:
        // Al tocarlo, selecciona un plato ALEATORIAMENTE y muestra el modal elegante
        binding.cardPlatoSeleccionado.setOnClickListener {
            seleccionarPlatoAleatorioYMostrarDetalle()
        }

        // 5. Barra de navegación inferior
        binding.barraNavegacion.navInventario.setOnClickListener {
            startActivity(Intent(this, InventarioActivity::class.java))
        }

        binding.barraNavegacion.navAvisos.setOnClickListener {
            startActivity(Intent(this, AvisosActivity::class.java))
        }

        binding.barraNavegacion.navPerfil.setOnClickListener {
            startActivity(Intent(this, PerfilActivity::class.java))
        }
    }

    /**
     * Consume los endpoints de Django para platos, secciones y detalles.
     */
    private fun cargarDatosDesdeBackend() {
        lifecycleScope.launch {
            try {
                // A) Platos desde /api/platos/
                val resPlatos = menuRepository.obtenerPlatos()
                if (resPlatos.isSuccessful && !resPlatos.body().isNullOrEmpty()) {
                    listaPlatos = resPlatos.body()!!
                    platoActualSeleccionado = listaPlatos.first()
                    actualizarTarjetaPlato(platoActualSeleccionado!!)
                    configurarMenusAnteriores()
                    Log.d("BACKEND_SIRAE", "Platos recibidos de la BD: ${listaPlatos.size}")
                } else {
                    Log.d("BACKEND_SIRAE", "Código respuesta platos: ${resPlatos.code()}")
                }

                // B) Secciones de menú desde /api/secciones_menu/
                val resSecciones = menuRepository.obtenerSeccionesMenu()
                if (resSecciones.isSuccessful && !resSecciones.body().isNullOrEmpty()) {
                    listaSecciones = resSecciones.body()!!
                }

                // C) Detalle de platos desde /api/detalle_plato/
                val resDetalles = menuRepository.obtenerDetallePlatos()
                if (resDetalles.isSuccessful && !resDetalles.body().isNullOrEmpty()) {
                    listaDetalles = resDetalles.body()!!
                }

            } catch (e: Exception) {
                Log.e("BACKEND_SIRAE", "Error al conectar con Render: ${e.message}")
            }
        }
    }

    /**
     * Asigna platos reales de la base de datos a las tarjetas de "Menús anteriores"
     * y configura el click para abrir el diálogo con su receta/componentes
     */
    private fun configurarMenusAnteriores() {
        if (listaPlatos.isEmpty()) return

        val plato1 = listaPlatos.firstOrNull { it.nombrePlato?.contains("pollo", ignoreCase = true) == true }
            ?: listaPlatos.getOrNull(0)

        val plato2 = listaPlatos.firstOrNull { it.nombrePlato?.contains("bandeja", ignoreCase = true) == true }
            ?: listaPlatos.getOrNull(1)

        val plato3 = listaPlatos.firstOrNull { it.nombrePlato?.contains("valenciana", ignoreCase = true) == true || it.nombrePlato?.contains("lenteja", ignoreCase = true) == true }
            ?: listaPlatos.getOrNull(2) ?: listaPlatos.lastOrNull()

        // Tarjeta 1
        plato1?.let { p ->
            binding.tvPlatoAnterior1.text = formatearNombrePlato(p.nombrePlato)
            binding.cardMenuAnterior1.setOnClickListener {
                mostrarDialogoPlato(p)
            }
        }

        // Tarjeta 2
        plato2?.let { p ->
            binding.tvPlatoAnterior2.text = formatearNombrePlato(p.nombrePlato)
            binding.cardMenuAnterior2.setOnClickListener {
                mostrarDialogoPlato(p)
            }
        }

        // Tarjeta 3
        plato3?.let { p ->
            binding.tvPlatoAnterior3.text = formatearNombrePlato(p.nombrePlato)
            binding.cardMenuAnterior3.setOnClickListener {
                mostrarDialogoPlato(p)
            }
        }
    }

    private fun formatearNombrePlato(nombre: String?): String {
        return nombre?.split(" ")?.joinToString(" ") { palabra ->
            palabra.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        } ?: "Plato del día"
    }

    /**
     * Selecciona un plato aleatorio de la lista y despliega el diálogo visual
     */
    private fun seleccionarPlatoAleatorioYMostrarDetalle() {
        binding.cardPlatoSeleccionado.animate()
            .scaleX(1.04f).scaleY(1.04f).setDuration(80)
            .withEndAction {
                binding.cardPlatoSeleccionado.animate().scaleX(1f).scaleY(1f).setDuration(80).start()
            }.start()

        val platoAleatorio = listaPlatos.random()
        mostrarDialogoPlato(platoAleatorio)
    }

    /**
     * Muestra el modal elegante de SIRAE con los datos del plato, su sección, componentes y detalle
     */
    private fun mostrarDialogoPlato(plato: PlatoResponse) {
        platoActualSeleccionado = plato
        actualizarTarjetaPlato(plato)

        // 1. Buscar información relacionada (sección, detalle, componentes)
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
        val componenteTipo = if (!plato.componente.isNullOrEmpty()) plato.componente else "Principio y Proteína"

        // 2. Inflar el diseño personalizado con ViewBinding (DialogDetallePlatoBinding)
        val dialogBinding = DialogDetallePlatoBinding.inflate(layoutInflater)

        // Asignar los datos a la tarjeta
        val nombreFormateado = formatearNombrePlato(plato.nombrePlato)
        dialogBinding.tvTituloPlatoModal.text = nombreFormateado
        dialogBinding.tvSeccionModal.text = nombreSeccion
        dialogBinding.tvComponenteModal.text = componenteTipo
        dialogBinding.tvPorcionModal.text = porcion
        dialogBinding.tvTotalPrepararModal.text = "$totalPreparar $unidad"
        dialogBinding.tvBadgeEstadoModal.text = estado

        // Asignar imagen acorde al plato
        val imgRes = when {
            plato.nombrePlato?.contains("bandeja", ignoreCase = true) == true -> R.drawable.frijoles
            plato.nombrePlato?.contains("frijol", ignoreCase = true) == true -> R.drawable.frijoles
            plato.nombrePlato?.contains("chocolate", ignoreCase = true) == true -> R.drawable.chocolate
            plato.nombrePlato?.contains("huevo", ignoreCase = true) == true -> R.drawable.huevo_perico
            plato.nombrePlato?.contains("pollo", ignoreCase = true) == true -> R.drawable.apanado
            plato.nombrePlato?.contains("arroz", ignoreCase = true) == true -> R.drawable.arroz_de_leche
            else -> R.drawable.frijoles
        }
        dialogBinding.imgPlatoModal.setImageResource(imgRes)

        // Crear el diálogo con fondo transparente para destacar la tarjeta con bordes dorados
        val dialog = MaterialAlertDialogBuilder(this)
            .setView(dialogBinding.root)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        // Acción: Botón Cerrar (X)
        dialogBinding.btnCerrarModal.setOnClickListener {
            dialog.dismiss()
        }

        // Acción: Botón "Ver componentes e ingredientes" -> Lleva a ComponentesActivity con este plato
        dialogBinding.btnVerComponentesModal.setOnClickListener {
            dialog.dismiss()
            val intent = Intent(this, ComponentesActivity::class.java).apply {
                putExtra("id_plato", plato.idPlato)
                putExtra("id_seccion", plato.idSeccion ?: -1)
                putExtra("nombre_plato", plato.nombrePlato)
            }
            startActivity(intent)
        }

        // Acción: Botón "Elegir otro plato al azar"
        dialogBinding.btnOtroAleatorioModal.setOnClickListener {
            dialog.dismiss()
            seleccionarPlatoAleatorioYMostrarDetalle()
        }

        dialog.show()
    }

    private fun actualizarTarjetaPlato(plato: PlatoResponse) {
        binding.tvPlatoSeleccionado.text = plato.nombrePlato ?: "Plato sin nombre"
        val componenteTexto = if (!plato.componente.isNullOrEmpty()) {
            "• ${plato.componente} disponible"
        } else {
            "• Toca para ver detalles"
        }
        binding.tvIngredientes.text = componenteTexto
    }

    private fun configurarGiroRuleta() {
        var totalRotation = 0f
        var lastAngle = 0f

        binding.wheelContainer.setOnTouchListener { view, event ->
            val centerX = view.width / 2f
            val centerY = view.height / 2f
            val x = event.x
            val y = event.y

            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    lastAngle = Math.toDegrees(atan2((y - centerY).toDouble(), (x - centerX).toDouble())).toFloat()
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val currentAngle = Math.toDegrees(atan2((y - centerY).toDouble(), (x - centerX).toDouble())).toFloat()
                    var deltaAngle = currentAngle - lastAngle

                    if (deltaAngle > 180f) deltaAngle -= 360f
                    if (deltaAngle < -180f) deltaAngle += 360f

                    totalRotation += deltaAngle
                    binding.wheelContainer.rotation = totalRotation
                    lastAngle = currentAngle
                    true
                }
                MotionEvent.ACTION_UP,
                MotionEvent.ACTION_CANCEL -> {
                    if (listaPlatos.isNotEmpty()) {
                        val normalizedRotation = (totalRotation % 360f + 360f) % 360f
                        val index = ((normalizedRotation + 45f) / 90f).toInt() % listaPlatos.size
                        val platoActual = listaPlatos[index]
                        platoActualSeleccionado = platoActual
                        actualizarTarjetaPlato(platoActual)
                    }
                    true
                }
                else -> false
            }
        }
    }
}