package com.universidad.streamzone

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface UserDao {
    @Insert
    fun insert(user: UserEntity): Long

    @Query("SELECT * FROM users ORDER BY id DESC")
    fun getAll(): List<UserEntity>
}

