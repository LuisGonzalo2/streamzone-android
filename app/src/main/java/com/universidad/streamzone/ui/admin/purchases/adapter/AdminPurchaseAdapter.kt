package com.universidad.streamzone.ui.admin.purchases.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.universidad.streamzone.R
import com.universidad.streamzone.data.model.PurchaseEntity
import java.text.SimpleDateFormat
import java.util.*

class AdminPurchaseAdapter(
    private val onAssignClick: (PurchaseEntity) -> Unit
) : ListAdapter<PurchaseEntity, AdminPurchaseAdapter.PurchaseViewHolder>(PurchaseDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PurchaseViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_admin_purchase, parent, false)
        return PurchaseViewHolder(view, onAssignClick)
    }

    override fun onBindViewHolder(holder: PurchaseViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class PurchaseViewHolder(
        itemView: View,
        private val onAssignClick: (PurchaseEntity) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {

        private val tvServiceIcon: TextView = itemView.findViewById(R.id.tv_service_icon)
        private val tvServiceName: TextView = itemView.findViewById(R.id.tv_service_name)
        private val tvServicePrice: TextView = itemView.findViewById(R.id.tv_service_price)
        private val tvUserName: TextView = itemView.findViewById(R.id.tv_user_name)
        private val tvUserEmail: TextView = itemView.findViewById(R.id.tv_user_email)
        private val tvPurchaseDate: TextView = itemView.findViewById(R.id.tv_purchase_date)
        private val tvDuration: TextView = itemView.findViewById(R.id.tv_duration)
        private val btnAssignCredentials: Button = itemView.findViewById(R.id.btn_assign_credentials)

        fun bind(purchase: PurchaseEntity) {
            // Icono según el servicio
            tvServiceIcon.text = getServiceIcon(purchase.serviceId)
            tvServiceName.text = purchase.serviceName
            tvServicePrice.text = purchase.servicePrice
            tvUserName.text = purchase.userName
            tvUserEmail.text = purchase.userEmail
            tvDuration.text = purchase.serviceDuration

            // Formatear fecha
            val dateFormat = SimpleDateFormat("dd MMM yyyy - HH:mm", Locale("es", "ES"))
            tvPurchaseDate.text = dateFormat.format(Date(purchase.purchaseDate))

            // Click en asignar credenciales
            btnAssignCredentials.setOnClickListener {
                onAssignClick(purchase)
            }
        }

        private fun getServiceIcon(serviceId: String): String {
            return when (serviceId.lowercase()) {
                "netflix" -> "📺"
                "spotify" -> "🎵"
                "disney_plus_premium", "disney_plus_standard" -> "✨"
                "max" -> "🎬"
                "prime" -> "📦"
                "youtube_premium" -> "▶️"
                "chatgpt" -> "🤖"
                "canva" -> "🎨"
                else -> "📦"
            }
        }
    }

    class PurchaseDiffCallback : DiffUtil.ItemCallback<PurchaseEntity>() {
        override fun areItemsTheSame(oldItem: PurchaseEntity, newItem: PurchaseEntity): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: PurchaseEntity, newItem: PurchaseEntity): Boolean {
            return oldItem == newItem
        }
    }
}