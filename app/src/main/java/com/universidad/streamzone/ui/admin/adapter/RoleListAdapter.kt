package com.universidad.streamzone.ui.admin.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.universidad.streamzone.R
import com.universidad.streamzone.data.model.RoleEntity

class RoleListAdapter(
    private var roles: List<RoleEntity>,
    private val onEditClick: (RoleEntity) -> Unit,
    private val onDeleteClick: (RoleEntity) -> Unit
) : RecyclerView.Adapter<RoleListAdapter.RoleViewHolder>() {

    class RoleViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvRoleName: TextView = view.findViewById(R.id.tvRoleName)
        val tvRoleDescription: TextView = view.findViewById(R.id.tvRoleDescription)
        val tvRoleStatus: TextView = view.findViewById(R.id.tvRoleStatus)
        val btnEditRole: Button = view.findViewById(R.id.btnEditRole)
        val btnDeleteRole: Button = view.findViewById(R.id.btnDeleteRole)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RoleViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_role, parent, false)
        return RoleViewHolder(view)
    }

    override fun onBindViewHolder(holder: RoleViewHolder, position: Int) {
        val role = roles[position]

        holder.tvRoleName.text = role.name
        holder.tvRoleDescription.text = role.description

        // Estado del rol
        if (role.isActive) {
            holder.tvRoleStatus.text = "✓ Activo"
            holder.tvRoleStatus.setTextColor(0xFF10B981.toInt())
        } else {
            holder.tvRoleStatus.text = "✗ Inactivo"
            holder.tvRoleStatus.setTextColor(0xFFEF4444.toInt())
        }

        holder.btnEditRole.setOnClickListener {
            onEditClick(role)
        }

        holder.btnDeleteRole.setOnClickListener {
            onDeleteClick(role)
        }
    }

    override fun getItemCount(): Int = roles.size

    fun updateRoles(newRoles: List<RoleEntity>) {
        roles = newRoles
        notifyDataSetChanged()
    }
}