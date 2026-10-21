package com.example.movilmanupuladora.ui.manipuladora_Jefa

import androidx.appcompat.app.AppCompatActivity
import com.example.movilmanupuladora.R
import com.example.movilmanupuladora.ui.manipuladora_Jefe.InicioJefaActivity
import com.example.movilmanupuladora.ui.manipuladora_Jefe.InventarioJefaActivity
import com.example.movilmanupuladora.ui.manipuladora_Jefe.ManipuladorasJefaActivity
import com.example.movilmanupuladora.ui.manipuladora_Jefe.PerfilJefaActivity
import com.example.movilmanupuladora.ui.manipuladora_Jefe.PreparacionesJefaActivity
import com.example.movilmanupuladora.utils.NavigationHelperJefa

fun AppCompatActivity.configurarNavegacionInferior() {
    val tabActual = when (this) {
        is InicioJefaActivity -> NavigationHelperJefa.TabJefa.INICIO
        is InventarioJefaActivity -> NavigationHelperJefa.TabJefa.INVENTARIO
        is PreparacionesJefaActivity -> NavigationHelperJefa.TabJefa.PREPARACIONES
        is ManipuladorasJefaActivity -> NavigationHelperJefa.TabJefa.MANIPULADORAS
        is PerfilJefaActivity -> NavigationHelperJefa.TabJefa.PERFIL
        else -> NavigationHelperJefa.TabJefa.INICIO
    }

    val bottomBarView = findViewById<android.view.View>(R.id.viewBottomBarContainer)
        ?: findViewById<android.view.View>(R.id.barraNavegacionJefaContainer)
        ?: findViewById<android.view.View>(R.id.barra_navegacion_jefa)
        ?: window.decorView

    NavigationHelperJefa.setupBarraNavegacionJefaView(this, bottomBarView, tabActual)
}
