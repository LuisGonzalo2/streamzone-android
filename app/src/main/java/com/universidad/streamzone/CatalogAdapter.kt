package com.universidad.streamzone

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.universidad.streamzone.model.CatalogItem

class CatalogAdapter(
    private val items: List<CatalogItem>,
    private val onBuyClick: (CatalogItem) -> Unit
) : RecyclerView.Adapter<CatalogAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imgIcon: ImageView = view.findViewById(R.id.imgIcon)
        val txtTitle: TextView = view.findViewById(R.id.txtItemTitle)
        val txtPrice: TextView = view.findViewById(R.id.txtItemPrice)
        val txtDesc: TextView = view.findViewById(R.id.txtItemDesc)
        val btnBuy: Button = view.findViewById(R.id.btnComprar)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_catalog_card, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.txtTitle.text = item.title
        holder.txtPrice.text = item.price
        holder.txtDesc.text = item.shortDesc
        if (item.drawableRes != null) {
            holder.imgIcon.setImageResource(item.drawableRes)
        } else {
            holder.imgIcon.setImageResource(R.mipmap.ic_launcher)
        }

        holder.btnBuy.setOnClickListener {
            onBuyClick(item)
        }
    }

    override fun getItemCount(): Int = items.size
}

