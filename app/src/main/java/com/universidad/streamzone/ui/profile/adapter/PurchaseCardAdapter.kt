package com.universidad.streamzone.ui.profile.adapter

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.universidad.streamzone.R
import com.universidad.streamzone.data.model.PurchaseEntity
import java.text.SimpleDateFormat
import java.util.*

class PurchaseCardAdapter(
    private val purchases: List<PurchaseEntity>,
    private val onPurchaseClick: (PurchaseEntity) -> Unit = {}
) : RecyclerView.Adapter<PurchaseCardAdapter.PurchaseViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PurchaseViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_purchase_card, parent, false)
        return PurchaseViewHolder(view)
    }

    override fun onBindViewHolder(holder: PurchaseViewHolder, position: Int) {
        holder.bind(purchases[position])
    }

    override fun getItemCount() = purchases.size

    inner class PurchaseViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val tvServiceIcon: TextView = view.findViewById(R.id.tv_service_icon)
        private val tvServiceName: TextView = view.findViewById(R.id.tv_service_name)
        private val tvServicePrice: TextView = view.findViewById(R.id.tv_service_price)
        private val tvStatusBadge: TextView = view.findViewById(R.id.tv_status_badge)
        private val tvPurchaseDate: TextView = view.findViewById(R.id.tv_purchase_date)
        private val tvExpirationDate: TextView = view.findViewById(R.id.tv_expiration_date)
        private val tvDuration: TextView = view.findViewById(R.id.tv_duration)
        private val credentialsContainer: LinearLayout = view.findViewById(R.id.credentials_container)
        private val tvCredentialEmail: TextView = view.findViewById(R.id.tv_credential_email)
        private val tvCredentialPassword: TextView = view.findViewById(R.id.tv_credential_password)
        private val btnCopyEmail: Button = view.findViewById(R.id.btn_copy_email)
        private val btnCopyPassword: Button = view.findViewById(R.id.btn_copy_password)
        private val tvPendingMessage: TextView = view.findViewById(R.id.tv_pending_message)

        fun bind(purchase: PurchaseEntity) {
            // Icono según el servicio
            tvServiceIcon.text = getServiceIcon(purchase.serviceId)
            tvServiceName.text = purchase.serviceName
            tvServicePrice.text = purchase.servicePrice
            tvDuration.text = purchase.serviceDuration

            // Formatear fechas
            val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale("es", "ES"))
            tvPurchaseDate.text = dateFormat.format(Date(purchase.purchaseDate))
            tvExpirationDate.text = dateFormat.format(Date(purchase.expirationDate))

            // Estado
            when (purchase.status) {
                "active" -> {
                    tvStatusBadge.text = "✅ Activo"
                    tvStatusBadge.setBackgroundResource(R.drawable.badge_active)
                    credentialsContainer.visibility = View.VISIBLE
                    tvPendingMessage.visibility = View.GONE

                    // Mostrar credenciales
                    tvCredentialEmail.text = purchase.email ?: "No disponible"
                    tvCredentialPassword.text = purchase.password ?: "••••••••"

                    // Botones de copiar
                    btnCopyEmail.setOnClickListener {
                        copyToClipboard(itemView.context, "Email", purchase.email ?: "")
                    }
                    btnCopyPassword.setOnClickListener {
                        copyToClipboard(itemView.context, "Contraseña", purchase.password ?: "")
                    }
                }
                "pending" -> {
                    tvStatusBadge.text = "⏳ Pendiente"
                    tvStatusBadge.setBackgroundResource(R.drawable.badge_pending)
                    credentialsContainer.visibility = View.GONE
                    tvPendingMessage.visibility = View.VISIBLE
                }
                "expired" -> {
                    tvStatusBadge.text = "❌ Expirado"
                    tvStatusBadge.setBackgroundResource(R.drawable.badge_expired)
                    credentialsContainer.visibility = View.GONE
                    tvPendingMessage.visibility = View.GONE
                }
            }

            itemView.setOnClickListener {
                onPurchaseClick(purchase)
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

        private fun copyToClipboard(context: Context, label: String, text: String) {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText(label, text)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(context, "$label copiado", Toast.LENGTH_SHORT).show()
        }
    }
}