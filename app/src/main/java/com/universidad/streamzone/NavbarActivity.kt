package com.universidad.streamzone

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.widget.TextView

class NavbarActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_navbar)

        val btnHome = findViewById<TextView>(R.id.btn_home)
        val btnGift = findViewById<TextView>(R.id.btn_gift)
        val btnLock = findViewById<TextView>(R.id.btn_lock)
        val btnSettings = findViewById<TextView>(R.id.btn_settings)

        btnHome.setOnClickListener { /* Acción para Home */ }
        btnGift.setOnClickListener { /* Acción para Gift */ }
        btnLock.setOnClickListener { /* Acción para Lock */ }
        btnSettings.setOnClickListener { /* Acción para Settings */ }
    }
}
