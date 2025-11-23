package com.universidad.streamzone.data.firebase.models

import com.google.firebase.Timestamp

/**
 * Modelo de notificación para Firebase Firestore
 */
data class Notification(
    val id: String = "",
    val title: String = "",
    val message: String = "",
    val type: NotificationType = NotificationType.SYSTEM,
    val timestamp: Timestamp = Timestamp.now(),
    val isRead: Boolean = false,
    val userId: String? = null, // null = para todos los usuarios
    val actionType: String? = null, // "open_category", "open_service"
    val actionData: String? = null,
    val icon: String = "",
    val createdAt: Timestamp = Timestamp.now()
) {
    constructor() : this(
        id = "",
        title = "",
        message = "",
        type = NotificationType.SYSTEM,
        timestamp = Timestamp.now(),
        isRead = false,
        userId = null,
        actionType = null,
        actionData = null,
        icon = "",
        createdAt = Timestamp.now()
    )

    fun toMap(): Map<String, Any?> {
        return mapOf(
            "title" to title,
            "message" to message,
            "type" to type.name,
            "timestamp" to timestamp,
            "isRead" to isRead,
            "userId" to userId,
            "actionType" to actionType,
            "actionData" to actionData,
            "icon" to icon,
            "createdAt" to createdAt
        )
    }

    companion object {
        fun fromMap(id: String, map: Map<String, Any?>): Notification {
            return Notification(
                id = id,
                title = map["title"] as? String ?: "",
                message = map["message"] as? String ?: "",
                type = try {
                    NotificationType.valueOf(map["type"] as? String ?: "SYSTEM")
                } catch (e: Exception) {
                    NotificationType.SYSTEM
                },
                timestamp = map["timestamp"] as? Timestamp ?: Timestamp.now(),
                isRead = map["isRead"] as? Boolean ?: false,
                userId = map["userId"] as? String,
                actionType = map["actionType"] as? String,
                actionData = map["actionData"] as? String,
                icon = map["icon"] as? String ?: "",
                createdAt = map["createdAt"] as? Timestamp ?: Timestamp.now()
            )
        }
    }
}

enum class NotificationType {
    SYSTEM,
    CATEGORY,
    SERVICE,
    OFFER,
    PURCHASE,
    ROLE,
    ADMIN,
    UPDATE
}
