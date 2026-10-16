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

            // Mostrar imagen según el alimento para que "se vea o parezca que va una imagen" real
            when {
                item.nombre.contains("arroz", ignoreCase = true) -> {
                    imgProductoAdmin.setImageResource(R.drawable.arroz_pollo)
                }
                item.nombre.contains("frijol", ignoreCase = true) -> {
                    imgProductoAdmin.setImageResource(R.drawable.frijoles)
                }
                item.nombre.contains("lenteja", ignoreCase = true) -> {
                    imgProductoAdmin.setImageResource(R.drawable.lentejas_arroz)
                }
                item.nombre.contains("pasta", ignoreCase = true) -> {
                    imgProductoAdmin.setImageResource(R.drawable.pasta_carne)
                }
                item.nombre.contains("leche", ignoreCase = true) -> {
                    imgProductoAdmin.setImageResource(R.drawable.ic_leche)
                }
                item.nombre.contains("huevo", ignoreCase = true) -> {
                    imgProductoAdmin.setImageResource(R.drawable.huevo_perico)
                }
                item.nombre.contains("pollo", ignoreCase = true) || item.nombre.contains("carne", ignoreCase = true) -> {
                    imgProductoAdmin.setImageResource(R.drawable.pollo_guisado)
                }
                item.categoria.equals("Lácteos", ignoreCase = true) || item.categoria.equals("Lacteos", ignoreCase = true) -> {
                    imgProductoAdmin.setImageResource(R.drawable.ic_leche)
                }
                else -> {
                    imgProductoAdmin.setImageResource(item.imagenRes)
                }
            }

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
