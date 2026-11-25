
package com.example.movilmanupuladora.ui.auth

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import com.example.movilmanupuladora.R
import com.example.movilmanupuladora.data.api.RetrofitClient
import com.example.movilmanupuladora.data.repository.UsuarioRepository
import com.example.movilmanupuladora.databinding.ActivityLoginBinding
import com.example.movilmanupuladora.ui.manipuladora.TurnoActivity
import com.example.movilmanupuladora.ui.manipuladora_Jefe.InicioJefaActivity
import com.example.movilmanupuladora.utils.SessionManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONObject

class LoginActivity : AppCompatActivity() {

    companion object {
        fun normalizarTexto(texto: String?): String {
            if (texto.isNullOrBlank()) return ""
            val nfd = java.text.Normalizer.normalize(texto, java.text.Normalizer.Form.NFD)
            return nfd.replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "")
                .lowercase()
                .trim()
        }

        fun esUsuarioJefa(
            rol: String?,
            email: String?,
            idRol: Int? = null,
            nombreRol: String? = null
        ): Boolean {
            val r = normalizarTexto(rol)
            val nr = normalizarTexto(nombreRol)
            val e = normalizarTexto(email)

            // 1. Chequeo por ID numérico asignado a Jefa de Manipuladoras en backend
            if (idRol == 4 || idRol == 26 || idRol == 27 || idRol == 28) return true

            // 2. Si el rol llegó como string numérico ("4")
            val idParsed = r.toIntOrNull() ?: nr.toIntOrNull()
            if (idParsed == 4 || idParsed == 26 || idParsed == 27 || idParsed == 28) return true

            // 3. Chequeo dinámico por nombre o descripción de rol (sin datos quemados)
            val rolTexto = "$r $nr".trim()
            if (rolTexto.contains("jefa") || rolTexto.contains("jefe") || rolTexto.contains("coordinad")) {
                return true
            }

            // 4. Si el correo institucional indica explícitamente jefa/jefe
            if (e.contains("jefe") || e.contains("jefa")) {
                return true
            }

            return false
        }

