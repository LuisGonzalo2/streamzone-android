package com.universidad.streamzone.ui.auth

import android.content.Intent
import android.content.SharedPreferences
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.util.Patterns
import android.widget.CheckBox
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.universidad.streamzone.ui.home.HomeNativeActivity
import com.universidad.streamzone.R
import com.universidad.streamzone.data.remote.FirebaseService
import com.universidad.streamzone.data.local.database.AppDatabase
import com.universidad.streamzone.utils.sync.SyncService
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

        findViewById<TextView>(R.id.tv_forgot_password).setOnClickListener {
            handleForgotPassword()
        }
    }

    private fun handleLogin() {
        val email = etEmail.text.toString().trim()
        val password = etPassword.text.toString().trim()

        Log.d("LoginActivity", "=== INICIO LOGIN ===")
        Log.d("LoginActivity", "Email: $email")
        Log.d("LoginActivity", "Password length: ${password.length}")

        tilEmail.error = null
        tilPassword.error = null

        if (!validateEmail(email)) return
        if (!validatePassword(password)) return

        val dao = AppDatabase.Companion.getInstance(this).usuarioDao()

        lifecycleScope.launch {
            // Primero buscar en Room (local)
            val usuarioLocal = dao.buscarPorEmail(email)

            Log.d("LoginActivity", "Usuario en Room: $usuarioLocal")

            if (usuarioLocal != null) {
                // Usuario encontrado localmente
                Log.d("LoginActivity", "Password en Room: ${usuarioLocal.password}")
                Log.d("LoginActivity", "Password ingresada: $password")
                Log.d("LoginActivity", "¿Coinciden?: ${usuarioLocal.password == password}")

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
                Log.d("LoginActivity", "Usuario NO encontrado en Room")

                if (isNetworkAvailable()) {
                    Log.d("LoginActivity", "Buscando en Firebase...")

                    FirebaseService.verificarUsuarioPorEmail(email) { usuarioFirebase ->
                        Log.d("LoginActivity", "Usuario en Firebase: $usuarioFirebase")

                        if (usuarioFirebase == null) {
                            runOnUiThread {
                                tilEmail.error = "Esta cuenta no existe. Por favor regístrate primero."
                                etEmail.requestFocus()
                            }
                        } else {
                            Log.d("LoginActivity", "Password en Firebase: ${usuarioFirebase.password}")
                            Log.d("LoginActivity", "Password ingresada: $password")
                            Log.d("LoginActivity", "¿Coinciden?: ${usuarioFirebase.password == password}")

                            if (usuarioFirebase.password != password) {
                                runOnUiThread {
                                    tilPassword.error = "Contraseña incorrecta"
                                    etPassword.requestFocus()
                                }
                            } else {
                                // Guardar en Room para uso offline
                                lifecycleScope.launch {
                                    dao.insertar(usuarioFirebase)
                                    Log.d("LoginActivity", "Usuario guardado en Room desde Firebase")
                                    runOnUiThread {
                                        loginExitoso(usuarioFirebase.fullname, email)
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // Sin internet y no está en Room
                    Log.d("LoginActivity", "Sin internet y usuario no en Room")
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

            !email.endsWith(".com") -> {
                tilEmail.error = "Solo se aceptan correos .com"
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
                tilPassword.error = "La contraseña debe tener como maximo 20 caracteres"
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

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
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
        val currentTypeface = etPassword.typeface
        val selection = etPassword.selectionEnd

        if (isPasswordVisible) {
            etPassword.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            btnTogglePassword.setIconResource(R.drawable.ic_visibility) // Ojo cerrado
            isPasswordVisible = false
        } else {
            etPassword.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            btnTogglePassword.setIconResource(R.drawable.ic_visibility_off) // Ojo abierto
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

    private fun registerNetworkCallback() {
        val connectivityManager = getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager

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
                val connectivityManager = getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
                connectivityManager.unregisterNetworkCallback(it)
            } catch (e: Exception) {
                // Silencioso
            }
        }
        networkCallback = null
    }
}