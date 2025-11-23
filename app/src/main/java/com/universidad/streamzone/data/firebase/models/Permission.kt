package com.universidad.streamzone.data.firebase.models

import com.google.firebase.Timestamp

/**
 * Modelo de permiso para Firebase Firestore
 */
data class Permission(
    val id: String = "",
    val code: String = "", // "MANAGE_PURCHASES", etc.
    val name: String = "",
    val description: String = "",
    val createdAt: Timestamp = Timestamp.now()
) {
    constructor() : this(
        id = "",
        code = "",
        name = "",
        description = "",
        createdAt = Timestamp.now()
    )

    fun toMap(): Map<String, Any?> {
        return mapOf(
            "code" to code,
            "name" to name,
            "description" to description,
            "createdAt" to createdAt
        )
    }

    companion object {
        fun fromMap(id: String, map: Map<String, Any?>): Permission {
            return Permission(
                id = id,
                code = map["code"] as? String ?: "",
                name = map["name"] as? String ?: "",
                description = map["description"] as? String ?: "",
                createdAt = map["createdAt"] as? Timestamp ?: Timestamp.now()
            )
        }
    }
}
