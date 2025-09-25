package com.universidad.carpoolapp

import android.os.Bundle
import android.widget.Toast
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

        // me olvide la contra
        findViewById<android.widget.TextView>(R.id.tv_forgot_password).setOnClickListener {
            handleForgotPassword()
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

    private fun isValidEmail(email: String): Boolean {
        return email.contains("@") && email.contains(".")
    }
}