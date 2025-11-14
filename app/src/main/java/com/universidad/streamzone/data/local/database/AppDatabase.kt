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
        CategoryEntity::class
    ],
    version = 6,
    exportSchema = false
)
abstract class AppDatabase: RoomDatabase() {
    abstract fun usuarioDao(): UsuarioDao
    abstract fun purchaseDao(): PurchaseDao
    abstract fun roleDao(): RoleDao
    abstract fun permissionDao(): PermissionDao
    abstract fun userRoleDao(): UserRoleDao
    abstract fun serviceDao(): ServiceDao
    abstract fun categoryDao(): CategoryDao

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
                        MIGRATION_5_6
                    )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}