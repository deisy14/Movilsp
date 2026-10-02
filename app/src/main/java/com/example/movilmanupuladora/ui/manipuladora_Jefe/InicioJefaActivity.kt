package com.example.movilmanupuladora.ui.manipuladora_Jefe

import com.example.movilmanupuladora.R

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.movilmanupuladora.data.api.RetrofitClient
import com.example.movilmanupuladora.data.repository.MenuRepository
import com.example.movilmanupuladora.databinding.ActivityInicioJefaBinding
import com.example.movilmanupuladora.utils.SessionManager
import kotlinx.coroutines.launch

class InicioJefaActivity : AppCompatActivity() {

    private lateinit var binding: ActivityInicioJefaBinding
    private lateinit var sessionManager: SessionManager
    private val menuRepository = MenuRepository(RetrofitClient.apiService)

    // =========================================================
    // JORNADA
    // =========================================================

    private var jornadaSeleccionada = Jornada.MANANA

    // =========================================================
    // PLATOS
    // =========================================================

    data class Plato(
        val id: Int? = null,
        val nombre: String,
        val componente: String? = null,
        val imagen: Int = R.drawable.pollo_guisado
    )

    private val platosManana = mutableListOf(
        Plato(id = 5, nombre = "Arroz con pollo", componente = "Pollo"),
        Plato(id = 6, nombre = "Café con pan", componente = "Desayuno"),
        Plato(id = 7, nombre = "Bandeja paisa", componente = "Frijoles y carne"),
        Plato(id = 10, nombre = "Carne asada", componente = "Proteína")
    )

    private val platosTarde = mutableListOf(
        Plato(id = 4, nombre = "Arroz a la valenciana", componente = "Arroz y verduras"),
        Plato(id = 2, nombre = "Arroz con Pollo Especial", componente = "Pollo especial"),
        Plato(id = 7, nombre = "Bandeja paisa", componente = "Carne y arroz"),
        Plato(id = 10, nombre = "Carne asada", componente = "Proteína")
    )

    private var posicionPlato = 0

    // =========================================================
    // ESTADOS
    // =========================================================

    private var platoSeleccionado = false

    private var preparacionesTotales = 4
    private var preparacionesCompletadas = 1
    private var preparacionesEnCurso = 1

    // =========================================================
    // ENUM JORNADA
    // =========================================================

    enum class Jornada {
        MANANA,
        TARDE
    }

