package com.example.movilmanupuladora.ui.manipuladora_Jefe

import com.example.movilmanupuladora.R

import android.app.AlertDialog
import android.os.Bundle
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.movilmanupuladora.databinding.ActivityManipuladorasJefaBinding
import com.example.movilmanupuladora.utils.NavigationHelperJefa

class ManipuladorasJefaActivity : AppCompatActivity() {

    private lateinit var binding: ActivityManipuladorasJefaBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityManipuladorasJefaBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        configurarEventos()
        configurarNavegacionInferior()
        cargarDatos()
    }

    private fun configurarEventos() {
        binding.cardManipuladora1.setOnClickListener {
            mostrarDetalleManipuladora(0)
        }

        binding.cardManipuladora2.setOnClickListener {
            mostrarDetalleManipuladora(1)
        }

        binding.cardManipuladora3.setOnClickListener {
            mostrarDetalleManipuladora(2)
        }
    }

    private fun configurarNavegacionInferior() {
        NavigationHelperJefa.setupBarraNavegacionJefa(
            this,
            binding.barraNavegacionJefa,
            NavigationHelperJefa.TabJefa.MANIPULADORAS
        )
    }

    private fun cargarDatos() {
        val lista = ManipuladorasManager.obtenerManipuladoras()
        val total = lista.size
        val activas = lista.count { it.activa }

        binding.txtTotalManipuladoras.text = total.toString()
        binding.txtManipuladorasActivas.text = activas.toString()

        if (lista.isNotEmpty()) {
            cargarManipuladora(
                lista[0],
                binding.txtNombreManipuladora1,
                binding.txtFuncionManipuladora1,
                binding.txtJornadaManipuladora1,
                binding.txtEstadoManipuladora1
            )
            binding.cardManipuladora1.visibility = android.view.View.VISIBLE
        } else {
            binding.cardManipuladora1.visibility = android.view.View.GONE
        }

        if (lista.size > 1) {
            cargarManipuladora(
                lista[1],
                binding.txtNombreManipuladora2,
                binding.txtFuncionManipuladora2,
                binding.txtJornadaManipuladora2,
                binding.txtEstadoManipuladora2
            )
            binding.cardManipuladora2.visibility = android.view.View.VISIBLE
        } else {
            binding.cardManipuladora2.visibility = android.view.View.GONE
        }

        if (lista.size > 2) {
            cargarManipuladora(
                lista[2],
                binding.txtNombreManipuladora3,
                binding.txtFuncionManipuladora3,
                binding.txtJornadaManipuladora3,
                binding.txtEstadoManipuladora3
            )
            binding.cardManipuladora3.visibility = android.view.View.VISIBLE
        } else {
            binding.cardManipuladora3.visibility = android.view.View.GONE
        }
    }

    private fun cargarManipuladora(
        manipuladora: ManipuladorasManager.Manipuladora,
        nombre: android.widget.TextView,
        funcion: android.widget.TextView,
        jornada: android.widget.TextView,
        estado: android.widget.TextView
    ) {
        nombre.text = manipuladora.nombre
        funcion.text = manipuladora.funcion
        jornada.text = manipuladora.jornada

        if (manipuladora.activa) {
            estado.text = "Activa"
            estado.setTextColor(getColor(R.color.negro_principal))
            estado.setBackgroundResource(R.drawable.bg_estado_activo)
        } else {
            estado.text = "Inactiva"
            estado.setTextColor(getColor(R.color.blanco))
            estado.setBackgroundResource(R.drawable.bg_estado_inactivo)
        }
    }

    private fun mostrarDetalleManipuladora(posicion: Int) {
        val lista = ManipuladorasManager.obtenerManipuladoras()
        if (posicion < 0 || posicion >= lista.size) return

        val manipuladora = lista[posicion]
        AlertDialog.Builder(this)
            .setTitle(manipuladora.nombre)
            .setMessage("Función: ${manipuladora.funcion}\nJornada: ${manipuladora.jornada}\nEstado: ${manipuladora.estado}")
            .setPositiveButton("Cerrar", null)
            .setNeutralButton("Cambiar Estado") { _, _ ->
                ManipuladorasManager.actualizarEstado(manipuladora.id, !manipuladora.activa, if (!manipuladora.activa) "Activa" else "Inactiva")
                cargarDatos()
                Toast.makeText(this, "Estado actualizado", Toast.LENGTH_SHORT).show()
            }
            .show()
    }

    override fun onResume() {
        super.onResume()
        if (::binding.isInitialized) {
            cargarDatos()
        }
    }
}
