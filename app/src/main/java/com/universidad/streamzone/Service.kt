package com.universidad.streamzone

data class Service(
    val id: String,
    val title: String,
    val price: String,
    val desc: String,
    val iconRes: Int? = null // drawable opcional para el icono redondeado
)
