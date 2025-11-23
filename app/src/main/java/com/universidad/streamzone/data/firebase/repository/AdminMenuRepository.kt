package com.universidad.streamzone.data.firebase.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.Timestamp
import com.universidad.streamzone.data.firebase.models.AdminMenuOption
import kotlinx.coroutines.tasks.await

/**
 * Repositorio para manejar opciones de menú admin en Firebase Firestore
 */
class AdminMenuRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val adminMenuOptionsCollection = firestore.collection("admin_menu_options")

    /**
     * Inserta una nueva opción de menú
     * @return El ID de la opción creada
     */
    suspend fun insert(option: AdminMenuOption): String {
        val docRef = adminMenuOptionsCollection.document()
        val optionWithId = option.copy(
            id = docRef.id,
            createdAt = Timestamp.now()
        )
        docRef.set(optionWithId.toMap()).await()
        return docRef.id
    }

    /**
     * Actualiza una opción de menú
     */
    suspend fun update(option: AdminMenuOption) {
        adminMenuOptionsCollection.document(option.id)
            .set(option.toMap())
            .await()
    }

    /**
     * Obtiene una opción por ID
     */
    suspend fun findById(optionId: String): AdminMenuOption? {
        val doc = adminMenuOptionsCollection.document(optionId).get().await()
        return doc.data?.let { AdminMenuOption.fromMap(doc.id, it) }
    }

    /**
     * Obtiene todas las opciones activas
     */
    suspend fun getActiveOptions(): List<AdminMenuOption> {
        val snapshot = adminMenuOptionsCollection
            .whereEqualTo("isActive", true)
            .orderBy("orderIndex", Query.Direction.ASCENDING)
            .get()
            .await()

        return snapshot.documents.mapNotNull { doc ->
            doc.data?.let { AdminMenuOption.fromMap(doc.id, it) }
        }
    }

    /**
     * Obtiene todas las opciones
     */
    suspend fun getAll(): List<AdminMenuOption> {
        val snapshot = adminMenuOptionsCollection
            .orderBy("orderIndex", Query.Direction.ASCENDING)
            .get()
            .await()

        return snapshot.documents.mapNotNull { doc ->
            doc.data?.let { AdminMenuOption.fromMap(doc.id, it) }
        }
    }

    /**
     * Elimina una opción
     */
    suspend fun delete(optionId: String) {
        adminMenuOptionsCollection.document(optionId).delete().await()
    }
}
