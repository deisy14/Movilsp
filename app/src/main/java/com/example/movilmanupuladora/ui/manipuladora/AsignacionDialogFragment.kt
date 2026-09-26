package com.example.movilmanupuladora.ui.manipuladora

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.example.movilmanupuladora.R
import com.example.movilmanupuladora.databinding.DialogDetalleAsignacionBinding

class AsignacionDialogFragment : DialogFragment() {

    private var _binding: DialogDetalleAsignacionBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = DialogDetalleAsignacionBinding.inflate(
            inflater,
            container,
            false
        )

        return binding.root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)

        cargarDatos()
        configurarBotones()
    }

    private fun cargarDatos() {

        val nombre = arguments?.getString(ARG_NOMBRE)
            ?: "Manipuladora"

        val estado = arguments?.getString(ARG_ESTADO)
            ?: "Asignada"

        val plato = arguments?.getString(ARG_PLATO)
            ?: "Plato asignado"

        val imagen = arguments?.getInt(ARG_IMAGEN)
            ?: R.drawable.frijoles

        val pasos = arguments?.getStringArrayList(ARG_PASOS)
            ?: arrayListOf()

        val ingredientes = arguments?.getStringArrayList(ARG_INGREDIENTES)
            ?: arrayListOf()


        // TÍTULO
        binding.tvAsignacionTitulo.text =
            "ASIGNACIÓN: ${nombre.uppercase()}"


        // IMAGEN MANIPULADORA
        binding.imgManipuladoraDetalle.setImageResource(
            R.drawable.ic_person
        )


        // PLATO
        binding.tvNombrePlatoAsignacion.text = plato


        // ESTADO
        binding.tvEstadoAsignacion.text = estado


        // PASOS
        binding.tvPaso1.text =
            if (pasos.isNotEmpty()) {
                "Paso 1: ${pasos[0]}"
            } else {
                "Paso 1: No registrado"
            }

        binding.tvPaso2.text =
            if (pasos.size > 1) {
                "Paso 2: ${pasos[1]}"
            } else {
                "Paso 2: No registrado"
            }

        binding.tvPaso3.text =
            if (pasos.size > 2) {
                "Paso 3: ${pasos[2]}"
            } else {
                "Paso 3: No registrado"
            }


        // INGREDIENTES
        binding.tvIngredientesAsignacion.text =
            if (ingredientes.isEmpty()) {
                "• No hay ingredientes registrados."
            } else {
                ingredientes.joinToString("\n") {
                    "• $it"
                }
            }
    }


    private fun configurarBotones() {

        // CERRAR
        binding.btnCerrarDetalle.setOnClickListener {
            dismiss()
        }


        // MARCAR COMPLETADO
        binding.btnMarcarCompletado.setOnClickListener {

            binding.tvEstadoAsignacion.text = "Completado"

            binding.btnMarcarCompletado.text = "Completado"

            binding.btnMarcarCompletado.isEnabled = false
        }
    }


    override fun onStart() {

        super.onStart()

        dialog?.window?.let { window ->

            window.setBackgroundDrawableResource(
                android.R.color.transparent
            )

            val ancho =
                (resources.displayMetrics.widthPixels * 0.90).toInt()

            window.setLayout(
                ancho,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }
    }


    override fun onDestroyView() {

        super.onDestroyView()

        _binding = null
    }


    companion object {

        private const val ARG_NOMBRE = "nombre"
        private const val ARG_ESTADO = "estado"
        private const val ARG_PLATO = "plato"
        private const val ARG_IMAGEN = "imagen"
        private const val ARG_PASOS = "pasos"
        private const val ARG_INGREDIENTES = "ingredientes"


        fun newInstance(
            nombre: String,
            estado: String,
            plato: String,
            imagen: Int,
            pasos: List<String>,
            ingredientes: List<String>
        ): AsignacionDialogFragment {

            return AsignacionDialogFragment().apply {

                arguments = Bundle().apply {

                    putString(
                        ARG_NOMBRE,
                        nombre
                    )

                    putString(
                        ARG_ESTADO,
                        estado
                    )

                    putString(
                        ARG_PLATO,
                        plato
                    )

                    putInt(
                        ARG_IMAGEN,
                        imagen
                    )

                    putStringArrayList(
                        ARG_PASOS,
                        ArrayList(pasos)
                    )

                    putStringArrayList(
                        ARG_INGREDIENTES,
                        ArrayList(ingredientes)
                    )
                }
            }
        }
    }
}