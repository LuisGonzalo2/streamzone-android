package com.universidad.streamzone.data.local.dao

import androidx.room.*
import com.universidad.streamzone.data.model.OfferEntity

@Dao
interface OfferDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(offer: OfferEntity): Long

    @Update
    suspend fun update(offer: OfferEntity)

    @Delete
    suspend fun delete(offer: OfferEntity)

    @Query("SELECT * FROM offers WHERE id = :offerId")
    suspend fun getById(offerId: Long): OfferEntity?

    @Query("SELECT * FROM offers ORDER BY startDate DESC")
    suspend fun getAll(): List<OfferEntity>

    @Query("""
        SELECT * FROM offers
        WHERE isActive = 1
        AND startDate <= :currentTime
        AND endDate >= :currentTime
        ORDER BY startDate DESC
        LIMIT 1
    """)
    suspend fun getActiveOffer(currentTime: Long = System.currentTimeMillis()): OfferEntity?

    @Query("SELECT * FROM offers WHERE isActive = 1")
    suspend fun getAllActive(): List<OfferEntity>

    @Query("UPDATE offers SET isActive = 0")
    suspend fun deactivateAll()

    @Query("UPDATE offers SET isActive = :isActive WHERE id = :offerId")
    suspend fun updateActiveStatus(offerId: Long, isActive: Boolean)

    @Query("DELETE FROM offers WHERE id = :offerId")
    suspend fun deleteById(offerId: Long)

    @Query("SELECT * FROM offers WHERE sincronizado = 0")
    suspend fun getUnsynchronized(): List<OfferEntity>

    @Query("UPDATE offers SET sincronizado = 1, firebaseId = :firebaseId WHERE id = :id")
    suspend fun markAsSynchronized(id: Long, firebaseId: String)
}