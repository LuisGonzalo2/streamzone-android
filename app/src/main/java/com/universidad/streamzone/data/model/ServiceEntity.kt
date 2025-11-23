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
    val iconBase64: String? = null, // Imagen en base64
    val iconUrl: String? = null, // URL de Firebase Storage
    val iconDrawable: Int? = null, // Drawable local (temporal)
    val categoryId: Int,
    val isActive: Boolean = true,
    val isPopular: Boolean = false, // Para marcar servicios populares
    val sincronizado: Boolean = false,
    val firebaseId: String? = null
)