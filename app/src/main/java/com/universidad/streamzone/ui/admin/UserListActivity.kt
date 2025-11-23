package com.universidad.streamzone.ui.admin

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.Timestamp
import com.universidad.streamzone.R
import com.universidad.streamzone.data.firebase.repository.UserRepository
import com.universidad.streamzone.data.model.UsuarioEntity
import com.universidad.streamzone.ui.admin.adapter.UserAdapter
import com.universidad.streamzone.util.PermissionManager
import kotlinx.coroutines.launch

class UserListActivity : BaseAdminActivity() {

    override val requiredPermission: String = PermissionManager.MANAGE_USERS

    // Firebase Repository
    private val userRepository = UserRepository()

    private lateinit var btnBack: ImageButton
    private lateinit var tvTotalUsers: TextView
    private lateinit var tvAdminUsers: TextView
    private lateinit var rvUsers: RecyclerView
    private lateinit var userAdapter: UserAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_list)
    }

    override fun onPermissionGranted() {
        initViews()
        setupRecyclerView()
        loadUsers()
    }

    private fun initViews() {
        btnBack = findViewById(R.id.btnBack)
        tvTotalUsers = findViewById(R.id.tvTotalUsers)
        tvAdminUsers = findViewById(R.id.tvAdminUsers)
        rvUsers = findViewById(R.id.rvUsers)

        btnBack.setOnClickListener {
            finish()
        }
    }

    private fun setupRecyclerView() {
        userAdapter = UserAdapter(
            users = emptyList(),
            onToggleAdmin = { user ->
                toggleAdminStatus(user)
            },
            onManageRoles = { user ->
                val intent = Intent(this, AssignRolesActivity::class.java)
                intent.putExtra("USER_ID", user.id)
                intent.putExtra("USER_NAME", user.fullname)
                startActivity(intent)
            }
        )

        rvUsers.layoutManager = LinearLayoutManager(this)
        rvUsers.adapter = userAdapter
    }

    override fun onResume() {
        super.onResume()
        loadUsers()
    }

    private fun loadUsers() {
        lifecycleScope.launch {
            try {
                // Obtener usuarios directamente desde Firebase
                val firebaseUsers = userRepository.getAll()

                // Convertir a UsuarioEntity para la UI (mantener compatibilidad con el adapter)
                val userEntities = firebaseUsers.map { fbUser ->
                    UsuarioEntity(
                        id = fbUser.id.hashCode(),
                        fullname = fbUser.fullname,
                        email = fbUser.email,
                        phone = fbUser.phone,
                        password = "", // No exponer contraseña en listado
                        fotoBase64 = fbUser.photoUrl,
                        isAdmin = fbUser.isAdmin,
                        firebaseId = fbUser.id,
                        sincronizado = true
                    )
                }

                val adminCount = userEntities.count { it.isAdmin }

                runOnUiThread {
                    tvTotalUsers.text = userEntities.size.toString()
                    tvAdminUsers.text = adminCount.toString()
                    userAdapter.updateUsers(userEntities)
                }

            } catch (e: Exception) {
                Log.e("UserList", "❌ Error al cargar usuarios", e)
                runOnUiThread {
                    Toast.makeText(this@UserListActivity, "Error al cargar usuarios", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }


    private fun toggleAdminStatus(user: UsuarioEntity) {
        val newStatus = !user.isAdmin
        val message = if (newStatus) {
            "¿Dar permisos de administrador a ${user.fullname}?"
        } else {
            "¿Quitar permisos de administrador a ${user.fullname}?"
        }

        AlertDialog.Builder(this)
            .setTitle("Cambiar Permisos")
            .setMessage(message)
            .setPositiveButton("Confirmar") { _, _ ->
                updateAdminStatus(user, newStatus)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun updateAdminStatus(user: UsuarioEntity, isAdmin: Boolean) {
        lifecycleScope.launch {
            try {
                // Obtener usuario de Firebase y actualizar
                if (user.firebaseId != null) {
                    val firebaseUser = userRepository.findById(user.firebaseId!!)
                    if (firebaseUser != null) {
                        val updatedUser = firebaseUser.copy(
                            isAdmin = isAdmin,
                            updatedAt = Timestamp.now()
                        )
                        userRepository.update(updatedUser)

                        Log.d("UserList", "✅ Usuario actualizado en Firebase")

                        runOnUiThread {
                            val msg = if (isAdmin) "Admin activado" else "Admin desactivado"
                            Toast.makeText(this@UserListActivity, msg, Toast.LENGTH_SHORT).show()
                            loadUsers()
                        }
                    }
                }

            } catch (e: Exception) {
                Log.e("UserList", "❌ Error al actualizar usuario", e)
                runOnUiThread {
                    Toast.makeText(
                        this@UserListActivity,
                        "Error: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

}