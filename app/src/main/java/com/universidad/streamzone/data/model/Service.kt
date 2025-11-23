package com.universidad.streamzone.data.model

data class Service(
    val id: String,
    val title: String,
    val price: String,
    val desc: String,
    val iconRes: Int? = null, // drawable opcional para el icono redondeado
    val iconBase64: String? = null // Imagen en base64
)