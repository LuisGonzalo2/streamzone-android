package com.universidad.streamzone.ui.notifications

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.universidad.streamzone.R
import com.universidad.streamzone.data.local.database.AppDatabase
import com.universidad.streamzone.data.model.NotificationEntity
import com.universidad.streamzone.services.NotificationListenerService
import com.universidad.streamzone.ui.components.NavbarManager
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class NotificationsActivity : AppCompatActivity() {

    private lateinit var navbarManager: NavbarManager
    private lateinit var rvNotifications: RecyclerView
    private lateinit var emptyState: LinearLayout
    private lateinit var notificationAdapter: NotificationAdapter
    private lateinit var notificationListenerService: NotificationListenerService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notifications)

        // Configurar padding para el notch
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false)
        }

        // Aplicar padding superior al contenedor principal
        val mainContainer = findViewById<View>(R.id.notifications_main_container)
        mainContainer?.setOnApplyWindowInsetsListener { view, insets ->
            val systemBars = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                insets.getInsets(android.view.WindowInsets.Type.systemBars())
            } else {
                @Suppress("DEPRECATION")
                android.graphics.Insets.of(0, insets.systemWindowInsetTop, 0, 0)
            }
            view.setPadding(
                view.paddingLeft,
                systemBars.top + 16,
                view.paddingRight,
                view.paddingBottom
            )
            insets
        }

        // Configurar navbar
        navbarManager = NavbarManager(this, NavbarManager.Screen.NOTIFICATIONS)

        // Iniciar listeners de notificaciones
        notificationListenerService = NotificationListenerService(this)
        notificationListenerService.startListening()

        initViews()
        loadNotifications()
    }

    override fun onDestroy() {
        super.onDestroy()
        notificationListenerService.stopListening()
    }

    private fun initViews() {
        rvNotifications = findViewById(R.id.rv_notifications)
        emptyState = findViewById(R.id.empty_state_notifications)

        // Configurar RecyclerView
        notificationAdapter = NotificationAdapter(
            onNotificationClick = { notification ->
                markAsRead(notification)
            }
        )
        rvNotifications.layoutManager = LinearLayoutManager(this)
        rvNotifications.adapter = notificationAdapter
    }

    private fun loadNotifications() {
        lifecycleScope.launch {
            val db = AppDatabase.getInstance(this@NotificationsActivity)
            val notificationDao = db.notificationDao()

            // Observar cambios en tiempo real
            notificationDao.getAllFlow().collectLatest { notifications ->
                if (notifications.isEmpty()) {
                    showEmptyState()
                } else {
                    showNotifications()
                    notificationAdapter.submitList(notifications)
                }
            }
        }
    }

    private fun markAsRead(notification: NotificationEntity) {
        if (!notification.isRead) {
            lifecycleScope.launch {
                val db = AppDatabase.getInstance(this@NotificationsActivity)
                val notificationDao = db.notificationDao()
                notificationDao.markAsRead(notification.id)
            }
        }
    }

    private fun showEmptyState() {
        rvNotifications.visibility = View.GONE
        emptyState.visibility = View.VISIBLE
    }

    private fun showNotifications() {
        rvNotifications.visibility = View.VISIBLE
        emptyState.visibility = View.GONE
    }

    // Adapter para las notificaciones
    inner class NotificationAdapter(
        private val onNotificationClick: (NotificationEntity) -> Unit
    ) : RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder>() {

        private var notifications = listOf<NotificationEntity>()

        fun submitList(newNotifications: List<NotificationEntity>) {
            notifications = newNotifications
            notifyDataSetChanged()
        }

        inner class NotificationViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val icon: TextView = view.findViewById(R.id.tv_notification_icon)
            val title: TextView = view.findViewById(R.id.tv_notification_title)
            val message: TextView = view.findViewById(R.id.tv_notification_message)
            val time: TextView = view.findViewById(R.id.tv_notification_time)
            val unreadIndicator: View = view.findViewById(R.id.unread_indicator)
            val container: View = view.findViewById(R.id.notification_container)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_notification, parent, false)
            return NotificationViewHolder(view)
        }

        override fun onBindViewHolder(holder: NotificationViewHolder, position: Int) {
            val notification = notifications[position]

            holder.icon.text = notification.icon
            holder.title.text = notification.title
            holder.message.text = notification.message
            holder.time.text = formatTimestamp(notification.timestamp)

            // Mostrar u ocultar indicador de no leída
            holder.unreadIndicator.visibility = if (notification.isRead) View.GONE else View.VISIBLE

            // Marcar como leída al hacer clic
            holder.container.setOnClickListener {
                onNotificationClick(notification)
            }

            // Aplicar estilo diferente si ya fue leída
            if (notification.isRead) {
                holder.container.alpha = 0.6f
            } else {
                holder.container.alpha = 1.0f
            }
        }

        override fun getItemCount() = notifications.size

        private fun formatTimestamp(timestamp: Long): String {
            val now = System.currentTimeMillis()
            val diff = now - timestamp

            return when {
                diff < TimeUnit.MINUTES.toMillis(1) -> "Ahora"
                diff < TimeUnit.HOURS.toMillis(1) -> {
                    val minutes = TimeUnit.MILLISECONDS.toMinutes(diff)
                    "Hace $minutes min"
                }
                diff < TimeUnit.DAYS.toMillis(1) -> {
                    val hours = TimeUnit.MILLISECONDS.toHours(diff)
                    "Hace $hours h"
                }
                diff < TimeUnit.DAYS.toMillis(7) -> {
                    val days = TimeUnit.MILLISECONDS.toDays(diff)
                    "Hace $days d"
                }
                else -> {
                    val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                    sdf.format(Date(timestamp))
                }
            }
        }
    }
}