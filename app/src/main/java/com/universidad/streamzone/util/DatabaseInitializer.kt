package com.universidad.streamzone.util

import android.content.Context
import android.util.Log
import com.universidad.streamzone.data.local.database.AppDatabase
import com.universidad.streamzone.data.model.CategoryEntity
import com.universidad.streamzone.data.model.OfferEntity
import com.universidad.streamzone.data.model.PermissionEntity
import com.universidad.streamzone.data.model.ServiceEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar

/**
 * Inicializador de base de datos
 * Inserta datos predefinidos si no existen
 */
object DatabaseInitializer {

    private const val TAG = "DatabaseInitializer"

    /**
     * Inicializa todos los datos predefinidos del sistema
     */
    suspend fun initializeDatabase(context: Context) = withContext(Dispatchers.IO) {
        initializePermissions(context)
        initializeCategories(context)
        initializeServices(context)
        initializeOffers(context)
    }

    /**
     * Inicializa los permisos predefinidos del sistema
     */
    private suspend fun initializePermissions(context: Context) {
        try {
            val db = AppDatabase.getInstance(context)
            val permissionDao = db.permissionDao()

            // Verificar si ya existen permisos
            val existingPermissions = permissionDao.getAll()
            if (existingPermissions.isNotEmpty()) {
                Log.d(TAG, "Permisos ya inicializados (${existingPermissions.size} permisos)")
                return
            }

            // Definir permisos predefinidos
            val predefinedPermissions = listOf(
                PermissionEntity(
                    id = 0,
                    name = "Gestionar Compras",
                    code = PermissionManager.MANAGE_PURCHASES,
                    description = "Permite ver y gestionar todas las compras del sistema"
                ),
                PermissionEntity(
                    id = 0,
                    name = "Ver Todas las Compras",
                    code = PermissionManager.VIEW_ALL_PURCHASES,
                    description = "Permite visualizar el historial completo de compras"
                ),
                PermissionEntity(
                    id = 0,
                    name = "Gestionar Usuarios",
                    code = PermissionManager.MANAGE_USERS,
                    description = "Permite crear, editar y eliminar usuarios del sistema"
                ),
                PermissionEntity(
                    id = 0,
                    name = "Gestionar Roles",
                    code = PermissionManager.MANAGE_ROLES,
                    description = "Permite crear, editar y eliminar roles y asignar permisos"
                ),
                PermissionEntity(
                    id = 0,
                    name = "Gestionar Servicios",
                    code = PermissionManager.MANAGE_SERVICES,
                    description = "Permite agregar, editar y eliminar servicios disponibles"
                ),
                PermissionEntity(
                    id = 0,
                    name = "Gestionar Categorías",
                    code = PermissionManager.MANAGE_CATEGORIES,
                    description = "Permite crear, editar y eliminar categorías de servicios"
                ),
                PermissionEntity(
                    id = 0,
                    name = "Gestionar Ofertas",
                    code = PermissionManager.MANAGE_OFFERS,
                    description = "Permite crear, editar y eliminar ofertas especiales"
                ),
                PermissionEntity(
                    id = 0,
                    name = "Subir Imágenes",
                    code = PermissionManager.UPLOAD_IMAGES,
                    description = "Permite subir y gestionar imágenes del sistema"
                ),
                PermissionEntity(
                    id = 0,
                    name = "Editar Info de Pago",
                    code = PermissionManager.EDIT_PAYMENT_INFO,
                    description = "Permite modificar información de pago de servicios"
                ),
                PermissionEntity(
                    id = 0,
                    name = "Editar Instrucciones",
                    code = PermissionManager.EDIT_INSTRUCTIONS,
                    description = "Permite modificar instrucciones de servicios y compras"
                ),
                PermissionEntity(
                    id = 0,
                    name = "Editar Calificaciones",
                    code = PermissionManager.EDIT_RATINGS,
                    description = "Permite modificar y eliminar calificaciones de usuarios"
                ),
                PermissionEntity(
                    id = 0,
                    name = "Acceso Total",
                    code = PermissionManager.FULL_ACCESS,
                    description = "Acceso completo a todas las funcionalidades del sistema"
                )
            )

            // Insertar permisos
            predefinedPermissions.forEach { permission ->
                permissionDao.insertar(permission)
            }

            Log.d(TAG, "✅ Permisos predefinidos insertados (${predefinedPermissions.size} permisos)")

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error al inicializar permisos", e)
        }
    }

