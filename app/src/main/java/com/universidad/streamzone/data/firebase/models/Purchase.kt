package com.universidad.streamzone.data.firebase.models

import com.google.firebase.Timestamp

/**
 * Modelo de compra para Firebase Firestore
 */
data class Purchase(
    val id: String = "",
    val userId: String = "", // Referencia a users
    val userEmail: String = "", // Denormalizado para queries
    val userName: String = "", // Denormalizado
    val serviceId: String = "", // Referencia a services
    val serviceName: String = "", // Denormalizado
    val servicePrice: String = "",
    val serviceDuration: String = "",
    val credentials: PurchaseCredentials? = null,
    val purchaseDate: Timestamp = Timestamp.now(),
    val expirationDate: Timestamp = Timestamp.now(),
    val status: String = "active", // "active", "expired", "pending"
    val createdAt: Timestamp = Timestamp.now(),
    val updatedAt: Timestamp = Timestamp.now()
) {
    constructor() : this(
        id = "",
        userId = "",
        userEmail = "",
        userName = "",
        serviceId = "",
        serviceName = "",
        servicePrice = "",
        serviceDuration = "",
        credentials = null,
        purchaseDate = Timestamp.now(),
        expirationDate = Timestamp.now(),
        status = "active",
        createdAt = Timestamp.now(),
        updatedAt = Timestamp.now()
    )

    fun toMap(): Map<String, Any?> {
        return mapOf(
            "userId" to userId,
            "userEmail" to userEmail,
            "userName" to userName,
            "serviceId" to serviceId,
            "serviceName" to serviceName,
            "servicePrice" to servicePrice,
            "serviceDuration" to serviceDuration,
            "credentials" to credentials?.toMap(),
            "purchaseDate" to purchaseDate,
            "expirationDate" to expirationDate,
            "status" to status,
            "createdAt" to createdAt,
            "updatedAt" to updatedAt
        )
    }

    companion object {
        fun fromMap(id: String, map: Map<String, Any?>): Purchase {
            val credentialsMap = map["credentials"] as? Map<*, *>
            return Purchase(
                id = id,
                userId = map["userId"] as? String ?: "",
                userEmail = map["userEmail"] as? String ?: "",
                userName = map["userName"] as? String ?: "",
                serviceId = map["serviceId"] as? String ?: "",
                serviceName = map["serviceName"] as? String ?: "",
                servicePrice = map["servicePrice"] as? String ?: "",
                serviceDuration = map["serviceDuration"] as? String ?: "",
                credentials = credentialsMap?.let {
                    PurchaseCredentials.fromMap(it as Map<String, Any?>)
                },
                purchaseDate = map["purchaseDate"] as? Timestamp ?: Timestamp.now(),
                expirationDate = map["expirationDate"] as? Timestamp ?: Timestamp.now(),
                status = map["status"] as? String ?: "active",
                createdAt = map["createdAt"] as? Timestamp ?: Timestamp.now(),
                updatedAt = map["updatedAt"] as? Timestamp ?: Timestamp.now()
            )
        }
    }
}

/**
 * Credenciales del servicio comprado
 */
data class PurchaseCredentials(
    val email: String? = null,
    val password: String? = null
) {
    constructor() : this(null, null)

    fun toMap(): Map<String, Any?> {
        return mapOf(
            "email" to email,
            "password" to password
        )
    }

    companion object {
        fun fromMap(map: Map<String, Any?>): PurchaseCredentials {
            return PurchaseCredentials(
                email = map["email"] as? String,
                password = map["password"] as? String
            )
        }
    }
}