        fun esRolAdministrativo(
            rol: String?,
            email: String? = null,
            idRol: Int? = null,
            nombreRol: String? = null
        ): Boolean {
            val r = normalizarTexto(rol)
            val nr = normalizarTexto(nombreRol)
            val e = normalizarTexto(email)

            if (idRol in listOf(1, 2, 3)) return true

            val idParsed = r.toIntOrNull() ?: nr.toIntOrNull()
            if (idParsed in listOf(1, 2, 3)) return true

            val rolTexto = "$r $nr".trim()
            if (rolTexto.contains("super") ||
                rolTexto.contains("admin") ||
                rolTexto.contains("supervisor")
            ) {
                return true
            }

            if (e.contains("superadmin") || e.contains("admin@") || e.contains("supervisor@")) {
                return true
            }

            return false
        }
    }

    private lateinit var binding: ActivityLoginBinding

    private val usuarioRepository =
        UsuarioRepository(RetrofitClient.apiService)

    private lateinit var sessionManager: SessionManager
    private var jobAnimacionCargando: Job? = null

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

        // 1. Permitir que el fondo se dibuje detrás de las barras (Edge-to-Edge moderno)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        // 2. Extender el fondo alrededor de la cámara/notch
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            window.attributes.layoutInDisplayCutoutMode =
                android.view.WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }

        // 3. Configurar el color de los iconos de la barra para que sean legibles sobre la foto
        val insetsController = WindowCompat.getInsetsController(window, window.decorView)
        // true = iconos oscuros (para que resalten sobre los gorros blancos y cocina clara)
        insetsController.isAppearanceLightStatusBars = true
        insetsController.isAppearanceLightNavigationBars = false

        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Precalentar silenciosamente el backend de Render mientras el usuario escribe sus credenciales
        RetrofitClient.despertarBackend()

        sessionManager = SessionManager(this)

        // Si ya hay una sesión iniciada, entrar directamente sin pedir login de nuevo.
        // La sesión se mantiene siempre activa hasta que el usuario elija "Cerrar sesión".
        if (sessionManager.isLoggedIn()) {
            val token = sessionManager.fetchAuthToken()
            RetrofitClient.authToken = token

            val rol = sessionManager.getUserRole()
            val email = sessionManager.getUserEmail()

            if (esRolAdministrativo(rol, email)) {
                sessionManager.clearSession()
                RetrofitClient.authToken = null
                mostrarDialogoAccesoWeb(rol ?: "Administrativo")
            } else {
                val esJefa = esUsuarioJefa(rol = rol, email = email)
                val destino = if (esJefa) {
                    InicioJefaActivity::class.java
                } else {
                    TurnoActivity::class.java
                }
                startActivity(Intent(this, destino))
                finish()
                return
            }
        }

        // Hacer scroll automático al enfocar campos de texto cuando sale el teclado
        binding.txtCorreo.onFocusChangeListener = View.OnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                binding.scrollLogin.postDelayed({
                    binding.scrollLogin.smoothScrollTo(0, binding.txtCorreo.top)
                }, 150)
            }
        }

        binding.txtPassword.onFocusChangeListener = View.OnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                binding.scrollLogin.postDelayed({
                    binding.scrollLogin.smoothScrollTo(0, binding.txtPassword.top)
                }, 150)
            }
        }

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

        // Botón de Reconocimiento Facial Biométrico
        binding.btnReconocimientoFacial.setOnClickListener {
            val intent = Intent(this, ReconocimientoFacialActivity::class.java).apply {
                putExtra(ReconocimientoFacialActivity.EXTRA_MODO, ReconocimientoFacialActivity.MODO_LOGIN)
            }
            startActivity(intent)
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

                    val userInfo = loginRes.usuario
                    val rolTexto = normalizarTexto(userInfo?.rolEfectivo)
                    val email = (userInfo?.correo?.takeIf { it.isNotBlank() } ?: correo)
                        .trim()
                        .lowercase()

                    // Los roles administrativos ingresan exclusivamente desde la plataforma Web.
                    val esAdmin = esRolAdministrativo(
                        rol = userInfo?.rolEfectivo ?: userInfo?.rol,
                        email = email,
                        idRol = userInfo?.idRol,
                        nombreRol = userInfo?.nombreRol
                    )

                    if (esAdmin) {
                        mostrarCargando(false)
                        sessionManager.clearSession()
                        RetrofitClient.authToken = null
                        mostrarDialogoAccesoWeb(userInfo?.nombreRol ?: userInfo?.rolEfectivo ?: userInfo?.rol ?: "Administrativo")
                        return@launch
                    }

                    // Limpiar datos de la sesión anterior para evitar mezclar usuarios
                    sessionManager.clearSession()

                    // Guardar el token de autenticación.
                    RetrofitClient.authToken = token
                    sessionManager.saveAuthToken(token)

                    // Identificar dinámicamente si es jefa (sin datos quemados)
                    val esJefa = esUsuarioJefa(
                        rol = userInfo?.rol,
                        email = email,
                        idRol = userInfo?.idRol,
                        nombreRol = userInfo?.nombreRol
                    )

                    // Guardar los datos del usuario y su sede asociada
                    userInfo?.let { usuario ->
                        val nombreCompleto = usuario.nombreCompleto.ifBlank {
                            if (usuario.nombre.isNotBlank()) usuario.nombre else email
                        }

                        val sedeDisplay = usuario.sedeEfectiva
                            ?: usuario.sedeNombre
                            ?: usuario.institucionNombre

                        val rolGuardar = when {
                            !usuario.nombreRol.isNullOrBlank() -> usuario.nombreRol
                            !usuario.rol.isNullOrBlank() && usuario.rol.toIntOrNull() == null -> usuario.rol
                            esJefa -> "Jefa de Manipuladoras"
                            else -> "Manipuladora de alimentos"
                        }

                        sessionManager.saveUserData(
                            name = nombreCompleto,
                            role = rolGuardar,
                            email = email,
                            idUsuario = usuario.idUsuario,
                            idSede = usuario.idSede,
                            sedeNombre = usuario.sedeNombre ?: sedeDisplay,
                            idInstitucion = usuario.idInstitucion,
                            institucionNombre = usuario.institucionNombre
                        )

                        Log.d(
                            "LOGIN_AUTH",
                            "Usuario: $nombreCompleto | Rol: $rolGuardar | Sede: $sedeDisplay (idSede=${usuario.idSede})"
                        )
                    }

                    // Determinar la pantalla de destino.
                    val destino = if (esJefa) {
                        InicioJefaActivity::class.java
                    } else {
                        TurnoActivity::class.java
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

                            detail.contains("bloque", ignoreCase = true) ||
                            detail.contains("lock", ignoreCase = true) ||
                            detail.contains("intentos", ignoreCase = true) ->
                                "Cuenta bloqueada temporalmente por múltiples intentos fallidos. Por favor espera unos minutos antes de reintentar."

                            response.code() == 429 ->
                                "Demasiados intentos de inicio de sesión. Por seguridad, espera unos minutos antes de volver a intentar."

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

            binding.btnIngresar.icon = null

            binding.txtCorreo.isEnabled = false
            binding.txtPassword.isEnabled = false
            binding.txtOlvide.isEnabled = false
            binding.txtAdministrador.isEnabled = false
            binding.layoutAdminContacto.isEnabled = false

            // Animación de puntos en el botón: "Ingresando." -> "Ingresando.." -> "Ingresando..."
            jobAnimacionCargando?.cancel()
            jobAnimacionCargando = lifecycleScope.launch {
                val estados = listOf("Ingresando.", "Ingresando..", "Ingresando...")
                var index = 0
                while (isActive) {
                    binding.btnIngresar.text = estados[index]
                    index = (index + 1) % estados.size
                    delay(600)
                }
            }

        } else {

            jobAnimacionCargando?.cancel()
            jobAnimacionCargando = null

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

    private fun mostrarDialogoAccesoWeb(rol: String) {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Acceso Restringido")
            .setMessage("Tu cuenta tiene rol administrativo ($rol).\n\nEsta aplicación móvil es de uso exclusivo para el personal de cocina (Manipuladoras y Jefas de Manipuladoras).\n\nPor favor ingresa desde la plataforma Web del PAE.")
            .setPositiveButton("Entendido", null)
            .show()
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

    override fun onResume() {
        super.onResume()
        val insetsController = WindowCompat.getInsetsController(window, window.decorView)
        insetsController.isAppearanceLightStatusBars = true
    }

    override fun onDestroy() {
        super.onDestroy()
        jobAnimacionCargando?.cancel()
        jobAnimacionCargando = null
    }
}
