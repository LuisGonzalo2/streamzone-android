package com.universidad.streamzone.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.universidad.streamzone.data.local.dao.*
import com.universidad.streamzone.data.model.*

@Database(
    entities = [
        UsuarioEntity::class,
        PurchaseEntity::class,
        RoleEntity::class,
        PermissionEntity::class,
        RolePermissionEntity::class,
        UserRoleEntity::class,
        ServiceEntity::class,
        CategoryEntity::class,
        OfferEntity::class,
        AdminMenuOptionEntity::class
    ],
    version = 8,
    exportSchema = false
)
abstract class AppDatabase: RoomDatabase() {
    abstract fun usuarioDao(): UsuarioDao
    abstract fun purchaseDao(): PurchaseDao
    abstract fun roleDao(): RoleDao
    abstract fun permissionDao(): PermissionDao
    abstract fun rolePermissionDao(): RolePermissionDao
    abstract fun userRoleDao(): UserRoleDao
    abstract fun serviceDao(): ServiceDao
    abstract fun categoryDao(): CategoryDao
    abstract fun offerDao(): OfferDao
    abstract fun adminMenuOptionDao(): AdminMenuOptionDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        // Migración de versión 1 a 2
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE usuarios ADD COLUMN sincronizado INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE usuarios ADD COLUMN firebaseId TEXT")
            }
        }

        // Migración de versión 2 a 3
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // No changes
            }
        }

        // Migración de versión 3 a 4 - Agregar tabla purchases
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS purchases (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        userEmail TEXT NOT NULL,
                        userName TEXT NOT NULL,
                        serviceId TEXT NOT NULL,
                        serviceName TEXT NOT NULL,
                        servicePrice TEXT NOT NULL,
                        serviceDuration TEXT NOT NULL,
                        email TEXT,
                        password TEXT,
                        purchaseDate INTEGER NOT NULL,
                        expirationDate INTEGER NOT NULL,
                        status TEXT NOT NULL,
                        sincronizado INTEGER NOT NULL DEFAULT 0,
                        firebaseId TEXT
                    )
                """.trimIndent())
            }
        }

        // Migración de versión 4 a 5 - Agregar fotoBase64 a usuarios
        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE usuarios ADD COLUMN fotoBase64 TEXT")
            }
        }

        // Migración de versión 5 a 6 - Sistema de roles y permisos + Servicios y Categorías
        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // 1. Agregar campos de admin a usuarios
                database.execSQL("ALTER TABLE usuarios ADD COLUMN isAdmin INTEGER NOT NULL DEFAULT 0")

                // 2. Crear tabla de roles
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS roles (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        description TEXT NOT NULL,
                        isActive INTEGER NOT NULL DEFAULT 1,
                        sincronizado INTEGER NOT NULL DEFAULT 0,
                        firebaseId TEXT
                    )
                """.trimIndent())

                // 3. Crear tabla de permisos
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS permissions (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        code TEXT NOT NULL,
                        name TEXT NOT NULL,
                        description TEXT NOT NULL
                    )
                """.trimIndent())

                // 4. Crear tabla de relación role_permissions
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS role_permissions (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        roleId INTEGER NOT NULL,
                        permissionId INTEGER NOT NULL,
                        FOREIGN KEY(roleId) REFERENCES roles(id) ON DELETE CASCADE,
                        FOREIGN KEY(permissionId) REFERENCES permissions(id) ON DELETE CASCADE
                    )
                """.trimIndent())

                // Índices para role_permissions
                database.execSQL("CREATE INDEX IF NOT EXISTS index_role_permissions_roleId ON role_permissions(roleId)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_role_permissions_permissionId ON role_permissions(permissionId)")

                // 5. Crear tabla de relación user_roles
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS user_roles (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        userId INTEGER NOT NULL,
                        roleId INTEGER NOT NULL,
                        FOREIGN KEY(userId) REFERENCES usuarios(id) ON DELETE CASCADE,
                        FOREIGN KEY(roleId) REFERENCES roles(id) ON DELETE CASCADE
                    )
                """.trimIndent())

                // Índices para user_roles
                database.execSQL("CREATE INDEX IF NOT EXISTS index_user_roles_userId ON user_roles(userId)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_user_roles_roleId ON user_roles(roleId)")

                // 6. Crear tabla de servicios
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS services (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        serviceId TEXT NOT NULL,
                        name TEXT NOT NULL,
                        price TEXT NOT NULL,
                        description TEXT NOT NULL,
                        iconUrl TEXT,
                        iconDrawable INTEGER,
                        categoryId INTEGER NOT NULL,
                        isActive INTEGER NOT NULL DEFAULT 1,
                        sincronizado INTEGER NOT NULL DEFAULT 0,
                        firebaseId TEXT
                    )
                """.trimIndent())

                // 7. Crear tabla de categorías
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS categories (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        categoryId TEXT NOT NULL,
                        name TEXT NOT NULL,
                        icon TEXT NOT NULL,
                        description TEXT NOT NULL,
                        gradientStart TEXT NOT NULL,
                        gradientEnd TEXT NOT NULL,
                        isActive INTEGER NOT NULL DEFAULT 1,
                        sincronizado INTEGER NOT NULL DEFAULT 0,
                        firebaseId TEXT
                    )
                """.trimIndent())

                // 8. Insertar permisos por defecto
                insertDefaultPermissions(database)

                // 9. Crear rol de Super Admin
                insertSuperAdminRole(database)
            }

            private fun insertDefaultPermissions(database: SupportSQLiteDatabase) {
                val permissions = listOf(
                    "('MANAGE_PURCHASES', 'Gestionar Compras', 'Asignar credenciales a compras pendientes')",
                    "('VIEW_ALL_PURCHASES', 'Ver Todas las Compras', 'Visualizar histórico completo de compras')",
                    "('MANAGE_USERS', 'Gestionar Usuarios', 'Administrar usuarios y sus datos')",
                    "('MANAGE_ROLES', 'Gestionar Roles', 'Crear y editar roles y permisos')",
                    "('MANAGE_SERVICES', 'Gestionar Servicios', 'Crear, editar y eliminar servicios')",
                    "('MANAGE_CATEGORIES', 'Gestionar Categorías', 'Crear y editar categorías')",
                    "('MANAGE_OFFERS', 'Gestionar Ofertas', 'Crear y editar ofertas especiales')",
                    "('UPLOAD_IMAGES', 'Subir Imágenes', 'Subir y gestionar imágenes de servicios')",
                    "('EDIT_PAYMENT_INFO', 'Editar Info de Pago', 'Modificar información de métodos de pago')",
                    "('EDIT_INSTRUCTIONS', 'Editar Instrucciones', 'Modificar instrucciones de compra')",
                    "('EDIT_RATINGS', 'Editar Valoraciones', 'Gestionar ratings y reseñas')",
                    "('FULL_ACCESS', 'Acceso Total', 'Acceso completo a todas las funciones')"
                )

                permissions.forEach { permission ->
                    database.execSQL("INSERT INTO permissions (code, name, description) VALUES $permission")
                }
            }

            private fun insertSuperAdminRole(database: SupportSQLiteDatabase) {
                // Crear rol Super Admin
                database.execSQL("""
                    INSERT INTO roles (name, description, isActive) 
                    VALUES ('Super Admin', 'Acceso total a todas las funciones del sistema', 1)
                """.trimIndent())

                // Obtener el ID del permiso FULL_ACCESS (siempre será el último insertado)
                database.execSQL("""
                    INSERT INTO role_permissions (roleId, permissionId)
                    SELECT 1, id FROM permissions WHERE code = 'FULL_ACCESS'
                """.trimIndent())
            }
        }

        // Migración de versión 6 a 7 - Agregar tabla de ofertas
        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Crear tabla de ofertas
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS offers (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        title TEXT NOT NULL,
                        description TEXT NOT NULL,
                        serviceIds TEXT NOT NULL,
                        originalPrice REAL NOT NULL,
                        comboPrice REAL NOT NULL,
                        discountPercent INTEGER NOT NULL,
                        startDate INTEGER NOT NULL,
                        endDate INTEGER NOT NULL,
                        isActive INTEGER NOT NULL DEFAULT 1,
                        sincronizado INTEGER NOT NULL DEFAULT 0,
                        firebaseId TEXT
                    )
                """.trimIndent())

                // Insertar oferta de ejemplo: Combo Netflix + Spotify
                // Fecha: del 1 de noviembre al 30 de noviembre de 2025
                val startDate = 1730419200000L // 1 Nov 2025 00:00:00
                val endDate = 1733011199000L   // 30 Nov 2025 23:59:59

                database.execSQL("""
                    INSERT INTO offers (
                        title, description, serviceIds,
                        originalPrice, comboPrice, discountPercent,
                        startDate, endDate, isActive
                    ) VALUES (
                        'Combo: Netflix + Spotify',
                        'Suscripción mensual de Netflix Premium + Spotify Premium. Disfruta de entretenimiento ilimitado con este combo especial.',
                        '1,5',
                        9.38,
                        7.50,
                        20,
                        $startDate,
                        $endDate,
                        1
                    )
                """.trimIndent())
            }
        }

        // Migración de versión 7 a 8 - Datos dinámicos: servicios, categorías y menú admin
        private val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // 1. Agregar nuevos campos a tabla services
                database.execSQL("ALTER TABLE services ADD COLUMN iconBase64 TEXT")
                database.execSQL("ALTER TABLE services ADD COLUMN isPopular INTEGER NOT NULL DEFAULT 0")

                // 2. Crear tabla admin_menu_options
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS admin_menu_options (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        title TEXT NOT NULL,
                        description TEXT NOT NULL,
                        icon TEXT NOT NULL,
                        permissionCode TEXT NOT NULL,
                        activityClass TEXT NOT NULL,
                        orderIndex INTEGER NOT NULL,
                        isActive INTEGER NOT NULL DEFAULT 1
                    )
                """.trimIndent())

                // 3. Poblar categorías
                insertCategories(database)

                // 4. Poblar servicios
                insertServices(database)

                // 5. Poblar opciones del menú admin
                insertAdminMenuOptions(database)
            }

            private fun insertCategories(database: SupportSQLiteDatabase) {
                val categories = listOf(
                    "('streaming', 'Streaming', '📺', 'Netflix, Disney+, Max y más', '#8B5CF6', '#6366F1', 1)",
                    "('music', 'Música', '🎵', 'Spotify, Deezer, YouTube Music', '#10B981', '#059669', 1)",
                    "('design', 'Diseño', '🎨', 'Canva, Office, Autodesk', '#F59E0B', '#D97706', 1)",
                    "('ai', 'IA', '🤖', 'ChatGPT y más', '#EF4444', '#DC2626', 1)"
                )

                categories.forEach { category ->
                    database.execSQL("""
                        INSERT INTO categories (categoryId, name, icon, description, gradientStart, gradientEnd, isActive)
                        VALUES $category
                    """.trimIndent())
                }
            }

            private fun insertServices(database: SupportSQLiteDatabase) {
                // STREAMING (categoryId = 1)
                val streamingServices = listOf(
                    "('netflix', 'Netflix', 'US\$ 4,00 /mes', 'Acceso inmediato', 1, 1, 1)",
                    "('disney_plus_premium', 'Disney+ Premium', 'US\$ 3,75 /mes', 'Acceso inmediato', 1, 0, 1)",
                    "('disney_plus_standard', 'Disney+ Standard', 'US\$ 3,25 /mes', 'Acceso inmediato', 1, 0, 1)",
                    "('max', 'Max', 'US\$ 3,00 /mes', 'Acceso inmediato', 1, 0, 1)",
                    "('vix', 'ViX', 'US\$ 2,50 /mes', 'Acceso inmediato', 1, 0, 1)",
                    "('prime', 'Prime Video', 'US\$ 3,00 /mes', 'Acceso inmediato', 1, 0, 1)",
                    "('paramount', 'Paramount+', 'US\$ 2,75 /mes', 'Acceso inmediato', 1, 0, 1)",
                    "('appletv', 'Apple TV+', 'US\$ 3,50 /mes', 'Acceso inmediato', 1, 0, 1)",
                    "('crunchyroll', 'Crunchyroll', 'US\$ 2,50 /mes', 'Acceso inmediato', 1, 0, 1)"
                )

                // MÚSICA (categoryId = 2)
                val musicServices = listOf(
                    "('spotify', 'Spotify', 'US\$ 3,50 /mes', 'Acceso inmediato', 2, 1, 1)",
                    "('deezer', 'Deezer', 'US\$ 3,00 /mes', 'Acceso inmediato', 2, 0, 1)",
                    "('youtube_premium', 'YouTube Premium', 'US\$ 3,35 /mes', 'Acceso inmediato', 2, 0, 1)"
                )

                // DISEÑO (categoryId = 3)
                val designServices = listOf(
                    "('canva', 'Canva Pro', 'US\$ 2,00 /mes', 'Acceso inmediato', 3, 0, 1)",
                    "('canva_year', 'Canva Pro (1 año)', 'US\$ 17,50 /año', 'Licencia anual', 3, 0, 1)",
                    "('m365_year', 'Microsoft 365 (M365)', 'US\$ 15,00 /año', 'Licencia anual', 3, 0, 1)",
                    "('office365_year', 'Office 365 (O365)', 'US\$ 15,00 /año', 'Licencia anual', 3, 0, 1)",
                    "('autodesk_year', 'Autodesk (AD)', 'US\$ 12,50 /año', 'Licencia anual', 3, 0, 1)"
                )

                // IA (categoryId = 4)
                val aiServices = listOf(
                    "('chatgpt', 'ChatGPT', 'US\$ 4,00 /mes', 'Acceso inmediato', 4, 1, 1)"
                )

                val allServices = streamingServices + musicServices + designServices + aiServices

                allServices.forEach { service ->
                    database.execSQL("""
                        INSERT INTO services (serviceId, name, price, description, categoryId, isPopular, isActive)
                        VALUES $service
                    """.trimIndent())
                }
            }

            private fun insertAdminMenuOptions(database: SupportSQLiteDatabase) {
                val options = listOf(
                    "('Gestionar Compras', 'Ver y administrar compras pendientes', '📦', 'MANAGE_PURCHASES', 'PendingPurchasesActivity', 1, 1)",
                    "('Gestionar Ofertas', 'Crear y editar ofertas especiales', '🎯', 'MANAGE_OFFERS', 'OffersManagerActivity', 2, 1)",
                    "('Gestionar Usuarios', 'Ver y editar usuarios del sistema', '👥', 'MANAGE_USERS', 'UserListActivity', 3, 1)",
                    "('Gestionar Roles', 'Configurar roles y permisos', '🔐', 'MANAGE_ROLES', 'RolesManagerActivity', 4, 1)",
                    "('Gestionar Servicios', 'Agregar y editar servicios disponibles', '📺', 'MANAGE_SERVICES', 'ServicesManagerActivity', 5, 1)",
                    "('Gestionar Categorías', 'Organizar categorías de servicios', '📁', 'MANAGE_CATEGORIES', 'CategoriesManagerActivity', 6, 1)"
                )

                options.forEach { option ->
                    database.execSQL("""
                        INSERT INTO admin_menu_options (title, description, icon, permissionCode, activityClass, orderIndex, isActive)
                        VALUES $option
                    """.trimIndent())
                }
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "zonastream.db"
                )
                    .addMigrations(
                        MIGRATION_1_2,
                        MIGRATION_2_3,
                        MIGRATION_3_4,
                        MIGRATION_4_5,
                        MIGRATION_5_6,
                        MIGRATION_6_7,
                        MIGRATION_7_8
                    )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}