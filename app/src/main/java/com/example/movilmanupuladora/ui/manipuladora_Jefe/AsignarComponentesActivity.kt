package com.example.movilmanupuladora.ui.manipuladora_Jefe

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.movilmanupuladora.R
import com.example.movilmanupuladora.data.model.MenuPaeCatalog
import com.example.movilmanupuladora.databinding.ActivityAsignarComponentesBinding
import com.example.movilmanupuladora.utils.NavigationHelperJefa
import com.example.movilmanupuladora.utils.PreparacionSyncManager
import com.example.movilmanupuladora.utils.SessionManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class AsignarComponentesActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAsignarComponentesBinding
    private lateinit var sessionManager: SessionManager

    private var fechaSeleccionada: String = ""
    private var servicioActual: String = "Desayuno"

    // Mapa de asignaciones por servicio: servicio -> (componente -> Manipuladora)
    private val asignacionesPorServicio = mutableMapOf<String, MutableMap<String, ManipuladorasManager.Manipuladora>>()

    private var recetaPlegada: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            window.isStatusBarContrastEnforced = false
        }
        androidx.core.view.WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars = true

        binding = ActivityAsignarComponentesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
            insets
        }

        sessionManager = SessionManager(this)

        // Cargar fecha pasada por Intent o usar fecha actual
        fechaSeleccionada = intent.getStringExtra("fecha_seleccionada") ?: run {
            SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        }

        servicioActual = intent.getStringExtra("servicio") ?: "Desayuno"

        configurarFechaVisual()
        configurarEventos()

        NavigationHelperJefa.setupBarraNavegacionJefaView(
            this,
            binding.barraNavegacionJefa.root,
            NavigationHelperJefa.TabJefa.PREPARACIONES
        )

        lifecycleScope.launch {
            try {
                PreparacionSyncManager.sincronizarDesdeBackend(this@AsignarComponentesActivity)
                ManipuladorasManager.cargarDesdeBackend()
            } catch (_: Exception) { }
            cargarAsignacionesGuardadas()
            actualizarVistaServicio()
        }
    }

    private fun configurarFechaVisual() {
        val sdfEntrada = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val fechaDate = try { sdfEntrada.parse(fechaSeleccionada) } catch (_: Exception) { Date() } ?: Date()

        val sdfSalida = SimpleDateFormat("EEEE, d 'de' MMMM", Locale.forLanguageTag("es-CO"))
        val textoFecha = sdfSalida.format(fechaDate).replaceFirstChar {
            if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
        }
        binding.txtFechaSeleccionada.text = textoFecha
    }

    private fun configurarEventos() {
        binding.btnVolver.setOnClickListener {
            finish()
        }

        binding.btnInfo.setOnClickListener {
            MaterialAlertDialogBuilder(this)
                .setTitle("Asignación de componentes")
                .setMessage("En esta pantalla puedes asignar a cada cocinera el componente que preparará para cada servicio del día (Desayuno, Almuerzo o Merienda).\n\nLas porciones se calculan automáticamente con base en el registro de asistencia de primaria y secundaria.")
                .setPositiveButton("Entendido", null)
                .show()
        }

        binding.btnTabDesayuno.setOnClickListener {
            cambiarServicio("Desayuno")
        }

        binding.btnTabAlmuerzo.setOnClickListener {
            cambiarServicio("Almuerzo")
        }

        binding.btnTabMerienda.setOnClickListener {
            cambiarServicio("Merienda")
        }

        binding.cardReceta.setOnClickListener {
            recetaPlegada = !recetaPlegada
            binding.txtInstruccionesReceta.visibility = if (recetaPlegada) View.GONE else View.VISIBLE
            binding.txtToggleReceta.text = if (recetaPlegada) "▼" else "▲"
        }

        binding.btnGuardarAsignacion.setOnClickListener {
            guardarAsignacionActual()
        }
    }

    private fun cambiarServicio(nuevoServicio: String) {
        servicioActual = nuevoServicio

        // Actualizar visual de pestañas
        val colorActivo = Color.parseColor("#F4B41F")
        val colorInactivo = Color.TRANSPARENT
        val textoActivo = Color.parseColor("#1B3317")
        val textoInactivo = Color.parseColor("#64748B")

        binding.btnTabDesayuno.backgroundTintList = ColorStateList.valueOf(if (nuevoServicio == "Desayuno") colorActivo else colorInactivo)
        binding.btnTabDesayuno.setTextColor(if (nuevoServicio == "Desayuno") textoActivo else textoInactivo)

        binding.btnTabAlmuerzo.backgroundTintList = ColorStateList.valueOf(if (nuevoServicio == "Almuerzo") colorActivo else colorInactivo)
        binding.btnTabAlmuerzo.setTextColor(if (nuevoServicio == "Almuerzo") textoActivo else textoInactivo)

        binding.btnTabMerienda.backgroundTintList = ColorStateList.valueOf(if (nuevoServicio == "Merienda") colorActivo else colorInactivo)
        binding.btnTabMerienda.setTextColor(if (nuevoServicio == "Merienda") textoActivo else textoInactivo)

        actualizarVistaServicio()
    }

    private fun actualizarVistaServicio() {
        // 1. Obtener plato asignado para este día y servicio
        val platoNombre = sessionManager.getPlatoDiaServicio(this, fechaSeleccionada, servicioActual)
            ?: when (servicioActual) {
                "Desayuno" -> "Huevos revueltos, pan y fruta"
                "Almuerzo" -> "Arroz con Pollo Criollo"
                else -> "Refresco y galletas"
            }

        binding.txtCategoriaPlato.text = "${servicioActual.uppercase()} DEL DÍA"
        binding.txtNombrePlato.text = platoNombre
        binding.txtDescripcionPlato.text = obtenerDescripcionPorPlato(platoNombre, servicioActual)

        val imgRes = MenuPaeCatalog.obtenerImagenPorNombre(platoNombre)
        binding.imgPlato.setImageResource(imgRes)

        // 2. Tarjeta de Asistencia
        val prim = AsistenciaManager.obtenerPrimaria(this)
        val sec = AsistenciaManager.obtenerSecundaria(this)

        val (tituloAsist, porciones, etiquetaUnidad) = when (servicioActual) {
            "Desayuno" -> Triple("Asistencia - Primaria", prim, "niños")
            "Almuerzo" -> Triple("Asistencia - Secundaria", sec, "estudiantes")
            else -> Triple("Asistencia - Merienda", (prim + sec), "estudiantes")
        }

        binding.txtTituloAsistencia.text = tituloAsist
        if (porciones > 0) {
            binding.txtSubtituloAsistencia.text = "$porciones porciones para el ${servicioActual.lowercase()}"
            binding.txtNumeroAsistencia.text = porciones.toString()
            binding.txtEtiquetaAsistencia.text = etiquetaUnidad
        } else {
            binding.txtSubtituloAsistencia.text = "Sin registro de asistencia para hoy"
            binding.txtNumeroAsistencia.text = "0"
            binding.txtEtiquetaAsistencia.text = "estudiantes"
        }

        // 3. Receta
        binding.txtInstruccionesReceta.text = obtenerRecetaPorPlato(platoNombre)

        // 4. Componentes
        val componentes = MenuPaeCatalog.obtenerComponentesPorNombre(platoNombre)
        binding.txtTituloComponentes.text = "Componentes del ${servicioActual.lowercase()}"

        renderizarComponentes(componentes, porciones)
    }

    private fun renderizarComponentes(componentes: List<String>, porciones: Int) {
        binding.layoutListaComponentes.removeAllViews()

        val mapaServicio = asignacionesPorServicio.getOrPut(servicioActual) { mutableMapOf() }
        val inflater = LayoutInflater.from(this)

        var asignadosCount = 0

        for (comp in componentes) {
            val itemView = inflater.inflate(R.layout.item_componente_asignar, binding.layoutListaComponentes, false)

            val txtNombre = itemView.findViewById<TextView>(R.id.txtNombreComponente)
            val txtPorciones = itemView.findViewById<TextView>(R.id.txtPorcionesComponente)
            val btnSelector = itemView.findViewById<View>(R.id.btnSelectorCocinera)
            val txtCocinera = itemView.findViewById<TextView>(R.id.txtCocineraAsignada)
            val imgIcono = itemView.findViewById<ImageView>(R.id.imgIconoComponente)

            txtNombre.text = comp

            // Unidades o porciones según el ingrediente
            val sufijo = if (comp.lowercase().contains("pan") || comp.lowercase().contains("fruta") || comp.lowercase().contains("arepa")) {
                "unidades"
            } else {
                "porciones"
            }
            txtPorciones.text = if (porciones > 0) "$porciones $sufijo" else "Pendiente de asistencia"

            // Icono contextual
            val compLower = comp.lowercase()
            when {
                compLower.contains("huevo") -> imgIcono.setImageResource(R.drawable.ic_food)
                compLower.contains("pan") -> imgIcono.setImageResource(R.drawable.ic_food)
                compLower.contains("fruta") || compLower.contains("papaya") || compLower.contains("mango") -> imgIcono.setImageResource(R.drawable.ic_food)
                compLower.contains("arroz") || compLower.contains("pasta") -> imgIcono.setImageResource(R.drawable.ic_food)
                compLower.contains("pollo") || compLower.contains("carne") -> imgIcono.setImageResource(R.drawable.ic_food)
                else -> imgIcono.setImageResource(R.drawable.ic_food)
            }

            val cocineraAsignada = mapaServicio[comp]
            if (cocineraAsignada != null) {
                asignadosCount++
                txtCocinera.text = "${cocineraAsignada.nombre} ▾"
                txtCocinera.setTextColor(Color.parseColor("#1E293B"))
            } else {
                txtCocinera.text = "Asignar ▾"
                txtCocinera.setTextColor(Color.parseColor("#64748B"))
            }

            btnSelector.setOnClickListener {
                mostrarDialogoSeleccionarCocinera(comp, cocineraAsignada)
            }

            binding.layoutListaComponentes.addView(itemView)
        }

        binding.txtBadgeProgreso.text = "✓ $asignadosCount de ${componentes.size}"
    }

    private fun mostrarDialogoSeleccionarCocinera(
        componente: String,
        cocineraActual: ManipuladorasManager.Manipuladora?
    ) {
        val cocinerasActivas = ManipuladorasManager.obtenerManipuladoras().filter { it.activa }
        if (cocinerasActivas.isEmpty()) {
            Toast.makeText(this, "No hay cocineras activas disponibles", Toast.LENGTH_SHORT).show()
            return
        }

        val dialogBinding = com.example.movilmanupuladora.databinding.DialogSeleccionarCocineraBinding.inflate(layoutInflater)
        dialogBinding.txtTituloSeleccionarCocinera.text = "Asignar cocinera para:\n\"$componente\""

        val dialog = com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
            .setView(dialogBinding.root)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val items = mutableListOf<String>()
        cocinerasActivas.forEach { items.add(it.nombre) }
        if (cocineraActual != null) {
            items.add("✕ Quitar asignación")
        }

        val adapter = object : androidx.recyclerview.widget.RecyclerView.Adapter<androidx.recyclerview.widget.RecyclerView.ViewHolder>() {
            override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): androidx.recyclerview.widget.RecyclerView.ViewHolder {
                val itemBinding = com.example.movilmanupuladora.databinding.ItemCocineraDialogBinding.inflate(layoutInflater, parent, false)
                return object : androidx.recyclerview.widget.RecyclerView.ViewHolder(itemBinding.root) {}
            }

            override fun onBindViewHolder(holder: androidx.recyclerview.widget.RecyclerView.ViewHolder, position: Int) {
                val itemBinding = com.example.movilmanupuladora.databinding.ItemCocineraDialogBinding.bind(holder.itemView)
                val texto = items[position]
                itemBinding.txtNombreCocineraItem.text = texto
                if (texto.startsWith("✕")) {
                    itemBinding.txtNombreCocineraItem.setTextColor(android.graphics.Color.parseColor("#DC2626"))
                } else {
                    itemBinding.txtNombreCocineraItem.setTextColor(android.graphics.Color.parseColor("#1E293B"))
                }

                itemBinding.root.setOnClickListener {
                    dialog.dismiss()
                    val mapaServicio = asignacionesPorServicio.getOrPut(servicioActual) { mutableMapOf() }
                    if (cocineraActual != null && position == cocinerasActivas.size) {
                        mapaServicio.remove(componente)
                    } else {
                        val elegida = cocinerasActivas[position]
                        mapaServicio[componente] = elegida
                    }
                    actualizarVistaServicio()
                }
            }

            override fun getItemCount() = items.size
        }

        dialogBinding.recyclerCocinerasDialog.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(this)
        dialogBinding.recyclerCocinerasDialog.adapter = adapter

        dialogBinding.btnCancelarCocinera.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun guardarAsignacionActual() {
        val mapaServicio = asignacionesPorServicio[servicioActual] ?: mutableMapOf()
        val platoNombre = binding.txtNombrePlato.text.toString()

        val prefs = getSharedPreferences("ASIGNACIONES_PAE", Context.MODE_PRIVATE)
        val editor = prefs.edit()
        val jefaPrefs = getSharedPreferences("ManipuladoraJefe", Context.MODE_PRIVATE)
        val jEditor = jefaPrefs.edit()

        val asignacionesList = mutableListOf<PreparacionSyncManager.AsignacionItem>()

        for ((comp, cocinera) in mapaServicio) {
            val correoNorm = cocinera.correo.lowercase().trim().replace("@", "_").replace(".", "_")
            val nombreNorm = cocinera.nombre.lowercase().trim().replace(" ", "_")
            val primerNombre = cocinera.nombre.trim().split(" ").firstOrNull()?.lowercase() ?: ""

            editor.putString("asig_${fechaSeleccionada}_${servicioActual}_$comp", cocinera.nombre)
            editor.putString("asig_correo_${fechaSeleccionada}_${servicioActual}_$comp", cocinera.correo)
            editor.putInt("asig_uid_${fechaSeleccionada}_${servicioActual}_$comp", cocinera.idUsuario)

            if (correoNorm.isNotBlank()) {
                editor.putString("comp_correo_$correoNorm", comp)
                editor.putString("comp_correo_${servicioActual}_$correoNorm", comp)
                jEditor.putString("comp_correo_$correoNorm", comp)
                jEditor.putString("comp_correo_${servicioActual}_$correoNorm", comp)
            }
            if (cocinera.idUsuario > 0) {
                editor.putString("comp_uid_${cocinera.idUsuario}", comp)
                editor.putString("comp_uid_${servicioActual}_${cocinera.idUsuario}", comp)
                jEditor.putString("comp_uid_${cocinera.idUsuario}", comp)
                jEditor.putString("comp_uid_${servicioActual}_${cocinera.idUsuario}", comp)
            }
            if (nombreNorm.isNotBlank()) {
                editor.putString("comp_nombre_$nombreNorm", comp)
                jEditor.putString("comp_$nombreNorm", comp)
            }
            if (primerNombre.isNotBlank()) {
                editor.putString("comp_nombre_$primerNombre", comp)
                jEditor.putString("comp_$primerNombre", comp)
            }

            asignacionesList.add(
                PreparacionSyncManager.AsignacionItem(
                    idUsuario = cocinera.idUsuario,
                    nombre = cocinera.nombre,
                    correo = cocinera.correo,
                    componente = comp,
                    estado = "Asignada"
                )
            )
        }
        editor.apply()
        jEditor.apply()

        // Sincronizar en PreparacionSyncManager para reflejo en manipuladoras
        lifecycleScope.launch {
            try {
                val jornadaNombre = servicioActual
                PreparacionSyncManager.guardarPlatoYAsignacionesCompleto(
                    context = this@AsignarComponentesActivity,
                    nombreMenu = platoNombre,
                    idPlato = sessionManager.getPlatoDiaServicioId(this@AsignarComponentesActivity, fechaSeleccionada, servicioActual),
                    jornada = jornadaNombre,
                    asignaciones = asignacionesList
                )
            } catch (_: Exception) { }

            Toast.makeText(
                this@AsignarComponentesActivity,
                "✓ Asignación de $servicioActual guardada correctamente",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun cargarAsignacionesGuardadas() {
        val prefs = getSharedPreferences("ASIGNACIONES_PAE", Context.MODE_PRIVATE)
        val cocineras = ManipuladorasManager.obtenerManipuladoras()

        for (serv in listOf("Desayuno", "Almuerzo", "Merienda")) {
            val platoNombre = sessionManager.getPlatoDiaServicio(this, fechaSeleccionada, serv) ?: continue
            val componentes = MenuPaeCatalog.obtenerComponentesPorNombre(platoNombre)
            val mapa = asignacionesPorServicio.getOrPut(serv) { mutableMapOf() }

            for (comp in componentes) {
                val guardado = prefs.getString("asig_${fechaSeleccionada}_${serv}_$comp", null)
                if (!guardado.isNullOrBlank()) {
                    val c = cocineras.firstOrNull { it.nombre.equals(guardado, ignoreCase = true) }
                        ?: ManipuladorasManager.Manipuladora(
                            id = 0,
                            nombre = guardado,
                            funcion = "Preparación",
                            jornada = "Mañana",
                            activa = true
                        )
                    mapa[comp] = c
                }
            }
        }
    }

    private fun obtenerDescripcionPorPlato(nombre: String?, servicio: String): String {
        val n = nombre?.lowercase() ?: ""
        return when {
            n.contains("huevos revueltos") || n.contains("revueltos") ->
                "Un inicio de día nutritivo para nuestros estudiantes."
            n.contains("pericos") ->
                "Desayuno tradicional alto en proteína para iniciar la jornada escolar."
            servicio == "Desayuno" ->
                "Desayuno escolar balanceado con aporte calórico y vitamínico según normas PAE."
            servicio == "Almuerzo" ->
                "Almuerzo completo con proteína, carbohidrato y nutrientes para el almuerzo escolar."
            else ->
                "Merienda saludable para el refuerzo nutricional de la jornada de la tarde."
        }
    }

    private fun obtenerRecetaPorPlato(nombre: String?): String {
        val n = nombre?.lowercase() ?: ""
        return when {
            n.contains("huevos revueltos") || n.contains("revueltos") ->
                "Cocinar los huevos batidos a fuego suave hasta que estén bien cocidos. Servir con pan integral y papaya lavada, pelada y cortada en cubos."
            n.contains("pericos") ->
                "Picar tomate y cebolla finamente, sofreír a fuego medio, añadir los huevos batidos y revolver suavemente hasta cuajar. Acompañar con arroz blanco y refresco de avena."
            n.contains("pollo") ->
                "Sellar las presas de pollo con cebolla, pimentón y ajo. Cocinar a fuego medio en caldo de verduras durante 30 minutos. Servir con arroz y ensalada fresca."
            n.contains("carne") || n.contains("res") ->
                "Guisar la carne de res con tomate, cebolla, zanahoria y especias a fuego lento durante 45 minutos. Servir con arroz blanco y porción de fruta."
            n.contains("lenteja") || n.contains("lentejas") ->
                "Cocinar las lentejas previamente remojadas con guiso casero y zanahoria picada. Servir caliente con arroz y tajadas de plátano."
            n.contains("frijol") || n.contains("frijoles") ->
                "Cocinar los frijoles con hogao casero hasta que queden tiernos y con buena textura. Servir con arroz blanco y ensalada."
            n.contains("pasta") || n.contains("salsa") ->
                "Cocinar la pasta al dente. Preparar salsa blanca suave a base de leche y mantequilla, mezclar uniformemente y servir caliente."
            n.contains("chocolate") || n.contains("hojaldra") ->
                "Calentar la leche con pastilla de chocolate hasta hervir batiendo para formar espuma. Hornear las hojaldras y servir con fruta fresca."
            else ->
                "Preparar los ingredientes frescos bajo las normas de inocuidad del PAE. Cocinar a fuego controlado y distribuir las porciones balanceadas según la minuta."
        }
    }
}
