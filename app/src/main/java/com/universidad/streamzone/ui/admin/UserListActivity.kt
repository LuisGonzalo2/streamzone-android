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
import com.universidad.streamzone.R
import com.universidad.streamzone.data.local.database.AppDatabase
import com.universidad.streamzone.data.model.UsuarioEntity
import com.universidad.streamzone.data.remote.FirebaseService
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
                // Sincronizar usuarios desde Firebase primero
                if (isNetworkAvailable()) {
                    // 1. Primero sincronizar ROLES (para que existan en Room)
                    syncRolesFromFirebase()

                    // 2. Luego sincronizar USUARIOS
                    syncUsersFromFirebase()

                    // 3. Finalmente sincronizar USER_ROLES (asignaciones)
                    syncUserRolesFromFirebase()
                }

                // Cargar usuarios desde Room (incluye los de Firebase)
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
                Log.e("UserList", "❌ Error al cargar usuarios", e)
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

    /**
     * Sincroniza usuarios de Firebase a Room
     */
    private suspend fun syncUsersFromFirebase() {
        FirebaseService.obtenerTodosLosUsuarios { firebaseUsers ->
            lifecycleScope.launch {
                try {
                    val db = AppDatabase.getInstance(this@UserListActivity)
                    val usuarioDao = db.usuarioDao()

                    firebaseUsers.forEach { firebaseUser ->
                        val localUser = usuarioDao.buscarPorEmail(firebaseUser.email)

                        if (localUser == null) {
                            // Usuario nuevo → Insertar
                            usuarioDao.insertar(firebaseUser)
                        } else if (localUser.firebaseId != firebaseUser.firebaseId) {
                            // Actualizar solo si hay cambios
                            val updated = localUser.copy(
                                fullname = firebaseUser.fullname,
                                phone = firebaseUser.phone,
                                isAdmin = firebaseUser.isAdmin,
                                firebaseId = firebaseUser.firebaseId,
                                sincronizado = true
                            )
                            usuarioDao.actualizar(updated)
                        }
                    }

                    // Recargar después de sincronizar
                    val users = usuarioDao.getAll()
                    val adminCount = users.count { it.isAdmin }

                    runOnUiThread {
                        tvTotalUsers.text = users.size.toString()
                        tvAdminUsers.text = adminCount.toString()
                        userAdapter.updateUsers(users)
                    }

                } catch (e: Exception) {
                    Log.e("UserList", "❌ Error al guardar usuarios de Firebase", e)
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

                // Actualizar en Room
                val updatedUser = user.copy(isAdmin = isAdmin)
                usuarioDao.actualizar(updatedUser)

                // Actualizar en Firebase
                if (isNetworkAvailable()) {
                    FirebaseService.actualizarUsuario(
                        updatedUser,
                        onSuccess = {
                            Log.d("UserList", "✅ Usuario actualizado en Firebase")
                        },
                        onFailure = { e ->
                            Log.e("UserList", "❌ Error Firebase: ${e.message}")
                        }
                    )
                }

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

    /**
     * Sincronizar roles desde Firebase a Room
     * IMPORTANTE: Debe ejecutarse ANTES de syncUserRolesFromFirebase()
     */
    private suspend fun syncRolesFromFirebase() {
        FirebaseService.obtenerTodosLosRoles { firebaseRoles ->
            lifecycleScope.launch {
                try {
                    val db = AppDatabase.getInstance(this@UserListActivity)
                    val roleDao = db.roleDao()
                    val permissionDao = db.permissionDao()
                    val rolePermissionDao = db.rolePermissionDao()

                    firebaseRoles.forEach { firebaseRole ->
                        val allLocalRoles = roleDao.getAll()

                        // Buscar por firebaseId O por nombre (si es un rol local sin firebaseId)
                        val localRole = allLocalRoles.find { it.firebaseId == firebaseRole.firebaseId }
                            ?: allLocalRoles.find { it.name.equals(firebaseRole.name, ignoreCase = true) && it.firebaseId == null }

                        val roleLocalId = if (localRole == null) {
                            // Rol nuevo → Insertar
                            roleDao.insertar(firebaseRole).toInt()
                        } else {
                            // Rol existe → Actualizar con firebaseId
                            val updated = firebaseRole.copy(id = localRole.id)
                            roleDao.actualizar(updated)
                            localRole.id
                        }

                        // Sincronizar permisos del rol
                        if (firebaseRole.firebaseId != null) {
                            FirebaseService.obtenerPermisosRol(
                                roleFirebaseId = firebaseRole.firebaseId!!,
                                onSuccess = { permissionCodes ->
                                    lifecycleScope.launch {
                                        try {
                                            rolePermissionDao.eliminarPermisosPorRol(roleLocalId)
                                            val allPermissions = permissionDao.getAll()

                                            permissionCodes.forEach { code ->
                                                val permission = allPermissions.find { it.code == code }
                                                if (permission != null) {
                                                    rolePermissionDao.insertar(
                                                        com.universidad.streamzone.data.model.RolePermissionEntity(
                                                            roleId = roleLocalId,
                                                            permissionId = permission.id
                                                        )
                                                    )
                                                }
                                            }
                                        } catch (e: Exception) {
                                            Log.e("UserList", "Error al sincronizar permisos del rol: ${e.message}")
                                        }
                                    }
                                },
                                onFailure = { e ->
                                    Log.e("UserList", "Error al obtener permisos: ${e.message}")
                                }
                            )
                        }
                    }
                } catch (e: Exception) {
                    Log.e("UserList", "❌ Error al sincronizar roles", e)
                }
            }
        }
    }

    /**
     * Sincronizar user_roles (asignaciones de roles a usuarios) desde Firebase
     * IMPORTANTE: Esto sincroniza qué usuarios tienen qué roles
     */
    private fun syncUserRolesFromFirebase() {
        FirebaseService.obtenerTodosLosUserRoles(
            onSuccess = { userRolesMap ->
                lifecycleScope.launch {
                    try {
                        val db = AppDatabase.getInstance(this@UserListActivity)
                        val usuarioDao = db.usuarioDao()
                        val roleDao = db.roleDao()
                        val userRoleDao = db.userRoleDao()

                        var totalAsignados = 0
                        var errores = 0

                        userRolesMap.forEach { (email, roleFirebaseIds) ->
                            // Buscar usuario por email
                            val usuario = usuarioDao.buscarPorEmail(email)
                            if (usuario == null) {
                                errores++
                                return@forEach
                            }

                            if (roleFirebaseIds.isEmpty()) {
                                // Usuario no tiene roles, eliminar asignaciones locales
                                userRoleDao.eliminarRolesPorUsuario(usuario.id)
                                return@forEach
                            }

                            // Eliminar roles actuales para evitar duplicados
                            userRoleDao.eliminarRolesPorUsuario(usuario.id)

                            // Obtener todos los roles locales
                            val allRoles = roleDao.getAll()

                            // Asignar nuevos roles
                            roleFirebaseIds.forEach { firebaseId ->
                                val role = allRoles.find { it.firebaseId == firebaseId }
                                if (role != null) {
                                    userRoleDao.insertar(
                                        com.universidad.streamzone.data.model.UserRoleEntity(
                                            userId = usuario.id,
                                            roleId = role.id
                                        )
                                    )
                                    totalAsignados++
                                } else {
                                    errores++
                                }
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("UserList", "Error al sincronizar user_roles", e)
                    }
                }
            },
            onFailure = { e ->
                Log.e("UserList", "Error al obtener user_roles desde Firebase", e)
            }
        )
    }

    /**
     * Verifica si hay conexión a internet
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
}