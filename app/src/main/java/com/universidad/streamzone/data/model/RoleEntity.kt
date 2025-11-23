package com.universidad.streamzone.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "roles")
data class RoleEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String, // "Super Admin", "Gestor de Compras", etc.
    val description: String,
    val isActive: Boolean = true,
    val sincronizado: Boolean = false,
    val firebaseId: String? = null
)