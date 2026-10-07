package com.example.movilmanupuladora.ui.manipuladora_Jefe

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.movilmanupuladora.databinding.ItemSugerenciaIaBinding

class SugerenciaIaAdapter(
    private val listaPlatos: List<InicioJefaActivity.Plato>,
    private val onItemClick: (InicioJefaActivity.Plato) -> Unit
) : RecyclerView.Adapter<SugerenciaIaAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: ItemSugerenciaIaBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemSugerenciaIaBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val plato = listaPlatos[position]
        holder.binding.imgPlatoCarousel.setImageResource(plato.imagen)
        holder.binding.tvNombrePlatoCarousel.text = plato.nombre
        holder.binding.tvBadgeDisponibilidad.text = "✓ Ingredientes disponibles"

        holder.itemView.setOnClickListener {
            onItemClick(plato)
        }
    }

    override fun getItemCount(): Int = listaPlatos.size
}