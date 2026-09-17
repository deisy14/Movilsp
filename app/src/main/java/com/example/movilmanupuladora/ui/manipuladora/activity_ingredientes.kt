package com.example.movilmanupuladora.ui.manipuladora

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.movilmanupuladora.R

class activity_ingredientes : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setContentView(R.layout.activity_ingredientes)

        // ==========================================
        // BARRAS DEL SISTEMA
        // ==========================================

        val main = findViewById<View>(R.id.main)

        ViewCompat.setOnApplyWindowInsetsListener(main) { v, insets ->

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

        // ==========================================
        // BOTÓN CONTINUAR A PREPARACIÓN
        // ==========================================

        val btnContinuarPreparacion =
            findViewById<Button>(R.id.btnContinuarPreparacion)

        btnContinuarPreparacion.setOnClickListener {

            val intent = Intent(
                this,
                activity_preparacion::class.java
            )

            startActivity(intent)
        }
    }
}