package com.example.movilmanupuladora.ui.manipuladora_Jefe

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.CompositePageTransformer
import androidx.viewpager2.widget.ViewPager2
import com.example.movilmanupuladora.R
import com.example.movilmanupuladora.data.api.RetrofitClient
import com.example.movilmanupuladora.data.repository.MenuRepository
import com.example.movilmanupuladora.databinding.ActivityInicioJefaBinding
import com.example.movilmanupuladora.databinding.PopupNotificacionesBinding
import com.example.movilmanupuladora.utils.NavigationHelperJefa
import com.example.movilmanupuladora.utils.SessionManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class InicioJefaActivity : AppCompatActivity() {

    private lateinit var binding: ActivityInicioJefaBinding
    private lateinit var sessionManager: SessionManager
    private val menuRepository = MenuRepository(RetrofitClient.apiService)

    private var jornadaSeleccionada = Jornada.MANANA

    data class Plato(
        val id: Int? = null,
        val nombre: String,
        val componente: String? = null,
        val imagen: Int = R.drawable.pollo_guisado
    )

    private val platosManana = mutableListOf(
        Plato(id = 5, nombre = "arroz chino", componente = "Arroz", imagen = R.drawable.pollo_guisado),
        Plato(id = 6, nombre = "cafe con pan", componente = "Desayuno", imagen = R.drawable.pollo_guisado),
        Plato(id = 7, nombre = "polloooo", componente = "Proteína", imagen = R.drawable.pollo_guisado)
    )

    private val platosTarde = mutableListOf(
        Plato(id = 4, nombre = "Arroz a la Valenciana", componente = "Arroz y verduras", imagen = R.drawable.arroz_de_leche),
        Plato(id = 2, nombre = "Arroz con Pollo Especial", componente = "Pollo especial", imagen = R.drawable.arroz_pollo),
        Plato(id = 7, nombre = "Lentejas con Arroz", componente = "Leguminosa", imagen = R.drawable.lentejas_arroz)
    )

    private var posicionPlato = 0

    enum class Jornada {
        MANANA,
        TARDE
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityInicioJefaBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
            insets
        }

        sessionManager = SessionManager(this)

        configurarPantalla()
        configurarEventos()
        configurarViewPagerCarousel()
        configurarMenuDisponiblePAE()
        configurarNavegacionInferior()

        actualizarPantalla()
        cargarPlatosYMenusDelBackend()
        cargarAsistenciaDelBackend()
    }

    override fun onResume() {
        super.onResume()
        actualizarResumen()
        cargarAsistenciaDelBackend()
    }

    private fun configurarPantalla() {
        val nombre = sessionManager.getUserName() ?: "Jefa de Cocina"
        binding.txtSaludo.text = "Hola, $nombre"

        val fecha = SimpleDateFormat("EEEE, d 'de' MMMM", Locale.forLanguageTag("es-CO")).format(Date())
        binding.txtFecha.text = "Hoy es $fecha"

        seleccionarJornada(Jornada.MANANA)
        actualizarResumen()
    }

    private fun configurarViewPagerCarousel() {
        val platosActuales = obtenerPlatos()
        val adapter = SugerenciaIaAdapter(platosActuales) { plato ->
            val intent = Intent(this, IniciarPreparacionActivity::class.java).apply {
                putExtra("nombre_menu", plato.nombre)
                putExtra("id_plato", plato.id ?: -1)
                putExtra(
                    "jornada",
                    if (jornadaSeleccionada == Jornada.MANANA) "Mañana" else "Tarde"
                )
            }
            startActivity(intent)
        }

        binding.viewPagerSugerenciasIA.adapter = adapter
        binding.viewPagerSugerenciasIA.offscreenPageLimit = 3

        // Desactivar recortes en el RecyclerView interno de ViewPager2
        (binding.viewPagerSugerenciasIA.getChildAt(0) as? RecyclerView)?.apply {
            clipChildren = false
            clipToPadding = false
        }

        val pageTransformer = CompositePageTransformer().apply {
            addTransformer { page, position ->
                val absPosition = Math.abs(position)

                // 1. Escala: La tarjeta principal seleccionada (centro, position = 0) destaca al 108%, laterales al 70%
                val scale = 0.70f + (1.08f - 0.70f) * (1.0f - Math.min(1.0f, absPosition))
                page.scaleX = scale
                page.scaleY = scale

                // 2. Opacidad: La seleccionada al 100%, laterales al 80%
                page.alpha = 0.80f + (1.0f - 0.80f) * (1.0f - Math.min(1.0f, absPosition))

                // 3. Elevación Z: La tarjeta central seleccionada se ubica SIEMPRE POR ENCIMA DE TODAS
                page.translationZ = (1.0f - Math.min(1.0f, absPosition)) * 30f

                // 4. Traslape Horizontal: Desplaza los discos laterales hacia adentro para que queden DETRÁS
                val overlap = page.width * 0.32f
                page.translationX = -position * overlap
            }
        }

        binding.viewPagerSugerenciasIA.setPageTransformer(pageTransformer)

        binding.viewPagerSugerenciasIA.registerOnPageChangeCallback(object :
            ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                posicionPlato = position
                actualizarIndicadorPuntos(position, platosActuales.size)
            }
        })

        val initialPos = if (platosActuales.size >= 3) 1 else 0
        binding.viewPagerSugerenciasIA.setCurrentItem(initialPos, false)
        posicionPlato = initialPos
        actualizarIndicadorPuntos(initialPos, platosActuales.size)
    }

    private fun configurarMenuDisponiblePAE() {
        val platosActuales = obtenerPlatos()
        val adapter = MenuDisponibleAdapter(platosActuales) { plato ->
            val intent = Intent(this, IniciarPreparacionActivity::class.java).apply {
                putExtra("nombre_menu", plato.nombre)
                putExtra("id_plato", plato.id ?: -1)
                putExtra("jornada", if (jornadaSeleccionada == Jornada.MANANA) "Mañana" else "Tarde")
            }
            startActivity(intent)
        }
        binding.recyclerMenuDisponiblePAE.adapter = adapter
        binding.recyclerMenuDisponiblePAE.layoutManager = LinearLayoutManager(this, RecyclerView.HORIZONTAL, false)

        binding.btnVerTodosPlatos.setOnClickListener {
            startActivity(Intent(this, TodosLosPlatosActivity::class.java))
        }
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

    private fun configurarEventos() {
        binding.btnPerfilHeader.setOnClickListener {
            startActivity(Intent(this, PerfilJefaActivity::class.java))
        }

        binding.btnNotificaciones.setOnClickListener {
            mostrarPopupNotificaciones()
        }

        binding.cardResumen.setOnClickListener {
            startActivity(Intent(this, AsistenciaJefaActivity::class.java))
        }

        binding.cardMisPreparaciones.setOnClickListener {
            startActivity(Intent(this, HistorialPreparacionesActivity::class.java))
        }

        binding.cardCalendario.setOnClickListener {
            startActivity(Intent(this, CalendarioMenuActivity::class.java))
        }

        binding.cardEvolucionAlmuerzo.setOnClickListener {
            startActivity(Intent(this, ResumenDiaActivity::class.java))
        }

        binding.btnInfoIA.setOnClickListener {
            Toast.makeText(
                this,
                "La IA recomienda platos teniendo en cuenta la asistencia y los alimentos disponibles en cocina.",
                Toast.LENGTH_LONG
            ).show()
        }

        binding.btnPlatoAnterior.setOnClickListener {
            val current = binding.viewPagerSugerenciasIA.currentItem
            val count = obtenerPlatos().size
            if (count > 0) {
                binding.viewPagerSugerenciasIA.currentItem = if (current > 0) current - 1 else count - 1
            }
        }

        binding.btnPlatoSiguiente.setOnClickListener {
            val current = binding.viewPagerSugerenciasIA.currentItem
            val count = obtenerPlatos().size
            if (count > 0) {
                binding.viewPagerSugerenciasIA.currentItem = if (current < count - 1) current + 1 else 0
            }
        }

        binding.btnManana.setOnClickListener { seleccionarJornada(Jornada.MANANA) }
        binding.btnTarde.setOnClickListener { seleccionarJornada(Jornada.TARDE) }

        binding.btnSeleccionarPlato.setOnClickListener {
            val platos = obtenerPlatos()
            if (platos.isNotEmpty() && posicionPlato in platos.indices) {
                val plato = platos[posicionPlato]
                sessionManager.savePlatoSeleccionado(plato.nombre, plato.id)

                val intent = Intent(this, IniciarPreparacionActivity::class.java).apply {
                    putExtra("nombre_menu", plato.nombre)
                    putExtra("id_plato", plato.id ?: -1)
                    putExtra(
                        "jornada",
                        if (jornadaSeleccionada == Jornada.MANANA) "Mañana" else "Tarde"
                    )
                }
                startActivity(intent)
            }
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
            startActivity(Intent(this, HistorialPreparacionesActivity::class.java))
        }

        popupBinding.notificacionInventario.setOnClickListener {
            dialog.dismiss()
            startActivity(Intent(this, InventarioJefaActivity::class.java))
        }

        popupBinding.notificacionAsistencia.setOnClickListener {
            dialog.dismiss()
            startActivity(Intent(this, AsistenciaJefaActivity::class.java))
        }

        popupBinding.btnVerTodasNotificaciones.setOnClickListener {
            dialog.dismiss()
            Toast.makeText(this, "Todas las notificaciones al día", Toast.LENGTH_SHORT).show()
        }

        dialog.show()
    }

    private fun seleccionarJornada(jornada: Jornada) {
        jornadaSeleccionada = jornada
        posicionPlato = 0

        if (jornada == Jornada.MANANA) {
            binding.btnManana.setBackgroundResource(R.drawable.bg_pill_yellow)
            binding.btnManana.setTextColor(Color.parseColor("#7A5500"))

            binding.btnTarde.setBackgroundResource(R.drawable.bg_tag_plato)
            binding.txtSubtituloIA.text = "Platos recomendados para la jornada de la mañana"
        } else {
            binding.btnTarde.setBackgroundResource(R.drawable.bg_pill_yellow)
            binding.btnTarde.setTextColor(Color.parseColor("#7A5500"))

            binding.btnManana.setBackgroundResource(R.drawable.bg_tag_plato)
            binding.txtSubtituloIA.text = "Platos recomendados para la jornada de la tarde"
        }

        configurarViewPagerCarousel()
        configurarMenuDisponiblePAE()
        actualizarResumen()
    }

    private fun obtenerPlatos(): List<Plato> {
        return if (jornadaSeleccionada == Jornada.MANANA) platosManana else platosTarde
    }

    private fun actualizarResumen() {
        val p = AsistenciaManager.obtenerPrimaria(this)
        val s = AsistenciaManager.obtenerSecundaria(this)
        val suma = p + s
        val guardados = if (suma > 0) {
            suma
        } else if (jornadaSeleccionada == Jornada.MANANA) {
            AsistenciaManager.obtenerManana(this)
        } else {
            AsistenciaManager.obtenerTarde(this)
        }

        binding.txtNinos.text = if (guardados > 0) {
            "$guardados niños asistirán hoy (Primaria: $p | Secundaria: $s)"
        } else {
            "Asistencia de hoy pendiente de registro"
        }
    }

    private fun cargarAsistenciaDelBackend() {
        lifecycleScope.launch {
            try {
                sessionManager.fetchAuthToken()
                val fechaHoy = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

                val gradosRes = RetrofitClient.apiService.obtenerGrados()
                val listaGrados = gradosRes.body().orEmpty()
                val idPrimaria = listaGrados.find { it.nombreGrado.contains("primaria", ignoreCase = true) }?.idGrado ?: 16
                val idSecundaria = listaGrados.find { it.nombreGrado.contains("secundaria", ignoreCase = true) }?.idGrado ?: 19

                val asistenciasRes = RetrofitClient.apiService.obtenerAsistenciaDiaria()
                if (asistenciasRes.isSuccessful && !asistenciasRes.body().isNullOrEmpty()) {
                    val lista = asistenciasRes.body()!!
                    var asistenciasHoy = lista.filter { it.fecha.trim().startsWith(fechaHoy) }
                    if (asistenciasHoy.isEmpty()) {
                        val fechaMasReciente = lista.maxByOrNull { it.idAsistencia ?: 0 }?.fecha?.trim()?.take(10)
                        if (!fechaMasReciente.isNullOrEmpty()) {
                            asistenciasHoy = lista.filter { it.fecha.trim().startsWith(fechaMasReciente) }
                        }
                    }

                    val regPrimaria = asistenciasHoy.filter { it.idGrado == idPrimaria || it.idGrado == 16 }.maxByOrNull { it.idAsistencia ?: 0 }
                    val regSecundaria = asistenciasHoy.filter { it.idGrado == idSecundaria || it.idGrado == 19 }.maxByOrNull { it.idAsistencia ?: 0 }

                    var p = regPrimaria?.ninosPresentes ?: AsistenciaManager.obtenerPrimaria(this@InicioJefaActivity)
                    var s = regSecundaria?.ninosPresentes ?: AsistenciaManager.obtenerSecundaria(this@InicioJefaActivity)

                    if (regPrimaria == null && regSecundaria == null && asistenciasHoy.isNotEmpty()) {
                        val regLegacy = asistenciasHoy.maxByOrNull { it.idAsistencia ?: 0 }!!
                        val tot = regLegacy.ninosPresentes
                        p = (tot * 0.6).toInt()
                        s = tot - p
                    }

                    val total = p + s
                    AsistenciaManager.guardarPrimaria(this@InicioJefaActivity, p)
                    AsistenciaManager.guardarSecundaria(this@InicioJefaActivity, s)
                    AsistenciaManager.guardarManana(this@InicioJefaActivity, total)

                    actualizarResumen()
                }
            } catch (e: Exception) {
                // Si no hay conexión, se mantiene el resumen local
            }
        }
    }

    private fun actualizarPantalla() {
        actualizarResumen()
    }

    private fun configurarNavegacionInferior() {
        NavigationHelperJefa.setupBarraNavegacionJefa(
            this,
            binding.barraNavegacionJefa,
            NavigationHelperJefa.TabJefa.INICIO
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
                    configurarMenuDisponiblePAE()
                }
            } catch (e: Exception) {
                // Conserva platos locales de respaldo
            }
        }
    }
}