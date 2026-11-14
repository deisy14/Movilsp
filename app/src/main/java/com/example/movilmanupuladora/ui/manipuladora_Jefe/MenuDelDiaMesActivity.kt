package com.example.movilmanupuladora.ui.manipuladora_Jefe

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.movilmanupuladora.R
import com.example.movilmanupuladora.data.api.RetrofitClient
import com.example.movilmanupuladora.data.model.MenuPaeCatalog
import com.example.movilmanupuladora.data.model.PlatoResponse
import com.example.movilmanupuladora.data.repository.MenuRepository
import com.example.movilmanupuladora.databinding.ActivityMenuDelDiaMesBinding
import com.example.movilmanupuladora.databinding.DialogDetallePlatoBinding
import com.example.movilmanupuladora.databinding.ItemPlatoCatalogoBinding
import com.example.movilmanupuladora.utils.PreparacionSyncManager
import com.example.movilmanupuladora.utils.SessionManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class MenuDelDiaMesActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMenuDelDiaMesBinding

    private var mesActual: Calendar = Calendar.getInstance()
    private var diaSeleccionado: Calendar = Calendar.getInstance()

    data class DetallePlatoDia(
        val nombre: String,
        val seccion: String,
        val componente: String,
        val porciones: String,
        val imagen: Int
    )

    data class PlatoOpcion(
        val id: Int,
        val nombre: String,
        val componente: String,
        val imagenRes: Int
    )

    private var platoActualDesayuno: DetallePlatoDia? = null
    private var platoActualAlmuerzo: DetallePlatoDia? = null
    private var platoActualMerienda: DetallePlatoDia? = null
    private val sessionManager by lazy { SessionManager(this) }
    private val menuRepository = MenuRepository(RetrofitClient.apiService)
    private val platosCatalogoPae = mutableListOf<PlatoResponse>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityMenuDelDiaMesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Obtener fecha inicial pasada por Intent si existe
        val milis = intent.getLongExtra("fecha_milisegundos", System.currentTimeMillis())
        diaSeleccionado.timeInMillis = milis
        mesActual.timeInMillis = milis

        configurarEventos()
        generarGridMes()
        actualizarDetallePlatos()
        cargarPlatosBackend()
    }

    override fun onResume() {
        super.onResume()
        actualizarDetallePlatos()
        generarGridMes()
    }

    private fun cargarPlatosBackend() {
        lifecycleScope.launch {
            try {
                val res = menuRepository.obtenerPlatos()
                if (res.isSuccessful && !res.body().isNullOrEmpty()) {
                    platosCatalogoPae.clear()
                    platosCatalogoPae.addAll(res.body()!!)
                }
            } catch (_: Exception) { }
        }
    }

    private fun obtenerPlatosPaeDisponibles(): List<PlatoOpcion> {
        val lista = mutableListOf<PlatoOpcion>()
        val mapa = mutableSetOf<String>()

        // 1. Platos oficiales registrados y supervisados en el backend PAE
        for (p in platosCatalogoPae) {
            val nom = p.nombrePlato?.trim()
            if (!nom.isNullOrBlank() && mapa.add(nom.lowercase())) {
                val img = if (p.idPlato in 1..20) {
                    MenuPaeCatalog.obtenerMenuPorId(p.idPlato).imagenRes
                } else {
                    MenuPaeCatalog.obtenerImagenPorNombre(nom)
                }
                lista.add(
                    PlatoOpcion(
                        id = p.idPlato,
                        nombre = nom,
                        componente = p.componente ?: "Sopas, Sazón y Proteína estipulada PAE",
                        imagenRes = img
                    )
                )
            }
        }

        // 2. Platos oficiales de los 20 menús del ciclo PAE
        for (m in MenuPaeCatalog.menus) {
            if (mapa.add(m.nombre.lowercase())) {
                lista.add(
                    PlatoOpcion(
                        id = m.id,
                        nombre = m.nombre,
                        componente = m.componente,
                        imagenRes = m.imagenRes
                    )
                )
            }
        }

        return lista
    }

    private fun configurarEventos() {
        binding.btnVolver.setOnClickListener {
            finish()
        }

        binding.btnMesAnterior.setOnClickListener {
            mesActual.add(Calendar.MONTH, -1)
            generarGridMes()
        }

        binding.btnMesSiguiente.setOnClickListener {
            mesActual.add(Calendar.MONTH, 1)
            generarGridMes()
        }

        // Programar o cambiar plato para la jornada seleccionada
        binding.btnProgramarDesayuno.setOnClickListener { mostrarDialogoSeleccionarPlato("Desayuno") }
        binding.cardDetalleDesayuno.setOnClickListener { mostrarDialogoSeleccionarPlato("Desayuno") }
        binding.btnVerDetalleDesayuno.setOnClickListener { platoActualDesayuno?.let { mostrarModalDetalle(it) } }

        binding.btnProgramarAlmuerzo.setOnClickListener { mostrarDialogoSeleccionarPlato("Almuerzo") }
        binding.cardDetalleAlmuerzo.setOnClickListener { mostrarDialogoSeleccionarPlato("Almuerzo") }
        binding.btnVerDetalleAlmuerzo.setOnClickListener { platoActualAlmuerzo?.let { mostrarModalDetalle(it) } }

        binding.btnProgramarMerienda.setOnClickListener { mostrarDialogoSeleccionarPlato("Merienda") }
        binding.cardDetalleMerienda.setOnClickListener { mostrarDialogoSeleccionarPlato("Merienda") }
        binding.btnVerDetalleMerienda.setOnClickListener { platoActualMerienda?.let { mostrarModalDetalle(it) } }

        // Botón para acceder directamente a la planificación semanal completa
        binding.btnPlanificarSemanaSiguiente.setOnClickListener {
            val intent = Intent(this, PlanificacionSemanalJefaActivity::class.java).apply {
                putExtra("semana_offset", 1) // Siguiente semana
            }
            startActivity(intent)
        }
    }

    // =========================================================
    // GENERAR CUADRÍCULA DE DÍAS DEL MES (7 COLUMNAS)
    // =========================================================

    private fun generarGridMes() {
        binding.contenedorGridCalendario.removeAllViews()

        // Actualizar título del mes (ej. "Octubre 2026")
        val formatoMes = SimpleDateFormat("MMMM yyyy", Locale.forLanguageTag("es-CO"))
        val mesAnioTexto = formatoMes.format(mesActual.time).replaceFirstChar {
            if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
        }
        binding.txtMesAnio.text = mesAnioTexto

        val calGrid = mesActual.clone() as Calendar
        calGrid.set(Calendar.DAY_OF_MONTH, 1)

        // Encontrar el día de inicio respecto al Lunes (1 = Lun, 2 = Mar, ..., 7 = Dom)
        val primerDiaSemana = calGrid.get(Calendar.DAY_OF_WEEK)
        val offsetLunes = when (primerDiaSemana) {
            Calendar.MONDAY -> 0
            Calendar.TUESDAY -> 1
            Calendar.WEDNESDAY -> 2
            Calendar.THURSDAY -> 3
            Calendar.FRIDAY -> 4
            Calendar.SATURDAY -> 5
            Calendar.SUNDAY -> 6
            else -> 0
        }

        val diasEnMes = calGrid.getActualMaximum(Calendar.DAY_OF_MONTH)

        // Días del mes anterior para rellenar
        val calMesAnterior = calGrid.clone() as Calendar
        calMesAnterior.add(Calendar.MONTH, -1)
        val maxDiasMesAnterior = calMesAnterior.getActualMaximum(Calendar.DAY_OF_MONTH)

        var diaMesActual = 1
        var diaMesSiguiente = 1
        val totalSemanas = if (offsetLunes + diasEnMes > 35) 6 else 5

        for (semana in 0 until totalSemanas) {
            val fila = LinearLayout(this).apply {
                layoutWidth(LinearLayout.LayoutParams.MATCH_PARENT)
                layoutHeight(LinearLayout.LayoutParams.WRAP_CONTENT)
                orientation = LinearLayout.HORIZONTAL
                weightSum = 7f
                setPadding(0, 4, 0, 4)
            }

            for (columna in 0 until 7) {
                val indiceTotal = semana * 7 + columna

                val cellContainer = FrameLayout(this).apply {
                    val p = LinearLayout.LayoutParams(0, (40 * resources.displayMetrics.density).toInt(), 1f)
                    layoutParams = p
                }

                val txtDia = TextView(this).apply {
                    val p = FrameLayout.LayoutParams(
                        (34 * resources.displayMetrics.density).toInt(),
                        (34 * resources.displayMetrics.density).toInt()
                    ).apply { gravity = Gravity.CENTER }
                    layoutParams = p
                    gravity = Gravity.CENTER
                    textSize = 13f
                }

                when {
                    indiceTotal < offsetLunes -> {
                        // Días del mes anterior
                        val numDia = maxDiasMesAnterior - offsetLunes + indiceTotal + 1
                        txtDia.text = numDia.toString()
                        txtDia.setTextColor(Color.parseColor("#CBD5E1"))
                    }
                    diaMesActual <= diasEnMes -> {
                        // Días del mes actual
                        val diaLoop = diaMesActual
                        txtDia.text = diaLoop.toString()

                        val calDiaActual = mesActual.clone() as Calendar
                        calDiaActual.set(Calendar.DAY_OF_MONTH, diaLoop)
                        calDiaActual.set(Calendar.HOUR_OF_DAY, 0)
                        calDiaActual.set(Calendar.MINUTE, 0)
                        calDiaActual.set(Calendar.SECOND, 0)
                        calDiaActual.set(Calendar.MILLISECOND, 0)

                        val hoyCero = Calendar.getInstance().apply {
                            set(Calendar.HOUR_OF_DAY, 0)
                            set(Calendar.MINUTE, 0)
                            set(Calendar.SECOND, 0)
                            set(Calendar.MILLISECOND, 0)
                        }

                        val isToday = esMismoDia(calDiaActual, Calendar.getInstance())
                        val isPast = calDiaActual.before(hoyCero)
                        val isSelected = esMismoDia(calDiaActual, diaSeleccionado)

                        val fechaStrLoop = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calDiaActual.time)
                        val tieneProgramado = !sessionManager.getPlatoDiaServicio(this, fechaStrLoop, "Desayuno").isNullOrBlank() ||
                                              !sessionManager.getPlatoDiaServicio(this, fechaStrLoop, "Almuerzo").isNullOrBlank() ||
                                              !sessionManager.getPlatoDiaServicio(this, fechaStrLoop, "Merienda").isNullOrBlank()

                        when {
                            isToday -> {
                                txtDia.setBackgroundResource(R.drawable.circulo_amarillo_badge)
                                txtDia.setTextColor(Color.parseColor("#1E293B"))
                                txtDia.typeface = android.graphics.Typeface.DEFAULT_BOLD
                            }
                            isSelected -> {
                                txtDia.setBackgroundResource(R.drawable.bg_date_selected)
                                txtDia.setTextColor(Color.parseColor("#7A5500"))
                                txtDia.typeface = android.graphics.Typeface.DEFAULT_BOLD
                            }
                            tieneProgramado -> {
                                txtDia.setBackgroundResource(R.drawable.bg_pill_green)
                                txtDia.setTextColor(Color.parseColor("#2A5226"))
                                txtDia.typeface = android.graphics.Typeface.DEFAULT_BOLD
                            }
                            isPast -> {
                                txtDia.setBackgroundResource(R.drawable.bg_date_selected)
                                txtDia.setTextColor(Color.parseColor("#7A5500"))
                                txtDia.typeface = android.graphics.Typeface.DEFAULT_BOLD
                            }
                            else -> {
                                txtDia.setBackgroundColor(Color.TRANSPARENT)
                                txtDia.setTextColor(Color.parseColor("#1E293B"))
                                txtDia.typeface = android.graphics.Typeface.DEFAULT
                            }
                        }

                        cellContainer.isClickable = true
                        cellContainer.isFocusable = true
                        cellContainer.setOnClickListener {
                            diaSeleccionado = calDiaActual
                            generarGridMes()
                            actualizarDetallePlatos()
                        }

                        diaMesActual++
                    }
                    else -> {
                        // Días del mes siguiente
                        txtDia.text = diaMesSiguiente.toString()
                        txtDia.setTextColor(Color.parseColor("#CBD5E1"))
                        diaMesSiguiente++
                    }
                }

                cellContainer.addView(txtDia)
                fila.addView(cellContainer)
            }

            binding.contenedorGridCalendario.addView(fila)
        }
    }

    private fun esMismoDia(c1: Calendar, c2: Calendar): Boolean {
        return c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR) &&
                c1.get(Calendar.DAY_OF_YEAR) == c2.get(Calendar.DAY_OF_YEAR)
    }

    // =========================================================
    // ACTUALIZAR DETALLE DE PLATOS (DESAYUNO, ALMUERZO, MERIENDA)
    // =========================================================

    private fun actualizarDetallePlatos() {
        val fechaClave = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(diaSeleccionado.time)
        val customDes = sessionManager.getPlatoDiaServicio(this, fechaClave, "Desayuno")
        val customAlm = sessionManager.getPlatoDiaServicio(this, fechaClave, "Almuerzo")
        val customMer = sessionManager.getPlatoDiaServicio(this, fechaClave, "Merienda")

        val menuDia = MenuPaeCatalog.obtenerMenuDiaCompleto(diaSeleccionado)

        // 1. DESAYUNO
        val nomDes = customDes ?: menuDia.desayuno.nombre
        val imgDes = MenuPaeCatalog.obtenerImagenPorNombre(nomDes)
        platoActualDesayuno = DetallePlatoDia(
            nombre = nomDes,
            seccion = "Desayuno",
            componente = menuDia.desayuno.componente,
            porciones = "42",
            imagen = imgDes
        )
        binding.imgDetalleDesayuno.setImageResource(imgDes)
        binding.txtDetalleNombreDesayuno.text = nomDes
        if (!customDes.isNullOrBlank()) {
            binding.badgeEstadoDesayuno.text = "✓ Programado"
            binding.badgeEstadoDesayuno.setBackgroundResource(R.drawable.bg_pill_green)
            binding.badgeEstadoDesayuno.setTextColor(Color.parseColor("#2A5226"))
        } else {
            binding.badgeEstadoDesayuno.text = "Sugerido PAE"
            binding.badgeEstadoDesayuno.setBackgroundResource(R.drawable.bg_date_selected)
            binding.badgeEstadoDesayuno.setTextColor(Color.parseColor("#854D0E"))
        }

        // 2. ALMUERZO
        val nomAlm = customAlm ?: menuDia.almuerzo.nombre
        val imgAlm = MenuPaeCatalog.obtenerImagenPorNombre(nomAlm)
        platoActualAlmuerzo = DetallePlatoDia(
            nombre = nomAlm,
            seccion = "Almuerzo",
            componente = menuDia.almuerzo.componente,
            porciones = "60",
            imagen = imgAlm
        )
        binding.imgDetalleAlmuerzo.setImageResource(imgAlm)
        binding.txtDetalleNombreAlmuerzo.text = nomAlm
        if (!customAlm.isNullOrBlank()) {
            binding.badgeEstadoAlmuerzo.text = "✓ Programado"
            binding.badgeEstadoAlmuerzo.setBackgroundResource(R.drawable.bg_pill_green)
            binding.badgeEstadoAlmuerzo.setTextColor(Color.parseColor("#2A5226"))
        } else {
            binding.badgeEstadoAlmuerzo.text = "Sugerido PAE"
            binding.badgeEstadoAlmuerzo.setBackgroundResource(R.drawable.bg_date_selected)
            binding.badgeEstadoAlmuerzo.setTextColor(Color.parseColor("#854D0E"))
        }

        // 3. MERIENDA
        val nomMer = customMer ?: menuDia.merienda.nombre
        val imgMer = MenuPaeCatalog.obtenerImagenPorNombre(nomMer)
        platoActualMerienda = DetallePlatoDia(
            nombre = nomMer,
            seccion = "Merienda",
            componente = menuDia.merienda.componente,
            porciones = "30",
            imagen = imgMer
        )
        binding.imgDetalleMerienda.setImageResource(imgMer)
        binding.txtDetalleNombreMerienda.text = nomMer
        if (!customMer.isNullOrBlank()) {
            binding.badgeEstadoMerienda.text = "✓ Programado"
            binding.badgeEstadoMerienda.setBackgroundResource(R.drawable.bg_pill_green)
            binding.badgeEstadoMerienda.setTextColor(Color.parseColor("#2A5226"))
        } else {
            binding.badgeEstadoMerienda.text = "Sugerido PAE"
            binding.badgeEstadoMerienda.setBackgroundResource(R.drawable.bg_date_selected)
            binding.badgeEstadoMerienda.setTextColor(Color.parseColor("#854D0E"))
        }

        val sedeTexto = sessionManager.getSedeNombre().orEmpty().lowercase()
        val esSoloDesayuno = sedeTexto.contains("piagua")
        if (esSoloDesayuno) {
            binding.cardDetalleAlmuerzo.visibility = View.GONE
            binding.cardDetalleMerienda.visibility = View.GONE
        } else {
            binding.cardDetalleAlmuerzo.visibility = View.VISIBLE
            binding.cardDetalleMerienda.visibility = View.VISIBLE
        }
    }

    // =========================================================
    // MODAL DE SELECCIÓN DE PLATOS DEL PAE PARA LA JORNADA
    // =========================================================

    private fun mostrarDialogoSeleccionarPlato(servicio: String) {
        val sedeTexto = sessionManager.getSedeNombre().orEmpty().lowercase()
        val esSoloDesayuno = sedeTexto.contains("piagua")
        if (esSoloDesayuno && !servicio.equals("Desayuno", ignoreCase = true)) {
            MaterialAlertDialogBuilder(this)
                .setTitle("Servicio no autorizado")
                .setMessage("La Sede Piagua opera exclusivamente para el servicio de Desayuno.")
                .setPositiveButton("Entendido", null)
                .show()
            return
        }

        val fechaClave = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(diaSeleccionado.time)
        val fechaFormato = SimpleDateFormat("EEEE, d 'de' MMMM", Locale.forLanguageTag("es-CO")).format(diaSeleccionado.time)
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }

        // Calcular platos servidos el día inmediatamente anterior (Regla de Rotación PAE)
        val calAyer = (diaSeleccionado.clone() as Calendar).apply { add(Calendar.DAY_OF_MONTH, -1) }
        val fechaAyer = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calAyer.time)
        val platosAyer = listOfNotNull(
            sessionManager.getPlatoDiaServicio(this, fechaAyer, "Desayuno"),
            sessionManager.getPlatoDiaServicio(this, fechaAyer, "Almuerzo"),
            sessionManager.getPlatoDiaServicio(this, fechaAyer, "Merienda")
        ).toSet()

        val platoAsignadoActual = sessionManager.getPlatoDiaServicio(this, fechaClave, servicio)
        val listaPlatosOpciones = obtenerPlatosPaeDisponibles()

        // Contenedor principal con ScrollView
        val scroll = androidx.core.widget.NestedScrollView(this).apply {
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        }
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(
                (20 * resources.displayMetrics.density).toInt(),
                (14 * resources.displayMetrics.density).toInt(),
                (20 * resources.displayMetrics.density).toInt(),
                (20 * resources.displayMetrics.density).toInt()
            )
        }
        scroll.addView(layout)

        // Subtítulo explicativo
        val tvSub = TextView(this).apply {
            text = "📅 $fechaFormato\nSelecciona el plato estipulado del PAE para la jornada:"
            textSize = 13f
            setTextColor(Color.parseColor("#475569"))
            setPadding(0, 0, 0, (14 * resources.displayMetrics.density).toInt())
        }
        layout.addView(tvSub)

        var dialogRef: androidx.appcompat.app.AlertDialog? = null

        for (plato in listaPlatosOpciones) {
            val esDeAyer = platosAyer.contains(plato.nombre)
            val esActual = platoAsignadoActual != null && platoAsignadoActual.equals(plato.nombre, ignoreCase = true)

            val itemBinding = ItemPlatoCatalogoBinding.inflate(layoutInflater, layout, false)
            itemBinding.tvNombrePlatoCatalogo.text = plato.nombre
            itemBinding.tvComponenteCatalogo.text = plato.componente
            itemBinding.imgPlatoCatalogo.setImageResource(plato.imagenRes)

            when {
                esActual -> {
                    itemBinding.root.strokeColor = Color.parseColor("#15803D")
                    itemBinding.root.strokeWidth = (2 * resources.displayMetrics.density).toInt()
                    itemBinding.tvComponenteCatalogo.text = "✓ Actualmente seleccionado para $servicio"
                    itemBinding.tvComponenteCatalogo.setTextColor(Color.parseColor("#15803D"))
                }
                esDeAyer -> {
                    itemBinding.root.alpha = 0.5f
                    itemBinding.tvComponenteCatalogo.text = "⛔ No disponible: servido el día anterior (rotación PAE)"
                    itemBinding.tvComponenteCatalogo.setTextColor(Color.parseColor("#BE123C"))
                }
                else -> {
                    itemBinding.root.strokeColor = Color.parseColor("#E2E8F0")
                    itemBinding.root.strokeWidth = (1 * resources.displayMetrics.density).toInt()
                }
            }

            itemBinding.root.setOnClickListener {
                if (esDeAyer) {
                    val diaAyerTexto = SimpleDateFormat("EEEE d", Locale.forLanguageTag("es-CO")).format(calAyer.time)
                    MaterialAlertDialogBuilder(this)
                        .setTitle("Plato no disponible por rotación")
                        .setMessage("El plato \"${plato.nombre}\" ya fue seleccionado para el día anterior ($diaAyerTexto).\n\nPor lineamiento PAE de variedad nutricional, no se puede repetir en días consecutivos.")
                        .setPositiveButton("Entendido", null)
                        .show()
                    return@setOnClickListener
                }

                // Guardar plato en sesión local y sincronizar con backend
                sessionManager.savePlatoDiaServicio(this@MenuDelDiaMesActivity, fechaClave, servicio, plato.nombre, plato.id)
                lifecycleScope.launch {
                    PreparacionSyncManager.guardarPlatoServicio(
                        this@MenuDelDiaMesActivity,
                        servicio,
                        plato.nombre,
                        plato.id,
                        fechaClave
                    )
                }

                Toast.makeText(this@MenuDelDiaMesActivity, "✓ $servicio programado: ${plato.nombre}", Toast.LENGTH_SHORT).show()
                actualizarDetallePlatos()
                generarGridMes()
                dialogRef?.dismiss()
            }

            layout.addView(itemBinding.root)
        }

        dialogRef = MaterialAlertDialogBuilder(this)
            .setTitle("Programar $servicio")
            .setView(scroll)
            .setNegativeButton("Cerrar", null)
            .create()

        dialogRef.show()
    }

    private fun mostrarModalDetalle(detalle: DetallePlatoDia) {
        val dialogBinding = DialogDetallePlatoBinding.inflate(layoutInflater)
        dialogBinding.tvTituloPlatoModal.text = detalle.nombre
        dialogBinding.tvSeccionModal.text = detalle.seccion
        dialogBinding.tvComponenteModal.text = "Componente: ${detalle.componente}"
        dialogBinding.tvPorcionModal.text = "100g aprox. por ración"
        dialogBinding.tvTotalPrepararModal.text = "${detalle.porciones} porciones"
        dialogBinding.tvBadgeEstadoModal.text = "Programado"
        dialogBinding.imgPlatoModal.setImageResource(detalle.imagen)

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
                putExtra("nombre_menu", detalle.nombre)
                putExtra("jornada", detalle.seccion)
            }
            startActivity(intent)
        }

        dialogBinding.btnOtroAleatorioModal.text = "Cambiar plato"
        dialogBinding.btnOtroAleatorioModal.setOnClickListener {
            dialog.dismiss()
            mostrarDialogoSeleccionarPlato(detalle.seccion)
        }

        dialog.show()
    }

    private fun LinearLayout.layoutWidth(width: Int) {
        val p = layoutParams ?: LinearLayout.LayoutParams(width, LinearLayout.LayoutParams.WRAP_CONTENT)
        p.width = width
        layoutParams = p
    }

    private fun LinearLayout.layoutHeight(height: Int) {
        val p = layoutParams ?: LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, height)
        p.height = height
        layoutParams = p
    }
}
