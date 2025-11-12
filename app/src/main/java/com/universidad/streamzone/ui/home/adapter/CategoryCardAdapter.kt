package com.universidad.streamzone.ui.home.adapter

import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.universidad.streamzone.R
import com.universidad.streamzone.data.model.Category

class CategoryCardAdapter(
    private val categories: List<Category>,
    private val onCategoryClick: (Category) -> Unit
) : RecyclerView.Adapter<CategoryCardAdapter.CategoryViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_category_card_grid, parent, false)
        return CategoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        val category = categories[position]
        holder.bind(category)
        holder.itemView.setOnClickListener { onCategoryClick(category) }
    }

    override fun getItemCount() = categories.size

    class CategoryViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val container: LinearLayout = view.findViewById(R.id.category_container)
        private val icon: TextView = view.findViewById(R.id.tv_category_icon)
        private val name: TextView = view.findViewById(R.id.tv_category_name)
        private val serviceCount: TextView = view.findViewById(R.id.tv_service_count)

        fun bind(category: Category) {
            icon.text = category.icon
            name.text = category.name
            serviceCount.text = "${category.serviceCount} servicios"

            // Aplicar gradiente al fondo
            val gradientDrawable = GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                intArrayOf(
                    ContextCompat.getColor(itemView.context, category.gradientStart),
                    ContextCompat.getColor(itemView.context, category.gradientEnd)
                )
            )
            gradientDrawable.cornerRadius = 20f * itemView.context.resources.displayMetrics.density
            container.background = gradientDrawable
        }
    }
}