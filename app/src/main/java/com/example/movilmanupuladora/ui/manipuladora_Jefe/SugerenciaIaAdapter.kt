package com.example.movilmanupuladora.ui.manipuladora_Jefe

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.movilmanupuladora.databinding.ItemSugerenciaIaBinding

class SugerenciaIaAdapter(
    private val listaPlatos: List<InicioJefaActivity.Plato>,
    private val servicioActual: String = "Desayuno",
    private val platoSeleccionadoActual: InicioJefaActivity.Plato? = null,
    private val otrosAsignados: Map<String, String> = emptyMap(),
    private val platosDiaAnterior: Set<String> = emptySet(),
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

        val esEsteServicio = platoSeleccionadoActual != null &&
                plato.nombre.equals(platoSeleccionadoActual.nombre, ignoreCase = true)
        val otroServicio = otrosAsignados[plato.nombre]
        val esDeAyer = platosDiaAnterior.contains(plato.nombre)

        when {
            esEsteServicio -> {
                holder.binding.tvBadgeDisponibilidad.text = "✓ Seleccionado"
                holder.binding.cardPlatoContenedor.strokeColor = Color.parseColor("#15803D")
            }
            esDeAyer -> {
                holder.binding.tvBadgeDisponibilidad.text = "Servido ayer"
                holder.binding.cardPlatoContenedor.strokeColor = Color.parseColor("#EF4444")
            }
            otroServicio != null -> {
                holder.binding.tvBadgeDisponibilidad.text = "En $otroServicio"
                holder.binding.cardPlatoContenedor.strokeColor = Color.parseColor("#CBD5E1")
            }
            else -> {
                holder.binding.tvBadgeDisponibilidad.text = "Recom. IA"
                holder.binding.cardPlatoContenedor.strokeColor = Color.parseColor("#1B3317")
            }
        }

        holder.binding.cardPlatoContenedor.setOnClickListener {
            onItemClick(plato)
        }
        holder.itemView.setOnClickListener {
            onItemClick(plato)
        }
    }

    override fun getItemCount(): Int = listaPlatos.size
}