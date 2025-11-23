package com.universidad.streamzone.ui.admin

import android.os.Bundle
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.SwitchCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.Timestamp
import com.universidad.streamzone.R
import com.universidad.streamzone.data.firebase.models.Category
import com.universidad.streamzone.data.firebase.repository.CategoryRepository
import com.universidad.streamzone.util.PermissionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CreateEditCategoryActivity : BaseAdminActivity() {

    override val requiredPermission: String = PermissionManager.MANAGE_CATEGORIES

    // Firebase Repository
    private val categoryRepository = CategoryRepository()

    private lateinit var btnBack: ImageButton
    private lateinit var tvTitle: TextView
    private lateinit var etCategoryIcon: TextInputEditText
    private lateinit var etCategoryName: TextInputEditText
    private lateinit var etCategoryDescription: TextInputEditText
    private lateinit var etGradientStart: TextInputEditText
    private lateinit var etGradientEnd: TextInputEditText
    private lateinit var switchIsActive: SwitchCompat
    private lateinit var btnSave: MaterialButton

    private var categoryId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_edit_category)
    }

    override fun onPermissionGranted() {
        initViews()

        // Verificar si es edición
        val categoryIdFromIntent = intent.getStringExtra("CATEGORY_ID")
        if (categoryIdFromIntent != null && categoryIdFromIntent.isNotEmpty()) {
            categoryId = categoryIdFromIntent
            tvTitle.text = "Editar Categoría"
            loadCategoryData(categoryId!!)
        }
    }

    private fun initViews() {
        btnBack = findViewById(R.id.btnBack)
        tvTitle = findViewById(R.id.tvTitle)
        etCategoryIcon = findViewById(R.id.etCategoryIcon)
        etCategoryName = findViewById(R.id.etCategoryName)
        etCategoryDescription = findViewById(R.id.etCategoryDescription)
        etGradientStart = findViewById(R.id.etGradientStart)
        etGradientEnd = findViewById(R.id.etGradientEnd)
        switchIsActive = findViewById(R.id.switchIsActive)
        btnSave = findViewById(R.id.btnSave)

        btnBack.setOnClickListener { finish() }
        btnSave.setOnClickListener { saveCategory() }
    }

    private fun loadCategoryData(id: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // Obtener categoría desde Firebase
                val category = categoryRepository.findById(id)

                withContext(Dispatchers.Main) {
                    if (category != null) {
                        etCategoryIcon.setText(category.icon)
                        etCategoryName.setText(category.name)
                        etCategoryDescription.setText(category.description)
                        etGradientStart.setText(category.gradientStart)
                        etGradientEnd.setText(category.gradientEnd)
                        switchIsActive.isChecked = category.isActive
                    } else {
                        Toast.makeText(
                            this@CreateEditCategoryActivity,
                            "Categoría no encontrada",
                            Toast.LENGTH_SHORT
                        ).show()
                        finish()
                    }
                }

            } catch (e: Exception) {
                android.util.Log.e("CreateCategory", "❌ Error al cargar categoría", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@CreateEditCategoryActivity,
                        "Error al cargar categoría: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun saveCategory() {
        val icon = etCategoryIcon.text?.toString()?.trim()
        val name = etCategoryName.text?.toString()?.trim()
        val description = etCategoryDescription.text?.toString()?.trim()
        val gradientStart = etGradientStart.text?.toString()?.trim()
        val gradientEnd = etGradientEnd.text?.toString()?.trim()

        // Validaciones
        if (icon.isNullOrEmpty()) {
            Toast.makeText(this, "Ingresa un icono (emoji)", Toast.LENGTH_SHORT).show()
            return
        }

        if (name.isNullOrEmpty()) {
            Toast.makeText(this, "Ingresa el nombre de la categoría", Toast.LENGTH_SHORT).show()
            return
        }

        if (description.isNullOrEmpty()) {
            Toast.makeText(this, "Ingresa la descripción", Toast.LENGTH_SHORT).show()
            return
        }

        if (gradientStart.isNullOrEmpty()) {
            Toast.makeText(this, "Ingresa el color inicial", Toast.LENGTH_SHORT).show()
            return
        }

        if (gradientEnd.isNullOrEmpty()) {
            Toast.makeText(this, "Ingresa el color final", Toast.LENGTH_SHORT).show()
            return
        }

        // Validar formato de colores
        if (!gradientStart.matches(Regex("^#[0-9A-Fa-f]{6}$"))) {
            Toast.makeText(this, "Color inicial debe tener formato #RRGGBB", Toast.LENGTH_SHORT).show()
            return
        }

        if (!gradientEnd.matches(Regex("^#[0-9A-Fa-f]{6}$"))) {
            Toast.makeText(this, "Color final debe tener formato #RRGGBB", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // Crear objeto Category de Firebase
                val category = Category(
                    id = categoryId ?: "", // Si es edición, usar categoryId; si es nuevo, Firebase generará el ID
                    categoryId = name.lowercase().replace(" ", "_"),
                    name = name,
                    icon = icon,
                    description = description,
                    gradientStart = gradientStart,
                    gradientEnd = gradientEnd,
                    isActive = switchIsActive.isChecked,
                    order = 0, // Orden por defecto
                    createdAt = Timestamp.now(),
                    updatedAt = Timestamp.now()
                )

                // Guardar o actualizar en Firebase
                if (categoryId == null) {
                    categoryRepository.insert(category)
                    android.util.Log.d("CreateCategory", "✅ Nueva categoría creada en Firebase")
                } else {
                    categoryRepository.update(category)
                    android.util.Log.d("CreateCategory", "✅ Categoría actualizada en Firebase")
                }

                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@CreateEditCategoryActivity,
                        if (categoryId != null) "Categoría actualizada" else "Categoría creada",
                        Toast.LENGTH_SHORT
                    ).show()
                    finish()
                }

            } catch (e: Exception) {
                android.util.Log.e("CreateCategory", "❌ Error al guardar categoría", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@CreateEditCategoryActivity,
                        "Error al guardar: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }
}