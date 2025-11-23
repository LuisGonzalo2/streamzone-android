package com.universidad.streamzone.ui.admin

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.universidad.streamzone.R
import com.universidad.streamzone.data.firebase.repository.RoleRepository
import com.universidad.streamzone.data.model.RoleEntity
import com.universidad.streamzone.ui.admin.adapter.RoleListAdapter
import com.universidad.streamzone.util.PermissionManager
import com.universidad.streamzone.util.toRoleEntityList
import kotlinx.coroutines.launch

/**
 * Activity para listar y gestionar roles del sistema
 */
class RoleListActivity : BaseAdminActivity() {

    override val requiredPermission: String = PermissionManager.MANAGE_ROLES

    // Firebase Repository
    private val roleRepository = RoleRepository()

    private lateinit var btnBack: ImageButton
    private lateinit var rvRoles: RecyclerView
    private lateinit var llEmptyState: LinearLayout
    private lateinit var fabAddRole: FloatingActionButton
    private lateinit var roleAdapter: RoleListAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d("RoleList", "🎯 onCreate() - Iniciando RoleListActivity")
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_role_list)
        Log.d("RoleList", "✅ onCreate() - ContentView establecido")
    }

    override fun onPermissionGranted() {
        Log.d("RoleList", "✅ onPermissionGranted() - Permisos concedidos")
        initViews()
        setupRecyclerView()
        loadRoles()
    }

    private fun initViews() {
        btnBack = findViewById(R.id.btnBack)
        rvRoles = findViewById(R.id.rvRoles)
        llEmptyState = findViewById(R.id.llEmptyState)
        fabAddRole = findViewById(R.id.fabAddRole)

        btnBack.setOnClickListener { finish() }
        fabAddRole.setOnClickListener {
            val intent = Intent(this, CreateEditRoleActivity::class.java)
            startActivity(intent)
        }
    }

    private fun setupRecyclerView() {
        roleAdapter = RoleListAdapter(
            roles = emptyList(),
            onEditClick = { role ->
                val intent = Intent(this, CreateEditRoleActivity::class.java)
                intent.putExtra("ROLE_ID", role.firebaseId ?: "")
                startActivity(intent)
            },
            onDeleteClick = { role ->
                showDeleteConfirmation(role)
            }
        )

        rvRoles.layoutManager = LinearLayoutManager(this)
        rvRoles.adapter = roleAdapter
    }

    override fun onResume() {
        super.onResume()
        Log.d("RoleList", "🔄 onResume() - Actividad resumida")
        loadRoles()
    }

    private fun loadRoles() {
        lifecycleScope.launch {
            try {
                // Obtener roles directamente desde Firebase
                val firebaseRoles = roleRepository.getActiveRoles()

                // Convertir a RoleEntity para la UI
                val roleEntities = firebaseRoles.toRoleEntityList()

                Log.d("RoleList", "✅ Roles cargados: ${roleEntities.size}")

                runOnUiThread {
                    if (roleEntities.isEmpty()) {
                        rvRoles.visibility = View.GONE
                        llEmptyState.visibility = View.VISIBLE
                    } else {
                        rvRoles.visibility = View.VISIBLE
                        llEmptyState.visibility = View.GONE
                        roleAdapter.updateRoles(roleEntities)
                    }
                }

            } catch (e: Exception) {
                Log.e("RoleList", "❌ Error al cargar roles", e)
                runOnUiThread {
                    Toast.makeText(
                        this@RoleListActivity,
                        "Error al cargar roles: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun showDeleteConfirmation(role: RoleEntity) {
        AlertDialog.Builder(this)
            .setTitle("Eliminar Rol")
            .setMessage("¿Estás seguro de eliminar el rol '${role.name}'? Esta acción no se puede deshacer.")
            .setPositiveButton("Eliminar") { _, _ ->
                deleteRole(role)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun deleteRole(role: RoleEntity) {
        lifecycleScope.launch {
            try {
                // Eliminar directamente desde Firebase
                if (role.firebaseId != null) {
                    roleRepository.delete(role.firebaseId!!)

                    Log.d("RoleList", "✅ Rol eliminado de Firebase")

                    runOnUiThread {
                        Toast.makeText(
                            this@RoleListActivity,
                            "Rol eliminado correctamente",
                            Toast.LENGTH_SHORT
                        ).show()
                        loadRoles()
                    }
                }

            } catch (e: Exception) {
                Log.e("RoleList", "❌ Error al eliminar rol", e)
                runOnUiThread {
                    Toast.makeText(
                        this@RoleListActivity,
                        "Error al eliminar rol: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }
}