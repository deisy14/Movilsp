package com.example.movilmanupuladora.ui.manipuladora_Jefe

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.movilmanupuladora.R
import com.example.movilmanupuladora.data.api.RetrofitClient
import com.example.movilmanupuladora.data.model.PlatoResponse
import com.example.movilmanupuladora.data.repository.MenuRepository
import com.example.movilmanupuladora.databinding.ActivityTodosLosPlatosBinding
import com.example.movilmanupuladora.databinding.DialogDetallePlatoBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch

class TodosLosPlatosActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTodosLosPlatosBinding
    private val menuRepository = MenuRepository(RetrofitClient.apiService)

    private val platosRespaldo = listOf(
        PlatoResponse(idPlato = 1, nombrePlato = "Pollo Guisado Criollo", componente = "Pollo y Guiso"),
        PlatoResponse(idPlato = 2, nombrePlato = "Arroz con Pollo Especial", componente = "Arroz y Pollo"),
        PlatoResponse(idPlato = 3, nombrePlato = "Bandeja Paisa Tradicional", componente = "Frijoles y Carne"),
        PlatoResponse(idPlato = 4, nombrePlato = "Lentejas con Arroz", componente = "Leguminosa"),
        PlatoResponse(idPlato = 5, nombrePlato = "Pasta con Carne Molida", componente = "Proteína y Pasta")
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityTodosLosPlatosBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        binding.btnVolver.setOnClickListener { finish() }

        cargarPlatosBackend()
    }

    private fun cargarPlatosBackend() {
        lifecycleScope.launch {
            try {
                val res = menuRepository.obtenerPlatos()
                if (res.isSuccessful && !res.body().isNullOrEmpty()) {
                    val lista = res.body()!!
                    mostrarLista(lista)
                } else {
                    mostrarLista(platosRespaldo)
                }
            } catch (e: Exception) {
                mostrarLista(platosRespaldo)
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

        dialogBinding.tvTituloPlatoModal.text = plato.nombrePlato ?: "Plato PAE"
        dialogBinding.tvSeccionModal.text = "Menú Disponible PAE"
        dialogBinding.tvComponenteModal.text = "Componente: ${plato.componente ?: "Sopas, Sazón y Proteína"}"
        dialogBinding.tvPorcionModal.text = "100g aprox. por ración"
        dialogBinding.tvTotalPrepararModal.text = "120 porciones"
        dialogBinding.tvBadgeEstadoModal.text = "Disponible"

        val imgRes = when {
            (plato.nombrePlato ?: "").lowercase().contains("bandeja") -> R.drawable.bandeja_paisa
            (plato.nombrePlato ?: "").lowercase().contains("frijol") -> R.drawable.frijoles
            (plato.nombrePlato ?: "").lowercase().contains("chocolate") -> R.drawable.chocolate
            (plato.nombrePlato ?: "").lowercase().contains("huevo") -> R.drawable.huevo_perico
            (plato.nombrePlato ?: "").lowercase().contains("pasta") -> R.drawable.pasta_carne
            (plato.nombrePlato ?: "").lowercase().contains("lenteja") -> R.drawable.lentejas_arroz
            else -> R.drawable.pollo_guisado
        }
        dialogBinding.imgPlatoModal.setImageResource(imgRes)

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

        dialogBinding.btnOtroAleatorioModal.text = "Iniciar Preparación"
        dialogBinding.btnOtroAleatorioModal.setOnClickListener {
            dialog.dismiss()
            val intent = Intent(this, IniciarPreparacionActivity::class.java).apply {
                putExtra("nombre_menu", plato.nombrePlato)
                putExtra("id_plato", plato.idPlato)
            }
            startActivity(intent)
        }

        dialog.show()
    }
}