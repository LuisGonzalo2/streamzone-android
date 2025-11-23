package com.universidad.streamzone.util

import com.universidad.streamzone.data.firebase.models.Role
import com.universidad.streamzone.data.model.RoleEntity

/**
 * Convierte Firebase Role a RoleEntity para la UI
 */
fun Role.toRoleEntity(): RoleEntity {
    return RoleEntity(
        id = this.id.hashCode(),
        name = this.name,
        description = this.description,
        isActive = this.isActive,
        sincronizado = true,
        firebaseId = this.id
    )
}

/**
 * Convierte lista de Firebase Role a lista de RoleEntity
 */
fun List<Role>.toRoleEntityList(): List<RoleEntity> {
    return this.map { it.toRoleEntity() }
}
