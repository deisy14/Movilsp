
package com.example.movilmanupuladora.ui.manipuladora

import android.content.ContentResolver
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.movilmanupuladora.data.api.RetrofitClient
import com.example.movilmanupuladora.data.model.Notificacion
import com.example.movilmanupuladora.databinding.ActivityAvisosBinding
import com.example.movilmanupuladora.utils.NavigationHelper
import com.google.gson.JsonPrimitive
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AvisosActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAvisosBinding
    private lateinit var campoTitulo: EditText
    private lateinit var campoMensaje: EditText
    private lateinit var vistaPreviaImagen: ImageView
    private lateinit var textoImagen: TextView
    private lateinit var botonEnviar: Button

    private var imagenSeleccionada: Uri? = null
    private var enviando = false

    private val selectorImagen =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri != null) {
                imagenSeleccionada = uri
                vistaPreviaImagen.setImageURI(uri)
                vistaPreviaImagen.visibility = View.VISIBLE
                textoImagen.text = "Imagen seleccionada."
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityAvisosBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { vista, insets ->
            val barras = insets.getInsets(WindowInsetsCompat.Type.systemBars())

            vista.setPadding(
                barras.left,
                barras.top,
                barras.right,
                0
            )

            insets
        }

        configurarBarraNavegacion()
        cargarNotificaciones()
    }

    private fun dp(valor: Int): Int =
        (valor * resources.displayMetrics.density).toInt()

    private fun crearTexto(
        texto: String,
        tamano: Float = 14f,
        color: Int = Color.DKGRAY,
        negrita: Boolean = false
    ): TextView {
        return TextView(this).apply {
            text = texto
            textSize = tamano
            setTextColor(color)

            if (negrita) {
                setTypeface(typeface, Typeface.BOLD)
            }
        }
    }

    private fun crearCampo(
        pista: String,
        multilinea: Boolean = false
    ): EditText {
        return EditText(this).apply {
            hint = pista
            textSize = 14f
            setTextColor(Color.rgb(35, 45, 35))
            setHintTextColor(Color.GRAY)
            setPadding(dp(12), dp(10), dp(12), dp(10))

            background = android.graphics.drawable.GradientDrawable().apply {
                setColor(Color.WHITE)
                setStroke(dp(1), Color.rgb(215, 220, 215))
                cornerRadius = dp(10).toFloat()
            }

            if (multilinea) {
                inputType =
                    android.text.InputType.TYPE_CLASS_TEXT or
                            android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE

                minLines = 4
                maxLines = 6
                gravity = Gravity.TOP or Gravity.START
            } else {
                inputType =
                    android.text.InputType.TYPE_CLASS_TEXT or
                            android.text.InputType.TYPE_TEXT_FLAG_CAP_SENTENCES

                maxLines = 1
            }

            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dp(12)
            }
        }
    }

    private fun crearBoton(
        texto: String,
        color: Int
    ): Button {
        return Button(this).apply {
            text = texto
            setTextColor(Color.WHITE)
            backgroundTintList =
                android.content.res.ColorStateList.valueOf(color)
            isAllCaps = false
            textSize = 14f
        }
    }

    private fun configurarFormulario(contenedor: LinearLayout) {
        val tarjeta = CardView(this).apply {
            radius = dp(16).toFloat()
            cardElevation = dp(3).toFloat()
            setCardBackgroundColor(Color.WHITE)

            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dp(20)
            }
        }

        val contenido = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(16), dp(16), dp(16))
        }

        contenido.addView(
            crearTexto(
                "Enviar aviso al administrador",
                18f,
                Color.rgb(27, 51, 23),
                true
            )
        )

        val descripcion = crearTexto(
            "Describe una novedad de tu sede. Puedes adjuntar una fotografía.",
            13f
        )

        descripcion.setPadding(0, dp(6), 0, dp(16))
        contenido.addView(descripcion)

        contenido.addView(
            crearTexto("Título del aviso", 13f, Color.DKGRAY, true)
        )

        campoTitulo = crearCampo("Ej. Falta de insumos")
        contenido.addView(campoTitulo)

        contenido.addView(
            crearTexto("Descripción", 13f, Color.DKGRAY, true)
        )

        campoMensaje = crearCampo(
            "Escribe aquí lo que necesitas informar...",
            multilinea = true
        )
        contenido.addView(campoMensaje)

        textoImagen = crearTexto(
            "No has seleccionado ninguna imagen.",
            12f,
            Color.GRAY
        )

        textoImagen.setPadding(0, 0, 0, dp(8))
        contenido.addView(textoImagen)

        vistaPreviaImagen = ImageView(this).apply {
            visibility = View.GONE
            scaleType = ImageView.ScaleType.CENTER_CROP

            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(180)
            ).apply {
                bottomMargin = dp(10)
            }
        }

        contenido.addView(vistaPreviaImagen)

        val botonImagen = crearBoton(
            "Seleccionar fotografía",
            Color.rgb(100, 100, 100)
        )

        botonImagen.setOnClickListener {
            selectorImagen.launch("image/*")
        }

        contenido.addView(botonImagen)

        val botonQuitarImagen = crearBoton(
            "Quitar fotografía",
            Color.rgb(120, 70, 70)
        )

        botonQuitarImagen.setOnClickListener {
            imagenSeleccionada = null
            vistaPreviaImagen.setImageDrawable(null)
            vistaPreviaImagen.visibility = View.GONE
            textoImagen.text = "No has seleccionado ninguna imagen."
        }

        contenido.addView(botonQuitarImagen)

        botonEnviar = crearBoton(
            "Enviar aviso",
            Color.rgb(42, 82, 38)
        )

        botonEnviar.setOnClickListener {
            enviarNotificacion()
        }

        contenido.addView(
            botonEnviar,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dp(12)
            }
        )

        tarjeta.addView(contenido)
        contenedor.addView(tarjeta)
    }

    private fun cargarNotificaciones() {
        lifecycleScope.launch {
            val contenedor = binding.contenedorAvisos

            contenedor.removeAllViews()
            configurarFormulario(contenedor)

            val indicador = crearTexto(
                "Consultando avisos recibidos...",
                13f,
                Color.GRAY
            )

            contenedor.addView(indicador)

            try {
                val respuesta =
                    RetrofitClient.apiService.obtenerNotificaciones()

                contenedor.removeView(indicador)

                if (respuesta.isSuccessful) {
                    val lista = respuesta.body().orEmpty()

                    mostrarNotificaciones(
                        lista,
                        if (lista.isEmpty()) {
                            "Todavía no tienes avisos recibidos."
                        } else {
                            null
                        }
                    )
                } else {
                    mostrarNotificaciones(
                        emptyList(),
                        "No se pudieron consultar los avisos. " +
                                "Código HTTP: ${respuesta.code()}."
                    )
                }
            } catch (e: Exception) {
                contenedor.removeView(indicador)

                mostrarNotificaciones(
                    emptyList(),
                    "No fue posible conectar con el servidor. " +
                            "Revisa tu conexión e inténtalo de nuevo."
                )
            }
        }
    }

    private fun mostrarNotificaciones(
        lista: List<Notificacion>,
        mensajeEstado: String?
    ) {
        val contenedor = binding.contenedorAvisos

        contenedor.removeAllViews()
        configurarFormulario(contenedor)

        if (mensajeEstado != null) {
            val estado = crearTexto(
                mensajeEstado,
                13f,
                Color.rgb(100, 100, 100)
            )

            estado.setPadding(dp(4), dp(4), dp(4), dp(16))
            contenedor.addView(estado)
        }

        if (lista.isEmpty()) return

        val encabezado = crearTexto(
            "Avisos recibidos",
            17f,
            Color.rgb(27, 51, 23),
            true
        )

        encabezado.setPadding(0, dp(4), 0, dp(12))
        contenedor.addView(encabezado)

        for (notificacion in lista) {
            val tarjeta = CardView(this).apply {
                radius = dp(14).toFloat()
                cardElevation = dp(2).toFloat()
                setCardBackgroundColor(Color.WHITE)

                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    bottomMargin = dp(10)
                }
            }

            val contenido = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(14), dp(14), dp(14), dp(14))
            }

            contenido.addView(
                crearTexto(
                    notificacion.titulo ?: "Aviso del sistema",
                    15f,
                    Color.rgb(27, 51, 23),
                    true
                )
            )

            val mensaje = crearTexto(
                notificacion.mensaje.orEmpty(),
                13f
            )

            mensaje.setPadding(0, dp(6), 0, dp(6))
            contenido.addView(mensaje)

            contenido.addView(
                crearTexto(
                    notificacion.fecha ?: "Fecha no disponible",
                    11f,
                    Color.GRAY
                )
            )

            tarjeta.addView(contenido)
            contenedor.addView(tarjeta)
        }
    }

    private fun enviarNotificacion() {
        if (enviando) return

        val titulo = campoTitulo.text.toString().trim()
        val mensaje = campoMensaje.text.toString().trim()

        if (titulo.isEmpty()) {
            campoTitulo.error = "Escribe el título del aviso"
            campoTitulo.requestFocus()
            return
        }

        if (mensaje.isEmpty()) {
            campoMensaje.error = "Escribe la descripción del aviso"
            campoMensaje.requestFocus()
            return
        }

        lifecycleScope.launch {
            enviando = true
            botonEnviar.isEnabled = false
            botonEnviar.text = "Enviando..."

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
                    campoTitulo.text.clear()
                    campoMensaje.text.clear()

                    imagenSeleccionada = null
                    vistaPreviaImagen.setImageDrawable(null)
                    vistaPreviaImagen.visibility = View.GONE
                    textoImagen.text = "No has seleccionado ninguna imagen."

                    val cantidad = respuesta.body()
                        ?.get("cantidad_destinatarios")
                        ?.takeIf { it is JsonPrimitive && it.asJsonPrimitive.isNumber }
                        ?.asInt

                    val confirmacion = if (cantidad != null) {
                        "Aviso enviado correctamente. Destinatarios: $cantidad."
                    } else {
                        "El servidor confirmó el envío del aviso."
                    }

                    Toast.makeText(
                        this@AvisosActivity,
                        confirmacion,
                        Toast.LENGTH_LONG
                    ).show()

                    cargarNotificaciones()
                } else {
                    val detalle = respuesta.errorBody()
                        ?.string()
                        ?.take(300)
                        .orEmpty()

                    Toast.makeText(
                        this@AvisosActivity,
                        "No se pudo enviar. HTTP ${respuesta.code()}. $detalle",
                        Toast.LENGTH_LONG
                    ).show()
                }
            } catch (e: Exception) {
                Toast.makeText(
                    this@AvisosActivity,
                    "Error al enviar: ${e.message ?: "revisa la conexión"}",
                    Toast.LENGTH_LONG
                ).show()
            } finally {
                enviando = false
                botonEnviar.isEnabled = true
                botonEnviar.text = "Enviar aviso"
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

    private fun configurarBarraNavegacion() {
        NavigationHelper.setupBarraNavegacion(
            this,
            binding.barraNavegacion,
            NavigationHelper.Tab.AVISOS
        )
    }
}
