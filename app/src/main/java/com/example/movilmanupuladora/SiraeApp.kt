package com.example.movilmanupuladora

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import com.example.movilmanupuladora.utils.SessionManager

class SiraeApp : Application() {

    companion object {
        lateinit var instance: SiraeApp
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        // Desactiva el modo oscuro en toda la app para que siempre
        // se mantengan los colores originales y el diseño diseñado
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)

        // Asegurar que el token JWT esté cargado en RetrofitClient desde el arranque
        SessionManager.getToken(this)
    }
}
