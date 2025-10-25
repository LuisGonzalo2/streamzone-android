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
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.universidad.streamzone.cloud.FirebaseService
import com.universidad.streamzone.database.AppDatabase
import com.universidad.streamzone.sync.SyncService
import kotlinx.coroutines.launch
import android.widget.CheckBox

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
    private var networkCallback: ConnectivityManager.NetworkCallback? = null

    companion object {
        private const val SESSION_DURATION_MS = 4 * 60 * 60 * 1000L // 4 horas en milisegundos
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
    }

    override fun onResume() {
        super.onResume()
        registerNetworkCallback()

        // Intentar sincronizar datos pendientes si hay internet (silenciosamente)
        if (isNetworkAvailable()) {
            SyncService.sincronizarUsuariosPendientes(this)
        }
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

        android.util.Log.d("LoginActivity", "=== INICIO LOGIN ===")
        android.util.Log.d("LoginActivity", "Email: $email")
        android.util.Log.d("LoginActivity", "Password length: ${password.length}")

        tilEmail.error = null
        tilPassword.error = null

        if (!validateEmail(email)) return
        if (!validatePassword(password)) return

        val dao = AppDatabase.getInstance(this).usuarioDao()

        lifecycleScope.launch {
            // Primero buscar en Room (local)
            val usuarioLocal = dao.buscarPorEmail(email)

            android.util.Log.d("LoginActivity", "Usuario en Room: $usuarioLocal")

            if (usuarioLocal != null) {
                // Usuario encontrado localmente
                android.util.Log.d("LoginActivity", "Password en Room: ${usuarioLocal.password}")
                android.util.Log.d("LoginActivity", "Password ingresada: $password")
                android.util.Log.d("LoginActivity", "¿Coinciden?: ${usuarioLocal.password == password}")

                if (usuarioLocal.password != password) {
                    runOnUiThread {
                        tilPassword.error = "Contraseña incorrecta"
                        etPassword.requestFocus()
                    }
                    return@launch
                }

                // Login exitoso
                runOnUiThread {
                    loginExitoso(usuarioLocal.fullname, email)
                }
            } else {
                // No está en Room, buscar en Firebase si hay internet
                android.util.Log.d("LoginActivity", "Usuario NO encontrado en Room")

                if (isNetworkAvailable()) {
                    android.util.Log.d("LoginActivity", "Buscando en Firebase...")

                    FirebaseService.verificarUsuarioPorEmail(email) { usuarioFirebase ->
                        android.util.Log.d("LoginActivity", "Usuario en Firebase: $usuarioFirebase")

                        if (usuarioFirebase == null) {
                            runOnUiThread {
                                tilEmail.error = "Esta cuenta no existe. Por favor regístrate primero."
                                etEmail.requestFocus()
                            }
                        } else {
                            android.util.Log.d("LoginActivity", "Password en Firebase: ${usuarioFirebase.password}")
                            android.util.Log.d("LoginActivity", "Password ingresada: $password")
                            android.util.Log.d("LoginActivity", "¿Coinciden?: ${usuarioFirebase.password == password}")

                            if (usuarioFirebase.password != password) {
                                runOnUiThread {
                                    tilPassword.error = "Contraseña incorrecta"
                                    etPassword.requestFocus()
                                }
                            } else {
                                // Guardar en Room para uso offline
                                lifecycleScope.launch {
                                    dao.insertar(usuarioFirebase)
                                    android.util.Log.d("LoginActivity", "Usuario guardado en Room desde Firebase")
                                    runOnUiThread {
                                        loginExitoso(usuarioFirebase.fullname, email)
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // Sin internet y no está en Room
                    android.util.Log.d("LoginActivity", "Sin internet y usuario no en Room")
                    runOnUiThread {
                        AlertDialog.Builder(this@LoginActivity)
                            .setTitle("Sin conexión")
                            .setMessage("No se pudo verificar tu cuenta. Por favor, conéctate a internet.")
                            .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
                            .show()
                    }
                }
            }
        }
    }

    private fun loginExitoso(nombreUsuario: String, email: String) {
        Toast.makeText(
            this@LoginActivity,
            "Bienvenido",
            Toast.LENGTH_SHORT
        ).show()

        sharedPrefs.edit().putString("logged_in_user_email", email).apply()
        sharedPrefs.edit().putString("logged_in_user_name", nombreUsuario).apply()

        val intent = Intent(this@LoginActivity, HomeActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
        finish()
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
            "Se envió un enlace de recuperación a $email",
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

    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false

        return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
    }

    private fun registerNetworkCallback() {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                runOnUiThread {
                    // Sincronizar silenciosamente sin mostrar mensaje
                    SyncService.sincronizarUsuariosPendientes(this@LoginActivity)
                }
            }

            override fun onLost(network: Network) {
                // Silencioso - sin mensajes
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