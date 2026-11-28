package com.example.movilmanupuladora.ui.manipuladora_Jefe

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.movilmanupuladora.R
import com.example.movilmanupuladora.data.model.PlatoResponse
import com.example.movilmanupuladora.databinding.ItemPlatoCatalogoBinding

class PlatoCatalogoAdapter(
    private val listaPlatos: List<PlatoResponse>,
    private val onItemClick: (PlatoResponse) -> Unit
) : RecyclerView.Adapter<PlatoCatalogoAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemPlatoCatalogoBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemPlatoCatalogoBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val plato = listaPlatos[position]
        val nombreLimpio = com.example.movilmanupuladora.utils.SessionManager.limpiarPrefijoMenu(
            plato.nombrePlato ?: "Plato PAE"
        )
        holder.binding.tvNombrePlatoCatalogo.text = nombreLimpio
        holder.binding.tvComponenteCatalogo.text = plato.componente?.takeIf { it.isNotBlank() }
            ?: "Componentes configurados en PAE"

        val imgRes = InicioJefaActivity.obtenerImagenPlatoPorNombre(nombreLimpio)
        holder.binding.imgPlatoCatalogo.setImageResource(imgRes)

        holder.itemView.setOnClickListener {
            onItemClick(plato)
        }
    }

    override fun getItemCount(): Int = listaPlatos.size
}