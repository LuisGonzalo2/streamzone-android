package com.universidad.streamzone.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.universidad.streamzone.data.model.UsuarioEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UsuarioDao {

    @Insert
    suspend fun insertar(usuario: UsuarioEntity): Long

    @Update
    suspend fun actualizar(usuario: UsuarioEntity)

    @Query("SELECT * FROM usuarios ORDER BY id DESC")
    fun obtenerTodos(): Flow<List<UsuarioEntity>>

    // Buscar usuario por email
    @Query("SELECT * FROM usuarios WHERE email = :email LIMIT 1")
    suspend fun buscarPorEmail(email: String): UsuarioEntity?

    // Buscar usuario por número de teléfono
    @Query("SELECT * FROM usuarios WHERE phone = :phone LIMIT 1")
    suspend fun buscarPorTelefono(phone: String): UsuarioEntity?

    // Obtener usuarios no sincronizados
    @Query("SELECT * FROM usuarios WHERE sincronizado = 0")
    suspend fun obtenerNoSincronizados(): List<UsuarioEntity>

    // Marcar usuario como sincronizado
    @Query("UPDATE usuarios SET sincronizado = 1, firebaseId = :firebaseId WHERE id = :id")
    suspend fun marcarComoSincronizado(id: Int, firebaseId: String)
}