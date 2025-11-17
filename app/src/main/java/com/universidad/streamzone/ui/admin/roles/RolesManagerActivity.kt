package com.universidad.streamzone.ui.admin.roles

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.universidad.streamzone.R
import com.universidad.streamzone.data.local.database.AppDatabase
import com.universidad.streamzone.data.model.RoleEntity
import com.universidad.streamzone.ui.admin.roles.adapter.RoleAdapter
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class RolesManagerActivity : AppCompatActivity() {

    private lateinit var rvRoles: RecyclerView
    private lateinit var emptyState: LinearLayout
    private lateinit var btnBack: MaterialButton
    private lateinit var fabAddRole: FloatingActionButton
    private lateinit var adapter: RoleAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_roles_manager)

        // Configurar padding para el notch
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false)
        }

        val mainContainer = findViewById<View>(R.id.roles_manager_container)
        mainContainer?.setOnApplyWindowInsetsListener { view, insets ->
            val systemBars = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                insets.getInsets(android.view.WindowInsets.Type.systemBars())
            } else {
                @Suppress("DEPRECATION")
                android.graphics.Insets.of(0, insets.systemWindowInsetTop, 0, 0)
            }
            view.setPadding(
                view.paddingLeft,
                systemBars.top + 16,
                view.paddingRight,
                view.paddingBottom
            )
            insets
        }

        initViews()
        setupRecyclerView()
        loadRoles()
    }

    private fun initViews() {
        rvRoles = findViewById(R.id.rv_roles)
        emptyState = findViewById(R.id.empty_state_roles)
        btnBack = findViewById(R.id.btn_back)
        fabAddRole = findViewById(R.id.fab_add_role)

        btnBack.setOnClickListener {
            finish()
        }

        fabAddRole.setOnClickListener {
            openCreateRoleDialog()
        }
    }

    private fun setupRecyclerView() {
        adapter = RoleAdapter(
            onEditClick = { role -> openEditRoleDialog(role) },
            onDeleteClick = { role -> confirmDeleteRole(role) }
        )

        rvRoles.layoutManager = LinearLayoutManager(this)
        rvRoles.adapter = adapter
    }

    private fun loadRoles() {
        lifecycleScope.launch {
            try {
                val dao = AppDatabase.getInstance(this@RolesManagerActivity).roleDao()

                dao.obtenerTodos().collectLatest { roles ->
                    runOnUiThread {
                        if (roles.isEmpty()) {
                            showEmptyState()
                        } else {
                            showRoles(roles)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("RolesManager", "Error al cargar roles", e)
                runOnUiThread {
                    showEmptyState()
                    Toast.makeText(
                        this@RolesManagerActivity,
                        "Error al cargar roles: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun showRoles(roles: List<RoleEntity>) {
        rvRoles.visibility = View.VISIBLE
        emptyState.visibility = View.GONE
        adapter.submitList(roles)
    }

    private fun showEmptyState() {
        rvRoles.visibility = View.GONE
        emptyState.visibility = View.VISIBLE
    }

    private fun openCreateRoleDialog() {
        val dialog = RoleFormDialogFragment.newInstance()
        dialog.show(supportFragmentManager, "createRole")
    }

    private fun openEditRoleDialog(role: RoleEntity) {
        val dialog = RoleFormDialogFragment.newInstance(role)
        dialog.show(supportFragmentManager, "editRole")
    }

    private fun confirmDeleteRole(role: RoleEntity) {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Eliminar Rol")
            .setMessage("¿Estás seguro de eliminar el rol '${role.name}'?")
            .setPositiveButton("Eliminar") { _, _ ->
                deleteRole(role)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun deleteRole(role: RoleEntity) {
        lifecycleScope.launch {
            try {
                val dao = AppDatabase.getInstance(this@RolesManagerActivity).roleDao()
                dao.eliminar(role)

                runOnUiThread {
                    Toast.makeText(
                        this@RolesManagerActivity,
                        "✅ Rol eliminado",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                Log.e("RolesManager", "Error al eliminar rol", e)
                runOnUiThread {
                    Toast.makeText(
                        this@RolesManagerActivity,
                        "Error: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }
}