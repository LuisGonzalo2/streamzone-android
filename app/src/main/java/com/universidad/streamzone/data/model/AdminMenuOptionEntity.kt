package com.universidad.streamzone.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entidad para opciones del menú de administrador
 * Permite configurar dinámicamente qué opciones se muestran en el panel de admin
 */
@Entity(tableName = "admin_menu_options")
data class AdminMenuOptionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val title: String, // "Gestionar Compras"
    val description: String, // "Ver y administrar compras pendientes"
    val icon: String, // Emoji: "📦"
    val permissionCode: String, // "MANAGE_PURCHASES"
    val activityClass: String, // "PendingPurchasesActivity"
    val orderIndex: Int, // Para ordenar las opciones
    val isActive: Boolean = true
)