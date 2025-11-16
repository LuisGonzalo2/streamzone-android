package com.universidad.streamzone.ui.admin.catalog

import android.graphics.Color
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.universidad.streamzone.R
import com.universidad.streamzone.data.local.database.AppDatabase
import com.universidad.streamzone.data.model.CategoryEntity
import kotlinx.coroutines.launch

class CategoryFormActivity : AppCompatActivity() {

    private lateinit var etName: EditText
    private lateinit var etDescription: EditText
    private lateinit var etCategoryId: EditText
    private lateinit var tvSelectedEmoji: TextView
    private lateinit var btnSelectEmoji: MaterialButton
    private lateinit var btnSelectStartColor: MaterialButton
    private lateinit var btnSelectEndColor: MaterialButton
    private lateinit var viewStartColorPreview: TextView
    private lateinit var viewEndColorPreview: TextView
    private lateinit var btnSave: Button
    private lateinit var btnCancel: Button
    private lateinit var btnBack: MaterialButton

    private var selectedEmoji = "📦"
    private var selectedStartColor = "#8B5CF6"
    private var selectedEndColor = "#6D28D9"
    private var categoryId: Int? = null
    private var editingCategory: CategoryEntity? = null

    private val emojiList = listOf(
        "📺", "🎵", "🎨", "🤖", "🎬", "🎮", "📚", "🍔",
        "✈️", "💼", "🏋️", "🎓", "🏠", "🚗", "💊", "🎭",
        "📱", "💻", "⌚", "📷", "🎧", "🎹", "🎸", "🎤",
        "⚽", "🏀", "🎾", "🏐", "🏈", "⚾", "🎯", "🎲"
    )

    private val colorPresets = listOf(
        "#8B5CF6" to "#6D28D9", // Púrpura
        "#3B82F6" to "#1D4ED8", // Azul
        "#10B981" to "#047857", // Verde
        "#F59E0B" to "#D97706", // Naranja
        "#EF4444" to "#DC2626", // Rojo
        "#EC4899" to "#DB2777", // Rosa
        "#6366F1" to "#4F46E5", // Índigo
        "#14B8A6" to "#0D9488"  // Teal
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_category_form)

