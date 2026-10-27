package com.example.movilmanupuladora.ui.manipuladora

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.movilmanupuladora.R
import com.example.movilmanupuladora.data.api.RetrofitClient
import com.example.movilmanupuladora.data.model.pasos_preparacion
import com.example.movilmanupuladora.databinding.ActivityPreparacionBinding
import com.example.movilmanupuladora.ui.manipuladora_Jefe.PreparacionEstadoManager
import com.example.movilmanupuladora.utils.NavigationHelper
import com.example.movilmanupuladora.utils.SessionManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch

class PreparacionActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPreparacionBinding
    private var nombrePlatoActual: String = "Pollo Guisado Criollo"
    private var componenteAsignado: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityPreparacionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnVolver.setOnClickListener { finish() }

        NavigationHelper.setupBarraNavegacion(
            this,
            binding.barraNavegacion,
            NavigationHelper.Tab.INICIO,
        )

        // Obtener datos del plato y componente
        val sessionMgr = SessionManager(this)
        componenteAsignado = intent.getStringExtra("componente_seleccionado")
            ?: sessionMgr.getComponenteAsignado(this)

        nombrePlatoActual = intent.getStringExtra("nombre_plato")
            ?: sessionMgr.getPlatoJefa(this)

        val miNombre = sessionMgr.getUserName() ?: "Manipuladora"
        val miCorreo = sessionMgr.getUserEmail() ?: ""

        val estadoActual = PreparacionEstadoManager.obtenerEstado(
            this,
            miNombre,
            componenteAsignado,
            miCorreo
        )
        actualizarVistaEstado(estadoActual)

        binding.btnMarcarPreparado.setOnClickListener {
            mostrarDialogoEstadoPreparacion()
        }

        val nombreFormateado = nombrePlatoActual.split(" ").joinToString(" ") { palabra ->
            palabra.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        }
        binding.tvNombrePlatoPreparacion.text = nombreFormateado

        // Mostrar el componente asignado si existe
        if (!componenteAsignado.isNullOrBlank()) {
            binding.tvComponentePreparacion.visibility = View.VISIBLE
            binding.tvComponentePreparacion.text = "🥣 Componente a preparar: $componenteAsignado"
        } else {
            binding.tvComponentePreparacion.visibility = View.GONE
        }

        val imgIntent = intent.getIntExtra("id_imagen", -1)
        val imgRes = if (imgIntent > 0) imgIntent else com.example.movilmanupuladora.ui.manipuladora_Jefe.InicioJefaActivity.obtenerImagenPlatoPorNombre(nombrePlatoActual)
        binding.imgPlatoPreparacion.setImageResource(imgRes)

        // Cargar pasos de preparación específicos para el componente
        if (!componenteAsignado.isNullOrBlank()) {
            mostrarPasos(obtenerPasosComponente(componenteAsignado!!))
        } else {
            cargarPasos()
        }
    }

    private fun actualizarVistaEstado(estado: String) {
        when (estado) {
            PreparacionEstadoManager.ESTADO_POR_HACER -> {
                binding.badgeTurnoPreparacion.text = "Estado: Por hacer ⏳"
                binding.badgeTurnoPreparacion.setBackgroundResource(R.drawable.bg_tag_plato)
                binding.badgeTurnoPreparacion.setTextColor(0xFF1B3317.toInt())
                binding.btnMarcarPreparado.text = "Cambiar Estado (Por hacer)"
                binding.btnMarcarPreparado.alpha = 1.0f
            }
            PreparacionEstadoManager.ESTADO_EN_PROCESO -> {
                binding.badgeTurnoPreparacion.text = "Estado: En proceso 🔥"
                binding.badgeTurnoPreparacion.setBackgroundResource(R.drawable.bg_pill_yellow)
                binding.badgeTurnoPreparacion.setTextColor(0xFF855B00.toInt())
                binding.btnMarcarPreparado.text = "En proceso (Cambiar Estado)"
                binding.btnMarcarPreparado.alpha = 1.0f
            }
            PreparacionEstadoManager.ESTADO_FINALIZADA -> {
                binding.badgeTurnoPreparacion.text = "Estado: Finalizada ✅"
                binding.badgeTurnoPreparacion.setBackgroundResource(R.drawable.bg_pill_green)
                binding.badgeTurnoPreparacion.setTextColor(0xFF176B35.toInt())
                binding.btnMarcarPreparado.text = "Preparación Finalizada ✅ (Cambiar)"
                binding.btnMarcarPreparado.alpha = 1.0f
            }
            else -> {
                binding.badgeTurnoPreparacion.text = "Estado: $estado"
                binding.badgeTurnoPreparacion.setBackgroundResource(R.drawable.bg_tag_plato)
                binding.btnMarcarPreparado.text = "Cambiar Estado"
            }
        }
    }

    /**
     * Genera pasos de preparación específicos según el componente asignado
     */
    private fun obtenerPasosComponente(componente: String): List<pasos_preparacion> {
        val c = componente.lowercase().trim()
        return when {
            c.contains("chocolate") || c.contains("café") || c.contains("cafe") -> listOf(
                pasos_preparacion(idPlato = 1, numeroPaso = "1", descripcionPaso = "Hervir el agua o la leche en olla grande a temperatura controlada."),
                pasos_preparacion(idPlato = 1, numeroPaso = "2", descripcionPaso = "Agregar el chocolate o café en polvo y revolver constantemente hasta disolver."),
                pasos_preparacion(idPlato = 1, numeroPaso = "3", descripcionPaso = "Endulzar según la receta institucional y verificar temperatura (mínimo 75°C)."),
                pasos_preparacion(idPlato = 1, numeroPaso = "4", descripcionPaso = "Servir en las porciones establecidas por el programa PAE.")
            )
            c.contains("leche") -> listOf(
                pasos_preparacion(idPlato = 1, numeroPaso = "1", descripcionPaso = "Verificar la fecha de vencimiento y estado de la leche antes de abrir."),
                pasos_preparacion(idPlato = 1, numeroPaso = "2", descripcionPaso = "Calentar la leche a temperatura adecuada sin dejar hervir."),
                pasos_preparacion(idPlato = 1, numeroPaso = "3", descripcionPaso = "Servir en las porciones establecidas por el gramaje del programa PAE."),
                pasos_preparacion(idPlato = 1, numeroPaso = "4", descripcionPaso = "Mantener refrigerada la leche sobrante y registrar las porciones servidas.")
            )
            c.contains("hojaldra") -> listOf(
                pasos_preparacion(idPlato = 1, numeroPaso = "1", descripcionPaso = "Preparar la masa con harina, sal, mantequilla y agua según la receta."),
                pasos_preparacion(idPlato = 1, numeroPaso = "2", descripcionPaso = "Amasar y dejar reposar la masa tapada por 15 minutos."),
                pasos_preparacion(idPlato = 1, numeroPaso = "3", descripcionPaso = "Estirar, cortar en porciones y freír en aceite caliente hasta dorar."),
                pasos_preparacion(idPlato = 1, numeroPaso = "4", descripcionPaso = "Escurrir el exceso de aceite y servir según las porciones del gramaje.")
            )
            c.contains("papaya") || c.contains("mango") || c.contains("manzana") || c.contains("fruta") -> listOf(
                pasos_preparacion(idPlato = 1, numeroPaso = "1", descripcionPaso = "Lavar y desinfectar la fruta con solución aprobada por la normativa."),
                pasos_preparacion(idPlato = 1, numeroPaso = "2", descripcionPaso = "Pelar y cortar en porciones según el gramaje institucional."),
                pasos_preparacion(idPlato = 1, numeroPaso = "3", descripcionPaso = "Distribuir las porciones en los recipientes de servido."),
                pasos_preparacion(idPlato = 1, numeroPaso = "4", descripcionPaso = "Mantener refrigerada hasta el momento de servir a los estudiantes.")
            )
            c.contains("yogurt") -> listOf(
                pasos_preparacion(idPlato = 1, numeroPaso = "1", descripcionPaso = "Verificar fecha de vencimiento y cadena de frío del yogurt."),
                pasos_preparacion(idPlato = 1, numeroPaso = "2", descripcionPaso = "Servir en vasos o recipientes individuales según el gramaje."),
                pasos_preparacion(idPlato = 1, numeroPaso = "3", descripcionPaso = "Mantener refrigerado hasta la hora de distribución."),
                pasos_preparacion(idPlato = 1, numeroPaso = "4", descripcionPaso = "Registrar las porciones servidas y el lote del producto.")
            )
            c.contains("torta") || c.contains("pastel") || c.contains("galleta") || c.contains("cuca") -> listOf(
                pasos_preparacion(idPlato = 1, numeroPaso = "1", descripcionPaso = "Verificar el estado y empaque del producto antes de servir."),
                pasos_preparacion(idPlato = 1, numeroPaso = "2", descripcionPaso = "Porcionar según el gramaje establecido por el programa PAE."),
                pasos_preparacion(idPlato = 1, numeroPaso = "3", descripcionPaso = "Distribuir en los platos o bandejas de servido."),
                pasos_preparacion(idPlato = 1, numeroPaso = "4", descripcionPaso = "Registrar las unidades servidas y mantener control de inventario.")
            )
            c.contains("maní") || c.contains("mani") -> listOf(
                pasos_preparacion(idPlato = 1, numeroPaso = "1", descripcionPaso = "Verificar estado, empaque y fecha de vencimiento del maní."),
                pasos_preparacion(idPlato = 1, numeroPaso = "2", descripcionPaso = "Porcionar en bolsitas o recipientes individuales según gramaje."),
                pasos_preparacion(idPlato = 1, numeroPaso = "3", descripcionPaso = "Distribuir junto con los demás componentes del menú."),
                pasos_preparacion(idPlato = 1, numeroPaso = "4", descripcionPaso = "Registrar las porciones entregadas.")
            )
            c.contains("avena") || c.contains("refresco") || c.contains("jugo") -> listOf(
                pasos_preparacion(idPlato = 1, numeroPaso = "1", descripcionPaso = "Hervir agua en olla grande a temperatura controlada."),
                pasos_preparacion(idPlato = 1, numeroPaso = "2", descripcionPaso = "Agregar la avena/fruta y azúcar según la receta institucional."),
                pasos_preparacion(idPlato = 1, numeroPaso = "3", descripcionPaso = "Revolver constantemente y cocinar hasta alcanzar la consistencia deseada."),
                pasos_preparacion(idPlato = 1, numeroPaso = "4", descripcionPaso = "Dejar enfriar a temperatura segura y servir en porciones establecidas.")
            )
            c.contains("carne") -> listOf(
                pasos_preparacion(idPlato = 1, numeroPaso = "1", descripcionPaso = "Descongelar y lavar la carne siguiendo el protocolo de inocuidad."),
                pasos_preparacion(idPlato = 1, numeroPaso = "2", descripcionPaso = "Cortar y sazonar con los condimentos de la receta institucional."),
                pasos_preparacion(idPlato = 1, numeroPaso = "3", descripcionPaso = "Cocinar hasta alcanzar temperatura interna mínima de 75°C en el centro."),
                pasos_preparacion(idPlato = 1, numeroPaso = "4", descripcionPaso = "Porcionar según el gramaje y servir caliente.")
            )
            c.contains("arroz") -> listOf(
                pasos_preparacion(idPlato = 1, numeroPaso = "1", descripcionPaso = "Lavar el arroz hasta que el agua salga clara."),
                pasos_preparacion(idPlato = 1, numeroPaso = "2", descripcionPaso = "Hervir agua con sal y aceite, agregar el arroz lavado."),
                pasos_preparacion(idPlato = 1, numeroPaso = "3", descripcionPaso = "Cocinar a fuego medio-bajo tapado hasta que esté suelto y seco."),
                pasos_preparacion(idPlato = 1, numeroPaso = "4", descripcionPaso = "Servir según el gramaje institucional por porción.")
            )
            else -> listOf(
                pasos_preparacion(idPlato = 1, numeroPaso = "1", descripcionPaso = "Lavar y desinfectar los ingredientes, utensilios y mesas de trabajo."),
                pasos_preparacion(idPlato = 1, numeroPaso = "2", descripcionPaso = "Preparar el componente '$componente' según la receta institucional del PAE."),
                pasos_preparacion(idPlato = 1, numeroPaso = "3", descripcionPaso = "Verificar sazón, textura y temperatura antes de servir (mínimo 75°C si aplica)."),
                pasos_preparacion(idPlato = 1, numeroPaso = "4", descripcionPaso = "Porcionar y servir según las tablas de gramaje del programa.")
            )
        }
    }

    private fun obtenerImagenPorNombre(nombre: String): Int {
        return com.example.movilmanupuladora.ui.manipuladora_Jefe.InicioJefaActivity.obtenerImagenPlatoPorNombre(nombre)
    }

    private fun mostrarDialogoEstadoPreparacion() {
        val opciones = arrayOf(
            "⏳ Por hacer / Pendiente",
            "🔥 En proceso / En preparación",
            "✅ Finalizada / Completada"
        )

        MaterialAlertDialogBuilder(this)
            .setTitle("Actualizar Estado de Preparación")
            .setItems(opciones) { _, which ->
                val session = SessionManager(this)
                val miNombre = session.getUserName() ?: "Manipuladora"
                val miCorreo = session.getUserEmail() ?: ""
                val preferencias = getSharedPreferences("SIRAE", MODE_PRIVATE)

                val nuevoEstado = when (which) {
                    1 -> PreparacionEstadoManager.ESTADO_EN_PROCESO
                    2 -> PreparacionEstadoManager.ESTADO_FINALIZADA
                    else -> PreparacionEstadoManager.ESTADO_POR_HACER
                }

                PreparacionEstadoManager.guardarEstado(this, miNombre, componenteAsignado, nuevoEstado, miCorreo)

                // Sincronizar en tiempo real al backend para que la Jefa y el equipo lo vean de inmediato
                lifecycleScope.launch {
                    com.example.movilmanupuladora.utils.PreparacionSyncManager.actualizarEstadoEnBackend(
                        this@PreparacionActivity,
                        nuevoEstado,
                        componenteAsignado
                    )
                }

                when (which) {
                    0 -> { // Por hacer
                        preferencias.edit().putString("estado_plato", "PENDIENTE").apply()
                        actualizarVistaEstado(nuevoEstado)
                        Toast.makeText(this, "Estado actualizado: Por hacer ⏳", Toast.LENGTH_SHORT).show()
                    }
                    1 -> { // En proceso
                        preferencias.edit().putString("estado_plato", "EN_PREPARACION").apply()
                        actualizarVistaEstado(nuevoEstado)
                        Toast.makeText(this, "Estado actualizado: En proceso 🔥", Toast.LENGTH_SHORT).show()
                    }
                    2 -> { // Finalizada
                        preferencias.edit().putBoolean("plato_preparado", true).apply()
                        preferencias.edit().putString("estado_plato", "FINALIZADO").apply()
                        actualizarVistaEstado(nuevoEstado)

                        MaterialAlertDialogBuilder(this)
                            .setTitle("¡Preparación Finalizada!")
                            .setMessage("El componente '${componenteAsignado ?: nombrePlatoActual}' ha sido marcado como Finalizado con éxito.\n\n¿Deseas ver la pantalla de confirmación?")
                            .setPositiveButton("Ver Confirmación") { _, _ ->
                                val intentConf = Intent(this, ConfirmacionActivity::class.java).apply {
                                    putExtra("nombre_plato", nombrePlatoActual)
                                }
                                startActivity(intentConf)
                                finish()
                            }
                            .setNegativeButton("Permanecer aquí", null)
                            .show()
                    }
                }
            }
            .setNegativeButton("Cancelar") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    private val pasosFallback = listOf(
        pasos_preparacion(idPlato = 1, numeroPaso = "1", descripcionPaso = "Lavar y desinfectar los ingredientes, utensilios y mesas de trabajo siguiendo el protocolo de bioseguridad."),
        pasos_preparacion(idPlato = 1, numeroPaso = "2", descripcionPaso = "Cocinar y sellar la proteína y los principios a temperatura controlada (mínimo 75°C)."),
        pasos_preparacion(idPlato = 1, numeroPaso = "3", descripcionPaso = "Verificar sazón, textura, cocción completa y temperatura antes del ensamble del plato."),
        pasos_preparacion(idPlato = 1, numeroPaso = "4", descripcionPaso = "Porcionar y servir según las tablas de gramaje institucional del programa de alimentación escolar.")
    )

    private fun cargarPasos() {
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.obtenerPasosPreparacion()

                if (response.isSuccessful && !response.body().isNullOrEmpty()) {
                    val listaPasos: List<pasos_preparacion> = response.body()!!
                    mostrarPasos(listaPasos)
                } else {
                    mostrarPasos(pasosFallback)
                }
            } catch (e: Exception) {
                mostrarPasos(pasosFallback)
            }
        }
    }

    private fun mostrarPasos(pasos: List<pasos_preparacion>) {
        binding.contenedorPasos.removeAllViews()

        // Si hay componente asignado, mostrar encabezado de componente
        if (!componenteAsignado.isNullOrBlank()) {
            val tvComponente = TextView(this).apply {
                text = "📋 Pasos para preparar: $componenteAsignado"
                setTextColor(0xFF176B35.toInt())
                textSize = 14f
                setTypeface(typeface, android.graphics.Typeface.BOLD)
                setPadding(0, 0, 0, 16)
            }
            binding.contenedorPasos.addView(tvComponente)
        }

        for (paso in pasos) {
            val fila = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(0, 24, 0, 0)
                gravity = android.view.Gravity.CENTER_VERTICAL
            }

            val tvNumero = TextView(this).apply {
                text = paso.numeroPaso
                setTextColor(0xFF1B3317.toInt())
                textSize = 13f
                gravity = android.view.Gravity.CENTER
                setBackgroundResource(R.drawable.bg_pill_yellow)
                setTypeface(typeface, android.graphics.Typeface.BOLD)
                layoutParams = LinearLayout.LayoutParams(54, 54)
            }

            val tvDescripcion = TextView(this).apply {
                text = paso.descripcionPaso
                setTextColor(0xFF1B3317.toInt())
                textSize = 13f
                layoutParams = LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
                ).also { it.marginStart = 20 }
            }

            fila.addView(tvNumero)
            fila.addView(tvDescripcion)
            binding.contenedorPasos.addView(fila)
        }
    }
}