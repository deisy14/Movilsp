package com.example.movilmanupuladora.ui.manipuladora

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.movilmanupuladora.R
import com.example.movilmanupuladora.databinding.ActivityAsignadasBinding

class AsignadasActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAsignadasBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityAsignadasBinding.inflate(layoutInflater)
        setContentView(binding.root)

        configurarManipuladoras()
        configurarNavegacion()
    }

    /**
     * Datos temporales para probar la interfaz.
     * Después estos datos vendrán del backend.
     */
    private fun configurarManipuladoras() {

        // ==========================================
        // MANIPULADORA 1
        // ==========================================

        binding.cardManipuladora1.setOnClickListener {

            mostrarAsignacion(
                nombre = "Sofía",
                categoria = "Ensaladas",
                estado = "Ingredientes listos",
                plato = "Ensalada Premium",
                imagen = R.drawable.frijoles,
                pasos = listOf(
                    "Preparar la base de lechuga",
                    "Añadir los tomates y el aderezo",
                    "Servir en un bol"
                ),
                ingredientes = listOf(
                    "Lechuga Romana",
                    "Tomate Cherry",
                    "Aceitunas",
                    "Aderezo César"
                )
            )
        }

        // ==========================================
        // MANIPULADORA 2
        // ==========================================

        binding.cardManipuladora2.setOnClickListener {

            mostrarAsignacion(
                nombre = "Diego",
                categoria = "Ceviche",
                estado = "Ingredientes listos",
                plato = "Ceviche de pescado",
                imagen = R.drawable.apanado,
                pasos = listOf(
                    "Cortar el pescado",
                    "Agregar limón y verduras",
                    "Mezclar y dejar reposar",
                    "Servir"
                ),
                ingredientes = listOf(
                    "Pescado",
                    "Limón",
                    "Cebolla",
                    "Tomate",
                    "Cilantro"
                )
            )
        }

        // ==========================================
        // MANIPULADORA 3
        // ==========================================

        binding.cardManipuladora3.setOnClickListener {

            mostrarAsignacion(
                nombre = "Elena",
                categoria = "Postres",
                estado = "Ingredientes listos",
                plato = "Postre del día",
                imagen = R.drawable.arroz_de_leche,
                pasos = listOf(
                    "Preparar la mezcla",
                    "Cocinar los ingredientes",
                    "Dejar enfriar",
                    "Servir las porciones"
                ),
                ingredientes = listOf(
                    "Leche",
                    "Arroz",
                    "Azúcar",
                    "Canela"
                )
            )
        }

        // ==========================================
        // MANIPULADORA 4
        // ==========================================

        binding.cardManipuladora4.setOnClickListener {

            mostrarAsignacion(
                nombre = "Mara",
                categoria = "Asados",
                estado = "Ingredientes listos",
                plato = "Pollo asado",
                imagen = R.drawable.apanado,
                pasos = listOf(
                    "Preparar y sazonar el pollo",
                    "Llevar a cocción",
                    "Verificar el punto de cocción",
                    "Servir"
                ),
                ingredientes = listOf(
                    "Pollo",
                    "Sal",
                    "Condimentos",
                    "Aceite"
                )
            )
        }

        // ==========================================
        // MANIPULADORA 5
        // ==========================================

        binding.cardManipuladora5.setOnClickListener {

            mostrarAsignacion(
                nombre = "Riaro",
                categoria = "Almuerzo",
                estado = "Ingredientes listos",
                plato = "Bandeja Paisa",
                imagen = R.drawable.bandeja_paisa,
                pasos = listOf(
                    "Preparar el arroz",
                    "Preparar los frijoles",
                    "Preparar la proteína",
                    "Organizar todos los componentes",
                    "Servir"
                ),
                ingredientes = listOf(
                    "Arroz",
                    "Frijoles",
                    "Carne molida",
                    "Chicharrón",
                    "Aguacate"
                )
            )
        }

        // ==========================================
        // MANIPULADORA 6
        // ==========================================

        binding.cardManipuladora6.setOnClickListener {

            mostrarAsignacion(
                nombre = "Boron",
                categoria = "Desayuno",
                estado = "Ingredientes listos",
                plato = "Desayuno del día",
                imagen = R.drawable.huevo_perico,
                pasos = listOf(
                    "Preparar los ingredientes",
                    "Cocinar los huevos",
                    "Preparar el acompañamiento",
                    "Servir"
                ),
                ingredientes = listOf(
                    "Huevo",
                    "Tomate",
                    "Cebolla",
                    "Pan",
                    "Bebida"
                )
            )
        }
    }

    /**
     * Abre el flotante exclusivo de Asignadas.
     */
    private fun mostrarAsignacion(
        nombre: String,
        categoria: String,
        estado: String,
        plato: String,
        imagen: Int,
        pasos: List<String>,
        ingredientes: List<String>
    ) {

        val dialog = AsignacionDialogFragment.newInstance(
            nombre = nombre,
            estado = estado,
            plato = plato,
            imagen = imagen,
            pasos = pasos,
            ingredientes = ingredientes
        )

        dialog.show(
            supportFragmentManager,
            "AsignacionDialog"
        )
    }

    /**
     * Navegación inferior.
     */
    private fun configurarNavegacion() {
        com.example.movilmanupuladora.utils.NavigationHelper.setupBarraNavegacion(
            this,
            binding.barraNavegacion,
            com.example.movilmanupuladora.utils.NavigationHelper.Tab.ASIGNADAS
        )
    }
}