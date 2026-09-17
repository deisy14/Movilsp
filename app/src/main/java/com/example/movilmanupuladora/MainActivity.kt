package com.example.movilmanupuladora

import android.os.Bundle
import android.view.MotionEvent
import androidx.appcompat.app.AppCompatActivity
import com.example.movilmanupuladora.databinding.ActivityMainBinding
import kotlin.math.atan2

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    data class Plato(
        val nombre: String,
        val ingredientes: String,
        val imagenResId: Int
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val listaPlatos = listOf(
            Plato(
                "CUY FRITO",
                "• Ingredientes disponibles",
                R.drawable.apanado
            ),
            Plato(
                "CASUELA DE FRIJOLES",
                "• Ingredientes disponibles",
                R.drawable.frijoles
            ),
            Plato(
                "RATICA BROASTER",
                "• Ingredientes disponibles",
                R.drawable.apanado
            ),
            Plato(
                "FRIJOLES CON CARNE MOLIDA",
                "• Ingredientes disponibles",
                R.drawable.frijoles
            )
        )

        var totalRotation = 0f
        var lastAngle = 0f

        binding.wheelContainer.setOnTouchListener { view, event ->

            val centerX = view.width / 2f
            val centerY = view.height / 2f

            val x = event.x
            val y = event.y

            when (event.action) {

                MotionEvent.ACTION_DOWN -> {

                    lastAngle = Math.toDegrees(
                        atan2(
                            (y - centerY).toDouble(),
                            (x - centerX).toDouble()
                        )
                    ).toFloat()

                    true
                }

                MotionEvent.ACTION_MOVE -> {

                    val currentAngle = Math.toDegrees(
                        atan2(
                            (y - centerY).toDouble(),
                            (x - centerX).toDouble()
                        )
                    ).toFloat()

                    var deltaAngle = currentAngle - lastAngle

                    if (deltaAngle > 180f) {
                        deltaAngle -= 360f
                    }

                    if (deltaAngle < -180f) {
                        deltaAngle += 360f
                    }

                    totalRotation += deltaAngle

                    binding.wheelContainer.rotation =
                        totalRotation

                    lastAngle = currentAngle

                    true
                }

                MotionEvent.ACTION_UP,
                MotionEvent.ACTION_CANCEL -> {

                    val normalizedRotation =
                        (totalRotation % 360f + 360f) % 360f

                    val index =
                        ((normalizedRotation + 45f) / 90f)
                            .toInt() % listaPlatos.size

                    val platoActual =
                        listaPlatos[index]

                    binding.tvPlatoSeleccionado.text =
                        platoActual.nombre

                    binding.tvIngredientes.text =
                        platoActual.ingredientes

                    true
                }

                else -> false
            }
        }

    }

}