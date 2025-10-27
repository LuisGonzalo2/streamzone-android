package com.universidad.streamzone.ui.auth

import android.content.Intent
import android.content.SharedPreferences
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.Bundle
import android.text.Editable
import android.text.InputFilter
import android.text.InputType
import android.text.TextWatcher
import android.util.Log
import android.util.Patterns
import android.widget.ArrayAdapter
import android.widget.CheckBox
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.hbb20.CountryCodePicker
import com.universidad.streamzone.R
import com.universidad.streamzone.data.remote.FirebaseService
import com.universidad.streamzone.data.local.dao.UsuarioDao
import com.universidad.streamzone.data.local.database.AppDatabase
import com.universidad.streamzone.data.model.UsuarioEntity
import kotlinx.coroutines.launch
import kotlin.text.substringAfterLast

class RegisterActivity : AppCompatActivity() {

    private fun registerNetworkCallback() {
        val connectivityManager = getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager

        networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                // Conexión restaurada, no mostrar mensaje
            }

            override fun onLost(network: Network) {
                // Conexión perdida, no mostrar mensaje
            }
        }

        try {
            connectivityManager.registerDefaultNetworkCallback(networkCallback!!)
        } catch (e: Exception) {
            // Silencioso
        }
    }private lateinit var tilFullName: TextInputLayout
    private lateinit var tilEmail: TextInputLayout
    // AÑADE ESTA LÍNEA
    private lateinit var ccp: CountryCodePicker

    private lateinit var tilPassword: TextInputLayout
    private lateinit var tilConfirmPassword: TextInputLayout
    private lateinit var tilPhone: TextInputLayout
    private lateinit var etFullName: TextInputEditText
    private lateinit var etEmail: TextInputEditText
    private lateinit var etPassword: TextInputEditText
    private lateinit var etConfirmPassword: TextInputEditText
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
        setupPhone()
        setupListeners()
    }

    override fun onResume() {
        super.onResume()
        restoreFormData()
    }

    override fun onPause() {
        super.onPause()
        saveFormData()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isFinishing) {
            clearTempData()
        }
    }

    private fun initViews() {
        tilFullName = findViewById(R.id.til_full_name)
        tilEmail = findViewById(R.id.til_email)
        tilPassword = findViewById(R.id.til_password)
        tilConfirmPassword = findViewById(R.id.til_confirm_password)
        tilPhone = findViewById(R.id.til_phone)
        ccp = findViewById(R.id.ccp)
        etFullName = findViewById(R.id.et_full_name)
        etEmail = findViewById(R.id.et_email)
        etPassword = findViewById(R.id.et_password)
        etConfirmPassword = findViewById(R.id.et_confirm_password)
        etPhone = findViewById(R.id.et_Phone)
        btnTogglePassword = findViewById(R.id.btn_toggle_password)
        btnToggleConfirmPassword = findViewById(R.id.btn_toggle_confirm_password)
        btnRegister = findViewById(R.id.btn_register)
        checkShowPassword = findViewById(R.id.check_show_password)
        tvBackToLogin = findViewById(R.id.tv_back_to_login)
        ccp.registerCarrierNumberEditText(etPhone)





    }

    private fun setupPhone() {
        // Filtro para permitir solo dígitos, guiones y espacios
        val digitsDashSpaceFilter = InputFilter { source, start, end, dest, dstart, dend ->
            for (i in start until end) {
                val char = source[i]
                if (!char.isDigit() && char != '-' && char != ' ') {
                    return@InputFilter ""
                }
            }
            null
        }

        // Límite de longitud máxima (15 dígitos según estándar E.164)
        val lengthFilter = InputFilter.LengthFilter(15)

        // Aplicar ambos filtros
        etPhone.filters = arrayOf(digitsDashSpaceFilter, lengthFilter)

        // Listener para quitar el 0 inicial automáticamente
        etPhone.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                if (s != null && s.isNotEmpty() && s[0] == '0') {
                    etPhone.setText(s.substring(1))
                    etPhone.setSelection(etPhone.text?.length ?: 0)
                }
            }
        })
    }



    private fun setupListeners() {
        btnRegister.setOnClickListener {
            handleRegister()
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
                btnTogglePassword.setIconResource(R.drawable.ic_visibility_off)
                btnToggleConfirmPassword.setIconResource(R.drawable.ic_visibility_off)
                isPasswordVisible = true
                isConfirmPasswordVisible = true
            } else {
                etPassword.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                etConfirmPassword.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                btnTogglePassword.setIconResource(R.drawable.ic_visibility)
                btnToggleConfirmPassword.setIconResource(R.drawable.ic_visibility)
                isPasswordVisible = false
                isConfirmPasswordVisible = false
            }
            etPassword.setSelection(etPassword.text?.length ?: 0)
            etConfirmPassword.setSelection(etConfirmPassword.text?.length ?: 0)
        }


    }

    private fun handleRegister() {
        val fullName = etFullName.text.toString().trim()
        val email = etEmail.text.toString().trim()
        val phoneWithCountryCode = ccp.fullNumberWithPlus // Ej: +593987654321
        val phone = phoneWithCountryCode.replace("+", "") // Guardar sin el +
        val password = etPassword.text.toString()
        val confirmPassword = etConfirmPassword.text.toString()


        clearAllErrors()

        if (!validateFullName(fullName)) return
        if (!validatePhone(phone)) return
        if (!validateEmail(email)) return
        if (!validatePassword(password)) return
        if (!validateConfirmPassword(password, confirmPassword)) return

        // Mostrar que está procesando
        runOnUiThread {
            btnRegister.isEnabled = false
            btnRegister.text = "Creando cuenta..."
        }

        val dao = AppDatabase.Companion.getInstance(this).usuarioDao()

        lifecycleScope.launch {
            try {
                Log.d("RegisterActivity", "Iniciando registro para: $email")

                // PASO 1: Verificar duplicados locales (Room)
                val usuarioExistentePorEmail = dao.buscarPorEmail(email)
                val usuarioExistentePorTelefono = dao.buscarPorTelefono(phone)

                Log.d("RegisterActivity", "Usuario por email en Room: $usuarioExistentePorEmail")
                Log.d("RegisterActivity", "Usuario por teléfono en Room: $usuarioExistentePorTelefono")

                if (usuarioExistentePorEmail != null) {
                    runOnUiThread {
                        tilEmail.error = "Este correo ya está registrado"
                        btnRegister.isEnabled = true
                        btnRegister.text = "Crear cuenta"
                    }
                    return@launch
                }

                if (usuarioExistentePorTelefono != null) {
                    runOnUiThread {
                        etPhone.error = "Este número ya está registrado"
                        btnRegister.isEnabled = true
                        btnRegister.text = "Crear cuenta"
                    }
                    return@launch
                }

                // PASO 2: Si hay internet, verificar duplicados en Firebase
                val hayInternet = isNetworkAvailable()
                Log.d("RegisterActivity", "Hay internet: $hayInternet")

                if (hayInternet) {
                    // Verificar email en Firebase
                    FirebaseService.verificarEmailExiste(email) { emailExiste ->
                        if (emailExiste) {
                            runOnUiThread {
                                tilEmail.error = "Este correo ya está registrado"
                                btnRegister.isEnabled = true
                                btnRegister.text = "Crear cuenta"
                            }
                            return@verificarEmailExiste
                        }

                        // Verificar teléfono en Firebase
                        FirebaseService.verificarTelefonoExiste(phone) { telefonoExiste ->
                            if (telefonoExiste) {
                                runOnUiThread {
                                    etPhone.error = "Este número ya está registrado"
                                    btnRegister.isEnabled = true
                                    btnRegister.text = "Crear cuenta"
                                }
                                return@verificarTelefonoExiste
                            }

                            // PASO 3: No hay duplicados, proceder a guardar
                            guardarUsuario(fullName, email, phone, password, confirmPassword, dao, hayInternet)
                        }
                    }
                } else {
                    // Sin internet, solo guardar localmente
                    guardarUsuario(fullName, email, phone, password, confirmPassword, dao, hayInternet)
                }

            } catch (e: Exception) {
                Log.e("RegisterActivity", "Error general en handleRegister", e)
                runOnUiThread {
                    btnRegister.isEnabled = true
                    btnRegister.text = "Crear cuenta"
                    Toast.makeText(
                        this@RegisterActivity,
                        "Error al crear la cuenta. Intenta de nuevo",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun guardarUsuario(
        fullName: String,
        email: String,
        phone: String,
        password: String,
        confirmPassword: String,
        dao: UsuarioDao,
        hayInternet: Boolean
    ) {
        lifecycleScope.launch {
            try {
                // Obtener número completo con código de país
                val fullPhoneNumber = ccp.fullNumberWithPlus.replace("+", "")
                // Crear usuario
                val usuario = UsuarioEntity(
                    fullname = fullName,
                    email = email,
                    phone = phone,
                    password = password,
                    confirmPassword = confirmPassword,
                    sincronizado = false
                )

                Log.d("RegisterActivity", "Usuario creado: $usuario")

                if (hayInternet) {
                    Log.d("RegisterActivity", "Guardando en Firebase...")
                    runOnUiThread {
                        btnRegister.text = "Creando cuenta..."
                    }

                    // HAY INTERNET: Intentar guardar en Firebase primero
                    FirebaseService.guardarUsuario(
                        usuario = usuario,
                        onSuccess = { firebaseId ->
                            Log.d("RegisterActivity", "Guardado en Firebase con ID: $firebaseId")
                            lifecycleScope.launch {
                                try {
                                    // Guardar en Room con flag sincronizado
                                    val usuarioSincronizado = usuario.copy(
                                        sincronizado = true,
                                        firebaseId = firebaseId
                                    )
                                    dao.insertar(usuarioSincronizado)
                                    Log.d("RegisterActivity", "Guardado en Room")

                                    runOnUiThread {
                                        btnRegister.isEnabled = true
                                        btnRegister.text = "Crear cuenta"
                                        Toast.makeText(
                                            this@RegisterActivity,
                                            "Cuenta creada exitosamente",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        navigateToLogin()
                                    }
                                } catch (e: Exception) {
                                    Log.e("RegisterActivity", "Error al guardar en Room", e)
                                    runOnUiThread {
                                        btnRegister.isEnabled = true
                                        btnRegister.text = "Crear cuenta"
                                        Toast.makeText(
                                            this@RegisterActivity,
                                            "Error al crear la cuenta. Intenta de nuevo",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            }
                        },
                        onFailure = { e ->
                            Log.e("RegisterActivity", "Error en Firebase", e)
                            lifecycleScope.launch {
                                try {
                                    // Si falla Firebase, guardar solo en Room
                                    dao.insertar(usuario)
                                    Log.d("RegisterActivity", "Guardado solo en Room")
                                    runOnUiThread {
                                        btnRegister.isEnabled = true
                                        btnRegister.text = "Crear cuenta"
                                        Toast.makeText(
                                            this@RegisterActivity,
                                            "Cuenta creada exitosamente",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        navigateToLogin()
                                    }
                                } catch (ex: Exception) {
                                    Log.e("RegisterActivity", "Error al guardar en Room", ex)
                                    runOnUiThread {
                                        btnRegister.isEnabled = true
                                        btnRegister.text = "Crear cuenta"
                                        Toast.makeText(
                                            this@RegisterActivity,
                                            "Error al crear la cuenta. Intenta de nuevo",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            }
                        }
                    )
                } else {
                    Log.d("RegisterActivity", "Sin internet, guardando solo en Room...")
                    runOnUiThread {
                        btnRegister.text = "Creando cuenta..."
                    }

                    try {
                        // NO HAY INTERNET: Guardar solo en Room
                        dao.insertar(usuario)
                        Log.d("RegisterActivity", "Guardado en Room exitosamente")
                        runOnUiThread {
                            btnRegister.isEnabled = true
                            btnRegister.text = "Crear cuenta"
                            Toast.makeText(
                                this@RegisterActivity,
                                "Cuenta creada exitosamente",
                                Toast.LENGTH_SHORT
                            ).show()
                            navigateToLogin()
                        }
                    } catch (e: Exception) {
                        Log.e("RegisterActivity", "Error al guardar en Room", e)
                        runOnUiThread {
                            btnRegister.isEnabled = true
                            btnRegister.text = "Crear cuenta"
                            Toast.makeText(
                                this@RegisterActivity,
                                "Error al crear la cuenta. Intenta de nuevo",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("RegisterActivity", "Error en guardarUsuario", e)
                runOnUiThread {
                    btnRegister.isEnabled = true
                    btnRegister.text = "Crear cuenta"
                    Toast.makeText(
                        this@RegisterActivity,
                        "Error al crear la cuenta. Intenta de nuevo",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
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
            name.length > 20 -> {
                tilFullName.error = "El nombre no puede exceder los 20 caracteres"
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

    private fun validatePhone(phone: String): Boolean {
        return when {
            phone.isEmpty() -> {
                etPhone.error = "El número de teléfono es obligatorio"
                etPhone.requestFocus()
                false
            }
            phone.startsWith("0") -> {
                etPhone.error = "No incluyas el 0 inicial"
                etPhone.requestFocus()
                false
            }
            !ccp.isValidFullNumber -> {
                val countryName = ccp.selectedCountryName
                val phoneLength = etPhone.text.toString().replace(Regex("[^0-9]"), "").length

                // Validación específica por país
                val mensaje = when (ccp.selectedCountryNameCode) {
                    "EC" -> if (phoneLength != 9) "El número debe tener 9 dígitos (sin el 0)" else "Número no válido"
                    "US", "CA" -> if (phoneLength != 10) "El número debe tener 10 dígitos" else "Número no válido"
                    "CO" -> if (phoneLength != 10) "El número debe tener 10 dígitos" else "Número no válido"
                    "PE" -> if (phoneLength != 9) "El número debe tener 9 dígitos" else "Número no válido"
                    "MX" -> if (phoneLength != 10) "El número debe tener 10 dígitos" else "Número no válido"
                    else -> "Número no válido para $countryName"
                }

                etPhone.error = mensaje
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
            password.length > 20 -> {
                tilPassword.error = "La contraseña debe tener como maximo 20 caracteres"
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

    private fun navigateToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        // Pasar el email registrado al login
        intent.putExtra("registered_email", etEmail.text.toString().trim())
        startActivity(intent)
        finish()
    }

    private fun togglePasswordVisibility(
        editText: TextInputEditText,
        button: MaterialButton,
        getter: () -> Boolean,
        setter: (Boolean) -> Unit
    ) {
        val currentTypeface = editText.typeface
        val selection = editText.selectionEnd

        if (getter()) {
            editText.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            button.setIconResource(R.drawable.ic_visibility)
            setter(false)
        } else {
            editText.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            button.setIconResource(R.drawable.ic_visibility_off)
            setter(true)
        }
        editText.typeface = currentTypeface
        editText.setSelection(selection)
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
        val connectivityManager = getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false

        return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
    }
}