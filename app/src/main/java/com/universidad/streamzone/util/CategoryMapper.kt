package com.universidad.streamzone.util

import android.graphics.Color
import com.universidad.streamzone.data.model.Category
import com.universidad.streamzone.data.model.CategoryEntity

/**
 * Extensiones para convertir entre CategoryEntity (BD) y Category (UI)
 */

/**
 * Convierte Firebase Category a CategoryEntity para la UI
 */
fun com.universidad.streamzone.data.firebase.models.Category.toCategoryEntity(): CategoryEntity {
    return CategoryEntity(
        id = this.id.hashCode(),
        categoryId = this.categoryId,
        name = this.name,
        icon = this.icon,
        description = this.description,
        gradientStart = this.gradientStart,
        gradientEnd = this.gradientEnd,
        isActive = this.isActive,
        order = this.order,
        sincronizado = true,
        firebaseId = this.id
    )
}

/**
 * Convierte una lista de Firebase Category a lista de CategoryEntity
 */
fun List<com.universidad.streamzone.data.firebase.models.Category>.toCategoryEntityList(): List<CategoryEntity> {
    return this.map { it.toCategoryEntity() }
}

/**
 * Convierte CategoryEntity (base de datos) a Category (modelo de UI)
 * Nota: serviceCount y serviceIds deben ser calculados externamente
 */
fun CategoryEntity.toCategory(serviceCount: Int = 0): Category {
    return Category(
        id = this.categoryId,
        name = this.name,
        icon = this.icon,
        description = this.description,
        serviceCount = serviceCount,
        gradientStart = parseColorToInt(this.gradientStart),
        gradientEnd = parseColorToInt(this.gradientEnd),
        serviceIds = emptyList() // Ya no se usa, se carga dinámicamente
    )
}

/**
 * Convierte un color hex string (#RRGGBB) a Int
 */
private fun parseColorToInt(hexColor: String): Int {
    return try {
        Color.parseColor(hexColor)
    } catch (e: Exception) {
        Color.parseColor("#6366F1") // Color por defecto
    }
}

/**
 * Convierte una lista de CategoryEntity a lista de Category
 */
fun List<CategoryEntity>.toCategoryList(serviceCounts: Map<Int, Int> = emptyMap()): List<Category> {
    return this.map { categoryEntity ->
        val count = serviceCounts[categoryEntity.id] ?: 0
        categoryEntity.toCategory(count)
    }
}