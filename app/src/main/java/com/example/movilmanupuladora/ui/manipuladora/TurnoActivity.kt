package com.example.movilmanupuladora.ui.manipuladora

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.movilmanupuladora.databinding.ActivityTurnoBinding

class TurnoActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTurnoBinding

    private val handler = Handler(Looper.getMainLooper())

    private val irSiguientePantalla = Runnable {

        val intent = Intent(
            this,
            MainActivity::class.java
        )

        startActivity(intent)
        finish()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        // =====================================================
        // VIEW BINDING
        // =====================================================

        binding = ActivityTurnoBinding.inflate(layoutInflater)

        setContentView(binding.root)

        // =====================================================
        // BARRAS DEL SISTEMA
        // =====================================================

        ViewCompat.setOnApplyWindowInsetsListener(
            binding.main
        ) { view, insets ->

            val systemBars =
                insets.getInsets(
                    WindowInsetsCompat.Type.systemBars()
                )

            view.setPadding(
                systemBars.left,
                systemBars.top,
                systemBars.right,
                systemBars.bottom
            )

            insets
        }

        // =====================================================
        // CONTINUAR AUTOMÁTICAMENTE
        // =====================================================

        handler.postDelayed(
            irSiguientePantalla,
            15_000
        )
    }

    override fun onDestroy() {
        super.onDestroy()

        // Evita que la navegación se ejecute
        // si la Activity se destruye antes.
        handler.removeCallbacks(
            irSiguientePantalla
        )
    }
}