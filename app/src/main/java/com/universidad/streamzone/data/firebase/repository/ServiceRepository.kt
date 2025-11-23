package com.universidad.streamzone.data.firebase.repository

import android.net.Uri
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.Timestamp
import com.universidad.streamzone.data.firebase.models.Service
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Repositorio para manejar servicios en Firebase Firestore
 */
class ServiceRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()

    private val servicesCollection = firestore.collection("services")

    /**
     * Inserta un nuevo servicio
     * @return El ID del servicio creado
     */
    suspend fun insert(service: Service): String {
        val docRef = servicesCollection.document()
        val serviceWithId = service.copy(
            id = docRef.id,
            createdAt = Timestamp.now(),
            updatedAt = Timestamp.now()
        )
        docRef.set(serviceWithId.toMap()).await()
        return docRef.id
    }

    /**
     * Actualiza un servicio existente
     */
    suspend fun update(service: Service) {
        val serviceMap = service.toMap().toMutableMap()
        serviceMap["updatedAt"] = Timestamp.now()
        servicesCollection.document(service.id).set(serviceMap).await()
    }

    /**
     * Elimina un servicio
     */
    suspend fun delete(service: Service) {
        servicesCollection.document(service.id).delete().await()
    }

    /**
     * Obtiene un servicio por ID
     */
    suspend fun findById(serviceId: String): Service? {
        val doc = servicesCollection.document(serviceId).get().await()
        return doc.data?.let { Service.fromMap(doc.id, it) }
    }

    /**
     * Obtiene un servicio por serviceId (ej: "netflix")
     */
    suspend fun findByServiceId(serviceId: String): Service? {
        val snapshot = servicesCollection
            .whereEqualTo("serviceId", serviceId)
            .limit(1)
            .get()
            .await()

        return snapshot.documents.firstOrNull()?.let { doc ->
            doc.data?.let { Service.fromMap(doc.id, it) }
        }
    }

    /**
     * Obtiene todos los servicios activos como Flow
     */
    fun getActiveServices(): Flow<List<Service>> = callbackFlow {
        val listener = servicesCollection
            .whereEqualTo("isActive", true)
            .orderBy("name")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val services = snapshot?.documents?.mapNotNull { doc ->
                    doc.data?.let { Service.fromMap(doc.id, it) }
                } ?: emptyList()

                trySend(services)
            }

        awaitClose { listener.remove() }
    }

    /**
     * Obtiene servicios por categoría como Flow
     */
    fun getServicesByCategory(categoryId: String): Flow<List<Service>> = callbackFlow {
        val listener = servicesCollection
            .whereEqualTo("categoryId", categoryId)
            .whereEqualTo("isActive", true)
            .orderBy("name")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val services = snapshot?.documents?.mapNotNull { doc ->
                    doc.data?.let { Service.fromMap(doc.id, it) }
                } ?: emptyList()

                trySend(services)
            }

        awaitClose { listener.remove() }
    }

    /**
     * Obtiene servicios populares
     */
    suspend fun getPopularServices(): List<Service> {
        val snapshot = servicesCollection
            .whereEqualTo("isPopular", true)
            .whereEqualTo("isActive", true)
            .orderBy("name")
            .get()
            .await()

        return snapshot.documents.mapNotNull { doc ->
            doc.data?.let { Service.fromMap(doc.id, it) }
        }
    }

    /**
     * Obtiene todos los servicios como Flow
     */
    fun getAll(): Flow<List<Service>> = callbackFlow {
        val listener = servicesCollection
            .orderBy("name")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val services = snapshot?.documents?.mapNotNull { doc ->
                    doc.data?.let { Service.fromMap(doc.id, it) }
                } ?: emptyList()

                trySend(services)
            }

        awaitClose { listener.remove() }
    }

    /**
     * Sube un icono de servicio y retorna la URL
     */
    suspend fun uploadServiceIcon(serviceId: String, iconUri: Uri): String {
        val storageRef = storage.reference
            .child("services/$serviceId/icon.png")

        storageRef.putFile(iconUri).await()
        return storageRef.downloadUrl.await().toString()
    }

    /**
     * Obtiene todos los servicios (versión síncrona)
     */
    suspend fun getAllSync(): List<Service> {
        val snapshot = servicesCollection
            .orderBy("name")
            .get()
            .await()

        return snapshot.documents.mapNotNull { doc ->
            doc.data?.let { Service.fromMap(doc.id, it) }
        }
    }
}
