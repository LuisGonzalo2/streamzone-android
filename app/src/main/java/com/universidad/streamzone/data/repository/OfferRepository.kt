package com.universidad.streamzone.data.repository

import com.universidad.streamzone.data.local.dao.OfferDao
import com.universidad.streamzone.data.local.dao.ServiceDao
import com.universidad.streamzone.data.model.OfferEntity
import com.universidad.streamzone.data.model.OfferWithServices
import com.universidad.streamzone.data.model.Service

class OfferRepository(
    private val offerDao: OfferDao,
    private val serviceDao: ServiceDao
) {

    /**
     * Obtiene la oferta activa del mes con todos sus servicios
     */
    suspend fun getActiveOfferWithServices(): OfferWithServices? {
        val offer = offerDao.getActiveOffer() ?: return null
        val services = getServicesForOffer(offer)
        return OfferWithServices(offer, services)
    }

    /**
     * Obtiene una oferta por ID con sus servicios
     */
    suspend fun getOfferWithServices(offerId: Long): OfferWithServices? {
        val offer = offerDao.getById(offerId) ?: return null
        val services = getServicesForOffer(offer)
        return OfferWithServices(offer, services)
    }

    /**
     * Obtiene todas las ofertas con sus servicios
     */
    suspend fun getAllOffersWithServices(): List<OfferWithServices> {
        val offers = offerDao.getAll()
        return offers.map { offer ->
            val services = getServicesForOffer(offer)
            OfferWithServices(offer, services)
        }
    }

    /**
     * Obtiene los servicios asociados a una oferta
     */
    private suspend fun getServicesForOffer(offer: OfferEntity): List<Service> {
        // Parsear los IDs de servicios (formato: "1,5,8")
        val serviceIds = offer.serviceIds.split(",")
            .mapNotNull { it.trim().toLongOrNull() }

        // Obtener las entidades de servicios desde la BD
        val serviceEntities = serviceIds.mapNotNull { id ->
            serviceDao.getById(id)
        }

        // Convertir ServiceEntity a Service (data class para UI)
        return serviceEntities.map { entity ->
            Service(
                id = entity.serviceId,
                title = entity.name,
                price = entity.price,
                desc = entity.description,
                iconRes = entity.iconDrawable ?: 0
            )
        }
    }

    /**
     * Inserta o actualiza una oferta
     */
    suspend fun saveOffer(offer: OfferEntity): Long {
        return offerDao.insert(offer)
    }

    /**
     * Actualiza una oferta existente
     */
    suspend fun updateOffer(offer: OfferEntity) {
        offerDao.update(offer)
    }

    /**
     * Elimina una oferta
     */
    suspend fun deleteOffer(offer: OfferEntity) {
        offerDao.delete(offer)
    }

    /**
     * Elimina una oferta por ID
     */
    suspend fun deleteOfferById(offerId: Long) {
        offerDao.deleteById(offerId)
    }

    /**
     * Desactiva todas las ofertas (útil antes de activar una nueva)
     */
    suspend fun deactivateAllOffers() {
        offerDao.deactivateAll()
    }

    /**
     * Activa o desactiva una oferta específica
     */
    suspend fun setOfferActiveStatus(offerId: Long, isActive: Boolean) {
        offerDao.updateActiveStatus(offerId, isActive)
    }
}