package com.universidad.streamzone.data.local.dao

import androidx.room.*
import com.universidad.streamzone.data.model.ServiceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ServiceDao {

    @Insert
    suspend fun insertar(service: ServiceEntity): Long

    @Update
    suspend fun actualizar(service: ServiceEntity)

    @Delete
    suspend fun eliminar(service: ServiceEntity)

    @Query("SELECT * FROM services WHERE id = :serviceId")
    suspend fun obtenerPorId(serviceId: Int): ServiceEntity?

    @Query("SELECT * FROM services WHERE serviceId = :serviceId")
    suspend fun obtenerPorServiceId(serviceId: String): ServiceEntity?

    @Query("SELECT * FROM services WHERE isActive = 1 ORDER BY name ASC")
    fun obtenerServiciosActivos(): Flow<List<ServiceEntity>>

    @Query("SELECT * FROM services WHERE categoryId = :categoryId AND isActive = 1")
    fun obtenerServiciosPorCategoria(categoryId: Int): Flow<List<ServiceEntity>>

    @Query("SELECT * FROM services ORDER BY name ASC")
    fun obtenerTodos(): Flow<List<ServiceEntity>>
}