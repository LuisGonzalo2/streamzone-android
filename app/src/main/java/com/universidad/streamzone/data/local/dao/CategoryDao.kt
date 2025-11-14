package com.universidad.streamzone.data.local.dao

import androidx.room.*
import com.universidad.streamzone.data.model.CategoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {

    @Insert
    suspend fun insertar(category: CategoryEntity): Long

    @Update
    suspend fun actualizar(category: CategoryEntity)

    @Delete
    suspend fun eliminar(category: CategoryEntity)

    @Query("SELECT * FROM categories WHERE id = :categoryId")
    suspend fun obtenerPorId(categoryId: Int): CategoryEntity?

    @Query("SELECT * FROM categories WHERE categoryId = :categoryId")
    suspend fun obtenerPorCategoryId(categoryId: String): CategoryEntity?

    @Query("SELECT * FROM categories WHERE isActive = 1 ORDER BY name ASC")
    fun obtenerCategoriasActivas(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories ORDER BY name ASC")
    fun obtenerTodas(): Flow<List<CategoryEntity>>
}