package com.example.movilmanupuladora.ui.manipuladora_Jefe

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.movilmanupuladora.R
import com.example.movilmanupuladora.databinding.ItemProductoAdminBinding
import com.example.movilmanupuladora.utils.ImageLoaderUtil
import java.util.Locale

class ProductoAdminAdapter(
    private var listaOriginal: List<ProductoAdmin>,
    private val modoSoloLectura: Boolean = false,
    private val onEditarClick: ((ProductoAdmin) -> Unit)? = null
) : RecyclerView.Adapter<ProductoAdminAdapter.ProductoViewHolder>() {

    private var listaFiltrada: MutableList<ProductoAdmin> = listaOriginal.toMutableList()
    private var categoriaActual: String? = null
    private var queryActual: String = ""

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
            val unidadBonita = formatearUnidad(item.unidadMedida)
            txtUnidadProductoAdmin.text = "Unidad: $unidadBonita"
            txtCantidadValorAdmin.text = String.format(Locale.US, "%.2f", item.cantidad)
            txtCantidadUnidadMiniAdmin.text = unidadBonita

            // Cargar imagen real de Cloudinary si existe; si no, dejar recuadro limpio sin icono amarillo
            if (!item.imagenUrl.isNullOrBlank()) {
                cardImagenProductoAdmin.visibility = android.view.View.VISIBLE
                ImageLoaderUtil.cargarImagen(imgProductoAdmin, item.imagenUrl, android.R.color.transparent)
            } else {
                imgProductoAdmin.setImageDrawable(null)
                cardImagenProductoAdmin.visibility = android.view.View.VISIBLE
            }

            if (!modoSoloLectura && onEditarClick != null) {
                imgFlechaItemAdmin.visibility = android.view.View.VISIBLE
                cardProductoAdmin.isClickable = true
                cardProductoAdmin.setOnClickListener {
                    onEditarClick(item)
                }
            } else {
                imgFlechaItemAdmin.visibility = android.view.View.GONE
                cardProductoAdmin.setOnClickListener(null)
                cardProductoAdmin.isClickable = false
            }
        }
    }

    private fun formatearUnidad(unidad: String): String {
        return when (unidad.lowercase().trim()) {
            "kg", "kilogramo", "kilogramos", "k" -> "Kg"
            "g", "gr", "gramo", "gramos" -> "g"
            "l", "litro", "litros", "ltr" -> "L"
            "ml", "mililitro", "mililitros" -> "ml"
            "u", "und", "unidad", "unidades" -> "Und"
            else -> unidad.ifBlank { "Kg" }
        }
    }

    override fun getItemCount(): Int = listaFiltrada.size

    fun actualizarLista(nuevaLista: List<ProductoAdmin>) {
        listaOriginal = nuevaLista
        aplicarFiltros(categoriaActual, queryActual)
    }

    fun filtrarPorCategoria(categoria: String?) {
        aplicarFiltros(categoria, queryActual)
    }

    fun filtrarPorTexto(texto: String) {
        aplicarFiltros(categoriaActual, texto)
    }

    fun aplicarFiltros(categoria: String?, texto: String) {
        categoriaActual = categoria
        queryActual = texto.trim()

        listaFiltrada = listaOriginal.filter { item ->
            val coincideCat = if (categoriaActual.isNullOrBlank()) {
                true
            } else {
                normalizar(item.categoria) == normalizar(categoriaActual!!)
            }
            val coincideTexto = if (queryActual.isEmpty()) {
                true
            } else {
                normalizar(item.nombre).contains(normalizar(queryActual)) ||
                    normalizar(item.categoria).contains(normalizar(queryActual))
            }
            coincideCat && coincideTexto
        }.toMutableList()
        notifyDataSetChanged()
    }

    private fun normalizar(txt: String): String {
        val nfd = java.text.Normalizer.normalize(txt, java.text.Normalizer.Form.NFD)
        return nfd.replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "")
            .lowercase(Locale.getDefault())
            .trim()
    }
}
