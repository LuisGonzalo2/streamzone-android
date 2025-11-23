package com.universidad.streamzone.util

import com.universidad.streamzone.data.model.PurchaseEntity
import com.universidad.streamzone.data.firebase.models.Purchase

/**
 * Extensiones para convertir entre Purchase (Firebase) y PurchaseEntity (UI)
 */

/**
 * Convierte Firebase Purchase a PurchaseEntity (modelo de UI)
 */
fun Purchase.toPurchaseEntity(): PurchaseEntity {
    return PurchaseEntity(
        id = 0, // No se usa en la UI
        userEmail = this.userEmail,
        userName = this.userName,
        serviceId = this.serviceId,
        serviceName = this.serviceName,
        servicePrice = this.servicePrice,
        serviceDuration = this.serviceDuration,
        email = this.credentials?.email,
        password = this.credentials?.password,
        purchaseDate = this.purchaseDate.seconds * 1000,
        expirationDate = this.expirationDate.seconds * 1000,
        status = this.status,
        sincronizado = true,
        firebaseId = this.id
    )
}

/**
 * Convierte una lista de Firebase Purchase a lista de PurchaseEntity
 */
fun List<Purchase>.toPurchaseEntityList(): List<PurchaseEntity> {
    return this.map { it.toPurchaseEntity() }
}
