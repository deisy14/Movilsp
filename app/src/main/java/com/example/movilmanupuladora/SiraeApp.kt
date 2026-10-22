package com.example.movilmanupuladora

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate

class SiraeApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Desactiva el modo oscuro en toda la app para que siempre
        // se mantengan los colores originales y el diseño diseñado
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
    }
}
