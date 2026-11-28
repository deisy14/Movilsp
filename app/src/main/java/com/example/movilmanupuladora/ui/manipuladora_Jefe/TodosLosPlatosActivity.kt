package com.example.movilmanupuladora.ui.manipuladora_Jefe

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.movilmanupuladora.R
import com.example.movilmanupuladora.data.api.RetrofitClient
import com.example.movilmanupuladora.data.model.MenuPaeCatalog
import com.example.movilmanupuladora.data.model.PlatoResponse
import com.example.movilmanupuladora.data.repository.MenuRepository
import com.example.movilmanupuladora.databinding.ActivityTodosLosPlatosBinding
import com.example.movilmanupuladora.databinding.DialogDetallePlatoBinding
import com.example.movilmanupuladora.utils.PreparacionSyncManager
import com.example.movilmanupuladora.utils.SedeContratoManager
import com.example.movilmanupuladora.utils.SessionManager
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class TodosLosPlatosActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTodosLosPlatosBinding
    private val sessionManager by lazy { SessionManager(this) }
    private val menuRepository = MenuRepository(RetrofitClient.apiService)
    private var fechaSeleccionada: String = ""

    private val platosRespaldo = MenuPaeCatalog.menus.map {
        PlatoResponse(idPlato = it.id, nombrePlato = it.nombre, componente = it.componente)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityTodosLosPlatosBinding.inflate(layoutInflater)
        setContentView(binding.root)

        fechaSeleccionada = intent.getStringExtra("fecha_seleccionada") ?: run {
            SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        }

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        binding.btnVolver.setOnClickListener { finish() }

        cargarPlatosBackend()
    }

    private fun cargarPlatosBackend() {
        // Mostrar el catálogo oficial completo del PAE con los nombres e imágenes actualizados
        mostrarLista(platosRespaldo)

        lifecycleScope.launch {
            try {
                val res = menuRepository.obtenerPlatos()
                if (res.isSuccessful && !res.body().isNullOrEmpty()) {
                    val lista = res.body()!!
                    MenuPaeCatalog.registrarPlatosDesdeBackend(lista)
                    mostrarLista(lista)
                }
            } catch (_: Exception) {
                // Mantiene el catálogo oficial del PAE
            }
        }
    }

    private fun mostrarLista(platos: List<PlatoResponse>) {
        val adapter = PlatoCatalogoAdapter(platos) { plato ->
            mostrarDialogoDetallePlato(plato)
        }
        binding.recyclerTodosLosPlatos.adapter = adapter
        binding.recyclerTodosLosPlatos.layoutManager = LinearLayoutManager(this)
    }

    private fun mostrarDialogoDetallePlato(plato: PlatoResponse) {
        val dialogBinding = DialogDetallePlatoBinding.inflate(layoutInflater)
        val sessionMgr = SessionManager(this)

        val nombrePlatoActual = SessionManager.limpiarPrefijoMenu(plato.nombrePlato ?: "Plato PAE")
        dialogBinding.tvTituloPlatoModalHeader.text = nombrePlatoActual
        dialogBinding.tvTituloPlatoModal.text = nombrePlatoActual
        dialogBinding.cardSeccionModal.visibility = View.GONE
        (dialogBinding.cardComponenteModal.layoutParams as? android.view.ViewGroup.MarginLayoutParams)?.let { lp ->
            lp.marginStart = 0
            dialogBinding.cardComponenteModal.layoutParams = lp
        }
        dialogBinding.tvComponenteModal.text = plato.componente?.takeIf { it.isNotBlank() } ?: "Componentes PAE"
        dialogBinding.tvPorcionModal.text = "100g aprox. por ración"
        dialogBinding.layoutTotalPrepararRow.visibility = View.GONE
        dialogBinding.layoutGramajeTotalHoySection.visibility = View.GONE
        dialogBinding.tvBadgeEstadoModal.text = "Verificando inventario..."

        val fechaLegible = try {
            val parsed = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(fechaSeleccionada)
            if (parsed != null) SimpleDateFormat("EEEE d 'de' MMMM", Locale.forLanguageTag("es-CO")).format(parsed).lowercase()
            else fechaSeleccionada
        } catch (_: Exception) { fechaSeleccionada }
        dialogBinding.tvTituloAsignarServicio.text = "Configurar este plato para el día $fechaLegible:"

        val componentes = MenuPaeCatalog.obtenerComponentesPorNombre(nombrePlatoActual)
            .ifEmpty { (plato.componente ?: "").split(",").map { it.trim() }.filter { it.isNotBlank() } }
        val recetaItems = plato.recetaComponentes.orEmpty()
        val ingredientesBackend = recetaItems
            .mapNotNull { it.nombreIngrediente?.trim()?.takeIf { n -> n.isNotBlank() } }
        val pasosBackend = plato.pasosPreparacion.orEmpty()
            .mapNotNull { it.descripcionPaso?.trim()?.takeIf { p -> p.isNotBlank() } }

        val pasosContainer = dialogBinding.layoutPasosCatalogoModal
        pasosContainer.removeAllViews()
        if (ingredientesBackend.isNotEmpty() || pasosBackend.isNotEmpty() || componentes.isNotEmpty()) {
            pasosContainer.visibility = View.VISIBLE
            if (ingredientesBackend.isNotEmpty()) {
                val lblIng = android.widget.TextView(this).apply {
                    text = "INGREDIENTES DE LA RECETA"
                    textSize = 10.5f
                    setTypeface(null, android.graphics.Typeface.BOLD)
                    setTextColor(Color.parseColor("#92400E"))
                    setPadding(0, 0, 0, 4)
                }
                pasosContainer.addView(lblIng)
                val txtIng = android.widget.TextView(this).apply {
                    text = ingredientesBackend.joinToString(", ")
                    textSize = 12f
                    setTextColor(Color.parseColor("#334155"))
                    setPadding(0, 0, 0, 10)
                }
                pasosContainer.addView(txtIng)
            }

            val lblTitulo = android.widget.TextView(this).apply {
                text = "PREPARACIÓN PASO A PASO"
                textSize = 10.5f
                setTypeface(null, android.graphics.Typeface.BOLD)
                setTextColor(Color.parseColor("#92400E"))
                setPadding(0, 0, 0, 6)
            }
            pasosContainer.addView(lblTitulo)

            val listaPasosMostrar = if (pasosBackend.isNotEmpty()) {
                pasosBackend
            } else {
                componentes.map { "$it: Preparar y porcionar según gramaje PAE e inocuidad alimentaria." }
            }
            listaPasosMostrar.forEachIndexed { idx, paso ->
                val pasoTxt = android.widget.TextView(this).apply {
                    text = "${idx + 1}. $paso"
                    textSize = 12f
                    setTextColor(Color.parseColor("#334155"))
                    setPadding(0, 4, 0, 4)
                }
                pasosContainer.addView(pasoTxt)
            }
        }

        val imgRes = if (plato.idPlato in 1..20) {
            MenuPaeCatalog.obtenerMenuPorId(plato.idPlato).imagenRes
        } else {
            MenuPaeCatalog.obtenerImagenPorNombre(plato.nombrePlato ?: plato.componente)
        }
        dialogBinding.imgPlatoModal.setImageResource(imgRes)

        // Ocultar botones redundantes; dejar configurar este plato para el día al final
        dialogBinding.btnVerComponentesModal.visibility = View.GONE
        dialogBinding.btnOtroAleatorioModal.visibility = View.GONE
        dialogBinding.layoutSeleccionServicioModal.visibility = View.VISIBLE

        // Consultar inventario real y asistencia del día seleccionado
        lifecycleScope.launch {
            try {
                val invRes = com.example.movilmanupuladora.data.api.RetrofitClient.apiService.obtenerInventario()
                val sedeId = sessionMgr.getSedeId()
                val listaInv = (invRes.body() ?: emptyList()).filter {
                    sedeId <= 0 || it.idSede == sedeId || it.sede == sedeId || (it.idSede == null && it.sede == null)
                }
                val tieneTodos = if (recetaItems.isEmpty()) {
                    false
                } else {
                    recetaItems.all { rc ->
                        val idIng = rc.idIngrediente
                        val nomIng = rc.nombreIngrediente?.trim()?.lowercase()
                        listaInv.any { inv ->
                            val matchId = idIng != null && inv.obtenerIdIngredienteReal() == idIng
                            val matchNom = !nomIng.isNullOrBlank() &&
                                (inv.nombreIngrediente?.trim()?.lowercase() == nomIng || inv.nombre?.trim()?.lowercase() == nomIng)
                            val stock = inv.cantidadActual?.toDoubleOrNull() ?: inv.cantidad ?: 0.0
                            (matchId || matchNom) && stock > 0.0
                        }
                    }
                }
                if (tieneTodos) {
                    dialogBinding.tvBadgeEstadoModal.text = "Inventario completo"
                    dialogBinding.tvBadgeEstadoModal.setTextColor(Color.parseColor("#15803D"))
                    dialogBinding.imgBadgeEstadoModal.setColorFilter(Color.parseColor("#15803D"))
                    dialogBinding.layoutBadgeEstadoModal.setBackgroundResource(R.drawable.bg_pill_light_green)
                } else {
                    dialogBinding.tvBadgeEstadoModal.text = if (recetaItems.isEmpty()) "Sin ingredientes en receta" else "Faltan ingredientes en inventario"
                    dialogBinding.tvBadgeEstadoModal.setTextColor(Color.parseColor("#B91C1C"))
                    dialogBinding.imgBadgeEstadoModal.setColorFilter(Color.parseColor("#B91C1C"))
                    dialogBinding.layoutBadgeEstadoModal.setBackgroundResource(R.drawable.bg_chip_pendiente)
                }
            } catch (_: Exception) {
                dialogBinding.tvBadgeEstadoModal.text = "Inventario no verificado"
            }

            try {
                val hoyStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                if (fechaSeleccionada <= hoyStr) {
                    val asisRes = com.example.movilmanupuladora.data.api.RetrofitClient.apiService.obtenerAsistenciaDiaria()
                    val delDia = (asisRes.body() ?: emptyList()).filter { it.fecha == fechaSeleccionada }
                    val prim = delDia.filter { it.obtenerIdGradoReal() == 16 }.maxByOrNull { it.obtenerIdReal() ?: 0 }?.ninosPresentes ?: 0
                    val sec = delDia.filter { it.obtenerIdGradoReal() == 19 }.maxByOrNull { it.obtenerIdReal() ?: 0 }?.ninosPresentes ?: 0
                    val otros = if (prim == 0 && sec == 0) delDia.sumOf { it.ninosPresentes } else prim + sec
                    val totalAsistencia = if (otros > 0) otros else if (fechaSeleccionada == hoyStr) {
                        AsistenciaManager.obtenerPrimaria(this@TodosLosPlatosActivity) +
                            AsistenciaManager.obtenerSecundaria(this@TodosLosPlatosActivity)
                    } else 0

                    if (totalAsistencia > 0) {
                        dialogBinding.layoutTotalPrepararRow.visibility = View.VISIBLE
                        dialogBinding.tvTotalPrepararModal.text = "$totalAsistencia porciones"
                        if (recetaItems.isNotEmpty()) {
                            dialogBinding.layoutGramajeTotalHoySection.visibility = View.VISIBLE
                            dialogBinding.tvTituloGramajeModal.text = "Gramaje total requerido ($totalAsistencia niños)"
                            dialogBinding.layoutFilasGramajeModal.removeAllViews()
                            val density = resources.displayMetrics.density
                            recetaItems.forEach { rc ->
                                val nom = rc.nombreIngrediente?.takeIf { it.isNotBlank() } ?: return@forEach
                                val cantPorNino = rc.cantidad ?: 1.0
                                val totalG = if (rc.baseCantidad == "TOTAL_PREPARACION") cantPorNino else cantPorNino * totalAsistencia
                                val textoTotal = if (totalG >= 1000.0) {
                                    String.format(Locale.US, "%.2f kg", totalG / 1000.0)
                                } else {
                                    String.format(Locale.US, "%.1f g", totalG)
                                }
                                val textoPorNino = if (rc.baseCantidad == "TOTAL_PREPARACION") {
                                    "Total preparación"
                                } else {
                                    String.format(Locale.US, "%.1f g por niño", cantPorNino)
                                }
                                val card = com.google.android.material.card.MaterialCardView(this@TodosLosPlatosActivity).apply {
                                    layoutParams = android.widget.LinearLayout.LayoutParams(
                                        android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                                        android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
                                    ).apply { bottomMargin = (8 * density).toInt() }
                                    setCardBackgroundColor(Color.WHITE)
                                    radius = 12 * density
                                    cardElevation = 1 * density
                                    strokeColor = Color.parseColor("#E2E8F0")
                                    strokeWidth = (1 * density).toInt()
                                }
                                val row = android.widget.LinearLayout(this@TodosLosPlatosActivity).apply {
                                    orientation = android.widget.LinearLayout.HORIZONTAL
                                    gravity = android.view.Gravity.CENTER_VERTICAL
                                    val pad = (12 * density).toInt()
                                    setPadding(pad, pad, pad, pad)
                                }
                                val colIzq = android.widget.LinearLayout(this@TodosLosPlatosActivity).apply {
                                    orientation = android.widget.LinearLayout.VERTICAL
                                    layoutParams = android.widget.LinearLayout.LayoutParams(0, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                                }
                                colIzq.addView(android.widget.TextView(this@TodosLosPlatosActivity).apply {
                                    text = nom
                                    textSize = 13.5f
                                    setTypeface(null, android.graphics.Typeface.BOLD)
                                    setTextColor(Color.parseColor("#1E293B"))
                                })
                                colIzq.addView(android.widget.TextView(this@TodosLosPlatosActivity).apply {
                                    text = textoPorNino
                                    textSize = 11f
                                    setTextColor(Color.parseColor("#64748B"))
                                })
                                val badge = android.widget.TextView(this@TodosLosPlatosActivity).apply {
                                    text = textoTotal
                                    textSize = 12.5f
                                    setTypeface(null, android.graphics.Typeface.BOLD)
                                    setTextColor(Color.parseColor("#15803D"))
                                    setBackgroundResource(R.drawable.bg_pill_light_green)
                                    setPadding((12 * density).toInt(), (5 * density).toInt(), (12 * density).toInt(), (5 * density).toInt())
                                }
                                row.addView(colIzq)
                                row.addView(badge)
                                card.addView(row)
                                dialogBinding.layoutFilasGramajeModal.addView(card)
                            }
                        }
                    }
                }
            } catch (_: Exception) { }
        }

        val dialog = MaterialAlertDialogBuilder(this)
            .setView(dialogBinding.root)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        dialogBinding.btnCerrarModal.setOnClickListener {
            dialog.dismiss()
        }

        dialogBinding.btnVerComponentesModal.setOnClickListener {
            dialog.dismiss()
            val intent = Intent(this, IniciarPreparacionActivity::class.java).apply {
                putExtra("nombre_menu", plato.nombrePlato)
                putExtra("id_plato", plato.idPlato)
            }
            startActivity(intent)
        }

        // Calcular platos servidos el día inmediatamente anterior
        val calAyer = Calendar.getInstance().apply {
            try {
                time = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(fechaSeleccionada) ?: Date()
            } catch (_: Exception) { }
            add(Calendar.DAY_OF_MONTH, -1)
        }
        val fechaAyer = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calAyer.time)
        val platosAyer = listOfNotNull(
            sessionMgr.getPlatoDiaServicio(this, fechaAyer, "Desayuno"),
            sessionMgr.getPlatoDiaServicio(this, fechaAyer, "Almuerzo"),
            sessionMgr.getPlatoDiaServicio(this, fechaAyer, "Merienda")
        ).toSet()

        // Configurar botones de servicio (Desayuno, Almuerzo, Merienda)
        fun estaServicioBloqueado(servicio: String): Boolean {
            val pDia = sessionMgr.getPlatoDiaServicio(this, fechaSeleccionada, servicio)
            val est = sessionMgr.getEstadoMenuDiaServicio(this, fechaSeleccionada, servicio).trim()
            return !pDia.isNullOrBlank() &&
                est.isNotBlank() &&
                !est.equals("Borrador", ignoreCase = true) &&
                !est.equals("Rechazado", ignoreCase = true)
        }

        fun actualizarBotonesServicios() {
            val hoyClave = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            val calFechaSel = Calendar.getInstance().apply {
                try {
                    time = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(fechaSeleccionada) ?: Date()
                } catch (_: Exception) { }
            }
            val dowSel = calFechaSel.get(Calendar.DAY_OF_WEEK)
            val esFinSemanaFecha = dowSel == Calendar.SATURDAY || dowSel == Calendar.SUNDAY
            val esDiaPasadoFecha = fechaSeleccionada < hoyClave

            val platoDesayuno = sessionMgr.getPlatoDiaServicio(this, fechaSeleccionada, "Desayuno")
            val platoAlmuerzo = sessionMgr.getPlatoDiaServicio(this, fechaSeleccionada, "Almuerzo")
            val platoMerienda = sessionMgr.getPlatoDiaServicio(this, fechaSeleccionada, "Merienda")

            val esDesayuno = platoDesayuno != null && platoDesayuno.equals(nombrePlatoActual, ignoreCase = true)
            val esAlmuerzo = platoAlmuerzo != null && platoAlmuerzo.equals(nombrePlatoActual, ignoreCase = true)
            val esMerienda = platoMerienda != null && platoMerienda.equals(nombrePlatoActual, ignoreCase = true)
            val esDeAyer = platosAyer.contains(nombrePlatoActual)

            val bloqDesayuno = esFinSemanaFecha || esDiaPasadoFecha || estaServicioBloqueado("Desayuno")
            val bloqAlmuerzo = esFinSemanaFecha || esDiaPasadoFecha || estaServicioBloqueado("Almuerzo")
            val bloqMerienda = esFinSemanaFecha || esDiaPasadoFecha || estaServicioBloqueado("Merienda")

            fun pintarBoton(
                btn: MaterialButton,
                servicioNombre: String,
                esEste: Boolean,
                platoAsignado: String?,
                bloqueado: Boolean
            ) {
                btn.isEnabled = !bloqueado
                btn.alpha = if (bloqueado) 0.55f else 1.0f
                if (bloqueado) {
                    btn.text = if (esEste) "✓ $servicioNombre (En espera)" else "$servicioNombre (En espera)"
                    btn.setBackgroundColor(Color.TRANSPARENT)
                    btn.setTextColor(Color.parseColor("#94A3B8"))
                    btn.strokeColor = android.content.res.ColorStateList.valueOf(Color.parseColor("#CBD5E1"))
                    btn.strokeWidth = (1 * resources.displayMetrics.density).toInt()
                } else if (esEste) {
                    btn.text = "✓ $servicioNombre"
                    btn.setBackgroundColor(Color.parseColor("#F4B41F"))
                    btn.setTextColor(Color.parseColor("#1E293B"))
                    btn.strokeWidth = 0
                } else if (esDeAyer) {
                    btn.text = "$servicioNombre"
                    btn.setBackgroundColor(Color.TRANSPARENT)
                    btn.setTextColor(Color.parseColor("#94A3B8"))
                    btn.strokeColor = android.content.res.ColorStateList.valueOf(Color.parseColor("#E2E8F0"))
                    btn.strokeWidth = (1 * resources.displayMetrics.density).toInt()
                } else if (!platoAsignado.isNullOrBlank()) {
                    btn.text = "$servicioNombre*"
                    btn.setBackgroundColor(Color.TRANSPARENT)
                    btn.setTextColor(Color.parseColor("#64748B"))
                    btn.strokeColor = android.content.res.ColorStateList.valueOf(Color.parseColor("#CBD5E1"))
                    btn.strokeWidth = (1.5 * resources.displayMetrics.density).toInt()
                } else {
                    btn.text = servicioNombre
                    btn.setBackgroundColor(Color.parseColor("#FEF3C7"))
                    btn.setTextColor(Color.parseColor("#92400E"))
                    btn.strokeColor = android.content.res.ColorStateList.valueOf(Color.parseColor("#FDE68A"))
                    btn.strokeWidth = (1.5 * resources.displayMetrics.density).toInt()
                }
            }

            pintarBoton(dialogBinding.btnAsignarDesayunoModal, "Desayuno", esDesayuno, platoDesayuno, bloqDesayuno)
            pintarBoton(dialogBinding.btnAsignarAlmuerzoModal, "Almuerzo", esAlmuerzo, platoAlmuerzo, bloqAlmuerzo)
            pintarBoton(dialogBinding.btnAsignarMeriendaModal, "Merienda", esMerienda, platoMerienda, bloqMerienda)

            val secciones = SedeContratoManager.obtenerSeccionesActivas(this)
            val visDes = secciones.isEmpty() || secciones.contains("Desayuno")
            val visAlm = secciones.contains("Almuerzo")
            val visMer = secciones.contains("Merienda")
            dialogBinding.btnAsignarDesayunoModal.visibility = if (visDes) View.VISIBLE else View.GONE
            dialogBinding.btnAsignarAlmuerzoModal.visibility = if (visAlm) View.VISIBLE else View.GONE
            dialogBinding.btnAsignarMeriendaModal.visibility = if (visMer) View.VISIBLE else View.GONE

            val todosBloqueados = (!visDes || bloqDesayuno) && (!visAlm || bloqAlmuerzo) && (!visMer || bloqMerienda)

            // Texto informativo del estado (sin sobrescribir el badge de inventario)
            when {
                esFinSemanaFecha -> {
                    dialogBinding.tvTituloAsignarServicio.text = "Fin de semana ($fechaLegible):"
                    dialogBinding.tvEstadoAsignacionPlato.text = "Solo se asignan menús de lunes a viernes."
                }
                esDiaPasadoFecha -> {
                    dialogBinding.tvTituloAsignarServicio.text = "Día anterior ($fechaLegible):"
                    dialogBinding.tvEstadoAsignacionPlato.text = "Solo lectura. No se pueden editar menús de días anteriores."
                }
                todosBloqueados -> {
                    dialogBinding.tvTituloAsignarServicio.text = "Programación enviada al supervisor ($fechaLegible):"
                    dialogBinding.tvEstadoAsignacionPlato.text = "En espera de aprobación. No se pueden modificar platos para esta fecha."
                }
                esDeAyer -> {
                    dialogBinding.tvEstadoAsignacionPlato.text = "⚠️ Este plato fue servido el día anterior. No se puede repetir en días consecutivos."
                }
                esDesayuno -> {
                    dialogBinding.tvEstadoAsignacionPlato.text = "✓ Asignado como Desayuno para el $fechaLegible. Toca para quitar o cambiar."
                }
                esAlmuerzo -> {
                    dialogBinding.tvEstadoAsignacionPlato.text = "✓ Asignado como Almuerzo para el $fechaLegible. Toca para quitar o cambiar."
                }
                esMerienda -> {
                    dialogBinding.tvEstadoAsignacionPlato.text = "✓ Asignado como Merienda para el $fechaLegible. Toca para quitar o cambiar."
                }
                else -> {
                    val disponibles = mutableListOf<String>()
                    if (visDes && platoDesayuno.isNullOrBlank()) disponibles.add("Desayuno")
                    if (visAlm && platoAlmuerzo.isNullOrBlank()) disponibles.add("Almuerzo")
                    if (visMer && platoMerienda.isNullOrBlank()) disponibles.add("Merienda")

                    if (disponibles.isNotEmpty()) {
                        dialogBinding.tvEstadoAsignacionPlato.text = "Servicios disponibles ($fechaLegible): ${disponibles.joinToString(", ")}"
                    } else {
                        dialogBinding.tvEstadoAsignacionPlato.text = "Toca un servicio para reemplazar su plato el día $fechaLegible."
                    }
                }
            }
        }

        actualizarBotonesServicios()

        fun procesarClickServicio(servicio: String) {
            val platoExistenteDia = sessionMgr.getPlatoDiaServicio(this, fechaSeleccionada, servicio)
            val estadoDia = sessionMgr.getEstadoMenuDiaServicio(this, fechaSeleccionada, servicio).trim()
            if (!platoExistenteDia.isNullOrBlank() &&
                estadoDia.isNotBlank() &&
                !estadoDia.equals("Borrador", ignoreCase = true) &&
                !estadoDia.equals("Rechazado", ignoreCase = true)
            ) {
                MaterialAlertDialogBuilder(this)
                    .setTitle("Menú en espera de aprobación")
                    .setMessage("El servicio de $servicio para esta fecha ya fue enviado al supervisor ($estadoDia). Solo puedes cambiarlo si es rechazado.")
                    .setPositiveButton("Entendido", null)
                    .show()
                return
            }

            // 1. Validar si ya fue servido ayer
            if (platosAyer.contains(nombrePlatoActual)) {
                val nombreAyer = SimpleDateFormat("EEEE d", Locale.forLanguageTag("es-CO")).format(calAyer.time)
                MaterialAlertDialogBuilder(this)
                    .setTitle("Plato no disponible para este día")
                    .setMessage("El plato \"$nombrePlatoActual\" ya fue seleccionado para el día anterior ($nombreAyer).\n\nPor normatividad PAE de rotación de menú, no se puede repetir en días consecutivos.")
                    .setPositiveButton("Entendido", null)
                    .show()
                return
            }

            val platoServicioActual = sessionMgr.getPlatoDiaServicio(this, fechaSeleccionada, servicio)
                ?: sessionMgr.getPlatoServicio(this, servicio)

            // 2. Si este plato YA está asignado a este servicio -> Confirmar si desea quitarlo
            if (platoServicioActual != null && platoServicioActual.equals(nombrePlatoActual, ignoreCase = true)) {
                val dialogBinding = com.example.movilmanupuladora.databinding.DialogQuitarPlatoBinding.inflate(layoutInflater)
                dialogBinding.txtMensajeQuitarPlato.text = "¿Estás segura de que deseas quitar \"$nombrePlatoActual\" de $servicio para el día $fechaLegible?"

                val confirmDialog = com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                    .setView(dialogBinding.root)
                    .create()

                confirmDialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

                dialogBinding.btnSiQuitar.setOnClickListener {
                    confirmDialog.dismiss()
                    lifecycleScope.launch {
                        PreparacionSyncManager.desasignarPlatoServicio(
                            context = this@TodosLosPlatosActivity,
                            servicio = servicio,
                            fecha = fechaSeleccionada
                        )
                    }
                    Toast.makeText(this@TodosLosPlatosActivity, "Plato quitado de $servicio", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                }

                dialogBinding.btnCancelarQuitar.setOnClickListener {
                    confirmDialog.dismiss()
                }

                confirmDialog.show()
                return
            }

            // 3. Si el servicio ya tiene OTRO plato asignado -> Alerta de confirmación para cambiar
            if (!platoServicioActual.isNullOrBlank() && !platoServicioActual.equals(nombrePlatoActual, ignoreCase = true)) {
                MaterialAlertDialogBuilder(this)
                    .setTitle("Servicio ya seleccionado")
                    .setMessage("El servicio de $servicio para el día $fechaLegible ya tiene asignado \"$platoServicioActual\". ¿Deseas cambiar esta selección por \"$nombrePlatoActual\"?")
                    .setPositiveButton("Sí, cambiar") { _, _ ->
                        lifecycleScope.launch {
                            PreparacionSyncManager.guardarPlatoServicio(
                                context = this@TodosLosPlatosActivity,
                                servicio = servicio,
                                nombrePlato = nombrePlatoActual,
                                idPlato = plato.idPlato,
                                fecha = fechaSeleccionada
                            )
                        }
                        Toast.makeText(this@TodosLosPlatosActivity, "Plato asignado a $servicio", Toast.LENGTH_SHORT).show()
                        dialog.dismiss()
                    }
                    .setNegativeButton("Cancelar", null)
                    .show()
                return
            }

            // 4. Asignación directa si el servicio estaba libre
            lifecycleScope.launch {
                PreparacionSyncManager.guardarPlatoServicio(
                    context = this@TodosLosPlatosActivity,
                    servicio = servicio,
                    nombrePlato = nombrePlatoActual,
                    idPlato = plato.idPlato,
                    fecha = fechaSeleccionada
                )
            }
            Toast.makeText(this, "Plato asignado a $servicio", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
        }

        dialogBinding.btnAsignarDesayunoModal.setOnClickListener {
            procesarClickServicio("Desayuno")
        }

        dialogBinding.btnAsignarAlmuerzoModal.setOnClickListener {
            procesarClickServicio("Almuerzo")
        }

        dialogBinding.btnAsignarMeriendaModal.setOnClickListener {
            procesarClickServicio("Merienda")
        }

        dialog.show()
    }
}