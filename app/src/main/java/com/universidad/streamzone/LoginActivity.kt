package com.universidad.streamzone

import android.os.Bundle
import android.text.InputType
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

class LoginActivity : AppCompatActivity() {

    // Declaración de vistas
    private lateinit var tilEmail: TextInputLayout
    private lateinit var tilPassword: TextInputLayout
    private lateinit var etEmail: TextInputEditText
    private lateinit var etPassword: TextInputEditText
    private lateinit var btnLogin: MaterialButton
    private lateinit var btnRegister: MaterialButton
    private lateinit var btnTogglePassword: MaterialButton

    private var isPasswordVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // Inicializar vistas
        initViews()

        // Configurar listeners
        setupClickListeners()
    }

    private fun initViews() {
        // Inputs
        tilEmail = findViewById(R.id.til_email)
        tilPassword = findViewById(R.id.til_password)
        etEmail = findViewById(R.id.et_email)
        etPassword = findViewById(R.id.et_password)

        // Botones
        btnLogin = findViewById(R.id.btn_login)
        btnRegister = findViewById(R.id.btn_register)
        btnTogglePassword = findViewById(R.id.btn_toggle_password)
    }

    private fun setupClickListeners() {

        // Botón Login - Mostrar mensaje de que no existe la cuenta
        btnLogin.setOnClickListener {
            handleLogin()
        }

        // Botón Registro - Mostrar mensaje de que pronto estará disponible
        btnRegister.setOnClickListener {
            handleRegister()
        }

        // Botón toggle password (ojo)
        btnTogglePassword.setOnClickListener {
            togglePasswordVisibility()
        }

        // ¿Olvidaste contraseña?
        findViewById<android.widget.TextView>(R.id.tv_forgot_password).setOnClickListener {
            handleForgotPassword()
        }

        // Volver al inicio
        findViewById<android.widget.TextView>(R.id.tv_back_home).setOnClickListener {
            handleBackHome()
        }
    }

    private fun handleLogin() {
        val email = etEmail.text.toString().trim()
        val password = etPassword.text.toString().trim()

        // Limpiar errores anteriores
        tilEmail.error = null
        tilPassword.error = null

        // Validaciones básicas
        when {
            email.isEmpty() -> {
                tilEmail.error = "Ingresa tu correo electrónico"
                etEmail.requestFocus()
                return
            }

            password.isEmpty() -> {
                tilPassword.error = "Ingresa tu contraseña"
                etPassword.requestFocus()
                return
            }

            !isValidEmail(email) -> {
                tilEmail.error = "Ingresa un correo válido"
                etEmail.requestFocus()
                return
            }
        }

        // Mostrar mensaje de que no existe la cuenta
        Toast.makeText(
            this,
            "❌ Esta cuenta no existe. Por favor regístrate primero.",
            Toast.LENGTH_LONG
        ).show()
    }

    private fun handleRegister() {
        Toast.makeText(
            this,
            "📝 La pantalla de registro estará disponible pronto.",
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun handleForgotPassword() {
        Toast.makeText(
            this,
            "🔐 Función de recuperar contraseña próximamente.",
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun handleBackHome() {
        Toast.makeText(
            this,
            "🏠 Volviendo al inicio...",
            Toast.LENGTH_SHORT
        ).show()
        // Aquí podrías hacer finish() o navegar a otra actividad
    }

    private fun togglePasswordVisibility() {
        if (isPasswordVisible) {
            // Ocultar contraseña
            etPassword.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            btnTogglePassword.setIconResource(R.drawable.ic_eye)
            isPasswordVisible = false
        } else {
            // Mostrar contraseña
            etPassword.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            btnTogglePassword.setIconResource(R.drawable.ic_eye_off)
            isPasswordVisible = true
        }

        // Mantener cursor al final
        etPassword.setSelection(etPassword.text?.length ?: 0)
    }

    private fun isValidEmail(email: String): Boolean {
        return email.contains("@") && email.contains(".")
    }
}