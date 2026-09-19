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
import com.example.movilmanupuladora.data.model.SeccionMenu
import com.example.movilmanupuladora.data.repository.MenuRepository
import com.example.movilmanupuladora.databinding.ActivityComponentesBinding
import com.example.movilmanupuladora.databinding.ItemComponentePlatoBinding
import kotlinx.coroutines.launch

class activity_componentes : AppCompatActivity() {

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

        // 4. Botón Ver Ingredientes
        binding.btnVerIngredientes.setOnClickListener {
            val intent = Intent(this, activity_ingredientes::class.java)
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
        cargarDatosBackend(idPlatoIntent, idSeccionIntent)
    }

    private fun cargarDatosBackend(idPlatoBuscado: Int, idSeccionBuscada: Int) {
        lifecycleScope.launch {
            try {
                // A) Consumir Secciones de Menú
                val resSecciones = menuRepository.obtenerSeccionesMenu()
                var seccionNombre: String? = null
                if (resSecciones.isSuccessful) {
                    val secciones = resSecciones.body() ?: emptyList()
                    val seccionEncontrada = if (idSeccionBuscada != -1) {
                        secciones.firstOrNull { it.idSeccion == idSeccionBuscada }
                    } else {
                        secciones.firstOrNull()
                    }
                    seccionNombre = seccionEncontrada?.nombreSeccion
                    if (seccionNombre != null) {
                        binding.tvTituloSeccion.text = "Componentes de $seccionNombre"
                    }
                }

                // B) Consumir Platos
                val resPlatos = menuRepository.obtenerPlatos()
                if (resPlatos.isSuccessful) {
                    val listaPlatos = resPlatos.body() ?: emptyList()

                    if (listaPlatos.isNotEmpty()) {
                        // Plato seleccionado
                        platoActual = if (idPlatoBuscado != -1) {
                            listaPlatos.firstOrNull { it.idPlato == idPlatoBuscado } ?: listaPlatos.first()
                        } else {
                            listaPlatos.first()
                        }

                        // Actualizar UI del plato principal
                        binding.tvNombrePlato.text = platoActual?.nombrePlato ?: "Plato sin nombre"

                        // Renderizar lista dinámica de componentes (Principio, Proteína, Acompañamiento, etc.)
                        mostrarComponentes(listaPlatos)
                    }
                } else {
                    Log.e("Backend", "Error al obtener platos: ${resPlatos.code()}")
                }

                // C) Consumir Detalle de Platos (porciones, total a preparar y estado)
                val resDetalles = menuRepository.obtenerDetallePlatos()
                if (resDetalles.isSuccessful) {
                    val detalles = resDetalles.body() ?: emptyList()
                    val idPlato = platoActual?.idPlato
                    detalleActual = detalles.firstOrNull { it.idPlato == idPlato }

                    detalleActual?.let { detalle ->
                        // Si tiene estado de preparación en el backend, actualizar badge
                        detalle.estadoPreparacion?.let { estado ->
                            binding.tvBadgeEstado.text = estado
                        }
                    }
                }

            } catch (e: Exception) {
                Log.e("Backend", "Error de conexión con el backend", e)
                Toast.makeText(
                    this@activity_componentes,
                    "Conectando al backend SIRAE...",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    /**
     * Muestra dinámicamente cada plato/componente usando ViewBinding (ItemComponentePlatoBinding)
     */
    private fun mostrarComponentes(platos: List<PlatoResponse>) {
        // Limpiamos los elementos estáticos anteriores del contenedor
        binding.contenedorComponentes.removeAllViews()

        for (plato in platos) {
            val itemBinding = ItemComponentePlatoBinding.inflate(layoutInflater, binding.contenedorComponentes, false)

            // Nombre del plato / preparación
            itemBinding.tvNombreComponente.text = plato.nombrePlato ?: "Sin nombre"

            // Componente nutricional (ej: Proteína, Principio, Jugo, Sopa)
            val tipo = plato.componente
            if (!tipo.isNullOrEmpty()) {
                itemBinding.tvTipoComponente.visibility = View.VISIBLE
                itemBinding.tvTipoComponente.text = tipo
            } else {
                itemBinding.tvTipoComponente.visibility = View.GONE
            }

            // Al hacer clic en un componente, podemos actualizar el plato principal seleccionado
            itemBinding.root.setOnClickListener {
                platoActual = plato
                binding.tvNombrePlato.text = plato.nombrePlato ?: "Plato sin nombre"
            }

            binding.contenedorComponentes.addView(itemBinding.root)
        }
    }
}