package com.universidad.streamzone

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.CheckBox
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

class RegisterActivity : AppCompatActivity() {

    // Vistas
    private lateinit var tilFullName: TextInputLayout
    private lateinit var tilEmail: TextInputLayout
    private lateinit var tilPassword: TextInputLayout
    private lateinit var tilConfirmPassword: TextInputLayout
    private lateinit var etFullName: TextInputEditText
    private lateinit var etEmail: TextInputEditText
    private lateinit var etPassword: TextInputEditText
    private lateinit var etConfirmPassword: TextInputEditText
    private lateinit var spinnerCountryCode: Spinner
    private lateinit var etPhone: TextInputEditText
    private lateinit var btnTogglePassword: MaterialButton
    private lateinit var btnToggleConfirmPassword: MaterialButton
    private lateinit var checkShowPassword: CheckBox
    private lateinit var btnRegister: MaterialButton
    private lateinit var tvBackToLogin: TextView

    private var isPasswordVisible = false
    private var isConfirmPasswordVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        initViews()
        setupSpinner()
        setupListeners()
    }

    override fun onStart() {
        super.onStart()
        // Limpiar campos si es necesario
        clearErrorsOnStart()
    }

    override fun onResume() {
        super.onResume()
    }

    override fun onPause() {
        super.onPause()
    }
    private fun initViews() {
        // TextInputLayouts
        tilFullName = findViewById(R.id.til_full_name)
        tilEmail = findViewById(R.id.til_email)
        tilPassword = findViewById(R.id.til_password)
        tilConfirmPassword = findViewById(R.id.til_confirm_password)

        // EditTexts
        etFullName = findViewById(R.id.et_full_name)
        etEmail = findViewById(R.id.et_email)
        etPassword = findViewById(R.id.et_password)
        etConfirmPassword = findViewById(R.id.et_confirm_password)
        etPhone = findViewById(R.id.et_phone)

        // Spinner
        spinnerCountryCode = findViewById(R.id.spinner_country_code)

        // Botones
        btnTogglePassword = findViewById(R.id.btn_toggle_password)
        btnToggleConfirmPassword = findViewById(R.id.btn_toggle_confirm_password)
        btnRegister = findViewById(R.id.btn_register)

        // CheckBox
        checkShowPassword = findViewById(R.id.check_show_password)

        // TextView
        tvBackToLogin = findViewById(R.id.tv_back_to_login)
    }

    private fun setupSpinner() {
        val countryCodes = arrayOf(
            "EC +593 🇪🇨",
            "US +1 🇺🇸",
            "CO +57 🇨🇴",
            "PE +51 🇵🇪",
            "AR +54 🇦🇷",
            "MX +52 🇲🇽",
            "VE +58 🇻🇪",
            "CL +56 🇨🇱",
            "BR +55 🇧🇷",
            "ES +34 🇪🇸",
            "BO +591 🇧🇴",
            "PY +595 🇵🇾",
            "UY +598 🇺🇾",
            "CR +506 🇨🇷",
            "PA +507 🇵🇦",
            "GT +502 🇬🇹",
            "DO +1-809 🇩🇴",
            "CU +53 🇨🇺",
            "HN +504 🇭🇳",
            "NI +505 🇳🇮",
            "SV +503 🇸🇻"
        )
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, countryCodes)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerCountryCode.adapter = adapter
    }

    private fun setupListeners() {
        // Botón de registro
        btnRegister.setOnClickListener {
            handleRegister()
        }

        // Volver al login
        tvBackToLogin.setOnClickListener {
            navigateToLogin()
        }

        // Toggle contraseña
        btnTogglePassword.setOnClickListener {
            togglePasswordVisibility(etPassword, btnTogglePassword, ::isPasswordVisible::get, ::isPasswordVisible::set)
        }

        // Toggle confirmar contraseña
        btnToggleConfirmPassword.setOnClickListener {
            togglePasswordVisibility(etConfirmPassword, btnToggleConfirmPassword, ::isConfirmPasswordVisible::get, ::isConfirmPasswordVisible::set)
        }

        // CheckBox mostrar contraseñas
        checkShowPassword.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                etPassword.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                etConfirmPassword.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                btnTogglePassword.setIconResource(R.drawable.ic_eye_off)
                btnToggleConfirmPassword.setIconResource(R.drawable.ic_eye_off)
                isPasswordVisible = true
                isConfirmPasswordVisible = true
            } else {
                etPassword.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                etConfirmPassword.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                btnTogglePassword.setIconResource(R.drawable.ic_eye)
                btnToggleConfirmPassword.setIconResource(R.drawable.ic_eye)
                isPasswordVisible = false
                isConfirmPasswordVisible = false
            }
            etPassword.setSelection(etPassword.text?.length ?: 0)
            etConfirmPassword.setSelection(etConfirmPassword.text?.length ?: 0)
        }
    }

    private fun handleRegister() {
        // Obtener valores
        val fullName = etFullName.text.toString().trim()
        val email = etEmail.text.toString().trim()
        val phone = etPhone.text.toString().trim()
        val password = etPassword.text.toString()
        val confirmPassword = etConfirmPassword.text.toString()

        // Limpiar errores
        clearAllErrors()

        // Validar nombre completo
        if (!validateFullName(fullName)) return

        // Validar email
        if (!validateEmail(email)) return

        // Validar teléfono
        if (!validatePhone(phone)) return

        // Validar contraseña
        if (!validatePassword(password)) return

        // Validar confirmación de contraseña
        if (!validateConfirmPassword(password, confirmPassword)) return

        // Si todas las validaciones pasan
        showSuccessAndNavigate(fullName, email)
    }

    private fun validateFullName(name: String): Boolean {
        return when {
            name.isEmpty() -> {
                tilFullName.error = "El nombre completo es obligatorio"
                etFullName.requestFocus()
                false
            }
            name.length < 3 -> {
                tilFullName.error = "El nombre debe tener al menos 3 caracteres"
                etFullName.requestFocus()
                false
            }
            !name.contains(" ") -> {
                tilFullName.error = "Ingresa tu nombre y apellido"
                etFullName.requestFocus()
                false
            }
            !name.matches(Regex("^[a-zA-ZáéíóúÁÉÍÓÚñÑ ]+$")) -> {
                tilFullName.error = "Solo se permiten letras y espacios"
                etFullName.requestFocus()
                false
            }
            else -> true
        }
    }

    private fun validateEmail(email: String): Boolean {
        return when {
            email.isEmpty() -> {
                tilEmail.error = "El correo electrónico es obligatorio"
                etEmail.requestFocus()
                false
            }
            !email.contains("@") -> {
                tilEmail.error = "El correo debe contener @"
                etEmail.requestFocus()
                false
            }
            !email.contains(".") -> {
                tilEmail.error = "El correo debe contener un dominio válido"
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

    private fun validatePhone(phone: String): Boolean {
        return when {
            phone.isEmpty() -> {
                Toast.makeText(this, "El número de teléfono es obligatorio", Toast.LENGTH_SHORT).show()
                etPhone.requestFocus()
                false
            }
            phone.length != 10 -> {
                Toast.makeText(this, "El teléfono debe tener exactamente 10 dígitos", Toast.LENGTH_SHORT).show()
                etPhone.requestFocus()
                false
            }
            else -> true
        }
    }

    private fun validatePassword(password: String): Boolean {
        return when {
            password.isEmpty() -> {
                tilPassword.error = "La contraseña es obligatoria"
                etPassword.requestFocus()
                false
            }
            password.length < 6 -> {
                tilPassword.error = "La contraseña debe tener al menos 6 caracteres"
                etPassword.requestFocus()
                false
            }
            !password.matches(Regex(".*[A-Z].*")) -> {
                tilPassword.error = "Debe contener al menos una mayúscula"
                etPassword.requestFocus()
                false
            }
            !password.matches(Regex(".*[a-z].*")) -> {
                tilPassword.error = "Debe contener al menos una minúscula"
                etPassword.requestFocus()
                false
            }
            !password.matches(Regex(".*[0-9].*")) -> {
                tilPassword.error = "Debe contener al menos un número"
                etPassword.requestFocus()
                false
            }
            else -> true
        }
    }

    private fun validateConfirmPassword(password: String, confirmPassword: String): Boolean {
        return when {
            confirmPassword.isEmpty() -> {
                tilConfirmPassword.error = "Confirma tu contraseña"
                etConfirmPassword.requestFocus()
                false
            }
            password != confirmPassword -> {
                tilConfirmPassword.error = "Las contraseñas no coinciden"
                etConfirmPassword.requestFocus()
                false
            }
            else -> true
        }
    }

    private fun clearAllErrors() {
        tilFullName.error = null
        tilEmail.error = null
        tilPassword.error = null
        tilConfirmPassword.error = null
    }

    private fun clearErrorsOnStart() {
        tilFullName.error = null
        tilEmail.error = null
        tilPassword.error = null
        tilConfirmPassword.error = null
    }

    private fun showSuccessAndNavigate(name: String, email: String) {
        Toast.makeText(
            this,
            "✅ ¡Cuenta creada exitosamente!\nBienvenido $name",
            Toast.LENGTH_LONG
        ).show()

        Log.d("RegisterActivity", "Usuario registrado: $name - $email")

        // Navegar al login después de 1.5 segundos
        etFullName.postDelayed({
            navigateToLogin()
        }, 1500)
    }

    private fun navigateToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
        finish()
    }

    private fun togglePasswordVisibility(
        editText: TextInputEditText,
        button: MaterialButton,
        getter: () -> Boolean,
        setter: (Boolean) -> Unit
    ) {
        if (getter()) {
            editText.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            button.setIconResource(R.drawable.ic_eye)
            setter(false)
        } else {
            editText.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            button.setIconResource(R.drawable.ic_eye_off)
            setter(true)
        }
        editText.setSelection(editText.text?.length ?: 0)
    }
}