package com.universidad.streamzone.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.universidad.streamzone.model.UsuarioEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UsuarioDao {

    @Insert
    suspend fun insertar(usuario: UsuarioEntity)

    @Query("SELECT * FROM usuarios ORDER BY id DESC")
    fun obtenerTodos(): Flow<List<UsuarioEntity>>

    //Buscar usuario por email
    @Query("SELECT * FROM usuarios WHERE email = :email LIMIT 1")
    suspend fun buscarPorEmail(email: String): UsuarioEntity?

    //Buscar usuario por número de teléfono
    @Query("SELECT * FROM usuarios WHERE phone = :phone LIMIT 1")
    suspend fun buscarPorTelefono(phone: String): UsuarioEntity?
}
