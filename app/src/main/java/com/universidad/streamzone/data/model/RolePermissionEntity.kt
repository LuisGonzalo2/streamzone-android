package com.universidad.streamzone.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "role_permissions",
    foreignKeys = [
        ForeignKey(
            entity = RoleEntity::class,
            parentColumns = ["id"],
            childColumns = ["roleId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = PermissionEntity::class,
            parentColumns = ["id"],
            childColumns = ["permissionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["roleId"]),
        Index(value = ["permissionId"])
    ]
)
data class RolePermissionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val roleId: Int,
    val permissionId: Int
)