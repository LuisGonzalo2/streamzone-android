package com.universidad.streamzone.ui.home

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.universidad.streamzone.R
import com.universidad.streamzone.data.local.database.AppDatabase
import com.universidad.streamzone.ui.auth.LoginActivity
import kotlinx.coroutines.launch

class UserProfileActivity : AppCompatActivity() {

    private lateinit var sharedPrefs: SharedPreferences
    private lateinit var tvUserName: TextView
    private lateinit var tvUserEmail: TextView
    private lateinit var btnEditProfile: Button
    private lateinit var btnLogout: Button

    // ✅ NUEVAS VARIABLES DECLARADAS
    private lateinit var tvFullName: TextView
    private lateinit var tvPhone: TextView
    private lateinit var tvPersonalEmail: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_profile)

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

        // ✅ NUEVAS REFERENCIAS para la sección de información personal
        tvFullName = findViewById(R.id.tv_full_name)
        tvPhone = findViewById(R.id.tv_phone)
        tvPersonalEmail = findViewById(R.id.tv_personal_email)
    }

    private fun loadUserData() {
        // Cargar datos básicos desde SharedPreferences
        val userName = sharedPrefs.getString("logged_in_user_name", "Usuario")
        val userEmail = sharedPrefs.getString("logged_in_user_email", "usuario@email.com")

        // ✅ ACTUALIZAR TODAS LAS SECCIONES
        tvUserName.text = userName ?: "Usuario"
        tvUserEmail.text = userEmail ?: "usuario@email.com"
        tvFullName.text = userName ?: "No disponible"
        tvPersonalEmail.text = userEmail ?: "No disponible"

        // Cargar datos completos desde la base de datos (incluyendo teléfono)
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
                    // Actualizar la UI con los datos completos del usuario
                    runOnUiThread {
                        tvUserName.text = user.fullname
                        tvUserEmail.text = user.email
                        tvFullName.text = user.fullname
                        tvPersonalEmail.text = user.email
                        tvPhone.text = user.phone ?: "No registrado"

                        Log.d("UserProfile", "Datos cargados: ${user.fullname} - ${user.email} - ${user.phone}")
                    }
                }
            } catch (e: Exception) {
                Log.e("UserProfile", "Error al cargar datos del usuario", e)
            }
        }
    }

    private fun setupClickListeners() {
        btnEditProfile.setOnClickListener {
            Toast.makeText(this, "Próximamente: Editar Perfil", Toast.LENGTH_SHORT).show()
        }

        btnLogout.setOnClickListener {
            logout()
        }


        // Botones "Mostrar" contraseña (simulados)
        setupPasswordButtons()
    }

    private fun setupPasswordButtons() {
        // Simular funcionalidad de mostrar/ocultar contraseña
        val btnShowPassword1 = findViewById<Button>(R.id.btn_show_password_1)
        val btnShowPassword2 = findViewById<Button>(R.id.btn_show_password_2)

        btnShowPassword1?.setOnClickListener {
            Toast.makeText(this, "Contraseña: crunchy123", Toast.LENGTH_SHORT).show()
        }

        btnShowPassword2?.setOnClickListener {
            Toast.makeText(this, "Contraseña: amazon123", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupBottomNavbar() {
        // Botón Home - Regresar al home
        val btnHome = findViewById<View>(R.id.btn_home)
        btnHome.setOnClickListener {
            val intent = Intent(this, HomeNativeActivity::class.java)
            startActivity(intent)
            finish()
        }

        // Botón Regalos
        val btnGift = findViewById<View>(R.id.btn_gift)
        btnGift.setOnClickListener {
            Toast.makeText(this, "Próximamente: Sección de Regalos", Toast.LENGTH_SHORT).show()
        }

        // Botón Perfil - Ya estamos en perfil
        val btnProfile = findViewById<View>(R.id.btn_profile)
        btnProfile.setOnClickListener {
            Toast.makeText(this, "Ya estás en tu perfil", Toast.LENGTH_SHORT).show()
        }

        // Botón Cerrar Sesión
        val btnLogoutNav = findViewById<View>(R.id.btn_logout_nav)
        btnLogoutNav.setOnClickListener {
            logout()
        }
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