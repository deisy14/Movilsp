
package com.example.movilmanupuladora.ui.auth

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.movilmanupuladora.R
import com.example.movilmanupuladora.data.api.RetrofitClient
import com.example.movilmanupuladora.data.repository.UsuarioRepository
import com.example.movilmanupuladora.databinding.ActivityLoginBinding
import com.example.movilmanupuladora.ui.manipuladora.TurnoActivity
import com.example.movilmanupuladora.ui.manipuladora_Jefe.InicioJefaActivity
import com.example.movilmanupuladora.utils.SessionManager
import kotlinx.coroutines.launch
import org.json.JSONObject

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding

    private val usuarioRepository =
        UsuarioRepository(RetrofitClient.apiService)

    private lateinit var sessionManager: SessionManager

    private val recuperarPasswordLauncher = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
    ) { result ->

        if (result.resultCode == android.app.Activity.RESULT_OK) {

            val correo = result.data?.getStringExtra(
                RecuperarPasswordActivity.EXTRA_CORREO
            )

            val nuevaPwd = result.data?.getStringExtra("nueva_password")

            if (!correo.isNullOrEmpty()) {
                binding.txtCorreo.setText(correo)

                if (!nuevaPwd.isNullOrEmpty()) {
                    binding.txtPassword.setText(nuevaPwd)
                } else {
                    binding.txtPassword.setText("")
                }

                binding.txtPassword.requestFocus()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessionManager = SessionManager(this)

        // Recuperar el token guardado.
        sessionManager.fetchAuthToken()?.let { savedToken ->
            RetrofitClient.authToken = savedToken
        }

        // Mostrar u ocultar la contraseña.
        var passwordVisible = false

        binding.btnTogglePassword.setOnClickListener {

            passwordVisible = !passwordVisible

            if (passwordVisible) {

                binding.txtPassword.transformationMethod =
                    android.text.method.HideReturnsTransformationMethod.getInstance()

                binding.btnTogglePassword.setImageResource(
                    R.drawable.ic_visibility_white
                )

            } else {

                binding.txtPassword.transformationMethod =
                    android.text.method.PasswordTransformationMethod.getInstance()

                binding.btnTogglePassword.setImageResource(
                    R.drawable.ic_visibility_off_white
                )
            }

            binding.txtPassword.setSelection(
                binding.txtPassword.text.length
            )
        }

        // Botón ingresar.
        binding.btnIngresar.setOnClickListener {

            val correo = binding.txtCorreo.text
                .toString()
                .trim()

            // No modificar los espacios de la contraseña.
            val password = binding.txtPassword.text.toString()

            if (correo.isEmpty()) {
                binding.txtCorreo.requestFocus()
                binding.txtCorreo.error = "Ingresa tu correo"
                return@setOnClickListener
            }

            if (password.isEmpty()) {
                binding.txtPassword.requestFocus()
                binding.txtPassword.error = "Ingresa tu contraseña"
                return@setOnClickListener
            }

            ocultarTeclado()
            iniciarSesion(correo, password)
        }

        // Recuperación de contraseña.
        binding.txtOlvide.setOnClickListener {

            val correoActual =
                binding.txtCorreo.text.toString().trim()

            val intent = Intent(
                this,
                RecuperarPasswordActivity::class.java
            ).apply {

                if (correoActual.isNotEmpty()) {
                    putExtra(
                        RecuperarPasswordActivity.EXTRA_CORREO,
                        correoActual
                    )
                }
            }

            recuperarPasswordLauncher.launch(intent)
        }

        // Contactar al administrador.
        val clickAdminListener = View.OnClickListener {

            androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Contactar Administrador")
                .setMessage(
                    "Si tienes inconvenientes para ingresar a la " +
                            "aplicación móvil, comunícate con el " +
                            "Administrador institucional del sistema SIRAE."
                )
                .setPositiveButton("Aceptar", null)
                .show()
        }

        binding.txtAdministrador.setOnClickListener(clickAdminListener)
        binding.layoutAdminContacto.setOnClickListener(clickAdminListener)
    }

    // =========================================================
    // INICIAR SESIÓN
    // =========================================================

    private fun iniciarSesion(
        correo: String,
        pass: String
    ) {

        mostrarCargando(true)

        lifecycleScope.launch {

            try {

                val response = usuarioRepository.login(correo, pass)

                if (response.isSuccessful && response.body() != null) {

                    val loginRes = response.body()!!
                    val token = loginRes.authToken

                    // Comprobar que el backend haya enviado un token.
                    if (token.isNullOrBlank()) {

                        mostrarCargando(false)

                        Log.e(
                            "LOGIN_ERROR",
                            "El backend respondió correctamente, " +
                                    "pero no se recibió un token."
                        )

                        Toast.makeText(
                            this@LoginActivity,
                            "Error: Token no recibido",
                            Toast.LENGTH_LONG
                        ).show()

                        return@launch
                    }

                    // Identificar el rol y el correo.
                    val rol = (loginRes.usuario?.rol ?: "")
                        .trim()
                        .lowercase()

                    val email = (loginRes.usuario?.correo ?: correo)
                        .trim()
                        .lowercase()

                    // El administrador ingresa desde la página web.
                    if (rol.contains("admin")) {

                        mostrarCargando(false)

                        Toast.makeText(
                            this@LoginActivity,
                            "Este usuario debe ingresar desde la página web.",
                            Toast.LENGTH_LONG
                        ).show()

                        return@launch
                    }

                    // Guardar el token de autenticación.
                    RetrofitClient.authToken = token
                    sessionManager.saveAuthToken(token)

                    // Guardar los datos del usuario y su sede.
                    loginRes.usuario?.let { usuario ->

                        val nombreCompleto = listOf(
                            usuario.nombre,
                            usuario.apellido.orEmpty()
                        )
                            .filter { it.isNotBlank() }
                            .joinToString(" ")
                            .trim()

                        val correoFinal = usuario.correo
                            .takeIf { it.isNotBlank() }
                            ?: correo.trim()

                        sessionManager.saveUserData(
                            name = nombreCompleto,
                            role = usuario.rol,
                            email = correoFinal,
                            idUsuario = usuario.idUsuario,
                            idSede = usuario.idSede,
                            sedeNombre = usuario.sedeNombre
                        )

                        Log.d(
                            "SEDE_DEBUG",
                            "idSede=${usuario.idSede}, " +
                                    "sedeNombre=${usuario.sedeNombre}"
                        )
                    }

                    // Identificar a la jefa de manipuladoras.
                    val esJefa =
                        rol.contains("jefa") ||
                                rol.contains("jefe") ||
                                email.contains("jefe") ||
                                email.contains("jefa") ||
                                email.contains("isaperez") ||
                                email.contains("carlos")

                    // Determinar la pantalla de destino.
                    val destino = when {
                        esJefa -> InicioJefaActivity::class.java
                        rol.contains("manipuladora") -> TurnoActivity::class.java
                        else -> TurnoActivity::class.java
                    }

                    Toast.makeText(
                        this@LoginActivity,
                        "¡Bienvenido ${loginRes.usuario?.nombre ?: ""}!",
                        Toast.LENGTH_SHORT
                    ).show()

                    startActivity(
                        Intent(this@LoginActivity, destino)
                    )

                    finish()

                } else {

                    mostrarCargando(false)

                    val errorBody = response.errorBody()?.string()

                    // Registrar el error del backend sin mostrar contraseñas.
                    Log.e(
                        "LOGIN_ERROR",
                        "Código HTTP: ${response.code()} | " +
                                "Respuesta backend: ${errorBody ?: "(vacía)"}"
                    )

                    val msg = try {

                        val json = JSONObject(errorBody ?: "")

                        val detail = json.optString(
                            "detail",
                            json.optString("error", "")
                        ).trim()

                        when {

                            detail.contains(
                                "No existe ninguna cuenta",
                                ignoreCase = true
                            ) -> "Correo no registrado o no encontrado"

                            detail.contains(
                                "contraseña",
                                ignoreCase = true
                            ) ||
                                    detail.contains(
                                        "password",
                                        ignoreCase = true
                                    ) -> "Contraseña incorrecta"

                            detail.isNotEmpty() -> detail

                            response.code() == 401 ->
                                "Correo o contraseña incorrectos"

                            else ->
                                "Error ${response.code()}: No se pudo iniciar sesión"
                        }

                    } catch (e: Exception) {

                        Log.e(
                            "LOGIN_ERROR",
                            "No se pudo interpretar la respuesta del backend.",
                            e
                        )

                        if (response.code() == 401) {
                            "Correo o contraseña incorrectos"
                        } else {
                            "Error ${response.code()}: No se pudo iniciar sesión"
                        }
                    }

                    Toast.makeText(
                        this@LoginActivity,
                        msg,
                        Toast.LENGTH_LONG
                    ).show()
                }

            } catch (e: Exception) {

                mostrarCargando(false)

                Log.e(
                    "LOGIN_ERROR",
                    "Excepción durante el inicio de sesión: " +
                            (e.message ?: "Error desconocido"),
                    e
                )

                val errorMsg = if (
                    e is java.net.SocketTimeoutException ||
                    e.message?.contains(
                        "timeout",
                        ignoreCase = true
                    ) == true
                ) {
                    "El servidor está tardando en responder. " +
                            "Comprueba la conexión e inténtalo nuevamente."
                } else {
                    "Error de red: " +
                            (e.localizedMessage
                                ?: "No se pudo conectar al servidor")
                }

                Toast.makeText(
                    this@LoginActivity,
                    errorMsg,
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    // =========================================================
    // ESTADO DEL BOTÓN
    // =========================================================

    private fun mostrarCargando(cargando: Boolean) {

        binding.btnIngresar.isEnabled = !cargando

        if (cargando) {

            binding.btnIngresar.text = "Ingresando..."
            binding.btnIngresar.icon = null

            binding.txtCorreo.isEnabled = false
            binding.txtPassword.isEnabled = false
            binding.txtOlvide.isEnabled = false
            binding.txtAdministrador.isEnabled = false
            binding.layoutAdminContacto.isEnabled = false

        } else {

            binding.btnIngresar.text = "Ingresar"

            binding.btnIngresar.icon =
                androidx.core.content.ContextCompat.getDrawable(
                    this,
                    R.drawable.ic_arrow_forward
                )

            binding.btnIngresar.iconTint =
                android.content.res.ColorStateList.valueOf(
                    android.graphics.Color.parseColor("#181B1F")
                )

            binding.txtCorreo.isEnabled = true
            binding.txtPassword.isEnabled = true
            binding.txtOlvide.isEnabled = true
            binding.txtAdministrador.isEnabled = true
            binding.layoutAdminContacto.isEnabled = true
        }
    }

    // =========================================================
    // OCULTAR TECLADO
    // =========================================================

    private fun ocultarTeclado() {

        val imm = getSystemService(
            Context.INPUT_METHOD_SERVICE
        ) as InputMethodManager

        imm.hideSoftInputFromWindow(
            binding.root.windowToken,
            0
        )
    }
}
