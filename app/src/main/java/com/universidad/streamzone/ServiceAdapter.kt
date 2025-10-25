package com.universidad.streamzone

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

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
        private val tvBadge: TextView = view.findViewById(R.id.tvBadge)
        val btnReserve: Button = view.findViewById(R.id.btnReserve)

        fun bind(s: Service) {
            // popular textos
            tvTitle.text = s.title
            tvPrice.text = s.price
            tvQuickAccess.text = s.desc

            // Intentar usar un drawable `logo_<id>` si existe en res/drawable (PNG que conservaste)
            val ctx = itemView.context
            val logoResName = "logo_${s.id}"
            val logoResId = ctx.resources.getIdentifier(logoResName, "drawable", ctx.packageName)
            if (logoResId != 0) {
                // usar el PNG como background del tvIcon (no requiere cambiar layout)
                tvIcon.text = ""
                tvIcon.setBackgroundResource(logoResId)
            } else if (s.iconRes != null) {
                // Si se proporcionó un drawable redondeado en el modelo, usarlo
                tvIcon.text = ""
                tvIcon.setBackgroundResource(s.iconRes)
            } else {
                // elegir un icono/emoji según el id del servicio para mantener consistencia con la versión web
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
                // restablecer background por si antes hubo uno
                tvIcon.setBackgroundResource(R.drawable.rounded_square)
            }

            // badge simple
            tvBadge.visibility = View.VISIBLE
        }
    }
}
