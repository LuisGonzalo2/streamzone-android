package com.universidad.streamzone.ui.admin.users
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.universidad.streamzone.R
import com.universidad.streamzone.data.model.UsuarioEntity
import com.universidad.streamzone.utils.ImageUtils

class UserAdapter : ListAdapter<UsuarioEntity, UserAdapter.UserViewHolder>(UserDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_user_admin, parent, false)
        return UserViewHolder(view)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        val user = getItem(position)
        holder.bind(user)
    }

    class UserViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivUserProfile: ImageView = itemView.findViewById(R.id.ivUserProfile)
        private val tvUserName: TextView = itemView.findViewById(R.id.tvUserName)
        private val tvUserEmail: TextView = itemView.findViewById(R.id.tvUserEmail)
        private val btnUserOptions: MaterialButton = itemView.findViewById(R.id.btnUserOptions)
        fun bind(user: UsuarioEntity) {
            tvUserName.text = user.fullname
            tvUserEmail.text = user.email

            val bitmap = ImageUtils.convertirBase64ABitmap(user.fotoBase64)
            if (bitmap != null) {
                ivUserProfile.setImageBitmap(bitmap)
            } else {
                ivUserProfile.setImageResource(R.drawable.ic_person_placeholder)
            }

            btnUserOptions.setOnClickListener {
                Toast.makeText(
                    itemView.context,
                    "Funciona para el usuario: ${user.fullname}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }


    class UserDiffCallback : DiffUtil.ItemCallback<UsuarioEntity>() {
        override fun areItemsTheSame(oldItem: UsuarioEntity, newItem: UsuarioEntity): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: UsuarioEntity, newItem: UsuarioEntity): Boolean {
            return oldItem == newItem
        }
    }
}
