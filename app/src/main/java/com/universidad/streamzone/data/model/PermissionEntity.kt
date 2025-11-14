package com.universidad.streamzone.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "permissions")
data class PermissionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val code: String, // "MANAGE_PURCHASES", "EDIT_SERVICES", etc.
    val name: String,
    val description: String
)