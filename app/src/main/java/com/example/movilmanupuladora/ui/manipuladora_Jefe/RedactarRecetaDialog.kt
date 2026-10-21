package com.example.movilmanupuladora.ui.manipuladora_Jefe

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.Window
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import com.example.movilmanupuladora.R
import com.google.android.material.button.MaterialButton

object RedactarRecetaDialog {

    fun mostrar(
        context: Context,
        onRecetaGuardada: ((nombre: String, ingredientes: String, receta: String) -> Unit)? = null
    ) {
        val dialog = Dialog(context)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)

        val inflater = LayoutInflater.from(context)
        val view = inflater.inflate(R.layout.dialog_redactar_receta, null)
        dialog.setContentView(view)

        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window?.setLayout(
            (context.resources.displayMetrics.widthPixels * 0.92).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

        val etNombrePlato = view.findViewById<EditText>(R.id.etNombrePlato)
        val etIngredientes = view.findViewById<EditText>(R.id.etIngredientes)
        val etReceta = view.findViewById<EditText>(R.id.etReceta)

        val btnCerrar = view.findViewById<ImageView>(R.id.btnCerrarModalReceta)
        val btnGuardar = view.findViewById<MaterialButton>(R.id.btnGuardarReceta)
        val btnCancelar = view.findViewById<MaterialButton>(R.id.btnCancelarReceta)

        btnCerrar?.setOnClickListener { dialog.dismiss() }
        btnCancelar?.setOnClickListener { dialog.dismiss() }

        btnGuardar?.setOnClickListener {
            val nombre = etNombrePlato.text.toString().trim()
            val ingredientes = etIngredientes.text.toString().trim()
            val receta = etReceta.text.toString().trim()

            if (nombre.isEmpty()) {
                etNombrePlato.error = "Ingresa el nombre del plato"
                return@setOnClickListener
            }

            if (ingredientes.isEmpty()) {
                etIngredientes.error = "Ingresa los ingredientes"
                return@setOnClickListener
            }

            if (receta.isEmpty()) {
                etReceta.error = "Ingresa la receta / pasos de preparación"
                return@setOnClickListener
            }

            Toast.makeText(context, "¡Receta '$nombre' guardada exitosamente!", Toast.LENGTH_LONG).show()
            onRecetaGuardada?.invoke(nombre, ingredientes, receta)
            dialog.dismiss()
        }

        dialog.show()
    }
}