    // =========================================================
    // ON CREATE
    // =========================================================

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityInicioJefaBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessionManager = SessionManager(this)
        configurarPantalla()
        configurarEventos()
        configurarNavegacionInferior()
        actualizarPantalla()
        cargarPlatosYMenusDelBackend()
    }

    override fun onResume() {
        super.onResume()
        actualizarResumen()
    }

    // =========================================================
    // CONFIGURACIÓN INICIAL
    // =========================================================

    private fun configurarPantalla() {

        val nombre = com.example.movilmanupuladora.utils.SessionManager(this).getUserName() ?: "Jefa"
        binding.txtSaludo.text = "Hola, $nombre"

        actualizarFecha()

        seleccionarJornada(Jornada.MANANA)

        actualizarPlato()

        actualizarResumen()

        actualizarEvolucion()

        actualizarMisPreparaciones()
    }

    // =========================================================
    // EVENTOS
    // =========================================================

    private fun configurarEventos() {

        // -----------------------------------------------------
        // NOTIFICACIONES
        // -----------------------------------------------------

        binding.btnNotificaciones.setOnClickListener {
            startActivity(Intent(this, PopupNotificacionesActivity::class.java))
        }

        // -----------------------------------------------------
        // RESUMEN / ASISTENCIA
        // -----------------------------------------------------

        binding.cardResumen.setOnClickListener {
            startActivity(Intent(this, AsistenciaJefaActivity::class.java))
        }


        // -----------------------------------------------------
        // INFORMACIÓN IA
        // -----------------------------------------------------

        binding.btnInfoIA.setOnClickListener {

            Toast.makeText(
                this,
                "La IA recomienda platos teniendo en cuenta la asistencia y los alimentos disponibles.",
                Toast.LENGTH_LONG
            ).show()
        }


        // -----------------------------------------------------
        // JORNADA MAÑANA
        // -----------------------------------------------------

        binding.btnManana.setOnClickListener {

            seleccionarJornada(Jornada.MANANA)

        }


        // -----------------------------------------------------
        // JORNADA TARDE
        // -----------------------------------------------------

        binding.btnTarde.setOnClickListener {

            seleccionarJornada(Jornada.TARDE)

        }


        // -----------------------------------------------------
        // PLATO ANTERIOR
        // -----------------------------------------------------

        binding.btnPlatoAnterior.setOnClickListener {

            cambiarPlato(-1)

        }


        // -----------------------------------------------------
        // PLATO SIGUIENTE
        // -----------------------------------------------------

        binding.btnPlatoSiguiente.setOnClickListener {

            cambiarPlato(1)

        }


        // -----------------------------------------------------
        // SELECCIONAR PLATO
        // -----------------------------------------------------

        binding.btnSeleccionarPlato.setOnClickListener {

            seleccionarPlato()

        }


        // -----------------------------------------------------
        // MIS PREPARACIONES
        // -----------------------------------------------------

        binding.cardMisPreparaciones.setOnClickListener {

            abrirMisPreparaciones()

        }


        // -----------------------------------------------------
        // CALENDARIO
        // -----------------------------------------------------

        binding.cardCalendario.setOnClickListener {

            abrirCalendario()

        }


        // -----------------------------------------------------
        // EVOLUCIÓN
        // -----------------------------------------------------

        binding.btnVerEvolucion.setOnClickListener {

            abrirEvolucion()

        }
    }

    // =========================================================
    // JORNADA
    // =========================================================

    private fun seleccionarJornada(jornada: Jornada) {

        jornadaSeleccionada = jornada

        posicionPlato = 0
        platoSeleccionado = false

        when (jornada) {

            Jornada.MANANA -> {

                binding.btnManana.setBackgroundResource(
                    R.drawable.bg_jornada_seleccionada
                )

                binding.btnManana.setTextColor(
                    getColor(R.color.blanco)
                )

                binding.btnTarde.setBackgroundResource(
                    R.drawable.bg_jornada_no_seleccionada
                )

                binding.btnTarde.setTextColor(
                    getColor(R.color.negro_principal)
                )

                binding.txtSubtituloIA.text =
                    "Plato recomendado para la mañana"

                txtJornadaMenu()

                actualizarPlato()
            }

            Jornada.TARDE -> {

                binding.btnTarde.setBackgroundResource(
                    R.drawable.bg_jornada_seleccionada
                )

                binding.btnTarde.setTextColor(
                    getColor(R.color.blanco)
                )

                binding.btnManana.setBackgroundResource(
                    R.drawable.bg_jornada_no_seleccionada
                )

                binding.btnManana.setTextColor(
                    getColor(R.color.negro_principal)
                )

                binding.txtSubtituloIA.text =
                    "Plato recomendado para la tarde"

                txtJornadaMenu()

                actualizarPlato()
            }
        }

        actualizarEstado()
    }

    // =========================================================
    // ACTUALIZAR TEXTO DE JORNADA
    // =========================================================

    private fun txtJornadaMenu() {

        when (jornadaSeleccionada) {

            Jornada.MANANA -> {

                binding.txtTituloJornada.text =
                    "Jornada de la mañana"

            }

            Jornada.TARDE -> {

                binding.txtTituloJornada.text =
                    "Jornada de la tarde"
            }
        }
    }

    // =========================================================
    // OBTENER PLATOS
    // =========================================================

    private fun obtenerPlatos(): List<Plato> {

        return when (jornadaSeleccionada) {

            Jornada.MANANA -> platosManana

            Jornada.TARDE -> platosTarde
        }
    }

    // =========================================================
    // ACTUALIZAR PLATO
    // =========================================================

    private fun actualizarPlato() {

        val platos = obtenerPlatos()

        if (platos.isEmpty()) {
            return
        }

        if (posicionPlato >= platos.size) {
            posicionPlato = 0
        }

        if (posicionPlato < 0) {
            posicionPlato = platos.lastIndex
        }

        val plato = platos[posicionPlato]

        binding.imgPlatoSugerido.setImageResource(
            plato.imagen
        )

        binding.txtNombrePlato.text =
            plato.nombre

        actualizarIndicador()

        actualizarEstado()
    }

    // =========================================================
    // CAMBIAR PLATO
    // =========================================================

    private fun cambiarPlato(direccion: Int) {

        val platos = obtenerPlatos()

        if (platos.isEmpty()) {
            return
        }

        posicionPlato += direccion

        if (posicionPlato > platos.lastIndex) {
            posicionPlato = 0
        }

        if (posicionPlato < 0) {
            posicionPlato = platos.lastIndex
        }

        platoSeleccionado = false

        actualizarPlato()
    }

    // =========================================================
    // INDICADOR
    // =========================================================

    private fun actualizarIndicador() {

        val platos = obtenerPlatos()

        if (platos.isEmpty()) {
            binding.txtIndicadorPlatos.text = ""
            return
        }

        val indicador = StringBuilder()

        platos.forEachIndexed { index, _ ->

            if (index == posicionPlato) {
                indicador.append("●")
            } else {
                indicador.append("○")
            }

            if (index != platos.lastIndex) {
                indicador.append(" ")
            }
        }

        binding.txtIndicadorPlatos.text =
            indicador.toString()
    }

    // =========================================================
    // SELECCIONAR PLATO
    // =========================================================

    private fun seleccionarPlato() {

        val platos = obtenerPlatos()

        if (platos.isEmpty()) {
            mostrarMensaje("No hay platos disponibles")
            return
        }

        val plato = platos[posicionPlato]

        platoSeleccionado = true
        sessionManager.savePlatoSeleccionado(plato.nombre, plato.id)

        binding.txtNombrePlato.text =
            plato.nombre

        binding.txtEstado.text =
            "Seleccionado"

        binding.txtEstado.setBackgroundResource(
            R.drawable.bg_icono_verde
        )

        binding.txtEstado.setTextColor(
            getColor(R.color.negro_principal)
        )

        binding.txtEstadoResumen.text =
            "Listo para preparar"

        binding.txtEstadoResumen.setTextColor(
            getColor(R.color.verde_prinpipal)
        )

        binding.btnSeleccionarPlato.text =
            "Plato seleccionado"

        mostrarMensaje(
            "${plato.nombre} seleccionado"
        )
    }

    // =========================================================
    // ESTADO
    // =========================================================

    private fun actualizarEstado() {

        if (platoSeleccionado) {

            binding.txtEstado.text =
                "Seleccionado"

            binding.txtEstadoResumen.text =
                "Listo para preparar"

            return
        }

        binding.txtEstado.text =
            "Pendiente"

        binding.txtEstado.setBackgroundResource(
            R.drawable.bg_estado
        )

        binding.txtEstadoResumen.text =
            "Pendiente"

        binding.txtEstadoResumen.setTextColor(
            getColor(R.color.rojo_principal)
        )

        binding.btnSeleccionarPlato.text =
            "Seleccionar plato"
    }

    // =========================================================
    // RESUMEN
    // =========================================================

    private fun actualizarResumen() {

        // Estos valores posteriormente pueden venir
        // de Firebase.

        val guardados = if (jornadaSeleccionada == Jornada.MANANA) {
            AsistenciaManager.obtenerManana(this)
        } else {
            AsistenciaManager.obtenerTarde(this)
        }
        val cantidadNinos = if (guardados > 0) guardados else 120

        binding.txtNinos.text =
            "$cantidadNinos niños"

        binding.txtPreparaciones.text =
            "$preparacionesTotales platos"

        binding.txtEstadoResumen.text =
            if (platoSeleccionado) {
                "Listo para preparar"
            } else {
                "Pendiente"
            }
    }

    // =========================================================
    // EVOLUCIÓN DEL ALMUERZO
    // =========================================================

    private fun actualizarEvolucion() {

        val pendientes =
            preparacionesTotales -
                    preparacionesCompletadas -
                    preparacionesEnCurso

        binding.txtPendientesEvolucion.text =
            pendientes.coerceAtLeast(0).toString()

        binding.txtEnPreparacionEvolucion.text =
            preparacionesEnCurso.toString()

        binding.txtCompletadasEvolucion.text =
            preparacionesCompletadas.toString()

        binding.txtDetalleEvolucion.text =
            "$preparacionesCompletadas de " +
                    "$preparacionesTotales preparaciones completadas"

        val progreso =
            preparacionesCompletadas.toFloat() /
                    preparacionesTotales.toFloat()

        actualizarBarraProgreso(progreso)

        binding.txtEstadoEvolucion.text =
            when {

                preparacionesCompletadas >= preparacionesTotales ->
                    "Preparación completada"

                preparacionesEnCurso > 0 ->
                    "Preparación en curso"

                else ->
                    "Preparación pendiente"
            }
    }

    // =========================================================
    // BARRA EVOLUCIÓN
    // =========================================================

    private fun actualizarBarraProgreso(
        progreso: Float
    ) {

        val porcentaje =
            progreso.coerceIn(0f, 1f)

        val restante =
            1f - porcentaje

        binding.progresoEvolucion.layoutParams =
            binding.progresoEvolucion.layoutParams.apply {

                width = 0

            }

        binding.progresoEvolucion.layoutParams =
            binding.progresoEvolucion.layoutParams

        val parent =
            binding.progresoEvolucion.parent

        if (parent is android.widget.LinearLayout) {

            val progresoView =
                binding.progresoEvolucion

            val restanteView =
                parent.getChildAt(1)

            val paramsProgreso =
                progresoView.layoutParams as android.widget.LinearLayout.LayoutParams

            val paramsRestante =
                restanteView.layoutParams as android.widget.LinearLayout.LayoutParams

            paramsProgreso.weight = porcentaje
            paramsRestante.weight = restante

            progresoView.layoutParams =
                paramsProgreso

            restanteView.layoutParams =
                paramsRestante
        }
    }

    // =========================================================
    // MIS PREPARACIONES
    // =========================================================

    private fun actualizarMisPreparaciones() {

        val pendientes =
            preparacionesTotales -
                    preparacionesCompletadas

        if (pendientes <= 0) {

            binding.txtMisPreparaciones.text =
                "Preparaciones completadas"

            binding.txtEstadoMisPreparaciones.text =
                "No hay preparaciones pendientes"

        } else {

            val platoActual =
                obtenerPlatos()[posicionPlato]

            binding.txtMisPreparaciones.text =
                platoActual.nombre

            binding.txtEstadoMisPreparaciones.text =
                if (preparacionesEnCurso > 0) {
                    "Preparación en curso"
                } else {
                    "Pendiente de preparar"
                }
        }

        val progreso =
            preparacionesCompletadas.toFloat() /
                    preparacionesTotales.toFloat()

        actualizarBarraMisPreparaciones(progreso)
    }

    // =========================================================
    // BARRA MIS PREPARACIONES
    // =========================================================

    private fun actualizarBarraMisPreparaciones(
        progreso: Float
    ) {

        val porcentaje =
            progreso.coerceIn(0f, 1f)

        val restante =
            1f - porcentaje

        val parent =
            binding.progresoMisPreparaciones.parent

        if (parent is android.widget.LinearLayout) {

            val progresoView =
                binding.progresoMisPreparaciones

            val restanteView =
                parent.getChildAt(1)

            val paramsProgreso =
                progresoView.layoutParams as android.widget.LinearLayout.LayoutParams

            val paramsRestante =
                restanteView.layoutParams as android.widget.LinearLayout.LayoutParams

            paramsProgreso.weight =
                if (porcentaje == 0f) 0.01f else porcentaje

            paramsRestante.weight =
                if (restante == 0f) 0.01f else restante

            progresoView.layoutParams =
                paramsProgreso

            restanteView.layoutParams =
                paramsRestante
        }
    }

    // =========================================================
    // FECHA
    // =========================================================

    private fun actualizarFecha() {

        val fecha =
            java.text.SimpleDateFormat(
                "EEEE, d 'de' MMMM",
                java.util.Locale("es", "CO")
            ).format(
                java.util.Date()
            )

        binding.txtFecha.text =
            "Hoy es $fecha"
    }

    // =========================================================
    // ACTUALIZAR TODA LA PANTALLA
    // =========================================================

    private fun actualizarPantalla() {

        actualizarPlato()

        actualizarResumen()

        actualizarEvolucion()

        actualizarMisPreparaciones()
    }

    // =========================================================
    // NAVEGACIÓN - MIS PREPARACIONES
    // =========================================================

    private fun abrirMisPreparaciones() {

        try {

            val intent =
                Intent(
                    this,
                    PreparacionEnCursoActivity::class.java
                )

            startActivity(intent)

        } catch (e: Exception) {

            mostrarMensaje(
                "Pantalla de preparaciones no disponible"
            )
        }
    }

    // =========================================================
    // NAVEGACIÓN - CALENDARIO
    // =========================================================

    private fun abrirCalendario() {

        try {

            val intent =
                Intent(
                    this,
                    CalendarioMenuActivity::class.java
                )

            startActivity(intent)

        } catch (e: Exception) {

            mostrarMensaje(
                "Pantalla de calendario no disponible"
            )
        }
    }

    // =========================================================
    // NAVEGACIÓN - EVOLUCIÓN
    // =========================================================

    private fun abrirEvolucion() {

        try {

            val intent =
                Intent(
                    this,
                    ResumenDiaActivity::class.java
                )

            startActivity(intent)

        } catch (e: Exception) {

            mostrarMensaje(
                "Detalle de evolución no disponible"
            )
        }
    }

    // =========================================================
    // MENSAJE
    // =========================================================

    private fun mostrarMensaje(
        mensaje: String
    ) {

        Toast.makeText(
            this,
            mensaje,
            Toast.LENGTH_SHORT
        ).show()
    }

    // =========================================================
    // CARGAR PLATOS Y MENÚS DEL BACKEND
    // =========================================================

    private fun cargarPlatosYMenusDelBackend() {
        lifecycleScope.launch {
            try {
                val platosRes = menuRepository.obtenerPlatos()
                if (platosRes.isSuccessful && !platosRes.body().isNullOrEmpty()) {
                    val lista = platosRes.body()!!
                    val tempManana = mutableListOf<Plato>()
                    val tempTarde = mutableListOf<Plato>()

                    for (p in lista) {
                        val nombre = p.nombrePlato ?: "Plato del día"
                        val plato = Plato(
                            id = p.idPlato,
                            nombre = nombre,
                            componente = p.componente
                        )
                        when (p.idSeccion) {
                            1 -> tempManana.add(plato)
                            3 -> tempTarde.add(plato)
                            else -> {
                                tempManana.add(plato)
                                tempTarde.add(plato)
                            }
                        }
                    }

                    if (tempManana.isNotEmpty()) {
                        platosManana.clear()
                        platosManana.addAll(tempManana)
                    }
                    if (tempTarde.isNotEmpty()) {
                        platosTarde.clear()
                        platosTarde.addAll(tempTarde)
                    }

                    actualizarPlato()
                }

                val menuRes = menuRepository.obtenerMenus()
                if (menuRes.isSuccessful && !menuRes.body().isNullOrEmpty()) {
                    val fechaHoy = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
                    val menuHoy = menuRes.body()!!.find { it.fecha == fechaHoy } ?: menuRes.body()!!.last()
                    menuHoy.informacion_nutricional?.let { info ->
                        if (info.isNotBlank()) {
                            binding.txtSubtituloIA.text = "Sugerencia IA: $info"
                        }
                    }
                }
            } catch (e: Exception) {
                // Conserva los platos precargados si hay fallo de red
            }
        }
    }
}
