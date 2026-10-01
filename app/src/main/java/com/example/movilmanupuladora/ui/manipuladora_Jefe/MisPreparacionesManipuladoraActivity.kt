package com.example.manipuladorajefe

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.manipuladorajefe.databinding.ActivityMisPreparacionesManipuladoraBinding

class MisPreparacionesManipuladoraActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMisPreparacionesManipuladoraBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityMisPreparacionesManipuladoraBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        try {
            binding.root.findViewById<android.view.View>(R.id.iconoCocina)?.setOnClickListener {
                startActivity(Intent(this, PreparacionEnCursoActivity::class.java))
            }
        } catch (e: Exception) {
            // fallback
        }
    }
}