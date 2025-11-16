package com.universidad.streamzone.data.local.dao

import androidx.room.*
import com.universidad.streamzone.data.model.OfferEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface OfferDao {

    @Insert
    suspend fun insertar(offer: OfferEntity): Long

    @Update
    suspend fun actualizar(offer: OfferEntity)

    @Delete
    suspend fun eliminar(offer: OfferEntity)

    @Query("SELECT * FROM offers WHERE id = :offerId")
    suspend fun obtenerPorId(offerId: Int): OfferEntity?

    @Query("SELECT * FROM offers ORDER BY startDate DESC")
    fun obtenerTodas(): Flow<List<OfferEntity>>

    @Query("SELECT * FROM offers WHERE isActive = 1 ORDER BY startDate DESC")
    fun obtenerActivas(): Flow<List<OfferEntity>>

    // Obtener oferta vigente actual
    @Query("""
        SELECT * FROM offers 
        WHERE isActive = 1 
        AND startDate <= :currentTime 
        AND endDate >= :currentTime 
        ORDER BY startDate DESC 
        LIMIT 1
    """)
    suspend fun obtenerOfertaVigente(currentTime: Long): OfferEntity?

    // Obtener ofertas no sincronizadas
    @Query("SELECT * FROM offers WHERE sincronizado = 0")
    suspend fun obtenerNoSincronizadas(): List<OfferEntity>

    // Marcar como sincronizada
    @Query("UPDATE offers SET sincronizado = 1, firebaseId = :firebaseId WHERE id = :id")
    suspend fun marcarComoSincronizada(id: Int, firebaseId: String)
}