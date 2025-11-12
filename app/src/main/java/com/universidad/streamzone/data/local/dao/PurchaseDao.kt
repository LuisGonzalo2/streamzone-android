package com.universidad.streamzone.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.universidad.streamzone.data.model.PurchaseEntity
import com.universidad.streamzone.data.model.ServicioPopular
import kotlinx.coroutines.flow.Flow

@Dao
interface PurchaseDao {

    @Insert
    suspend fun insertar(purchase: PurchaseEntity): Long

    @Update
    suspend fun actualizar(purchase: PurchaseEntity)

    // Obtener todas las compras de un usuario
    @Query("SELECT * FROM purchases WHERE userEmail = :email ORDER BY purchaseDate DESC")
    fun obtenerComprasPorUsuario(email: String): Flow<List<PurchaseEntity>>

    // Obtener compras activas de un usuario
    @Query("SELECT * FROM purchases WHERE userEmail = :email AND status = 'active' ORDER BY purchaseDate DESC")
    fun obtenerComprasActivas(email: String): Flow<List<PurchaseEntity>>

    // Obtener compras no sincronizadas
    @Query("SELECT * FROM purchases WHERE sincronizado = 0")
    suspend fun obtenerNoSincronizadas(): List<PurchaseEntity>

    // Marcar como sincronizada
    @Query("UPDATE purchases SET sincronizado = 1, firebaseId = :firebaseId WHERE id = :id")
    suspend fun marcarComoSincronizada(id: Int, firebaseId: String)

    // Actualizar estado de compra
    @Query("UPDATE purchases SET status = :status WHERE id = :id")
    suspend fun actualizarEstado(id: Int, status: String)

    // Obtener una compra por ID
    @Query("SELECT * FROM purchases WHERE id = :id")
    suspend fun obtenerPorId(id: Int): PurchaseEntity?

    // Eliminar compras expiradas
    @Query("DELETE FROM purchases WHERE expirationDate < :timestamp AND status = 'expired'")
    suspend fun eliminarExpiradas(timestamp: Long)

    // Obtener los servicios más populares (top 3 más vendidos)
    @Query("""
    SELECT serviceId, serviceName, servicePrice, COUNT(*) as purchaseCount
    FROM purchases
    WHERE status IN ('active', 'pending')
    GROUP BY serviceId
    ORDER BY purchaseCount DESC
    LIMIT 3
""")
    suspend fun obtenerServiciosMasPopulares(): List<ServicioPopular>
}