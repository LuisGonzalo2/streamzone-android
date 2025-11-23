package com.universidad.streamzone.util

import com.universidad.streamzone.data.firebase.models.Notification
import com.universidad.streamzone.data.model.NotificationEntity

/**
 * Extensiones para convertir entre Notification (Firebase) y NotificationEntity (UI)
 */

/**
 * Convierte Firebase Notification a NotificationEntity (modelo de UI)
 */
fun Notification.toNotificationEntity(): NotificationEntity {
    return NotificationEntity(
        id = this.id.hashCode(), // Convertir String ID a Int para compatibilidad
        title = this.title,
        message = this.message,
        timestamp = this.timestamp.seconds * 1000, // Convertir Timestamp a millis
        isRead = this.isRead,
        type = this.type.name, // Convertir enum a String
        icon = this.icon,
        userId = this.userId,
        actionType = this.actionType,
        actionData = this.actionData
    )
}

/**
 * Convierte una lista de Firebase Notification a lista de NotificationEntity
 */
fun List<Notification>.toNotificationEntityList(): List<NotificationEntity> {
    return this.map { it.toNotificationEntity() }
}
