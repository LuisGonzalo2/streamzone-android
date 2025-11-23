package com.universidad.streamzone.data.firebase.models

import com.google.firebase.Timestamp

/**
 * Modelo de servicio para Firebase Firestore
 */
data class Service(
    val id: String = "",
    val serviceId: String = "", // "netflix", "spotify", etc.
    val name: String = "",
    val price: String = "",
    val description: String = "",
    val iconUrl: String? = null, // URL de Firebase Storage
    val categoryId: String = "", // Referencia a categories
    val isActive: Boolean = true,
    val isPopular: Boolean = false,
    val createdAt: Timestamp = Timestamp.now(),
    val updatedAt: Timestamp = Timestamp.now()
) {
    constructor() : this(
        id = "",
        serviceId = "",
        name = "",
        price = "",
        description = "",
        iconUrl = null,
        categoryId = "",
        isActive = true,
        isPopular = false,
        createdAt = Timestamp.now(),
        updatedAt = Timestamp.now()
    )

    fun toMap(): Map<String, Any?> {
        return mapOf(
            "serviceId" to serviceId,
            "name" to name,
            "price" to price,
            "description" to description,
            "iconUrl" to iconUrl,
            "categoryId" to categoryId,
            "isActive" to isActive,
            "isPopular" to isPopular,
            "createdAt" to createdAt,
            "updatedAt" to updatedAt
        )
    }

    companion object {
        fun fromMap(id: String, map: Map<String, Any?>): Service {
            return Service(
                id = id,
                serviceId = map["serviceId"] as? String ?: "",
                name = map["name"] as? String ?: "",
                price = map["price"] as? String ?: "",
                description = map["description"] as? String ?: "",
                iconUrl = map["iconUrl"] as? String,
                categoryId = map["categoryId"] as? String ?: "",
                isActive = map["isActive"] as? Boolean ?: true,
                isPopular = map["isPopular"] as? Boolean ?: false,
                createdAt = map["createdAt"] as? Timestamp ?: Timestamp.now(),
                updatedAt = map["updatedAt"] as? Timestamp ?: Timestamp.now()
            )
        }
    }
}
