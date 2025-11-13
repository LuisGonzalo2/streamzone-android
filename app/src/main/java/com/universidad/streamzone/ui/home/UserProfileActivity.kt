package com.universidad.streamzone.ui.home

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.universidad.streamzone.R
import com.universidad.streamzone.data.local.database.AppDatabase
import com.universidad.streamzone.ui.auth.LoginActivity
import com.universidad.streamzone.ui.components.NavbarManager
import com.universidad.streamzone.ui.profile.EditProfileActivity
import com.universidad.streamzone.ui.profile.adapter.PurchaseCardAdapter
import kotlinx.coroutines.launch


class UserProfileActivity : AppCompatActivity() {

    private lateinit var sharedPrefs: SharedPreferences
    private lateinit var tvUserName: TextView
    private lateinit var tvUserEmail: TextView
    private lateinit var btnEditProfile: Button
    private lateinit var btnLogout: Button
    private lateinit var tvFullName: TextView
    private lateinit var tvPhone: TextView
    private lateinit var tvPersonalEmail: TextView
    private val EDIT_PROFILE_REQUEST = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_profile)

        // Configurar padding para el notch
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false)
        }

        // Aplicar padding superior al ScrollView
        val scrollView = findViewById<ScrollView>(R.id.profile_scroll_view)
        scrollView?.setOnApplyWindowInsetsListener { view, insets ->
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

        sharedPrefs = getSharedPreferences("StreamZoneData", MODE_PRIVATE)

        initViews()
        loadUserData()
        setupClickListeners()
        setupBottomNavbar()
    }

    private fun initViews() {
        tvUserName = findViewById(R.id.tv_user_name)
        tvUserEmail = findViewById(R.id.tv_user_email)
        btnEditProfile = findViewById(R.id.btn_edit_profile)
        btnLogout = findViewById(R.id.btn_logout)
        tvFullName = findViewById(R.id.tv_full_name)
        tvPhone = findViewById(R.id.tv_phone)
        tvPersonalEmail = findViewById(R.id.tv_personal_email)

    }

    private fun loadUserData() {
        val userName = sharedPrefs.getString("logged_in_user_name", "Usuario")
        val userEmail = sharedPrefs.getString("logged_in_user_email", "usuario@email.com")

        tvUserName.text = userName ?: "Usuario"
        tvUserEmail.text = userEmail ?: "usuario@email.com"
        tvFullName.text = userName ?: "No disponible"
        tvPersonalEmail.text = userEmail ?: "No disponible"

        if (!userEmail.isNullOrEmpty()) {
            loadUserDetailsFromDatabase(userEmail)
        }
    }

    private fun loadUserDetailsFromDatabase(userEmail: String) {
        lifecycleScope.launch {
            try {
                val dao = AppDatabase.getInstance(this@UserProfileActivity).usuarioDao()
                val usuario = dao.buscarPorEmail(userEmail)

                usuario?.let { user ->
                    runOnUiThread {
                        tvUserName.text = user.fullname
                        tvUserEmail.text = user.email
                        tvFullName.text = user.fullname
                        tvPersonalEmail.text = user.email
                        tvPhone.text = user.phone ?: "No registrado"
                    }
                }
            } catch (e: Exception) {
                Log.e("UserProfile", "Error al cargar datos del usuario", e)
            }
        }
    }




    private fun setupClickListeners() {
        btnEditProfile.setOnClickListener {
            val intent = Intent(this, EditProfileActivity::class.java)
            startActivityForResult(intent, EDIT_PROFILE_REQUEST)
        }


        btnLogout.setOnClickListener {
            logout()
        }
    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == EDIT_PROFILE_REQUEST && resultCode == RESULT_OK) {
            // Recargar datos del perfil
            loadUserData()
            Toast.makeText(this, "Perfil actualizado", Toast.LENGTH_SHORT).show()
        }
    }
    private lateinit var navbarManager: NavbarManager

    private fun setupBottomNavbar() {
        navbarManager = NavbarManager(this, NavbarManager.Screen.PROFILE)
    }

    private fun logout() {
        sharedPrefs.edit().apply {
            remove("logged_in_user_email")
            remove("logged_in_user_name")
            remove("session_start_time")
            apply()
        }

        Toast.makeText(this, "👋 Sesión cerrada", Toast.LENGTH_SHORT).show()

        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
        finish()
    }
}