package com.universidad.streamzone.ui.auth

import android.content.Intent
import android.content.SharedPreferences
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.util.Patterns
import android.widget.CheckBox
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.universidad.streamzone.ui.home.HomeNativeActivity
import com.universidad.streamzone.R
import com.universidad.streamzone.data.firebase.repository.UserRepository
import com.universidad.streamzone.data.firebase.repository.RoleRepository
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private lateinit var tilEmail: TextInputLayout
    private lateinit var tilPassword: TextInputLayout
    private lateinit var etEmail: TextInputEditText
    private lateinit var etPassword: TextInputEditText
    private lateinit var btnLogin: MaterialButton
    private lateinit var btnRegister: MaterialButton
    private lateinit var btnTogglePassword: MaterialButton
    private lateinit var checkKeepLoggedIn: CheckBox

    private var isPasswordVisible = false

    private lateinit var sharedPrefs: SharedPreferences

    // Firebase Repositories
    private val userRepository = UserRepository()
    private val roleRepository = RoleRepository()

    companion object {
        private const val TAG = "LoginActivity"
        private const val SESSION_DURATION_MS = 4 * 60 * 60 * 1000L // 4 horas
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        sharedPrefs = getSharedPreferences("StreamZoneData", MODE_PRIVATE)

        initViews()
        setupClickListeners()
    }

    override fun onStart() {
        super.onStart()
        restoreEmail()

        // Si viene desde registro, usar ese email
        val registeredEmail = intent.getStringExtra("registered_email")
        if (!registeredEmail.isNullOrEmpty()) {
            etEmail.setText(registeredEmail)
            etPassword.requestFocus()
        }

        // Verificar sesión activa
        if (checkActiveSession()) {
            val userEmail = sharedPrefs.getString("logged_in_user_email", null)
            val userName = sharedPrefs.getString("logged_in_user_name", null)
            if (userEmail != null && userName != null) {
                Log.d(TAG, "Sesión activa encontrada")
                navegarAHome(userName)
            }
        }
    }

    override fun onPause() {
        super.onPause()
        saveEmail()
    }

    private fun initViews() {
        tilEmail = findViewById(R.id.til_email)
        tilPassword = findViewById(R.id.til_password)
        etEmail = findViewById(R.id.et_email)
        etPassword = findViewById(R.id.et_password)
        btnLogin = findViewById(R.id.btn_login)
        btnRegister = findViewById(R.id.btn_register)
        btnTogglePassword = findViewById(R.id.btn_toggle_password)
        checkKeepLoggedIn = findViewById(R.id.check_keep_logged_in)

        // Restaurar estado del checkbox
        checkKeepLoggedIn.isChecked = sharedPrefs.getBoolean("keep_logged_in_preference", false)
    }

    private fun setupClickListeners() {
        btnLogin.setOnClickListener {
            handleLogin()
        }

        btnRegister.setOnClickListener {
            handleRegister()
        }

        btnTogglePassword.setOnClickListener {
            togglePasswordVisibility()
        }
    }

    private fun handleLogin() {
        val email = etEmail.text.toString().trim()
        val password = etPassword.text.toString().trim()

        Log.d(TAG, "=== INICIO LOGIN ===")
        Log.d(TAG, "Email: $email")

        tilEmail.error = null
        tilPassword.error = null

        if (!validateEmail(email)) return
        if (!validatePassword(password)) return

        if (!isNetworkAvailable()) {
            tilEmail.error = "Se requiere conexión a internet para iniciar sesión"
            return
        }

        // Deshabilitar botón mientras se procesa
        btnLogin.isEnabled = false
        btnLogin.text = "Iniciando sesión..."

        lifecycleScope.launch {
            try {
                // Buscar usuario en Firebase
                val user = userRepository.findByEmail(email)

                if (user == null) {
                    runOnUiThread {
                        tilEmail.error = "Esta cuenta no existe. Por favor regístrate primero."
                        etEmail.requestFocus()
                        btnLogin.isEnabled = true
                        btnLogin.text = "Iniciar sesión"
                    }
                    return@launch
                }

                Log.d(TAG, "Usuario encontrado en Firebase: ${user.fullname}")

                // Verificar contraseña
                if (user.password != password) {
                    runOnUiThread {
                        tilPassword.error = "Contraseña incorrecta"
                        etPassword.requestFocus()
                        btnLogin.isEnabled = true
                        btnLogin.text = "Iniciar sesión"
                    }
                    return@launch
                }

                // Login exitoso
                Log.d(TAG, "✅ Login exitoso para: ${user.email}")

                runOnUiThread {
                    loginExitoso(user.fullname, email)
                    btnLogin.isEnabled = true
                    btnLogin.text = "Iniciar sesión"
                }

            } catch (e: Exception) {
                Log.e(TAG, "❌ Error al iniciar sesión", e)
                runOnUiThread {
                    Toast.makeText(
                        this@LoginActivity,
                        "Error al iniciar sesión: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                    btnLogin.isEnabled = true
                    btnLogin.text = "Iniciar sesión"
                }
            }
        }
    }

    private fun loginExitoso(nombreUsuario: String, email: String) {
        // Guardar sesión
        val keepLoggedIn = checkKeepLoggedIn.isChecked
        sharedPrefs.edit().apply {
            putString("logged_in_user_email", email)
            putString("logged_in_user_name", nombreUsuario)
            putBoolean("keep_logged_in_preference", keepLoggedIn)

            if (keepLoggedIn) {
                putLong("session_start_time", System.currentTimeMillis())
            } else {
                remove("session_start_time")
            }
            apply()
        }

        Toast.makeText(
            this@LoginActivity,
            "Bienvenido, $nombreUsuario",
            Toast.LENGTH_SHORT
        ).show()

        navegarAHome(nombreUsuario)
    }

    private fun navegarAHome(nombreUsuario: String) {
        val intent = Intent(this@LoginActivity, HomeNativeActivity::class.java)
        intent.putExtra("USER_FULLNAME", nombreUsuario)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
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

    private fun validateEmail(email: String): Boolean {
        return when {
            email.isEmpty() -> {
                tilEmail.error = "El correo electrónico es obligatorio"
                etEmail.requestFocus()
                false
            }
            email.length > 30 -> {
                tilEmail.error = "El correo electrónico es demasiado largo"
                etEmail.requestFocus()
                false
            }
            !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                tilEmail.error = "Formato de correo inválido"
                etEmail.requestFocus()
                false
            }
            else -> true
        }
    }

    private fun validatePassword(password: String): Boolean {
        return when {
            password.isEmpty() -> {
                tilPassword.error = "Ingresa tu contraseña"
                etPassword.requestFocus()
                false
            }
            password.length < 6 -> {
                tilPassword.error = "La contraseña debe tener al menos 6 caracteres"
                etPassword.requestFocus()
                false
            }
            password.length > 20 -> {
                tilPassword.error = "La contraseña debe tener como máximo 20 caracteres"
                etPassword.requestFocus()
                false
            }
            else -> true
        }
    }

    private fun handleRegister() {
        val intent = Intent(this, RegisterActivity::class.java)
        startActivity(intent)
    }

    private fun togglePasswordVisibility() {
        val currentTypeface = etPassword.typeface
        val selection = etPassword.selectionEnd

        if (isPasswordVisible) {
            etPassword.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            btnTogglePassword.setIconResource(R.drawable.ic_visibility)
            isPasswordVisible = false
        } else {
            etPassword.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            btnTogglePassword.setIconResource(R.drawable.ic_visibility_off)
            isPasswordVisible = true
        }

        etPassword.typeface = currentTypeface
        etPassword.setSelection(selection)
    }

    private fun saveEmail() {
        val email = etEmail.text.toString().trim()
        if (email.isNotEmpty()) {
            sharedPrefs.edit().putString("last_email", email).apply()
        }
    }

    private fun restoreEmail() {
        val savedEmail = sharedPrefs.getString("last_email", "")
        if (!savedEmail.isNullOrEmpty()) {
            etEmail.setText(savedEmail)
        }
    }

    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false

        return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
    }
}
