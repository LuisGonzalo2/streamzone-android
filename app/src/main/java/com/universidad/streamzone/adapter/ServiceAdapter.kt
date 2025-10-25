package com.universidad.streamzone.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.universidad.streamzone.R
import com.universidad.streamzone.model.ServiceItem
import com.universidad.streamzone.utils.GradientUtils

/**
 * Adaptador para el RecyclerView de servicios
 *
 * Usa ListAdapter para eficiencia con DiffUtil
 */
class ServiceAdapter(
    private val onServiceClick: (ServiceItem) -> Unit
) : ListAdapter<ServiceItem, ServiceAdapter.ServiceViewHolder>(ServiceDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ServiceViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_service_card, parent, false)
        return ServiceViewHolder(view, onServiceClick)
    }

    override fun onBindViewHolder(holder: ServiceViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    /**
     * ViewHolder para cada tarjeta de servicio
     */
    class ServiceViewHolder(
        itemView: View,
        private val onServiceClick: (ServiceItem) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {

        private val frameServiceIcon: FrameLayout = itemView.findViewById(R.id.frameServiceIcon)
        private val txtServiceIcon: TextView = itemView.findViewById(R.id.txtServiceIcon)
        private val txtServiceTitle: TextView = itemView.findViewById(R.id.txtServiceTitle)
        private val txtServicePrice: TextView = itemView.findViewById(R.id.txtServicePrice)
        private val txtServiceDescription: TextView = itemView.findViewById(R.id.txtServiceDescription)
        private val btnBuyService: MaterialButton = itemView.findViewById(R.id.btnBuyService)

        fun bind(service: ServiceItem) {
            // Título
            txtServiceTitle.text = service.title

            // Precio
            txtServicePrice.text = service.price

            // Descripción
            txtServiceDescription.text = service.description

            // Ícono (primera letra del servicio)
            txtServiceIcon.text = service.getIconLetter()

            // Aplicar gradiente al ícono
            val cornerRadius = itemView.context.resources
                .getDimension(R.dimen.service_icon_corner_radius)

            GradientUtils.applyGradient(
                view = frameServiceIcon,
                startColorHex = service.iconGradientStart,
                endColorHex = service.iconGradientEnd,
                cornerRadius = cornerRadius
            )

            // Click en el botón
            btnBuyService.setOnClickListener {
                onServiceClick(service)
            }

            // Click en toda la tarjeta (opcional)
            itemView.setOnClickListener {
                onServiceClick(service)
            }
        }
    }

    /**
     * DiffUtil para comparar items eficientemente
     */
    private class ServiceDiffCallback : DiffUtil.ItemCallback<ServiceItem>() {
        override fun areItemsTheSame(oldItem: ServiceItem, newItem: ServiceItem): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: ServiceItem, newItem: ServiceItem): Boolean {
            return oldItem == newItem
        }
    }
}