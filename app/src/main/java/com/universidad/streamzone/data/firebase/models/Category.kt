package com.universidad.streamzone.data.firebase.models

import com.google.firebase.Timestamp

/**
 * Modelo de categoría para Firebase Firestore
 */
data class Category(
    val id: String = "",
    val categoryId: String = "", // "streaming", "music", etc.
    val name: String = "",
    val icon: String = "", // Emoji
    val description: String = "",
    val gradientStart: String = "",
    val gradientEnd: String = "",
    val isActive: Boolean = true,
    val order: Int = 0,
    val createdAt: Timestamp = Timestamp.now(),
    val updatedAt: Timestamp = Timestamp.now()
) {
    constructor() : this(
        id = "",
        categoryId = "",
        name = "",
        icon = "",
        description = "",
        gradientStart = "",
        gradientEnd = "",
        isActive = true,
        order = 0,
        createdAt = Timestamp.now(),
        updatedAt = Timestamp.now()
    )

    fun toMap(): Map<String, Any?> {
        return mapOf(
            "categoryId" to categoryId,
            "name" to name,
            "icon" to icon,
            "description" to description,
            "gradientStart" to gradientStart,
            "gradientEnd" to gradientEnd,
            "isActive" to isActive,
            "order" to order,
            "createdAt" to createdAt,
            "updatedAt" to updatedAt
        )
    }

    companion object {
        fun fromMap(id: String, map: Map<String, Any?>): Category {
            return Category(
                id = id,
                categoryId = map["categoryId"] as? String ?: "",
                name = map["name"] as? String ?: "",
                icon = map["icon"] as? String ?: "",
                description = map["description"] as? String ?: "",
                gradientStart = map["gradientStart"] as? String ?: "",
                gradientEnd = map["gradientEnd"] as? String ?: "",
                isActive = map["isActive"] as? Boolean ?: true,
                order = (map["order"] as? Long)?.toInt() ?: 0,
                createdAt = map["createdAt"] as? Timestamp ?: Timestamp.now(),
                updatedAt = map["updatedAt"] as? Timestamp ?: Timestamp.now()
            )
        }
    }
}
