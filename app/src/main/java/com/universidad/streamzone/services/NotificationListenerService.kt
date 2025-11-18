package com.universidad.streamzone.services

import android.content.Context
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.universidad.streamzone.data.local.database.AppDatabase
import com.universidad.streamzone.data.model.CategoryEntity
import com.universidad.streamzone.data.model.NotificationEntity
import com.universidad.streamzone.data.model.NotificationType
import com.universidad.streamzone.data.model.ServiceEntity
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
        listenToCategories()
        listenToServices()
        listenToOffers()
        listenToPurchases()
        listenToUserRoles()

        // Marcar que la primera carga ya pasó después de 2 segundos
        serviceScope.launch {
            kotlinx.coroutines.delay(2000)
            isFirstLoad = false
        }

        Log.d(TAG, "Listeners de notificaciones iniciados")
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
                            // Sincronizar a base de datos local
                            serviceScope.launch {
                                try {
                                    val appDb = AppDatabase.getInstance(context)
                                    val categoryDao = appDb.categoryDao()

                                    // Verificar si ya existe
                                    val existingCategory = categoryDao.obtenerPorCategoryId(categoryId)
                                    if (existingCategory == null) {
                                        val newCategory = CategoryEntity(
                                            id = 0,
                                            categoryId = categoryId,
                                            name = categoryName,
                                            icon = categoryIcon,
                                            description = "",
                                            gradientStart = "#1E3A8A",
                                            gradientEnd = "#3B82F6",
                                            isActive = isActive
                                        )
                                        categoryDao.insertar(newCategory)
                                        Log.d(TAG, "✅ Categoría sincronizada desde Firebase: $categoryName")
                                    }
                                } catch (e: Exception) {
                                    Log.e(TAG, "Error al sincronizar categoría", e)
                                }
                            }

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
                            // Actualizar en base de datos local
                            serviceScope.launch {
                                try {
                                    val appDb = AppDatabase.getInstance(context)
                                    val categoryDao = appDb.categoryDao()
                                    val existingCategory = categoryDao.obtenerPorCategoryId(categoryId)
                                    if (existingCategory != null) {
                                        val updatedCategory = existingCategory.copy(
                                            name = categoryName,
                                            icon = categoryIcon,
                                            isActive = isActive
                                        )
                                        categoryDao.actualizar(updatedCategory)
                                        Log.d(TAG, "✅ Categoría actualizada desde Firebase: $categoryName")
                                    }
                                } catch (e: Exception) {
                                    Log.e(TAG, "Error al actualizar categoría", e)
                                }
                            }

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
                    val serviceDescription = change.document.getString("description") ?: ""
                    val servicePrice = change.document.getString("price") ?: ""
                    val isActive = change.document.getBoolean("isActive") ?: true
                    val isPopular = change.document.getBoolean("isPopular") ?: false
                    val categoryId = change.document.getLong("categoryId")?.toInt() ?: 0

                    when (change.type) {
                        com.google.firebase.firestore.DocumentChange.Type.ADDED -> {
                            // Sincronizar a base de datos local
                            serviceScope.launch {
                                try {
                                    val appDb = AppDatabase.getInstance(context)
                                    val serviceDao = appDb.serviceDao()

                                    // Verificar si ya existe
                                    val existingService = serviceDao.obtenerPorServiceId(serviceId)
                                    if (existingService == null) {
                                        val newService = ServiceEntity(
                                            id = 0,
                                            serviceId = serviceId,
                                            name = serviceName,
                                            description = serviceDescription,
                                            price = servicePrice,
                                            iconDrawable = null,
                                            iconBase64 = change.document.getString("iconBase64"),
                                            iconUrl = change.document.getString("imageUrl"),
                                            categoryId = categoryId,
                                            isActive = isActive,
                                            isPopular = isPopular
                                        )
                                        serviceDao.insertar(newService)
                                        Log.d(TAG, "✅ Servicio sincronizado desde Firebase: $serviceName (con imagen)")
                                    }
                                } catch (e: Exception) {
                                    Log.e(TAG, "Error al sincronizar servicio", e)
                                }
                            }

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
                            // Actualizar en base de datos local
                            serviceScope.launch {
                                try {
                                    val appDb = AppDatabase.getInstance(context)
                                    val serviceDao = appDb.serviceDao()
                                    val existingService = serviceDao.obtenerPorServiceId(serviceId)
                                    if (existingService != null) {
                                        val updatedService = existingService.copy(
                                            name = serviceName,
                                            description = serviceDescription,
                                            price = servicePrice,
                                            iconBase64 = change.document.getString("iconBase64"),
                                            iconUrl = change.document.getString("imageUrl"),
                                            isActive = isActive,
                                            isPopular = isPopular
                                        )
                                        serviceDao.actualizar(updatedService)
                                        Log.d(TAG, "✅ Servicio actualizado desde Firebase: $serviceName (con imagen)")
                                    }
                                } catch (e: Exception) {
                                    Log.e(TAG, "Error al actualizar servicio", e)
                                }
                            }

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
        // Obtener el email del usuario actual desde SharedPreferences
        val sharedPrefs = context.getSharedPreferences("StreamZoneData", Context.MODE_PRIVATE)
        val userEmail = sharedPrefs.getString("USER_EMAIL", "") ?: ""

        if (userEmail.isEmpty()) {
            Log.d(TAG, "No hay usuario logueado, no se escucharán compras")
            return
        }

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
                    val email = change.document.getString("email")
                    val password = change.document.getString("password")

                    when (change.type) {
                        com.google.firebase.firestore.DocumentChange.Type.ADDED -> {
                            seenPurchases.add(purchaseId)
                        }
                        com.google.firebase.firestore.DocumentChange.Type.MODIFIED -> {
                            // Notificar cuando se asignan credenciales
                            if (seenPurchases.contains(purchaseId) &&
                                status == "completed" &&
                                !email.isNullOrEmpty() &&
                                !password.isNullOrEmpty()) {
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
        // Obtener el email del usuario actual desde SharedPreferences
        val sharedPrefs = context.getSharedPreferences("StreamZoneData", Context.MODE_PRIVATE)
        val userEmail = sharedPrefs.getString("USER_EMAIL", "") ?: ""

        if (userEmail.isEmpty()) {
            Log.d(TAG, "No hay usuario logueado, no se escucharán roles")
            return
        }

        userRolesListener = db.collection("user_roles")
            .whereEqualTo("userEmail", userEmail)
            .addSnapshotListener { snapshots, error ->
                if (error != null) {
                    Log.e(TAG, "Error al escuchar roles de usuario", error)
                    return@addSnapshotListener
                }

                snapshots?.documentChanges?.forEach { change ->
                    val userRoleId = change.document.id
                    val roleName = change.document.getString("roleName") ?: "Rol"

                    when (change.type) {
                        com.google.firebase.firestore.DocumentChange.Type.ADDED -> {
                            if (!seenUserRoles.contains(userRoleId) && !isFirstLoad) {
                                createNotification(
                                    title = "¡Nuevo rol asignado!",
                                    message = "Se te ha asignado el rol de $roleName. Ahora tienes nuevos permisos.",
                                    type = NotificationType.ROLE,
                                    icon = "👤",
                                    userId = userEmail,
                                    actionType = "refresh_permissions",
                                    actionData = roleName
                                )
                            }
                            seenUserRoles.add(userRoleId)
                        }
                        com.google.firebase.firestore.DocumentChange.Type.REMOVED -> {
                            if (seenUserRoles.contains(userRoleId)) {
                                createNotification(
                                    title = "Rol removido",
                                    message = "El rol de $roleName ha sido removido de tu cuenta.",
                                    type = NotificationType.ROLE,
                                    icon = "⚠️",
                                    userId = userEmail
                                )
                                seenUserRoles.remove(userRoleId)
                            }
                        }
                        else -> {}
                    }
                }
            }
    }

    /**
     * Crea y guarda una notificación en la base de datos local
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
                val appDb = AppDatabase.getInstance(context)
                val notificationDao = appDb.notificationDao()

                val notification = NotificationEntity(
                    title = title,
                    message = message,
                    type = type,
                    userId = userId,
                    actionType = actionType,
                    actionData = actionData,
                    icon = icon
                )

                notificationDao.insert(notification)

                // Mostrar notificación local
                NotificationManager.showNotification(
                    context = context,
                    title = title,
                    message = message,
                    icon = icon
                )

                Log.d(TAG, "Notificación creada: $title")
            } catch (e: Exception) {
                Log.e(TAG, "Error al crear notificación", e)
            }
        }
    }
}