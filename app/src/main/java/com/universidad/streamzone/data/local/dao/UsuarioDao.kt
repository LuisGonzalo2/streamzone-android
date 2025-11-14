package com.universidad.streamzone.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.universidad.streamzone.data.model.PermissionEntity
import com.universidad.streamzone.data.model.UsuarioEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UsuarioDao {

    @Insert
    suspend fun insertar(usuario: UsuarioEntity): Long

    @Update
    suspend fun actualizar(usuario: UsuarioEntity)

    @Query("SELECT * FROM usuarios ORDER BY id DESC")
    fun obtenerTodos(): Flow<List<UsuarioEntity>>

    // Buscar usuario por email
    @Query("SELECT * FROM usuarios WHERE email = :email LIMIT 1")
    suspend fun buscarPorEmail(email: String): UsuarioEntity?

    // Buscar usuario por ID
    @Query("SELECT * FROM usuarios WHERE id = :userId LIMIT 1")
    suspend fun buscarPorId(userId: Int): UsuarioEntity?

    // Buscar usuario por número de teléfono
    @Query("SELECT * FROM usuarios WHERE phone = :phone LIMIT 1")
    suspend fun buscarPorTelefono(phone: String): UsuarioEntity?

    // Obtener usuarios no sincronizados
    @Query("SELECT * FROM usuarios WHERE sincronizado = 0")
    suspend fun obtenerNoSincronizados(): List<UsuarioEntity>

    // Marcar usuario como sincronizado
    @Query("UPDATE usuarios SET sincronizado = 1, firebaseId = :firebaseId WHERE id = :id")
    suspend fun marcarComoSincronizado(id: Int, firebaseId: String)

    // ⬅️ NUEVAS FUNCIONES PARA ADMIN

    // Obtener todos los usuarios administradores
    @Query("SELECT * FROM usuarios WHERE isAdmin = 1 ORDER BY fullname ASC")
    fun obtenerAdministradores(): Flow<List<UsuarioEntity>>

    // Buscar usuarios por nombre o email (para filtros)
    @Query("""
        SELECT * FROM usuarios 
        WHERE fullname LIKE '%' || :query || '%' 
        OR email LIKE '%' || :query || '%'
        ORDER BY fullname ASC
    """)
    fun buscarUsuarios(query: String): Flow<List<UsuarioEntity>>

    // Obtener TODOS los permisos de un usuario (combinando todos sus roles)
    @Query("""
        SELECT DISTINCT p.* FROM permissions p
        INNER JOIN role_permissions rp ON p.id = rp.permissionId
        INNER JOIN user_roles ur ON rp.roleId = ur.roleId
        WHERE ur.userId = :userId
    """)
    suspend fun obtenerPermisosDeUsuario(userId: Int): List<PermissionEntity>

    // Verificar si un usuario tiene un permiso específico
    @Query("""
        SELECT COUNT(*) FROM permissions p
        INNER JOIN role_permissions rp ON p.id = rp.permissionId
        INNER JOIN user_roles ur ON rp.roleId = ur.roleId
        WHERE ur.userId = :userId AND p.code = :permissionCode
    """)
    suspend fun tienePermiso(userId: Int, permissionCode: String): Int
}