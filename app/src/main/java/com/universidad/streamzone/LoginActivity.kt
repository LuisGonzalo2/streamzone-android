package com.universidad.streamzone

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.Bundle
import android.text.InputType
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

class LoginActivity : AppCompatActivity() {

    private lateinit var tilEmail: TextInputLayout
    private lateinit var tilPassword: TextInputLayout
    private lateinit var etEmail: TextInputEditText
    private lateinit var etPassword: TextInputEditText
    private lateinit var btnLogin: MaterialButton
    private lateinit var btnRegister: MaterialButton
    private lateinit var btnTogglePassword: MaterialButton

    private var isPasswordVisible = false

    private lateinit var sharedPrefs: SharedPreferences
    private var networkCallback: ConnectivityManager.NetworkCallback? = null
    private var loginAttempts = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        sharedPrefs = getSharedPreferences("StreamZoneData", MODE_PRIVATE)

        initViews()
        setupClickListeners()
    }

    override fun onStart() {
        super.onStart()

        if (!isNetworkAvailable()) {
            showNoInternetDialog()
        }

        restoreEmail()
    }

    override fun onResume() {
        super.onResume()

        registerNetworkCallback()
        checkLoginAttempts()
    }

    override fun onPause() {
        super.onPause()

        saveEmail()
        unregisterNetworkCallback()
    }

    override fun onDestroy() {
        super.onDestroy()

        unregisterNetworkCallback()
    }

    private fun initViews() {
        tilEmail = findViewById(R.id.til_email)
        tilPassword = findViewById(R.id.til_password)
        etEmail = findViewById(R.id.et_email)
        etPassword = findViewById(R.id.et_password)
        btnLogin = findViewById(R.id.btn_login)
        btnRegister = findViewById(R.id.btn_register)
        btnTogglePassword = findViewById(R.id.btn_toggle_password)
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

        findViewById<TextView>(R.id.tv_forgot_password).setOnClickListener {
            handleForgotPassword()
        }
    }

    private fun handleLogin() {
        val email = etEmail.text.toString().trim()
        val password = etPassword.text.toString().trim()

        tilEmail.error = null
        tilPassword.error = null

        if (!validateEmail(email)) return
        if (!validatePassword(password)) return

        loginAttempts = sharedPrefs.getInt("login_attempts", 0) + 1
        sharedPrefs.edit().putInt("login_attempts", loginAttempts).apply()

        // VERIFICAR SI EL USUARIO EXISTE EN LA BASE DE DATOS
        checkUserInDatabase(email, password)
    }

    private fun checkUserInDatabase(email: String, password: String) {
        try {
            val db = AppDatabase.getInstance(this)

            // Buscar usuario por email
            Thread {
                val users = db.userDao().getAll()
                val user = users.find { it.email.equals(email, ignoreCase = true) }

                runOnUiThread {
                    if (user != null) {
                        // USUARIO ENCONTRADO - INICIAR SESIÓN EXITOSA
                        loginSuccess(user)
                    } else {
                        // USUARIO NO ENCONTRADO
                        loginFailed()
                    }
                }
            }.start()

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(
                this,
                "Error al verificar usuario: ${e.message}",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun loginSuccess(user: UserEntity) {
        // Guardar datos del usuario en SharedPreferences
        sharedPrefs.edit().apply {
            putString("user_name", user.fullName)
            putString("user_email", user.email)
            putString("user_phone", user.phone)
            putInt("login_attempts", 0) // Resetear intentos
            apply()
        }

        Toast.makeText(
            this,
            "✅ ¡Bienvenido ${user.fullName}!",
            Toast.LENGTH_SHORT
        ).show()

        // Navegar al panel de usuario
        val intent = Intent(this, UserPanelActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
        finish()
    }

    private fun loginFailed() {
        Toast.makeText(
            this,
            "❌ Esta cuenta no existe. Por favor regístrate primero.",
            Toast.LENGTH_LONG
        ).show()

        // Limpiar campos
        etPassword.text?.clear()
        etPassword.requestFocus()
    }

    private fun validateEmail(email: String): Boolean {
        return when {
            email.isEmpty() -> {
                tilEmail.error = "Ingresa tu correo electrónico"
                etEmail.requestFocus()
                false
            }
            !email.contains("@") -> {
                tilEmail.error = "El correo debe contener @"
                etEmail.requestFocus()
                false
            }
            !email.contains(".") -> {
                tilEmail.error = "Ingresa un correo válido"
                etEmail.requestFocus()
                false
            }
            !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
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
            else -> true
        }
    }

    private fun handleRegister() {
        val intent = Intent(this, RegisterActivity::class.java)
        startActivity(intent)
    }

    private fun handleForgotPassword() {
        val email = etEmail.text.toString().trim()

        if (email.isEmpty()) {
            Toast.makeText(
                this,
                "Ingresa tu correo para recuperar la contraseña",
                Toast.LENGTH_SHORT
            ).show()
            etEmail.requestFocus()
            return
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.error = "Ingresa un correo válido primero"
            etEmail.requestFocus()
            return
        }

        Toast.makeText(
            this,
            "📧 Se envió un enlace de recuperación a $email",
            Toast.LENGTH_LONG
        ).show()
    }

    private fun togglePasswordVisibility() {
        if (isPasswordVisible) {
            etPassword.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            btnTogglePassword.setIconResource(R.drawable.ic_eye)
            isPasswordVisible = false
        } else {
            etPassword.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            btnTogglePassword.setIconResource(R.drawable.ic_eye_off)
            isPasswordVisible = true
        }
        etPassword.setSelection(etPassword.text?.length ?: 0)
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

    private fun checkLoginAttempts() {
        val attempts = sharedPrefs.getInt("login_attempts", 0)
        if (attempts >= 3) {
            AlertDialog.Builder(this)
                .setTitle("Múltiples intentos fallidos")
                .setMessage("Has intentado iniciar sesión $attempts veces sin éxito.\n\n¿Necesitas ayuda o prefieres crear una cuenta?")
                .setPositiveButton("Crear Cuenta") { _, _ ->
                    handleRegister()
                }
                .setNegativeButton("Reintentar") { dialog, _ ->
                    sharedPrefs.edit().putInt("login_attempts", 0).apply()
                    dialog.dismiss()
                }
                .show()
        }
    }

    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false

        return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
    }

    private fun showNoInternetDialog() {
        AlertDialog.Builder(this)
            .setTitle("Sin Conexión a Internet")
            .setMessage("Necesitas conexión a internet para iniciar sesión.")
            .setPositiveButton("Continuar") { dialog, _ ->
                dialog.dismiss()
            }
            .setNegativeButton("Salir") { _, _ ->
                finish()
            }
            .setCancelable(false)
            .show()
    }

    private fun registerNetworkCallback() {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                runOnUiThread {
                    Toast.makeText(this@LoginActivity, "Conexión restaurada", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onLost(network: Network) {
                runOnUiThread {
                    Toast.makeText(this@LoginActivity, "Conexión perdida", Toast.LENGTH_SHORT).show()
                }
            }
        }

        try {
            connectivityManager.registerDefaultNetworkCallback(networkCallback!!)
        } catch (e: Exception) {
            // Silencioso
        }
    }

    private fun unregisterNetworkCallback() {
        networkCallback?.let {
            try {
                val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
                connectivityManager.unregisterNetworkCallback(it)
            } catch (e: Exception) {
                // Silencioso
            }
        }
        networkCallback = null
    }
}