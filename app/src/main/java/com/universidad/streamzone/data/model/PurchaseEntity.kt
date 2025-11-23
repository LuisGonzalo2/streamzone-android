package com.universidad.streamzone.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "purchases")
data class PurchaseEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    // Información del usuario
    val userEmail: String,
    val userName: String,

    // Información del servicio
    val serviceId: String,
    val serviceName: String,
    val servicePrice: String,
    val serviceDuration: String, // "1 mes", "1 año", etc.

    // Credenciales del servicio
    val email: String? = null,
    val password: String? = null,

    // Fechas
    val purchaseDate: Long, // timestamp
    val expirationDate: Long, // timestamp

    // Estado
    val status: String, // "active", "expired", "pending"

    // Sincronización con Firebase
    val sincronizado: Boolean = false,
    val firebaseId: String? = null
)