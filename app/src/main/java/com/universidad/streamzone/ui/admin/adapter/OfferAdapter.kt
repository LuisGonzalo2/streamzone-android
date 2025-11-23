package com.universidad.streamzone.ui.admin.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.universidad.streamzone.R
import com.universidad.streamzone.data.model.OfferEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class OfferAdapter(
    private var offers: List<OfferEntity>,
    private val onEditClick: (OfferEntity) -> Unit,
    private val onDeleteClick: (OfferEntity) -> Unit
) : RecyclerView.Adapter<OfferAdapter.OfferViewHolder>() {

    class OfferViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvOfferTitle: TextView = view.findViewById(R.id.tvOfferTitle)
        val tvOfferStatus: TextView = view.findViewById(R.id.tvOfferStatus)
        val tvOfferPrice: TextView = view.findViewById(R.id.tvOfferPrice)
        val tvOfferDates: TextView = view.findViewById(R.id.tvOfferDates)
        val btnEditOffer: Button = view.findViewById(R.id.btnEditOffer)
        val btnDeleteOffer: Button = view.findViewById(R.id.btnDeleteOffer)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OfferViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_offer, parent, false)
        return OfferViewHolder(view)
    }

    override fun onBindViewHolder(holder: OfferViewHolder, position: Int) {
        val offer = offers[position]
        val context = holder.itemView.context

        // Título
        holder.tvOfferTitle.text = offer.title

        // Estado
        val now = System.currentTimeMillis()
        val isActive = offer.isActive && now >= offer.startDate && now <= offer.endDate
        val isExpired = now > offer.endDate

        when {
            isExpired -> {
                holder.tvOfferStatus.text = "❌ Expirada"
                holder.tvOfferStatus.setTextColor(context.getColor(R.color.white))
                holder.tvOfferStatus.setBackgroundColor(context.getColor(android.R.color.holo_red_dark))
            }
            !offer.isActive -> {
                holder.tvOfferStatus.text = "⏸️ Inactiva"
                holder.tvOfferStatus.setTextColor(context.getColor(R.color.white))
                holder.tvOfferStatus.setBackgroundColor(context.getColor(android.R.color.darker_gray))
            }
            isActive -> {
                holder.tvOfferStatus.text = "✅ Activa"
                holder.tvOfferStatus.setTextColor(0xFF10B981.toInt())
                holder.tvOfferStatus.setBackgroundColor(0x2010B981)
            }
        }

        // Precio
        holder.tvOfferPrice.text = "US$ %.2f (%d%% OFF)".format(
            offer.comboPrice,
            offer.discountPercent
        )

        // Vigencia
        val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale("es", "ES"))
        val endDate = Date(offer.endDate)
        holder.tvOfferDates.text = "Válida hasta: ${dateFormat.format(endDate)}"

        // Botones
        holder.btnEditOffer.setOnClickListener {
            onEditClick(offer)
        }

        holder.btnDeleteOffer.setOnClickListener {
            onDeleteClick(offer)
        }
    }

    override fun getItemCount(): Int = offers.size

    fun updateOffers(newOffers: List<OfferEntity>) {
        offers = newOffers
        notifyDataSetChanged()
    }
}