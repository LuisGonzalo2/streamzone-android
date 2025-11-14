package com.universidad.streamzone.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "services")
data class ServiceEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val serviceId: String, // "netflix", "spotify"
    val name: String,
    val price: String,
    val description: String,
    val iconUrl: String? = null, // URL de Firebase Storage
    val iconDrawable: Int? = null, // Drawable local (temporal)
    val categoryId: Int,
    val isActive: Boolean = true,
    val sincronizado: Boolean = false,
    val firebaseId: String? = null
)