package com.universidad.streamzone.ui.home.adapter

import android.graphics.BitmapFactory
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.universidad.streamzone.R
import com.universidad.streamzone.data.model.Service

class ServiceAdapter(
    private val items: List<Service>,
    private val onReserve: (Service) -> Unit
) : RecyclerView.Adapter<ServiceAdapter.ServiceViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ServiceViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_service_card, parent, false)
        return ServiceViewHolder(view)
    }

    override fun onBindViewHolder(holder: ServiceViewHolder, position: Int) {
        val s = items[position]
        holder.bind(s)
        holder.btnReserve.setOnClickListener { onReserve(s) }
    }

    override fun getItemCount() = items.size

    class ServiceViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val tvTitle: TextView = view.findViewById(R.id.tvServiceTitle)
        private val tvPrice: TextView = view.findViewById(R.id.tvServicePrice)
        private val tvQuickAccess: TextView = view.findViewById(R.id.tvQuickAccess)
        private val tvIcon: TextView = view.findViewById(R.id.tvServiceIcon)
        private val ivIcon: ImageView = view.findViewById(R.id.ivServiceIcon)
        private val tvBadge: TextView = view.findViewById(R.id.tvBadge)
        val btnReserve: Button = view.findViewById(R.id.btnReserve)

        fun bind(s: Service) {
            // popular textos
            tvTitle.text = s.title
            tvPrice.text = s.price
            tvQuickAccess.text = s.desc

            // PRIORIDAD 1: Usar imagen base64 si existe
            if (!s.iconBase64.isNullOrEmpty()) {
                try {
                    val decodedBytes = Base64.decode(s.iconBase64, Base64.DEFAULT)
                    val bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                    ivIcon.setImageBitmap(bitmap)
                    ivIcon.visibility = View.VISIBLE
                    tvIcon.visibility = View.GONE
                } catch (e: Exception) {
                    // Si falla la decodificación, usar fallback
                    ivIcon.visibility = View.GONE
                    tvIcon.visibility = View.VISIBLE
                    loadFallbackIcon(s)
                }
            } else {
                // Si no hay base64, usar fallback
                ivIcon.visibility = View.GONE
                tvIcon.visibility = View.VISIBLE
                loadFallbackIcon(s)
            }

            // badge simple
            tvBadge.visibility = View.VISIBLE
        }

        private fun loadFallbackIcon(s: Service) {
            // Intentar usar un drawable `logo_<id>` si existe en res/drawable
            val ctx = itemView.context
            val logoResName = "logo_${s.id}"
            val logoResId = ctx.resources.getIdentifier(logoResName, "drawable", ctx.packageName)
            if (logoResId != 0) {
                tvIcon.text = ""
                tvIcon.setBackgroundResource(logoResId)
            } else if (s.iconRes != null) {
                // Si se proporcionó un drawable redondeado en el modelo, usarlo
                tvIcon.text = ""
                tvIcon.setBackgroundResource(s.iconRes)
            } else {
                // elegir un icono/emoji según el id del servicio
                val icon = when (s.id) {
                    "netflix" -> "🎬"
                    "disney_plus_premium" -> "🎧"
                    "disney_plus_standard" -> "🎧"
                    "max" -> "📺"
                    "vix" -> "🎞️"
                    "prime" -> "💠"
                    "youtube_premium" -> "🎶"
                    "paramount" -> "⭐"
                    else -> s.title.take(1).uppercase()
                }
                tvIcon.text = icon
                tvIcon.setBackgroundResource(R.drawable.rounded_square)
            }
        }
    }
}