package com.example.movilmanupuladora.ui.manipuladora

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
            txtCategoriaProductoManipuladora.text = "${item.categoria} • Inventario Admin"
            txtCantidadUnidadManipuladora.text = item.cantidadConUnidad
            imgProductoManipuladora.setImageResource(R.drawable.ic_food)
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
