package com.universidad.streamzone

import android.app.Application
import android.util.Log
import com.google.firebase.FirebaseApp

class StreamZoneApp : Application() {

    override fun onCreate() {
        super.onCreate()

        try {
            // Inicializar Firebase
            FirebaseApp.initializeApp(this)
            Log.d("StreamZoneApp", "Firebase inicializado correctamente")
        } catch (e: Exception) {
            Log.e("StreamZoneApp", "Error al inicializar Firebase", e)
        }
    }
}