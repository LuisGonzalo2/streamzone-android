package com.universidad.streamzone.data.firebase.models

import com.google.firebase.Timestamp

/**
 * Modelo de usuario para Firebase Firestore
 */
data class User(
    val id: String = "",
    val fullname: String = "",
    val email: String = "",
    val phone: String = "",
    val password: String = "", // Hasheado
    val photoUrl: String? = null, // URL de Firebase Storage
    val isAdmin: Boolean = false,
    val roleIds: List<String> = emptyList(), // IDs de roles asignados
    val createdAt: Timestamp = Timestamp.now(),
    val updatedAt: Timestamp = Timestamp.now()
) {
    // Constructor vacío requerido por Firestore
    constructor() : this(
        id = "",
        fullname = "",
        email = "",
        phone = "",
        password = "",
        photoUrl = null,
        isAdmin = false,
        roleIds = emptyList(),
        createdAt = Timestamp.now(),
        updatedAt = Timestamp.now()
    )

    /**
     * Convierte a Map para guardar en Firestore
     */
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "fullname" to fullname,
            "email" to email,
            "phone" to phone,
            "password" to password,
            "photoUrl" to photoUrl,
            "isAdmin" to isAdmin,
            "roleIds" to roleIds,
            "createdAt" to createdAt,
            "updatedAt" to updatedAt
        )
    }

    companion object {
        /**
         * Convierte desde DocumentSnapshot de Firestore
         */
        fun fromMap(id: String, map: Map<String, Any?>): User {
            return User(
                id = id,
                fullname = map["fullname"] as? String ?: "",
                email = map["email"] as? String ?: "",
                phone = map["phone"] as? String ?: "",
                password = map["password"] as? String ?: "",
                photoUrl = map["photoUrl"] as? String,
                isAdmin = map["isAdmin"] as? Boolean ?: false,
                roleIds = (map["roleIds"] as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
                createdAt = map["createdAt"] as? Timestamp ?: Timestamp.now(),
                updatedAt = map["updatedAt"] as? Timestamp ?: Timestamp.now()
            )
        }
    }
}
