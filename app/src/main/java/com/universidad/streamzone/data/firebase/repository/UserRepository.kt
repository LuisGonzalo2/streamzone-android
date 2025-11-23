package com.universidad.streamzone.data.firebase.repository

import android.net.Uri
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.Timestamp
import com.universidad.streamzone.data.firebase.models.Permission
import com.universidad.streamzone.data.firebase.models.User
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Repositorio para manejar usuarios en Firebase Firestore
 */
class UserRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()

    private val usersCollection = firestore.collection("users")
    private val rolesCollection = firestore.collection("roles")
    private val permissionsCollection = firestore.collection("permissions")

    /**
     * Inserta un nuevo usuario
     * @return El ID del usuario creado
     */
    suspend fun insert(user: User): String {
        val docRef = usersCollection.document()
        val userWithId = user.copy(id = docRef.id, createdAt = Timestamp.now(), updatedAt = Timestamp.now())
        docRef.set(userWithId.toMap()).await()
        return docRef.id
    }

    /**
     * Actualiza un usuario existente
     */
    suspend fun update(user: User) {
        val userMap = user.toMap().toMutableMap()
        userMap["updatedAt"] = Timestamp.now()
        usersCollection.document(user.id).set(userMap).await()
    }

    /**
     * Obtiene todos los usuarios como Flow
     */
    fun getAll(): Flow<List<User>> = callbackFlow {
        val listener = usersCollection
            .orderBy("fullname")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val users = snapshot?.documents?.mapNotNull { doc ->
                    doc.data?.let { User.fromMap(doc.id, it) }
                } ?: emptyList()

                trySend(users)
            }

        awaitClose { listener.remove() }
    }

    /**
     * Busca un usuario por email
     */
    suspend fun findByEmail(email: String): User? {
        val snapshot = usersCollection
            .whereEqualTo("email", email)
            .limit(1)
            .get()
            .await()

        return snapshot.documents.firstOrNull()?.let { doc ->
            doc.data?.let { User.fromMap(doc.id, it) }
        }
    }

    /**
     * Busca un usuario por ID
     */
    suspend fun findById(userId: String): User? {
        val doc = usersCollection.document(userId).get().await()
        return doc.data?.let { User.fromMap(doc.id, it) }
    }

    /**
     * Busca un usuario por teléfono
     */
    suspend fun findByPhone(phone: String): User? {
        val snapshot = usersCollection
            .whereEqualTo("phone", phone)
            .limit(1)
            .get()
            .await()

        return snapshot.documents.firstOrNull()?.let { doc ->
            doc.data?.let { User.fromMap(doc.id, it) }
        }
    }

    /**
     * Obtiene todos los administradores como Flow
     */
    fun getAdministrators(): Flow<List<User>> = callbackFlow {
        val listener = usersCollection
            .whereEqualTo("isAdmin", true)
            .orderBy("fullname")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val users = snapshot?.documents?.mapNotNull { doc ->
                    doc.data?.let { User.fromMap(doc.id, it) }
                } ?: emptyList()

                trySend(users)
            }

        awaitClose { listener.remove() }
    }

    /**
     * Busca usuarios por nombre (query) como Flow
     */
    fun searchUsers(query: String): Flow<List<User>> = callbackFlow {
        val listener = usersCollection
            .orderBy("fullname")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val users = snapshot?.documents?.mapNotNull { doc ->
                    doc.data?.let { User.fromMap(doc.id, it) }
                }?.filter { user ->
                    user.fullname.contains(query, ignoreCase = true) ||
                    user.email.contains(query, ignoreCase = true) ||
                    user.phone.contains(query, ignoreCase = true)
                } ?: emptyList()

                trySend(users)
            }

        awaitClose { listener.remove() }
    }

    /**
     * Obtiene los permisos de un usuario
     */
    suspend fun getUserPermissions(userId: String): List<Permission> {
        val user = findById(userId) ?: return emptyList()

        if (user.roleIds.isEmpty()) return emptyList()

        // Obtener todos los roles del usuario
        val rolesSnapshot = rolesCollection
            .whereIn(FieldPath.documentId(), user.roleIds)
            .get()
            .await()

        // Recolectar todos los IDs de permisos de todos los roles
        val permissionIds = mutableSetOf<String>()
        rolesSnapshot.documents.forEach { roleDoc ->
            val permIds = roleDoc.get("permissionIds") as? List<*>
            permIds?.filterIsInstance<String>()?.let { permissionIds.addAll(it) }
        }

        if (permissionIds.isEmpty()) return emptyList()

        // Obtener todos los permisos
        val permissionsSnapshot = permissionsCollection
            .whereIn(FieldPath.documentId(), permissionIds.toList())
            .get()
            .await()

        return permissionsSnapshot.documents.mapNotNull { doc ->
            doc.data?.let { Permission.fromMap(doc.id, it) }
        }
    }

    /**
     * Verifica si un usuario tiene un permiso específico
     */
    suspend fun hasPermission(userId: String, permissionCode: String): Boolean {
        val permissions = getUserPermissions(userId)
        return permissions.any { it.code == permissionCode }
    }

    /**
     * Sube una foto de perfil y retorna la URL
     */
    suspend fun uploadProfilePhoto(userId: String, photoUri: Uri): String {
        val storageRef = storage.reference
            .child("users/$userId/profile.jpg")

        storageRef.putFile(photoUri).await()
        return storageRef.downloadUrl.await().toString()
    }

    /**
     * Elimina un usuario
     */
    suspend fun delete(userId: String) {
        usersCollection.document(userId).delete().await()
    }

    /**
     * Asigna roles a un usuario
     */
    suspend fun assignRoles(userId: String, roleIds: List<String>) {
        usersCollection.document(userId)
            .update("roleIds", roleIds, "updatedAt", Timestamp.now())
            .await()
    }

    /**
     * Obtiene los roles de un usuario
     */
    suspend fun getUserRoles(userId: String): List<String> {
        val user = findById(userId)
        return user?.roleIds ?: emptyList()
    }
}
