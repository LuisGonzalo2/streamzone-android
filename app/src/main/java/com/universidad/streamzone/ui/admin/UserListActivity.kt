package com.universidad.streamzone.ui.admin

import android.content.Intent
import android.os.Bundle
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.universidad.streamzone.R
import com.universidad.streamzone.data.local.database.AppDatabase
import com.universidad.streamzone.data.model.UsuarioEntity
import com.universidad.streamzone.ui.admin.adapter.UserAdapter
import com.universidad.streamzone.util.PermissionManager
import kotlinx.coroutines.launch

class UserListActivity : BaseAdminActivity() {

    override val requiredPermission: String = PermissionManager.MANAGE_USERS

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
                val intent = Intent(this, RolesManagerActivity::class.java)
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
                val db = AppDatabase.getInstance(this@UserListActivity)
                val usuarioDao = db.usuarioDao()

                val users = usuarioDao.getAll()
                val adminCount = users.count { it.isAdmin }

                runOnUiThread {
                    tvTotalUsers.text = users.size.toString()
                    tvAdminUsers.text = adminCount.toString()
                    userAdapter.updateUsers(users)
                }

            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(
                        this@UserListActivity,
                        "Error al cargar usuarios: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
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
                val db = AppDatabase.getInstance(this@UserListActivity)
                val usuarioDao = db.usuarioDao()

                val updatedUser = user.copy(isAdmin = isAdmin)
                usuarioDao.actualizar(updatedUser)

                runOnUiThread {
                    val msg = if (isAdmin) "Admin activado" else "Admin desactivado"
                    Toast.makeText(this@UserListActivity, msg, Toast.LENGTH_SHORT).show()
                    loadUsers()
                }

            } catch (e: Exception) {
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