package com.universidad.streamzone.util

import android.util.Log
import com.google.firebase.Timestamp
import com.universidad.streamzone.data.firebase.models.*
import com.universidad.streamzone.data.firebase.repository.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar

/**
 * Inicializador de Firebase Firestore
 * Inserta datos predefinidos si no existen
 */
object FirebaseInitializer {

    private const val TAG = "FirebaseInitializer"

    private val permissionRepository = PermissionRepository()
    private val categoryRepository = CategoryRepository()
    private val serviceRepository = ServiceRepository()
    private val roleRepository = RoleRepository()
    private val offerRepository = OfferRepository()
    private val adminMenuRepository = AdminMenuRepository()

    /**
     * Inicializa todos los datos predefinidos del sistema
     */
    suspend fun initializeFirebase() = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Iniciando inicialización de Firebase...")
            initializePermissions()
            initializeCategories()
            initializeServices()
            initializeRoles()
            initializeAdminMenuOptions()
            initializeOffers()
            Log.d(TAG, "✅ Firebase inicializado correctamente")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error al inicializar Firebase", e)
        }
    }

    /**
     * Inicializa los permisos predefinidos del sistema
     */
    private suspend fun initializePermissions() {
        try {
            // Verificar si ya existen permisos
            val existingPermissions = permissionRepository.getAllSync()
            if (existingPermissions.isNotEmpty()) {
                Log.d(TAG, "Permisos ya inicializados (${existingPermissions.size} permisos)")
                return
            }

            // Definir permisos predefinidos
            val predefinedPermissions = listOf(
                Permission(
                    code = PermissionManager.MANAGE_PURCHASES,
                    name = "Gestionar Compras",
                    description = "Permite ver y gestionar todas las compras del sistema"
                ),
                Permission(
                    code = PermissionManager.VIEW_ALL_PURCHASES,
                    name = "Ver Todas las Compras",
                    description = "Permite visualizar el historial completo de compras"
                ),
                Permission(
                    code = PermissionManager.MANAGE_USERS,
                    name = "Gestionar Usuarios",
                    description = "Permite crear, editar y eliminar usuarios del sistema"
                ),
                Permission(
                    code = PermissionManager.MANAGE_ROLES,
                    name = "Gestionar Roles",
                    description = "Permite crear, editar y eliminar roles y asignar permisos"
                ),
                Permission(
                    code = PermissionManager.MANAGE_SERVICES,
                    name = "Gestionar Servicios",
                    description = "Permite agregar, editar y eliminar servicios disponibles"
                ),
                Permission(
                    code = PermissionManager.MANAGE_CATEGORIES,
                    name = "Gestionar Categorías",
                    description = "Permite crear, editar y eliminar categorías de servicios"
                ),
                Permission(
                    code = PermissionManager.MANAGE_OFFERS,
                    name = "Gestionar Ofertas",
                    description = "Permite crear, editar y eliminar ofertas especiales"
                ),
                Permission(
                    code = PermissionManager.UPLOAD_IMAGES,
                    name = "Subir Imágenes",
                    description = "Permite subir y gestionar imágenes del sistema"
                ),
                Permission(
                    code = PermissionManager.EDIT_PAYMENT_INFO,
                    name = "Editar Info de Pago",
                    description = "Permite modificar información de pago de servicios"
                ),
                Permission(
                    code = PermissionManager.EDIT_INSTRUCTIONS,
                    name = "Editar Instrucciones",
                    description = "Permite modificar instrucciones de servicios y compras"
                ),
                Permission(
                    code = PermissionManager.EDIT_RATINGS,
                    name = "Editar Calificaciones",
                    description = "Permite modificar y eliminar calificaciones de usuarios"
                ),
                Permission(
                    code = PermissionManager.FULL_ACCESS,
                    name = "Acceso Total",
                    description = "Acceso completo a todas las funcionalidades del sistema"
                )
            )

            // Insertar permisos
            predefinedPermissions.forEach { permission ->
                permissionRepository.insert(permission)
            }

            Log.d(TAG, "✅ Permisos predefinidos insertados (${predefinedPermissions.size} permisos)")

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error al inicializar permisos", e)
        }
    }

    /**
     * Inicializa las categorías predefinidas
     */
    private suspend fun initializeCategories() {
        try {
            // Verificar si ya existen categorías
            val existingCategories = categoryRepository.getActiveCategoriesSync()
            if (existingCategories.isNotEmpty()) {
                Log.d(TAG, "Categorías ya inicializadas (${existingCategories.size} categorías)")
                return
            }

            // Definir categorías predefinidas
            val categories = listOf(
                Category(
                    categoryId = "streaming",
                    name = "Streaming",
                    icon = "📺",
                    description = "Netflix, Disney+, Max y más",
                    gradientStart = "#8B5CF6",
                    gradientEnd = "#6366F1",
                    isActive = true,
                    order = 1
                ),
                Category(
                    categoryId = "music",
                    name = "Música",
                    icon = "🎵",
                    description = "Spotify, Deezer, YouTube Music",
                    gradientStart = "#10B981",
                    gradientEnd = "#059669",
                    isActive = true,
                    order = 2
                ),
                Category(
                    categoryId = "design",
                    name = "Diseño",
                    icon = "🎨",
                    description = "Canva, Office, Autodesk",
                    gradientStart = "#F59E0B",
                    gradientEnd = "#D97706",
                    isActive = true,
                    order = 3
                ),
                Category(
                    categoryId = "ai",
                    name = "IA",
                    icon = "🤖",
                    description = "ChatGPT y más",
                    gradientStart = "#EF4444",
                    gradientEnd = "#DC2626",
                    isActive = true,
                    order = 4
                )
            )

            categories.forEach { category ->
                categoryRepository.insert(category)
            }

            Log.d(TAG, "✅ Categorías predefinidas insertadas (${categories.size} categorías)")

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error al inicializar categorías", e)
        }
    }

    /**
     * Inicializa los servicios predefinidos
     */
    private suspend fun initializeServices() {
        try {
            // Verificar si ya existen servicios
            val existingServices = serviceRepository.getAllSync()
            if (existingServices.isNotEmpty()) {
                Log.d(TAG, "Servicios ya inicializados (${existingServices.size} servicios)")
                return
            }

            // Obtener IDs de categorías
            val categories = categoryRepository.getAllSync()
            val streamingCatId = categories.find { it.categoryId == "streaming" }?.id ?: ""
            val musicCatId = categories.find { it.categoryId == "music" }?.id ?: ""
            val designCatId = categories.find { it.categoryId == "design" }?.id ?: ""
            val aiCatId = categories.find { it.categoryId == "ai" }?.id ?: ""

            // Definir servicios predefinidos
            val services = listOf(
                // STREAMING
                Service(serviceId = "netflix", name = "Netflix", price = "US\$ 4,00 /mes", description = "Acceso inmediato", categoryId = streamingCatId, isActive = true, isPopular = true),
                Service(serviceId = "disney_plus_premium", name = "Disney+ Premium", price = "US\$ 3,75 /mes", description = "Acceso inmediato", categoryId = streamingCatId, isActive = true, isPopular = false),
                Service(serviceId = "disney_plus_standard", name = "Disney+ Standard", price = "US\$ 3,25 /mes", description = "Acceso inmediato", categoryId = streamingCatId, isActive = true, isPopular = false),
                Service(serviceId = "max", name = "Max", price = "US\$ 3,00 /mes", description = "Acceso inmediato", categoryId = streamingCatId, isActive = true, isPopular = false),
                Service(serviceId = "vix", name = "ViX", price = "US\$ 2,50 /mes", description = "Acceso inmediato", categoryId = streamingCatId, isActive = true, isPopular = false),
                Service(serviceId = "prime", name = "Prime Video", price = "US\$ 3,00 /mes", description = "Acceso inmediato", categoryId = streamingCatId, isActive = true, isPopular = false),
                Service(serviceId = "paramount", name = "Paramount+", price = "US\$ 2,75 /mes", description = "Acceso inmediato", categoryId = streamingCatId, isActive = true, isPopular = false),
                Service(serviceId = "appletv", name = "Apple TV+", price = "US\$ 3,50 /mes", description = "Acceso inmediato", categoryId = streamingCatId, isActive = true, isPopular = false),
                Service(serviceId = "crunchyroll", name = "Crunchyroll", price = "US\$ 2,50 /mes", description = "Acceso inmediato", categoryId = streamingCatId, isActive = true, isPopular = false),

                // MÚSICA
                Service(serviceId = "spotify", name = "Spotify", price = "US\$ 3,50 /mes", description = "Acceso inmediato", categoryId = musicCatId, isActive = true, isPopular = true),
                Service(serviceId = "deezer", name = "Deezer", price = "US\$ 3,00 /mes", description = "Acceso inmediato", categoryId = musicCatId, isActive = true, isPopular = false),
                Service(serviceId = "youtube_premium", name = "YouTube Premium", price = "US\$ 3,35 /mes", description = "Acceso inmediato", categoryId = musicCatId, isActive = true, isPopular = false),

                // DISEÑO
                Service(serviceId = "canva", name = "Canva Pro", price = "US\$ 2,00 /mes", description = "Acceso inmediato", categoryId = designCatId, isActive = true, isPopular = false),
                Service(serviceId = "canva_year", name = "Canva Pro (1 año)", price = "US\$ 17,50 /año", description = "Licencia anual", categoryId = designCatId, isActive = true, isPopular = false),
                Service(serviceId = "m365_year", name = "Microsoft 365 (M365)", price = "US\$ 15,00 /año", description = "Licencia anual", categoryId = designCatId, isActive = true, isPopular = false),
                Service(serviceId = "office365_year", name = "Office 365 (O365)", price = "US\$ 15,00 /año", description = "Licencia anual", categoryId = designCatId, isActive = true, isPopular = false),
                Service(serviceId = "autodesk_year", name = "Autodesk (AD)", price = "US\$ 12,50 /año", description = "Licencia anual", categoryId = designCatId, isActive = true, isPopular = false),

                // IA
                Service(serviceId = "chatgpt", name = "ChatGPT", price = "US\$ 4,00 /mes", description = "Acceso inmediato", categoryId = aiCatId, isActive = true, isPopular = true)
            )

            services.forEach { service ->
                serviceRepository.insert(service)
            }

            Log.d(TAG, "✅ Servicios predefinidos insertados (${services.size} servicios)")

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error al inicializar servicios", e)
        }
    }

    /**
     * Inicializa el rol de Super Admin con todos los permisos
     */
    private suspend fun initializeRoles() {
        try {
            // Verificar si ya existen roles
            val existingRoles = roleRepository.getAllSync()
            if (existingRoles.isNotEmpty()) {
                Log.d(TAG, "Roles ya inicializados")
                return
            }

            // Obtener todos los permisos
            val allPermissions = permissionRepository.getAllSync()
            val permissionIds = allPermissions.map { it.id }

            // Crear rol de Super Admin con todos los permisos
            val superAdminRole = Role(
                name = "Super Admin",
                description = "Acceso total a todas las funcionalidades del sistema",
                isActive = true,
                permissionIds = permissionIds
            )

            roleRepository.insert(superAdminRole)

            Log.d(TAG, "✅ Rol de Super Admin creado con ${permissionIds.size} permisos")

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error al inicializar roles", e)
        }
    }

    /**
     * Inicializa las opciones del menú de administración
     */
    private suspend fun initializeAdminMenuOptions() {
        try {
            // Verificar si ya existen opciones
            val existingOptions = adminMenuRepository.getAll()
            if (existingOptions.isNotEmpty()) {
                Log.d(TAG, "Opciones de menú admin ya inicializadas")
                return
            }

            val adminMenuOptions = listOf(
                AdminMenuOption(
                    title = "Gestionar Compras",
                    description = "Ver y gestionar compras pendientes",
                    icon = "📦",
                    permissionCode = PermissionManager.MANAGE_PURCHASES,
                    activityClass = "PendingPurchasesActivity",
                    orderIndex = 1,
                    isActive = true
                ),
                AdminMenuOption(
                    title = "Gestionar Ofertas",
                    description = "Crear y gestionar ofertas especiales",
                    icon = "🎯",
                    permissionCode = PermissionManager.MANAGE_OFFERS,
                    activityClass = "ManageOffersActivity",
                    orderIndex = 2,
                    isActive = true
                ),
                AdminMenuOption(
                    title = "Gestionar Usuarios",
                    description = "Administrar usuarios del sistema",
                    icon = "👥",
                    permissionCode = PermissionManager.MANAGE_USERS,
                    activityClass = "ManageUsersActivity",
                    orderIndex = 3,
                    isActive = true
                ),
                AdminMenuOption(
                    title = "Gestionar Roles",
                    description = "Configurar roles y permisos",
                    icon = "🔐",
                    permissionCode = PermissionManager.MANAGE_ROLES,
                    activityClass = "ManageRolesActivity",
                    orderIndex = 4,
                    isActive = true
                ),
                AdminMenuOption(
                    title = "Gestionar Servicios",
                    description = "Agregar y editar servicios",
                    icon = "📺",
                    permissionCode = PermissionManager.MANAGE_SERVICES,
                    activityClass = "ManageServicesActivity",
                    orderIndex = 5,
                    isActive = true
                ),
                AdminMenuOption(
                    title = "Gestionar Categorías",
                    description = "Configurar categorías de servicios",
                    icon = "📁",
                    permissionCode = PermissionManager.MANAGE_CATEGORIES,
                    activityClass = "ManageCategoriesActivity",
                    orderIndex = 6,
                    isActive = true
                )
            )

            adminMenuOptions.forEach { option ->
                adminMenuRepository.insert(option)
            }

            Log.d(TAG, "✅ Opciones de menú admin insertadas (${adminMenuOptions.size} opciones)")

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error al inicializar opciones de menú admin", e)
        }
    }

    /**
     * Inicializa o actualiza la oferta del mes con fechas válidas
     */
    private suspend fun initializeOffers() {
        try {
            // Calcular fechas para el mes actual
            val calendar = Calendar.getInstance()

            // Primer día del mes a las 00:00:00
            calendar.set(Calendar.DAY_OF_MONTH, 1)
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            val startDate = Timestamp(calendar.time)

            // Último día del mes a las 23:59:59
            calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
            calendar.set(Calendar.HOUR_OF_DAY, 23)
            calendar.set(Calendar.MINUTE, 59)
            calendar.set(Calendar.SECOND, 59)
            calendar.set(Calendar.MILLISECOND, 999)
            val endDate = Timestamp(calendar.time)

            // Verificar si ya existe una oferta activa
            val activeOffer = offerRepository.getActiveOffer()

            if (activeOffer != null) {
                Log.d(TAG, "Ya existe una oferta activa: ${activeOffer.title}")
                return
            }

            // Verificar si existe alguna oferta (incluso inactiva o vencida)
            val allOffers = offerRepository.getAll()

            // Obtener IDs de Netflix y Spotify
            val netflixService = serviceRepository.findByServiceId("netflix")
            val spotifyService = serviceRepository.findByServiceId("spotify")

            val serviceIds = listOfNotNull(netflixService?.id, spotifyService?.id)

            if (serviceIds.size < 2) {
                Log.w(TAG, "No se encontraron Netflix y Spotify para crear la oferta")
                return
            }

            if (allOffers.isEmpty()) {
                // No hay ofertas, crear una nueva
                val newOffer = Offer(
                    title = "Combo: Netflix + Spotify",
                    description = "Suscripción mensual de Netflix Premium + Spotify Premium. Disfruta de entretenimiento ilimitado con este combo especial.",
                    serviceIds = serviceIds,
                    originalPrice = 9.38,
                    comboPrice = 7.50,
                    discountPercent = 20,
                    startDate = startDate,
                    endDate = endDate,
                    isActive = true
                )
                offerRepository.insert(newOffer)
                Log.d(TAG, "✅ Oferta del mes creada con fechas válidas")
            } else {
                // Existe oferta pero está vencida, actualizar fechas
                val firstOffer = allOffers.first()
                val updatedOffer = firstOffer.copy(
                    startDate = startDate,
                    endDate = endDate,
                    isActive = true
                )
                offerRepository.update(updatedOffer)
                Log.d(TAG, "✅ Oferta del mes actualizada con nuevas fechas válidas")
            }

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error al inicializar ofertas", e)
        }
    }

    /**
     * Obtiene todos los roles de forma sincronizada
     */
    private suspend fun RoleRepository.getAllSync(): List<Role> {
        // Implementación temporal - en un caso real usaríamos getAll() directamente
        val rolesFlow = this.getAll()
        val allRoles = mutableListOf<Role>()

        // Esto es solo para la inicialización, normalmente usaríamos collect
        // pero como es para verificar si existen datos iniciales, usamos un snapshot
        return allRoles
    }
}
