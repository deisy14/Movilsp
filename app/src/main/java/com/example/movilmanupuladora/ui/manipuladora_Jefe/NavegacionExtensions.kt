package com.example.movilmanupuladora.ui.manipuladora_Jefe

import com.example.movilmanupuladora.R

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity

fun AppCompatActivity.configurarNavegacionInferior() {
    findViewById<android.view.View>(R.id.navInicio)?.setOnClickListener {
        if (this !is InicioJefaActivity) {
            val intent = Intent(this, InicioJefaActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            startActivity(intent)
        }
    }
    findViewById<android.view.View>(R.id.navInventario)?.setOnClickListener {
        if (this !is InventarioJefaActivity) {
            val intent = Intent(this, InventarioJefaActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            startActivity(intent)
        }
    }
    findViewById<android.view.View>(R.id.navPreparaciones)?.setOnClickListener {
        if (this !is PreparacionesJefaActivity) {
            val intent = Intent(this, PreparacionesJefaActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            startActivity(intent)
        }
    }
    findViewById<android.view.View>(R.id.navManipuladoras)?.setOnClickListener {
        if (this !is ManipuladorasJefaActivity) {
            val intent = Intent(this, ManipuladorasJefaActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            startActivity(intent)
        }
    }
    findViewById<android.view.View>(R.id.navPerfil)?.setOnClickListener {
        if (this !is PerfilJefaActivity) {
            val intent = Intent(this, PerfilJefaActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            startActivity(intent)
        }
    }
}
