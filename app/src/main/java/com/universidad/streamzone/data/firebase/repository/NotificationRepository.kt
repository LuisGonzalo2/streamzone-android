package com.universidad.streamzone.data.firebase.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.Timestamp
import com.universidad.streamzone.data.firebase.models.Notification
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx:coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Repositorio para manejar notificaciones en Firebase Firestore
 */
class NotificationRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val notificationsCollection = firestore.collection("notifications")

    /**
     * Inserta una nueva notificación
     * @return El ID de la notificación creada
     */
    suspend fun insert(notification: Notification): String {
        val docRef = notificationsCollection.document()
        val notificationWithId = notification.copy(
            id = docRef.id,
            createdAt = Timestamp.now()
        )
        docRef.set(notificationWithId.toMap()).await()
        return docRef.id
    }

    /**
     * Inserta múltiples notificaciones
     */
    suspend fun insertAll(notifications: List<Notification>) {
        val batch = firestore.batch()

        notifications.forEach { notification ->
            val docRef = notificationsCollection.document()
            val notificationWithId = notification.copy(
                id = docRef.id,
                createdAt = Timestamp.now()
            )
            batch.set(docRef, notificationWithId.toMap())
        }

        batch.commit().await()
    }

    /**
     * Actualiza una notificación
     */
    suspend fun update(notification: Notification) {
        notificationsCollection.document(notification.id)
            .set(notification.toMap())
            .await()
    }

    /**
     * Obtiene todas las notificaciones como Flow
     */
    fun getAll(): Flow<List<Notification>> = callbackFlow {
        val listener = notificationsCollection
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val notifications = snapshot?.documents?.mapNotNull { doc ->
                    doc.data?.let { Notification.fromMap(doc.id, it) }
                } ?: emptyList()

                trySend(notifications)
            }

        awaitClose { listener.remove() }
    }

    /**
     * Obtiene notificaciones para un usuario específico (o globales) como Flow
     */
    fun getNotificationsForUser(userId: String): Flow<List<Notification>> = callbackFlow {
        val listener = notificationsCollection
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val notifications = snapshot?.documents?.mapNotNull { doc ->
                    doc.data?.let { Notification.fromMap(doc.id, it) }
                }?.filter { notification ->
                    // Mostrar notificaciones globales (userId == null) o específicas del usuario
                    notification.userId == null || notification.userId == userId
                } ?: emptyList()

                trySend(notifications)
            }

        awaitClose { listener.remove() }
    }

    /**
     * Obtiene el conteo de notificaciones no leídas para un usuario
     */
    fun getUnreadCount(userId: String): Flow<Int> = callbackFlow {
        val listener = notificationsCollection
            .whereEqualTo("isRead", false)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val count = snapshot?.documents?.mapNotNull { doc ->
                    doc.data?.let { Notification.fromMap(doc.id, it) }
                }?.count { notification ->
                    notification.userId == null || notification.userId == userId
                } ?: 0

                trySend(count)
            }

        awaitClose { listener.remove() }
    }

    /**
     * Marca una notificación como leída
     */
    suspend fun markAsRead(notificationId: String) {
        notificationsCollection.document(notificationId)
            .update("isRead", true)
            .await()
    }

    /**
     * Marca todas las notificaciones de un usuario como leídas
     */
    suspend fun markAllAsRead(userId: String) {
        // Obtener todas las notificaciones no leídas del usuario
        val snapshot = notificationsCollection
            .whereEqualTo("isRead", false)
            .get()
            .await()

        val batch = firestore.batch()
        snapshot.documents.forEach { doc ->
            val notification = doc.data?.let { Notification.fromMap(doc.id, it) }
            if (notification != null && (notification.userId == null || notification.userId == userId)) {
                batch.update(doc.reference, "isRead", true)
            }
        }
        batch.commit().await()
    }

    /**
     * Elimina una notificación
     */
    suspend fun delete(notificationId: String) {
        notificationsCollection.document(notificationId).delete().await()
    }

    /**
     * Elimina notificaciones más antiguas que un timestamp
     */
    suspend fun deleteOlderThan(timestamp: Timestamp) {
        val snapshot = notificationsCollection
            .whereLessThan("timestamp", timestamp)
            .get()
            .await()

        val batch = firestore.batch()
        snapshot.documents.forEach { doc ->
            batch.delete(doc.reference)
        }
        batch.commit().await()
    }
}
