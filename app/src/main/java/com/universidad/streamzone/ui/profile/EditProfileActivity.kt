package com.universidad.streamzone.ui.profile

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Base64
import android.util.Log
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.content.FileProvider
import androidx.exifinterface.media.ExifInterface
import androidx.lifecycle.lifecycleScope
import com.universidad.streamzone.R
import com.universidad.streamzone.data.local.database.AppDatabase
import com.universidad.streamzone.data.remote.FirebaseService
import kotlinx.coroutines.launch
import com.google.android.material.button.MaterialButton
import com.hbb20.CountryCodePicker
import java.io.ByteArrayOutputStream
import java.io.File

class EditProfileActivity : AppCompatActivity() {

    private lateinit var sharedPrefs: SharedPreferences
    private lateinit var etFullName: EditText
    private lateinit var tvPhoneDisplay: TextView
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

    // Componentes de foto de perfil
    private lateinit var imgFotoPerfil: ImageView
    private lateinit var cardFotoPerfil: CardView
    private var fotoBase64: String? = null
    private var fotoUri: Uri? = null

    private var isCurrentPasswordVisible = false
    private var isNewPasswordVisible = false
    private var isConfirmPasswordVisible = false
    private var isChangePasswordExpanded = false

    private var userEmail: String = ""



    // Activity Result Launchers para cámara y galería
    private val camaraLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && fotoUri != null) {
            val bitmap = corregirRotacionImagen(fotoUri!!)
            imgFotoPerfil.setImageBitmap(bitmap)
            fotoBase64 = convertirBitmapABase64(bitmap)
        }
    }

    private val galeriaLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                val bitmap = corregirRotacionImagen(uri)
                imgFotoPerfil.setImageBitmap(bitmap)
                fotoBase64 = convertirBitmapABase64(bitmap)
            }
        }
    }

    private val permisosCamaraLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        when {
            permissions[Manifest.permission.CAMERA] == true -> {
                abrirCamara()
            }
            else -> {
                Toast.makeText(this, "Permiso de cámara denegado", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private val permisosGaleriaLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            abrirGaleria()
        } else {
            Toast.makeText(this, "Permiso de galería denegado", Toast.LENGTH_SHORT).show()
        }
    }

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
        loadUserData()
        setupClickListeners()
    }

    private fun initViews() {
        etFullName = findViewById(R.id.et_full_name)
        tvEmailDisplay = findViewById(R.id.tv_email_display)
        tvPhoneDisplay = findViewById(R.id.tv_phone_display)
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

        // Componentes de foto
        imgFotoPerfil = findViewById(R.id.img_foto_perfil)
        cardFotoPerfil = findViewById(R.id.card_foto_perfil)
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

                        tvPhoneDisplay.text = user.phone
                        // Cargar foto de perfil si existe
                        if (!user.fotoBase64.isNullOrEmpty()) {
                            val bitmap = convertirBase64ABitmap(user.fotoBase64!!)
                            imgFotoPerfil.setImageBitmap(bitmap)
                            fotoBase64 = user.fotoBase64
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("EditProfile", "Error al cargar datos", e)
            }
        }
    }

    private fun setupClickListeners() {
        btnBack.setOnClickListener {
            finish()
        }

        // Click en foto de perfil para cambiarla
        cardFotoPerfil.setOnClickListener {
            mostrarDialogoSeleccionImagen()
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

    // ========================================
    // FUNCIONES DE FOTO DE PERFIL
    // ========================================

    private fun mostrarDialogoSeleccionImagen() {
        val opciones = arrayOf("📷 Tomar foto", "🖼️ Elegir de galería")

        AlertDialog.Builder(this)
            .setTitle("Cambiar foto de perfil")
            .setItems(opciones) { _, which ->
                when (which) {
                    0 -> solicitarPermisoCamara()
                    1 -> solicitarPermisoGaleria()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun solicitarPermisoCamara() {
        permisosCamaraLauncher.launch(arrayOf(Manifest.permission.CAMERA))
    }

    private fun solicitarPermisoGaleria() {
        val permiso = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
        permisosGaleriaLauncher.launch(permiso)
    }

    private fun abrirCamara() {
        val file = File(filesDir, "foto_perfil_${System.currentTimeMillis()}.jpg")
        fotoUri = FileProvider.getUriForFile(
            this,
            "${packageName}.fileprovider",
            file
        )
        fotoUri?.let { uri ->
            camaraLauncher.launch(uri)
        }
    }

    private fun abrirGaleria() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        galeriaLauncher.launch(intent)
    }

    private fun corregirRotacionImagen(uri: Uri): Bitmap {
        val bitmap = BitmapFactory.decodeStream(contentResolver.openInputStream(uri))

        val inputStream = contentResolver.openInputStream(uri)
        val exif = ExifInterface(inputStream!!)
        val orientation = exif.getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_NORMAL
        )

        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
        }

        val rotatedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)

        // Redimensionar si es muy grande (max 800x800)
        return if (rotatedBitmap.width > 800 || rotatedBitmap.height > 800) {
            val ratio = Math.min(800.0 / rotatedBitmap.width, 800.0 / rotatedBitmap.height)
            val width = (ratio * rotatedBitmap.width).toInt()
            val height = (ratio * rotatedBitmap.height).toInt()
            Bitmap.createScaledBitmap(rotatedBitmap, width, height, true)
        } else {
            rotatedBitmap
        }
    }

    private fun convertirBitmapABase64(bitmap: Bitmap): String {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 70, outputStream)
        val byteArray = outputStream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.DEFAULT)
    }

    private fun convertirBase64ABitmap(base64: String): Bitmap {
        val decodedBytes = Base64.decode(base64, Base64.DEFAULT)
        return BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
    }

    // ========================================
    // FIN FUNCIONES DE FOTO DE PERFIL
    // ========================================

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

                // Actualizar datos (incluye foto si cambió)
                val updatedUser = usuario.copy(
                    fullname = newFullName,
                    password = if (changingPassword) newPassword else usuario.password,
                    fotoBase64 = fotoBase64 ?: usuario.fotoBase64  // Mantener foto existente si no cambió
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