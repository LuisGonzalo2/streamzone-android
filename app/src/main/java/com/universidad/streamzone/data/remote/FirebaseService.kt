package com.universidad.streamzone.data.remote

import android.os.Handler
import android.os.Looper
import android.util.Log
import com.universidad.streamzone.data.model.UsuarioEntity
import com.google.firebase.firestore.FirebaseFirestore
import com.universidad.streamzone.data.model.PurchaseEntity

object FirebaseService {
    private val db = FirebaseFirestore.getInstance()
    private const val TAG = "FirebaseService"

    // Verificar si un email ya existe en Firebase
    fun verificarEmailExiste(email: String, callback: (Boolean) -> Unit) {
        Log.d(TAG, "Verificando si email existe: $email")

        db.collection("usuarios")
            .whereEqualTo("email", email)
            .get()
            .addOnSuccessListener { documents ->
                val existe = !documents.isEmpty
                Log.d(TAG, "Email $email existe: $existe")
                callback(existe)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error al verificar email", e)
                callback(false)
            }
    }

    // Verificar si un teléfono ya existe en Firebase
    fun verificarTelefonoExiste(phone: String, callback: (Boolean) -> Unit) {
        Log.d(TAG, "Verificando si teléfono existe: $phone")

        db.collection("usuarios")
            .whereEqualTo("phone", phone)
            .get()
            .addOnSuccessListener { documents ->
                val existe = !documents.isEmpty
                Log.d(TAG, "Teléfono $phone existe: $existe")
                callback(existe)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error al verificar teléfono", e)
                callback(false)
            }
    }

    // Guardar usuario con callback de éxito/error
    fun guardarUsuario(usuario: UsuarioEntity, onSuccess: (String) -> Unit, onFailure: (Exception) -> Unit) {
        try {
            Log.d(TAG, "Iniciando guardado en Firebase para: ${usuario.email}")

            val data = hashMapOf(
                "fullname" to usuario.fullname,
                "email" to usuario.email,
                "phone" to usuario.phone,
                "password" to usuario.password,
                "confirmPassword" to usuario.confirmPassword,
                "fotoBase64" to usuario.fotoBase64,
                "isAdmin" to usuario.isAdmin
            )

            // Timeout de 10 segundos
            val timeoutHandler = Handler(Looper.getMainLooper())
            var completed = false

            timeoutHandler.postDelayed({
                if (!completed) {
                    Log.e(TAG, "Timeout al guardar en Firebase")
                    onFailure(Exception("Timeout: Firebase no respondió en 10 segundos"))
                }
            }, 10000)

            db.collection("usuarios")
                .add(data)
                .addOnSuccessListener { documentReference ->
                    completed = true
                    timeoutHandler.removeCallbacksAndMessages(null)
                    Log.d(TAG, "Usuario guardado correctamente en Firestore con ID: ${documentReference.id}")
                    onSuccess(documentReference.id)
                }
                .addOnFailureListener { e ->
                    completed = true
                    timeoutHandler.removeCallbacksAndMessages(null)
                    Log.e(TAG, "Error al guardar en Firestore", e)
                    onFailure(e)
                }
        } catch (e: Exception) {
            Log.e(TAG, "Excepción al intentar guardar en Firebase", e)
            onFailure(e)
        }
    }

