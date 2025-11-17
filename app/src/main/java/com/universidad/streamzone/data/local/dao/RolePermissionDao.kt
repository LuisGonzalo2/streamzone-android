package com.universidad.streamzone.data.local.dao

import androidx.room.*
import com.universidad.streamzone.data.model.RolePermissionEntity

@Dao
interface RolePermissionDao {

    @Insert
    suspend fun insertar(rolePermission: RolePermissionEntity): Long

    @Delete
    suspend fun eliminar(rolePermission: RolePermissionEntity)

    // Eliminar todos los permisos de un rol
    @Query("DELETE FROM role_permissions WHERE roleId = :roleId")
    suspend fun eliminarPermisosPorRol(roleId: Int)

    // Eliminar un permiso específico de un rol
    @Query("DELETE FROM role_permissions WHERE roleId = :roleId AND permissionId = :permissionId")
    suspend fun eliminarPermiso(roleId: Int, permissionId: Int)

    // Verificar si un rol tiene un permiso específico
    @Query("""
        SELECT COUNT(*) FROM role_permissions
        WHERE roleId = :roleId AND permissionId = :permissionId
    """)
    suspend fun tienePermiso(roleId: Int, permissionId: Int): Int
}