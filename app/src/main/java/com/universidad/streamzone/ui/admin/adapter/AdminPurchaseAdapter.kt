package com.universidad.streamzone.ui.admin.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.universidad.streamzone.R
import com.universidad.streamzone.data.model.PurchaseEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AdminPurchaseAdapter(
    private var purchases: List<PurchaseEntity>,
    private val onAssignCredentials: (PurchaseEntity) -> Unit
) : RecyclerView.Adapter<AdminPurchaseAdapter.PurchaseViewHolder>() {

    class PurchaseViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvUserName: TextView = view.findViewById(R.id.tvUserName)
        val tvUserEmail: TextView = view.findViewById(R.id.tvUserEmail)
        val tvPurchaseStatus: TextView = view.findViewById(R.id.tvPurchaseStatus)
        val tvServiceName: TextView = view.findViewById(R.id.tvServiceName)
        val tvPrice: TextView = view.findViewById(R.id.tvPrice)
        val tvDuration: TextView = view.findViewById(R.id.tvDuration)
        val tvPurchaseDate: TextView = view.findViewById(R.id.tvPurchaseDate)
        val btnAssignCredentials: Button = view.findViewById(R.id.btnAssignCredentials)
        val llCredentials: LinearLayout = view.findViewById(R.id.llCredentials)
        val tvCredentialEmail: TextView = view.findViewById(R.id.tvCredentialEmail)
        val tvCredentialPassword: TextView = view.findViewById(R.id.tvCredentialPassword)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PurchaseViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_admin_purchase, parent, false)
        return PurchaseViewHolder(view)
    }

    override fun onBindViewHolder(holder: PurchaseViewHolder, position: Int) {
        val purchase = purchases[position]
        val context = holder.itemView.context

        // Usuario
        holder.tvUserName.text = purchase.userName
        holder.tvUserEmail.text = purchase.userEmail

        // Estado
        when (purchase.status) {
            "pending" -> {
                holder.tvPurchaseStatus.text = "⏳ Pendiente"
                holder.tvPurchaseStatus.setTextColor(0xFFF59E0B.toInt())
                holder.tvPurchaseStatus.setBackgroundColor(0x20F59E0B)
                holder.btnAssignCredentials.visibility = View.VISIBLE
                holder.llCredentials.visibility = View.GONE
            }
            "active" -> {
                holder.tvPurchaseStatus.text = "✅ Activa"
                holder.tvPurchaseStatus.setTextColor(0xFF10B981.toInt())
                holder.tvPurchaseStatus.setBackgroundColor(0x2010B981)
                holder.btnAssignCredentials.visibility = View.GONE
                holder.llCredentials.visibility = View.VISIBLE

                // Mostrar credenciales
                holder.tvCredentialEmail.text = purchase.email ?: "No asignado"
                holder.tvCredentialPassword.text = purchase.password ?: "********"
            }
            "expired" -> {
                holder.tvPurchaseStatus.text = "❌ Expirada"
                holder.tvPurchaseStatus.setTextColor(context.getColor(R.color.white))
                holder.tvPurchaseStatus.setBackgroundColor(context.getColor(android.R.color.holo_red_dark))
                holder.btnAssignCredentials.visibility = View.GONE
                holder.llCredentials.visibility = View.GONE
            }
        }

        // Servicio
        holder.tvServiceName.text = purchase.serviceName

        // Precio y duración
        holder.tvPrice.text = purchase.servicePrice
        holder.tvDuration.text = purchase.serviceDuration

        // Fecha
        val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale("es", "ES"))
        holder.tvPurchaseDate.text = "Comprado: ${dateFormat.format(Date(purchase.purchaseDate))}"

        // Botón asignar credenciales
        holder.btnAssignCredentials.setOnClickListener {
            onAssignCredentials(purchase)
        }
    }

    override fun getItemCount(): Int = purchases.size

    fun updatePurchases(newPurchases: List<PurchaseEntity>) {
        purchases = newPurchases
        notifyDataSetChanged()
    }
}