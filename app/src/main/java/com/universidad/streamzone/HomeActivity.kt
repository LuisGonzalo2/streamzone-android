package com.universidad.streamzone

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.universidad.streamzone.sync.SyncService

class HomeActivity : AppCompatActivity() {

    private lateinit var sharedPrefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        sharedPrefs = getSharedPreferences("StreamZoneData", MODE_PRIVATE)

        setupWelcomeMessage()
        setupButtons()

        // Sincronizar datos pendientes si hay internet
        if (isNetworkAvailable()) {
            SyncService.sincronizarUsuariosPendientes(this) {
                runOnUiThread {
                    Toast.makeText(
                        this,
                        "✅ Datos sincronizados con la nube",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun setupWelcomeMessage() {
        val userName = sharedPrefs.getString("logged_in_user_name", "Usuario")
        val userEmail = sharedPrefs.getString("logged_in_user_email", "")

        findViewById<TextView>(R.id.txtTituloHome).text = "Bienvenido, $userName"
        findViewById<TextView>(R.id.txtSubtituloHome).text = userEmail

        // Mostrar estado de conexión
        val estadoConexion = if (isNetworkAvailable()) {
            "Conectado"
        } else {
            "Sin conexión"
        }
        findViewById<TextView>(R.id.txtEstadoConexion).text = estadoConexion
    }

    private fun setupButtons() {
        findViewById<Button>(R.id.btnCerrarSesion).setOnClickListener {
            cerrarSesion()
        }

        findViewById<Button>(R.id.btnSincronizar).setOnClickListener {
            sincronizarManualmente()
        }
    }

    private fun cerrarSesion() {
        sharedPrefs.edit().apply {
            remove("logged_in_user_email")
            remove("logged_in_user_name")
            apply()
        }

        Toast.makeText(this, "👋 Sesión cerrada", Toast.LENGTH_SHORT).show()

        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
        finish()
    }

    private fun sincronizarManualmente() {
        if (!isNetworkAvailable()) {
            Toast.makeText(
                this,
                "📴 No hay conexión a internet",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        // Deshabilitar el botón mientras sincroniza
        val btnSincronizar = findViewById<Button>(R.id.btnSincronizar)
        btnSincronizar.isEnabled = false
        btnSincronizar.text = "⏳ Sincronizando..."

        Toast.makeText(this, "🔄 Sincronizando...", Toast.LENGTH_SHORT).show()

        SyncService.sincronizarUsuariosPendientes(this) {
            runOnUiThread {
                btnSincronizar.isEnabled = true
                btnSincronizar.text = "🔄 Sincronizar datos"
                Toast.makeText(
                    this,
                    "✅ Sincronización completada",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false

        return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
    }
}