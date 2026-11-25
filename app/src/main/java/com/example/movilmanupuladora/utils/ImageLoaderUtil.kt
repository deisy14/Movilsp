package com.example.movilmanupuladora.utils

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.widget.ImageView
import androidx.collection.LruCache
import com.example.movilmanupuladora.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

object ImageLoaderUtil {

    private val maxMemory = (Runtime.getRuntime().maxMemory() / 1024).toInt()
    private val cacheSize = maxMemory / 8
    private val memoryCache = object : LruCache<String, Bitmap>(cacheSize) {
        override fun sizeOf(key: String, bitmap: Bitmap): Int {
            return bitmap.byteCount / 1024
        }
    }

    fun cargarImagen(imageView: ImageView, url: String?, fallbackRes: Int = 0) {
        val placeholder = if (fallbackRes != 0) fallbackRes else R.drawable.bg_placeholder_plato_ia

        if (url.isNullOrBlank()) {
            imageView.setImageResource(placeholder)
            return
        }

        val cached = memoryCache.get(url)
        if (cached != null) {
            imageView.setImageBitmap(cached)
            return
        }

        imageView.setImageResource(placeholder)
        imageView.tag = url

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val bitmap = descargarBitmap(url, maxRedirecciones = 3)
                if (bitmap != null) {
                    memoryCache.put(url, bitmap)
                    withContext(Dispatchers.Main) {
                        if (imageView.tag == url) {
                            imageView.setImageBitmap(bitmap)
                        }
                    }
                }
            } catch (_: Exception) { }
        }
    }

    private fun descargarBitmap(urlStr: String, maxRedirecciones: Int): Bitmap? {
        var urlActual = urlStr
        var redirecciones = 0

        while (redirecciones < maxRedirecciones) {
            val url = URL(urlActual)
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 25000
            conn.readTimeout = 25000
            conn.instanceFollowRedirects = false
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
            conn.setRequestProperty("Accept", "image/avif,image/webp,image/apng,image/svg+xml,image/*,*/*;q=0.8")
            conn.doInput = true
            conn.connect()

            val status = conn.responseCode
            if (status in 300..399) {
                val redirectUrl = conn.getHeaderField("Location")
                conn.disconnect()
                if (redirectUrl.isNullOrBlank()) return null
                urlActual = redirectUrl
                redirecciones++
                continue
            }

            if (status == HttpURLConnection.HTTP_OK) {
                val input: InputStream = conn.inputStream
                val bitmap = BitmapFactory.decodeStream(input)
                input.close()
                conn.disconnect()
                return bitmap
            } else {
                conn.disconnect()
                return null
            }
        }
        return null
    }
}
