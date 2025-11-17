package com.universidad.streamzone.ui.admin.catalog.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.universidad.streamzone.R
import com.universidad.streamzone.data.model.CategoryEntity

class CategoryAdapter(
    private val onEditClick: (CategoryEntity) -> Unit,
    private val onDeleteClick: (CategoryEntity) -> Unit
) : ListAdapter<CategoryEntity, CategoryAdapter.CategoryViewHolder>(CategoryDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_category_admin, parent, false)
        return CategoryViewHolder(view, onEditClick, onDeleteClick)
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class CategoryViewHolder(
        itemView: View,
        private val onEditClick: (CategoryEntity) -> Unit,
        private val onDeleteClick: (CategoryEntity) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {

        private val categoryContainer: LinearLayout = itemView.findViewById(R.id.category_container)
        private val tvCategoryIcon: TextView = itemView.findViewById(R.id.tv_category_icon)
        private val tvCategoryName: TextView = itemView.findViewById(R.id.tv_category_name)
        private val tvCategoryId: TextView = itemView.findViewById(R.id.tv_category_id)
        private val tvCategoryStatus: TextView = itemView.findViewById(R.id.tv_category_status)
        private val btnEdit: ImageButton = itemView.findViewById(R.id.btn_edit_category)
        private val btnDelete: ImageButton = itemView.findViewById(R.id.btn_delete_category)

        fun bind(category: CategoryEntity) {
            tvCategoryIcon.text = category.icon
            tvCategoryName.text = category.name
            tvCategoryId.text = "ID: ${category.categoryId}"

            // Estado
            if (category.isActive) {
                tvCategoryStatus.text = "✅ Activa"
                tvCategoryStatus.setTextColor(itemView.context.getColor(R.color.brand_blue))
            } else {
                tvCategoryStatus.text = "❌ Inactiva"
                tvCategoryStatus.setTextColor(itemView.context.getColor(R.color.text_secondary))
            }

            // Aplicar gradiente de fondo
            try {
                val gradientDrawable = android.graphics.drawable.GradientDrawable(
                    android.graphics.drawable.GradientDrawable.Orientation.TL_BR,
                    intArrayOf(
                        Color.parseColor(category.gradientStart),
                        Color.parseColor(category.gradientEnd)
                    )
                )
                gradientDrawable.cornerRadius = 20f * itemView.context.resources.displayMetrics.density
                categoryContainer.background = gradientDrawable
            } catch (e: Exception) {
                // Si falla el color, usar un gradiente por defecto
            }

            btnEdit.setOnClickListener {
                onEditClick(category)
            }

            btnDelete.setOnClickListener {
                onDeleteClick(category)
            }
        }
    }

    class CategoryDiffCallback : DiffUtil.ItemCallback<CategoryEntity>() {
        override fun areItemsTheSame(oldItem: CategoryEntity, newItem: CategoryEntity): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: CategoryEntity, newItem: CategoryEntity): Boolean {
            return oldItem == newItem
        }
    }
}