package com.example.movilmanupuladora.ui.manipuladora_Jefe

import com.example.movilmanupuladora.R

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.widget.CompositePageTransformer
import androidx.viewpager2.widget.MarginPageTransformer
import androidx.viewpager2.widget.ViewPager2
import com.example.movilmanupuladora.R
import com.example.movilmanupuladora.data.api.RetrofitClient
import com.example.movilmanupuladora.data.repository.MenuRepository
import com.example.movilmanupuladora.databinding.ActivityInicioJefaBinding
import com.example.movilmanupuladora.utils.NavigationHelper
import com.example.movilmanupuladora.utils.SessionManager
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
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

    data class Plato(
        val id: Int? = null,
        val nombre: String,
        val componente: String? = null,
        val imagen: Int = R.drawable.pollo_guisado
    )

    private val platosManana = mutableListOf(
        Plato(id = 5, nombre = "Pollo Guisado Criollo", componente = "Pollo y Guiso", imagen = R.drawable.pollo_guisado),
        Plato(id = 6, nombre = "Café con Pan y Huevo", componente = "Desayuno", imagen = R.drawable.comida_desayuno),
        Plato(id = 7, nombre = "Bandeja Paisa Tradicional", componente = "Frijoles y carne", imagen = R.drawable.bandeja_paisa),
        Plato(id = 10, nombre = "Pasta con Carne Molida", componente = "Proteína", imagen = R.drawable.pasta_carne)
    )

    private val platosTarde = mutableListOf(
        Plato(id = 4, nombre = "Arroz a la Valenciana", componente = "Arroz y verduras", imagen = R.drawable.arroz_de_leche),
        Plato(id = 2, nombre = "Arroz con Pollo Especial", componente = "Pollo especial", imagen = R.drawable.arroz_pollo),
        Plato(id = 7, nombre = "Lentejas con Arroz", componente = "Leguminosa", imagen = R.drawable.lentejas_arroz)
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

    enum class Jornada {
        MANANA,
        TARDE
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityInicioJefaBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessionManager = SessionManager(this)

        configurarPantalla()
        configurarEventos()
        configurarViewPagerCarousel()
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
        val nombre = sessionManager.getUserName() ?: "Jefa de Cocina"
        binding.txtSaludo.text = "Hola, $nombre"

        val fecha = SimpleDateFormat("EEEE, d 'de' MMMM", Locale.forLanguageTag("es-CO")).format(Date())
        binding.txtFecha.text = "Hoy es $fecha"
        val nombre = com.example.movilmanupuladora.utils.SessionManager(this).getUserName() ?: "Jefa"
        binding.txtSaludo.text = "Hola, $nombre"

        actualizarFecha()

        seleccionarJornada(Jornada.MANANA)
        actualizarResumen()
    }

    // =========================================================
    // CAROUSEL CON VISTA PREVIA (IZQUIERDA, CENTRO, DERECHA)
    // =========================================================

    private fun configurarViewPagerCarousel() {
        val platosActuales = obtenerPlatos()
        val adapter = SugerenciaIaAdapter(platosActuales) { plato ->
            val intent = Intent(this, IniciarPreparacionActivity::class.java).apply {
                putExtra("nombre_menu", plato.nombre)
                putExtra("id_plato", plato.id ?: -1)
                putExtra("jornada", if (jornadaSeleccionada == Jornada.MANANA) "Mañana" else "Tarde")
            }
            startActivity(intent)
        }

        binding.viewPagerSugerenciasIA.adapter = adapter
        binding.viewPagerSugerenciasIA.offscreenPageLimit = 3

        val pageTransformer = CompositePageTransformer().apply {
            addTransformer(MarginPageTransformer((12 * resources.displayMetrics.density).toInt()))
            addTransformer { page, position ->
                val r = 1 - Math.abs(position)
                page.scaleY = 0.85f + r * 0.15f
                page.scaleX = 0.85f + r * 0.15f
                page.alpha = 0.6f + r * 0.4f
            }
        }

        binding.viewPagerSugerenciasIA.setPageTransformer(pageTransformer)

        binding.viewPagerSugerenciasIA.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                posicionPlato = position
                actualizarIndicadorPuntos(position, platosActuales.size)
            }
        })
    }

    private fun actualizarIndicadorPuntos(position: Int, total: Int) {
        if (total <= 0) {
            binding.txtIndicadorPlatos.text = ""
            return
        }

        val sb = StringBuilder()
        for (i in 0 until total) {
            if (i == position) {
                sb.append("●")
            } else {
                sb.append("○")
            }
            if (i < total - 1) sb.append("  ")
        }
        binding.txtIndicadorPlatos.text = sb.toString()
    }

    // =========================================================
    // EVENTOS Y CONEXIONES DE PANTALLAS
    // =========================================================

    private fun configurarEventos() {
        // Avatar Header -> Perfil Jefa
        binding.btnPerfilHeader.setOnClickListener {
            startActivity(Intent(this, PerfilJefaActivity::class.java))
        }

        // Campana Notificaciones -> Popup Notificaciones
        binding.btnNotificaciones.setOnClickListener {
            startActivity(Intent(this, PopupNotificacionesActivity::class.java))
        }

        // Asistencia y Niños -> AsistenciaJefaActivity
        binding.cardResumen.setOnClickListener {
            startActivity(Intent(this, AsistenciaJefaActivity::class.java))
        }

        // Mis Preparaciones -> Historial/Preparaciones Jefa
        binding.cardMisPreparaciones.setOnClickListener {
            startActivity(Intent(this, HistorialPreparacionesActivity::class.java))
        }

        // Calendario y Programación -> CalendarioMenuActivity
        binding.cardCalendario.setOnClickListener {
            startActivity(Intent(this, CalendarioMenuActivity::class.java))
        }

        // Evolución / Resumen -> ResumenDiaActivity
        binding.cardEvolucionAlmuerzo.setOnClickListener {
            startActivity(Intent(this, ResumenDiaActivity::class.java))
        }

        // Información IA
        binding.btnInfoIA.setOnClickListener {
            Toast.makeText(
                this,
                "La IA recomienda platos teniendo en cuenta la asistencia y los alimentos disponibles en cocina.",
                Toast.LENGTH_LONG
            ).show()
        }

        // Jornada Mañana / Tarde
        binding.btnManana.setOnClickListener { seleccionarJornada(Jornada.MANANA) }
        binding.btnTarde.setOnClickListener { seleccionarJornada(Jornada.TARDE) }
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

        // Botón "Seleccionar este plato"
        binding.btnSeleccionarPlato.setOnClickListener {
            val platos = obtenerPlatos()
            if (platos.isNotEmpty() && posicionPlato in platos.indices) {
                val plato = platos[posicionPlato]
                sessionManager.savePlatoSeleccionado(plato.nombre, plato.id)

                val intent = Intent(this, IniciarPreparacionActivity::class.java).apply {
                    putExtra("nombre_menu", plato.nombre)
                    putExtra("id_plato", plato.id ?: -1)
                    putExtra("jornada", if (jornadaSeleccionada == Jornada.MANANA) "Mañana" else "Tarde")
                }
                startActivity(intent)
            }
        }
    }

    private fun seleccionarJornada(jornada: Jornada) {
        jornadaSeleccionada = jornada
        posicionPlato = 0

        if (jornada == Jornada.MANANA) {
            binding.btnManana.setBackgroundResource(R.drawable.bg_pill_yellow)
            binding.btnManana.setTextColor(Color.parseColor("#7A5500"))

            binding.btnTarde.setBackgroundResource(R.drawable.bg_tag_plato)
            binding.btnTarde.setTextColor(Color.parseColor("#64748B"))

            binding.txtSubtituloIA.text = "Platos recomendados para la jornada de la mañana"
        } else {
            binding.btnTarde.setBackgroundResource(R.drawable.bg_pill_yellow)
            binding.btnTarde.setTextColor(Color.parseColor("#7A5500"))

            binding.btnManana.setBackgroundResource(R.drawable.bg_tag_plato)
            binding.btnManana.setTextColor(Color.parseColor("#64748B"))

            binding.txtSubtituloIA.text = "Platos recomendados para la jornada de la tarde"
        }

        configurarViewPagerCarousel()
    }

    private fun obtenerPlatos(): List<Plato> {
        return if (jornadaSeleccionada == Jornada.MANANA) platosManana else platosTarde
    }

    private fun actualizarResumen() {
        val guardados = if (jornadaSeleccionada == Jornada.MANANA) {
            AsistenciaManager.obtenerManana(this)
        } else {
            AsistenciaManager.obtenerTarde(this)
        }
        val cantidadNinos = if (guardados > 0) guardados else 120
        binding.txtNinos.text = "$cantidadNinos niños asistirán hoy"
    }

    private fun actualizarPantalla() {
        actualizarResumen()
    }

    private fun configurarNavegacionInferior() {
        NavigationHelper.setupBarraNavegacion(
            this,
            binding.barraNavegacion,
            NavigationHelper.Tab.INICIO
        )
    }

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
                        val plato = Plato(id = p.idPlato, nombre = nombre, componente = p.componente)
                        if (p.idSeccion == 1) tempManana.add(plato) else tempTarde.add(plato)
                    }

                    if (tempManana.isNotEmpty()) {
                        platosManana.clear()
                        platosManana.addAll(tempManana)
                    }
                    if (tempTarde.isNotEmpty()) {
                        platosTarde.clear()
                        platosTarde.addAll(tempTarde)
                    }

                    configurarViewPagerCarousel()
                }
            } catch (e: Exception) {
                // Conserva platos locales de respaldo
            }
        }
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