    /**
     * Inicializa las categorías predefinidas
     */
    private suspend fun initializeCategories(context: Context) {
        try {
            val db = AppDatabase.getInstance(context)
            val categoryDao = db.categoryDao()

            // Verificar si ya existen categorías
            val existingCategories = categoryDao.obtenerCategoriasActivasSync()
            if (existingCategories.isNotEmpty()) {
                Log.d(TAG, "Categorías ya inicializadas (${existingCategories.size} categorías)")
                return
            }

            // Definir categorías predefinidas
            val categories = listOf(
                CategoryEntity(
                    id = 0,
                    categoryId = "streaming",
                    name = "Streaming",
                    icon = "📺",
                    description = "Netflix, Disney+, Max y más",
                    gradientStart = "#8B5CF6",
                    gradientEnd = "#6366F1",
                    isActive = true
                ),
                CategoryEntity(
                    id = 0,
                    categoryId = "music",
                    name = "Música",
                    icon = "🎵",
                    description = "Spotify, Deezer, YouTube Music",
                    gradientStart = "#10B981",
                    gradientEnd = "#059669",
                    isActive = true
                ),
                CategoryEntity(
                    id = 0,
                    categoryId = "design",
                    name = "Diseño",
                    icon = "🎨",
                    description = "Canva, Office, Autodesk",
                    gradientStart = "#F59E0B",
                    gradientEnd = "#D97706",
                    isActive = true
                ),
                CategoryEntity(
                    id = 0,
                    categoryId = "ai",
                    name = "IA",
                    icon = "🤖",
                    description = "ChatGPT y más",
                    gradientStart = "#EF4444",
                    gradientEnd = "#DC2626",
                    isActive = true
                )
            )

            categories.forEach { category ->
                categoryDao.insertar(category)
            }

            Log.d(TAG, "✅ Categorías predefinidas insertadas (${categories.size} categorías)")

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error al inicializar categorías", e)
        }
    }

    /**
     * Inicializa los servicios predefinidos
     */
    private suspend fun initializeServices(context: Context) {
        try {
            val db = AppDatabase.getInstance(context)
            val serviceDao = db.serviceDao()
            val categoryDao = db.categoryDao()

            // Verificar si ya existen servicios
            val existingServices = serviceDao.getAll()
            if (existingServices.isNotEmpty()) {
                Log.d(TAG, "Servicios ya inicializados (${existingServices.size} servicios)")
                return
            }

            // Obtener IDs de categorías
            val categories = categoryDao.obtenerCategoriasActivasSync()
            val streamingCatId = categories.find { it.categoryId == "streaming" }?.id ?: 1
            val musicCatId = categories.find { it.categoryId == "music" }?.id ?: 2
            val designCatId = categories.find { it.categoryId == "design" }?.id ?: 3
            val aiCatId = categories.find { it.categoryId == "ai" }?.id ?: 4

            // Definir servicios predefinidos
            val services = listOf(
                // STREAMING
                ServiceEntity(0, "netflix", "Netflix", "US\$ 4,00 /mes", "Acceso inmediato", null, null, null, streamingCatId, true, true),
                ServiceEntity(0, "disney_plus_premium", "Disney+ Premium", "US\$ 3,75 /mes", "Acceso inmediato", null, null, null, streamingCatId, true, false),
                ServiceEntity(0, "disney_plus_standard", "Disney+ Standard", "US\$ 3,25 /mes", "Acceso inmediato", null, null, null, streamingCatId, true, false),
                ServiceEntity(0, "max", "Max", "US\$ 3,00 /mes", "Acceso inmediato", null, null, null, streamingCatId, true, false),
                ServiceEntity(0, "vix", "ViX", "US\$ 2,50 /mes", "Acceso inmediato", null, null, null, streamingCatId, true, false),
                ServiceEntity(0, "prime", "Prime Video", "US\$ 3,00 /mes", "Acceso inmediato", null, null, null, streamingCatId, true, false),
                ServiceEntity(0, "paramount", "Paramount+", "US\$ 2,75 /mes", "Acceso inmediato", null, null, null, streamingCatId, true, false),
                ServiceEntity(0, "appletv", "Apple TV+", "US\$ 3,50 /mes", "Acceso inmediato", null, null, null, streamingCatId, true, false),
                ServiceEntity(0, "crunchyroll", "Crunchyroll", "US\$ 2,50 /mes", "Acceso inmediato", null, null, null, streamingCatId, true, false),

                // MÚSICA
                ServiceEntity(0, "spotify", "Spotify", "US\$ 3,50 /mes", "Acceso inmediato", null, null, null, musicCatId, true, true),
                ServiceEntity(0, "deezer", "Deezer", "US\$ 3,00 /mes", "Acceso inmediato", null, null, null, musicCatId, true, false),
                ServiceEntity(0, "youtube_premium", "YouTube Premium", "US\$ 3,35 /mes", "Acceso inmediato", null, null, null, musicCatId, true, false),

                // DISEÑO
                ServiceEntity(0, "canva", "Canva Pro", "US\$ 2,00 /mes", "Acceso inmediato", null, null, null, designCatId, true, false),
                ServiceEntity(0, "canva_year", "Canva Pro (1 año)", "US\$ 17,50 /año", "Licencia anual", null, null, null, designCatId, true, false),
                ServiceEntity(0, "m365_year", "Microsoft 365 (M365)", "US\$ 15,00 /año", "Licencia anual", null, null, null, designCatId, true, false),
                ServiceEntity(0, "office365_year", "Office 365 (O365)", "US\$ 15,00 /año", "Licencia anual", null, null, null, designCatId, true, false),
                ServiceEntity(0, "autodesk_year", "Autodesk (AD)", "US\$ 12,50 /año", "Licencia anual", null, null, null, designCatId, true, false),

                // IA
                ServiceEntity(0, "chatgpt", "ChatGPT", "US\$ 4,00 /mes", "Acceso inmediato", null, null, null, aiCatId, true, true)
            )

            services.forEach { service ->
                serviceDao.insertar(service)
            }

            Log.d(TAG, "✅ Servicios predefinidos insertados (${services.size} servicios)")

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error al inicializar servicios", e)
        }
    }

