package com.universidad.streamzone.application

import android.app.Application
import android.util.Log
import com.google.firebase.FirebaseApp
import com.universidad.streamzone.util.DatabaseInitializer
import com.universidad.streamzone.util.NotificationManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class StreamZoneApp : Application() {

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onCreate() {
        super.onCreate()

        try {
            // Inicializar Firebase
            FirebaseApp.initializeApp(this)
            Log.d("StreamZoneApp", "Firebase inicializado correctamente")
        } catch (e: Exception) {
            Log.e("StreamZoneApp", "Error al inicializar Firebase", e)
        }

        // Inicializar canal de notificaciones
        try {
            NotificationManager.createNotificationChannel(this)
            Log.d("StreamZoneApp", "Canal de notificaciones creado")
        } catch (e: Exception) {
            Log.e("StreamZoneApp", "Error al crear canal de notificaciones", e)
        }

        // Inicializar base de datos con datos predefinidos
        applicationScope.launch {
            try {
                DatabaseInitializer.initializeDatabase(this@StreamZoneApp)
                Log.d("StreamZoneApp", "✅ Base de datos inicializada correctamente")
            } catch (e: Exception) {
                Log.e("StreamZoneApp", "❌ Error al inicializar base de datos", e)
            }
        }
    }
}