package com.example.movilmanupuladora.ui.manipuladora_Jefe

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.movilmanupuladora.databinding.ItemMenuDisponiblePaeBinding

class MenuDisponibleAdapter(
    private val listaPlatos: List<InicioJefaActivity.Plato>,
    private val onItemClick: (InicioJefaActivity.Plato) -> Unit
) : RecyclerView.Adapter<MenuDisponibleAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemMenuDisponiblePaeBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemMenuDisponiblePaeBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val plato = listaPlatos[position]
        holder.binding.imgPlatoDisponible.setImageResource(plato.imagen)
        holder.binding.txtNombrePlatoDisponible.text = plato.nombre
        holder.binding.txtDisponibilidadIngredientes.text = "✓ Ingredientes disponibles"

        holder.binding.btnSeleccionarMenuDisponible.setOnClickListener {
            onItemClick(plato)
        }
        holder.itemView.setOnClickListener {
            onItemClick(plato)
        }
    }

    override fun getItemCount(): Int = listaPlatos.size
}