package com.universidad.streamzone.data.remote

import android.os.Handler
import android.os.Looper
import android.util.Log
import com.universidad.streamzone.data.model.UsuarioEntity
import com.google.firebase.firestore.FirebaseFirestore
import com.universidad.streamzone.data.model.PurchaseEntity
import com.google.firebase.firestore.FieldValue

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

    // ========================================
    // SINCRONIZACIÓN DE CATEGORÍAS
    // ========================================

    /**
     * Sincroniza una categoría con Firebase (crear o actualizar)
     */
    fun sincronizarCategoria(
        categoryId: String,
        name: String,
        icon: String,
        isActive: Boolean,
        order: Int,
        onSuccess: () -> Unit = {},
        onFailure: (Exception) -> Unit = {}
    ) {
        val data = hashMapOf(
            "name" to name,
            "icon" to icon,
            "isActive" to isActive,
            "order" to order,
            "timestamp" to FieldValue.serverTimestamp()
        )

        db.collection("categories")
            .document(categoryId)
            .set(data)
            .addOnSuccessListener {
                Log.d(TAG, "✅ Categoría sincronizada: $name")
                onSuccess()
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "❌ Error al sincronizar categoría", e)
                onFailure(e)
            }
    }

    // ========================================
    // SINCRONIZACIÓN DE SERVICIOS
    // ========================================

    /**
     * Sincroniza un servicio con Firebase (crear o actualizar)
     */
    fun sincronizarServicio(
        serviceId: String,
        name: String,
        description: String,
        categoryId: Int,
        price: String,
        duration: String,
        imageUrl: String?,
        isActive: Boolean,
        isPopular: Boolean,
        onSuccess: () -> Unit = {},
        onFailure: (Exception) -> Unit = {}
    ) {
        val data = hashMapOf(
            "name" to name,
            "description" to description,
            "categoryId" to categoryId,
            "price" to price,
            "duration" to duration,
            "imageUrl" to imageUrl,
            "isActive" to isActive,
            "isPopular" to isPopular,
            "timestamp" to FieldValue.serverTimestamp()
        )

        db.collection("services")
            .document(serviceId)
            .set(data)
            .addOnSuccessListener {
                Log.d(TAG, "✅ Servicio sincronizado: $name")
                onSuccess()
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "❌ Error al sincronizar servicio", e)
                onFailure(e)
            }
    }
    // ========================================
    // SINCRONIZACIÓN DE OFERTAS
    // ========================================

    /**
     * Guardar/actualizar una oferta en Firebase
     */
    fun sincronizarOferta(
        offer: com.universidad.streamzone.data.model.OfferEntity,
        onSuccess: (String) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        try {
            Log.d(TAG, "Sincronizando oferta: ${offer.title}")

            val data = hashMapOf(
                "title" to offer.title,
                "description" to offer.description,
                "serviceIds" to offer.serviceIds,
                "originalPrice" to offer.originalPrice,
                "comboPrice" to offer.comboPrice,
                "discountPercent" to offer.discountPercent,
                "startDate" to offer.startDate,
                "endDate" to offer.endDate,
                "isActive" to offer.isActive,
                "timestamp" to FieldValue.serverTimestamp()
            )

            if (offer.firebaseId.isNullOrEmpty()) {
                // Crear nueva oferta
                db.collection("offers")
                    .add(data)
                    .addOnSuccessListener { documentReference ->
                        Log.d(TAG, "✅ Oferta creada con ID: ${documentReference.id}")
                        onSuccess(documentReference.id)
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "❌ Error al crear oferta", e)
                        onFailure(e)
                    }
            } else {
                // Actualizar oferta existente
                db.collection("offers")
                    .document(offer.firebaseId!!)
                    .set(data)
                    .addOnSuccessListener {
                        Log.d(TAG, "✅ Oferta actualizada: ${offer.firebaseId}")
                        onSuccess(offer.firebaseId!!)
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "❌ Error al actualizar oferta", e)
                        onFailure(e)
                    }
            }

        } catch (e: Exception) {
            Log.e(TAG, "❌ Excepción al sincronizar oferta", e)
            onFailure(e)
        }
    }

    /**
     * Obtener todas las ofertas de Firebase
     */
    fun obtenerTodasLasOfertas(callback: (List<com.universidad.streamzone.data.model.OfferEntity>) -> Unit) {
        Log.d(TAG, "Obteniendo ofertas de Firebase...")

        db.collection("offers")
            .get()
            .addOnSuccessListener { result ->
                val ofertas = result.documents.mapNotNull { doc ->
                    try {
                        com.universidad.streamzone.data.model.OfferEntity(
                            id = 0, // Room asignará el ID
                            title = doc.getString("title") ?: "",
                            description = doc.getString("description") ?: "",
                            serviceIds = doc.getString("serviceIds") ?: "",
                            originalPrice = doc.getDouble("originalPrice") ?: 0.0,
                            comboPrice = doc.getDouble("comboPrice") ?: 0.0,
                            discountPercent = doc.getLong("discountPercent")?.toInt() ?: 0,
                            startDate = doc.getLong("startDate") ?: 0L,
                            endDate = doc.getLong("endDate") ?: 0L,
                            isActive = doc.getBoolean("isActive") ?: false,
                            sincronizado = true,
                            firebaseId = doc.id
                        )
                    } catch (e: Exception) {
                        Log.e(TAG, "Error al parsear oferta: ${e.message}")
                        null
                    }
                }

                Log.d(TAG, "✅ ${ofertas.size} ofertas obtenidas de Firebase")
                callback(ofertas)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "❌ Error al obtener ofertas de Firebase", e)
                callback(emptyList())
            }
    }

    /**
     * Eliminar una oferta de Firebase
     */
    fun eliminarOferta(
        firebaseId: String,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        db.collection("offers")
            .document(firebaseId)
            .delete()
            .addOnSuccessListener {
                Log.d(TAG, "✅ Oferta eliminada de Firebase")
                onSuccess()
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "❌ Error al eliminar oferta", e)
                onFailure(e)
            }
    }
}