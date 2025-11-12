package com.universidad.streamzone.ui.profile

import android.content.SharedPreferences
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.lifecycle.lifecycleScope
import com.universidad.streamzone.R
import com.universidad.streamzone.data.local.database.AppDatabase
import com.universidad.streamzone.data.remote.FirebaseService
import kotlinx.coroutines.launch
import com.google.android.material.button.MaterialButton

class EditProfileActivity : AppCompatActivity() {

    private lateinit var sharedPrefs: SharedPreferences
    private lateinit var etFullName: EditText
    private lateinit var spinnerCountry: Spinner
    private lateinit var tvCountryCode: TextView
    private lateinit var etPhone: EditText
    private lateinit var tvEmailDisplay: TextView
    private lateinit var etCurrentPassword: EditText
    private lateinit var etNewPassword: EditText
    private lateinit var etConfirmPassword: EditText
    private lateinit var btnSaveChanges: Button
    private lateinit var btnBack: Button
    private lateinit var btnToggleChangePassword: Button
    private lateinit var cardChangePassword: CardView

    private lateinit var btnToggleCurrentPassword: MaterialButton
    private lateinit var btnToggleNewPassword: MaterialButton
    private lateinit var btnToggleConfirmPassword: MaterialButton

    private var isCurrentPasswordVisible = false
    private var isNewPasswordVisible = false
    private var isConfirmPasswordVisible = false
    private var isChangePasswordExpanded = false

    private var userEmail: String = ""
    private var selectedCountryCode: String = "+593"

    // Países con sus códigos
    private val countries = mapOf(
        "Ecuador 🇪🇨" to "+593",
        "Estados Unidos 🇺🇸" to "+1",
        "México 🇲🇽" to "+52",
        "Colombia 🇨🇴" to "+57",
        "Perú 🇵🇪" to "+51",
        "Argentina 🇦🇷" to "+54",
        "Chile 🇨🇱" to "+56",
        "España 🇪🇸" to "+34"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_profile)

