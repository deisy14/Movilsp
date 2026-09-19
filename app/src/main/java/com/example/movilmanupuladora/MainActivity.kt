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
import com.example.movilmanupuladora.ui.manipuladora.activity_componentes
import com.example.movilmanupuladora.ui.manipuladora.activity_perfil
import com.example.movilmanupuladora.ui.manipuladora.avisos_Activity
import com.example.movilmanupuladora.ui.manipuladora.inventario_activity
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

    // Platos base de respaldo (para que responda de inmediato mientras Render despierta de reposo)
    private val platosBaseRespaldo = listOf(
        PlatoResponse(idPlato = 1, idSeccion = 2, nombrePlato = "Sopa de Frijoles", componente = "Principio"),
        PlatoResponse(idPlato = 2, idSeccion = 2, nombrePlato = "Arroz con Pollo", componente = "Proteína"),
        PlatoResponse(idPlato = 3, idSeccion = 2, nombrePlato = "Carne Molida Guisada", componente = "Proteína"),
        PlatoResponse(idPlato = 4, idSeccion = 2, nombrePlato = "Lentejas Caseras", componente = "Principio"),
        PlatoResponse(idPlato = 5, idSeccion = 1, nombrePlato = "Chocolate Caliente", componente = "Bebida"),
        PlatoResponse(idPlato = 6, idSeccion = 1, nombrePlato = "Huevos Pericos", componente = "Proteína"),
        PlatoResponse(idPlato = 7, idSeccion = 3, nombrePlato = "Yogurt con Galleta", componente = "Refrigerio")
    )

    private val seccionesRespaldo = listOf(
        SeccionMenu(idSeccion = 1, idJornada = 1, nombreSeccion = "Desayuno"),
        SeccionMenu(idSeccion = 2, idJornada = 1, nombreSeccion = "Almuerzo"),
        SeccionMenu(idSeccion = 3, idJornada = 1, nombreSeccion = "Refrigerio")
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

        // 2. Cargar datos reales de la base de datos de Django en Render
        cargarDatosDesdeBackend()

        // 3. Control de giro manual de la ruleta
        configurarGiroRuleta()

        // 4. Botón "Sopa de Frijoles" (cardPlatoSeleccionado):
        // Al tocarlo, selecciona un plato ALEATORIAMENTE y muestra el modal personalizado elegante
        binding.cardPlatoSeleccionado.setOnClickListener {
            seleccionarPlatoAleatorioYMostrarDetalle()
        }

        // 5. Barra de navegación inferior
        binding.barraNavegacion.navInventario.setOnClickListener {
            startActivity(Intent(this, inventario_activity::class.java))
        }

        binding.barraNavegacion.navAvisos.setOnClickListener {
            startActivity(Intent(this, avisos_Activity::class.java))
        }

        binding.barraNavegacion.navPerfil.setOnClickListener {
            startActivity(Intent(this, activity_perfil::class.java))
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
     * Selecciona un plato aleatorio de la lista y despliega el diseño visual personalizado
     */
    private fun seleccionarPlatoAleatorioYMostrarDetalle() {
        // Animación de pulsación táctil
        binding.cardPlatoSeleccionado.animate()
            .scaleX(1.04f).scaleY(1.04f).setDuration(80)
            .withEndAction {
                binding.cardPlatoSeleccionado.animate().scaleX(1f).scaleY(1f).setDuration(80).start()
            }.start()

        // 1. Escoger plato ALEATORIO
        val platoAleatorio = listaPlatos.random()
        platoActualSeleccionado = platoAleatorio
        actualizarTarjetaPlato(platoAleatorio)

        // 2. Buscar información relacionada (sección, detalle, componente)
        val seccion = listaSecciones.firstOrNull { it.idSeccion == platoAleatorio.idSeccion }
        val nombreSeccion = seccion?.nombreSeccion ?: "Almuerzo"

        val detalle = listaDetalles.firstOrNull { it.idPlato == platoAleatorio.idPlato }
        val porcion = detalle?.porcionPorNino ?: "100g aprox."
        val totalPreparar = detalle?.totalAPreparar ?: "50"
        val unidad = detalle?.unidadTotal ?: "porciones"
        val estado = detalle?.estadoPreparacion ?: "Asignado"
        val componenteTipo = platoAleatorio.componente ?: "Principio"

        // 3. Inflar el diseño personalizado con ViewBinding (DialogDetallePlatoBinding)
        val dialogBinding = DialogDetallePlatoBinding.inflate(layoutInflater)

        // Asignar los datos del backend a las vistas del diseño
        dialogBinding.tvTituloPlatoModal.text = platoAleatorio.nombrePlato ?: "Plato sin nombre"
        dialogBinding.tvSeccionModal.text = nombreSeccion
        dialogBinding.tvComponenteModal.text = componenteTipo
        dialogBinding.tvPorcionModal.text = porcion
        dialogBinding.tvTotalPrepararModal.text = "$totalPreparar $unidad"
        dialogBinding.tvBadgeEstadoModal.text = estado

        // Crear el diálogo con fondo transparente para destacar la tarjeta con bordes dorados
        val dialog = MaterialAlertDialogBuilder(this)
            .setView(dialogBinding.root)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        // Acción: Botón Cerrar (X)
        dialogBinding.btnCerrarModal.setOnClickListener {
            dialog.dismiss()
        }

        // Acción: Botón "Ver componentes e ingredientes"
        dialogBinding.btnVerComponentesModal.setOnClickListener {
            dialog.dismiss()
            val intent = Intent(this, activity_componentes::class.java).apply {
                putExtra("id_plato", platoAleatorio.idPlato)
                putExtra("id_seccion", platoAleatorio.idSeccion ?: -1)
                putExtra("nombre_plato", platoAleatorio.nombrePlato)
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