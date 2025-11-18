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
import com.universidad.streamzone.data.local.database.AppDatabase
import com.universidad.streamzone.data.model.RoleEntity
import com.universidad.streamzone.data.remote.FirebaseService
import com.universidad.streamzone.ui.admin.adapter.RoleListAdapter
import com.universidad.streamzone.util.PermissionManager
import kotlinx.coroutines.launch

/**
 * Activity para listar y gestionar roles del sistema
 */
class RoleListActivity : BaseAdminActivity() {

    override val requiredPermission: String = PermissionManager.MANAGE_ROLES

    private lateinit var btnBack: ImageButton
    private lateinit var rvRoles: RecyclerView
    private lateinit var llEmptyState: LinearLayout
    private lateinit var fabAddRole: FloatingActionButton
    private lateinit var roleAdapter: RoleListAdapter

    // Control de sincronización para evitar duplicados
    private var lastSyncTimestamp: Long = 0
    private val SYNC_COOLDOWN_MS = 5000 // 5 segundos (reducido para testing)

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
                intent.putExtra("ROLE_ID", role.id)
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
                val db = AppDatabase.getInstance(this@RoleListActivity)
                val roleDao = db.roleDao()

                // Sincronizar desde Firebase solo si pasó el tiempo de cooldown
                val currentTime = System.currentTimeMillis()
                val timeSinceLastSync = currentTime - lastSyncTimestamp

                Log.d("RoleList", "⏰ Tiempo desde última sync: ${timeSinceLastSync}ms (cooldown: ${SYNC_COOLDOWN_MS}ms)")

                if (isNetworkAvailable() && timeSinceLastSync > SYNC_COOLDOWN_MS) {
                    Log.d("RoleList", "🚀 INICIANDO SINCRONIZACIÓN COMPLETA...")
                    lastSyncTimestamp = currentTime
                    syncRolesFromFirebase()
                    syncUserRolesFromFirebase() // Sincronizar asignaciones de roles a usuarios
                } else {
                    if (!isNetworkAvailable()) {
                        Log.w("RoleList", "⚠️ Sin conexión a internet - saltando sincronización")
                    } else {
                        Log.d("RoleList", "⏸️ Cooldown activo - saltando sincronización (quedan ${SYNC_COOLDOWN_MS - timeSinceLastSync}ms)")
                    }
                }

                val roles = roleDao.getAll()

