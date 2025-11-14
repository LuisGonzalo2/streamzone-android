package com.universidad.streamzone.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.universidad.streamzone.data.local.dao.PurchaseDao
import com.universidad.streamzone.data.local.dao.UsuarioDao
import com.universidad.streamzone.data.model.PurchaseEntity
import com.universidad.streamzone.data.model.UsuarioEntity

@Database(
    entities = [UsuarioEntity::class, PurchaseEntity::class],
    version = 5,
    exportSchema = false
)
abstract class AppDatabase: RoomDatabase() {
    abstract fun usuarioDao(): UsuarioDao
    abstract fun purchaseDao(): PurchaseDao

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

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "zonastream.db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}