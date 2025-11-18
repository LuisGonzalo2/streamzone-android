package com.universidad.streamzone.data.local.dao

import androidx.room.*
import com.universidad.streamzone.data.model.PermissionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PermissionDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertar(permission: PermissionEntity): Long

    @Update
    suspend fun actualizar(permission: PermissionEntity)

    @Query("SELECT * FROM permissions WHERE id = :permissionId")
    suspend fun obtenerPorId(permissionId: Int): PermissionEntity?

    @Query("SELECT * FROM permissions ORDER BY name ASC")
     fun obtenerTodos(): Flow<List<PermissionEntity>>

    // Obtener permisos de un rol específico
    @Query("""
        SELECT p.* FROM permissions p
        INNER JOIN role_permissions rp ON p.id = rp.permissionId
        WHERE rp.roleId = :roleId
    """)
    suspend fun obtenerPermisosPorRol(roleId: Int): List<PermissionEntity>

    // Alias para compatibilidad (acepta Long)
    @Query("""
        SELECT p.* FROM permissions p
        INNER JOIN role_permissions rp ON p.id = rp.permissionId
        WHERE rp.roleId = :roleId
    """)
    suspend fun getPermissionsByRole(roleId: Long): List<PermissionEntity>

    @Query("SELECT * FROM permissions ORDER BY name ASC")
    suspend fun getAll(): List<PermissionEntity>
}