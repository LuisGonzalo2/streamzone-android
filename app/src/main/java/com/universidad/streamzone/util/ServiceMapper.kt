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