        // Configurar padding para el notch
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false)
        }

        categoryId = intent.getIntExtra("CATEGORY_ID", -1).takeIf { it != -1 }

        initViews()
        setupClickListeners()

        if (categoryId != null) {
            loadCategory()
        }
    }

    private fun initViews() {
        etName = findViewById(R.id.et_category_name)
        etDescription = findViewById(R.id.et_category_description)
        etCategoryId = findViewById(R.id.et_category_id)
        tvSelectedEmoji = findViewById(R.id.tv_selected_emoji)
        btnSelectEmoji = findViewById(R.id.btn_select_emoji)
        btnSelectStartColor = findViewById(R.id.btn_select_start_color)
        btnSelectEndColor = findViewById(R.id.btn_select_end_color)
        viewStartColorPreview = findViewById(R.id.view_start_color_preview)
        viewEndColorPreview = findViewById(R.id.view_end_color_preview)
        btnSave = findViewById(R.id.btn_save_category)
        btnCancel = findViewById(R.id.btn_cancel_category)
        btnBack = findViewById(R.id.btn_back)

        // Set default values
        tvSelectedEmoji.text = selectedEmoji
        updateColorPreview(viewStartColorPreview, selectedStartColor)
        updateColorPreview(viewEndColorPreview, selectedEndColor)
    }

    private fun setupClickListeners() {
        btnSelectEmoji.setOnClickListener {
            showEmojiPicker()
        }

        btnSelectStartColor.setOnClickListener {
            showColorPicker(true)
        }

        btnSelectEndColor.setOnClickListener {
            showColorPicker(false)
        }

        btnSave.setOnClickListener {
            saveCategory()
        }

        btnCancel.setOnClickListener {
            finish()
        }

        btnBack.setOnClickListener {
            finish()
        }
    }

    private fun loadCategory() {
        lifecycleScope.launch {
            try {
                val dao = AppDatabase.getInstance(this@CategoryFormActivity).categoryDao()
                val category = categoryId?.let { dao.obtenerPorId(it) }

                category?.let {
                    editingCategory = it
                    runOnUiThread {
                        etName.setText(it.name)
                        etDescription.setText(it.description)
                        etCategoryId.setText(it.categoryId)
                        selectedEmoji = it.icon
                        selectedStartColor = it.gradientStart
                        selectedEndColor = it.gradientEnd

                        tvSelectedEmoji.text = selectedEmoji
                        updateColorPreview(viewStartColorPreview, selectedStartColor)
                        updateColorPreview(viewEndColorPreview, selectedEndColor)

                        // Cambiar título
                        supportActionBar?.title = "Editar Categoría"
                    }
                }
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this@CategoryFormActivity, "Error al cargar categoría", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
        }
    }

    private fun showEmojiPicker() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Selecciona un emoji")

        val emojiArray = emojiList.toTypedArray()
        builder.setItems(emojiArray) { _, which ->
            selectedEmoji = emojiList[which]
            tvSelectedEmoji.text = selectedEmoji
        }

        builder.setNegativeButton("Cancelar", null)
        builder.show()
    }

    private fun showColorPicker(isStartColor: Boolean) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle(if (isStartColor) "Color inicial del gradiente" else "Color final del gradiente")

        val colorNames = arrayOf(
            "Púrpura", "Azul", "Verde", "Naranja",
            "Rojo", "Rosa", "Índigo", "Teal"
        )

        builder.setItems(colorNames) { _, which ->
            val colorPair = colorPresets[which]
            if (isStartColor) {
                selectedStartColor = colorPair.first
                updateColorPreview(viewStartColorPreview, selectedStartColor)
            } else {
                selectedEndColor = colorPair.second
                updateColorPreview(viewEndColorPreview, selectedEndColor)
            }
        }

        builder.setNegativeButton("Cancelar", null)
        builder.show()
    }

    private fun updateColorPreview(view: TextView, colorHex: String) {
        try {
            val color = Color.parseColor(colorHex)
            view.setBackgroundColor(color)
            view.text = colorHex
        } catch (e: Exception) {
            view.setBackgroundColor(Color.GRAY)
            view.text = "Error"
        }
    }

    private fun saveCategory() {
        val name = etName.text.toString().trim()
        val description = etDescription.text.toString().trim()
        val catId = etCategoryId.text.toString().trim()

        // Validaciones
        if (name.isEmpty()) {
            etName.error = "El nombre es obligatorio"
            etName.requestFocus()
            return
        }

        if (catId.isEmpty()) {
            etCategoryId.error = "El ID es obligatorio"
            etCategoryId.requestFocus()
            return
        }

        if (description.isEmpty()) {
            etDescription.error = "La descripción es obligatoria"
            etDescription.requestFocus()
            return
        }

        lifecycleScope.launch {
            try {
                val dao = AppDatabase.getInstance(this@CategoryFormActivity).categoryDao()

                // Verificar si el categoryId ya existe (solo al crear)
                if (editingCategory == null) {
                    val existing = dao.obtenerPorCategoryId(catId)
                    if (existing != null) {
                        runOnUiThread {
                            etCategoryId.error = "Este ID ya existe"
                            etCategoryId.requestFocus()
                        }
                        return@launch
                    }
                }

                val category = if (editingCategory != null) {
                    // Actualizar
                    editingCategory!!.copy(
                        categoryId = catId,
                        name = name,
                        description = description,
                        icon = selectedEmoji,
                        gradientStart = selectedStartColor,
                        gradientEnd = selectedEndColor,
                        sincronizado = false
                    )
                } else {
                    // Crear nuevo
                    CategoryEntity(
                        categoryId = catId,
                        name = name,
                        description = description,
                        icon = selectedEmoji,
                        gradientStart = selectedStartColor,
                        gradientEnd = selectedEndColor,
                        isActive = true,
                        sincronizado = false
                    )
                }

                if (editingCategory != null) {
                    dao.actualizar(category)
                } else {
                    dao.insertar(category)
                }

                // TODO: Sincronizar con Firebase

                runOnUiThread {
                    Toast.makeText(
                        this@CategoryFormActivity,
                        if (editingCategory != null) "Categoría actualizada" else "Categoría creada",
                        Toast.LENGTH_SHORT
                    ).show()
                    finish()
                }

            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(
                        this@CategoryFormActivity,
                        "Error al guardar: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }
}