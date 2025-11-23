package com.universidad.streamzone.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.universidad.streamzone.data.model.NotificationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NotificationDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(notification: NotificationEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(notifications: List<NotificationEntity>)

    @Update
    suspend fun update(notification: NotificationEntity)

    @Query("SELECT * FROM notifications ORDER BY timestamp DESC")
    fun getAllFlow(): Flow<List<NotificationEntity>>

    @Query("SELECT * FROM notifications WHERE userId IS NULL OR userId = :userId ORDER BY timestamp DESC")
    fun getNotificationsForUserFlow(userId: String): Flow<List<NotificationEntity>>

    @Query("SELECT * FROM notifications WHERE userId IS NULL OR userId = :userId ORDER BY timestamp DESC")
    suspend fun getNotificationsForUser(userId: String): List<NotificationEntity>

    @Query("SELECT COUNT(*) FROM notifications WHERE isRead = 0 AND (userId IS NULL OR userId = :userId)")
    fun getUnreadCountFlow(userId: String): Flow<Int>

    @Query("SELECT COUNT(*) FROM notifications WHERE isRead = 0 AND (userId IS NULL OR userId = :userId)")
    suspend fun getUnreadCount(userId: String): Int

    @Query("UPDATE notifications SET isRead = 1 WHERE id = :notificationId")
    suspend fun markAsRead(notificationId: Int)

    @Query("UPDATE notifications SET isRead = 1 WHERE userId IS NULL OR userId = :userId")
    suspend fun markAllAsRead(userId: String)

    @Query("DELETE FROM notifications WHERE id = :notificationId")
    suspend fun delete(notificationId: Int)

    @Query("DELETE FROM notifications WHERE timestamp < :timestamp")
    suspend fun deleteOlderThan(timestamp: Long)

    @Query("DELETE FROM notifications")
    suspend fun deleteAll()
}