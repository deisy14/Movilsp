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
import com.example.movilmanupuladora.R
import com.example.movilmanupuladora.data.api.RetrofitClient
import com.example.movilmanupuladora.data.model.MenuPaeCatalog
import com.example.movilmanupuladora.data.model.PlatoResponse
import com.example.movilmanupuladora.data.model.SugerenciaRequest
import com.example.movilmanupuladora.data.model.SugerenciaRecetaResponse
import com.example.movilmanupuladora.databinding.ActivityMenuDiaBinding
import com.example.movilmanupuladora.utils.SessionManager
import kotlinx.coroutines.launch

class MenuDiaActivity : AppCompatActivity() {

    private lateinit var sessionManager: SessionManager
    private lateinit var binding: ActivityMenuDiaBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Restaurar token de sesión
        sessionManager = SessionManager(this)
        RetrofitClient.authToken = sessionManager.fetchAuthToken()

        enableEdgeToEdge()

        // Inicializar la vista
        binding = ActivityMenuDiaBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Ajustar la pantalla a las barras del sistema
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars =
                insets.getInsets(WindowInsetsCompat.Type.systemBars())

            v.setPadding(
                systemBars.left,
                systemBars.top,
                systemBars.right,
                systemBars.bottom
            )

            insets
        }

        // Botón para ver componentes
        binding.btnVerComponentes.setOnClickListener {
            val intent = Intent(
                this@MenuDiaActivity,
                PreparacionActivity::class.java
            )
            startActivity(intent)
        }

        findViewById<View>(R.id.navAvisos)?.setOnClickListener {
            startActivity(
                Intent(
                    this@MenuDiaActivity,
                    AvisosActivity::class.java
                )
            )
        }

        // Cargar platos desde el backend
        cargarPlatos()
    }

    private fun cargarPlatos() {
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.obtenerPlatos()

                if (response.isSuccessful && !response.body().isNullOrEmpty() && response.body()!!.size >= 15) {
                    val platos = response.body()!!
                    mostrarPlatos(platos)
                } else {
                    val platosPae = MenuPaeCatalog.menus.map {
                        PlatoResponse(idPlato = it.id, nombrePlato = it.nombre, componente = it.componente)
                    }
                    mostrarPlatos(platosPae)
                }
            } catch (_: Exception) {
                val platosPae = MenuPaeCatalog.menus.map {
                    PlatoResponse(idPlato = it.id, nombrePlato = it.nombre, componente = it.componente)
                }
                mostrarPlatos(platosPae)
            }
        }
    }

    private fun mostrarPlatos(platos: List<PlatoResponse>) {
        val contenedorPrincipal =
            findViewById<LinearLayout>(R.id.contenedorPlatoPrincipal)

        val contenedorOtros =
            findViewById<LinearLayout>(R.id.contenedorOtrosPlatos)

        val tvOtrosPlatos =
            findViewById<TextView>(R.id.tvOtrosPlatos)

        contenedorPrincipal?.removeAllViews()
        contenedorOtros?.removeAllViews()

        if (platos.isEmpty()) return

        // Primer plato destacado
        val principal = platos.first()

        val cardPrincipal = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundResource(R.drawable.bg_seccion_seleccionada)
            setPadding(36, 36, 36, 36)
            gravity = android.view.Gravity.CENTER_VERTICAL
        }

        val infoLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
        }

        val tvSeccion = TextView(this).apply {
            text = principal.componente ?: "Plato del día"
            setTextColor(0xFF173F2A.toInt())
            textSize = 15f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        }

        val tvAsignado = TextView(this).apply {
            text = "Plato asignado"
            setTextColor(0xFF65736A.toInt())
            textSize = 12f
            setPadding(0, 8, 0, 0)
        }

        val tvNombrePrincipal = TextView(this).apply {
            text = principal.nombrePlato ?: "Sin nombre"
            setTextColor(0xFF173F2A.toInt())
            textSize = 20f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setPadding(0, 6, 0, 0)
        }

        infoLayout.addView(tvSeccion)
        infoLayout.addView(tvAsignado)
        infoLayout.addView(tvNombrePrincipal)

        cardPrincipal.addView(infoLayout)

        // Evento de clic en el plato principal para invocar la IA adaptada
        cardPrincipal.setOnClickListener {
            val idMenuActual = 1
            val idPlatoActual = principal.idPlato ?: 1
            solicitarSugerenciaIA(idMenuActual, idPlatoActual)
        }

        contenedorPrincipal?.addView(cardPrincipal)

        // Otros platos
        if (platos.size > 1) {
            tvOtrosPlatos?.visibility = View.VISIBLE
            tvOtrosPlatos?.text = "Otros platos del menú"

            for (i in 1 until platos.size) {
                val plato = platos[i]

                val fila = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = android.view.Gravity.CENTER_VERTICAL
                    setPadding(20, 26, 20, 26)

                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).also {
                        it.topMargin = 20
                    }
                }

                val tvNombre = TextView(this).apply {
                    text = plato.nombrePlato ?: "Sin nombre"
                    setTextColor(0xFF34443A.toInt())
                    textSize = 14f

                    layoutParams = LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1f
                    ).also {
                        it.marginStart = 30
                    }
                }

                val tvFlecha = TextView(this).apply {
                    text = "›"
                    setTextColor(0xFF173F2A.toInt())
                    textSize = 28f
                }

                fila.addView(tvNombre)
                fila.addView(tvFlecha)

                // Evento de clic en los otros platos para abrir ingredientes o solicitar IA
                fila.setOnClickListener {
                    val idMenuActual = 1
                    val idPlatoActual = plato.idPlato ?: 1
                    solicitarSugerenciaIA(idMenuActual, idPlatoActual)
                }

                contenedorOtros?.addView(fila)

                // Línea separadora
                val linea = View(this).apply {
                    setBackgroundColor(0xFFEDEDED.toInt())

                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        2
                    )
                }

                contenedorOtros?.addView(linea)
            }
        } else {
            tvOtrosPlatos?.visibility = View.GONE
        }
    }

    // --- Integración de Sugerencia de Receta con IA adaptada al Inventario (Corrutina) ---
    private fun solicitarSugerenciaIA(idMenu: Int, idPlato: Int) {
        val request = SugerenciaRequest(id_menu = idMenu, id_plato = idPlato)

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.obtenerSugerenciaReceta(request)
                if (response.isSuccessful && response.body() != null) {
                    val data = response.body()!!
                    mostrarIngredientesAdaptados(data)
                } else {
                    Toast.makeText(this@MenuDiaActivity, "Error al calcular sugerencia", Toast.LENGTH_SHORT).show()
                }
            } catch (t: Throwable) {
                Toast.makeText(this@MenuDiaActivity, "Fallo de red: ${t.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun mostrarIngredientesAdaptados(sugerencia: SugerenciaRecetaResponse) {
        // Recorre los ingredientes calculados por el backend en función de los niños presentes y stock
        for (ingrediente in sugerencia.ingredientes) {
            val nombre = ingrediente.nombre
            val cantidadTotal = ingrediente.cantidad_total
            val unidad = ingrediente.unidad
            val alcanza = ingrediente.alcanza // Indica si hay suficiente stock en la sede
        }

        Toast.makeText(this, "Sugerencia cargada para: ${sugerencia.nombre_plato}", Toast.LENGTH_SHORT).show()
    }
}