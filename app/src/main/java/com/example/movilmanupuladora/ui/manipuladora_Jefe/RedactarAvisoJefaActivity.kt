package com.example.movilmanupuladora.ui.manipuladora_Jefe

import android.content.ContentResolver
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.movilmanupuladora.data.api.RetrofitClient
import com.example.movilmanupuladora.databinding.ActivityRedactarAvisoJefaBinding
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class RedactarAvisoJefaActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRedactarAvisoJefaBinding
    private var imagenSeleccionada: Uri? = null
    private var enviando = false

    private val selectorImagen =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri != null) {
                imagenSeleccionada = uri
                binding.imgVistaPreviaAviso.setImageURI(uri)
                binding.imgVistaPreviaAviso.visibility = View.VISIBLE
                binding.txtEstadoImagen.text = "Imagen seleccionada."
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityRedactarAvisoJefaBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { vista, insets ->
            val barras = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            vista.setPadding(barras.left, barras.top, barras.right, barras.bottom)
            insets
        }

        binding.btnVolver.setOnClickListener {
            finish()
        }

        val productoNombre = intent.getStringExtra("producto_nombre")
        if (!productoNombre.isNullOrBlank()) {
            binding.edtTituloAviso.setText("Alerta de insumo: $productoNombre")
        }

        binding.btnSeleccionarFoto.setOnClickListener {
            selectorImagen.launch("image/*")
        }

        binding.btnQuitarFoto.setOnClickListener {
            imagenSeleccionada = null
            binding.imgVistaPreviaAviso.setImageDrawable(null)
            binding.imgVistaPreviaAviso.visibility = View.GONE
            binding.txtEstadoImagen.text = "No has seleccionado ninguna imagen."
        }

        binding.btnEnviarAviso.setOnClickListener {
            enviarNotificacion()
        }
    }

    private fun enviarNotificacion() {
        if (enviando) return

        val titulo = binding.edtTituloAviso.text.toString().trim()
        val mensaje = binding.edtMensajeAviso.text.toString().trim()

        if (titulo.isEmpty()) {
            binding.edtTituloAviso.error = "Escribe el título del aviso"
            binding.edtTituloAviso.requestFocus()
            return
        }

        if (mensaje.isEmpty()) {
            binding.edtMensajeAviso.error = "Escribe la descripción del aviso"
            binding.edtMensajeAviso.requestFocus()
            return
        }

        lifecycleScope.launch {
            enviando = true
            binding.btnEnviarAviso.isEnabled = false
            binding.btnEnviarAviso.text = "Enviando..."

            try {
                val tipoTexto = "text/plain".toMediaTypeOrNull()

                val tituloBody: RequestBody =
                    titulo.toRequestBody(tipoTexto)

                val mensajeBody: RequestBody =
                    mensaje.toRequestBody(tipoTexto)

                val parteImagen = imagenSeleccionada?.let { uri ->
                    crearParteImagen(contentResolver, uri)
                }

                val respuesta =
                    RetrofitClient.apiService.enviarNotificacion(
                        titulo = tituloBody,
                        mensaje = mensajeBody,
                        imagen = parteImagen
                    )

                if (respuesta.isSuccessful) {
                    Toast.makeText(
                        this@RedactarAvisoJefaActivity,
                        "Aviso enviado al administrador correctamente.",
                        Toast.LENGTH_LONG
                    ).show()
                    finish()
                } else {
                    val detalle = respuesta.errorBody()
                        ?.string()
                        ?.take(300)
                        .orEmpty()

                    Toast.makeText(
                        this@RedactarAvisoJefaActivity,
                        "No se pudo enviar el aviso. Código: ${respuesta.code()}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            } catch (e: Exception) {
                Toast.makeText(
                    this@RedactarAvisoJefaActivity,
                    "Error al enviar aviso: ${e.message ?: "revisa tu conexión"}",
                    Toast.LENGTH_LONG
                ).show()
            } finally {
                enviando = false
                binding.btnEnviarAviso.isEnabled = true
                binding.btnEnviarAviso.text = "Enviar aviso al Administrador"
            }
        }
    }

    private fun crearParteImagen(
        resolver: ContentResolver,
        uri: Uri
    ): MultipartBody.Part {
        val tipoMime = resolver.getType(uri) ?: "image/jpeg"

        require(tipoMime.startsWith("image/")) {
            "El archivo seleccionado no es una imagen."
        }

        val bytes = resolver.openInputStream(uri)?.use { input ->
            input.readBytes()
        } ?: throw IllegalStateException(
            "No se pudo leer la imagen seleccionada."
        )

        require(bytes.size <= 5 * 1024 * 1024) {
            "La imagen no puede superar los 5 MB."
        }

        val nombreArchivo = resolver.query(
            uri,
            arrayOf(OpenableColumns.DISPLAY_NAME),
            null,
            null,
            null
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                val columna = cursor.getColumnIndex(
                    OpenableColumns.DISPLAY_NAME
                )

                if (columna >= 0) cursor.getString(columna) else null
            } else {
                null
            }
        } ?: "fotografia_${
            SimpleDateFormat(
                "yyyyMMdd_HHmmss",
                Locale.getDefault()
            ).format(Date())
        }.jpg"

        val cuerpo = bytes.toRequestBody(
            tipoMime.toMediaTypeOrNull()
        )

        return MultipartBody.Part.createFormData(
            "imagen",
            nombreArchivo,
            cuerpo
        )
    }
}