package com.universidad.streamzone.data.firebase.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.Timestamp
import com.universidad.streamzone.data.firebase.models.Role
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Repositorio para manejar roles en Firebase Firestore
 */
class RoleRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val rolesCollection = firestore.collection("roles")

    /**
     * Inserta un nuevo rol
     * @return El ID del rol creado
     */
    suspend fun insert(role: Role): String {
        val docRef = rolesCollection.document()
        val roleWithId = role.copy(
            id = docRef.id,
            createdAt = Timestamp.now(),
            updatedAt = Timestamp.now()
        )
        docRef.set(roleWithId.toMap()).await()
        return docRef.id
    }

    /**
     * Actualiza un rol existente
     */
    suspend fun update(role: Role) {
        val roleMap = role.toMap().toMutableMap()
        roleMap["updatedAt"] = Timestamp.now()
        rolesCollection.document(role.id).set(roleMap).await()
    }

    /**
     * Elimina un rol
     */
    suspend fun delete(role: Role) {
        rolesCollection.document(role.id).delete().await()
    }

    /**
     * Obtiene un rol por ID
     */
    suspend fun findById(roleId: String): Role? {
        val doc = rolesCollection.document(roleId).get().await()
        return doc.data?.let { Role.fromMap(doc.id, it) }
    }

    /**
     * Obtiene todos los roles activos como Flow
     */
    fun getActiveRoles(): Flow<List<Role>> = callbackFlow {
        val listener = rolesCollection
            .whereEqualTo("isActive", true)
            .orderBy("name")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val roles = snapshot?.documents?.mapNotNull { doc ->
                    doc.data?.let { Role.fromMap(doc.id, it) }
                } ?: emptyList()

                trySend(roles)
            }

        awaitClose { listener.remove() }
    }

    /**
     * Obtiene todos los roles como Flow
     */
    fun getAll(): Flow<List<Role>> = callbackFlow {
        val listener = rolesCollection
            .orderBy("name")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val roles = snapshot?.documents?.mapNotNull { doc ->
                    doc.data?.let { Role.fromMap(doc.id, it) }
                } ?: emptyList()

                trySend(roles)
            }

        awaitClose { listener.remove() }
    }

    /**
     * Asigna permisos a un rol
     */
    suspend fun assignPermissions(roleId: String, permissionIds: List<String>) {
        rolesCollection.document(roleId)
            .update("permissionIds", permissionIds, "updatedAt", Timestamp.now())
            .await()
    }

    /**
     * Obtiene los permisos de un rol
     */
    suspend fun getRolePermissions(roleId: String): List<String> {
        val role = findById(roleId)
        return role?.permissionIds ?: emptyList()
    }
}
