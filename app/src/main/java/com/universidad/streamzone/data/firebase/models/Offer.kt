package com.universidad.streamzone.data.firebase.models

import com.google.firebase.Timestamp

/**
 * Modelo de oferta para Firebase Firestore
 */
data class Offer(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val serviceIds: List<String> = emptyList(), // IDs de servicios en la oferta
    val originalPrice: Double = 0.0,
    val comboPrice: Double = 0.0,
    val discountPercent: Int = 0,
    val startDate: Timestamp = Timestamp.now(),
    val endDate: Timestamp = Timestamp.now(),
    val isActive: Boolean = true,
    val createdAt: Timestamp = Timestamp.now(),
    val updatedAt: Timestamp = Timestamp.now()
) {
    constructor() : this(
        id = "",
        title = "",
        description = "",
        serviceIds = emptyList(),
        originalPrice = 0.0,
        comboPrice = 0.0,
        discountPercent = 0,
        startDate = Timestamp.now(),
        endDate = Timestamp.now(),
        isActive = true,
        createdAt = Timestamp.now(),
        updatedAt = Timestamp.now()
    )

    fun toMap(): Map<String, Any?> {
        return mapOf(
            "title" to title,
            "description" to description,
            "serviceIds" to serviceIds,
            "originalPrice" to originalPrice,
            "comboPrice" to comboPrice,
            "discountPercent" to discountPercent,
            "startDate" to startDate,
            "endDate" to endDate,
            "isActive" to isActive,
            "createdAt" to createdAt,
            "updatedAt" to updatedAt
        )
    }

    /**
     * Verifica si la oferta está vigente
     */
    fun isValid(): Boolean {
        val now = Timestamp.now()
        return isActive && now >= startDate && now <= endDate
    }

    /**
     * Calcula días restantes
     */
    fun getDaysRemaining(): Int {
        val now = Timestamp.now().seconds
        val end = endDate.seconds
        val diff = end - now
        return (diff / 86400).toInt() // 86400 segundos en un día
    }

    companion object {
        fun fromMap(id: String, map: Map<String, Any?>): Offer {
            return Offer(
                id = id,
                title = map["title"] as? String ?: "",
                description = map["description"] as? String ?: "",
                serviceIds = (map["serviceIds"] as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
                originalPrice = (map["originalPrice"] as? Number)?.toDouble() ?: 0.0,
                comboPrice = (map["comboPrice"] as? Number)?.toDouble() ?: 0.0,
                discountPercent = (map["discountPercent"] as? Long)?.toInt() ?: 0,
                startDate = map["startDate"] as? Timestamp ?: Timestamp.now(),
                endDate = map["endDate"] as? Timestamp ?: Timestamp.now(),
                isActive = map["isActive"] as? Boolean ?: true,
                createdAt = map["createdAt"] as? Timestamp ?: Timestamp.now(),
                updatedAt = map["updatedAt"] as? Timestamp ?: Timestamp.now()
            )
        }
    }
}
