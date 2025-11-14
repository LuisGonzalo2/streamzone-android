package com.universidad.streamzone.ui.notifications

import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.ScrollView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.universidad.streamzone.R
import com.universidad.streamzone.ui.components.NavbarManager

class NotificationsActivity : AppCompatActivity() {

    private lateinit var navbarManager: NavbarManager
    private lateinit var rvNotifications: RecyclerView
    private lateinit var emptyState: LinearLayout

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

        initViews()
        loadNotifications()
    }

    private fun initViews() {
        rvNotifications = findViewById(R.id.rv_notifications)
        emptyState = findViewById(R.id.empty_state_notifications)
    }

    private fun loadNotifications() {
        // Por ahora mostrar estado vacío
        // En el futuro aquí cargaremos notificaciones reales
        showEmptyState()
    }

    private fun showEmptyState() {
        rvNotifications.visibility = View.GONE
        emptyState.visibility = View.VISIBLE
    }
}