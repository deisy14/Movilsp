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
import com.example.movilmanupuladora.data.model.MenuPaeCatalog
import com.example.movilmanupuladora.data.model.PlatoResponse
import com.example.movilmanupuladora.data.repository.MenuRepository
import com.example.movilmanupuladora.databinding.ActivityTodosLosPlatosBinding
import com.example.movilmanupuladora.databinding.DialogDetallePlatoBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch

class TodosLosPlatosActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTodosLosPlatosBinding
    private val menuRepository = MenuRepository(RetrofitClient.apiService)

    private val platosRespaldo = MenuPaeCatalog.menus.map {
        PlatoResponse(idPlato = it.id, nombrePlato = it.nombre, componente = it.componente)
    }

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
        // Mostrar el catálogo oficial completo del PAE con los nombres e imágenes actualizados
        mostrarLista(platosRespaldo)

        lifecycleScope.launch {
            try {
                val res = menuRepository.obtenerPlatos()
                if (res.isSuccessful && !res.body().isNullOrEmpty()) {
                    val lista = res.body()!!
                    // Solo si el backend devuelve el catálogo completo de 15 o más platos
                    if (lista.size >= 15) {
                        mostrarLista(lista)
                    }
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

        dialogBinding.tvTituloPlatoModal.text = plato.nombrePlato ?: "Plato PAE"
        dialogBinding.tvSeccionModal.text = "Menú Disponible PAE"
        dialogBinding.tvComponenteModal.text = "Componente: ${plato.componente ?: "Sopas, Sazón y Proteína"}"
        dialogBinding.tvPorcionModal.text = "100g aprox. por ración"
        dialogBinding.tvTotalPrepararModal.text = "120 porciones"
        dialogBinding.tvBadgeEstadoModal.text = "Disponible"

        val imgRes = if (plato.idPlato in 1..20) {
            MenuPaeCatalog.obtenerMenuPorId(plato.idPlato).imagenRes
        } else {
            MenuPaeCatalog.obtenerImagenPorNombre(plato.nombrePlato ?: plato.componente)
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