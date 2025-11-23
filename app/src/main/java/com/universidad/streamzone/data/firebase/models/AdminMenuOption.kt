package com.universidad.streamzone.data.firebase.models

import com.google.firebase.Timestamp

/**
 * Modelo de opción de menú admin para Firebase Firestore
 */
data class AdminMenuOption(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val icon: String = "", // Emoji
    val permissionCode: String = "", // Vinculado a Permission.code
    val activityClass: String = "", // Clase de Activity
    val orderIndex: Int = 0,
    val isActive: Boolean = true,
    val createdAt: Timestamp = Timestamp.now()
) {
    constructor() : this(
        id = "",
        title = "",
        description = "",
        icon = "",
        permissionCode = "",
        activityClass = "",
        orderIndex = 0,
        isActive = true,
        createdAt = Timestamp.now()
    )

    fun toMap(): Map<String, Any?> {
        return mapOf(
            "title" to title,
            "description" to description,
            "icon" to icon,
            "permissionCode" to permissionCode,
            "activityClass" to activityClass,
            "orderIndex" to orderIndex,
            "isActive" to isActive,
            "createdAt" to createdAt
        )
    }

    companion object {
        fun fromMap(id: String, map: Map<String, Any?>): AdminMenuOption {
            return AdminMenuOption(
                id = id,
                title = map["title"] as? String ?: "",
                description = map["description"] as? String ?: "",
                icon = map["icon"] as? String ?: "",
                permissionCode = map["permissionCode"] as? String ?: "",
                activityClass = map["activityClass"] as? String ?: "",
                orderIndex = (map["orderIndex"] as? Long)?.toInt() ?: 0,
                isActive = map["isActive"] as? Boolean ?: true,
                createdAt = map["createdAt"] as? Timestamp ?: Timestamp.now()
            )
        }
    }
}
