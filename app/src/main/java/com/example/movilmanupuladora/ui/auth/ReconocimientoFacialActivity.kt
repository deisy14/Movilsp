package com.example.movilmanupuladora.ui.auth

import android.Manifest
import android.animation.ValueAnimator
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Matrix
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import com.example.movilmanupuladora.R
import com.example.movilmanupuladora.data.api.RetrofitClient
import com.example.movilmanupuladora.data.repository.ReconocimientoFacialRepository
import com.example.movilmanupuladora.databinding.ActivityReconocimientoFacialBinding
import com.example.movilmanupuladora.ui.manipuladora.TurnoActivity
import com.example.movilmanupuladora.ui.manipuladora_Jefe.InicioJefaActivity
import com.example.movilmanupuladora.utils.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class ReconocimientoFacialActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_MODO = "extra_modo"
        const val MODO_LOGIN = "MODO_LOGIN"
        const val MODO_REGISTRO = "MODO_REGISTRO"
        const val EXTRA_CORREO = "extra_correo"
        private const val TAG = "FacialRecognition"
    }

    private lateinit var binding: ActivityReconocimientoFacialBinding
    private lateinit var cameraExecutor: ExecutorService
    private var imageCapture: ImageCapture? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private var lensFacing = CameraSelector.LENS_FACING_FRONT

    private var laserAnimator: ValueAnimator? = null
    private var isProcessing = false

    private val sessionManager by lazy { SessionManager(this) }
    private val facialRepository by lazy { ReconocimientoFacialRepository(RetrofitClient.apiService) }

    private var modoActual: String = MODO_LOGIN
    private var correoRegistro: String? = null

    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            iniciarCamara()
        } else {
            Toast.makeText(
                this,
                "Se requiere permiso de cámara para el reconocimiento facial.",
                Toast.LENGTH_LONG
            ).show()
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Configuración Edge-to-edge con barra oscura para diseño inmersivo
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val insetsController = WindowCompat.getInsetsController(window, window.decorView)
        insetsController.isAppearanceLightStatusBars = false
        insetsController.isAppearanceLightNavigationBars = false

        binding = ActivityReconocimientoFacialBinding.inflate(layoutInflater)
        setContentView(binding.root)

        modoActual = intent.getStringExtra(EXTRA_MODO) ?: MODO_LOGIN
        correoRegistro = intent.getStringExtra(EXTRA_CORREO)?.ifBlank { null } ?: sessionManager.getUserEmail()

        configurarTextosPorModo()

        cameraExecutor = Executors.newSingleThreadExecutor()

        binding.btnVolverFacial.setOnClickListener {
            finish()
        }

        binding.btnCambiarCamara.setOnClickListener {
            alternarCamara()
        }

        binding.btnCapturarFacial.setOnClickListener {
            if (!isProcessing) {
                capturarYProcesar()
            }
        }

        verificarPermisosYComenzar()
    }

    private fun configurarTextosPorModo() {
        if (modoActual == MODO_REGISTRO) {
            binding.txtTituloFacial.text = "Registro Biométrico"
            binding.txtSubtituloFacial.text = "Vincula tu rostro a SIRAE"
            binding.txtMensajeEstado.text = "Centra tu rostro dentro del óvalo"
        } else {
            binding.txtTituloFacial.text = "Reconocimiento Facial"
            binding.txtSubtituloFacial.text = "Escaneo Biométrico SIRAE"
            binding.txtMensajeEstado.text = "Ubica tu rostro dentro del óvalo"
        }
    }

    private fun verificarPermisosYComenzar() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED
        ) {
            iniciarCamara()
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun iniciarCamara() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()
            bindCameraUseCases()
            iniciarAnimacionLaser()
        }, ContextCompat.getMainExecutor(this))
    }

    private fun bindCameraUseCases() {
        val provider = cameraProvider ?: return

        val preview = Preview.Builder().build().also {
            it.setSurfaceProvider(binding.previewViewFacial.surfaceProvider)
        }

        imageCapture = ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .build()

        val cameraSelector = CameraSelector.Builder()
            .requireLensFacing(lensFacing)
            .build()

        try {
            provider.unbindAll()
            provider.bindToLifecycle(
                this,
                cameraSelector,
                preview,
                imageCapture
            )
        } catch (exc: Exception) {
            Log.e(TAG, "Fallo al vincular la cámara", exc)
            Toast.makeText(this, "No se pudo iniciar la cámara: ${exc.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun alternarCamara() {
        lensFacing = if (lensFacing == CameraSelector.LENS_FACING_FRONT) {
            CameraSelector.LENS_FACING_BACK
        } else {
            CameraSelector.LENS_FACING_FRONT
        }
        bindCameraUseCases()
    }

    private fun iniciarAnimacionLaser() {
        binding.viewLaserScan.post {
            val totalDistance = (binding.viewOvalGuide.height - 20).toFloat().coerceAtLeast(180f)
            laserAnimator?.cancel()
            binding.viewLaserScan.visibility = View.VISIBLE

            laserAnimator = ValueAnimator.ofFloat(0f, totalDistance).apply {
                duration = 1500
                repeatCount = ValueAnimator.INFINITE
                repeatMode = ValueAnimator.REVERSE
                interpolator = AccelerateDecelerateInterpolator()
                addUpdateListener { anim ->
                    binding.viewLaserScan.translationY = anim.animatedValue as Float
                }
            }
            laserAnimator?.start()
        }
    }

    private fun detenerAnimacionLaser() {
        laserAnimator?.cancel()
        binding.viewLaserScan.visibility = View.INVISIBLE
    }

    private fun capturarYProcesar() {
        val capture = imageCapture ?: return

        isProcessing = true
        binding.btnCapturarFacial.isEnabled = false
        binding.btnCapturarFacial.alpha = 0.5f
        binding.progressFacial.visibility = View.VISIBLE
        binding.txtMensajeEstado.text = "Analizando biometría facial..."

        capture.takePicture(
            cameraExecutor,
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    procesarImagenCapturada(image)
                }

                override fun onError(exception: ImageCaptureException) {
                    Log.e(TAG, "Error en captura: ${exception.message}", exception)
                    runOnUiThread {
                        restablecerEstadoCaptura("Error al capturar imagen. Intenta de nuevo.")
                    }
                }
            }
        )
    }

    private fun procesarImagenCapturada(image: ImageProxy) {
        val rotationDegrees = image.imageInfo.rotationDegrees
        val rawBitmap = try {
            image.toBitmap()
        } catch (e: Exception) {
            Log.e(TAG, "Error convirtiendo imagen a bitmap", e)
            null
        } finally {
            image.close()
        }

        if (rawBitmap == null) {
            runOnUiThread {
                restablecerEstadoCaptura("No se pudo procesar la imagen de la cámara.")
            }
            return
        }

        // Rotar y voltear si es cámara frontal para que quede natural
        val matrix = Matrix().apply {
            postRotate(rotationDegrees.toFloat())
            if (lensFacing == CameraSelector.LENS_FACING_FRONT) {
                // Si la imagen frontal requiere mirroring visual
                postScale(-1f, 1f, rawBitmap.width / 2f, rawBitmap.height / 2f)
            }
        }

        val orientedBitmap = Bitmap.createBitmap(
            rawBitmap, 0, 0, rawBitmap.width, rawBitmap.height, matrix, true
        )

        // Redimensionar para transferir rápidamente (máx 720px)
        val resizedBitmap = redimensionarBitmap(orientedBitmap, 720)

        // Convertir a Base64 JPEG con compresión 85%
        val base64Image = bitmapABase64(resizedBitmap, 85)

        lifecycleScope.launch(Dispatchers.IO) {
            if (modoActual == MODO_REGISTRO) {
                ejecutarRegistroFacial(base64Image, resizedBitmap)
            } else {
                ejecutarLoginFacial(base64Image, resizedBitmap)
            }
        }
    }

    private suspend fun ejecutarLoginFacial(base64Image: String, displayBitmap: Bitmap) {
        try {
            val response = facialRepository.verificarRostro(base64Image)

            if (response.isSuccessful && response.body() != null) {
                val data = response.body()!!
                val token = data.access

                if (token.isNullOrBlank()) {
                    withContext(Dispatchers.Main) {
                        restablecerEstadoCaptura("Error: no se recibió token de sesión.")
                    }
                    return
                }

                val usuario = data.usuario
                val email = usuario?.correo?.trim()?.lowercase() ?: ""
                val rolTexto = usuario?.rol ?: ""

                // Bloqueo de roles administrativos en la app móvil
                if (LoginActivity.esRolAdministrativo(rolTexto, email)) {
                    sessionManager.clearSession()
                    RetrofitClient.authToken = null
                    withContext(Dispatchers.Main) {
                        detenerAnimacionLaser()
                        androidx.appcompat.app.AlertDialog.Builder(this@ReconocimientoFacialActivity)
                            .setTitle("Acceso Restringido")
                            .setMessage("Hola ${usuario?.nombre ?: ""}, tu cuenta tiene rol administrativo ($rolTexto).\n\nEsta aplicación móvil es de uso exclusivo para Manipuladoras y Jefas de cocina.\n\nPor favor ingresa desde la plataforma Web del PAE.")
                            .setPositiveButton("Entendido") { _, _ -> finish() }
                            .setCancelable(false)
                            .show()
                    }
                    return
                }

                // Limpiar sesión previa y guardar nuevo token
                sessionManager.clearSession()
                RetrofitClient.authToken = token
                sessionManager.saveAuthToken(token)

                val esJefa = LoginActivity.esUsuarioJefa(
                    rol = rolTexto,
                    email = email
                )

                val nombreCompleto = usuario?.nombreCompleto?.ifBlank {
                    usuario.nombre.ifBlank { email }
                } ?: "Usuario"

                val rolGuardar = when {
                    esJefa -> "Jefa de Manipuladoras"
                    rolTexto.isNotBlank() -> rolTexto
                    else -> "Manipuladora de alimentos"
                }

                sessionManager.saveUserData(
                    name = nombreCompleto,
                    role = rolGuardar,
                    email = email,
                    idUsuario = usuario?.idUsuario
                )

                withContext(Dispatchers.Main) {
                    mostrarPantallaExito(
                        bitmap = displayBitmap,
                        nombre = nombreCompleto,
                        rol = rolGuardar.uppercase(),
                        esJefa = esJefa
                    )
                }

            } else {
                val errorMsg = extraerMensajeError(
                    response.code(),
                    response.errorBody()?.string()
                )
                withContext(Dispatchers.Main) {
                    restablecerEstadoCaptura(errorMsg)
                }
            }

        } catch (e: Exception) {
            Log.e(TAG, "Excepción en login facial", e)
            val msg = if (e is java.io.IOException) {
                "Sin conexión con el servidor. Revisa tu internet."
            } else {
                "Ocurrió un error al verificar: ${e.localizedMessage}"
            }
            withContext(Dispatchers.Main) {
                restablecerEstadoCaptura(msg)
            }
        }
    }

    private suspend fun ejecutarRegistroFacial(base64Image: String, displayBitmap: Bitmap) {
        try {
            val correoFinal = correoRegistro?.ifBlank { null } ?: sessionManager.getUserEmail()
            val response = facialRepository.registrarRostro(base64Image, correoFinal)

            if (response.isSuccessful && response.body() != null) {
                val data = response.body()!!
                val nombreUsuario = sessionManager.getUserName() ?: data.usuario ?: "Usuario"

                withContext(Dispatchers.Main) {
                    mostrarPantallaExitoRegistro(
                        bitmap = displayBitmap,
                        mensaje = data.msg ?: "¡Rostro registrado exitosamente!",
                        usuario = nombreUsuario
                    )
                }
            } else {
                val errorMsg = extraerMensajeError(
                    response.code(),
                    response.errorBody()?.string()
                )
                withContext(Dispatchers.Main) {
                    restablecerEstadoCaptura(errorMsg)
                    if (errorMsg.contains("expirado", ignoreCase = true) || errorMsg.contains("sesión", ignoreCase = true)) {
                        androidx.appcompat.app.AlertDialog.Builder(this@ReconocimientoFacialActivity)
                            .setTitle("Sesión Expirada")
                            .setMessage("Tu sesión previa ha caducado. Por favor inicia sesión nuevamente con tu contraseña para renovar tu acceso.")
                            .setPositiveButton("Iniciar Sesión") { _, _ ->
                                sessionManager.clearSession()
                                RetrofitClient.authToken = null
                                val intent = Intent(this@ReconocimientoFacialActivity, LoginActivity::class.java).apply {
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                }
                                startActivity(intent)
                                finish()
                            }
                            .setNegativeButton("Cancelar", null)
                            .show()
                    }
                }
            }

        } catch (e: Exception) {
            Log.e(TAG, "Excepción en registro facial", e)
            val msg = if (e is java.io.IOException) {
                "Sin conexión con el servidor. Revisa tu internet."
            } else {
                "Error al registrar biometría: ${e.localizedMessage}"
            }
            withContext(Dispatchers.Main) {
                restablecerEstadoCaptura(msg)
            }
        }
    }

    private fun extraerMensajeError(httpCode: Int, errorBody: String?): String {
        var detalle = ""
        if (!errorBody.isNullOrBlank()) {
            try {
                val json = JSONObject(errorBody)
                detalle = json.optString("detail", json.optString("error", json.optString("msg", ""))).trim()
            } catch (_: Exception) {}
        }

        if (detalle.isNotBlank()) {
            if (detalle.contains("token", ignoreCase = true)) {
                return "Tu sesión ha expirado. Por favor cierra sesión e inicia de nuevo para renovar tu acceso."
            }
            return detalle
        }

        return when (httpCode) {
            400 -> "No se detectó un rostro claro. Enfoca bien dentro del óvalo."
            401 -> "Rostro no reconocido en el sistema. Debes registrarlo previamente."
            403 -> "Esta cuenta de usuario se encuentra inactiva."
            else -> "No se pudo verificar el rostro (Error $httpCode). Intenta nuevamente."
        }
    }

    private fun restablecerEstadoCaptura(mensaje: String) {
        isProcessing = false
        binding.progressFacial.visibility = View.GONE
        binding.btnCapturarFacial.isEnabled = true
        binding.btnCapturarFacial.alpha = 1.0f
        binding.txtMensajeEstado.text = mensaje

        // Feedback con Toast si es un error importante
        Toast.makeText(this, mensaje, Toast.LENGTH_SHORT).show()
    }

    private fun mostrarPantallaExito(
        bitmap: Bitmap,
        nombre: String,
        rol: String,
        esJefa: Boolean
    ) {
        detenerAnimacionLaser()

        binding.imgFotoVerificada.setImageBitmap(bitmap)
        binding.txtNombreVerificado.text = "¡Bienvenida, $nombre!"
        binding.txtRolVerificado.text = "ROL: $rol"
        binding.layoutExitoFacial.visibility = View.VISIBLE

        lifecycleScope.launch {
            delay(1300)

            val destino = if (esJefa) {
                InicioJefaActivity::class.java
            } else {
                TurnoActivity::class.java
            }

            val intent = Intent(this@ReconocimientoFacialActivity, destino).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            startActivity(intent)
            finish()
        }
    }

    private fun mostrarPantallaExitoRegistro(
        bitmap: Bitmap,
        mensaje: String,
        usuario: String
    ) {
        detenerAnimacionLaser()

        binding.imgFotoVerificada.setImageBitmap(bitmap)
        binding.txtNombreVerificado.text = usuario
        binding.txtRolVerificado.text = "BIOMETRÍA REGISTRADA"
        binding.layoutExitoFacial.visibility = View.VISIBLE

        lifecycleScope.launch {
            delay(1400)
            Toast.makeText(this@ReconocimientoFacialActivity, mensaje, Toast.LENGTH_LONG).show()
            setResult(RESULT_OK)
            finish()
        }
    }

    private fun redimensionarBitmap(original: Bitmap, maxDim: Int): Bitmap {
        val width = original.width
        val height = original.height
        if (width <= maxDim && height <= maxDim) return original

        val ratio = width.toFloat() / height.toFloat()
        val targetWidth: Int
        val targetHeight: Int

        if (width > height) {
            targetWidth = maxDim
            targetHeight = (maxDim / ratio).toInt()
        } else {
            targetHeight = maxDim
            targetWidth = (maxDim * ratio).toInt()
        }

        return Bitmap.createScaledBitmap(original, targetWidth, targetHeight, true)
    }

    private fun bitmapABase64(bitmap: Bitmap, calidad: Int): String {
        val byteArrayOutputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, calidad, byteArrayOutputStream)
        val byteArray = byteArrayOutputStream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.NO_WRAP)
    }

    override fun onDestroy() {
        super.onDestroy()
        laserAnimator?.cancel()
        if (::cameraExecutor.isInitialized) {
            cameraExecutor.shutdown()
        }
    }
}
