package com.universidad.streamzone.services

import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.universidad.streamzone.data.firebase.models.Notification
import com.universidad.streamzone.data.firebase.models.NotificationType
import com.universidad.streamzone.data.firebase.repository.NotificationRepository
import com.universidad.streamzone.util.NotificationManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class FCMService : FirebaseMessagingService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val notificationRepository = NotificationRepository()
    private val TAG = "FCMService"

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "Nuevo token FCM: $token")

        // Aquí podrías enviar el token a tu servidor backend
        // para poder enviar notificaciones push al dispositivo
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        Log.d(TAG, "Mensaje recibido de: ${message.from}")

        // Obtener datos del mensaje
        val title = message.data["title"] ?: message.notification?.title ?: "Notificación"
        val body = message.data["message"] ?: message.notification?.body ?: ""
        val type = message.data["type"] ?: "SYSTEM"
        val actionType = message.data["actionType"]
        val actionData = message.data["actionData"]
        val userId = message.data["userId"]
        val icon = message.data["icon"] ?: "🔔"

        // Guardar notificación en Firebase
        serviceScope.launch {
            try {
                val notification = Notification(
                    title = title,
                    message = body,
                    type = try {
                        NotificationType.valueOf(type)
                    } catch (e: Exception) {
                        NotificationType.SYSTEM
                    },
                    timestamp = Timestamp.now(),
                    isRead = false,
                    userId = userId,
                    actionType = actionType,
                    actionData = actionData,
                    icon = icon,
                    createdAt = Timestamp.now()
                )

                notificationRepository.insert(notification)

                // Mostrar notificación local
                NotificationManager.showNotification(
                    context = applicationContext,
                    title = title,
                    message = body,
                    icon = icon
                )

            } catch (e: Exception) {
                Log.e(TAG, "Error al procesar notificación", e)
            }
        }
    }
}