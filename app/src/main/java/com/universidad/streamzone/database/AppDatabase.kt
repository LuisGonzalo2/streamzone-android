package com.universidad.streamzone.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.universidad.streamzone.dao.UsuarioDao
import com.universidad.streamzone.model.UsuarioEntity

@Database(entities = [UsuarioEntity::class], version = 1, exportSchema = false)


abstract class AppDatabase: RoomDatabase() {
    abstract fun usuarioDao(): UsuarioDao
    companion object{
        @Volatile
        private var INSTANCE: AppDatabase? = null
        fun getInstance(context: Context): AppDatabase{
            return  INSTANCE?: synchronized(this){
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "appseptimoa.db"
                ).build()
                INSTANCE= instance
                instance
            }
        }

    }

}