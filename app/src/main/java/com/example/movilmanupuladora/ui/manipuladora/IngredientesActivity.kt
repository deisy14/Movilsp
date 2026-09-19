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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setContentView(R.layout.activity_ingredientes)

        // Barras del sistema
        val main = findViewById<View>(R.id.main)
        ViewCompat.setOnApplyWindowInsetsListener(main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Botón volver
        findViewById<View>(R.id.btnVolver)?.setOnClickListener {
            finish()
        }

        // Mostrar nombre del plato y componente si viene del intent
        val nombrePlato = intent.getStringExtra("nombre_plato") ?: "Bandeja Paisa"
        val componenteSeleccionado = intent.getStringExtra("componente_seleccionado")

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
            nombrePlato.contains("bandeja", ignoreCase = true) -> R.drawable.bandeja_paisa
            nombrePlato.contains("frijol", ignoreCase = true) -> R.drawable.frijoles
            nombrePlato.contains("chocolate", ignoreCase = true) -> R.drawable.chocolate
            nombrePlato.contains("huevo", ignoreCase = true) -> R.drawable.huevo_perico
            nombrePlato.contains("pollo", ignoreCase = true) -> R.drawable.apanado
            nombrePlato.contains("arroz", ignoreCase = true) -> R.drawable.arroz_de_leche
            else -> R.drawable.frijoles
        }
        imgPlato?.setImageResource(imgRes)

        // Botón continuar a preparación
        val btnContinuarPreparacion = findViewById<Button>(R.id.btnContinuarPreparacion)
        btnContinuarPreparacion.setOnClickListener {
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

    private val ingredientesFallback = listOf(
        Ingrediente(
            idIngrediente = 1,
            nombreIngrediente = "Sal yodada y fluorada",
            descripcion = "Sazón base y realce del sabor tradicional en cocciones, marinados y fondos",
            marcaIngrediente = "Refisal / 500g"
        ),
        Ingrediente(
            idIngrediente = 2,
            nombreIngrediente = "Orégano seco molido",
            descripcion = "Especia aromática para sazonar carnes, guisos y aportar fragancia al plato",
            marcaIngrediente = "El Rey / 50g"
        ),
        Ingrediente(
            idIngrediente = 3,
            nombreIngrediente = "Ajo blanco fresco picado",
            descripcion = "Aromatizante y condimento base indispensable para el sofrito y sofrito criollo",
            marcaIngrediente = "Finca El Campo / 250g"
        ),
        Ingrediente(
            idIngrediente = 4,
            nombreIngrediente = "Comino molido criollo",
            descripcion = "Condimento aromático tradicional para dar profundidad al sabor de fríjoles y carnes",
            marcaIngrediente = "El Rey / 50g"
        ),
        Ingrediente(
            idIngrediente = 5,
            nombreIngrediente = "Color / Triguisar con achiote",
            descripcion = "Condimento natural para dar apetitoso color dorado y sabor a guisos y sopas",
            marcaIngrediente = "Triguisar / 70g"
        ),
        Ingrediente(
            idIngrediente = 6,
            nombreIngrediente = "Pimienta negra molida",
            descripcion = "Toque especiado suave para marinar y despertar los sabores del adobo",
            marcaIngrediente = "El Rey / 40g"
        ),
        Ingrediente(
            idIngrediente = 7,
            nombreIngrediente = "Aceite vegetal institucional",
            descripcion = "Materia grasa vegetal para sofreír condimentos, dorar y cocinar las raciones",
            marcaIngrediente = "Premier / 500ml"
        ),
        Ingrediente(
            idIngrediente = 8,
            nombreIngrediente = "Cebolla cabezona y tomate fresco",
            descripcion = "Hortalizas frescas para elaborar la base del sofrito u hogao tradicional",
            marcaIngrediente = "Huerta Institucional / 1kg"
        ),
        Ingrediente(
            idIngrediente = 9,
            nombreIngrediente = "Arroz blanco de grano largo",
            descripcion = "Cereal base de acompañamiento cocinado al punto con toque de sal y ajo",
            marcaIngrediente = "Diana / 1kg"
        ),
        Ingrediente(
            idIngrediente = 10,
            nombreIngrediente = "Proteína cárnica seleccionada",
            descripcion = "Corte magro fresco certificado bajo estándar nutricional del programa",
            marcaIngrediente = "Carnes del Campo / 1kg"
        )
    )

    private fun cargarIngredientes() {
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.obtenerIngredientes()

                if (response.isSuccessful && !response.body().isNullOrEmpty()) {
                    val listaBackend = response.body()!!
                    val tieneSal = listaBackend.any { it.nombreIngrediente?.contains("sal", ignoreCase = true) == true }
                    val tieneOregano = listaBackend.any { it.nombreIngrediente?.contains("or", ignoreCase = true) == true }

                    val listaFinal = if (!tieneSal || !tieneOregano) {
                        // Complementar con los ingredientes de sazón requeridos si no están en el backend
                        val condimentosFaltantes = ingredientesFallback.filter { fallback ->
                            listaBackend.none { backend ->
                                backend.nombreIngrediente?.contains(fallback.nombreIngrediente?.split(" ")?.firstOrNull() ?: "", ignoreCase = true) == true
                            }
                        }
                        listaBackend + condimentosFaltantes
                    } else {
                        listaBackend
                    }

                    mostrarIngredientes(listaFinal)
                } else {
                    mostrarIngredientes(ingredientesFallback)
                }
            } catch (e: Exception) {
                mostrarIngredientes(ingredientesFallback)
            }
        }
    }

    private fun mostrarIngredientes(ingredientes: List<Ingrediente>) {
        val contenedor = findViewById<LinearLayout>(R.id.contenedorIngredientes) ?: return
        contenedor.removeAllViews()

        for (ingrediente in ingredientes) {
            val itemBinding = com.example.movilmanupuladora.databinding.ItemIngredienteTablaBinding.inflate(
                layoutInflater,
                contenedor,
                false
            )

            // Nombre
            itemBinding.tvNombreIngrediente.text = ingrediente.nombreIngrediente ?: "Ingrediente"

            // Marca / Presentación
            itemBinding.tvMarcaIngrediente.text = ingrediente.marcaIngrediente ?: "Certificado"

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