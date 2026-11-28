package com.example.movilmanupuladora.ui.manipuladora

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import com.example.movilmanupuladora.R
import com.example.movilmanupuladora.databinding.ActivityAsignadasBinding
import com.example.movilmanupuladora.databinding.DialogDetalleAsignacionBinding
import com.example.movilmanupuladora.ui.manipuladora_Jefe.AsistenciaManager
import com.example.movilmanupuladora.ui.manipuladora_Jefe.InicioJefaActivity
import com.example.movilmanupuladora.ui.manipuladora_Jefe.ManipuladorasManager
import com.example.movilmanupuladora.ui.manipuladora_Jefe.PreparacionEstadoManager
import com.example.movilmanupuladora.utils.NavigationHelper
import com.example.movilmanupuladora.utils.SessionManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AsignadasActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAsignadasBinding
    private lateinit var sessionMgr: SessionManager

    private var idPlato: Int = -1
    private var nombrePlato: String = "Menú pendiente por la Jefa"
    private var infoNutricionalMenu: String? = null
    private var nivelNutricionalPrimaria: Boolean = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityAsignadasBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessionMgr = SessionManager(this)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
            insets
        }

        NavigationHelper.setupStickyTopBar(this, binding.root, lifecycleScope)

        val platoJefa = sessionMgr.getPlatoJefa(this)
        nombrePlato = if (intent.hasExtra("nombre_plato")) {
            intent.getStringExtra("nombre_plato") ?: platoJefa.takeIf { it.isNotBlank() } ?: "Menú pendiente por la Jefa"
        } else {
            platoJefa.takeIf { it.isNotBlank() } ?: "Menú pendiente por la Jefa"
        }
        idPlato = intent.getIntExtra("id_plato", sessionMgr.getPlatoJefaId(this))

        configurarEncabezadoYPlato()
        configurarAcciones()
        configurarConmutadorNutricional()
        configurarListaOperarias()
        configurarNavegacion()

        sincronizarConBackend()
        cargarDatosRealesAsignadas()
    }

    override fun onResume() {
        super.onResume()
        sincronizarConBackend()
        cargarDatosRealesAsignadas()
    }

    private fun sincronizarConBackend() {
        lifecycleScope.launch {
            try {
                com.example.movilmanupuladora.utils.PreparacionSyncManager.sincronizarDesdeBackend(this@AsignadasActivity)
                val platoActualizado = sessionMgr.getPlatoJefa(this@AsignadasActivity)
                if (!intent.hasExtra("nombre_plato") || intent.getStringExtra("nombre_plato").isNullOrBlank()) {
                    nombrePlato = platoActualizado
                }
                idPlato = sessionMgr.getPlatoJefaId(this@AsignadasActivity)
                configurarEncabezadoYPlato()
                configurarListaOperarias()
            } catch (_: Exception) { }
        }
    }

    private fun limpiarNombreMenu(raw: String): String {
        return raw.trim()
            .replace(Regex("(?i)^men[uú]\\s*(?:n[°o.]?\\s*)?\\d*\\s*:\\s*"), "")
            .trim()
            .ifBlank { raw.trim() }
    }

    private fun configurarEncabezadoYPlato() {
        val fechaHoy = SimpleDateFormat("EEEE / d / MMM / yyyy", Locale.forLanguageTag("es-CO"))
            .format(Date())
            .uppercase()
        binding.tvFechaAsignadas.text = fechaHoy

        val nombreLimpio = limpiarNombreMenu(nombrePlato)
        val nombreFormateado = nombreLimpio.split(" ").joinToString(" ") { palabra ->
            palabra.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        }
        binding.tvNombreMiPlato.text = nombreFormateado

        val sedeNombre = sessionMgr.getSedeNombre()?.takeIf { it.isNotBlank() } ?: "Sede asignada"
        val jornada = getSharedPreferences("TURNO_PAE", MODE_PRIVATE).getString("plato_jornada", null)
            ?.takeIf { it.isNotBlank() } ?: "Turno por asignar"
        binding.tvDetalleMiPlato.text = "$jornada · $sedeNombre"

        val totalLocal = AsistenciaManager.obtenerManana(this)
        binding.tvRacionesAsignadas.text = if (totalLocal > 0) "$totalLocal RACIONES PAE" else "— RACIONES PAE"

        val misAsignaciones = com.example.movilmanupuladora.utils.PreparacionSyncManager.obtenerMisAsignaciones(this)
        val pendientes = misAsignaciones.filter {
            !it.estado.contains("final", true) &&
                !it.estado.contains("complet", true) &&
                !it.estado.contains("listo", true)
        }
        val principal = pendientes.firstOrNull()

        binding.contenedorComponentesMiPlato.removeAllViews()
        binding.contenedorComponentesMiPlato.visibility = View.GONE

        if (principal != null) {
            val platoPrincipal = limpiarNombreMenu(principal.plato.takeIf { it.isNotBlank() } ?: nombrePlato)
            val mismoMenu = pendientes.filter { it.servicio.equals(principal.servicio, true) }

            binding.tvNombreMiPlato.text = platoPrincipal.split(" ").joinToString(" ") { p ->
                p.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
            }
            binding.tvDetalleMiPlato.text = "${principal.servicio} · $sedeNombre"
            binding.cardComponenteMiPlato.setCardBackgroundColor(android.graphics.Color.parseColor("#F5B81F"))
            binding.tvComponenteMiPlato.setTextColor(android.graphics.Color.WHITE)
            binding.tvComponenteMiPlato.text = "Comenzar preparación · ${principal.componente}"
            binding.imgMiPlato.setImageResource(InicioJefaActivity.obtenerImagenPlatoPorNombre(platoPrincipal))

            val abrirPrincipal = {
                abrirPreparacionComponente(platoPrincipal, principal.componente, principal.servicio)
            }
            binding.cardComponenteMiPlato.isEnabled = true
            binding.cardMiPlatoAsignado.isEnabled = true
            binding.cardComponenteMiPlato.setOnClickListener { abrirPrincipal() }
            binding.tvComponenteMiPlato.setOnClickListener { abrirPrincipal() }
            binding.cardMiPlatoAsignado.setOnClickListener { abrirPrincipal() }

            val extrasMismoMenu = mismoMenu.drop(1)
            if (extrasMismoMenu.isNotEmpty()) {
                binding.contenedorComponentesMiPlato.visibility = View.VISIBLE
                val density = resources.displayMetrics.density
                for (extra in extrasMismoMenu) {
                    val extraCard = com.google.android.material.card.MaterialCardView(this).apply {
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        ).also { it.topMargin = (8 * density).toInt() }
                        radius = 12 * density
                        cardElevation = 0f
                        setCardBackgroundColor(android.graphics.Color.parseColor("#F5B81F"))
                        strokeWidth = 0
                        isClickable = true
                        isFocusable = true
                        setOnClickListener {
                            abrirPreparacionComponente(
                                limpiarNombreMenu(extra.plato.ifBlank { platoPrincipal }),
                                extra.componente,
                                extra.servicio
                            )
                        }
                    }
                    val row = LinearLayout(this).apply {
                        orientation = LinearLayout.HORIZONTAL
                        gravity = android.view.Gravity.CENTER
                        setPadding((14 * density).toInt(), (12 * density).toInt(), (14 * density).toInt(), (12 * density).toInt())
                    }
                    val tv = TextView(this).apply {
                        text = "Comenzar preparación · ${extra.componente}"
                        setTextColor(android.graphics.Color.WHITE)
                        textSize = 13f
                        setTypeface(typeface, android.graphics.Typeface.BOLD)
                    }
                    row.addView(tv)
                    extraCard.addView(row)
                    binding.contenedorComponentesMiPlato.addView(extraCard)
                }
            }
        } else {
            if (misAsignaciones.isNotEmpty()) {
                binding.tvComponenteMiPlato.text = "Preparación finalizada ✓"
                binding.cardComponenteMiPlato.setCardBackgroundColor(android.graphics.Color.parseColor("#E6EDE2"))
                binding.tvComponenteMiPlato.setTextColor(android.graphics.Color.parseColor("#2B5424"))
                binding.cardComponenteMiPlato.isEnabled = false
                binding.cardMiPlatoAsignado.isEnabled = false
            } else {
                binding.tvComponenteMiPlato.text = "Componente pendiente de asignación"
                binding.cardComponenteMiPlato.setCardBackgroundColor(android.graphics.Color.parseColor("#F5B81F"))
                binding.tvComponenteMiPlato.setTextColor(android.graphics.Color.WHITE)
            }
            binding.imgMiPlato.setImageResource(InicioJefaActivity.obtenerImagenPlatoPorNombre(nombrePlato))
        }

        val finalizadas = misAsignaciones.filter {
            it.estado.contains("final", true) ||
                it.estado.contains("complet", true) ||
                it.estado.contains("listo", true)
        }
        renderizarTodasMisAsignaciones(finalizadas)
    }

    private fun abrirPreparacionComponente(platoDestino: String, componenteDestino: String?, servicioDestino: String?) {
        val intent = Intent(this, PreparacionActivity::class.java).apply {
            putExtra("id_plato", idPlato)
            putExtra("nombre_plato", platoDestino)
            putExtra("componente_seleccionado", componenteDestino)
            putExtra("servicio_seleccionado", servicioDestino)
            putExtra("id_imagen", InicioJefaActivity.obtenerImagenPlatoPorNombre(platoDestino))
        }
        startActivity(intent)
    }

    private fun renderizarTodasMisAsignaciones(
        asignaciones: List<com.example.movilmanupuladora.utils.PreparacionSyncManager.MiAsignacionItem>
    ) {
        val contenedor = binding.contenedorMisAsignacionesMultiples
        contenedor.removeAllViews()
        if (asignaciones.isEmpty()) {
            contenedor.visibility = View.GONE
            return
        }

        contenedor.visibility = View.VISIBLE
        val density = resources.displayMetrics.density
        val tvHeader = TextView(this).apply {
            text = "ASIGNACIONES FINALIZADAS (${asignaciones.size})"
            setTextColor(android.graphics.Color.parseColor("#2B5424"))
            textSize = 11.5f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setPadding((4 * density).toInt(), 0, 0, (6 * density).toInt())
        }
        contenedor.addView(tvHeader)

        for (asig in asignaciones) {
            val platoLimpio = limpiarNombreMenu(asig.plato.ifBlank { nombrePlato })
            val card = com.google.android.material.card.MaterialCardView(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).also { it.bottomMargin = (8 * density).toInt() }
                radius = 14 * density
                cardElevation = 1 * density
                setCardBackgroundColor(android.graphics.Color.parseColor("#F7FAF5"))
                strokeColor = android.graphics.Color.parseColor("#C6D6BF")
                strokeWidth = (1 * density).toInt()
                isClickable = true
                isFocusable = true
                setOnClickListener {
                    abrirPreparacionComponente(platoLimpio, asig.componente, asig.servicio)
                }
            }
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = android.view.Gravity.CENTER_VERTICAL
                setPadding((14 * density).toInt(), (10 * density).toInt(), (14 * density).toInt(), (10 * density).toInt())
            }
            val col = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            val tvTop = TextView(this).apply {
                text = "${asig.servicio.uppercase()} · $platoLimpio"
                setTextColor(android.graphics.Color.parseColor("#2B5424"))
                textSize = 11.5f
                setTypeface(typeface, android.graphics.Typeface.BOLD)
            }
            val tvComp = TextView(this).apply {
                text = asig.componente
                setTextColor(android.graphics.Color.parseColor("#1E293B"))
                textSize = 13.5f
                setTypeface(typeface, android.graphics.Typeface.BOLD)
                setPadding(0, (2 * density).toInt(), 0, 0)
            }
            val badge = TextView(this).apply {
                text = "Finalizada ✓"
                setTextColor(android.graphics.Color.parseColor("#2B5424"))
                textSize = 11f
                setTypeface(typeface, android.graphics.Typeface.BOLD)
                setBackgroundResource(R.drawable.bg_pill_light_green)
                setPadding((10 * density).toInt(), (4 * density).toInt(), (10 * density).toInt(), (4 * density).toInt())
            }
            col.addView(tvTop)
            col.addView(tvComp)
            row.addView(col)
            row.addView(badge)
            card.addView(row)
            contenedor.addView(card)
        }
    }

    private fun configurarAcciones() {
        binding.btnVerInsumos.setOnClickListener {
            val intent = Intent(this, IngredientesActivity::class.java).apply {
                putExtra("id_plato", idPlato)
                putExtra("nombre_plato", nombrePlato)
            }
            startActivity(intent)
        }

        binding.btnIniciarPreparacionTask.setOnClickListener {
            val misAsig = com.example.movilmanupuladora.utils.PreparacionSyncManager.obtenerMisAsignaciones(this)
            val activa = misAsig.firstOrNull {
                !it.estado.contains("final", true) &&
                    !it.estado.contains("complet", true) &&
                    !it.estado.contains("listo", true)
            } ?: misAsig.firstOrNull()
            val platoDestino = activa?.plato?.takeIf { it.isNotBlank() } ?: nombrePlato
            val compAsignado = activa?.componente ?: sessionMgr.getComponenteAsignado(this)
            abrirPreparacionComponente(platoDestino, compAsignado, activa?.servicio)
        }

        binding.cardVerEquipo.setOnClickListener {
            if (binding.layoutEquipoOperarias.visibility == View.VISIBLE) {
                binding.layoutEquipoOperarias.visibility = View.GONE
            } else {
                binding.layoutEquipoOperarias.visibility = View.VISIBLE
            }
        }

        binding.cardPasoAsignado1.setOnClickListener {
            val miNombre = sessionMgr.getUserName() ?: "Manipuladora"
            mostrarDialogoDetalleAsignacion("Pesado y verificación de insumos", miNombre)
        }

        binding.cardPasoAsignado2.setOnClickListener {
            val miNombre = sessionMgr.getUserName() ?: "Manipuladora"
            mostrarDialogoDetalleAsignacion("Alistamiento e inocuidad (Mise en place)", miNombre)
        }

        binding.cardPasoAsignado3.setOnClickListener {
            val miNombre = sessionMgr.getUserName() ?: "Manipuladora"
            mostrarDialogoDetalleAsignacion("Cocción y servido escolar", miNombre)
        }
    }

    private fun mostrarDialogoDetalleAsignacion(nombrePaso: String, operaria: String) {
        val dialogBinding = DialogDetalleAsignacionBinding.inflate(layoutInflater)
        dialogBinding.tvAsignacionTitulo.text = "DETALLE DE ASIGNACIÓN PAE"
        dialogBinding.tvNombrePlatoAsignacion.text = nombrePlato
        dialogBinding.tvEstadoAsignacion.text = "Responsable: $operaria · $nombrePaso"
        dialogBinding.tvPaso1.text = "1. Recepción y desinfección de materia prima"
        dialogBinding.tvPaso2.text = "2. $nombrePaso"
        dialogBinding.tvPaso3.text = "3. Servido y control de gramaje escolar"
        dialogBinding.tvIngredientesAsignacion.text = "• Insumos del componente asignado según receta PAE\n• Sazón y control de inocuidad\n• Raciones completas para estudiantes"

        val dialog = MaterialAlertDialogBuilder(this)
            .setView(dialogBinding.root)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        dialogBinding.btnMarcarCompletado.setOnClickListener {
            dialog.dismiss()
            val sedeNombre = sessionMgr.getSedeNombre()?.takeIf { it.isNotBlank() } ?: "Sede asignada"
        val jornada = getSharedPreferences("TURNO_PAE", MODE_PRIVATE).getString("plato_jornada", null)
            ?.takeIf { it.isNotBlank() } ?: "Turno por asignar"
        binding.tvDetalleMiPlato.text = "$jornada · $sedeNombre"

        val totalLocal = AsistenciaManager.obtenerManana(this)
        binding.tvRacionesAsignadas.text = if (totalLocal > 0) "$totalLocal RACIONES PAE" else "— RACIONES PAE"

        val compAsignado = sessionMgr.getComponenteAsignado(this) ?: "Preparación general"
            val miNombre = sessionMgr.getUserName() ?: "Manipuladora"
            val miCorreo = sessionMgr.getUserEmail() ?: ""

            val estadoActual = PreparacionEstadoManager.obtenerEstado(this, miNombre, compAsignado, miCorreo)
            val siguienteEstado = when (estadoActual) {
                PreparacionEstadoManager.ESTADO_POR_HACER -> PreparacionEstadoManager.ESTADO_EN_PROCESO
                PreparacionEstadoManager.ESTADO_EN_PROCESO -> PreparacionEstadoManager.ESTADO_FINALIZADA
                else -> PreparacionEstadoManager.ESTADO_FINALIZADA
            }

            PreparacionEstadoManager.guardarEstado(
                context = this,
                persona = miNombre,
                componente = compAsignado,
                estado = siguienteEstado,
                correo = miCorreo
            )

            configurarEncabezadoYPlato()
            configurarListaOperarias()
            Toast.makeText(this, "¡Estado de $compAsignado actualizado a '$siguienteEstado'!", Toast.LENGTH_SHORT).show()
        }

        dialogBinding.btnCerrarDetalle.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun mostrarDialogoOpcionesEstadoAsignada(componente: String, persona: String) {
        val miCorreo = sessionMgr.getUserEmail() ?: ""
        val opciones = listOf(
            Triple("Por hacer / Pendiente", android.graphics.Color.parseColor("#FEF3C7"), android.graphics.Color.parseColor("#D97706")),
            Triple("En proceso / En preparación ⏳", android.graphics.Color.parseColor("#DBEAFE"), android.graphics.Color.parseColor("#2563EB")),
            Triple("Finalizada / Completada ✓", android.graphics.Color.parseColor("#DCFCE7"), android.graphics.Color.parseColor("#16A34A"))
        )

        val dialogBinding = com.example.movilmanupuladora.databinding.DialogOpcionesJefaBinding.inflate(layoutInflater)
        dialogBinding.txtTituloOpciones.text = "Seleccionar Estado de $componente"
        dialogBinding.contenedorBotonesOpciones.removeAllViews()

        val dialog = MaterialAlertDialogBuilder(this)
            .setView(dialogBinding.root)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        for ((index, item) in opciones.withIndex()) {
            val (opcion, bgHex, strokeHex) = item
            val btn = com.google.android.material.button.MaterialButton(this).apply {
                text = opcion
                isAllCaps = false
                setTextColor(strokeHex)
                textSize = 14f
                backgroundTintList = android.content.res.ColorStateList.valueOf(bgHex)
                strokeColor = android.content.res.ColorStateList.valueOf(strokeHex)
                strokeWidth = (1.5 * resources.displayMetrics.density).toInt()
                cornerRadius = (14 * resources.displayMetrics.density).toInt()
                gravity = android.view.Gravity.START or android.view.Gravity.CENTER_VERTICAL
                setPadding((16 * resources.displayMetrics.density).toInt(), (14 * resources.displayMetrics.density).toInt(), (16 * resources.displayMetrics.density).toInt(), (14 * resources.displayMetrics.density).toInt())

                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    bottomMargin = (10 * resources.displayMetrics.density).toInt()
                }

                setOnClickListener {
                    dialog.dismiss()
                    val nuevoEstado = when (index) {
                        0 -> PreparacionEstadoManager.ESTADO_POR_HACER
                        1 -> PreparacionEstadoManager.ESTADO_EN_PROCESO
                        2 -> PreparacionEstadoManager.ESTADO_FINALIZADA
                        else -> PreparacionEstadoManager.ESTADO_POR_HACER
                    }
                    PreparacionEstadoManager.guardarEstado(
                        context = this@AsignadasActivity,
                        persona = persona,
                        componente = componente,
                        estado = nuevoEstado,
                        correo = miCorreo
                    )
                    configurarEncabezadoYPlato()
                    configurarListaOperarias()
                    Toast.makeText(this@AsignadasActivity, "Estado actualizado a: $nuevoEstado", Toast.LENGTH_SHORT).show()
                }
            }
            dialogBinding.contenedorBotonesOpciones.addView(btn)
        }

        dialogBinding.btnCancelarOpciones.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.90).toInt(),
            android.view.ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    private fun cargarDatosRealesAsignadas() {
        lifecycleScope.launch {
            try {
                com.example.movilmanupuladora.utils.SessionManager.getToken(this@AsignadasActivity)
                val sedeId = sessionMgr.getSedeId()

                // 1. Raciones reales desde AsistenciaDiaria
                runCatching {
                    val respAsist = com.example.movilmanupuladora.data.repository.AsistenciaRepository(
                        com.example.movilmanupuladora.data.api.RetrofitClient.apiService
                    ).obtenerAsistencias()
                    if (respAsist.isSuccessful && respAsist.body() != null) {
                        val hoy = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                        val lista = respAsist.body()!!
                        val deHoy = lista.filter { it.fecha.startsWith(hoy) }.ifEmpty { lista }
                        val total = deHoy.sumOf { it.ninosPresentes }
                        if (total > 0) {
                            binding.tvRacionesAsignadas.text = "$total RACIONES PAE"
                        }
                    }
                }

                // 2. Estado real de insumos en bodega
                runCatching {
                    val api = com.example.movilmanupuladora.data.api.RetrofitClient.apiService
                    val respInv = api.obtenerInventario()
                    val items = respInv.body().orEmpty().filter {
                        sedeId <= 0 || it.idSede == null || it.idSede == sedeId
                    }
                    binding.badgeEstadoTurno.text = when {
                        items.isEmpty() -> "Sin Insumos"
                        items.any { (it.cantidadActual?.toDoubleOrNull() ?: it.cantidad ?: 0.0) <= 0.0 } -> "Insumos Bajos"
                        else -> "Insumos Listos"
                    }
                }

                // 3. Información nutricional real desde Menús
                runCatching {
                    val respMenus = com.example.movilmanupuladora.data.repository.MenuRepository(
                        com.example.movilmanupuladora.data.api.RetrofitClient.apiService
                    ).obtenerMenus()
                    if (respMenus.isSuccessful && respMenus.body() != null) {
                        val hoy = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                        val menus = respMenus.body()!!
                        val menuActivo = menus.firstOrNull {
                            it.fecha?.startsWith(hoy) == true && !it.informacion_nutricional.isNullOrBlank()
                        } ?: menus.lastOrNull { !it.informacion_nutricional.isNullOrBlank() }
                        infoNutricionalMenu = menuActivo?.informacion_nutricional
                        actualizarVistaNutricional()
                    }
                }
            } catch (_: Exception) { }
        }
    }

    private fun actualizarVistaNutricional() {
        val raw = infoNutricionalMenu?.trim().orEmpty()
        fun extraer(patron: String): String? =
            Regex(patron, RegexOption.IGNORE_CASE).find(raw)?.groupValues?.getOrNull(1)?.trim()

        val kcal = extraer("""(\d+(?:[.,]\d+)?)\s*kcal""")
        val prot = extraer("""prot[eí]nas?\s*[:\-]?\s*(\d+(?:[.,]\d+)?\s*g)""")
        val carb = extraer("""carbohidratos?\s*[:\-]?\s*(\d+(?:[.,]\d+)?\s*g)""")
        val gras = extraer("""grasas?\s*[:\-]?\s*(\d+(?:[.,]\d+)?\s*g)""")
        val porc = extraer("""(\d+(?:[.,]\d+)?%)\s*(?:req|requerimiento)?""")
        val gram = extraer("""(\d+(?:[.,]\d+)?\s*g\s*/\s*raci[oó]n)""")

        binding.tvEnergiaKcal.text = kcal?.let { "$it kcal" } ?: if (raw.isNotBlank()) "Según minuta" else "Sin dato"
        binding.tvRequerimientoDiario.text = porc?.let { "$it requerimiento diario" }
            ?: if (nivelNutricionalPrimaria) "Nivel Primaria PAE" else "Nivel Secundaria PAE"
        binding.tvGramosRacion.text = gram ?: raw.takeIf { it.isNotBlank() && it.length <= 42 } ?: "Minuta oficial PAE"
        binding.tvProteinasValor.text = prot ?: "—"
        binding.tvCarbohidratosValor.text = carb ?: "—"
        binding.tvGrasasValor.text = gras ?: "—"
        binding.tvCalcioHierroValor.text = if (raw.contains("calcio", true) || raw.contains("hierro", true)) {
            "Incluido"
        } else {
            "—"
        }
    }

    private fun configurarConmutadorNutricional() {
        val btnPrimaria = findViewById<TextView>(R.id.btnPrimaria)
        val btnSecundaria = findViewById<TextView>(R.id.btnSecundaria)

        btnPrimaria?.setOnClickListener {
            nivelNutricionalPrimaria = true
            btnPrimaria.setBackgroundResource(R.drawable.bg_pill_green)
            btnPrimaria.setTextColor(android.graphics.Color.WHITE)
            btnSecundaria?.background = null
            btnSecundaria?.setTextColor(android.graphics.Color.parseColor("#64748B"))
            actualizarVistaNutricional()
        }

        btnSecundaria?.setOnClickListener {
            nivelNutricionalPrimaria = false
            btnSecundaria.setBackgroundResource(R.drawable.bg_pill_green)
            btnSecundaria.setTextColor(android.graphics.Color.WHITE)
            btnPrimaria?.background = null
            btnPrimaria?.setTextColor(android.graphics.Color.parseColor("#64748B"))
            actualizarVistaNutricional()
        }
    }

    private fun configurarListaOperarias() {
        val prefs = getSharedPreferences("ManipuladoraJefe", MODE_PRIVATE)
        val listaReal = ManipuladorasManager.obtenerManipuladoras().filter { it.activa }
        val miNombre = sessionMgr.getUserName()?.trim()?.lowercase() ?: ""
        val miCorreo = sessionMgr.getUserEmail()?.trim()?.lowercase() ?: ""

        binding.contenedorListaOperarias.removeAllViews()

        val compJefa = prefs.getString("comp_jefa", null)
        if (!compJefa.isNullOrBlank()) {
            val esJefaActual = sessionMgr.getUserRole()?.lowercase()?.contains("jef") == true
            val nombreJefa = prefs.getString("jefa_nombre", null)?.takeIf { it.isNotBlank() }
                ?: (if (esJefaActual) sessionMgr.getUserName() ?: "Jefa de Cocina" else "Jefa de Cocina")
            val estadoJefa = PreparacionEstadoManager.obtenerEstado(this, nombreJefa, compJefa)

            val itemJefaCard = com.google.android.material.card.MaterialCardView(this).apply {
                cardElevation = if (esJefaActual) 4f else 2f
                radius = 24f
                strokeWidth = if (esJefaActual) 2 else 1
                setStrokeColor(if (esJefaActual) 0xFFF4B41F.toInt() else 0xFFE2E8F0.toInt())
                setCardBackgroundColor(if (esJefaActual) 0xFFFFFDF5.toInt() else 0xFFFFFFFF.toInt())
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).also { it.setMargins(0, 8, 0, 8) }
            }

            val layoutJefaFila = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = android.view.Gravity.CENTER_VERTICAL
                setPadding(24, 16, 24, 16)
            }

            val imgJefaAvatar = android.widget.ImageView(this).apply {
                setImageResource(R.drawable.ic_nav_perfil)
                setBackgroundResource(R.drawable.bg_icono_amarillo)
                setPadding(12, 12, 12, 12)
                layoutParams = LinearLayout.LayoutParams(64, 64)
            }

            val layoutJefaInfo = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1f
                ).also { it.marginStart = 20 }
            }

            val tvNombreJ = TextView(this).apply {
                text = if (esJefaActual) "$nombreJefa (Tú)" else nombreJefa
                setTextColor(if (esJefaActual) 0xFF7A5500.toInt() else 0xFF1E293B.toInt())
                textSize = 13f
                setTypeface(typeface, android.graphics.Typeface.BOLD)
            }

            val tvCargoJ = TextView(this).apply {
                text = "Componente: $compJefa • $estadoJefa"
                val color = when (estadoJefa) {
                    PreparacionEstadoManager.ESTADO_FINALIZADA -> 0xFF176B35.toInt()
                    PreparacionEstadoManager.ESTADO_EN_PROCESO -> 0xFF855B00.toInt()
                    else -> 0xFF1B3317.toInt()
                }
                setTextColor(color)
                textSize = 11f
                setTypeface(typeface, android.graphics.Typeface.BOLD)
            }

            layoutJefaInfo.addView(tvNombreJ)
            layoutJefaInfo.addView(tvCargoJ)

            layoutJefaFila.addView(imgJefaAvatar)
            layoutJefaFila.addView(layoutJefaInfo)

            itemJefaCard.addView(layoutJefaFila)
            itemJefaCard.setOnClickListener {
                mostrarDialogoDetalleAsignacion("Preparación de $compJefa", nombreJefa)
            }

            binding.contenedorListaOperarias.addView(itemJefaCard)
        }

        for (m in listaReal) {
            val keyCorreo = "comp_correo_" + m.correo.trim().replace("@", "_").replace(".", "_").lowercase()
            val keyNombre = "comp_" + m.nombre.trim().replace(" ", "_").lowercase()
            val comp = prefs.getString(keyCorreo, null) ?: prefs.getString(keyNombre, null)

            val esUsuarioActual = (miCorreo.isNotBlank() && m.correo.trim().equals(miCorreo, ignoreCase = true)) ||
                    (miNombre.isNotBlank() && (m.nombre.trim().equals(miNombre, ignoreCase = true) || m.nombre.lowercase().contains(miNombre) || miNombre.contains(m.nombre.lowercase())))

            val estadoOperaria = if (!comp.isNullOrBlank()) {
                PreparacionEstadoManager.obtenerEstado(this, m.nombre, comp, m.correo)
            } else {
                "Sin asignar"
            }

            val density = resources.displayMetrics.density
            val esFin = estadoOperaria == PreparacionEstadoManager.ESTADO_FINALIZADA
            val esProc = estadoOperaria == PreparacionEstadoManager.ESTADO_EN_PROCESO
            val itemCard = com.google.android.material.card.MaterialCardView(this).apply {
                cardElevation = if (esUsuarioActual) 3f else 1f
                radius = 14 * density
                strokeWidth = if (esUsuarioActual) (1.5f * density).toInt() else (1 * density).toInt()
                setStrokeColor(
                    when {
                        esFin -> 0xFF2B5424.toInt()
                        esUsuarioActual -> 0xFFF5B81F.toInt()
                        else -> 0xFFE2E8F0.toInt()
                    }
                )
                setCardBackgroundColor(
                    when {
                        esFin -> 0xFFF7FAF5.toInt()
                        esUsuarioActual -> 0xFFFFFDF5.toInt()
                        else -> 0xFFFFFFFF.toInt()
                    }
                )
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).also { it.setMargins(0, (5 * density).toInt(), 0, (5 * density).toInt()) }
            }

            val outerRow = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
            }

            val barraColor = View(this).apply {
                layoutParams = LinearLayout.LayoutParams((4 * density).toInt(), LinearLayout.LayoutParams.MATCH_PARENT)
                setBackgroundColor(
                    when {
                        esFin -> 0xFF2B5424.toInt()
                        !comp.isNullOrBlank() -> 0xFFF5B81F.toInt()
                        else -> 0xFFCBD5E1.toInt()
                    }
                )
            }

            val layoutFila = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = android.view.Gravity.CENTER_VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                setPadding((12 * density).toInt(), (10 * density).toInt(), (12 * density).toInt(), (10 * density).toInt())
            }

            val avatarBg = com.google.android.material.card.MaterialCardView(this).apply {
                layoutParams = LinearLayout.LayoutParams((36 * density).toInt(), (36 * density).toInt())
                radius = 18 * density
                cardElevation = 0f
                strokeWidth = 0
                setCardBackgroundColor(if (esUsuarioActual) 0xFFFEF7E0.toInt() else 0xFFE6EDE2.toInt())
            }
            val imgAvatar = android.widget.ImageView(this).apply {
                setImageResource(R.drawable.ic_person)
                imageTintList = android.content.res.ColorStateList.valueOf(
                    if (esUsuarioActual) 0xFF7A5500.toInt() else 0xFF2B5424.toInt()
                )
                layoutParams = android.widget.FrameLayout.LayoutParams((20 * density).toInt(), (20 * density).toInt(), android.view.Gravity.CENTER)
            }
            avatarBg.addView(imgAvatar)

            val layoutInfo = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1f
                ).also { it.marginStart = (10 * density).toInt() }
            }

            val tvNombre = TextView(this).apply {
                text = if (esUsuarioActual) "${m.nombre} (Tú)" else m.nombre
                setTextColor(if (esUsuarioActual) 0xFF2B5424.toInt() else 0xFF1E293B.toInt())
                textSize = 13f
                setTypeface(typeface, android.graphics.Typeface.BOLD)
            }

            val tvCargo = TextView(this).apply {
                text = if (!comp.isNullOrBlank()) "Componente: $comp" else m.funcion
                setTextColor(0xFF475569.toInt())
                textSize = 11.5f
            }

            val tvBadge = TextView(this).apply {
                text = when {
                    comp.isNullOrBlank() -> "Sin asignar"
                    esFin -> "Finalizada ✓"
                    esProc -> "En preparación"
                    else -> "Por hacer"
                }
                val (bgRes, txtColor) = when {
                    comp.isNullOrBlank() -> R.drawable.bg_tag_plato to 0xFF64748B.toInt()
                    esFin -> R.drawable.bg_pill_light_green to 0xFF2B5424.toInt()
                    else -> R.drawable.bg_pill_yellow to 0xFF7A5500.toInt()
                }
                setBackgroundResource(bgRes)
                setTextColor(txtColor)
                textSize = 10.5f
                setTypeface(typeface, android.graphics.Typeface.BOLD)
                setPadding((10 * density).toInt(), (4 * density).toInt(), (10 * density).toInt(), (4 * density).toInt())
            }

            layoutInfo.addView(tvNombre)
            layoutInfo.addView(tvCargo)

            layoutFila.addView(avatarBg)
            layoutFila.addView(layoutInfo)
            layoutFila.addView(tvBadge)

            outerRow.addView(barraColor)
            outerRow.addView(layoutFila)
            itemCard.addView(outerRow)

            itemCard.setOnClickListener {
                val detalle = if (!comp.isNullOrBlank()) "Preparación de $comp ($estadoOperaria)" else m.funcion
                mostrarDialogoDetalleAsignacion(detalle, m.nombre)
            }

            binding.contenedorListaOperarias.addView(itemCard)
        }
    }

    private fun configurarNavegacion() {
        NavigationHelper.setupBarraNavegacion(
            this,
            binding.barraNavegacion,
            NavigationHelper.Tab.ASIGNADAS
        )
    }
}