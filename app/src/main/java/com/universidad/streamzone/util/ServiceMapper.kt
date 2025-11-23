package com.universidad.streamzone.util

import com.universidad.streamzone.data.model.Service
import com.universidad.streamzone.data.model.ServiceEntity

/**
 * Extensiones para convertir entre ServiceEntity (BD) y Service (UI)
 */

/**
 * Convierte ServiceEntity (base de datos) a Service (modelo de UI)
 */
fun ServiceEntity.toService(): Service {
    return Service(
        id = this.serviceId,
        title = this.name,
        price = this.price,
        desc = this.description,
        iconRes = this.iconDrawable,
        iconBase64 = this.iconBase64
    )
}

/**
 * Convierte una lista de ServiceEntity a lista de Service
 */
fun List<ServiceEntity>.toServiceList(): List<Service> {
    return this.map { it.toService() }
}

/**
 * Convierte Firebase Service a Service (modelo de UI)
 */
fun com.universidad.streamzone.data.firebase.models.Service.toUIService(): Service {
    return Service(
        id = this.serviceId,
        title = this.name,
        price = this.price,
        desc = this.description,
        iconRes = null, // Firebase usa URLs en lugar de recursos drawable
        iconBase64 = null // Firebase usa URLs en lugar de base64
    )
}

/**
 * Convierte una lista de Firebase Service a lista de Service (UI)
 */
fun List<com.universidad.streamzone.data.firebase.models.Service>.toUIServiceList(): List<Service> {
    return this.map { it.toUIService() }
}

/**
 * Convierte Firebase Service a ServiceEntity para administración
 */
fun com.universidad.streamzone.data.firebase.models.Service.toServiceEntity(): ServiceEntity {
    return ServiceEntity(
        id = this.id.hashCode(),
        serviceId = this.serviceId,
        name = this.name,
        price = this.price,
        description = this.description,
        iconBase64 = null,
        iconUrl = this.iconUrl,
        iconDrawable = null,
        categoryId = this.categoryId.hashCode(), // Convertir String ID a Int
        isActive = this.isActive,
        isPopular = this.isPopular,
        sincronizado = true,
        firebaseId = this.id
    )
}

/**
 * Convierte lista de Firebase Service a lista de ServiceEntity
 */
fun List<com.universidad.streamzone.data.firebase.models.Service>.toServiceEntityList(): List<ServiceEntity> {
    return this.map { it.toServiceEntity() }
}