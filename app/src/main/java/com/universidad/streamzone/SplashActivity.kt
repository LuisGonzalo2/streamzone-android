package com.universidad.streamzone

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.animation.AnimationUtils
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class SplashActivity : AppCompatActivity() {

    private lateinit var logoContainer: LinearLayout
    private lateinit var appName: TextView
    private lateinit var tagline: TextView
    private val splashDuration = 2500L

    private lateinit var sharedPrefs: SharedPreferences

    companion object {
        private const val SESSION_DURATION_MS = 4 * 60 * 60 * 1000L // 4 horas
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        sharedPrefs = getSharedPreferences("StreamZoneData", MODE_PRIVATE)

        initViews()
        startAnimations()
        navigateAfterDelay()
    }

    private fun initViews() {
        logoContainer = findViewById(R.id.logo_container)
        appName = findViewById(R.id.app_name)
        tagline = findViewById(R.id.tagline)
    }

    private fun startAnimations() {
        val logoAnimation = AnimationUtils.loadAnimation(this, R.anim.splash_logo_animation)
        logoContainer.startAnimation(logoAnimation)

        val textAnimation = AnimationUtils.loadAnimation(this, R.anim.splash_text_animation)
        appName.startAnimation(textAnimation)

        Handler(Looper.getMainLooper()).postDelayed({
            val taglineAnimation = AnimationUtils.loadAnimation(this, R.anim.splash_fade_in)
            tagline.startAnimation(taglineAnimation)
        }, 500)
    }

    private fun navigateAfterDelay() {
        Handler(Looper.getMainLooper()).postDelayed({
            // Verificar si hay sesión activa
            if (checkActiveSession()) {
                navigateToHome()
            } else {
                navigateToLogin()
            }
        }, splashDuration)
    }

    private fun navigateToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        finish()
    }

    private fun navigateToHome() {
        val userName = sharedPrefs.getString("logged_in_user_name", "")
        val intent = Intent(this, HomeNativeActivity::class.java)
        intent.putExtra("USER_FULLNAME", userName)
        startActivity(intent)
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        finish()
    }

    private fun checkActiveSession(): Boolean {
        val keepLoggedIn = sharedPrefs.getBoolean("keep_logged_in_preference", false)

        if (!keepLoggedIn) {
            return false
        }

        val sessionStartTime = sharedPrefs.getLong("session_start_time", 0)
        val currentTime = System.currentTimeMillis()
        val elapsedTime = currentTime - sessionStartTime

        // Si la sesión tiene menos de 4 horas
        if (sessionStartTime > 0 && elapsedTime < SESSION_DURATION_MS) {
            val userEmail = sharedPrefs.getString("logged_in_user_email", null)
            return !userEmail.isNullOrEmpty()
        } else {
            // Sesión expirada, limpiar
            clearSession()
            return false
        }
    }

    private fun clearSession() {
        sharedPrefs.edit().apply {
            remove("logged_in_user_email")
            remove("logged_in_user_name")
            remove("session_start_time")
            apply()
        }
    }
}