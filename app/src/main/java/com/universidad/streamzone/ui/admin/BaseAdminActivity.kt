package com.universidad.streamzone.ui.admin

import android.content.Intent
import android.os.Bundle
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
    protected var currentUserEmail: String = ""

    /**
     * Permiso requerido para acceder a esta pantalla
     * Override en clases hijas para especificar el permiso
     * Si es null, solo verifica que sea admin
     */
    protected open val requiredPermission: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        permissionManager = PermissionManager(this)

        // Obtener email del usuario logueado
        val sharedPrefs = getSharedPreferences("StreamZoneData", MODE_PRIVATE)
        currentUserEmail = sharedPrefs.getString("logged_in_user_email", "") ?: ""

        // Validar sesión y permisos
        validateAccess()
    }

    private fun validateAccess() {
        if (currentUserEmail.isEmpty()) {
            showAccessDenied("Debes iniciar sesión para acceder a esta pantalla")
            redirectToLogin()
            return
        }

        lifecycleScope.launch {
            try {
                // Verificar si es admin
                val isAdmin = permissionManager.isAdmin(currentUserEmail)

                if (!isAdmin) {
                    runOnUiThread {
                        showAccessDenied("No tienes permisos de administrador")
                        finish()
                    }
                    return@launch
                }

                // Si hay un permiso específico requerido, verificarlo
                requiredPermission?.let { permission ->
                    val hasPermission = permissionManager.hasPermission(currentUserEmail, permission)

                    if (!hasPermission) {
                        runOnUiThread {
                            showAccessDenied("No tienes permisos para acceder a esta función")
                            finish()
                        }
                        return@launch
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