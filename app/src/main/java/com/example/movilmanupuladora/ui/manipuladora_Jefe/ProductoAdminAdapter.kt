package com.example.movilmanupuladora.ui.manipuladora_Jefe

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.movilmanupuladora.R
import com.example.movilmanupuladora.databinding.ItemProductoAdminBinding

class ProductoAdminAdapter(
    private var listaOriginal: List<ProductoAdmin>,
    private val onEditarClick: (ProductoAdmin) -> Unit
) : RecyclerView.Adapter<ProductoAdminAdapter.ProductoViewHolder>() {

    private var listaFiltrada: MutableList<ProductoAdmin> = listaOriginal.toMutableList()

    inner class ProductoViewHolder(val binding: ItemProductoAdminBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductoViewHolder {
        val binding = ItemProductoAdminBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ProductoViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ProductoViewHolder, position: Int) {
        val item = listaFiltrada[position]
        with(holder.binding) {
            txtNombreProductoAdmin.text = item.nombre
            txtCategoriaProductoAdmin.text = "${item.categoria} • Inventario Admin"
            txtCantidadUnidadAdmin.text = item.cantidadConUnidad

            // Dejar el icono general de insumo para todos mientras se configuran las URLs de las fotos reales
            imgProductoAdmin.setImageResource(R.drawable.ic_food)

            btnEditarProductoAdmin.setOnClickListener {
                onEditarClick(item)
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
