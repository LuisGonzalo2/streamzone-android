package com.universidad.streamzone.data.local.dao

import androidx.room.*
import com.universidad.streamzone.data.model.RolePermissionEntity

@Dao
interface RolePermissionDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertar(rolePermission: RolePermissionEntity): Long

    @Delete
    suspend fun eliminar(rolePermission: RolePermissionEntity)

    // Metodo necesario para verificar permisos
    @Query("SELECT * FROM role_permissions WHERE roleId = :roleId")
    suspend fun obtenerPermisosPorRol(roleId: Int): List<RolePermissionEntity>

    // Eliminar todos los permisos de un rol
    @Query("DELETE FROM role_permissions WHERE roleId = :roleId")
    suspend fun eliminarPermisosPorRol(roleId: Int)

    // Asignar múltiples permisos a un rol
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun asignarRoles(rolePermissions: List<RolePermissionEntity>)
}