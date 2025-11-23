package com.universidad.streamzone.data.firebase.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.Timestamp
import com.universidad.streamzone.data.firebase.models.Purchase
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Repositorio para manejar compras en Firebase Firestore
 */
class PurchaseRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val purchasesCollection = firestore.collection("purchases")

    /**
     * Inserta una nueva compra
     * @return El ID de la compra creada
     */
    suspend fun insert(purchase: Purchase): String {
        val docRef = purchasesCollection.document()
        val purchaseWithId = purchase.copy(
            id = docRef.id,
            createdAt = Timestamp.now(),
            updatedAt = Timestamp.now()
        )
        docRef.set(purchaseWithId.toMap()).await()
        return docRef.id
    }

    /**
     * Actualiza una compra existente
     */
    suspend fun update(purchase: Purchase) {
        val purchaseMap = purchase.toMap().toMutableMap()
        purchaseMap["updatedAt"] = Timestamp.now()
        purchasesCollection.document(purchase.id).set(purchaseMap).await()
    }

    /**
     * Obtiene compras por usuario (email) como Flow
     */
    fun getPurchasesByUser(email: String): Flow<List<Purchase>> = callbackFlow {
        val listener = purchasesCollection
            .whereEqualTo("userEmail", email)
            .orderBy("purchaseDate", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val purchases = snapshot?.documents?.mapNotNull { doc ->
                    doc.data?.let { Purchase.fromMap(doc.id, it) }
                } ?: emptyList()

                trySend(purchases)
            }

        awaitClose { listener.remove() }
    }

    /**
     * Obtiene compras activas de un usuario como Flow
     */
    fun getActivePurchases(email: String): Flow<List<Purchase>> = callbackFlow {
        val listener = purchasesCollection
            .whereEqualTo("userEmail", email)
            .whereEqualTo("status", "active")
            .orderBy("expirationDate", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val purchases = snapshot?.documents?.mapNotNull { doc ->
                    doc.data?.let { Purchase.fromMap(doc.id, it) }
                } ?: emptyList()

                trySend(purchases)
            }

        awaitClose { listener.remove() }
    }

    /**
     * Actualiza el estado de una compra
     */
    suspend fun updateStatus(purchaseId: String, status: String) {
        purchasesCollection.document(purchaseId)
            .update("status", status, "updatedAt", Timestamp.now())
            .await()
    }

    /**
     * Obtiene una compra por ID
     */
    suspend fun findById(purchaseId: String): Purchase? {
        val doc = purchasesCollection.document(purchaseId).get().await()
        return doc.data?.let { Purchase.fromMap(doc.id, it) }
    }

    /**
     * Elimina compras expiradas
     */
    suspend fun deleteExpired(timestamp: Timestamp) {
        val snapshot = purchasesCollection
            .whereLessThan("expirationDate", timestamp)
            .get()
            .await()

        val batch = firestore.batch()
        snapshot.documents.forEach { doc ->
            batch.delete(doc.reference)
        }
        batch.commit().await()
    }

    /**
     * Obtiene los 3 servicios más populares (más comprados)
     */
    suspend fun getTopPopularServices(): List<ServicePopularityData> {
        val snapshot = purchasesCollection.get().await()

        // Agrupar por serviceId y contar
        val serviceCountMap = mutableMapOf<String, ServicePopularityData>()

        snapshot.documents.forEach { doc ->
            val serviceId = doc.getString("serviceId") ?: return@forEach
            val serviceName = doc.getString("serviceName") ?: return@forEach
            val servicePrice = doc.getString("servicePrice") ?: return@forEach

            if (serviceCountMap.containsKey(serviceId)) {
                serviceCountMap[serviceId] = serviceCountMap[serviceId]!!.copy(
                    purchaseCount = serviceCountMap[serviceId]!!.purchaseCount + 1
                )
            } else {
                serviceCountMap[serviceId] = ServicePopularityData(
                    serviceId = serviceId,
                    serviceName = serviceName,
                    servicePrice = servicePrice,
                    purchaseCount = 1
                )
            }
        }

        // Ordenar por cantidad de compras y tomar top 3
        return serviceCountMap.values
            .sortedByDescending { it.purchaseCount }
            .take(3)
    }

    /**
     * Obtiene todas las compras como Flow
     */
    fun getAll(): Flow<List<Purchase>> = callbackFlow {
        val listener = purchasesCollection
            .orderBy("purchaseDate", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val purchases = snapshot?.documents?.mapNotNull { doc ->
                    doc.data?.let { Purchase.fromMap(doc.id, it) }
                } ?: emptyList()

                trySend(purchases)
            }

        awaitClose { listener.remove() }
    }

    /**
     * Elimina una compra
     */
    suspend fun delete(purchaseId: String) {
        purchasesCollection.document(purchaseId).delete().await()
    }
}

/**
 * Data class para popularidad de servicios
 */
data class ServicePopularityData(
    val serviceId: String,
    val serviceName: String,
    val servicePrice: String,
    val purchaseCount: Int
)
