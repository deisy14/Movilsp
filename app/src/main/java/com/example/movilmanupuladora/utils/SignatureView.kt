package com.example.movilmanupuladora.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.util.Base64
import android.view.MotionEvent
import android.view.View
import java.io.ByteArrayOutputStream

/**
 * Vista de firma digital táctil interactiva para la Manipuladora Jefe en SIRAE PAE.
 * Permite trazar la firma con el dedo o stylus y exportar en Base64 o limpiar el trazo.
 */
class SignatureView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val path = Path()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#1B3317") // Verde oscuro institucional / azul tinta
        style = Paint.Style.STROKE
        strokeWidth = 6f
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    private var hasSignature = false

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawPath(path, paint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!isEnabled) return false
        parent?.requestDisallowInterceptTouchEvent(true)
        val x = event.x
        val y = event.y

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                path.moveTo(x, y)
                hasSignature = true
                invalidate()
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                path.lineTo(x, y)
                invalidate()
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                parent?.requestDisallowInterceptTouchEvent(false)
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    fun clear() {
        path.reset()
        hasSignature = false
        invalidate()
    }

    fun hasSignature(): Boolean = hasSignature

    fun getSignaturePngBytes(): ByteArray? {
        if (!hasSignature || width <= 0 || height <= 0) return null
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)
        draw(canvas)
        val maxW = 480
        val exportBitmap = if (width > maxW) {
            val scaledH = ((height.toFloat() / width.toFloat()) * maxW).toInt().coerceAtLeast(1)
            Bitmap.createScaledBitmap(bitmap, maxW, scaledH, true)
        } else {
            bitmap
        }
        val stream = ByteArrayOutputStream()
        exportBitmap.compress(Bitmap.CompressFormat.JPEG, 82, stream)
        if (exportBitmap !== bitmap) exportBitmap.recycle()
        bitmap.recycle()
        return stream.toByteArray()
    }

    fun getSignatureBase64(): String? {
        val byteArray = getSignaturePngBytes() ?: return null
        return Base64.encodeToString(byteArray, Base64.NO_WRAP)
    }
}
