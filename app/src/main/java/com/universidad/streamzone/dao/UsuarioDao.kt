package com.universidad.streamzone.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.universidad.streamzone.model.UsuarioEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UsuarioDao{
    @Insert
    suspend fun insertar(usuario: UsuarioEntity)

    @Query("SELECT * FROM usuarios ORDER BY id DESC")
    fun obtenerTodos(): Flow<List<UsuarioEntity>>
}