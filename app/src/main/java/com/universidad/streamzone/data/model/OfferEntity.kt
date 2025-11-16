package com.universidad.streamzone.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "offers")
data class OfferEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val title: String, // "Combo: Netflix + Spotify"
    val description: String, // "Ahorra 20%"
    val originalPrice: String, // "US$ 7.50"
    val discountPrice: String, // "US$ 6.00"
    val serviceIds: String, // "netflix,spotify" (CSV)
    val bannerText: String, // "OFERTA DEL MES"
    val startDate: Long, // timestamp
    val endDate: Long, // timestamp
    val isActive: Boolean = true,
    val sincronizado: Boolean = false,
    val firebaseId: String? = null
)