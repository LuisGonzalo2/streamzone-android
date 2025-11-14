package com.universidad.streamzone.data.local.dao

import androidx.room.*
import com.universidad.streamzone.data.model.RoleEntity
import com.universidad.streamzone.data.model.UserRoleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserRoleDao {

    @Insert
    suspend fun insertar(userRole: UserRoleEntity): Long

    @Delete
    suspend fun eliminar(userRole: UserRoleEntity)

    // Obtener todos los roles de un usuario
    @Query("""
        SELECT r.* FROM roles r
        INNER JOIN user_roles ur ON r.id = ur.roleId
        WHERE ur.userId = :userId
    """)
    fun obtenerRolesPorUsuario(userId: Int): Flow<List<RoleEntity>>

    // Verificar si un usuario tiene un rol específico
    @Query("""
        SELECT COUNT(*) FROM user_roles
        WHERE userId = :userId AND roleId = :roleId
    """)
    suspend fun tieneRol(userId: Int, roleId: Int): Int

    // Eliminar todos los roles de un usuario
    @Query("DELETE FROM user_roles WHERE userId = :userId")
    suspend fun eliminarRolesPorUsuario(userId: Int)

    // Asignar múltiples roles a un usuario
    @Insert
    suspend fun asignarRoles(userRoles: List<UserRoleEntity>)
}