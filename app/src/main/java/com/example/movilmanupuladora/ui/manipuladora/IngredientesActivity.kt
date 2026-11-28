package com.example.movilmanupuladora.ui.manipuladora

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
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
import com.example.movilmanupuladora.data.model.Ingrediente
import kotlinx.coroutines.launch

class IngredientesActivity : AppCompatActivity() {

    private var listaIngredientesGlobal = listOf<Ingrediente>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setContentView(R.layout.activity_ingredientes)

        // Barras del sistema
        val main = findViewById<View>(R.id.main)
        main?.let { vMain ->
            ViewCompat.setOnApplyWindowInsetsListener(vMain) { v, insets ->
                val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
                insets
            }
        }

        // Botón volver
        findViewById<View>(R.id.btnVolver)?.setOnClickListener {
            finish()
        }

        // Mostrar nombre del plato y componente si viene del intent o sesión
        val sessionMgr = com.example.movilmanupuladora.utils.SessionManager(this)
        val nombrePlato = intent.getStringExtra("nombre_plato")?.takeIf { it.isNotBlank() }
            ?: sessionMgr.getPlatoJefa(this).takeIf { it.isNotBlank() }
            ?: "Menú por asignar"
        val componenteSeleccionado = intent.getStringExtra("componente_seleccionado")?.takeIf { it.isNotBlank() }
            ?: sessionMgr.getComponenteAsignado(this)

        val prim = com.example.movilmanupuladora.ui.manipuladora_Jefe.AsistenciaManager.obtenerPrimaria(this)
        val sec = com.example.movilmanupuladora.ui.manipuladora_Jefe.AsistenciaManager.obtenerSecundaria(this)
        val tot = com.example.movilmanupuladora.ui.manipuladora_Jefe.AsistenciaManager.obtenerManana(this)
            .takeIf { it > 0 } ?: (prim + sec)
        findViewById<TextView>(R.id.tvRacionesIngredientes)?.text =
            if (tot > 0) "$tot raciones" else "— raciones"
        findViewById<TextView>(R.id.tvDesgloseIngredientes)?.text =
            if (prim > 0 || sec > 0) "Prim: $prim · Sec: $sec" else "Prim: — · Sec: —"

        val tvNombrePlato = findViewById<TextView>(R.id.tvNombrePlatoIngredientes)
        val nombreFormateado = nombrePlato.split(" ").joinToString(" ") { palabra ->
            palabra.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        }
        tvNombrePlato?.text = nombreFormateado

        val tvComponenteSubtitulo = findViewById<TextView>(R.id.tvComponenteSubtitulo)
        if (!componenteSeleccionado.isNullOrBlank()) {
            val compFormateado = componenteSeleccionado.replaceFirstChar {
                if (it.isLowerCase()) it.titlecase() else it.toString()
            }
            tvComponenteSubtitulo?.text = "Componente: $compFormateado • Insumos y sazón"
            tvComponenteSubtitulo?.visibility = View.VISIBLE
        } else {
            tvComponenteSubtitulo?.visibility = View.GONE
        }

        // Asignar imagen adecuada según el plato
        val imgPlato = findViewById<android.widget.ImageView>(R.id.imgPlatoIngredientes)
        val imgRes = when {
            nombrePlato.contains("bandeja", ignoreCase = true) -> R.drawable.plato1
            nombrePlato.contains("frijol", ignoreCase = true) -> R.drawable.plato2
            nombrePlato.contains("chocolate", ignoreCase = true) -> R.drawable.plato3
            nombrePlato.contains("huevo", ignoreCase = true) -> R.drawable.plato4
            nombrePlato.contains("pollo", ignoreCase = true) -> R.drawable.plato5
            nombrePlato.contains("arroz", ignoreCase = true) -> R.drawable.plato6
            else -> R.drawable.plato7
        }
        imgPlato?.setImageResource(imgRes)

        // Botón continuar a preparación
        val btnContinuarPreparacion = findViewById<Button>(R.id.btnContinuarPreparacion)
        btnContinuarPreparacion?.setOnClickListener {
            val intent = Intent(this, PreparacionActivity::class.java).apply {
                putExtra("nombre_plato", nombrePlato)
                putExtra("id_plato", intent.getIntExtra("id_plato", -1))
                putExtra("componente_seleccionado", componenteSeleccionado)
            }
            startActivity(intent)
        }

