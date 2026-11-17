package com.example.movilmanupuladora.ui.manipuladora_Jefe

import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.movilmanupuladora.data.api.RetrofitClient
import com.example.movilmanupuladora.databinding.ActivityNotificacionesBinding
import com.google.gson.JsonObject
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import java.text.Normalizer

class PopupNotificacionesActivity : AppCompatActivity() {

    private lateinit var binding: ActivityNotificacionesBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNotificacionesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        configurarPermisosPorRol()

        binding.btnEnviarNotificacion.setOnClickListener {
            enviarNotificacion()
        }

        binding.btnMarcarTodasLeidas.setOnClickListener {
            marcarTodasLeidas()
        }

        cargarNotificaciones()
    }

    private fun configurarPermisosPorRol() {
        val preferencias = getSharedPreferences("user_prefs", MODE_PRIVATE)
        val rolGuardado = preferencias.getString("rol", "") ?: ""

        val rol = Normalizer.normalize(rolGuardado, Normalizer.Form.NFD)
            .replace(Regex("\\p{Mn}+"), "")
            .lowercase()
            .replace(" ", "")
            .replace("_", "")

        // Verificamos si es un rol con privilegios (Jefaturas o Admins) y filtramos manipuladoras raso
        val esJefa = rol.contains("jefa") || rol.contains("admin") || rol.contains("super")
        val esManipuladoraComun = rol.contains("manipuladora") && !rol.contains("jefa")

        val puedeEnviar = esJefa && !esManipuladoraComun

        binding.edtTituloNotificacion.visibility = if (puedeEnviar) View.VISIBLE else View.GONE
        binding.edtMensajeNotificacion.visibility = if (puedeEnviar) View.VISIBLE else View.GONE
        binding.btnEnviarNotificacion.visibility = if (puedeEnviar) View.VISIBLE else View.GONE

        if (!puedeEnviar) {
            binding.txtEstadoNotificaciones.text = "Consulta tus notificaciones."
        }
    }

    private fun marcarTodasLeidas() {
        binding.btnMarcarTodasLeidas.isEnabled = false
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.marcarTodasLeidas()
                binding.btnMarcarTodasLeidas.isEnabled = true
                if (response.isSuccessful) {
                    cargarNotificaciones()
                } else {
                    binding.txtEstadoNotificaciones.text = "No se pudieron marcar como leídas (${response.code()})."
                }
            } catch (e: Exception) {
                binding.btnMarcarTodasLeidas.isEnabled = true
                binding.txtEstadoNotificaciones.text = "Error de conexión: ${e.message}"
            }
        }
    }

    private fun enviarNotificacion() {
        val tituloStr = binding.edtTituloNotificacion.text.toString().trim()
        val mensajeStr = binding.edtMensajeNotificacion.text.toString().trim()

        if (tituloStr.isEmpty()) {
            binding.edtTituloNotificacion.error = "Escribe el título"
            binding.edtTituloNotificacion.requestFocus()
            return
        }

        if (mensajeStr.isEmpty()) {
            binding.edtMensajeNotificacion.error = "Escribe el mensaje"
            binding.edtMensajeNotificacion.requestFocus()
            return
        }

        binding.btnEnviarNotificacion.isEnabled = false
        binding.txtEstadoNotificaciones.text = "Enviando notificación..."

        val tituloPart = tituloStr.toRequestBody("text/plain".toMediaTypeOrNull())
        val mensajePart = mensajeStr.toRequestBody("text/plain".toMediaTypeOrNull())

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.enviarNotificacion(tituloPart, mensajePart, null)
                binding.btnEnviarNotificacion.isEnabled = true

                if (response.isSuccessful) {
                    binding.txtEstadoNotificaciones.text = "Notificación enviada con éxito."
                    binding.edtTituloNotificacion.text.clear()
                    binding.edtMensajeNotificacion.text.clear()
                    cargarNotificaciones()
                } else {
                    binding.txtEstadoNotificaciones.text = "No se pudo enviar (${response.code()})."
                }
            } catch (e: Exception) {
                binding.btnEnviarNotificacion.isEnabled = true
                binding.txtEstadoNotificaciones.text = "Error al enviar: ${e.message}"
            }
        }
    }

    private fun cargarNotificaciones() {
        binding.txtEstadoNotificaciones.text = "Cargando notificaciones..."
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.obtenerNotificaciones()
                if (!response.isSuccessful) {
                    binding.txtEstadoNotificaciones.text = "No se pudieron cargar (${response.code()})."
                    return@launch
                }

                binding.contenedorNotificaciones.removeAllViews()
                val body = response.body()
                val lista = if (body is List<*>) body else emptyList<Any?>()

                if (lista.isEmpty()) {
                    binding.txtEstadoNotificaciones.text = "No tienes notificaciones."
                    return@launch
                }

                binding.txtEstadoNotificaciones.text = "${lista.size} notificación(es)"

                lista.forEach { item ->
                    val jsonObj = item as? JsonObject
                    val titulo = jsonObj?.get("titulo")?.takeIf { !it.isJsonNull }?.asString ?: "Aviso"
                    val mensaje = jsonObj?.get("mensaje")?.takeIf { !it.isJsonNull }?.asString ?: ""
                    val fecha = jsonObj?.get("fecha_hora")?.takeIf { !it.isJsonNull }?.asString ?: ""

                    val tarjeta = LinearLayout(this@PopupNotificacionesActivity).apply {
                        orientation = LinearLayout.VERTICAL
                        setPadding(24, 20, 24, 20)
                        setBackgroundColor(0xFFFFF8E1.toInt()) // Tono amarillo suave
                    }

                    val tituloView = TextView(this@PopupNotificacionesActivity).apply {
                        text = titulo
                        textSize = 16f
                        setTextColor(0xFF3E2723.toInt())
                    }

                    val mensajeView = TextView(this@PopupNotificacionesActivity).apply {
                        text = mensaje
                        textSize = 14f
                        setTextColor(0xFF5D4037.toInt())
                    }

                    val fechaView = TextView(this@PopupNotificacionesActivity).apply {
                        text = fecha
                        textSize = 11f
                        setTextColor(0xFF795548.toInt())
                    }

                    tarjeta.addView(tituloView)
                    tarjeta.addView(mensajeView)
                    tarjeta.addView(fechaView)

                    val parametros = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        setMargins(0, 0, 0, 16)
                    }

                    binding.contenedorNotificaciones.addView(tarjeta, parametros)
                }
            } catch (e: Exception) {
                binding.txtEstadoNotificaciones.text = "Error al cargar: ${e.message}"
            }
        }
    }
}