package com.universidad.streamzone.model
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "usuarios")
data class UsuarioEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val fullname: String,
    val email: String,
    val phone: String,
    val password: String,
    val confirmPassword: String,

    // campo para control de sincronizacion
    val sincronizado: Boolean = false,

    // ID de Firebase cuando se sincroniza
    val firebaseId: String? = null
)