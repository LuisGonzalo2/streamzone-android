package com.universidad.streamzone.data.model

/**
 * Data class que representa una oferta con la información de los servicios incluidos
 */
data class OfferWithServices(
    val offer: OfferEntity,
    val services: List<Service>
) {
    /**
     * Obtiene los nombres de los servicios concatenados con " + "
     */
    fun getServicesNames(): String {
        return services.joinToString(" + ") { it.title }
    }

    /**
     * Verifica si la oferta está vigente
     */
    fun isValid(): Boolean {
        val now = System.currentTimeMillis()
        return offer.isActive && now >= offer.startDate && now <= offer.endDate
    }

    /**
     * Obtiene los días restantes de la oferta
     */
    fun getDaysRemaining(): Int {
        val now = System.currentTimeMillis()
        val diff = offer.endDate - now
        return (diff / (1000 * 60 * 60 * 24)).toInt()
    }
}