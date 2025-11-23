package com.universidad.streamzone.data.model

data class Category (
    val id: String,
    val name: String,
    val icon: String, // emoji
    val description: String,
    val serviceCount: Int,
    val gradientStart: Int, // color resource ID
    val gradientEnd: Int,   // color resource ID
    val serviceIds: List<String> // IDs de servicios en esta categoría
)