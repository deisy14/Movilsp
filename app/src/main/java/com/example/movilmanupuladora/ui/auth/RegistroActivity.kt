package com.example.movilmanupuladora.ui.auth

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.movilmanupuladora.data.api.RetrofitClient
import com.example.movilmanupuladora.data.model.Rol
import com.example.movilmanupuladora.data.model.Usuario
import com.example.movilmanupuladora.data.repository.RolRepository
import com.example.movilmanupuladora.data.repository.UsuarioRepository
// import com.example.movilmanupuladora.databinding.ActivityRegistroBinding // Comentado porque falta el layout
import kotlinx.coroutines.launch

class RegistroActivity : AppCompatActivity() {

    // private lateinit var binding: ActivityRegistroBinding

    private val rolRepository = RolRepository(RetrofitClient.apiService)
    private val usuarioRepository = UsuarioRepository(RetrofitClient.apiService)

    private var listaRoles: List<Rol> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // binding = ActivityRegistroBinding.inflate(layoutInflater)
        // setContentView(binding.root)
        
        Toast.makeText(this, "RegistroActivity: Layout missing", Toast.LENGTH_LONG).show()
    }
}
