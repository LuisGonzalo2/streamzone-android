package com.universidad.streamzone

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.widget.ArrayAdapter
import android.widget.CheckBox
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.universidad.streamzone.model.UsuarioEntity
import androidx.lifecycle.lifecycleScope
import com.universidad.streamzone.cloud.FirebaseService
import com.universidad.streamzone.database.AppDatabase
import kotlinx.coroutines.launch



class RegisterActivity : AppCompatActivity() {

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

    private lateinit var sharedPrefs: SharedPreferences
    private var networkCallback: ConnectivityManager.NetworkCallback? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        sharedPrefs = getSharedPreferences("StreamZoneData", MODE_PRIVATE)

        initViews()
        setupSpinner()
        setupListeners()
    }

    override fun onStart() {
        super.onStart()

        if (!isNetworkAvailable()) {
            showNoInternetDialog()
        }
    }

    override fun onResume() {
        super.onResume()

        restoreFormData()
        registerNetworkCallback()
    }

    override fun onPause() {
        super.onPause()

        saveFormData()
        unregisterNetworkCallback()
    }

    override fun onDestroy() {
        super.onDestroy()

        if (isFinishing) {
            clearTempData()
        }

        unregisterNetworkCallback()
    }

    private fun initViews() {
        tilFullName = findViewById(R.id.til_full_name)
        tilEmail = findViewById(R.id.til_email)
        tilPassword = findViewById(R.id.til_password)
        tilConfirmPassword = findViewById(R.id.til_confirm_password)
        etFullName = findViewById(R.id.et_full_name)
        etEmail = findViewById(R.id.et_email)
        etPassword = findViewById(R.id.et_password)
        etConfirmPassword = findViewById(R.id.et_confirm_password)
        etPhone = findViewById(R.id.et_phone)
        spinnerCountryCode = findViewById(R.id.spinner_country_code)
        btnTogglePassword = findViewById(R.id.btn_toggle_password)
        btnToggleConfirmPassword = findViewById(R.id.btn_toggle_confirm_password)
        btnRegister = findViewById(R.id.btn_register)
        checkShowPassword = findViewById(R.id.check_show_password)
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
        btnRegister.setOnClickListener {
            // 1Primero validamos todos los campos
            val fullName = etFullName.text.toString().trim()
            val email = etEmail.text.toString().trim()
            val phone = etPhone.text.toString().trim()
            val password = etPassword.text.toString()
            val confirmPassword = etConfirmPassword.text.toString()

            clearAllErrors()

            if (!validateFullName(fullName)) return@setOnClickListener
            if (!validateEmail(email)) return@setOnClickListener
            if (!validatePhone(phone)) return@setOnClickListener
            if (!validatePassword(password)) return@setOnClickListener
            if (!validateConfirmPassword(password, confirmPassword)) return@setOnClickListener

            // Si todo está bien, creamos el usuario
            val usuario = UsuarioEntity(
                fullname = fullName,
                email = email,
                phone = phone,
                password = password,
                confirmPassword = confirmPassword
            )

            // Guardamos en Room y Firebase
            val dao = AppDatabase.getInstance(this).usuarioDao()

            btnRegister.isEnabled = false
            lifecycleScope.launch {
                dao.insertar(usuario)
                FirebaseService.guardarUsuario(usuario)
                runOnUiThread {
                    Toast.makeText(this@RegisterActivity, "✅ Registro exitoso", Toast.LENGTH_SHORT).show()
                    navigateToLogin()
                    btnRegister.isEnabled = true
                }
            }

        }




        tvBackToLogin.setOnClickListener {
            navigateToLogin()
        }

        btnTogglePassword.setOnClickListener {
            togglePasswordVisibility(etPassword, btnTogglePassword, ::isPasswordVisible::get, ::isPasswordVisible::set)
        }

        btnToggleConfirmPassword.setOnClickListener {
            togglePasswordVisibility(etConfirmPassword, btnToggleConfirmPassword, ::isConfirmPasswordVisible::get, ::isConfirmPasswordVisible::set)
        }

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

        etPhone.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                if (s != null) {
                    if (s.isNotEmpty() && s[0] == '0') {
                        etPhone.setText(s.substring(1))
                        etPhone.setSelection(etPhone.text?.length ?: 0)
                        return
                    }

                    if (s.length > 10) {
                        etPhone.setText(s.substring(0, 10))
                        etPhone.setSelection(10)
                    }
                }
            }
        })
    }

    private fun handleRegister() {
        val fullName = etFullName.text.toString().trim()
        val email = etEmail.text.toString().trim()
        val phone = etPhone.text.toString().trim()
        val password = etPassword.text.toString()
        val confirmPassword = etConfirmPassword.text.toString()

        clearAllErrors()

        if (!validateFullName(fullName)) return
        if (!validateEmail(email)) return
        if (!validatePhone(phone)) return
        if (!validatePassword(password)) return
        if (!validateConfirmPassword(password, confirmPassword)) return

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
            !phone.matches(Regex("^[1-9][0-9]{9}$")) -> {
                Toast.makeText(this, "El teléfono no puede empezar con 0", Toast.LENGTH_SHORT).show()
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

    private fun showSuccessAndNavigate(name: String, email: String) {
        Toast.makeText(
            this,
            "✅ ¡Cuenta creada exitosamente!\nBienvenido $name",
            Toast.LENGTH_LONG
        ).show()

        etFullName.postDelayed({
            navigateToLogin()
        }, 1500)
    }

    private fun navigateToLogin() {
        val intent = Intent(this, HomeActivity::class.java)
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

    private fun saveFormData() {
        val name = etFullName.text.toString()
        val email = etEmail.text.toString()
        val phone = etPhone.text.toString()

        if (name.isNotEmpty() || email.isNotEmpty() || phone.isNotEmpty()) {
            sharedPrefs.edit().apply {
                putString("temp_name", name)
                putString("temp_email", email)
                putString("temp_phone", phone)
                apply()
            }
        }
    }

    private fun restoreFormData() {
        val savedName = sharedPrefs.getString("temp_name", "")
        val savedEmail = sharedPrefs.getString("temp_email", "")
        val savedPhone = sharedPrefs.getString("temp_phone", "")

        if (!savedName.isNullOrEmpty()) {
            etFullName.setText(savedName)
            etEmail.setText(savedEmail)
            etPhone.setText(savedPhone)
        }
    }

    private fun clearTempData() {
        sharedPrefs.edit().apply {
            remove("temp_name")
            remove("temp_email")
            remove("temp_phone")
            apply()
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
            .setMessage("Necesitas conexión a internet para registrarte.")
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
                    Toast.makeText(this@RegisterActivity, "Conexión restaurada", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onLost(network: Network) {
                runOnUiThread {
                    Toast.makeText(this@RegisterActivity, "Conexión perdida", Toast.LENGTH_SHORT).show()
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