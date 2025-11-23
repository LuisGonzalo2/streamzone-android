package com.universidad.streamzone.util

import com.universidad.streamzone.data.firebase.models.User
import com.universidad.streamzone.data.firebase.repository.UserRepository

/**
 * Manager para validar permisos de usuarios con Firebase
 */
class PermissionManager {

    private val userRepository = UserRepository()

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
        // Obtener usuario
        val user = userRepository.findByEmail(userEmail) ?: return false

        // Si es admin, tiene acceso completo
        if (user.isAdmin) {
            return true
        }

        // Verificar permisos a través del repositorio
        return userRepository.hasPermission(user.id, permissionCode)
    }

    /**
     * Verifica si el usuario es administrador
     */
    suspend fun isAdmin(userEmail: String): Boolean {
        val user = userRepository.findByEmail(userEmail) ?: return false
        return user.isAdmin
    }

    /**
     * Obtiene el usuario por email
     */
    suspend fun getUser(userEmail: String): User? {
        return userRepository.findByEmail(userEmail)
    }
}