package com.universidad.streamzone.ui.admin.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.universidad.streamzone.R
import com.universidad.streamzone.data.model.UsuarioEntity

class UserAdapter(
    private var users: List<UsuarioEntity>,
    private val onToggleAdmin: (UsuarioEntity) -> Unit,
    private val onManageRoles: (UsuarioEntity) -> Unit
) : RecyclerView.Adapter<UserAdapter.UserViewHolder>() {

    class UserViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvUserName: TextView = view.findViewById(R.id.tvUserName)
        val tvUserEmail: TextView = view.findViewById(R.id.tvUserEmail)
        val tvUserPhone: TextView = view.findViewById(R.id.tvUserPhone)
        val tvAdminBadge: TextView = view.findViewById(R.id.tvAdminBadge)
        val btnToggleAdmin: Button = view.findViewById(R.id.btnToggleAdmin)
        val btnManageRoles: Button = view.findViewById(R.id.btnManageRoles)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_user, parent, false)
        return UserViewHolder(view)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        val user = users[position]

        holder.tvUserName.text = user.fullname
        holder.tvUserEmail.text = user.email
        holder.tvUserPhone.text = "📞 ${user.phone}"

        if (user.isAdmin) {
            holder.tvAdminBadge.visibility = View.VISIBLE
            holder.btnToggleAdmin.text = "Quitar Admin"
            holder.btnToggleAdmin.setBackgroundColor(0xFFEF4444.toInt())
        } else {
            holder.tvAdminBadge.visibility = View.GONE
            holder.btnToggleAdmin.text = "Hacer Admin"
            holder.btnToggleAdmin.setBackgroundColor(0xFF3B82F6.toInt())
        }

        holder.btnToggleAdmin.setOnClickListener {
            onToggleAdmin(user)
        }

        holder.btnManageRoles.setOnClickListener {
            onManageRoles(user)
        }
    }

    override fun getItemCount(): Int = users.size

    fun updateUsers(newUsers: List<UsuarioEntity>) {
        users = newUsers
        notifyDataSetChanged()
    }
}
