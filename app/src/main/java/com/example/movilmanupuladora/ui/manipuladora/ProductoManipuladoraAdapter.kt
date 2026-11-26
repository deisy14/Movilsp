package com.example.movilmanupuladora.ui.manipuladora

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.movilmanupuladora.R
import com.example.movilmanupuladora.databinding.ItemProductoManipuladoraBinding
import com.example.movilmanupuladora.ui.manipuladora_Jefe.ProductoAdmin

class ProductoManipuladoraAdapter(
    private var listaOriginal: List<ProductoAdmin>
) : RecyclerView.Adapter<ProductoManipuladoraAdapter.ManipuladoraViewHolder>() {

    private var listaFiltrada: MutableList<ProductoAdmin> = listaOriginal.toMutableList()

    inner class ManipuladoraViewHolder(val binding: ItemProductoManipuladoraBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ManipuladoraViewHolder {
        val binding = ItemProductoManipuladoraBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ManipuladoraViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ManipuladoraViewHolder, position: Int) {
        val item = listaFiltrada[position]
        with(holder.binding) {
            txtNombreProductoManipuladora.text = item.nombre
            txtCategoriaProductoManipuladora.text = "${item.categoria} · Inventario Admin"

            val c = item.cantidad
            val u = item.unidadMedida
            val cantidadFormatted = if (c % 1.0 == 0.0) "${c.toInt()}" else "%.1f".format(c)
            val unidadUpper = u.uppercase().take(1)
            txtCantidadUnidadManipuladora.text = "$cantidadFormatted$unidadUpper"

            if (item.cantidad > 0.0) {
                txtEstadoProductoManipuladora.text = "Disponible"
                txtEstadoProductoManipuladora.setTextColor(Color.parseColor("#15803D"))
                txtCantidadUnidadManipuladora.setBackgroundResource(R.drawable.bg_pill_green)
                root.strokeColor = Color.parseColor("#E2E8F0")
            } else {
                txtEstadoProductoManipuladora.text = "Agotado"
                txtEstadoProductoManipuladora.setTextColor(Color.parseColor("#B91C1C"))
                txtCantidadUnidadManipuladora.setBackgroundResource(R.drawable.bg_chip_desayuno)
                root.strokeColor = Color.parseColor("#F4B41F")
            }
        }
    }

    override fun getItemCount(): Int = listaFiltrada.size

    fun actualizarLista(nuevaLista: List<ProductoAdmin>) {
        listaOriginal = nuevaLista
        listaFiltrada = nuevaLista.toMutableList()
        notifyDataSetChanged()
    }

    fun filtrarPorCategoria(categoria: String?) {
        listaFiltrada = if (categoria.isNullOrEmpty()) {
            listaOriginal.toMutableList()
        } else {
            listaOriginal.filter {
                it.categoria.trim().equals(categoria.trim(), ignoreCase = true) ||
                (categoria.equals("Lácteos", ignoreCase = true) && it.categoria.equals("Lacteos", ignoreCase = true)) ||
                (categoria.equals("Lacteos", ignoreCase = true) && it.categoria.equals("Lácteos", ignoreCase = true))
            }.toMutableList()
        }
        notifyDataSetChanged()
    }
}
