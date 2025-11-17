package com.universidad.streamzone.ui.admin.roles.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.universidad.streamzone.R
import com.universidad.streamzone.data.model.RoleEntity

class RoleAdapter(
    private val onEditClick: (RoleEntity) -> Unit,
    private val onDeleteClick: (RoleEntity) -> Unit
) : ListAdapter<RoleEntity, RoleAdapter.RoleViewHolder>(RoleDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RoleViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_role, parent, false)
        return RoleViewHolder(view, onEditClick, onDeleteClick)
    }

    override fun onBindViewHolder(holder: RoleViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class RoleViewHolder(
        itemView: View,
        private val onEditClick: (RoleEntity) -> Unit,
        private val onDeleteClick: (RoleEntity) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {

        private val tvRoleName: TextView = itemView.findViewById(R.id.tv_role_name)
        private val tvRoleDescription: TextView = itemView.findViewById(R.id.tv_role_description)
        private val tvRoleStatus: TextView = itemView.findViewById(R.id.tv_role_status)
        private val btnEdit: ImageButton = itemView.findViewById(R.id.btn_edit_role)
        private val btnDelete: ImageButton = itemView.findViewById(R.id.btn_delete_role)

        fun bind(role: RoleEntity) {
            tvRoleName.text = role.name
            tvRoleDescription.text = role.description

            // Estado
            if (role.isActive) {
                tvRoleStatus.text = "✅ Activo"
                tvRoleStatus.setTextColor(itemView.context.getColor(R.color.brand_blue))
            } else {
                tvRoleStatus.text = "❌ Inactivo"
                tvRoleStatus.setTextColor(itemView.context.getColor(R.color.text_secondary))
            }

            btnEdit.setOnClickListener {
                onEditClick(role)
            }

            btnDelete.setOnClickListener {
                onDeleteClick(role)
            }
        }
    }

    class RoleDiffCallback : DiffUtil.ItemCallback<RoleEntity>() {
        override fun areItemsTheSame(oldItem: RoleEntity, newItem: RoleEntity): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: RoleEntity, newItem: RoleEntity): Boolean {
            return oldItem == newItem
        }
    }
}