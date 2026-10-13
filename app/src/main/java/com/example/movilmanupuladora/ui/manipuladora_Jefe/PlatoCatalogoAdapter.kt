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
        holder.binding.tvNombrePlatoCatalogo.text = plato.nombrePlato ?: "Plato PAE"
        holder.binding.tvComponenteCatalogo.text = plato.componente ?: "Sopas, Sazón y Proteína"

        val imgRes = when {
            (plato.nombrePlato ?: "").lowercase().contains("bandeja") -> R.drawable.bandeja_paisa
            (plato.nombrePlato ?: "").lowercase().contains("frijol") -> R.drawable.frijoles
            (plato.nombrePlato ?: "").lowercase().contains("chocolate") -> R.drawable.chocolate
            (plato.nombrePlato ?: "").lowercase().contains("huevo") -> R.drawable.huevo_perico
            (plato.nombrePlato ?: "").lowercase().contains("pasta") -> R.drawable.pasta_carne
            (plato.nombrePlato ?: "").lowercase().contains("lenteja") -> R.drawable.lentejas_arroz
            else -> R.drawable.pollo_guisado
        }
        holder.binding.imgPlatoCatalogo.setImageResource(imgRes)

        holder.itemView.setOnClickListener {
            onItemClick(plato)
        }
    }

    override fun getItemCount(): Int = listaPlatos.size
}