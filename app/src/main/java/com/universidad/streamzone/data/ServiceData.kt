package com.universidad.streamzone.data

import com.universidad.streamzone.model.ServiceItem

object ServiceData {

    /**
     * Lista de servicios disponibles en la plataforma
     */
    fun getServices(): List<ServiceItem> {
        return listOf(
            ServiceItem(
                id = "netflix",
                title = "Netflix",
                price = "US$ 4,00 /mes",
                description = "Acceso inmediato",
                iconGradientStart = "#ff4b4b",
                iconGradientEnd = "#b20a0a"
            ),
            ServiceItem(
                id = "disney_plus_premium",
                title = "Disney+ Premium",
                price = "US$ 3,75 /mes",
                description = "Acceso inmediato",
                iconGradientStart = "#4ea1ff",
                iconGradientEnd = "#1766d6"
            ),
            ServiceItem(
                id = "disney_plus_standard",
                title = "Disney+ Standard",
                price = "US$ 3,25 /mes",
                description = "Acceso inmediato",
                iconGradientStart = "#4ea1ff",
                iconGradientEnd = "#1766d6"
            ),
            ServiceItem(
                id = "max",
                title = "Max",
                price = "US$ 3,00 /mes",
                description = "Acceso inmediato",
                iconGradientStart = "#b374ff",
                iconGradientEnd = "#6b2bff"
            )
        )
    }

    /**
     * Obtiene un servicio por su ID
     */
    fun getServiceById(id: String): ServiceItem? {
        return getServices().find { it.id == id }
    }
}