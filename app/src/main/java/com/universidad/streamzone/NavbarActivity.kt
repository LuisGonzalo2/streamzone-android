package com.universidad.streamzone

import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton

class NavbarActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_navbar)

        // Referencias
        val appName = findViewById<TextView>(R.id.tv_app_name)
        val btnHome = findViewById<TextView>(R.id.btn_home)
        val btnGift = findViewById<TextView>(R.id.btn_gift)
        val btnLock = findViewById<TextView>(R.id.btn_lock)
        val btnSettings = findViewById<TextView>(R.id.btn_settings)
        val btnTheme = findViewById<TextView>(R.id.btn_theme)
        val btnHelp = findViewById<MaterialButton>(R.id.btn_help)

        // Listeners con Toasts de ejemplo
        appName.setOnClickListener {
            Toast.makeText(this, "Ir al inicio (AppName)", Toast.LENGTH_SHORT).show()
        }

        btnHome.setOnClickListener {
            Toast.makeText(this, "Ir a la página principal", Toast.LENGTH_SHORT).show()
        }

        btnGift.setOnClickListener {
            Toast.makeText(this, "Ir a la sección de regalos", Toast.LENGTH_SHORT).show()
        }

        btnLock.setOnClickListener {
            Toast.makeText(this, "Ir a seguridad / login", Toast.LENGTH_SHORT).show()
        }

        btnSettings.setOnClickListener {
            Toast.makeText(this, "Ir a configuración", Toast.LENGTH_SHORT).show()
        }

        btnTheme.setOnClickListener {
            Toast.makeText(this, "Cambiar tema (modo claro/oscuro)", Toast.LENGTH_SHORT).show()
        }

        btnHelp.setOnClickListener {
            Toast.makeText(this, "Abrir chat de ayuda en vivo", Toast.LENGTH_SHORT).show()
        }
    }
}
