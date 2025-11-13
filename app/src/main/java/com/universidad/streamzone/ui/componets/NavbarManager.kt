package com.universidad.streamzone.ui.components

import android.animation.ValueAnimator
import android.app.Activity
import android.content.Intent
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.universidad.streamzone.R
import com.universidad.streamzone.ui.home.HomeNativeActivity
import com.universidad.streamzone.ui.home.UserProfileActivity
import com.universidad.streamzone.ui.purchases.PurchaseHistoryActivity
import com.universidad.streamzone.ui.notifications.NotificationsActivity

class NavbarManager(private val activity: Activity, private val currentScreen: Screen) {

    enum class Screen {
        HOME, PURCHASES, NOTIFICATIONS, PROFILE
    }

    private val btnHome: View = activity.findViewById(R.id.btn_home)
    private val btnPurchases: View = activity.findViewById(R.id.btn_purchases)
    private val btnNotifications: View = activity.findViewById(R.id.btn_notifications)
    private val btnProfile: View = activity.findViewById(R.id.btn_profile)

    private val iconHome: ImageView = activity.findViewById(R.id.icon_home)
    private val iconPurchases: ImageView = activity.findViewById(R.id.icon_purchases)
    private val iconNotifications: ImageView = activity.findViewById(R.id.icon_notifications)
    private val iconProfile: ImageView = activity.findViewById(R.id.icon_profile)

    private val labelHome: TextView = activity.findViewById(R.id.label_home)
    private val labelPurchases: TextView = activity.findViewById(R.id.label_purchases)
    private val labelNotifications: TextView = activity.findViewById(R.id.label_notifications)
    private val labelProfile: TextView = activity.findViewById(R.id.label_profile)

    private val badgeNotifications: TextView = activity.findViewById(R.id.badge_notifications)

    private val activeColor = ContextCompat.getColor(activity, R.color.navbar_active)
    private val inactiveColor = ContextCompat.getColor(activity, R.color.navbar_inactive)

    init {
        setupClickListeners()
        setActiveScreen(currentScreen)
    }

    private fun setupClickListeners() {
        btnHome.setOnClickListener {
            if (currentScreen != Screen.HOME) {
                animateClick(btnHome)
                navigateTo(HomeNativeActivity::class.java)
            }
        }

        btnPurchases.setOnClickListener {
            if (currentScreen != Screen.PURCHASES) {
                animateClick(btnPurchases)
                navigateTo(PurchaseHistoryActivity::class.java)
            }
        }

        btnNotifications.setOnClickListener {
            if (currentScreen != Screen.NOTIFICATIONS) {
                animateClick(btnNotifications)
                navigateTo(NotificationsActivity::class.java)
            }
        }

        btnProfile.setOnClickListener {
            if (currentScreen != Screen.PROFILE) {
                animateClick(btnProfile)
                navigateTo(UserProfileActivity::class.java)
            }
        }
    }

    private fun setActiveScreen(screen: Screen) {
        // Reset todos
        resetAllButtons()

        // Activar el correspondiente
        when (screen) {
            Screen.HOME -> activateButton(iconHome, labelHome, R.drawable.ic_home_filled)
            Screen.PURCHASES -> activateButton(iconPurchases, labelPurchases, R.drawable.ic_shopping_filled)
            Screen.NOTIFICATIONS -> activateButton(iconNotifications, labelNotifications, R.drawable.ic_notifications_filled)
            Screen.PROFILE -> activateButton(iconProfile, labelProfile, R.drawable.ic_profile_filled)
        }
    }

    private fun resetAllButtons() {
        deactivateButton(iconHome, labelHome, R.drawable.ic_home_outline)
        deactivateButton(iconPurchases, labelPurchases, R.drawable.ic_shopping_outline)
        deactivateButton(iconNotifications, labelNotifications, R.drawable.ic_notifications_outline)
        deactivateButton(iconProfile, labelProfile, R.drawable.ic_profile_outline)
    }

    private fun activateButton(icon: ImageView, label: TextView, iconRes: Int) {
        icon.setImageResource(iconRes)
        icon.setColorFilter(activeColor)
        label.setTextColor(activeColor)

        // Animación de scale suave (estilo Spotify)
        icon.animate()
            .scaleX(1.1f)
            .scaleY(1.1f)
            .setDuration(200)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .start()
    }

    private fun deactivateButton(icon: ImageView, label: TextView, iconRes: Int) {
        icon.setImageResource(iconRes)
        icon.setColorFilter(inactiveColor)
        label.setTextColor(inactiveColor)

        icon.scaleX = 1f
        icon.scaleY = 1f
    }

    private fun animateClick(view: View) {
        // Animación de bounce al hacer click (estilo Spotify)
        view.animate()
            .scaleX(0.95f)
            .scaleY(0.95f)
            .setDuration(100)
            .withEndAction {
                view.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(100)
                    .start()
            }
            .start()
    }

    private fun navigateTo(activityClass: Class<*>) {
        val intent = Intent(activity, activityClass)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        activity.startActivity(intent)
        activity.finish()
    }

    // Función para actualizar el badge de notificaciones
    fun updateNotificationBadge(count: Int) {
        if (count > 0) {
            badgeNotifications.text = if (count > 9) "9+" else count.toString()
            badgeNotifications.visibility = View.VISIBLE

            // Animación de entrada del badge
            badgeNotifications.scaleX = 0f
            badgeNotifications.scaleY = 0f
            badgeNotifications.animate()
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(300)
                .setInterpolator(AccelerateDecelerateInterpolator())
                .start()
        } else {
            badgeNotifications.visibility = View.GONE
        }
    }
}