                runOnUiThread {
                    if (roles.isEmpty()) {
                        rvRoles.visibility = View.GONE
                        llEmptyState.visibility = View.VISIBLE
                    } else {
                        rvRoles.visibility = View.VISIBLE
                        llEmptyState.visibility = View.GONE
                        roleAdapter.updateRoles(roles)
                    }
                }

            } catch (e: Exception) {
                Log.e("RoleList", "❌ Error al cargar roles", e)
                runOnUiThread {
                }
            }
        }
    }

    /**
     * Sincronizar roles desde Firebase
     */
    private suspend fun syncRolesFromFirebase() {
        FirebaseService.obtenerTodosLosRoles { firebaseRoles ->
            lifecycleScope.launch {
                try {
                    val db = AppDatabase.getInstance(this@RoleListActivity)
                    val roleDao = db.roleDao()
                    val permissionDao = db.permissionDao()
                    val rolePermissionDao = db.rolePermissionDao()

                    firebaseRoles.forEach { firebaseRole ->
                        val localRole = roleDao.getAll()
                            .find { it.firebaseId == firebaseRole.firebaseId }

                        val roleLocalId = if (localRole == null) {
                            // Rol nuevo → Insertar
                            val newId = roleDao.insertar(firebaseRole).toInt()
                            Log.d("RoleList", "➕ Rol insertado: ${firebaseRole.name}")
                            newId
                        } else {
                            // Rol existe → Actualizar
                            val updated = firebaseRole.copy(id = localRole.id)
                            roleDao.actualizar(updated)
                            Log.d("RoleList", "🔄 Rol actualizado: ${firebaseRole.name}")
                            localRole.id
                        }

                        // Sincronizar permisos del rol desde Firebase
                        if (firebaseRole.firebaseId != null) {
                            FirebaseService.obtenerPermisosRol(
                                roleFirebaseId = firebaseRole.firebaseId!!,
                                onSuccess = { permissionCodes ->
                                    lifecycleScope.launch {
                                        try {
                                            // Eliminar permisos anteriores del rol
                                            rolePermissionDao.eliminarPermisosPorRol(roleLocalId)

                                            // Obtener todos los permisos locales
                                            val allPermissions = permissionDao.getAll()

                                            // Convertir códigos a IDs y asignar permisos
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
                                            Log.d("RoleList", "✅ Permisos sincronizados para rol: ${firebaseRole.name}")
                                        } catch (e: Exception) {
                                            Log.e("RoleList", "❌ Error al sincronizar permisos del rol: ${e.message}")
                                        }
                                    }
                                },
                                onFailure = { e ->
                                    Log.e("RoleList", "❌ Error al obtener permisos del rol: ${e.message}")
                                }
                            )
                        }
                    }

                } catch (e: Exception) {
                    Log.e("RoleList", "❌ Error al sincronizar roles", e)
                }
            }
        }
    }

    /**
     * Sincronizar user_roles (asignaciones de roles a usuarios) desde Firebase
     */
    private fun syncUserRolesFromFirebase() {
        FirebaseService.obtenerTodosLosUserRoles(
            onSuccess = { userRolesMap ->
                lifecycleScope.launch {
                    try {
                        Log.d("RoleList", "🔄 Sincronizando user_roles desde Firebase...")
                        Log.d("RoleList", "   ${userRolesMap.size} usuarios con roles encontrados")

                        val db = AppDatabase.getInstance(this@RoleListActivity)
                        val usuarioDao = db.usuarioDao()
                        val roleDao = db.roleDao()
                        val userRoleDao = db.userRoleDao()

                        var totalAsignados = 0

                        userRolesMap.forEach { (email, roleFirebaseIds) ->
                            // Buscar usuario por email
                            val usuario = usuarioDao.buscarPorEmail(email)
                            if (usuario == null) {
                                Log.w("RoleList", "⚠️ Usuario no encontrado: $email")
                                return@forEach
                            }

                            if (roleFirebaseIds.isEmpty()) {
                                // Usuario no tiene roles, eliminar asignaciones locales
                                userRoleDao.eliminarRolesPorUsuario(usuario.id)
                                Log.d("RoleList", "   Usuario $email: sin roles → limpiado")
                                return@forEach
                            }

                            // Eliminar roles actuales
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
                                    Log.d("RoleList", "   ✅ $email → ${role.name}")
                                } else {
                                    Log.w("RoleList", "⚠️ Rol con firebaseId $firebaseId no encontrado")
                                }
                            }
                        }

                        Log.d("RoleList", "✅ User roles sincronizados: $totalAsignados asignaciones")
                    } catch (e: Exception) {
                        Log.e("RoleList", "❌ Error al sincronizar user_roles", e)
                    }
                }
            },
            onFailure = { e ->
                Log.e("RoleList", "❌ Error al obtener user_roles desde Firebase", e)
            }
        )
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
                val db = AppDatabase.getInstance(this@RoleListActivity)
                val roleDao = db.roleDao()

                // Eliminar de Room
                roleDao.eliminar(role)

                // Eliminar de Firebase si tiene firebaseId
                if (role.firebaseId != null && isNetworkAvailable()) {
                    FirebaseService.eliminarRol(
                        firebaseId = role.firebaseId!!,
                        onSuccess = {
                            Log.d("RoleList", "✅ Rol eliminado de Firebase")
                        },
                        onFailure = { e ->
                            Log.e("RoleList", "❌ Error Firebase: ${e.message}")
                        }
                    )
                }

                runOnUiThread {
                    Toast.makeText(
                        this@RoleListActivity,
                        "Rol eliminado correctamente",
                        Toast.LENGTH_SHORT
                    ).show()
                    loadRoles()
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
}