        // Configurar padding para el notch
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false)
        }

        val scrollView = findViewById<ScrollView>(R.id.edit_profile_scroll_view)
        scrollView?.setOnApplyWindowInsetsListener { view, insets ->
            val systemBars = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                insets.getInsets(android.view.WindowInsets.Type.systemBars())
            } else {
                @Suppress("DEPRECATION")
                android.graphics.Insets.of(0, insets.systemWindowInsetTop, 0, 0)
            }
            view.setPadding(
                view.paddingLeft,
                systemBars.top + 16,
                view.paddingRight,
                view.paddingBottom
            )
            insets
        }

        sharedPrefs = getSharedPreferences("StreamZoneData", MODE_PRIVATE)

        initViews()
        setupCountrySpinner()
        loadUserData()
        setupClickListeners()
    }

    private fun initViews() {
        etFullName = findViewById(R.id.et_full_name)
        spinnerCountry = findViewById(R.id.spinner_country)
        tvCountryCode = findViewById(R.id.tv_country_code)
        etPhone = findViewById(R.id.et_phone)
        tvEmailDisplay = findViewById(R.id.tv_email_display)
        etCurrentPassword = findViewById(R.id.et_current_password)
        etNewPassword = findViewById(R.id.et_new_password)
        etConfirmPassword = findViewById(R.id.et_confirm_password)
        btnSaveChanges = findViewById(R.id.btn_save_changes)
        btnBack = findViewById(R.id.btn_back)
        btnToggleChangePassword = findViewById(R.id.btn_toggle_change_password)
        cardChangePassword = findViewById(R.id.card_change_password)

        btnToggleCurrentPassword = findViewById(R.id.btn_toggle_current_password)
        btnToggleNewPassword = findViewById(R.id.btn_toggle_new_password)
        btnToggleConfirmPassword = findViewById(R.id.btn_toggle_confirm_password)
    }

    private fun setupCountrySpinner() {
        val countryNames = countries.keys.toList()

        // Usar layouts personalizados
        val adapter = ArrayAdapter(this, R.layout.spinner_item, countryNames)
        adapter.setDropDownViewResource(R.layout.spinner_dropdown_item)
        spinnerCountry.adapter = adapter

        // Listener para cambiar el código cuando se selecciona un país
        spinnerCountry.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedCountry = countryNames[position]
                selectedCountryCode = countries[selectedCountry] ?: "+593"
                tvCountryCode.text = selectedCountryCode

                // Validar teléfono cuando cambia el país
                validatePhoneNumber()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // Validación en tiempo real del teléfono
        etPhone.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                validatePhoneNumber()
            }
        })
    }

    private fun validatePhoneNumber() {
        val phone = etPhone.text.toString()

        if (phone.isEmpty()) {
            etPhone.error = null
            return
        }

        val minLength = when (selectedCountryCode) {
            "+593" -> 9  // Ecuador
            "+1" -> 10   // USA
            "+52" -> 10  // México
            "+57" -> 10  // Colombia
            "+51" -> 9   // Perú
            "+54" -> 10  // Argentina
            "+56" -> 9   // Chile
            "+34" -> 9   // España
            else -> 8
        }

        if (phone.length < minLength) {
            etPhone.error = "Debe tener al menos $minLength dígitos"
        } else {
            etPhone.error = null
        }
    }

    private fun loadUserData() {
        userEmail = sharedPrefs.getString("logged_in_user_email", "") ?: ""

        if (userEmail.isEmpty()) {
            Toast.makeText(this, "Error: No hay sesión activa", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        tvEmailDisplay.text = userEmail

        lifecycleScope.launch {
            try {
                val dao = AppDatabase.getInstance(this@EditProfileActivity).usuarioDao()
                val usuario = dao.buscarPorEmail(userEmail)

                usuario?.let { user ->
                    runOnUiThread {
                        etFullName.setText(user.fullname)

                        // Parsear teléfono con código de país
                        if (!user.phone.isNullOrEmpty()) {
                            parseAndSetPhone(user.phone)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("EditProfile", "Error al cargar datos", e)
            }
        }
    }

    private fun parseAndSetPhone(fullPhone: String) {
        // Intentar extraer código de país del teléfono guardado
        countries.entries.forEachIndexed { index, entry ->
            if (fullPhone.startsWith(entry.value)) {
                spinnerCountry.setSelection(index)
                selectedCountryCode = entry.value
                tvCountryCode.text = selectedCountryCode

                // Quitar código de país del número
                val phoneWithoutCode = fullPhone.removePrefix(entry.value).trim()
                etPhone.setText(phoneWithoutCode)
                return
            }
        }

        // Si no tiene código, asumir que es solo el número
        etPhone.setText(fullPhone)
    }

    private fun setupClickListeners() {
        btnBack.setOnClickListener {
            finish()
        }

        // Toggle para mostrar/ocultar sección de cambiar contraseña
        btnToggleChangePassword.setOnClickListener {
            isChangePasswordExpanded = !isChangePasswordExpanded

            if (isChangePasswordExpanded) {
                cardChangePassword.visibility = View.VISIBLE
                btnToggleChangePassword.text = "🔒 Ocultar Cambio de Contraseña"
            } else {
                cardChangePassword.visibility = View.GONE
                btnToggleChangePassword.text = "🔒 Cambiar Contraseña"

                // Limpiar campos de contraseña
                etNewPassword.setText("")
                etConfirmPassword.setText("")
            }
        }

        btnSaveChanges.setOnClickListener {
            saveChanges()
        }

        // Toggle contraseña actual
        btnToggleCurrentPassword.setOnClickListener {
            isCurrentPasswordVisible = !isCurrentPasswordVisible
            togglePasswordVisibility(etCurrentPassword, btnToggleCurrentPassword, isCurrentPasswordVisible)
        }

        // Toggle nueva contraseña
        btnToggleNewPassword.setOnClickListener {
            isNewPasswordVisible = !isNewPasswordVisible
            togglePasswordVisibility(etNewPassword, btnToggleNewPassword, isNewPasswordVisible)
        }

        // Toggle confirmar contraseña
        btnToggleConfirmPassword.setOnClickListener {
            isConfirmPasswordVisible = !isConfirmPasswordVisible
            togglePasswordVisibility(etConfirmPassword, btnToggleConfirmPassword, isConfirmPasswordVisible)
        }
    }

    private fun togglePasswordVisibility(editText: EditText, button: MaterialButton, isVisible: Boolean) {
        if (isVisible) {
            editText.inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            button.setIconResource(R.drawable.ic_visibility_off)
        } else {
            editText.inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
            button.setIconResource(R.drawable.ic_visibility)
        }
        editText.setSelection(editText.text.length)
    }

    private fun saveChanges() {
        val newFullName = etFullName.text.toString().trim()
        val newPhone = etPhone.text.toString().trim()
        val currentPassword = etCurrentPassword.text.toString()
        val newPassword = etNewPassword.text.toString()
        val confirmPassword = etConfirmPassword.text.toString()

        // Validar que ingresó la contraseña actual (OBLIGATORIO)
        if (currentPassword.isEmpty()) {
            Toast.makeText(this, "⚠️ Ingresa tu contraseña actual para confirmar cambios", Toast.LENGTH_LONG).show()
            etCurrentPassword.requestFocus()
            return
        }

        // Validaciones básicas
        if (newFullName.isEmpty()) {
            Toast.makeText(this, "El nombre no puede estar vacío", Toast.LENGTH_SHORT).show()
            return
        }

        // Validar teléfono si no está vacío
        if (newPhone.isNotEmpty()) {
            if (etPhone.error != null) {
                Toast.makeText(this, "Corrige el número de teléfono", Toast.LENGTH_SHORT).show()
                return
            }
        }

        // Si está intentando cambiar la contraseña
        val changingPassword = isChangePasswordExpanded && (newPassword.isNotEmpty() || confirmPassword.isNotEmpty())

        if (changingPassword) {
            if (!validatePasswordChange(newPassword, confirmPassword)) {
                return
            }
        }

        // Guardar cambios
        lifecycleScope.launch {
            try {
                val dao = AppDatabase.getInstance(this@EditProfileActivity).usuarioDao()
                val usuario = dao.buscarPorEmail(userEmail)

                if (usuario == null) {
                    runOnUiThread {
                        Toast.makeText(this@EditProfileActivity, "Error: Usuario no encontrado", Toast.LENGTH_SHORT).show()
                    }
                    return@launch
                }

                // Verificar contraseña actual (SIEMPRE)
                if (usuario.password != currentPassword) {
                    runOnUiThread {
                        Toast.makeText(this@EditProfileActivity, "❌ La contraseña actual es incorrecta", Toast.LENGTH_LONG).show()
                        etCurrentPassword.requestFocus()
                    }
                    return@launch
                }

                // Construir teléfono completo con código de país
                val fullPhone = if (newPhone.isNotEmpty()) {
                    "$selectedCountryCode$newPhone"
                } else {
                    usuario.phone
                }

                // Actualizar datos
                val updatedUser = usuario.copy(
                    fullname = newFullName,
                    phone = fullPhone,
                    password = if (changingPassword) newPassword else usuario.password
                )

                dao.actualizar(updatedUser)

                // Sincronizar con Firebase
                FirebaseService.actualizarUsuario(
                    updatedUser,
                    onSuccess = {
                        Log.d("EditProfile", "Usuario sincronizado con Firebase")
                    },
                    onFailure = { e ->
                        Log.e("EditProfile", "Error al sincronizar con Firebase", e)
                    }
                )

                // Actualizar SharedPreferences
                sharedPrefs.edit().apply {
                    putString("logged_in_user_name", newFullName)
                    apply()
                }

                runOnUiThread {
                    Toast.makeText(
                        this@EditProfileActivity,
                        "✅ Perfil actualizado correctamente",
                        Toast.LENGTH_LONG
                    ).show()

                    // Indicar que se debe recargar el perfil
                    setResult(RESULT_OK)
                    finish()
                }

            } catch (e: Exception) {
                Log.e("EditProfile", "Error al guardar cambios", e)
                runOnUiThread {
                    Toast.makeText(
                        this@EditProfileActivity,
                        "Error al guardar cambios: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun validatePasswordChange(new: String, confirm: String): Boolean {
        if (new.isEmpty()) {
            Toast.makeText(this, "Ingresa tu nueva contraseña", Toast.LENGTH_SHORT).show()
            return false
        }

        if (new.length < 6) {
            Toast.makeText(this, "La contraseña debe tener al menos 6 caracteres", Toast.LENGTH_SHORT).show()
            return false
        }

        if (new != confirm) {
            Toast.makeText(this, "Las contraseñas no coinciden", Toast.LENGTH_SHORT).show()
            return false
        }

        return true
    }
}