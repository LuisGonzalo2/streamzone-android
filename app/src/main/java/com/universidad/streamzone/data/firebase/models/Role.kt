package com.universidad.streamzone.data.firebase.models

import com.google.firebase.Timestamp

/**
 * Modelo de rol para Firebase Firestore
 */
data class Role(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val isActive: Boolean = true,
    val permissionIds: List<String> = emptyList(), // IDs de permisos
    val createdAt: Timestamp = Timestamp.now(),
    val updatedAt: Timestamp = Timestamp.now()
) {
    constructor() : this(
        id = "",
        name = "",
        description = "",
        isActive = true,
        permissionIds = emptyList(),
        createdAt = Timestamp.now(),
        updatedAt = Timestamp.now()
    )

    fun toMap(): Map<String, Any?> {
        return mapOf(
            "name" to name,
            "description" to description,
            "isActive" to isActive,
            "permissionIds" to permissionIds,
            "createdAt" to createdAt,
            "updatedAt" to updatedAt
        )
    }

    companion object {
        fun fromMap(id: String, map: Map<String, Any?>): Role {
            return Role(
                id = id,
                name = map["name"] as? String ?: "",
                description = map["description"] as? String ?: "",
                isActive = map["isActive"] as? Boolean ?: true,
                permissionIds = (map["permissionIds"] as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
                createdAt = map["createdAt"] as? Timestamp ?: Timestamp.now(),
                updatedAt = map["updatedAt"] as? Timestamp ?: Timestamp.now()
            )
        }
    }
}
