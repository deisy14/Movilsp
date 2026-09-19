package com.example.movilmanupuladora.ui.manipuladora

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.movilmanupuladora.MainActivity
import com.example.movilmanupuladora.data.api.RetrofitClient
import com.example.movilmanupuladora.data.model.DetallePlato
import com.example.movilmanupuladora.data.model.PlatoResponse
import com.example.movilmanupuladora.data.repository.MenuRepository
import com.example.movilmanupuladora.databinding.ActivityComponentesBinding
import com.example.movilmanupuladora.databinding.DialogRecetaComponenteBinding
import com.example.movilmanupuladora.databinding.ItemComponentePlatoBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch

class ComponentesActivity : AppCompatActivity() {

    private lateinit var binding: ActivityComponentesBinding
    private val menuRepository = MenuRepository(RetrofitClient.apiService)

    private var platoActual: PlatoResponse? = null
    private var detalleActual: DetallePlato? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 1. Inicializar ViewBinding
        binding = ActivityComponentesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 2. Insets de pantalla
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(
                systemBars.left,
                systemBars.top,
                systemBars.right,
                systemBars.bottom
            )
            insets
        }

        // 3. Botón volver
        binding.btnVolver.setOnClickListener {
            finish()
        }

        // 4. Botón Ver Pasos de Preparación
        binding.btnVerIngredientes.setOnClickListener {
            val intent = Intent(this, PreparacionActivity::class.java)
            platoActual?.let { plato ->
                intent.putExtra("id_plato", plato.idPlato)
                intent.putExtra("nombre_plato", plato.nombrePlato)
            }
            startActivity(intent)
        }

        // 5. Barra de navegación inferior
        binding.barraNavegacion.navInicio.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }

        // 6. Cargar datos del Backend (Secciones, Platos, Detalle del Plato)
        val idPlatoIntent = intent.getIntExtra("id_plato", -1)
        val idSeccionIntent = intent.getIntExtra("id_seccion", -1)
        val nombrePlatoIntent = intent.getStringExtra("nombre_plato")
        cargarDatosBackend(idPlatoIntent, idSeccionIntent, nombrePlatoIntent)
    }

    // Datos de respaldo con los registros reales de la base de datos SIRAE
    private val platosBaseRespaldo = listOf(
        PlatoResponse(idPlato = 4, idSeccion = 2, nombrePlato = "bandeja paisa", componente = "carne molida, chicharron, aguacate, arroz, frijol"),
        PlatoResponse(idPlato = 1, idSeccion = 3, nombrePlato = "arroz a la valenciana", componente = "proteina"),
        PlatoResponse(idPlato = 2, idSeccion = 3, nombrePlato = "arroz con pollo", componente = "pollo"),
        PlatoResponse(idPlato = 3, idSeccion = 3, nombrePlato = "Arroz con Pollo Especial", componente = "Principal"),
        PlatoResponse(idPlato = 5, idSeccion = 4, nombrePlato = "cafe con pan", componente = "no se")
    )

    private fun cargarDatosBackend(idPlatoBuscado: Int, idSeccionBuscada: Int, nombrePlatoBuscado: String?) {
        lifecycleScope.launch {
            try {
                // A) Consumir Secciones de Menú
                var seccionNombre: String? = null
                val resSecciones = menuRepository.obtenerSeccionesMenu()
                if (resSecciones.isSuccessful) {
                    val secciones = resSecciones.body() ?: emptyList()
                    val seccionEncontrada = if (idSeccionBuscada != -1) {
                        secciones.firstOrNull { it.idSeccion == idSeccionBuscada }
                    } else {
                        secciones.firstOrNull()
                    }
                    seccionNombre = seccionEncontrada?.nombreSeccion
                }

                // B) Consumir Platos
                val resPlatos = menuRepository.obtenerPlatos()
                val listaPlatos = if (resPlatos.isSuccessful && !resPlatos.body().isNullOrEmpty()) {
                    resPlatos.body()!!
                } else {
                    platosBaseRespaldo
                }

                // Seleccionar plato: por ID, por Nombre, o buscar preferentemente "bandeja paisa"
                platoActual = when {
                    idPlatoBuscado != -1 -> listaPlatos.firstOrNull { it.idPlato == idPlatoBuscado }
                    !nombrePlatoBuscado.isNullOrEmpty() -> listaPlatos.firstOrNull { it.nombrePlato.equals(nombrePlatoBuscado, ignoreCase = true) }
                    else -> null
                } ?: listaPlatos.firstOrNull { it.nombrePlato?.contains("bandeja", ignoreCase = true) == true }
                  ?: listaPlatos.firstOrNull()

                // Actualizar interfaz del plato
                platoActual?.let { plato ->
                    actualizarUiPlato(plato, seccionNombre)
                    mostrarComponentesDelPlato(plato)
                }

                // C) Consumir Detalle de Platos (porciones, total a preparar y estado)
                val resDetalles = menuRepository.obtenerDetallePlatos()
                if (resDetalles.isSuccessful) {
                    val detalles = resDetalles.body() ?: emptyList()
                    val idPlato = platoActual?.idPlato
                    detalleActual = detalles.firstOrNull { it.idPlato == idPlato }

                    detalleActual?.let { detalle ->
                        detalle.estadoPreparacion?.let { estado ->
                            binding.tvBadgeEstado.text = estado
                        }
                    }
                }

            } catch (e: Exception) {
                Log.e("Backend", "Error de conexión con el backend", e)
                // Usar respaldo en caso de fallo de red
                platoActual = platosBaseRespaldo.firstOrNull { it.nombrePlato?.contains("bandeja", ignoreCase = true) == true }
                    ?: platosBaseRespaldo.first()
                actualizarUiPlato(platoActual!!, "Almuerzo")
                mostrarComponentesDelPlato(platoActual!!)
            }
        }
    }

    private fun actualizarUiPlato(plato: PlatoResponse, seccionNombre: String?) {
        val nombreFormateado = plato.nombrePlato?.split(" ")?.joinToString(" ") { palabra ->
            palabra.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        } ?: "Plato sin nombre"

        binding.tvNombrePlato.text = nombreFormateado

        val seccion = seccionNombre ?: when (plato.idSeccion) {
            1 -> "Desayuno"
            2 -> "Almuerzo"
            3 -> "Merienda"
            else -> "Almuerzo"
        }
        binding.tvTituloSeccion.text = "Componentes del $seccion"

        // Imagen destacada según el tipo de plato
        val imgRes = when {
            plato.nombrePlato?.contains("bandeja", ignoreCase = true) == true -> com.example.movilmanupuladora.R.drawable.frijoles
            plato.nombrePlato?.contains("frijol", ignoreCase = true) == true -> com.example.movilmanupuladora.R.drawable.frijoles
            plato.nombrePlato?.contains("chocolate", ignoreCase = true) == true -> com.example.movilmanupuladora.R.drawable.chocolate
            plato.nombrePlato?.contains("huevo", ignoreCase = true) == true -> com.example.movilmanupuladora.R.drawable.huevo_perico
            plato.nombrePlato?.contains("pollo", ignoreCase = true) == true -> com.example.movilmanupuladora.R.drawable.apanado
            plato.nombrePlato?.contains("arroz", ignoreCase = true) == true -> com.example.movilmanupuladora.R.drawable.arroz_de_leche
            else -> com.example.movilmanupuladora.R.drawable.frijoles
        }
        binding.imgPlato.setImageResource(imgRes)
    }

    /**
     * Extrae y muestra por separado los componentes individuales del plato seleccionado,
     * indicando al frente su clasificación (Proteína, Principio, Acompañante, Cereal / Base, etc.)
     */
    private fun mostrarComponentesDelPlato(plato: PlatoResponse) {
        binding.contenedorComponentes.removeAllViews()

        val rawComponentes = plato.componente?.trim() ?: ""
        val listaComponentes = if (rawComponentes.contains(",")) {
            rawComponentes.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        } else if (rawComponentes.isNotEmpty()) {
            listOf(rawComponentes)
        } else {
            // Componentes por defecto para platos conocidos si la base de datos no tuviera desglose
            when {
                plato.nombrePlato?.contains("bandeja", ignoreCase = true) == true ->
                    listOf("carne molida", "chicharron", "aguacate", "arroz", "frijol")
                plato.nombrePlato?.contains("pollo", ignoreCase = true) == true ->
                    listOf("pechuga de pollo", "arroz", "principio de arveja", "ensalada")
                else ->
                    listOf("Componente principal")
            }
        }

        for (componenteTexto in listaComponentes) {
            val itemBinding = ItemComponentePlatoBinding.inflate(layoutInflater, binding.contenedorComponentes, false)

            // 1. Nombre del componente con mayúscula inicial
            val nombreCapitalizado = componenteTexto.replaceFirstChar {
                if (it.isLowerCase()) it.titlecase() else it.toString()
            }
            itemBinding.tvNombreComponente.text = nombreCapitalizado

            // 2. Clasificación al frente (Proteína, Principio, Acompañante, Entrada, Bebida, etc.)
            val clasificacion = clasificarComponente(componenteTexto)
            itemBinding.tvTipoComponente.text = clasificacion
            itemBinding.tvTipoComponente.visibility = View.VISIBLE

            // 3. Icono según el alimento
            itemBinding.imgComponente.setImageResource(obtenerIconoComponente(componenteTexto))

            // 4. Click en el componente o en su clasificación (consumo de receta_componente)
            val clickListenerReceta = View.OnClickListener {
                mostrarDialogoRecetaComponente(componenteTexto, clasificacion, plato)
            }
            itemBinding.root.setOnClickListener(clickListenerReceta)
            itemBinding.tvTipoComponente.setOnClickListener(clickListenerReceta)

            binding.contenedorComponentes.addView(itemBinding.root)
        }
    }

    /**
     * Muestra el modal elegante de receta y detalles del componente (consumo de receta_componente)
     */
    private fun mostrarDialogoRecetaComponente(nombreComponente: String, tipoComponente: String, plato: PlatoResponse) {
        val dialogBinding = DialogRecetaComponenteBinding.inflate(layoutInflater)

        val nombreFormateado = nombreComponente.replaceFirstChar {
            if (it.isLowerCase()) it.titlecase() else it.toString()
        }

        dialogBinding.tvTituloComponenteModal.text = nombreFormateado
        dialogBinding.tvBadgeTipoModal.text = tipoComponente

        val platoNombre = plato.nombrePlato?.split(" ")?.joinToString(" ") { palabra ->
            palabra.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        } ?: "Plato escolar"
        dialogBinding.tvPlatoPerteneciente.text = platoNombre

        // Insumos e ingredientes sugeridos de la receta según el componente
        val n = nombreComponente.lowercase().trim()
        val insumosTexto = when {
            n.contains("pollo") -> "Pechuga o presa limpia, cebolla, ajo, pimentón, sal"
            n.contains("carne") || n.contains("res") -> "Carne de res seleccionada, tomate, cebolla, sal, comino"
            n.contains("chicharron") || n.contains("chicharrón") -> "Tocino carnudo en tiras, sal marina, adobo"
            n.contains("frijol") || n.contains("fríjol") -> "Fríjol rojo/cargamanto, plátano picado, zanahoria, hogao"
            n.contains("lenteja") -> "Lentejas seleccionadas, papa en cubos, zanahoria, guiso"
            n.contains("arroz") -> "Arroz de grano entero, aceite vegetal, ajo, agua y sal"
            n.contains("aguacate") -> "Aguacate fresco en rodajas, limón opcional"
            n.contains("huevo") -> "Huevo fresco, tomate y cebolla picados finos"
            n.contains("leche") -> "Leche entera pasteurizada, azúcar o canela"
            n.contains("chocolate") -> "Pastilla o polvo de chocolate, leche y canela"
            n.contains("azucar") || n.contains("azúcar") -> "Azúcar blanca o morena en porción medida"
            n.contains("canela") -> "Canela en astillas aromática"
            else -> "Insumos frescos certificados del programa de alimentación escolar"
        }
        dialogBinding.tvIngredientesComponente.text = insumosTexto

        // Gramaje / porción según tipo
        val porcionTexto = when (tipoComponente) {
            "Proteína" -> "80g - 100g cocido por ración"
            "Principio" -> "90g - 120g servido por ración"
            "Cereal / Base" -> "80g - 100g de cereal cocido"
            "Acompañante" -> "50g - 70g según tabla nutricional"
            "Entrada" -> "150ml - 200ml de caldo / sopa"
            "Bebida" -> "200ml vaso servido frío/tibio"
            else -> "1 porción estandarizada institucional"
        }
        dialogBinding.tvPorcionComponente.text = porcionTexto

        // Icono acorde
        dialogBinding.imgComponenteModal.setImageResource(obtenerIconoComponente(nombreComponente))

        val dialog = MaterialAlertDialogBuilder(this)
            .setView(dialogBinding.root)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        dialogBinding.btnCerrarModalComponente.setOnClickListener {
            dialog.dismiss()
        }

        dialogBinding.btnCerrarRecetaModal.setOnClickListener {
            dialog.dismiss()
        }

        dialogBinding.btnVerPasosModal.setOnClickListener {
            dialog.dismiss()
            val intent = Intent(this, PreparacionActivity::class.java).apply {
                putExtra("id_plato", plato.idPlato)
                putExtra("nombre_plato", plato.nombrePlato)
                putExtra("componente_seleccionado", nombreComponente)
            }
            startActivity(intent)
        }

        dialogBinding.btnVerIngredientesModal.setOnClickListener {
            dialog.dismiss()
            val intent = Intent(this, IngredientesActivity::class.java).apply {
                putExtra("id_plato", plato.idPlato)
                putExtra("nombre_plato", plato.nombrePlato)
                putExtra("componente_seleccionado", nombreComponente)
            }
            startActivity(intent)
        }

        dialog.show()
    }

    /**
     * Clasifica un componente dentro de las categorías de nutrición y menú escolar/típico
     */
    private fun clasificarComponente(nombre: String): String {
        val n = nombre.lowercase().trim()
        return when {
            n.contains("carne") || n.contains("chicharron") || n.contains("chicharrón") ||
            n.contains("pollo") || n.contains("pescado") || n.contains("huevo") ||
            n.contains("res") || n.contains("cerdo") || n.contains("atun") || n.contains("atún") ||
            n.contains("proteina") || n.contains("proteína") -> "Proteína"

            n.contains("frijol") || n.contains("fríjol") || n.contains("lenteja") ||
            n.contains("garbanzo") || n.contains("arveja") || n.contains("blanquillo") ||
            n.contains("principio") -> "Principio"

            n.contains("aguacate") || n.contains("ensalada") || n.contains("verdura") ||
            n.contains("tajada") || n.contains("platano") || n.contains("plátano") ||
            n.contains("patacon") || n.contains("patacón") || n.contains("hogao") ||
            n.contains("ahogado") || n.contains("acompañ") -> "Acompañante"

            n.contains("arroz") || n.contains("pasta") || n.contains("espagueti") ||
            n.contains("arepa") || n.contains("pan") || n.contains("yuca") ||
            n.contains("papa") || n.contains("cereal") || n.contains("base") -> "Cereal / Base"

            n.contains("sopa") || n.contains("crema") || n.contains("caldo") ||
            n.contains("consome") || n.contains("consomé") || n.contains("ajiaco") ||
            n.contains("sancocho") || n.contains("entrada") -> "Entrada"

            n.contains("jugo") || n.contains("leche") || n.contains("chocolate") ||
            n.contains("cafe") || n.contains("café") || n.contains("avena") ||
            n.contains("colada") || n.contains("limonada") || n.contains("bebida") -> "Bebida"

            n.contains("postre") || n.contains("fruta") || n.contains("gelatina") ||
            n.contains("bocadillo") -> "Postre"

            else -> "Componente"
        }
    }

    /**
     * Selecciona el recurso drawable más adecuado para cada componente
     */
    private fun obtenerIconoComponente(nombre: String): Int {
        val n = nombre.lowercase().trim()
        return when {
            n.contains("leche") -> com.example.movilmanupuladora.R.drawable.ic_leche
            n.contains("chocolate") -> com.example.movilmanupuladora.R.drawable.ic_chocolate_polvo
            n.contains("azucar") || n.contains("azúcar") -> com.example.movilmanupuladora.R.drawable.ic_azucar
            n.contains("canela") -> com.example.movilmanupuladora.R.drawable.ic_canela
            n.contains("frijol") || n.contains("fríjol") || n.contains("lenteja") -> com.example.movilmanupuladora.R.drawable.frijoles
            n.contains("huevo") -> com.example.movilmanupuladora.R.drawable.huevo_perico
            n.contains("pollo") || n.contains("carne") || n.contains("chicharron") || n.contains("chicharrón") -> com.example.movilmanupuladora.R.drawable.apanado
            n.contains("arroz") -> com.example.movilmanupuladora.R.drawable.arroz_de_leche
            else -> com.example.movilmanupuladora.R.drawable.ic_plato_asignado
        }
    }
}