package com.universidad.streamzone.data.firebase.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.Timestamp
import com.universidad.streamzone.data.firebase.models.Category
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Repositorio para manejar categorías en Firebase Firestore
 */
class CategoryRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val categoriesCollection = firestore.collection("categories")

    /**
     * Inserta una nueva categoría
     * @return El ID de la categoría creada
     */
    suspend fun insert(category: Category): String {
        val docRef = categoriesCollection.document()
        val categoryWithId = category.copy(
            id = docRef.id,
            createdAt = Timestamp.now(),
            updatedAt = Timestamp.now()
        )
        docRef.set(categoryWithId.toMap()).await()
        return docRef.id
    }

    /**
     * Actualiza una categoría existente
     */
    suspend fun update(category: Category) {
        val categoryMap = category.toMap().toMutableMap()
        categoryMap["updatedAt"] = Timestamp.now()
        categoriesCollection.document(category.id).set(categoryMap).await()
    }

    /**
     * Elimina una categoría
     */
    suspend fun delete(category: Category) {
        categoriesCollection.document(category.id).delete().await()
    }

    /**
     * Obtiene una categoría por ID
     */
    suspend fun findById(categoryId: String): Category? {
        val doc = categoriesCollection.document(categoryId).get().await()
        return doc.data?.let { Category.fromMap(doc.id, it) }
    }

    /**
     * Obtiene una categoría por categoryId (ej: "streaming")
     */
    suspend fun findByCategoryId(categoryId: String): Category? {
        val snapshot = categoriesCollection
            .whereEqualTo("categoryId", categoryId)
            .limit(1)
            .get()
            .await()

        return snapshot.documents.firstOrNull()?.let { doc ->
            doc.data?.let { Category.fromMap(doc.id, it) }
        }
    }

    /**
     * Obtiene todas las categorías activas como Flow
     */
    fun getActiveCategories(): Flow<List<Category>> = callbackFlow {
        val listener = categoriesCollection
            .whereEqualTo("isActive", true)
            .orderBy("order", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val categories = snapshot?.documents?.mapNotNull { doc ->
                    doc.data?.let { Category.fromMap(doc.id, it) }
                } ?: emptyList()

                trySend(categories)
            }

        awaitClose { listener.remove() }
    }

    /**
     * Obtiene todas las categorías activas (versión síncrona)
     */
    suspend fun getActiveCategoriesSync(): List<Category> {
        val snapshot = categoriesCollection
            .whereEqualTo("isActive", true)
            .orderBy("order", Query.Direction.ASCENDING)
            .get()
            .await()

        return snapshot.documents.mapNotNull { doc ->
            doc.data?.let { Category.fromMap(doc.id, it) }
        }
    }

    /**
     * Obtiene todas las categorías como Flow
     */
    fun getAll(): Flow<List<Category>> = callbackFlow {
        val listener = categoriesCollection
            .orderBy("order", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val categories = snapshot?.documents?.mapNotNull { doc ->
                    doc.data?.let { Category.fromMap(doc.id, it) }
                } ?: emptyList()

                trySend(categories)
            }

        awaitClose { listener.remove() }
    }

    /**
     * Obtiene todas las categorías (versión síncrona)
     */
    suspend fun getAllSync(): List<Category> {
        val snapshot = categoriesCollection
            .orderBy("order", Query.Direction.ASCENDING)
            .get()
            .await()

        return snapshot.documents.mapNotNull { doc ->
            doc.data?.let { Category.fromMap(doc.id, it) }
        }
    }
}