    /**
     * Inicializa o actualiza la oferta del mes con fechas válidas
     */
    private suspend fun initializeOffers(context: Context) {
        try {
            val db = AppDatabase.getInstance(context)
            val offerDao = db.offerDao()

            // Calcular fechas para el mes actual
            val calendar = Calendar.getInstance()

            // Primer día del mes a las 00:00:00
            calendar.set(Calendar.DAY_OF_MONTH, 1)
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            val startDate = calendar.timeInMillis

            // Último día del mes a las 23:59:59
            calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
            calendar.set(Calendar.HOUR_OF_DAY, 23)
            calendar.set(Calendar.MINUTE, 59)
            calendar.set(Calendar.SECOND, 59)
            calendar.set(Calendar.MILLISECOND, 999)
            val endDate = calendar.timeInMillis

            // Verificar si ya existe una oferta activa
            val activeOffer = offerDao.getActiveOffer()

            if (activeOffer != null) {
                Log.d(TAG, "Ya existe una oferta activa: ${activeOffer.title}")
                return
            }

            // Verificar si existe alguna oferta (incluso inactiva o vencida)
            val allOffers = offerDao.getAll()

            if (allOffers.isEmpty()) {
                // No hay ofertas, crear una nueva
                val newOffer = OfferEntity(
                    id = 0,
                    title = "Combo: Netflix + Spotify",
                    description = "Suscripción mensual de Netflix Premium + Spotify Premium. Disfruta de entretenimiento ilimitado con este combo especial.",
                    serviceIds = "1,5", // IDs de Netflix y Spotify
                    originalPrice = 9.38,
                    comboPrice = 7.50,
                    discountPercent = 20,
                    startDate = startDate,
                    endDate = endDate,
                    isActive = true
                )
                offerDao.insert(newOffer)
                Log.d(TAG, "✅ Oferta del mes creada con fechas válidas")
            } else {
                // Existe oferta pero está vencida, actualizar fechas
                val firstOffer = allOffers.first()
                val updatedOffer = firstOffer.copy(
                    startDate = startDate,
                    endDate = endDate,
                    isActive = true
                )
                offerDao.update(updatedOffer)
                Log.d(TAG, "✅ Oferta del mes actualizada con nuevas fechas válidas")
            }

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error al inicializar ofertas", e)
        }
    }
}