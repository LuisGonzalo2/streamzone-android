package com.universidad.streamzone.ui.admin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.universidad.streamzone.R
import com.universidad.streamzone.data.local.database.AppDatabase
import com.universidad.streamzone.data.model.RoleEntity
import com.universidad.streamzone.data.model.UserRoleEntity
import com.universidad.streamzone.util.PermissionManager
import kotlinx.coroutines.launch

class RolesManagerActivity : BaseAdminActivity() {

    override val requiredPermission: String = PermissionManager.MANAGE_ROLES

    private lateinit var btnBack: ImageButton
    private lateinit var tvUserInfo: TextView
    private lateinit var rvRoles: RecyclerView
    private lateinit var btnSaveRoles: Button

    private var userId: Int = 0
    private var userName: String = ""
    private val selectedRoles = mutableSetOf<Int>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_roles_manager)

        userId = intent.getIntExtra("USER_ID", 0)
        userName = intent.getStringExtra("USER_NAME") ?: ""
    }

    override fun onPermissionGranted() {
        initViews()
        loadRoles()
    }

    private fun initViews() {
        btnBack = findViewById(R.id.btnBack)
        tvUserInfo = findViewById(R.id.tvUserInfo)
        rvRoles = findViewById(R.id.rvRoles)
        btnSaveRoles = findViewById(R.id.btnSaveRoles)

        tvUserInfo.text = "Asignar roles a: $userName"

        btnBack.setOnClickListener { finish() }
        btnSaveRoles.setOnClickListener { saveRoles() }
    }

    private fun loadRoles() {
        lifecycleScope.launch {
            val db = AppDatabase.getInstance(this@RolesManagerActivity)
            val roleDao = db.roleDao()
            val userRoleDao = db.userRoleDao()

            val allRoles = roleDao.getAll()
            val userRoleIds = userRoleDao.getRolesByUserId(userId)

            selectedRoles.clear()
            selectedRoles.addAll(userRoleIds.map { it.toInt() })

            runOnUiThread {
                rvRoles.layoutManager = LinearLayoutManager(this@RolesManagerActivity)
                rvRoles.adapter = RoleAdapter(allRoles, selectedRoles)
            }
        }
    }

    private fun saveRoles() {
        lifecycleScope.launch {
            try {
                val db = AppDatabase.getInstance(this@RolesManagerActivity)
                val userRoleDao = db.userRoleDao()

                // Eliminar roles anteriores
                userRoleDao.eliminarRolesPorUsuario(userId)

                // Insertar nuevos roles
                val userRoles = selectedRoles.map { roleId ->
                    UserRoleEntity(userId = userId, roleId = roleId)
                }
                userRoleDao.asignarRoles(userRoles)

                runOnUiThread {
                    Toast.makeText(this@RolesManagerActivity, "Roles actualizados", Toast.LENGTH_SHORT).show()
                    finish()
                }
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this@RolesManagerActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    inner class RoleAdapter(
        private val roles: List<RoleEntity>,
        private val selectedRoles: MutableSet<Int>
    ) : RecyclerView.Adapter<RoleAdapter.RoleViewHolder>() {

        inner class RoleViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val checkBox: CheckBox = view.findViewById(R.id.cbRole)
            val tvDescription: TextView = view.findViewById(R.id.tvRoleDescription)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RoleViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_role_checkbox, parent, false)
            return RoleViewHolder(view)
        }

        override fun onBindViewHolder(holder: RoleViewHolder, position: Int) {
            val role = roles[position]
            holder.checkBox.text = role.name
            holder.tvDescription.text = role.description
            holder.checkBox.isChecked = selectedRoles.contains(role.id)

            holder.checkBox.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    selectedRoles.add(role.id)
                } else {
                    selectedRoles.remove(role.id)
                }
            }
        }

        override fun getItemCount() = roles.size
    }
}