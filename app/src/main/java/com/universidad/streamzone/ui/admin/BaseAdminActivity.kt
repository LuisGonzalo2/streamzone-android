package com.universidad.streamzone.ui.admin

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.universidad.streamzone.ui.auth.LoginActivity
import com.universidad.streamzone.util.PermissionManager
import kotlinx.coroutines.launch

/**
 * Activity base para todas las pantallas de administrador
 * Valida automáticamente permisos y sesión de usuario
 */
abstract class BaseAdminActivity : AppCompatActivity() {

    protected lateinit var permissionManager: PermissionManager
    protected lateinit var sharedPrefs: SharedPreferences
    protected var currentUserEmail: String = ""

    /**
     * Permiso requerido para acceder a esta pantalla
     * Override en clases hijas para especificar el permiso
     * Si es null, solo verifica que sea admin
     */
    protected open val requiredPermission: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Configurar edge-to-edge con padding dinámico para el notch
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false)
        }

        permissionManager = PermissionManager(this)

        // Obtener email del usuario logueado
        sharedPrefs = getSharedPreferences("StreamZoneData", MODE_PRIVATE)
        currentUserEmail = sharedPrefs.getString("logged_in_user_email", "") ?: ""

        // Validar sesión y permisos
        validateAccess()
    }

    override fun setContentView(layoutResID: Int) {
        super.setContentView(layoutResID)
        applyNotchPadding()
    }

    /**
     * Aplica padding superior para evitar que el contenido se superponga con el notch
     */
    private fun applyNotchPadding() {
        val rootView = findViewById<View>(android.R.id.content)
        rootView?.setOnApplyWindowInsetsListener { view, insets ->
            val systemBars = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                insets.getInsets(android.view.WindowInsets.Type.systemBars())
            } else {
                @Suppress("DEPRECATION")
                android.graphics.Insets.of(0, insets.systemWindowInsetTop, 0, 0)
            }
            view.setPadding(0, systemBars.top, 0, 0)
            insets
        }
    }

    private fun validateAccess() {
        if (currentUserEmail.isEmpty()) {
            showAccessDenied("Debes iniciar sesión para acceder a esta pantalla")
            redirectToLogin()
            return
        }

        lifecycleScope.launch {
            try {
                // Verificar si es admin O tiene algún rol
                val isAdmin = permissionManager.isAdmin(currentUserEmail)
                val hasAnyRole = hasAnyRole(currentUserEmail)

                // Si no es admin NO tiene roles, denegar acceso
                if (!isAdmin && !hasAnyRole) {
                    runOnUiThread {
                        showAccessDenied("No tienes permisos de administrador")
                        finish()
                    }
                    return@launch
                }

                // Si hay un permiso específico requerido, verificarlo
                // (solo si NO es admin, porque los admin tienen todos los permisos)
                requiredPermission?.let { permission ->
                    if (!isAdmin) {
                        val hasPermission = permissionManager.hasPermission(currentUserEmail, permission)

                        if (!hasPermission) {
                            runOnUiThread {
                                showAccessDenied("No tienes permisos para acceder a esta función")
                                finish()
                            }
                            return@launch
                        }
                    }
                }

                // Si pasa todas las validaciones, llamar a onPermissionGranted
                runOnUiThread {
                    onPermissionGranted()
                }

            } catch (e: Exception) {
                runOnUiThread {
                    showAccessDenied("Error al validar permisos: ${e.message}")
                    finish()
                }
            }
        }
    }

    //Verificar si el usuario tiene algún rol asignado
    private suspend fun hasAnyRole(userEmail: String): Boolean {
        return try {
            val db = com.universidad.streamzone.data.local.database.AppDatabase.getInstance(this)
            val usuarioDao = db.usuarioDao()
            val userRoleDao = db.userRoleDao()

            val usuario = usuarioDao.buscarPorEmail(userEmail) ?: return false
            val roles = userRoleDao.getRolesByUserId(usuario.id)

            roles.isNotEmpty()
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Método llamado cuando el usuario tiene los permisos necesarios
     * Override en clases hijas para inicializar la UI
     */
    protected open fun onPermissionGranted() {
        // Por defecto no hace nada, las clases hijas pueden override
    }

    private fun showAccessDenied(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    private fun redirectToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
        finish()
    }

    /**
     * Método auxiliar para verificar si el usuario tiene un permiso específico
     */
    protected suspend fun checkPermission(permission: String): Boolean {
        return permissionManager.hasPermission(currentUserEmail, permission)
    }
}