package com.universidad.streamzone.data.firebase.repository

import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.Timestamp
import com.universidad.streamzone.data.firebase.models.Offer
import com.universidad.streamzone.data.firebase.models.Service
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Repositorio para manejar ofertas en Firebase Firestore
 */
class OfferRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val offersCollection = firestore.collection("offers")
    private val servicesCollection = firestore.collection("services")

    /**
     * Inserta una nueva oferta
     * @return El ID de la oferta creada
     */
    suspend fun insert(offer: Offer): String {
        val docRef = offersCollection.document()
        val offerWithId = offer.copy(
            id = docRef.id,
            createdAt = Timestamp.now(),
            updatedAt = Timestamp.now()
        )
        docRef.set(offerWithId.toMap()).await()
        return docRef.id
    }

    /**
     * Actualiza una oferta existente
     */
    suspend fun update(offer: Offer) {
        val offerMap = offer.toMap().toMutableMap()
        offerMap["updatedAt"] = Timestamp.now()
        offersCollection.document(offer.id).set(offerMap).await()
    }

    /**
     * Elimina una oferta
     */
    suspend fun delete(offer: Offer) {
        offersCollection.document(offer.id).delete().await()
    }

    /**
     * Elimina una oferta por ID
     */
    suspend fun deleteById(offerId: String) {
        offersCollection.document(offerId).delete().await()
    }

    /**
     * Obtiene una oferta por ID
     */
    suspend fun findById(offerId: String): Offer? {
        val doc = offersCollection.document(offerId).get().await()
        return doc.data?.let { Offer.fromMap(doc.id, it) }
    }

    /**
     * Obtiene todas las ofertas
     */
    suspend fun getAll(): List<Offer> {
        val snapshot = offersCollection
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .get()
            .await()

        return snapshot.documents.mapNotNull { doc ->
            doc.data?.let { Offer.fromMap(doc.id, it) }
        }
    }

    /**
     * Obtiene la oferta activa vigente (si existe)
     */
    suspend fun getActiveOffer(): Offer? {
        val now = Timestamp.now()
        val snapshot = offersCollection
            .whereEqualTo("isActive", true)
            .whereGreaterThanOrEqualTo("endDate", now)
            .whereLessThanOrEqualTo("startDate", now)
            .limit(1)
            .get()
            .await()

        return snapshot.documents.firstOrNull()?.let { doc ->
            doc.data?.let { Offer.fromMap(doc.id, it) }
        }
    }

    /**
     * Obtiene todas las ofertas activas
     */
    suspend fun getAllActive(): List<Offer> {
        val snapshot = offersCollection
            .whereEqualTo("isActive", true)
            .orderBy("startDate", Query.Direction.DESCENDING)
            .get()
            .await()

        return snapshot.documents.mapNotNull { doc ->
            doc.data?.let { Offer.fromMap(doc.id, it) }
        }
    }

    /**
     * Desactiva todas las ofertas
     */
    suspend fun deactivateAll() {
        val snapshot = offersCollection
            .whereEqualTo("isActive", true)
            .get()
            .await()

        val batch = firestore.batch()
        snapshot.documents.forEach { doc ->
            batch.update(doc.reference, "isActive", false, "updatedAt", Timestamp.now())
        }
        batch.commit().await()
    }

    /**
     * Actualiza el estado activo de una oferta
     */
    suspend fun updateActiveStatus(offerId: String, isActive: Boolean) {
        offersCollection.document(offerId)
            .update("isActive", isActive, "updatedAt", Timestamp.now())
            .await()
    }

    /**
     * Obtiene una oferta con sus servicios asociados
     */
    suspend fun getOfferWithServices(offerId: String): OfferWithServicesData? {
        val offer = findById(offerId) ?: return null
        val services = getServicesForOffer(offer)
        return OfferWithServicesData(offer, services)
    }

    /**
     * Obtiene la oferta activa con sus servicios
     */
    suspend fun getActiveOfferWithServices(): OfferWithServicesData? {
        val offer = getActiveOffer() ?: return null
        val services = getServicesForOffer(offer)
        return OfferWithServicesData(offer, services)
    }

    /**
     * Obtiene todas las ofertas con sus servicios
     */
    suspend fun getAllOffersWithServices(): List<OfferWithServicesData> {
        val offers = getAll()
        return offers.mapNotNull { offer ->
            val services = getServicesForOffer(offer)
            OfferWithServicesData(offer, services)
        }
    }

    /**
     * Obtiene los servicios de una oferta
     */
    private suspend fun getServicesForOffer(offer: Offer): List<Service> {
        if (offer.serviceIds.isEmpty()) return emptyList()

        val snapshot = servicesCollection
            .whereIn(FieldPath.documentId(), offer.serviceIds)
            .get()
            .await()

        return snapshot.documents.mapNotNull { doc ->
            doc.data?.let { Service.fromMap(doc.id, it) }
        }
    }
}

/**
 * Data class para una oferta con sus servicios
 */
data class OfferWithServicesData(
    val offer: Offer,
    val services: List<Service>
) {
    /**
     * Obtiene los nombres de los servicios separados por coma
     */
    fun getServicesNames(): String {
        return services.joinToString(", ") { it.name }
    }

    /**
     * Verifica si la oferta es válida
     */
    fun isValid(): Boolean {
        return offer.isValid()
    }

    /**
     * Obtiene los días restantes
     */
    fun getDaysRemaining(): Int {
        return offer.getDaysRemaining()
    }
}
