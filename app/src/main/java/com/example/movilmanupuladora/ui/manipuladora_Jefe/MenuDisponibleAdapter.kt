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
    private val bloqueadoPorEnvio: Boolean = false,
    private val esFinDeSemana: Boolean = false,
    private val esDiaAnterior: Boolean = false,
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

        val bloqueado = esFinDeSemana || esDiaAnterior || bloqueadoPorEnvio
        holder.binding.btnSeleccionarMenuDisponible.isEnabled = !bloqueado
        holder.binding.btnSeleccionarMenuDisponible.alpha = if (esDiaAnterior) 0.5f else if (bloqueado) 0.6f else 1.0f
        holder.itemView.alpha = if (esDiaAnterior) 0.65f else 1.0f
        holder.itemView.isEnabled = !bloqueado

        when {
            esFinDeSemana -> {
                holder.binding.btnSeleccionarMenuDisponible.text = "No disponible"
                holder.binding.btnSeleccionarMenuDisponible.setBackgroundResource(R.drawable.bg_dia_semana_inactivo)
                holder.binding.btnSeleccionarMenuDisponible.setTextColor(Color.parseColor("#94A3B8"))
                holder.binding.txtDisponibilidadIngredientes.text = "Fin de semana sin servicio"
                holder.binding.txtDisponibilidadIngredientes.setTextColor(Color.parseColor("#94A3B8"))
            }
            esDiaAnterior -> {
                holder.binding.btnSeleccionarMenuDisponible.text = if (esEsteServicio) "✓ Servido (Solo lectura)" else "Día anterior (Solo lectura)"
                holder.binding.btnSeleccionarMenuDisponible.setBackgroundResource(R.drawable.bg_dia_semana_inactivo)
                holder.binding.btnSeleccionarMenuDisponible.setTextColor(Color.parseColor("#94A3B8"))
                holder.binding.txtDisponibilidadIngredientes.text = "No se pueden editar menús anteriores"
                holder.binding.txtDisponibilidadIngredientes.setTextColor(Color.parseColor("#94A3B8"))
            }
            bloqueadoPorEnvio && esEsteServicio -> {
                holder.binding.btnSeleccionarMenuDisponible.text = "✓ Enviado al supervisor"
                holder.binding.btnSeleccionarMenuDisponible.setBackgroundResource(R.drawable.bg_pill_yellow)
                holder.binding.btnSeleccionarMenuDisponible.setTextColor(Color.parseColor("#92400E"))
                holder.binding.txtDisponibilidadIngredientes.text = "En espera de aprobación ($servicioActual)"
                holder.binding.txtDisponibilidadIngredientes.setTextColor(Color.parseColor("#92400E"))
            }
            bloqueadoPorEnvio -> {
                holder.binding.btnSeleccionarMenuDisponible.text = "En espera de aprobación"
                holder.binding.btnSeleccionarMenuDisponible.setBackgroundResource(R.drawable.bg_dia_semana_inactivo)
                holder.binding.btnSeleccionarMenuDisponible.setTextColor(Color.parseColor("#94A3B8"))
                holder.binding.txtDisponibilidadIngredientes.text = "Día bloqueado tras envío"
                holder.binding.txtDisponibilidadIngredientes.setTextColor(Color.parseColor("#94A3B8"))
            }
            esEsteServicio -> {
                holder.binding.btnSeleccionarMenuDisponible.text = "✓ Seleccionado (Toca para quitar)"
                holder.binding.btnSeleccionarMenuDisponible.setBackgroundResource(R.drawable.bg_pill_yellow)
                holder.binding.btnSeleccionarMenuDisponible.setTextColor(Color.parseColor("#92400E"))
                holder.binding.txtDisponibilidadIngredientes.text = "Asignado a $servicioActual"
                holder.binding.txtDisponibilidadIngredientes.setTextColor(Color.parseColor("#92400E"))
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

        if (bloqueado) {
            holder.binding.btnSeleccionarMenuDisponible.setOnClickListener(null)
            holder.itemView.setOnClickListener(null)
        } else {
            holder.binding.btnSeleccionarMenuDisponible.setOnClickListener {
                onItemClick(plato)
            }
            holder.itemView.setOnClickListener {
                onItemClick(plato)
            }
        }
    }

    override fun getItemCount(): Int = listaPlatos.size
}