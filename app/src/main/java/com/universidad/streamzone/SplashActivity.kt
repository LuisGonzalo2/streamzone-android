package com.universidad.streamzone

import android.content.Intent
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        initViews()
        startAnimations()
        navigateToLogin()
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

    private fun navigateToLogin() {
        Handler(Looper.getMainLooper()).postDelayed({
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            finish()
        }, splashDuration)
    }
}