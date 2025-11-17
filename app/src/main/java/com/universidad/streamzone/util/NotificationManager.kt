package com.universidad.streamzone.util

import android.app.NotificationChannel
import android.app.NotificationManager as AndroidNotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.universidad.streamzone.R
import com.universidad.streamzone.ui.notifications.NotificationsActivity

object NotificationManager {

    private const val CHANNEL_ID = "streamzone_notifications"
    private const val CHANNEL_NAME = "Notificaciones de StreamZone"
    private const val CHANNEL_DESCRIPTION = "Notificaciones sobre nuevos servicios, ofertas y actualizaciones"

    private var notificationId = 1000

    /**
     * Crea el canal de notificaciones (requerido para Android O+)
     */
    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = AndroidNotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = CHANNEL_DESCRIPTION
            }

            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as AndroidNotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * Muestra una notificación local
     */
    fun showNotification(
        context: Context,
        title: String,
        message: String,
        icon: String = "🔔"
    ) {
        createNotificationChannel(context)

        // Intent para abrir la pantalla de notificaciones al hacer clic
        val intent = Intent(context, NotificationsActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Construir la notificación
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("$icon $title")
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        // Mostrar la notificación
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as AndroidNotificationManager
        notificationManager.notify(notificationId++, notification)
    }

    /**
     * Cancela todas las notificaciones
     */
    fun cancelAllNotifications(context: Context) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as AndroidNotificationManager
        notificationManager.cancelAll()
    }
}