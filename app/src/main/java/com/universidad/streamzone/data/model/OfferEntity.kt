package com.universidad.streamzone.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "offers")
data class OfferEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    // Información básica de la oferta
    val title: String,
    val description: String,

    // IDs de servicios incluidos en el combo (separados por coma)
    // Ejemplo: "1,5" para Netflix + Spotify
    val serviceIds: String,

    // Precios
    val originalPrice: Double,  // Suma de precios originales
    val comboPrice: Double,     // Precio del combo con descuento
    val discountPercent: Int,   // Porcentaje de descuento

    // Vigencia
    val startDate: Long,        // Timestamp
    val endDate: Long,          // Timestamp
    val isActive: Boolean = true,

    // Sincronización con Firebase
    val sincronizado: Boolean = false,
    val firebaseId: String? = null
)