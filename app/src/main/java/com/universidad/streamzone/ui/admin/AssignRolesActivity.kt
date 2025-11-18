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

/**
 * Activity para asignar roles a un usuario específico
 */
class AssignRolesActivity : BaseAdminActivity() {

    override val requiredPermission: String = PermissionManager.MANAGE_ROLES

    private lateinit var btnBack: ImageButton
    private lateinit var tvTitle: TextView
    private lateinit var tvUserName: TextView
    private lateinit var rvRoles: RecyclerView
    private lateinit var btnSaveRoles: Button

    private var userId: Int = -1
    private var userName: String = ""
    private var allRoles = listOf<RoleEntity>()
    private val selectedRoles = mutableSetOf<Int>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_assign_roles)

        userId = intent.getIntExtra("USER_ID", -1)
        userName = intent.getStringExtra("USER_NAME") ?: "Usuario"

        if (userId == -1) {
            Toast.makeText(this, "Error: Usuario no válido", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
    }

    override fun onPermissionGranted() {
        initViews()
        loadRoles()
        loadUserRoles()
    }

    private fun initViews() {
        btnBack = findViewById(R.id.btnBack)
        tvTitle = findViewById(R.id.tvTitle)
        tvUserName = findViewById(R.id.tvUserName)
        rvRoles = findViewById(R.id.rvRoles)
        btnSaveRoles = findViewById(R.id.btnSaveRoles)

        tvTitle.text = "Asignar Roles"
        tvUserName.text = userName

        btnBack.setOnClickListener { finish() }
        btnSaveRoles.setOnClickListener { saveRoles() }
    }

    private fun loadRoles() {
        lifecycleScope.launch {
            try {
                val db = AppDatabase.getInstance(this@AssignRolesActivity)
                val roleDao = db.roleDao()

                // Cargar solo roles activos
                allRoles = roleDao.getAll().filter { it.isActive }

                runOnUiThread {
                    rvRoles.layoutManager = LinearLayoutManager(this@AssignRolesActivity)
                    rvRoles.adapter = RoleCheckboxAdapter(allRoles, selectedRoles)
                }

            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(
                        this@AssignRolesActivity,
                        "Error al cargar roles: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun loadUserRoles() {
        lifecycleScope.launch {
            try {
                val db = AppDatabase.getInstance(this@AssignRolesActivity)
                val userRoleDao = db.userRoleDao()

                // Obtener roles actuales del usuario
                val userRoleIds = userRoleDao.getRolesByUserId(userId).map { it.toInt() }

                selectedRoles.clear()
                selectedRoles.addAll(userRoleIds)

                runOnUiThread {
                    rvRoles.adapter?.notifyDataSetChanged()
                }

            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(
                        this@AssignRolesActivity,
                        "Error al cargar roles del usuario: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun saveRoles() {
        lifecycleScope.launch {
            try {
                val db = AppDatabase.getInstance(this@AssignRolesActivity)
                val userRoleDao = db.userRoleDao()
                val usuarioDao = db.usuarioDao()

                // Eliminar todos los roles actuales del usuario
                userRoleDao.eliminarRolesPorUsuario(userId)

                // Insertar los nuevos roles seleccionados
                if (selectedRoles.isNotEmpty()) {
                    val userRoles = selectedRoles.map { roleId ->
                        UserRoleEntity(
                            userId = userId,
                            roleId = roleId
                        )
                    }
                    userRoleDao.asignarRoles(userRoles)
                }

                // Sincronizar roles a Firebase
                val usuario = usuarioDao.buscarPorId(userId)
                if (usuario != null && isNetworkAvailable()) {
                    val roleDao = db.roleDao()

                    // Convertir IDs de roles locales a firebaseIds
                    val roleFirebaseIds = selectedRoles.mapNotNull { roleId ->
                        allRoles.find { it.id == roleId }?.firebaseId
                    }

                    com.universidad.streamzone.data.remote.FirebaseService.sincronizarRolesUsuario(
                        userEmail = usuario.email,
                        roleFirebaseIds = roleFirebaseIds,
                        onSuccess = {
                            android.util.Log.d("AssignRoles", "✅ Roles sincronizados a Firebase")
                        },
                        onFailure = { e ->
                            android.util.Log.e("AssignRoles", "Error al sincronizar roles: ${e.message}")
                        }
                    )
                }

                runOnUiThread {
                    Toast.makeText(
                        this@AssignRolesActivity,
                        "Roles asignados correctamente",
                        Toast.LENGTH_SHORT
                    ).show()
                    finish()
                }

            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(
                        this@AssignRolesActivity,
                        "Error al guardar roles: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    /**
     * Verifica si hay conexión a internet
     */
    private fun isNetworkAvailable(): Boolean {
        return try {
            val connectivityManager = getSystemService(android.content.Context.CONNECTIVITY_SERVICE)
                    as android.net.ConnectivityManager
            val network = connectivityManager.activeNetwork ?: return false
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false

            capabilities.hasTransport(android.net.NetworkCapabilities.TRANSPORT_WIFI) ||
                    capabilities.hasTransport(android.net.NetworkCapabilities.TRANSPORT_CELLULAR) ||
                    capabilities.hasTransport(android.net.NetworkCapabilities.TRANSPORT_ETHERNET)
        } catch (e: Exception) {
            false
        }
    }

    inner class RoleCheckboxAdapter(
        private val roles: List<RoleEntity>,
        private val selectedRoles: MutableSet<Int>
    ) : RecyclerView.Adapter<RoleCheckboxAdapter.RoleViewHolder>() {

        inner class RoleViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val checkBox: CheckBox = view.findViewById(R.id.cbRole)
            val tvDescription: TextView = view.findViewById(R.id.tvRoleDescription)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RoleViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_role_checkbox, parent, false)
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