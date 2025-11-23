package com.universidad.streamzone.data.firebase.repository

import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.Timestamp
import com.universidad.streamzone.data.firebase.models.Permission
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Repositorio para manejar permisos en Firebase Firestore
 */
class PermissionRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val permissionsCollection = firestore.collection("permissions")

    /**
     * Inserta un nuevo permiso
     * @return El ID del permiso creado
     */
    suspend fun insert(permission: Permission): String {
        val docRef = permissionsCollection.document()
        val permissionWithId = permission.copy(
            id = docRef.id,
            createdAt = Timestamp.now()
        )
        docRef.set(permissionWithId.toMap()).await()
        return docRef.id
    }

    /**
     * Actualiza un permiso existente
     */
    suspend fun update(permission: Permission) {
        permissionsCollection.document(permission.id).set(permission.toMap()).await()
    }

    /**
     * Obtiene un permiso por ID
     */
    suspend fun findById(permissionId: String): Permission? {
        val doc = permissionsCollection.document(permissionId).get().await()
        return doc.data?.let { Permission.fromMap(doc.id, it) }
    }

    /**
     * Obtiene todos los permisos como Flow
     */
    fun getAll(): Flow<List<Permission>> = callbackFlow {
        val listener = permissionsCollection
            .orderBy("name")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val permissions = snapshot?.documents?.mapNotNull { doc ->
                    doc.data?.let { Permission.fromMap(doc.id, it) }
                } ?: emptyList()

                trySend(permissions)
            }

        awaitClose { listener.remove() }
    }

    /**
     * Obtiene todos los permisos (versión síncrona)
     */
    suspend fun getAllSync(): List<Permission> {
        val snapshot = permissionsCollection
            .orderBy("name")
            .get()
            .await()

        return snapshot.documents.mapNotNull { doc ->
            doc.data?.let { Permission.fromMap(doc.id, it) }
        }
    }

    /**
     * Obtiene permisos por IDs
     */
    suspend fun getPermissionsByIds(permissionIds: List<String>): List<Permission> {
        if (permissionIds.isEmpty()) return emptyList()

        val snapshot = permissionsCollection
            .whereIn(FieldPath.documentId(), permissionIds)
            .get()
            .await()

        return snapshot.documents.mapNotNull { doc ->
            doc.data?.let { Permission.fromMap(doc.id, it) }
        }
    }

    /**
     * Busca un permiso por código
     */
    suspend fun findByCode(code: String): Permission? {
        val snapshot = permissionsCollection
            .whereEqualTo("code", code)
            .limit(1)
            .get()
            .await()

        return snapshot.documents.firstOrNull()?.let { doc ->
            doc.data?.let { Permission.fromMap(doc.id, it) }
        }
    }

    /**
     * Elimina un permiso
     */
    suspend fun delete(permissionId: String) {
        permissionsCollection.document(permissionId).delete().await()
    }
}
