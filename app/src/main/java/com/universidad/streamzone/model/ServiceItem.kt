package com.universidad.streamzone.model

/**
 * Modelo de datos para un servicio de streaming
 *
 * @param id Identificador único del servicio (netflix, disney_plus_premium, etc.)
 * @param title Nombre visible del servicio
 * @param price Precio formateado con moneda (ej: "US$ 4,00 /mes")
 * @param description Descripción corta (ej: "Acceso inmediato")
 * @param iconGradientStart Color inicial del gradiente del ícono en formato hex
 * @param iconGradientEnd Color final del gradiente del ícono en formato hex
 */
data class ServiceItem(
    val id: String,
    val title: String,
    val price: String,
    val description: String,
    val iconGradientStart: String,
    val iconGradientEnd: String
) {
    /**
     * Obtiene la primera letra del título para usar como ícono
     */
    fun getIconLetter(): String {
        return title.firstOrNull()?.uppercase() ?: "S"
    }
}