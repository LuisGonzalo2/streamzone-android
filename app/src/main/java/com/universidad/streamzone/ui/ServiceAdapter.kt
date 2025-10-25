package com.universidad.streamzone.ui

import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import androidx.core.graphics.toColorInt
import com.universidad.streamzone.model.ServiceItem
import com.universidad.streamzone.R

class ServiceAdapter(
    private val items: List<ServiceItem>,
    private val onBuyClick: (ServiceItem) -> Unit
) : RecyclerView.Adapter<ServiceAdapter.VH>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_service_card, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        holder.bind(item)
    }

    override fun getItemCount(): Int = items.size

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        private val tvIcon: TextView = view.findViewById(R.id.tvServiceIcon)
        private val tvTitle: TextView = view.findViewById(R.id.tvServiceTitle)
        private val tvPrice: TextView = view.findViewById(R.id.tvServicePrice)
        private val tvQuick: TextView = view.findViewById(R.id.tvQuickAccess)
        private val tvBadge: TextView = view.findViewById(R.id.tvBadge)
        private val btnBuy: Button = view.findViewById(R.id.btnReserve)

        fun bind(item: ServiceItem) {
            val ctx = itemView.context

            // Title and price
            tvTitle.text = item.title
            tvPrice.text = item.priceText

            // Badge visibility
            tvBadge.visibility = if (item.showBadge) View.VISIBLE else View.GONE

            // Quick access visibility (oculto para licencias anuales)
            tvQuick.visibility = if (item.isAnnual) View.GONE else View.VISIBLE

            // Icon text or resource
            if (item.iconRes != null) {
                tvIcon.text = ""
                tvIcon.setBackgroundResource(item.iconRes)
            } else {
                tvIcon.text = item.iconText ?: ""

                // Create background programmatically to apply the requested color(s)
                val bg = when (item.title) {
                    "Deezer (DZ)" -> { // special multicolor gradient
                        val colors = intArrayOf(
                            0xFFE53935.toInt(), // red
                            0xFF1E88E5.toInt(), // blue
                            0xFFFFEB3B.toInt()  // yellow
                        )
                        GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, colors)
                    }
                    else -> {
                        val color = try {
                            item.colorHex.toColorInt()
                        } catch (_: Exception) {
                            "#333333".toColorInt()
                        }
                        val g = GradientDrawable()
                        g.setColor(color)
                        g
                    }
                }

                // Corner radius similar to rounded_square (12dp)
                val scale = ctx.resources.displayMetrics.density
                bg.cornerRadius = 12f * scale
                tvIcon.background = bg
            }

            // Buy button listener
            btnBuy.setOnClickListener { onBuyClick(item) }
        }
    }
}
