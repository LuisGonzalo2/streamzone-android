package com.universidad.streamzone.sync

import android.content.Context
import android.util.Log
import com.universidad.streamzone.cloud.FirebaseService
import com.universidad.streamzone.database.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object SyncService {
    private const val TAG = "SyncService"
    private var isSyncing = false

    /**
     * Sincroniza todos los usuarios pendientes de Room a Firebase
     */
    fun sincronizarUsuariosPendientes(context: Context, onComplete: (() -> Unit)? = null) {
        // Evitar sincronizaciones múltiples simultáneas
        if (isSyncing) {
            Log.d(TAG, "Ya hay una sincronización en curso, ignorando...")
            onComplete?.invoke()
            return
        }

        isSyncing = true

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val dao = AppDatabase.getInstance(context).usuarioDao()
                val usuariosNoSincronizados = dao.obtenerNoSincronizados()

                if (usuariosNoSincronizados.isEmpty()) {
                    Log.d(TAG, "No hay usuarios pendientes de sincronizar")
                    isSyncing = false
                    onComplete?.invoke()
                    return@launch
                }

                Log.d(TAG, "Sincronizando ${usuariosNoSincronizados.size} usuarios...")

                var sincronizados = 0
                var errores = 0

                usuariosNoSincronizados.forEach { usuario ->
                    Log.d(TAG, "Sincronizando usuario: ${usuario.email}")

                    FirebaseService.guardarUsuario(
                        usuario = usuario,
                        onSuccess = { firebaseId ->
                            CoroutineScope(Dispatchers.IO).launch {
                                try {
                                    dao.marcarComoSincronizado(usuario.id, firebaseId)
                                    sincronizados++
                                    Log.d(TAG, "✅ Usuario ${usuario.email} sincronizado correctamente con ID: $firebaseId")

                                    // Si terminamos con todos
                                    if (sincronizados + errores == usuariosNoSincronizados.size) {
                                        Log.d(TAG, "Sincronización completa: $sincronizados exitosos, $errores errores")
                                        isSyncing = false
                                        onComplete?.invoke()
                                    }
                                } catch (e: Exception) {
                                    Log.e(TAG, "Error al marcar como sincronizado", e)
                                    errores++
                                    if (sincronizados + errores == usuariosNoSincronizados.size) {
                                        isSyncing = false
                                        onComplete?.invoke()
                                    }
                                }
                            }
                        },
                        onFailure = { e ->
                            errores++
                            Log.e(TAG, "❌ Error al sincronizar usuario ${usuario.email}", e)

                            // Si terminamos con todos
                            if (sincronizados + errores == usuariosNoSincronizados.size) {
                                Log.d(TAG, "Sincronización completa: $sincronizados exitosos, $errores errores")
                                isSyncing = false
                                onComplete?.invoke()
                            }
                        }
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error general en sincronización", e)
                isSyncing = false
                onComplete?.invoke()
            }
        }
    }
}