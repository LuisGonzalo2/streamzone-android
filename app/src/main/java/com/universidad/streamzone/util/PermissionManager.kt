package com.universidad.streamzone.util

import android.content.Context
import com.universidad.streamzone.data.local.database.AppDatabase
import com.universidad.streamzone.data.model.UsuarioEntity

/**
 * Manager para validar permisos de usuarios
 */
class PermissionManager(private val context: Context) {

    companion object {
        // Códigos de permisos
        const val MANAGE_PURCHASES = "MANAGE_PURCHASES"
        const val VIEW_ALL_PURCHASES = "VIEW_ALL_PURCHASES"
        const val MANAGE_USERS = "MANAGE_USERS"
        const val MANAGE_ROLES = "MANAGE_ROLES"
        const val MANAGE_SERVICES = "MANAGE_SERVICES"
        const val MANAGE_CATEGORIES = "MANAGE_CATEGORIES"
        const val MANAGE_OFFERS = "MANAGE_OFFERS"
        const val UPLOAD_IMAGES = "UPLOAD_IMAGES"
        const val EDIT_PAYMENT_INFO = "EDIT_PAYMENT_INFO"
        const val EDIT_INSTRUCTIONS = "EDIT_INSTRUCTIONS"
        const val EDIT_RATINGS = "EDIT_RATINGS"
        const val FULL_ACCESS = "FULL_ACCESS"
    }

    /**
     * Verifica si un usuario tiene un permiso específico
     */
    suspend fun hasPermission(userEmail: String, permissionCode: String): Boolean {
        val db = AppDatabase.getInstance(context)
        val usuarioDao = db.usuarioDao()
        val permissionDao = db.permissionDao()
        val userRoleDao = db.userRoleDao()

        // Obtener usuario
        val usuario = usuarioDao.buscarPorEmail(userEmail) ?: return false

        // Si es admin, tiene acceso completo
        if (usuario.isAdmin) {
            return true
        }

        // Obtener roles del usuario
        val userRoles = userRoleDao.getRolesByUserId(usuario.id)

        // Verificar si alguno de sus roles tiene el permiso
        userRoles.forEach { roleId ->
            val permissions = permissionDao.getPermissionsByRole(roleId)

            // Si tiene FULL_ACCESS, tiene todos los permisos
            if (permissions.any { it.code == FULL_ACCESS }) {
                return true
            }

            // Verificar permiso específico
            if (permissions.any { it.code == permissionCode }) {
                return true
            }
        }

        return false
    }

    /**
     * Verifica si el usuario es administrador
     */
    suspend fun isAdmin(userEmail: String): Boolean {
        val db = AppDatabase.getInstance(context)
        val usuarioDao = db.usuarioDao()
        val usuario = usuarioDao.buscarPorEmail(userEmail) ?: return false
        return usuario.isAdmin
    }

    /**
     * Obtiene el usuario por email
     */
    suspend fun getUser(userEmail: String): UsuarioEntity? {
        val db = AppDatabase.getInstance(context)
        val usuarioDao = db.usuarioDao()
        return usuarioDao.buscarPorEmail(userEmail)
    }
}