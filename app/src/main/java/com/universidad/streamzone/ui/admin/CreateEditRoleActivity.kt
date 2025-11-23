package com.universidad.streamzone.ui.admin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.widget.SwitchCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.Timestamp
import com.universidad.streamzone.R
import com.universidad.streamzone.data.firebase.models.Role
import com.universidad.streamzone.data.firebase.repository.PermissionRepository
import com.universidad.streamzone.data.firebase.repository.RoleRepository
import com.universidad.streamzone.data.model.PermissionEntity
import com.universidad.streamzone.util.PermissionManager
import com.universidad.streamzone.util.toPermissionEntityList
import kotlinx.coroutines.launch

/**
 * Activity para crear o editar roles del sistema
 */
class CreateEditRoleActivity : BaseAdminActivity() {

    override val requiredPermission: String = PermissionManager.MANAGE_ROLES

    private lateinit var btnBack: ImageButton
    private lateinit var tvTitle: TextView
    private lateinit var etRoleName: EditText
    private lateinit var etRoleDescription: EditText
    private lateinit var switchIsActive: SwitchCompat
    private lateinit var rvPermissions: RecyclerView
    private lateinit var btnSaveRole: Button

    private var roleId: Int? = null
    private var allPermissions = listOf<PermissionEntity>()
    private val selectedPermissions = mutableSetOf<Int>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_edit_role)

        roleId = intent.getIntExtra("ROLE_ID", -1).takeIf { it != -1 }
    }

    override fun onPermissionGranted() {
        initViews()
        loadPermissions()

        if (roleId != null) {
            tvTitle.text = "Editar Rol"
            loadRole(roleId!!)
        }
    }

    private fun initViews() {
        btnBack = findViewById(R.id.btnBack)
        tvTitle = findViewById(R.id.tvTitle)
        etRoleName = findViewById(R.id.etRoleName)
        etRoleDescription = findViewById(R.id.etRoleDescription)
        switchIsActive = findViewById(R.id.switchIsActive)
        rvPermissions = findViewById(R.id.rvPermissions)
        btnSaveRole = findViewById(R.id.btnSaveRole)

        btnBack.setOnClickListener { finish() }
        btnSaveRole.setOnClickListener { saveRole() }

        // Por defecto, los roles nuevos están activos
        switchIsActive.isChecked = true
    }

    private fun loadPermissions() {
        lifecycleScope.launch {
            try {
                val db = AppDatabase.getInstance(this@CreateEditRoleActivity)
                val permissionDao = db.permissionDao()

                allPermissions = permissionDao.getAll()

                runOnUiThread {
                    rvPermissions.layoutManager = LinearLayoutManager(this@CreateEditRoleActivity)
                    rvPermissions.adapter = PermissionAdapter(allPermissions, selectedPermissions)
                }

            } catch (e: Exception) {
                Toast.makeText(
                    this@CreateEditRoleActivity,
                    "Error al cargar permisos: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun loadRole(id: Int) {
        lifecycleScope.launch {
            try {
                val db = AppDatabase.getInstance(this@CreateEditRoleActivity)
                val roleDao = db.roleDao()
                val permissionDao = db.permissionDao()

                val role = roleDao.obtenerPorId(id)
                if (role == null) {
                    Toast.makeText(
                        this@CreateEditRoleActivity,
                        "Rol no encontrado",
                        Toast.LENGTH_SHORT
                    ).show()
                    finish()
                    return@launch
                }

                // Obtener permisos del rol
                val rolePermissions = permissionDao.obtenerPermisosPorRol(id)
                selectedPermissions.clear()
                selectedPermissions.addAll(rolePermissions.map { it.id })

                runOnUiThread {
                    etRoleName.setText(role.name)
                    etRoleDescription.setText(role.description)
                    switchIsActive.isChecked = role.isActive

                    // Actualizar adapter
                    rvPermissions.adapter?.notifyDataSetChanged()
                }

            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(
                        this@CreateEditRoleActivity,
                        "Error al cargar rol: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun saveRole() {
        val name = etRoleName.text.toString().trim()
        if (name.isEmpty()) {
            Toast.makeText(this, "El nombre del rol es obligatorio", Toast.LENGTH_SHORT).show()
            return
        }

        val description = etRoleDescription.text.toString().trim()
        if (description.isEmpty()) {
            Toast.makeText(this, "La descripción del rol es obligatoria", Toast.LENGTH_SHORT).show()
            return
        }

        if (selectedPermissions.isEmpty()) {
            Toast.makeText(this, "Selecciona al menos un permiso", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            try {
                val db = AppDatabase.getInstance(this@CreateEditRoleActivity)
                val roleDao = db.roleDao()
                val rolePermissionDao = db.rolePermissionDao()

                // Obtener firebaseId si es edición
                var firebaseId: String? = null
                if (roleId != null) {
                    val existingRole = roleDao.obtenerPorId(roleId!!)
                    firebaseId = existingRole?.firebaseId
                }

                val role = RoleEntity(
                    id = roleId ?: 0,
                    name = name,
                    description = description,
                    isActive = switchIsActive.isChecked,
                    firebaseId = firebaseId
                )

                // Guardar en Room
                val savedRoleId: Long
                if (roleId == null) {
                    // Crear nuevo rol
                    savedRoleId = roleDao.insertar(role)
                } else {
                    // Actualizar rol existente
                    roleDao.actualizar(role)
                    savedRoleId = roleId!!.toLong()

                    // Eliminar permisos anteriores
                    rolePermissionDao.eliminarPermisosPorRol(roleId!!)
                }

                // Insertar nuevos permisos
                selectedPermissions.forEach { permissionId ->
                    val rolePermission = RolePermissionEntity(
                        roleId = savedRoleId.toInt(),
                        permissionId = permissionId
                    )
                    rolePermissionDao.insertar(rolePermission)
                }

                // Sincronizar con Firebase
                if (isNetworkAvailable()) {
                    val roleToSync = if (roleId == null) {
                        role.copy(id = savedRoleId.toInt())
                    } else {
                        role
                    }

                    android.util.Log.d("CreateEditRole", "🔄 Iniciando sincronización de rol: ${roleToSync.name}")
                    android.util.Log.d("CreateEditRole", "   FirebaseId: ${roleToSync.firebaseId}")
                    android.util.Log.d("CreateEditRole", "   Permisos seleccionados: ${selectedPermissions.size}")

                    com.universidad.streamzone.data.remote.FirebaseService.sincronizarRol(
                        role = roleToSync,
                        onSuccess = { newFirebaseId ->
                            android.util.Log.d("CreateEditRole", "✅ Rol sincronizado. FirebaseId: $newFirebaseId")

                            lifecycleScope.launch {
                                // Actualizar firebaseId en Room si es nuevo
                                if (firebaseId == null) {
                                    roleDao.actualizar(roleToSync.copy(
                                        firebaseId = newFirebaseId,
                                        sincronizado = true
                                    ))
                                    android.util.Log.d("CreateEditRole", "✅ FirebaseId guardado en Room")
                                }

                                // Convertir IDs de permisos a códigos de permisos
                                val permissionDao = db.permissionDao()
                                val permissionCodes = selectedPermissions.mapNotNull { permId ->
                                    allPermissions.find { it.id == permId }?.code
                                }

                                android.util.Log.d("CreateEditRole", "🔄 Sincronizando permisos...")
                                android.util.Log.d("CreateEditRole", "   Códigos: $permissionCodes")
                                android.util.Log.d("CreateEditRole", "   FirebaseId del rol: $newFirebaseId")

                                // Sincronizar permisos del rol usando códigos
                                com.universidad.streamzone.data.remote.FirebaseService.sincronizarPermisosRol(
                                    roleId = savedRoleId.toInt(),
                                    roleFirebaseId = newFirebaseId,
                                    permissionCodes = permissionCodes,
                                    onSuccess = {
                                        android.util.Log.d("CreateEditRole", "✅ Permisos sincronizados exitosamente")
                                        runOnUiThread {
                                            Toast.makeText(
                                                this@CreateEditRoleActivity,
                                                "Rol y permisos sincronizados con Firebase",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    },
                                    onFailure = { e ->
                                        android.util.Log.e("CreateEditRole", "❌ Error al sincronizar permisos", e)
                                        runOnUiThread {
                                            Toast.makeText(
                                                this@CreateEditRoleActivity,
                                                "Error al sincronizar permisos: ${e.message}",
                                                Toast.LENGTH_LONG
                                            ).show()
                                        }
                                    }
                                )
                            }
                        },
                        onFailure = { e ->
                            android.util.Log.e("CreateEditRole", "❌ Error al sincronizar rol", e)
                            runOnUiThread {
                                Toast.makeText(
                                    this@CreateEditRoleActivity,
                                    "Error al sincronizar rol: ${e.message}",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    )
                } else {
                    android.util.Log.w("CreateEditRole", "⚠️ No hay conexión a internet. Rol guardado solo localmente")
                    runOnUiThread {
                        Toast.makeText(
                            this@CreateEditRoleActivity,
                            "Rol guardado localmente (sin conexión)",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                runOnUiThread {
                    Toast.makeText(
                        this@CreateEditRoleActivity,
                        "Rol guardado correctamente",
                        Toast.LENGTH_SHORT
                    ).show()
                    finish()
                }

            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(
                        this@CreateEditRoleActivity,
                        "Error al guardar rol: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    /**
     * Verificar conectividad
     */
    private fun isNetworkAvailable(): Boolean {
        return try {
            val cm = getSystemService(android.content.Context.CONNECTIVITY_SERVICE)
                    as android.net.ConnectivityManager
            val network = cm.activeNetwork ?: return false
            val caps = cm.getNetworkCapabilities(network) ?: return false

            caps.hasTransport(android.net.NetworkCapabilities.TRANSPORT_WIFI) ||
                    caps.hasTransport(android.net.NetworkCapabilities.TRANSPORT_CELLULAR) ||
                    caps.hasTransport(android.net.NetworkCapabilities.TRANSPORT_ETHERNET)
        } catch (e: Exception) {
            false
        }
    }

    inner class PermissionAdapter(
        private val permissions: List<PermissionEntity>,
        private val selectedPermissions: MutableSet<Int>
    ) : RecyclerView.Adapter<PermissionAdapter.PermissionViewHolder>() {

        inner class PermissionViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val checkBox: CheckBox = view.findViewById(R.id.cbPermission)
            val tvDescription: TextView = view.findViewById(R.id.tvPermissionDescription)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PermissionViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_permission_checkbox, parent, false)
            return PermissionViewHolder(view)
        }

        override fun onBindViewHolder(holder: PermissionViewHolder, position: Int) {
            val permission = permissions[position]
            holder.checkBox.text = permission.name
            holder.tvDescription.text = permission.description
            holder.checkBox.isChecked = selectedPermissions.contains(permission.id)

            holder.checkBox.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    selectedPermissions.add(permission.id)
                } else {
                    selectedPermissions.remove(permission.id)
                }
            }
        }

        override fun getItemCount() = permissions.size
    }
}