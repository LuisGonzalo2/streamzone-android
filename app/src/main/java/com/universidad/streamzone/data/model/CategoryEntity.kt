package com.universidad.streamzone.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val categoryId: String, // "streaming", "music"
    val name: String,
    val icon: String, // emoji
    val description: String,
    val gradientStart: String, // color hex "#FF5733"
    val gradientEnd: String,
    val isActive: Boolean = true,
    val sincronizado: Boolean = false,
    val order: Int = 0,
    val firebaseId: String? = null
)