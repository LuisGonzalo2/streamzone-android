package com.universidad.carpoolapp

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity


class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Ir directamente al login
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
        finish() // Para que no se pueda volver atrás
    }
}