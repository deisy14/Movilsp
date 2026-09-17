package com.example.psirae

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class activity_comfrimacion : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        // Cargar la pantalla
        setContentView(R.layout.activity_comfrimacion)

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
        // HORA DE FINALIZACIÓN
        // ==========================================

        val txtHora = findViewById<TextView>(R.id.txtHora)

        val horaActual = SimpleDateFormat(
            "hh:mm a",
            Locale("es", "CO")
        ).format(Date())

        val horaEspañol = horaActual
            .replace("AM", "a. m.")
            .replace("PM", "p. m.")

        txtHora.text = horaEspañol

        // ==========================================
        // BOTÓN: VER MENÚ DEL DÍA
        // ==========================================

        val btnVerMenu = findViewById<View>(R.id.btnVerMenu)

        btnVerMenu.setOnClickListener {

            val intent = Intent(
                this,
                activity_menu_dia::class.java
            )

            intent.flags =
                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP

            startActivity(intent)

            finish()
        }

        // ==========================================
        // BOTÓN: VOLVER AL INICIO
        // ==========================================

        val btnVolverInicio =
            findViewById<View>(R.id.btnVolverInicio)

        btnVolverInicio.setOnClickListener {

            val intent = Intent(
                this,
                MainActivity::class.java
            )

            intent.flags =
                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP

            startActivity(intent)

            finish()
        }
    }
}