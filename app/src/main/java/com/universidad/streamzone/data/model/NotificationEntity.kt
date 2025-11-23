package com.universidad.streamzone.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notifications")
data class NotificationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val title: String,
    val message: String,
    val type: NotificationType,
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false,
    val userId: String? = null, // null = notificación para todos los usuarios
    val actionType: String? = null, // Para navegación (ej: "open_category", "open_service")
    val actionData: String? = null, // Datos adicionales (ej: ID de categoría)
    val icon: String = "🔔" // Emoji por defecto
)

enum class NotificationType {
    SYSTEM,          // Notificaciones del sistema
    CATEGORY,        // Nueva categoría o cambios
    SERVICE,         // Nuevo servicio o cambios
    OFFER,           // Nueva oferta
    PURCHASE,        // Cambios en compras
    ROLE,            // Cambios en roles/permisos
    ADMIN,           // Notificaciones administrativas
    UPDATE           // Actualizaciones de la app
}