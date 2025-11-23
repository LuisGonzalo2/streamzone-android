package com.universidad.streamzone.services

import android.content.Context
import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.universidad.streamzone.data.firebase.models.Notification
import com.universidad.streamzone.data.firebase.models.NotificationType
import com.universidad.streamzone.data.firebase.repository.NotificationRepository
import com.universidad.streamzone.util.NotificationManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Servicio que escucha cambios en tiempo real en Firebase Firestore
 * y genera notificaciones locales automáticamente
 */
class NotificationListenerService(private val context: Context) {

    private val TAG = "NotificationListener"
    private val db = FirebaseFirestore.getInstance()
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val notificationRepository = NotificationRepository()

    private var categoriesListener: ListenerRegistration? = null
    private var servicesListener: ListenerRegistration? = null
    private var offersListener: ListenerRegistration? = null
    private var purchasesListener: ListenerRegistration? = null
    private var userRolesListener: ListenerRegistration? = null

    // Map para rastrear documentos ya vistos (evitar notificaciones en la primera carga)
    private val seenCategories = mutableSetOf<String>()
    private val seenServices = mutableSetOf<String>()
    private val seenOffers = mutableSetOf<String>()
    private val seenPurchases = mutableSetOf<String>()
    private val seenUserRoles = mutableSetOf<String>()
    private var isFirstLoad = true

    /**
     * Inicia todos los listeners
     */
    fun startListening() {
        Log.d(TAG, "🚀 Iniciando listeners de notificaciones...")

        listenToCategories()
        listenToServices()
        listenToOffers()
        listenToPurchases()
        listenToUserRoles()

        // Marcar que la primera carga ya pasó después de 2 segundos
        serviceScope.launch {
            kotlinx.coroutines.delay(2000)
            isFirstLoad = false
            Log.d(TAG, "✅ isFirstLoad = false (ya pueden enviarse notificaciones)")
        }
    }

    /**
     * Detiene todos los listeners
     */
    fun stopListening() {
        categoriesListener?.remove()
        servicesListener?.remove()
        offersListener?.remove()
        purchasesListener?.remove()
        userRolesListener?.remove()
        Log.d(TAG, "Listeners de notificaciones detenidos")
    }

    /**
     * Escucha cambios en categorías
     */
    private fun listenToCategories() {
        categoriesListener = db.collection("categories")
            .addSnapshotListener { snapshots, error ->
                if (error != null) {
                    Log.e(TAG, "Error al escuchar categorías", error)
                    return@addSnapshotListener
                }

                snapshots?.documentChanges?.forEach { change ->
                    val categoryId = change.document.id
                    val categoryName = change.document.getString("name") ?: "Nueva categoría"
                    val categoryIcon = change.document.getString("icon") ?: "📁"
                    val isActive = change.document.getBoolean("isActive") ?: true

                    when (change.type) {
                        com.google.firebase.firestore.DocumentChange.Type.ADDED -> {
                            if (!seenCategories.contains(categoryId) && !isFirstLoad) {
                                createNotification(
                                    title = "Nueva categoría disponible",
                                    message = "Explora $categoryName y descubre nuevos servicios",
                                    type = NotificationType.CATEGORY,
                                    icon = categoryIcon,
                                    actionType = "open_category",
                                    actionData = categoryId
                                )
                            }
                            seenCategories.add(categoryId)
                        }
                        com.google.firebase.firestore.DocumentChange.Type.MODIFIED -> {
                            if (seenCategories.contains(categoryId) && isActive) {
                                createNotification(
                                    title = "Categoría actualizada",
                                    message = "La categoría $categoryName ha sido actualizada",
                                    type = NotificationType.CATEGORY,
                                    icon = categoryIcon
                                )
                            }
                        }
                        else -> {}
                    }
                }
            }
    }

    /**
     * Escucha cambios en servicios
     */
    private fun listenToServices() {
        servicesListener = db.collection("services")
            .addSnapshotListener { snapshots, error ->
                if (error != null) {
                    Log.e(TAG, "Error al escuchar servicios", error)
                    return@addSnapshotListener
                }

                snapshots?.documentChanges?.forEach { change ->
                    val serviceId = change.document.id
                    val serviceName = change.document.getString("name") ?: "Nuevo servicio"
                    val servicePrice = change.document.getString("price") ?: ""
                    val isActive = change.document.getBoolean("isActive") ?: true

                    when (change.type) {
                        com.google.firebase.firestore.DocumentChange.Type.ADDED -> {
                            if (!seenServices.contains(serviceId) && !isFirstLoad && isActive) {
                                createNotification(
                                    title = "Nuevo servicio disponible",
                                    message = "$serviceName ahora disponible por $servicePrice",
                                    type = NotificationType.SERVICE,
                                    icon = "⭐",
                                    actionType = "open_service",
                                    actionData = serviceId
                                )
                            }
                            seenServices.add(serviceId)
                        }
                        com.google.firebase.firestore.DocumentChange.Type.MODIFIED -> {
                            if (seenServices.contains(serviceId) && isActive) {
                                createNotification(
                                    title = "Servicio actualizado",
                                    message = "$serviceName - $servicePrice",
                                    type = NotificationType.SERVICE,
                                    icon = "📺"
                                )
                            }
                        }
                        else -> {}
                    }
                }
            }
    }

