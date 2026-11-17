package com.example.movilmanupuladora.ui.manipuladora_Jefe

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.movilmanupuladora.R
import com.example.movilmanupuladora.databinding.ItemMenuDisponiblePaeBinding

class MenuDisponibleAdapter(
    private val listaPlatos: List<InicioJefaActivity.Plato>,
    private val servicioActual: String = "Desayuno",
    private val platoSeleccionadoActual: InicioJefaActivity.Plato? = null,
    private val otrosAsignados: Map<String, String> = emptyMap(),
    private val platosDiaAnterior: Set<String> = emptySet(),
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

        val esEsteServicio = platoSeleccionadoActual != null &&
                plato.nombre.equals(platoSeleccionadoActual.nombre, ignoreCase = true)
        val otroServicio = otrosAsignados[plato.nombre]
        val esDeAyer = platosDiaAnterior.contains(plato.nombre)

        when {
            esEsteServicio -> {
                holder.binding.btnSeleccionarMenuDisponible.text = "✓ Seleccionado (Toca para quitar)"
                holder.binding.btnSeleccionarMenuDisponible.setBackgroundResource(R.drawable.bg_pill_green)
                holder.binding.btnSeleccionarMenuDisponible.setTextColor(Color.parseColor("#15803D"))
                holder.binding.txtDisponibilidadIngredientes.text = "Asignado a $servicioActual"
                holder.binding.txtDisponibilidadIngredientes.setTextColor(Color.parseColor("#15803D"))
            }
            esDeAyer -> {
                holder.binding.btnSeleccionarMenuDisponible.text = "Servido ayer"
                holder.binding.btnSeleccionarMenuDisponible.setBackgroundResource(R.drawable.bg_dia_semana_inactivo)
                holder.binding.btnSeleccionarMenuDisponible.setTextColor(Color.parseColor("#94A3B8"))
                holder.binding.txtDisponibilidadIngredientes.text = "No permitido (Día anterior)"
                holder.binding.txtDisponibilidadIngredientes.setTextColor(Color.parseColor("#EF4444"))
            }
            otroServicio != null -> {
                holder.binding.btnSeleccionarMenuDisponible.text = "En $otroServicio"
                holder.binding.btnSeleccionarMenuDisponible.setBackgroundResource(R.drawable.bg_dia_semana_inactivo)
                holder.binding.btnSeleccionarMenuDisponible.setTextColor(Color.parseColor("#64748B"))
                holder.binding.txtDisponibilidadIngredientes.text = "Asignado en $otroServicio"
                holder.binding.txtDisponibilidadIngredientes.setTextColor(Color.parseColor("#D97706"))
            }
            else -> {
                holder.binding.btnSeleccionarMenuDisponible.text = "Seleccionar plato"
                holder.binding.btnSeleccionarMenuDisponible.setBackgroundResource(R.drawable.bg_pill_yellow)
                holder.binding.btnSeleccionarMenuDisponible.setTextColor(Color.parseColor("#1B3317"))
                holder.binding.txtDisponibilidadIngredientes.text = "Ingredientes disponibles"
                holder.binding.txtDisponibilidadIngredientes.setTextColor(Color.parseColor("#2E7D32"))
            }
        }

        holder.binding.btnSeleccionarMenuDisponible.setOnClickListener {
            onItemClick(plato)
        }
        holder.itemView.setOnClickListener {
            onItemClick(plato)
        }
    }

    override fun getItemCount(): Int = listaPlatos.size
}