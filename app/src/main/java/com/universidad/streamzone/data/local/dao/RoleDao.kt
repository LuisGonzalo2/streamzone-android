package com.universidad.streamzone.data.local.dao

import androidx.room.*
import com.universidad.streamzone.data.model.RoleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RoleDao {

    @Insert
    suspend fun insertar(role: RoleEntity): Long

    @Update
    suspend fun actualizar(role: RoleEntity)

    @Delete
    suspend fun eliminar(role: RoleEntity)

    @Query("SELECT * FROM roles WHERE id = :roleId")
    suspend fun obtenerPorId(roleId: Int): RoleEntity?

    @Query("SELECT * FROM roles WHERE isActive = 1 ORDER BY name ASC")
    fun obtenerRolesActivos(): Flow<List<RoleEntity>>

    @Query("SELECT * FROM roles ORDER BY name ASC")
    fun obtenerTodos(): Flow<List<RoleEntity>>
}