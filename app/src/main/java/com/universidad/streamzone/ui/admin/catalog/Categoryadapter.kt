package com.universidad.streamzone.ui.admin.catalog

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.switchmaterial.SwitchMaterial
import com.universidad.streamzone.R
import com.universidad.streamzone.data.model.CategoryEntity

class CategoryAdapter(
    private val onEdit: (CategoryEntity) -> Unit,
    private val onDelete: (CategoryEntity) -> Unit,
    private val onToggleActive: (CategoryEntity) -> Unit
) : ListAdapter<CategoryEntity, CategoryAdapter.CategoryViewHolder>(CategoryDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_category_admin, parent, false)
        return CategoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        holder.bind(getItem(position), onEdit, onDelete, onToggleActive)
    }

    class CategoryViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val container: LinearLayout = view.findViewById(R.id.category_preview_container)
        private val tvIcon: TextView = view.findViewById(R.id.tv_category_icon_preview)
        private val tvName: TextView = view.findViewById(R.id.tv_category_name_admin)
        private val tvDescription: TextView = view.findViewById(R.id.tv_category_description_admin)
        private val tvServiceCount: TextView = view.findViewById(R.id.tv_category_service_count)
        private val switchActive: SwitchMaterial = view.findViewById(R.id.switch_category_active)
        private val btnEdit: MaterialButton = view.findViewById(R.id.btn_edit_category)
        private val btnDelete: ImageButton = view.findViewById(R.id.btn_delete_category)

        fun bind(
            category: CategoryEntity,
            onEdit: (CategoryEntity) -> Unit,
            onDelete: (CategoryEntity) -> Unit,
            onToggleActive: (CategoryEntity) -> Unit
        ) {
            tvIcon.text = category.icon
            tvName.text = category.name
            tvDescription.text = category.description
            tvServiceCount.text = "0 servicios" // TODO: contar servicios reales

            // Aplicar gradiente
            try {
                val startColor = Color.parseColor(category.gradientStart)
                val endColor = Color.parseColor(category.gradientEnd)

                val gradientDrawable = GradientDrawable(
                    GradientDrawable.Orientation.TL_BR,
                    intArrayOf(startColor, endColor)
                )
                gradientDrawable.cornerRadius = 16f * itemView.context.resources.displayMetrics.density
                container.background = gradientDrawable
            } catch (e: Exception) {
                // Fallback color si el parse falla
                container.setBackgroundColor(ContextCompat.getColor(itemView.context, R.color.dark_card_background))
            }

            // Switch estado
            switchActive.isChecked = category.isActive
            switchActive.setOnCheckedChangeListener { _, _ ->
                onToggleActive(category)
            }

            // Botones
            btnEdit.setOnClickListener { onEdit(category) }
            btnDelete.setOnClickListener { onDelete(category) }

            // Opacidad si está inactiva
            itemView.alpha = if (category.isActive) 1.0f else 0.5f
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