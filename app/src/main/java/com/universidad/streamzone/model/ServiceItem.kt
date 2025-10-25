package com.universidad.streamzone.model

data class ServiceItem(
    val id: String,
    val title: String,
    val priceText: String,
    val iconText: String? = null, // letra o siglas para mostrar en el icono
    val iconRes: Int? = null,     // opcional recurso drawable/mipmap
    val colorHex: String,         // color principal para el icono (ej. #E50914)
    val isAnnual: Boolean = false,
    val showBadge: Boolean = true
)

