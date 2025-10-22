package com.universidad.streamzone

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.universidad.streamzone.model.UsuarioEntity

class UsuarioAdapter : ListAdapter<UsuarioEntity, UsuarioAdapter.VH>(Diff()) {
    class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val txtNombre: TextView = itemView.findViewById(R.id.txtItemNombre)
        val txtDetalle: TextView = itemView.findViewById(R.id.txtItemDetalle)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_usuario, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val u = getItem(position)
        holder.txtNombre . text = u.fullname
        holder.txtDetalle.text =
            "email: ${u.email} | phone: ${u.phone} | Password: ${u.password } | ConfirPassword: ${u.confirmPassword}"
    }

    private class Diff : DiffUtil.ItemCallback<UsuarioEntity>() {
        override fun areItemsTheSame(oldItem: UsuarioEntity, newItem: UsuarioEntity) =
            oldItem.id == newItem.id
        override fun areContentsTheSame(
            oldItem: UsuarioEntity,
            newItem: UsuarioEntity
        ) = oldItem == newItem
    }
}