        // Cargar ingredientes desde el backend
        cargarIngredientes()
    }

    private fun cargarIngredientes() {
        lifecycleScope.launch {
            try {
                com.example.movilmanupuladora.utils.SessionManager.getToken(this@IngredientesActivity)
                val response = RetrofitClient.apiService.obtenerIngredientes()

                if (response.isSuccessful && !response.body().isNullOrEmpty()) {
                    val listaBackend = response.body()!!
                    listaIngredientesGlobal = listaBackend
                    configurarFiltrosChips()
                    mostrarIngredientes(listaBackend)
                } else {
                    listaIngredientesGlobal = emptyList()
                    configurarFiltrosChips()
                    mostrarIngredientes(emptyList())
                }
            } catch (e: Exception) {
                listaIngredientesGlobal = emptyList()
                configurarFiltrosChips()
                mostrarIngredientes(emptyList())
            }
        }
    }

    private fun configurarFiltrosChips() {
        val chipTodos = findViewById<TextView>(R.id.chipTodos)
        val chipBasePlato = findViewById<TextView>(R.id.chipBasePlato)
        val chipSazon = findViewById<TextView>(R.id.chipSazon)

        fun actualizarEstilos(seleccionado: TextView) {
            val chips = listOf(chipTodos, chipBasePlato, chipSazon)
            for (c in chips) {
                if (c == seleccionado) {
                    c?.setBackgroundResource(R.drawable.bg_pill_green)
                    c?.setTextColor(android.graphics.Color.WHITE)
                } else {
                    c?.setBackgroundResource(R.drawable.bg_tag_plato)
                    c?.setTextColor(android.graphics.Color.parseColor("#64748B"))
                }
            }
        }

        chipTodos?.setOnClickListener {
            actualizarEstilos(chipTodos)
            mostrarIngredientes(listaIngredientesGlobal)
        }

        chipBasePlato?.setOnClickListener {
            actualizarEstilos(chipBasePlato)
            val filtrados = listaIngredientesGlobal.filter { ing ->
                val n = (ing.nombreIngrediente ?: "").lowercase()
                !n.contains("sal") && !n.contains("orégano") && !n.contains("oregano") && !n.contains("ajo") &&
                !n.contains("comino") && !n.contains("color") && !n.contains("triguisar") && !n.contains("pimienta") && !n.contains("aceite")
            }
            mostrarIngredientes(filtrados)
        }

        chipSazon?.setOnClickListener {
            actualizarEstilos(chipSazon)
            val filtrados = listaIngredientesGlobal.filter { ing ->
                val n = (ing.nombreIngrediente ?: "").lowercase()
                n.contains("sal") || n.contains("orégano") || n.contains("oregano") || n.contains("ajo") ||
                n.contains("comino") || n.contains("color") || n.contains("triguisar") || n.contains("pimienta") || n.contains("aceite")
            }
            mostrarIngredientes(filtrados)
        }
    }

    private fun mostrarIngredientes(ingredientes: List<Ingrediente>) {
        val contenedor = findViewById<LinearLayout>(R.id.contenedorIngredientes) ?: return
        contenedor.removeAllViews()

        if (ingredientes.isEmpty()) {
            val tvVacio = TextView(this).apply {
                text = "Sin ingredientes registrados en el servidor para este plato."
                setTextColor(android.graphics.Color.parseColor("#64748B"))
                textSize = 13f
                setPadding(16, 24, 16, 24)
            }
            contenedor.addView(tvVacio)
            return
        }

        for (ingrediente in ingredientes) {
            val itemBinding = com.example.movilmanupuladora.databinding.ItemIngredienteTablaBinding.inflate(
                layoutInflater,
                contenedor,
                false
            )

            // Nombre
            itemBinding.tvNombreIngrediente.text = ingrediente.nombreIngrediente ?: "Ingrediente"

            // Marca / Presentación
            itemBinding.tvMarcaIngrediente.text = "Marca: ${ingrediente.marcaIngrediente ?: "Certificado"}"

            // Cantidad por defecto según posición o tipo
            val cantidadDefault = when {
                ingrediente.nombreIngrediente?.contains("huevo", true) == true -> "9.7 kg"
                ingrediente.nombreIngrediente?.contains("arroz", true) == true -> "8.4 kg"
                ingrediente.nombreIngrediente?.contains("tomate", true) == true -> "3.6 kg"
                ingrediente.nombreIngrediente?.contains("cebolla", true) == true -> "1.8 kg"
                ingrediente.nombreIngrediente?.contains("leche", true) == true -> "31.0 L"
                else -> "2.5 kg"
            }
            itemBinding.tvCantidadIngrediente.text = cantidadDefault

            // Descripción / función para dar sabor
            val descripcion = if (!ingrediente.descripcion.isNullOrBlank()) {
                ingrediente.descripcion
            } else {
                obtenerDescripcionSabor(ingrediente.nombreIngrediente ?: "")
            }
            itemBinding.tvDescripcionIngrediente.text = descripcion

            contenedor.addView(itemBinding.root)
        }
    }

    private fun obtenerDescripcionSabor(nombre: String): String {
        val n = nombre.lowercase().trim()
        return when {
            n.contains("sal") -> "Sazón base y realce del sabor tradicional en cocciones y marinados"
            n.contains("oregano") || n.contains("orégano") -> "Especia aromática para sazonar carnes, guisos y dar fragancia"
            n.contains("ajo") -> "Aromatizante y condimento base indispensable para el sofrito criollo"
            n.contains("comino") -> "Condimento aromático tradicional para dar profundidad al sabor de granos y carnes"
            n.contains("color") || n.contains("triguisar") || n.contains("achiote") -> "Aporte de color apetitoso y sabor tradicional para guisos"
            n.contains("pimienta") -> "Toque especiado suave para marinar y despertar los sabores del adobo"
            n.contains("aceite") -> "Materia grasa vegetal para sofreír, dorar y amalgamar sabores"
            n.contains("cebolla") || n.contains("tomate") -> "Base de hortalizas frescas para hogao y sazón de guisos"
            n.contains("canela") -> "Especia dulce aromática para postres y bebidas calientes"
            n.contains("azucar") || n.contains("azúcar") -> "Endulzante medido para balance de sabor"
            n.contains("leche") -> "Lácteo cremoso base para bebidas y preparaciones suaves"
            n.contains("arroz") -> "Cereal de grano entero cocido con toque de ajo y sal"
            n.contains("frijol") || n.contains("fríjol") -> "Leguminosa rica en proteína cocida con hogao criollo"
            n.contains("carne") || n.contains("pollo") -> "Proteína magra marinada con sal, especias y hierbas"
            else -> "Insumo certificado para preparación y sazón con estándar de calidad"
        }
    }
}
