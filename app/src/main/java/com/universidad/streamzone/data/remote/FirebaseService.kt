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
                callback(false) // En caso de error, asumimos que no existe
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
                callback(false) // En caso de error, asumimos que no existe
            }
    }

    // Guardar usuario con callback de éxito/error
    fun guardarUsuario(usuario: UsuarioEntity, onSuccess: (String) -> Unit, onFailure: (Exception) -> Unit) {
        try {
            Log.d(TAG, "Iniciando guardado en Firebase para: ${usuario.email}")

            val data = hashMapOf(
                "nombre" to usuario.fullname,
                "email" to usuario.email,
                "phone" to usuario.phone,
                "password" to usuario.password,
                "confirm_password" to usuario.confirmPassword
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
                        fullname = doc.getString("nombre") ?: "",
                        email = doc.getString("email") ?: "",
                        phone = doc.getString("phone") ?: "",
                        password = doc.getString("password") ?: "",
                        confirmPassword = doc.getString("confirm_password") ?: "",
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
                        fullname = doc.getString("nombre") ?: "",
                        email = doc.getString("email") ?: "",
                        phone = doc.getString("phone") ?: "",
                        password = doc.getString("password") ?: "",
                        confirmPassword = doc.getString("confirm_password") ?: "",
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
}