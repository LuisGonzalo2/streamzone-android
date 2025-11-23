package com.universidad.streamzone.util

import com.universidad.streamzone.data.firebase.models.Offer
import com.universidad.streamzone.data.model.OfferEntity

/**
 * Convierte un Offer de Firebase a OfferEntity para la UI
 */
fun Offer.toOfferEntity(): OfferEntity {
    return OfferEntity(
        id = this.id.hashCode().toLong(),
        title = this.title,
        description = this.description,
        serviceIds = this.serviceIds.joinToString(","), // Convertir List<String> a String separado por comas
        originalPrice = this.originalPrice,
        comboPrice = this.comboPrice,
        discountPercent = this.discountPercent,
        startDate = this.startDate.seconds * 1000, // Timestamp a milisegundos
        endDate = this.endDate.seconds * 1000,
        isActive = this.isActive,
        sincronizado = true,
        firebaseId = this.id
    )
}

/**
 * Convierte una lista de Offer de Firebase a lista de OfferEntity
 */
fun List<Offer>.toOfferEntityList(): List<OfferEntity> {
    return this.map { it.toOfferEntity() }
}