    /**
     * Escucha cambios en ofertas
     */
    private fun listenToOffers() {
        offersListener = db.collection("offers")
            .whereEqualTo("isActive", true)
            .addSnapshotListener { snapshots, error ->
                if (error != null) {
                    Log.e(TAG, "Error al escuchar ofertas", error)
                    return@addSnapshotListener
                }

                snapshots?.documentChanges?.forEach { change ->
                    val offerId = change.document.id
                    val offerTitle = change.document.getString("title") ?: "Nueva oferta"
                    val discount = change.document.getLong("discountPercent")?.toInt() ?: 0

                    when (change.type) {
                        com.google.firebase.firestore.DocumentChange.Type.ADDED -> {
                            if (!seenOffers.contains(offerId) && !isFirstLoad) {
                                createNotification(
                                    title = "¡Nueva oferta especial!",
                                    message = "$offerTitle - ¡Ahorra $discount%!",
                                    type = NotificationType.OFFER,
                                    icon = "🎯",
                                    actionType = "open_offer",
                                    actionData = offerId
                                )
                            }
                            seenOffers.add(offerId)
                        }
                        else -> {}
                    }
                }
            }
    }

    /**
     * Escucha cambios en compras
     */
    private fun listenToPurchases() {
        val sharedPrefs = context.getSharedPreferences("StreamZoneData", Context.MODE_PRIVATE)
        val userEmail = sharedPrefs.getString("logged_in_user_email", "") ?: ""

        if (userEmail.isEmpty()) {
            Log.d(TAG, "No hay usuario logueado, no se escucharán compras")
            return
        }

        Log.d(TAG, "Iniciando listener de compras para: $userEmail")

        purchasesListener = db.collection("purchases")
            .whereEqualTo("userEmail", userEmail)
            .addSnapshotListener { snapshots, error ->
                if (error != null) {
                    Log.e(TAG, "Error al escuchar compras", error)
                    return@addSnapshotListener
                }

                snapshots?.documentChanges?.forEach { change ->
                    val purchaseId = change.document.id
                    val serviceName = change.document.getString("serviceName") ?: "Servicio"
                    val status = change.document.getString("status") ?: "pending"

                    @Suppress("UNCHECKED_CAST")
                    val credentialsMap = change.document.get("credentials") as? Map<String, Any?>
                    val email = credentialsMap?.get("email") as? String
                    val password = credentialsMap?.get("password") as? String

                    when (change.type) {
                        com.google.firebase.firestore.DocumentChange.Type.ADDED -> {
                            Log.d(TAG, "➕ Nueva compra detectada: $serviceName")
                            seenPurchases.add(purchaseId)
                        }
                        com.google.firebase.firestore.DocumentChange.Type.MODIFIED -> {
                            Log.d(TAG, "🔔 Compra actualizada: $serviceName (status: $status)")

                            // Notificar cuando se asignan credenciales
                            if (seenPurchases.contains(purchaseId) &&
                                status == "active" &&
                                !email.isNullOrEmpty() &&
                                !password.isNullOrEmpty() &&
                                !isFirstLoad) {

                                Log.d(TAG, "📬 Enviando notificación de credenciales asignadas")
                                createNotification(
                                    title = "¡Credenciales asignadas!",
                                    message = "Tus credenciales de $serviceName están listas. Ve a 'Mis Compras' para verlas.",
                                    type = NotificationType.PURCHASE,
                                    icon = "✅",
                                    userId = userEmail,
                                    actionType = "open_purchases",
                                    actionData = purchaseId
                                )
                            }
                        }
                        else -> {}
                    }
                }
            }
    }

    /**
     * Escucha cambios en asignación de roles a usuarios
     */
    private fun listenToUserRoles() {
        val sharedPrefs = context.getSharedPreferences("StreamZoneData", Context.MODE_PRIVATE)
        val userEmail = sharedPrefs.getString("logged_in_user_email", "") ?: ""

        if (userEmail.isEmpty()) {
            Log.d(TAG, "No hay usuario logueado, no se escucharán roles")
            return
        }

        Log.d(TAG, "Iniciando listener de roles para: $userEmail")

        // Escuchar cambios en el usuario (sus roleIds)
        db.collection("users")
            .whereEqualTo("email", userEmail)
            .addSnapshotListener { snapshots, error ->
                if (error != null) {
                    Log.e(TAG, "Error al escuchar roles de usuario", error)
                    return@addSnapshotListener
                }

                snapshots?.documentChanges?.forEach { change ->
                    when (change.type) {
                        com.google.firebase.firestore.DocumentChange.Type.MODIFIED -> {
                            if (!isFirstLoad) {
                                createNotification(
                                    title = "¡Roles actualizados!",
                                    message = "Tus permisos han sido actualizados. Reinicia la app para verlos.",
                                    type = NotificationType.ROLE,
                                    icon = "👤",
                                    userId = userEmail,
                                    actionType = "refresh_permissions",
                                    actionData = null
                                )
                            }
                        }
                        else -> {}
                    }
                }
            }
    }

    /**
     * Crea y guarda una notificación en Firebase
     * y muestra una notificación push local
     */
    private fun createNotification(
        title: String,
        message: String,
        type: NotificationType,
        icon: String = "🔔",
        userId: String? = null,
        actionType: String? = null,
        actionData: String? = null
    ) {
        serviceScope.launch {
            try {
                val notification = Notification(
                    title = title,
                    message = message,
                    type = type,
                    timestamp = Timestamp.now(),
                    isRead = false,
                    userId = userId,
                    actionType = actionType,
                    actionData = actionData,
                    icon = icon,
                    createdAt = Timestamp.now()
                )

                notificationRepository.insert(notification)

                // Mostrar notificación local
                NotificationManager.showNotification(
                    context = context,
                    title = title,
                    message = message,
                    icon = icon
                )

                Log.d(TAG, "✅ Notificación creada: $title")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error al crear notificación", e)
            }
        }
    }
}
