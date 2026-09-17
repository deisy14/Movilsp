package com.example.movilmanupuladora.ui.manipuladora

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.movilmanupuladora.R
import com.example.psirae.MainActivity

class activity_turno : AppCompatActivity() {

    private val handler = Handler(Looper.getMainLooper())

    private val irSiguientePantalla = Runnable {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContentView(R.layout.activity_turno)

        // ==========================================
        // BARRAS DEL SISTEMA
        // ==========================================

        ViewCompat.setOnApplyWindowInsetsListener(
            findViewById(R.id.main)
        ) { v, insets ->

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
        // ESPERAR 15 SEGUNDOS
        // ==========================================

        handler.postDelayed(
            irSiguientePantalla,
            15_000
        )
    }

    override fun onDestroy() {
        super.onDestroy()

        // Evita ejecutar el cambio si la pantalla
        // se cierra antes de los 15 segundos
        handler.removeCallbacks(irSiguientePantalla)
    }
}