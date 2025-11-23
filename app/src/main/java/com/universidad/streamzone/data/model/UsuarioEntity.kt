package com.universidad.streamzone.data.model
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "usuarios",
    indices = [Index(value = ["email"], unique = true)]
)
data class UsuarioEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val fullname: String,
    val email: String,
    val phone: String,
    val password: String,
    val confirmPassword: String,
    val fotoBase64: String? = null,

    // Campo de administración
    val isAdmin: Boolean = false,

    // campo para control de sincronizacion
    val sincronizado: Boolean = false,

    // ID de Firebase cuando se sincroniza
    val firebaseId: String? = null
)