    // Verificar si un usuario existe en Firebase por email
    fun verificarUsuarioPorEmail(email: String, callback: (UsuarioEntity?) -> Unit) {
        Log.d(TAG, "Verificando usuario por email: $email")

        db.collection("usuarios")
            .whereEqualTo("email", email)
            .get()
            .addOnSuccessListener { documents ->
                Log.d(TAG, "Documentos encontrados: ${documents.size()}")

                if (documents.isEmpty) {
                    Log.d(TAG, "No se encontró usuario con email: $email")
                    callback(null)
                } else {
                    val doc = documents.documents[0]
                    Log.d(TAG, "Documento encontrado: ${doc.id}")
                    Log.d(TAG, "Datos: ${doc.data}")

                    val usuario = UsuarioEntity(
                        id = 0,
                        fullname = doc.getString("fullname") ?: "",
                        email = doc.getString("email") ?: "",
                        phone = doc.getString("phone") ?: "",
                        password = doc.getString("password") ?: "",
                        confirmPassword = doc.getString("confirmPassword") ?: "",
                        fotoBase64 = doc.getString("fotoBase64"),
                        isAdmin = doc.getBoolean("isAdmin") ?: false,
                        sincronizado = true,
                        firebaseId = doc.id
                    )

                    Log.d(TAG, "Usuario creado desde Firebase: $usuario")
                    callback(usuario)
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error al verificar usuario", e)
                callback(null)
            }
    }

    // Obtener todos los usuarios
    fun obtenerUsuarios(callback: (List<UsuarioEntity>) -> Unit) {
        db.collection("usuarios")
            .get()
            .addOnSuccessListener { result ->
                val lista = result.map { doc ->
                    UsuarioEntity(
                        id = 0,
                        fullname = doc.getString("fullname") ?: "",
                        email = doc.getString("email") ?: "",
                        phone = doc.getString("phone") ?: "",
                        password = doc.getString("password") ?: "",
                        confirmPassword = doc.getString("confirmPassword") ?: "",
                        fotoBase64 = doc.getString("fotoBase64"),
                        isAdmin = doc.getBoolean("isAdmin") ?: false,
                        sincronizado = true,
                        firebaseId = doc.id
                    )
                }
                callback(lista)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error al obtener usuarios", e)
                callback(emptyList())
            }
    }

    // ========================================
    // GESTIÓN DE COMPRAS
    // ========================================

    fun guardarCompra(purchase: PurchaseEntity, onSuccess: (String) -> Unit, onFailure: (Exception) -> Unit) {
        try {
            Log.d(TAG, "Guardando compra en Firebase: ${purchase.serviceName} para ${purchase.userEmail}")

            val data = hashMapOf(
                "userEmail" to purchase.userEmail,
                "userName" to purchase.userName,
                "serviceId" to purchase.serviceId,
                "serviceName" to purchase.serviceName,
                "servicePrice" to purchase.servicePrice,
                "serviceDuration" to purchase.serviceDuration,
                "email" to purchase.email,
                "password" to purchase.password,
                "purchaseDate" to purchase.purchaseDate,
                "expirationDate" to purchase.expirationDate,
                "status" to purchase.status
            )

            db.collection("purchases")
                .add(data)
                .addOnSuccessListener { documentReference ->
                    Log.d(TAG, "Compra guardada en Firestore con ID: ${documentReference.id}")
                    onSuccess(documentReference.id)
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Error al guardar compra en Firestore", e)
                    onFailure(e)
                }
        } catch (e: Exception) {
            Log.e(TAG, "Excepción al guardar compra", e)
            onFailure(e)
        }
    }

    fun obtenerComprasPorUsuario(email: String, callback: (List<PurchaseEntity>) -> Unit) {
        db.collection("purchases")
            .whereEqualTo("userEmail", email)
            .orderBy("purchaseDate", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { result ->
                val compras = result.map { doc ->
                    PurchaseEntity(
                        id = 0,
                        userEmail = doc.getString("userEmail") ?: "",
                        userName = doc.getString("userName") ?: "",
                        serviceId = doc.getString("serviceId") ?: "",
                        serviceName = doc.getString("serviceName") ?: "",
                        servicePrice = doc.getString("servicePrice") ?: "",
                        serviceDuration = doc.getString("serviceDuration") ?: "",
                        email = doc.getString("email"),
                        password = doc.getString("password"),
                        purchaseDate = doc.getLong("purchaseDate") ?: 0L,
                        expirationDate = doc.getLong("expirationDate") ?: 0L,
                        status = doc.getString("status") ?: "active",
                        sincronizado = true,
                        firebaseId = doc.id
                    )
                }
                callback(compras)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error al obtener compras", e)
                callback(emptyList())
            }
    }

    // ACTUALIZAR USUARIO
    fun actualizarUsuario(usuario: UsuarioEntity, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        try {
            if (usuario.firebaseId.isNullOrEmpty()) {
                Log.w(TAG, "Usuario no tiene firebaseId, buscando por email...")
                // Buscar el documento por email
                db.collection("usuarios")
                    .whereEqualTo("email", usuario.email)
                    .get()
                    .addOnSuccessListener { documents ->
                        if (documents.isEmpty) {
                            Log.w(TAG, "Usuario no encontrado en Firebase, creando nuevo...")
                            // Si no existe, crearlo
                            guardarUsuario(
                                usuario,
                                onSuccess = { firebaseId ->
                                    Log.d(TAG, "Usuario creado con ID: $firebaseId")
                                    onSuccess()
                                },
                                onFailure = onFailure
                            )
                        } else {
                            // Actualizar el existente
                            val docId = documents.documents[0].id
                            actualizarUsuarioPorId(docId, usuario, onSuccess, onFailure)
                        }
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "Error al buscar usuario", e)
                        onFailure(e)
                    }
            } else {
                // Ya tiene firebaseId, actualizar directamente
                actualizarUsuarioPorId(usuario.firebaseId!!, usuario, onSuccess, onFailure)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Excepción al actualizar usuario", e)
            onFailure(e)
        }
    }
    /**
     * Obtiene todos los usuarios de Firebase
     * Útil para sincronizar usuarios al abrir la app desde otro dispositivo
     */
    fun obtenerTodosLosUsuarios(callback: (List<UsuarioEntity>) -> Unit) {
        Log.d(TAG, "Obteniendo todos los usuarios de Firebase...")

        db.collection("usuarios")
            .get()
            .addOnSuccessListener { result ->
                val usuarios = result.documents.mapNotNull { doc ->
                    try {
                        UsuarioEntity(
                            id = 0, // Room asignará el ID
                            fullname = doc.getString("fullname") ?: "",
                            email = doc.getString("email") ?: "",
                            phone = doc.getString("phone") ?: "",
                            password = doc.getString("password") ?: "",
                            confirmPassword = doc.getString("confirmPassword") ?: "",
                            fotoBase64 = doc.getString("fotoBase64"),
                            isAdmin = doc.getBoolean("isAdmin") ?: false,
                            sincronizado = true,
                            firebaseId = doc.id
                        )
                    } catch (e: Exception) {
                        Log.e(TAG, "Error al parsear usuario: ${e.message}")
                        null
                    }
                }

                Log.d(TAG, "✅ ${usuarios.size} usuarios obtenidos de Firebase")
                callback(usuarios)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error al obtener usuarios de Firebase", e)
                callback(emptyList())
            }
    }

    /**
     * Sincroniza los roles de un usuario a Firebase
     * Los guarda como un array de IDs dentro del documento del usuario
     */
    fun sincronizarRolesUsuario(
        userEmail: String,
        roleIds: List<Int>,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        Log.d(TAG, "Sincronizando roles para $userEmail: $roleIds")

        // Buscar el documento del usuario
        db.collection("usuarios")
            .whereEqualTo("email", userEmail)
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    Log.w(TAG, "Usuario no encontrado en Firebase")
                    onFailure(Exception("Usuario no encontrado"))
                    return@addOnSuccessListener
                }

                val docId = documents.documents[0].id

                // Actualizar el campo roleIds
                db.collection("usuarios")
                    .document(docId)
                    .update("roleIds", roleIds)
                    .addOnSuccessListener {
                        Log.d(TAG, "✅ Roles sincronizados para $userEmail")
                        onSuccess()
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "Error al sincronizar roles", e)
                        onFailure(e)
                    }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error al buscar usuario", e)
                onFailure(e)
            }
    }

    /**
     * Obtiene los roles de un usuario desde Firebase
     */
    fun obtenerRolesUsuario(
        userEmail: String,
        onSuccess: (List<Int>) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        db.collection("usuarios")
            .whereEqualTo("email", userEmail)
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    onSuccess(emptyList())
                    return@addOnSuccessListener
                }

                val doc = documents.documents[0]
                val roleIds = (doc.get("roleIds") as? List<*>)
                    ?.mapNotNull {
                        when (it) {
                            is Long -> it.toInt()
                            is Int -> it
                            is String -> it.toIntOrNull()
                            else -> null
                        }
                    } ?: emptyList()

                Log.d(TAG, "Roles obtenidos para $userEmail: $roleIds")
                onSuccess(roleIds)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error al obtener roles", e)
                onFailure(e)
            }
    }

    private fun actualizarUsuarioPorId(
        docId: String,
        usuario: UsuarioEntity,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val data = hashMapOf(
            "fullname" to usuario.fullname,
            "email" to usuario.email,
            "phone" to usuario.phone,
            "password" to usuario.password,
            "confirmPassword" to usuario.confirmPassword,
            "fotoBase64" to usuario.fotoBase64,
            "isAdmin" to usuario.isAdmin
        )

        db.collection("usuarios")
            .document(docId)
            .update(data as Map<String, Any>)
            .addOnSuccessListener {
                Log.d(TAG, "Usuario actualizado en Firestore con ID: $docId")
                onSuccess()
            }
            .addOnFailureListener { e ->
                // Si el documento no existe (NOT_FOUND), crearlo con el mismo ID
                if (e.message?.contains("NOT_FOUND") == true) {
                    Log.w(TAG, "Documento no encontrado, creando nuevo con ID: $docId")
                    db.collection("usuarios")
                        .document(docId)
                        .set(data)
                        .addOnSuccessListener {
                            Log.d(TAG, "Usuario creado en Firestore con ID: $docId")
                            onSuccess()
                        }
                        .addOnFailureListener { createError ->
                            Log.e(TAG, "Error al crear usuario en Firestore", createError)
                            onFailure(createError)
                        }
                } else {
                    Log.e(TAG, "Error al actualizar usuario en Firestore", e)
                    onFailure(e)
                }
            }
    }
}