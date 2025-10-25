package com.universidad.streamzone.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.universidad.streamzone.data.local.dao.UsuarioDao
import com.universidad.streamzone.data.model.UsuarioEntity

@Database(entities = [UsuarioEntity::class], version = 3, exportSchema = false)
abstract class AppDatabase: RoomDatabase() {
    abstract fun usuarioDao(): UsuarioDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        // Migración de versión 1 a 2
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Agregar columnas de sincronización
                database.execSQL("ALTER TABLE usuarios ADD COLUMN sincronizado INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE usuarios ADD COLUMN firebaseId TEXT")
            }
        }

        // Migración de versión 2 a 3 - Limpiar datos antiguos
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "zonastream.db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .fallbackToDestructiveMigration() // Elimina y recrea si hay problemas
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}