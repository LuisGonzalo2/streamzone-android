package com.universidad.streamzone

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity

// Actividad mínima que recibe los extras enviados desde HomeNativeActivity
class ReserveActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // No es obligatorio inflar una UI completa para compilar; solo leer los extras

        val serviceId = intent.getStringExtra("SERVICE_ID") ?: ""
        val serviceTitle = intent.getStringExtra("SERVICE_TITLE") ?: ""
        val servicePrice = intent.getStringExtra("SERVICE_PRICE") ?: ""
        val serviceDesc = intent.getStringExtra("SERVICE_DESC") ?: ""
        val userFullname = intent.getStringExtra("USER_FULLNAME") ?: ""

        Log.d("ReserveActivity", "Servicio: $serviceId - $serviceTitle - $servicePrice - $serviceDesc - Usuario: $userFullname")

        // Si se desea, se puede establecer un layout existente:
        // setContentView(R.layout.activity_reserve)
    }
}
