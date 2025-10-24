package com.universidad.streamzone.model

data class CatalogItem(
    val id: Int,
    val title: String,
    val price: String,
    val shortDesc: String,
    val drawableRes: Int? = null
)

