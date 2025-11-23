package com.universidad.streamzone.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.universidad.streamzone.data.model.AdminMenuOptionEntity

@Dao
interface AdminMenuOptionDao {

    @Insert
    suspend fun insertar(option: AdminMenuOptionEntity): Long

    @Update
    suspend fun actualizar(option: AdminMenuOptionEntity)

    @Query("SELECT * FROM admin_menu_options WHERE id = :id")
    suspend fun obtenerPorId(id: Int): AdminMenuOptionEntity?

    @Query("SELECT * FROM admin_menu_options WHERE isActive = 1 ORDER BY orderIndex ASC")
    suspend fun obtenerOpcionesActivas(): List<AdminMenuOptionEntity>

    @Query("SELECT * FROM admin_menu_options ORDER BY orderIndex ASC")
    suspend fun obtenerTodas(): List<AdminMenuOptionEntity>

    @Query("DELETE FROM admin_menu_options WHERE id = :id")
    suspend fun eliminar(id: Int)
}