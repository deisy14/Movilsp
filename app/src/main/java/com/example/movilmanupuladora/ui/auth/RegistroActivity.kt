package com.example.movilmanupuladora.ui.auth
/*
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.movilmanipuladora.data.api.RetrofitClient
import com.example.movilmanipuladora.data.model.Rol
import com.example.movilmanipuladora.data.model.Usuario
import com.example.movilmanipuladora.data.repository.RolRepository
import com.example.movilmanipuladora.data.repository.UsuarioRepository
import com.example.movilmanipuladora.databinding.ActivityRegistroBinding // Si usas ViewBinding
import kotlinx.coroutines.launch

class RegistroActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegistroBinding

    private val rolRepository = RolRepository(RetrofitClient.apiService)
    private val usuarioRepository = UsuarioRepository(RetrofitClient.apiService)

    private var listaRoles: List<Rol> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegistroBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 1. Cargar la lista de roles apenas abre la pantalla
        cargarRoles()

        // 2. Evento del botón registrar
        binding.btnRegistrar.setOnClickListener {
            registrarUsuario()
        }
    }

    private fun cargarRoles() {
        lifecycleScope.launch {
            try {
                val response = rolRepository.obtenerRoles()
                if (response.isSuccessful && response.body() != null) {
                    listaRoles = response.body()!!

                    // Llenar el Spinner con los nombres de los roles
                    val nombresRoles = listaRoles.map { it.nombre }
                    val adapter = ArrayAdapter(
                        this@RegistroActivity,
                        android.R.layout.simple_spinner_dropdown_item,
                        nombresRoles
                    )
                    binding.spinnerRoles.adapter = adapter
                } else {
                    Toast.makeText(this@RegistroActivity, "Error al cargar roles", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@RegistroActivity, "Error de red: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun registrarUsuario() {
        val nombre = binding.etNombre.text.toString().trim()
        val apellido = binding.etApellido.text.toString().trim()
        val correo = binding.etCorreo.text.toString().trim()
        val tipoDoc = binding.etTipoDoc.text.toString().trim()
        val numDoc = binding.etNumDoc.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()

        // Validar campos vacíos
        if (nombre.isEmpty() || correo.isEmpty() || password.isEmpty() || numDoc.isEmpty()) {
            Toast.makeText(this, "Por favor completa todos los campos requeridos", Toast.LENGTH_SHORT).show()
            return
        }

        // Obtener el ID del rol seleccionado en el Spinner
        val posicionSeleccionada = binding.spinnerRoles.selectedItemPosition
        val idRolSeleccionado = if (listaRoles.isNotEmpty()) listaRoles[posicionSeleccionada].idRol else null

        // Construir el objeto Usuario
        val nuevoUsuario = Usuario(
            nombre = nombre,
            apellido = apellido,
            correo = correo,
            tipoDocumento = tipoDoc,
            numeroDocumento = numDoc,
            rol = idRolSeleccionado,
            password = password
        )

        // Enviar la petición POST al backend
        lifecycleScope.launch {
            try {
                val response = usuarioRepository.registrarUsuario(nuevoUsuario)
                if (response.isSuccessful && response.body() != null) {
                    val respuesta = response.body()!!
                    Toast.makeText(this@RegistroActivity, respuesta.mensaje, Toast.LENGTH_LONG).show()

                    // Cerrar pantalla o redirigir al Login
                    finish()
                } else {
                    Toast.makeText(this@RegistroActivity, "Error en el registro: ${response.code()}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@RegistroActivity, "Error de conexión: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
*/